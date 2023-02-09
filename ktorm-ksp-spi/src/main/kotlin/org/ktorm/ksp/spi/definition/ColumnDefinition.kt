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

import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import org.ktorm.ksp.api.Column
import org.ktorm.ksp.api.PrimaryKey
import org.ktorm.ksp.api.References

/**
 * Column definition metadata.
 */
public data class ColumnDefinition(

    /**
     * The annotated entity property of the column.
     */
    val entityProperty: KSPropertyDeclaration,

    /**
     * The belonging table.
     */
    val table: TableDefinition,

    /**
     * The name of the column, see [Column.name] and [References.name].
     */
    val name: String?,

    /**
     * Check if the column is a primary key, see [PrimaryKey].
     */
    val isPrimaryKey: Boolean,

    /**
     * The SQL type of the column, see [Column.sqlType].
     */
    val sqlType: KSType?,

    /**
     * Check if the column is a reference column, see [References].
     */
    val isReference: Boolean,

    /**
     * The referenced table of the column.
     */
    val referenceTable: TableDefinition,

    /**
     * The name of the corresponding column property in the generated table object,
     * see [Column.propertyName] and [References.propertyName].
     */
    val tablePropertyName: String?
)
