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

package org.apache.flink.table.types;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.table.catalog.DataTypeFactory;
import org.apache.flink.table.types.logical.LogicalType;

import javax.annotation.Nullable;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Partially resolved data type that requires a lookup in a catalog or configuration before creating
 * the corresponding {@link LogicalType}.
 *
 * <p>Users are able to influence the nullability and conversion class even if the actual {@link
 * LogicalType} is not fully known yet. The information is stored and verified when resolving to
 * {@link DataType} lazily.
 */
@PublicEvolving
public final class UnresolvedDataType implements AbstractDataType<UnresolvedDataType> {

    private static final String FORMAT = "[%s]"; // indicates that this is an unresolved type

    private final @Nullable Boolean isNullable;

    private final @Nullable Class<?> conversionClass;

    private final Supplier<String> description;

    private final Function<DataTypeFactory, DataType> resolutionFactory;

    private UnresolvedDataType(
            @Nullable Boolean isNullable,
            @Nullable Class<?> conversionClass,
            Supplier<String> description,
            Function<DataTypeFactory, DataType> resolutionFactory) {
        this.isNullable = isNullable;
        this.conversionClass = conversionClass;
        this.description = description;
        this.resolutionFactory = resolutionFactory;
    }

    public UnresolvedDataType(
            Supplier<String> description, Function<DataTypeFactory, DataType> resolutionFactory) {
        this(null, null, description, resolutionFactory);
    }

    /**
     * Converts this instance to a resolved {@link DataType} possibly enriched with additional
     * nullability and conversion class information.
     */
    /*
        1 DataType 的 4个实现：

            AtomicDataType        不可再分割的基本原子类型
            KeyValueDataType      字段是 map 类型
            CollectionDataType    字段是 集合 类型
            FieldsDataType        字段内部 可以嵌套多个字段

        2 KeyValueDataType 与 FieldsDataType 的区别：
               KeyValueDataType中的 k 和 v 的类型是固定的
               而 FieldsDataType中,每个子字段都可以定义自己的类型,类型可以无限多
 */

    public DataType toDataType(DataTypeFactory factory) {
        //  factory <= DataTypeFactoryImpl
        //   这里的apply 方法 一定调用  DataTypeFactoryImpl 的 createDataType方法
        DataType resolvedDataType = resolutionFactory.apply(factory);
        if (isNullable == Boolean.TRUE) {
            // 好比在ddl 定义了 某个字段可以为 null
            resolvedDataType = resolvedDataType.nullable();
        } else if (isNullable == Boolean.FALSE) {
            //  好比在ddl 定义了 某个字段为  is not null
            resolvedDataType = resolvedDataType.notNull();
        }
        if (conversionClass != null) {
            resolvedDataType = resolvedDataType.bridgedTo(conversionClass);
        }
        return resolvedDataType;
    }

    @Override
    public UnresolvedDataType notNull() {
        return new UnresolvedDataType(false, conversionClass, description, resolutionFactory);
    }

    @Override
    public UnresolvedDataType nullable() {
        return new UnresolvedDataType(true, conversionClass, description, resolutionFactory);
    }

    @Override
    public UnresolvedDataType bridgedTo(Class<?> newConversionClass) {
        return new UnresolvedDataType(
                isNullable, newConversionClass, description, resolutionFactory);
    }

    @Override
    public String toString() {
        return String.format(FORMAT, description.get());
    }
}
