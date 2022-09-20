/*
 * Copyright 2022 the original author or authors.
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

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSFile
import org.ktorm.ksp.codegen.definition.TableDefinition

/**
 * Context information for generating table code.
 */
public data class TableGenerateContext(

    /**
     * The table definition.
     */
    val table: TableDefinition,

    /**
     * The global code generate config.
     */
    val config: CodeGenerateConfig,

    /**
     * The column initializer generator. For creating column object in the table
     * Example:  int("id").primaryKey()
     */
    val columnInitializerGenerator: ColumnInitializerGenerator,

    /**
     * The ksp logger.
     */
    val logger: KSPLogger,

    /**
     * The associated file of the output table code, for incremental update of KSP.
     */
    val dependencyFiles: MutableSet<KSFile> = mutableSetOf()
)
