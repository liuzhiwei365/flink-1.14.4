/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.table.planner.delegation

import org.apache.flink.annotation.VisibleForTesting
import org.apache.flink.api.dag.Transformation
import org.apache.flink.configuration.ReadableConfig
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment
import org.apache.flink.table.api.config.{ExecutionConfigOptions, TableConfigOptions}
import org.apache.flink.table.api.{PlannerType, SqlDialect, TableConfig, TableEnvironment, TableException}
import org.apache.flink.table.catalog._
import org.apache.flink.table.connector.sink.DynamicTableSink
import org.apache.flink.table.delegation.{Executor, Parser, Planner}
import org.apache.flink.table.descriptors.{ConnectorDescriptorValidator, DescriptorProperties}
import org.apache.flink.table.factories.{FactoryUtil, TableFactoryUtil}
import org.apache.flink.table.operations.OutputConversionModifyOperation.UpdateMode
import org.apache.flink.table.operations._
import org.apache.flink.table.planner.JMap
import org.apache.flink.table.planner.calcite._
import org.apache.flink.table.planner.catalog.CatalogManagerCalciteSchema
import org.apache.flink.table.planner.connectors.DynamicSinkUtils
import org.apache.flink.table.planner.connectors.DynamicSinkUtils.validateSchemaAndApplyImplicitCast
import org.apache.flink.table.planner.delegation.ParserFactory.DefaultParserContext
import org.apache.flink.table.planner.expressions.PlannerTypeInferenceUtilImpl
import org.apache.flink.table.planner.hint.FlinkHints
import org.apache.flink.table.planner.plan.nodes.calcite.LogicalLegacySink
import org.apache.flink.table.planner.plan.nodes.exec.processor.{ExecNodeGraphProcessor, ProcessorContext}
import org.apache.flink.table.planner.plan.nodes.exec.serde.SerdeContext
import org.apache.flink.table.planner.plan.nodes.exec.{ExecNodeGraph, ExecNodeGraphGenerator}
import org.apache.flink.table.planner.plan.nodes.physical.FlinkPhysicalRel
import org.apache.flink.table.planner.plan.optimize.Optimizer
import org.apache.flink.table.planner.plan.reuse.SubplanReuser
import org.apache.flink.table.planner.plan.utils.SameRelObjectShuttle
import org.apache.flink.table.planner.sinks.DataStreamTableSink
import org.apache.flink.table.planner.sinks.TableSinkUtils.{inferSinkPhysicalSchema, validateLogicalPhysicalTypesCompatible, validateTableSink}
import org.apache.flink.table.planner.utils.InternalConfigOptions.{TABLE_QUERY_START_EPOCH_TIME, TABLE_QUERY_START_LOCAL_TIME}
import org.apache.flink.table.planner.utils.JavaScalaConversionUtil
import org.apache.flink.table.sinks.TableSink
import org.apache.flink.table.types.utils.LegacyTypeInfoDataTypeConverter

import org.apache.calcite.jdbc.CalciteSchemaBuilder.asRootSchema
import org.apache.calcite.plan.{RelTrait, RelTraitDef}
import org.apache.calcite.rel.RelNode
import org.apache.calcite.rel.hint.RelHint
import org.apache.calcite.tools.FrameworkConfig

import java.lang.{Long => JLong}
import java.util
import java.util.TimeZone

import _root_.scala.collection.JavaConversions._

/**
  * Implementation of a [[Planner]]. It supports only streaming use cases.
  * (The new [[org.apache.flink.table.sources.InputFormatTableSource]] should work, but will be
  * handled as streaming sources, and no batch specific optimizations will be applied).
  *
  * @param executor        instance of [[Executor]], needed to extract
  *                        [[StreamExecutionEnvironment]] for
  *                        [[org.apache.flink.table.sources.StreamTableSource.getDataStream]]
  * @param config          mutable configuration passed from corresponding [[TableEnvironment]]
  * @param functionCatalog catalog of functions
  * @param catalogManager  manager of catalog meta objects such as tables, views, databases etc.
  * @param isStreamingMode Determines if the planner should work in a batch (false}) or
  *                        streaming (true) mode.
  */
abstract class PlannerBase(
    executor: Executor,
    config: TableConfig,
    val functionCatalog: FunctionCatalog,
    val catalogManager: CatalogManager,
    isStreamingMode: Boolean)
  extends Planner {

  // temporary utility until we don't use planner expressions anymore
  functionCatalog.setPlannerTypeInferenceUtil(PlannerTypeInferenceUtilImpl.INSTANCE)

  private var parser: Parser = _
  private var currentDialect: SqlDialect = getTableConfig.getSqlDialect

  private val plannerConfiguration: ReadableConfig = new PlannerConfiguration(
    config.getConfiguration,
    executor.getConfiguration)

  @VisibleForTesting
  private[flink] val plannerContext: PlannerContext =
    new PlannerContext(
      !isStreamingMode,
      config,
      functionCatalog,
      catalogManager,
      asRootSchema(new CatalogManagerCalciteSchema(catalogManager, isStreamingMode)),
      getTraitDefs.toList
    )

  /** Returns the [[FlinkRelBuilder]] of this TableEnvironment. */
  private[flink] def getRelBuilder: FlinkRelBuilder = {
    val currentCatalogName = catalogManager.getCurrentCatalog
    val currentDatabase = catalogManager.getCurrentDatabase
    plannerContext.createRelBuilder(currentCatalogName, currentDatabase)
  }

  /** Returns the Calcite [[FrameworkConfig]] of this TableEnvironment. */
  @VisibleForTesting
  private[flink] def createFlinkPlanner: FlinkPlannerImpl = {
    val currentCatalogName = catalogManager.getCurrentCatalog
    val currentDatabase = catalogManager.getCurrentDatabase
    plannerContext.createFlinkPlanner(currentCatalogName, currentDatabase)
  }

  /** Returns the [[FlinkTypeFactory]] of this TableEnvironment. */
  private[flink] def getTypeFactory: FlinkTypeFactory = plannerContext.getTypeFactory

  /** Returns specific RelTraitDefs depends on the concrete type of this TableEnvironment. */
  protected def getTraitDefs: Array[RelTraitDef[_ <: RelTrait]]

  /** Returns specific query [[Optimizer]] depends on the concrete type of this TableEnvironment. */
  protected def getOptimizer: Optimizer

  def getTableConfig: TableConfig = config

  def getFlinkContext: FlinkContext = plannerContext.getFlinkContext

  /**
   * Gives access to both API specific table configuration and executor configuration.
   *
   * This configuration should be the main source of truth in the planner module.
   */
  def getConfiguration: ReadableConfig = plannerConfiguration

  /**
   * @deprecated Do not use this method anymore. Use [[getConfiguration]] to access options.
   *             Create transformations without it. A [[StreamExecutionEnvironment]] is a mixture
   *             of executor and stream graph generator/builder. In the long term, we would like
   *             to avoid the need for it in the planner module.
   */
  @deprecated
  private[flink] def getExecEnv: StreamExecutionEnvironment = {
    executor.asInstanceOf[DefaultExecutor].getExecutionEnvironment
  }

  def createNewParser: Parser = {
    val factoryIdentifier = getTableConfig.getSqlDialect.name().toLowerCase
    val parserFactory = FactoryUtil.discoverFactory(Thread.currentThread.getContextClassLoader,
      classOf[ParserFactory], factoryIdentifier)

    val context = new DefaultParserContext(catalogManager, plannerContext)
    parserFactory.create(context)
  }

  override def getParser: Parser = {
    if (parser == null || getTableConfig.getSqlDialect != currentDialect) {
      parser = createNewParser
      currentDialect = getTableConfig.getSqlDialect
    }
    parser
  }

  override def translate(
      modifyOperations: util.List[ModifyOperation]): util.List[Transformation[_]] = {
    validateAndOverrideConfiguration()
    if (modifyOperations.isEmpty) {
      return List.empty[Transformation[_]]
    }

    //将修改操作 都 转化为 RelNode ,逻辑是 translateToRel
    val relNodes = modifyOperations.map(translateToRel)

    // 优化器 优化 RelNode
    val optimizedRelNodes = optimize(relNodes)

    // 这里execGraph 是sql 层面的执行计划
    val execGraph = translateToExecNodeGraph(optimizedRelNodes)

    // 装变为 flink 内核层面的 装换操作
    val transformations = translateToPlan(execGraph)
    cleanupInternalConfigurations()
    transformations
  }

  /**
    * Converts a relational tree of [[ModifyOperation]] into a Calcite relational expression.
    */

    // 将当个的 ModifyOperation 翻译为 RelNode
  @VisibleForTesting
  private[flink] def translateToRel(modifyOperation: ModifyOperation): RelNode = {
    val dataTypeFactory = catalogManager.getDataTypeFactory
    /*
      modifyOperation 会根据不同的细分类型分别处理
    */
    modifyOperation match {

      case s: UnregisteredSinkModifyOperation[_] =>
        val input = getRelBuilder.queryOperation(s.getChild).build()
        val sinkSchema = s.getSink.getTableSchema

        // 检验 查询语句的 schema 和 sink 的schema , 如果需要的话,做类型转换
        val query = validateSchemaAndApplyImplicitCast(
          input,
          catalogManager.getSchemaResolver.resolve(sinkSchema.toSchema),
          null,
          dataTypeFactory,
          getTypeFactory)

        LogicalLegacySink.create(
          query,
          s.getSink,
          "UnregisteredSink",
          ConnectorCatalogTable.sink(s.getSink, !isStreamingMode))

      case collectModifyOperation: CollectModifyOperation =>
        val input = getRelBuilder.queryOperation(modifyOperation.getChild).build()
        DynamicSinkUtils.convertCollectToRel(
          getRelBuilder, input, collectModifyOperation, getTableConfig.getConfiguration)

        // insert 语句会匹配到 CatalogSinkModifyOperation
      case catalogSink: CatalogSinkModifyOperation =>
        val input = getRelBuilder.queryOperation(modifyOperation.getChild).build()
        val identifier = catalogSink.getTableIdentifier
        // dynamicOptions 承载了 sql hints的内容
        val dynamicOptions = catalogSink.getDynamicOptions

        /* getTableSink 返回 (ResolvedCatalogTable, Any) */
        getTableSink(identifier, dynamicOptions).map {
          // 这里的sink 要么是 TableSink,要么是 DynamicTableSink (TableSink是老api,推荐走DynamicTableSink的逻辑)
          case (table, sink: TableSink[_]) =>
            // 检查逻辑字段类型 和物理字段类型 是否兼容
            val queryLogicalType = FlinkTypeFactory.toLogicalRowType(input.getRowType)
            // 校验 逻辑schema 和 物理 schema 是否兼容
            validateLogicalPhysicalTypesCompatible(table, sink, queryLogicalType)
            // 校验TableSink
            validateTableSink(catalogSink, identifier, sink, table.getPartitionKeys)
            // 校验 查询的 schema 和 sink 的schema , 如果可能做类型装换
            val query = validateSchemaAndApplyImplicitCast(
              input,
              table.getResolvedSchema,
              catalogSink.getTableIdentifier,
              dataTypeFactory,
              getTypeFactory)
            // 处理sql暗示
            val hints = new util.ArrayList[RelHint]
            if (!dynamicOptions.isEmpty) {
              hints.add(RelHint.builder("OPTIONS").hintOptions(dynamicOptions).build)
            }
            LogicalLegacySink.create(
              query,
              hints,
              sink,
              identifier.toString,
              table,
              catalogSink.getStaticPartitions.toMap)

          case (table, sink: DynamicTableSink) =>
            // 新版本走的阳光道
            DynamicSinkUtils.convertSinkToRel(getRelBuilder, input, catalogSink, sink, table)
        } match {
          case Some(sinkRel) => sinkRel
          case None =>
            throw new TableException(s"Sink ${catalogSink.getTableIdentifier} does not exists")
        }

      case externalModifyOperation: ExternalModifyOperation =>
        // flink changelog 会走这里;  最后会返回一个 LogicalSink对象, 内部包含 ExternalDynamicSink 成员
        // flink任务将来运行时走的 主要的逻辑就在  ExternalDynamicSink (父类是 DynamicTableSink)里面

        val input = getRelBuilder.queryOperation(modifyOperation.getChild).build()
        DynamicSinkUtils.convertExternalToRel(getRelBuilder, input, externalModifyOperation)



      // 输出转换操作, 把普通流 , retract 流 , append 流 和 upsert 流相互装换
      case outputConversion: OutputConversionModifyOperation =>
        val input = getRelBuilder.queryOperation(outputConversion.getChild).build()
        // 处理 flinksql 的 cdc
        val (needUpdateBefore, withChangeFlag) = outputConversion.getUpdateMode match {
          case UpdateMode.RETRACT => (true, true)
          case UpdateMode.APPEND => (false, false)
          case UpdateMode.UPSERT => (false, true)
        }
        val typeInfo = LegacyTypeInfoDataTypeConverter.toLegacyTypeInfo(outputConversion.getType)
        val inputLogicalType = FlinkTypeFactory.toLogicalRowType(input.getRowType)
        val sinkPhysicalSchema = inferSinkPhysicalSchema(
          outputConversion.getType,
          inputLogicalType,
          withChangeFlag)

        // 检验 查询语句的 schema 和 sink 的schema , 如果需要的话,做类型转换
        val query = validateSchemaAndApplyImplicitCast(
          input,
          catalogManager.getSchemaResolver.resolve(sinkPhysicalSchema.toSchema),
          null,
          dataTypeFactory,
          getTypeFactory)

        val tableSink = new DataStreamTableSink(
          FlinkTypeFactory.toTableSchema(query.getRowType),
          typeInfo,
          needUpdateBefore,
          withChangeFlag)
        LogicalLegacySink.create(
          query,
          tableSink,
          "DataStreamTableSink",
          ConnectorCatalogTable.sink(tableSink, !isStreamingMode))

      case _ =>
        throw new TableException(s"Unsupported ModifyOperation: $modifyOperation")
    }
  }

  @VisibleForTesting
  private[flink] def optimize(relNodes: Seq[RelNode]): Seq[RelNode] = {
    val optimizedRelNodes = getOptimizer.optimize(relNodes)
    require(optimizedRelNodes.size == relNodes.size)
    optimizedRelNodes
  }

  @VisibleForTesting
  private[flink] def optimize(relNode: RelNode): RelNode = {
    val optimizedRelNodes = getOptimizer.optimize(Seq(relNode))
    require(optimizedRelNodes.size == 1)
    optimizedRelNodes.head
  }

  /**
   * Converts [[FlinkPhysicalRel]] DAG to [[ExecNodeGraph]],
   * tries to reuse duplicate sub-plans and transforms the graph based on the given processors.
   */

  @VisibleForTesting
  private[flink] def translateToExecNodeGraph(optimizedRelNodes: Seq[RelNode]): ExecNodeGraph = {

    //  step1  校验
    val nonPhysicalRel = optimizedRelNodes.filterNot(_.isInstanceOf[FlinkPhysicalRel])
    if (nonPhysicalRel.nonEmpty) {
      throw new TableException("The expected optimized plan is FlinkPhysicalRel plan, " +
        s"actual plan is ${nonPhysicalRel.head.getClass.getSimpleName} plan.")
    }
    require(optimizedRelNodes.forall(_.isInstanceOf[FlinkPhysicalRel]))

    //  step2  这种优化情况是 虽然 Filter1 和 Filter2 最终扫描的是同一个表
    //         但是它们的过滤条件不一样, 还是得扫描两次
    //  *      Join                       Join
    //  *     /    \                     /    \
    //  * Filter1 Filter2     =>     Filter1 Filter2
    //  *     \   /                     |      |
    //  *      Scan                  Scan1    Scan2
    val shuttle = new SameRelObjectShuttle()
    val relsWithoutSameObj = optimizedRelNodes.map(_.accept(shuttle))

    // step3  发现完全相同的子计划,  合并成一个 可重用的子计划
    //  *       Join                      Join
    //  *     /      \                  /      \
    //  * Filter1  Filter2          Filter1  Filter2
    //  *    |        |        =>       \     /
    //  * Project1 Project2            Project1
    //  *    |        |                   |
    //  *  Scan1    Scan2               Scan1
    //   装换的前提  Scan1 == Scan2  且 Project1 == max(原来的 Project1, 原来的 Project2)
    val reusedPlan = SubplanReuser.reuseDuplicatedSubplan(relsWithoutSameObj, config)

    // step4  将  FlinkPhysicalRel DAG  转变为  ExecNodeGraph
    // 在调用 generate 方法生成 sql 执行图之前, 先将所有的关系代数 强转为 FlinkPhysicalRel
    val generator = new ExecNodeGraphGenerator()
    val execGraph = generator.generate(reusedPlan.map(_.asInstanceOf[FlinkPhysicalRel]))

    // step5 处理执行图
    //   BatchPlanner 时 , processors 才有实际的内容;   StreamPlanner时, processors 为空
    val context = new ProcessorContext(this)
    val processors = getExecNodeGraphProcessors
    processors.foldLeft(execGraph)((graph, processor) => processor.process(graph, context))
  }

  protected def getExecNodeGraphProcessors: Seq[ExecNodeGraphProcessor]

  /**
    * Translates an [[ExecNodeGraph]] into a [[Transformation]] DAG.
    *
    * @param execGraph The node graph to translate.
    * @return The [[Transformation]] DAG that corresponds to the node DAG.
    */
  protected def translateToPlan(execGraph: ExecNodeGraph): util.List[Transformation[_]]

  private def getTableSink(
      objectIdentifier: ObjectIdentifier,
      dynamicOptions: JMap[String, String])
    : Option[(ResolvedCatalogTable, Any)] = {
    val optionalLookupResult =
      JavaScalaConversionUtil.toScala(catalogManager.getTable(objectIdentifier))
    if (optionalLookupResult.isEmpty) {
      return None
    }
    val lookupResult = optionalLookupResult.get
    lookupResult.getTable match {
      case connectorTable: ConnectorCatalogTable[_, _] =>
        val resolvedTable = lookupResult.getResolvedTable.asInstanceOf[ResolvedCatalogTable]
        JavaScalaConversionUtil.toScala(connectorTable.getTableSink) match {
          case Some(sink) => Some(resolvedTable, sink)
          case None => None
        }

      case regularTable: CatalogTable =>
        val resolvedTable = lookupResult.getResolvedTable.asInstanceOf[ResolvedCatalogTable]
        val tableToFind = if (dynamicOptions.nonEmpty) {
          // 将sql暗示 与 动态配置项 合并
          resolvedTable.copy(FlinkHints.mergeTableOptions(dynamicOptions, resolvedTable.getOptions))
        } else {
          resolvedTable
        }
        val catalog = catalogManager.getCatalog(objectIdentifier.getCatalogName)
        val isTemporary = lookupResult.isTemporary
        // connector.type
        if (isLegacyConnectorOptions(objectIdentifier, resolvedTable.getOrigin, isTemporary)) {
          // 返回 TableSink ,老api, DynamicTableSink 是它的替代者
          val tableSink = TableFactoryUtil.findAndCreateTableSink(
            catalog.orElse(null),
            objectIdentifier,
            tableToFind.getOrigin,
            getTableConfig.getConfiguration,
            isStreamingMode,
            isTemporary)
          Option(resolvedTable, tableSink)
        } else {
          // 返回 DynamicTableSink
          val tableSink = FactoryUtil.createTableSink(
            catalog.orElse(null),
            objectIdentifier,
            tableToFind,
            getTableConfig.getConfiguration,
            getClassLoader,
            isTemporary)
          Option(resolvedTable, tableSink)
        }

      case _ => None
    }
  }

  /**
   * Checks whether the [[CatalogTable]] uses legacy connector sink options.
   */
  private def isLegacyConnectorOptions(
      objectIdentifier: ObjectIdentifier,
      catalogTable: CatalogTable,
      isTemporary: Boolean) = {
    // normalize option keys
    val properties = new DescriptorProperties(true)
    properties.putProperties(catalogTable.getOptions)
    if (properties.containsKey(ConnectorDescriptorValidator.CONNECTOR_TYPE)) {
      true
    } else {
      val catalog = catalogManager.getCatalog(objectIdentifier.getCatalogName)
      try {
        // try to create legacy table source using the options,
        // some legacy factories uses the new 'connector' key
        TableFactoryUtil.findAndCreateTableSink(
          catalog.orElse(null),
          objectIdentifier,
          catalogTable,
          getTableConfig.getConfiguration,
          isStreamingMode,
          isTemporary)
        // success, then we will use the legacy factories
        true
      } catch {
        // fail, then we will use new factories
        case _: Throwable => false
      }
    }
  }

  override def getJsonPlan(modifyOperations: util.List[ModifyOperation]): String = {
    if (!isStreamingMode) {
      throw new TableException("Only streaming mode is supported now.")
    }
    validateAndOverrideConfiguration()
    val relNodes = modifyOperations.map(translateToRel)
    val optimizedRelNodes = optimize(relNodes)
    val execGraph = translateToExecNodeGraph(optimizedRelNodes)
    val jsonPlan = ExecNodeGraph.createJsonPlan(execGraph, createSerdeContext)
    cleanupInternalConfigurations()
    jsonPlan
  }

  override def translateJsonPlan(jsonPlan: String): util.List[Transformation[_]] = {
    if (!isStreamingMode) {
      throw new TableException("Only streaming mode is supported now.")
    }
    validateAndOverrideConfiguration()
    val execGraph = ExecNodeGraph.createExecNodeGraph(jsonPlan, createSerdeContext)
    val transformations = translateToPlan(execGraph)
    cleanupInternalConfigurations()
    transformations
  }

  protected def createSerdeContext: SerdeContext = {
    val planner = createFlinkPlanner
    new SerdeContext(
      planner.config.getContext.asInstanceOf[FlinkContext],
      getClassLoader,
      plannerContext.getTypeFactory,
      planner.operatorTable
    )
  }

  private def getClassLoader: ClassLoader = {
    Thread.currentThread().getContextClassLoader
  }

  /**
   * Different planner has different rules. Validate the planner and runtime mode is consistent with
   * the configuration before planner do optimization with [[ModifyOperation]] or other works.
   */
  protected def validateAndOverrideConfiguration(): Unit = {
    val configuration = config.getConfiguration
    if (!configuration.get(TableConfigOptions.TABLE_PLANNER).equals(PlannerType.BLINK)) {
      throw new IllegalArgumentException(
        "Mismatch between configured planner and actual planner. " +
          "Currently, the 'table.planner' can only be set when instantiating the " +
          "table environment. Subsequent changes are not supported. " +
          "Please instantiate a new TableEnvironment if necessary.");
    }

    // Add query start time to TableConfig, these config are used internally,
    // these configs will be used by temporal functions like CURRENT_TIMESTAMP,LOCALTIMESTAMP.
    val epochTime :JLong = System.currentTimeMillis()
    configuration.set(TABLE_QUERY_START_EPOCH_TIME, epochTime)
    val localTime :JLong =  epochTime +
      TimeZone.getTimeZone(config.getLocalTimeZone).getOffset(epochTime)
    configuration.set(TABLE_QUERY_START_LOCAL_TIME, localTime)

    getExecEnv.configure(
      configuration,
      Thread.currentThread().getContextClassLoader)

    // Use config parallelism to override env parallelism.
    val defaultParallelism = getTableConfig.getConfiguration.getInteger(
      ExecutionConfigOptions.TABLE_EXEC_RESOURCE_DEFAULT_PARALLELISM)
    if (defaultParallelism > 0) {
      getExecEnv.getConfig.setParallelism(defaultParallelism)
    }
  }

  /**
   * Cleanup all internal configuration after plan translation finished.
   */
  protected def cleanupInternalConfigurations(): Unit = {
    val configuration = config.getConfiguration
    configuration.removeConfig(TABLE_QUERY_START_EPOCH_TIME)
    configuration.removeConfig(TABLE_QUERY_START_LOCAL_TIME)
  }
}
