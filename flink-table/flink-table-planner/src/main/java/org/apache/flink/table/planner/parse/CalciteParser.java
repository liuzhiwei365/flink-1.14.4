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

package org.apache.flink.table.planner.parse;

import org.apache.flink.sql.parser.hive.impl.FlinkHiveSqlParserImpl;
import org.apache.flink.sql.parser.impl.FlinkSqlParserImpl;
import org.apache.flink.table.api.SqlParserException;

import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlAbstractParserImpl;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.util.SourceStringReader;

import java.io.Reader;

/**
 * Thin wrapper around {@link SqlParser} that does exception conversion and {@link SqlNode} casting.
 */
public class CalciteParser {
    private final SqlParser.Config config;

    public CalciteParser(SqlParser.Config config) {
        this.config = config;
    }

    /**
     * Parses a SQL statement into a {@link SqlNode}. The {@link SqlNode} is not yet validated.
     *
     * @param sql a sql string to parse
     * @return a parsed sql node
     * @throws SqlParserException if an exception is thrown when parsing the statement
     */
    public SqlNode parse(String sql) {
        try {
            // 这里调用 calcite 的 SqlParser的
            SqlParser parser = SqlParser.create(sql, config);
            /*
              最终会调用 FlinkSqlParserImpl.SqlStmt() 方法来将 sql语句解析成为 sqlNode

              而FlinkSqlParserImpl类 是由freemarker模版 和 javacc 生成的 ,所以我们分析逻辑的时候去看
              calcite中原生的模版文件Parser.jj 文件 和 codegen/includes/parserImpl.ftl 扩展的模版文件即可

              ****或者说,如果你不修改源码 , 你直接看 target/generated-sources/javacc/Parser.jj  更好, 该文件是由
              原生Parser.jj 文件 和 扩展模版逻辑 经过 freemarker合成的, 之后在经过javacc自动生成FlinkSqlParserImpl
              等解析器的java代码.
             */
            return parser.parseStmt();
        } catch (SqlParseException e) {
            throw new SqlParserException("SQL parse failed. " + e.getMessage(), e);
        }
    }

    /**
     * Parses a SQL expression into a {@link SqlNode}. The {@link SqlNode} is not yet validated.
     *
     * @param sqlExpression a SQL expression string to parse
     * @return a parsed SQL node
     * @throws SqlParserException if an exception is thrown when parsing the statement
     */
    public SqlNode parseExpression(String sqlExpression) {
        try {
            final SqlParser parser = SqlParser.create(sqlExpression, config);
            return parser.parseExpression();
        } catch (SqlParseException e) {
            throw new SqlParserException("SQL parse failed. " + e.getMessage(), e);
        }
    }

    /**
     * Parses a SQL string as an identifier into a {@link SqlIdentifier}.
     *
     * @param identifier a sql string to parse as an identifier
     * @return a parsed sql node
     * @throws SqlParserException if an exception is thrown when parsing the identifier
     */
    public SqlIdentifier parseIdentifier(String identifier) {
        try {
            SqlAbstractParserImpl flinkParser = createFlinkParser(identifier);
            if (flinkParser instanceof FlinkSqlParserImpl) {
                return ((FlinkSqlParserImpl) flinkParser).TableApiIdentifier();
            } else if (flinkParser instanceof FlinkHiveSqlParserImpl) {
                return ((FlinkHiveSqlParserImpl) flinkParser).TableApiIdentifier();
            } else {
                throw new IllegalArgumentException(
                        "Unrecognized sql parser type " + flinkParser.getClass().getName());
            }
        } catch (Exception e) {
            throw new SqlParserException(
                    String.format("Invalid SQL identifier %s.", identifier), e);
        }
    }

    /**
     * Equivalent to {@link SqlParser#create(Reader, SqlParser.Config)}. The only difference is we
     * do not wrap the {@link FlinkSqlParserImpl} with {@link SqlParser}.
     *
     * <p>It is so that we can access specific parsing methods not accessible through the {@code
     * SqlParser}.
     */
    private SqlAbstractParserImpl createFlinkParser(String expr) {
        SourceStringReader reader = new SourceStringReader(expr);
        SqlAbstractParserImpl parser = config.parserFactory().getParser(reader);
        parser.setTabSize(1);
        parser.setQuotedCasing(config.quotedCasing());
        parser.setUnquotedCasing(config.unquotedCasing());
        parser.setIdentifierMaxLength(config.identifierMaxLength());
        parser.setConformance(config.conformance());
        switch (config.quoting()) {
            case DOUBLE_QUOTE:
                parser.switchTo(SqlAbstractParserImpl.LexicalState.DQID);
                break;
            case BACK_TICK:
                parser.switchTo(SqlAbstractParserImpl.LexicalState.BTID);
                break;
            case BRACKET:
                parser.switchTo(SqlAbstractParserImpl.LexicalState.DEFAULT);
                break;
        }

        return parser;
    }
}
