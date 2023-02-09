/*
 * Copyright 2022-2023 the original author or authors.
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

package org.ktorm.ksp.spi.definition

import com.google.devtools.ksp.symbol.KSClassDeclaration
import org.ktorm.ksp.api.Table

/**
 * Table definition metadata.
 */
public data class TableDefinition(

    /**
     * The annotated entity class of the table.
     */
    val entityClass: KSClassDeclaration,

    /**
     * The name of the table, see [Table.name].
     */
    val name: String?,

    /**
     * The alias of the table, see [Table.alias].
     */
    val alias: String?,

    /**
     * The catalog of the table, see [Table.catalog].
     */
    val catalog: String?,

    /**
     * The schema of the table, see [Table.schema].
     */
    val schema: String?,

    /**
     * The name of the corresponding table class in the generated code, see [Table.className].
     */
    val tableClassName: String?,

    /**
     * The name of the corresponding entity sequence in the generated code, see [Table.entitySequenceName].
     */
    val entitySequenceName: String?,

    /**
     * Properties that should be ignored for generating column definitions, see [Table.ignoreProperties].
     */
    val ignoreProperties: Set<String>,

    /**
     * Columns in the table.
     */
    val columns: List<ColumnDefinition>
)
