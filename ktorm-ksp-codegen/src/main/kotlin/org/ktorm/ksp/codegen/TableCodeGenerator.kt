/*
 * Copyright 2018-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ktorm.ksp.codegen

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

/**
 * Standard interface for generating table code.
 */
public sealed interface TableCodeGenerator<T : Any> {

    /**
     * Generate table component code.
     * @param context the table generate context, including the definition of table and its column,
     * global configuration, etc.
     * @param emitter After generating the specified component code, call this function to submit the code
     */
    public fun generate(context: TableGenerateContext): T
}

/**
 * Generate table type code.
 */
public interface TableTypeGenerator : TableCodeGenerator<TypeSpec.Builder>

/**
 * Generate table properties code.
 */
public interface TablePropertyGenerator : TableCodeGenerator<List<PropertySpec>>

/**
 * Generate table functions code.
 */
public interface TableFunctionGenerator : TableCodeGenerator<List<FunSpec>>

/**
 * Generate top-level properties code in table file.
 */
public interface TopLevelPropertyGenerator : TableCodeGenerator<List<PropertySpec>>

/**
 * Generate top-level functions code in table file.
 */
public interface TopLevelFunctionGenerator : TableCodeGenerator<List<FunSpec>>
