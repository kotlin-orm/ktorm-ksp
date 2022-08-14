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

package org.ktorm.ksp.codegen.definition

import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeName

/**
 * Column definitions, which contain all the information about the column, including column names,
 * converters, types, etc.
 */
public data class ColumnDefinition(

    /**
     * The column name，Corresponds to the [org.ktorm.ksp.api.Column.columnName] property, may be an empty string.
     */
    val columnName: String,

    /**
     * Specifies whether the column is the primary key, Corresponds to the [org.ktorm.ksp.api.PrimaryKey] annotation.
     */
    val isPrimaryKey: Boolean,

    /**
     * Type name of the entity property.
     */
    val propertyTypeName: TypeName,

    /**
     * Name of the entity property.
     */
    val entityPropertyName: MemberName,

    /**
     * Name of the table property.
     */
    val tablePropertyName: MemberName,

    /**
     * The SqlType for this column.
     */
    val sqlType: ClassName?,

    /**
     * The SqlTypeFactory for this column
     */
    val sqlTypeFactory: ClassName?,

    /**
     * Entity property declaration.
     */
    val propertyDeclaration: KSPropertyDeclaration,

    /**
     * Entity property type.
     */
    val propertyType: KSType,

    /**
     * The table definition.
     */
    val tableDefinition: TableDefinition,

    /**
     * Is it a reference column，Corresponds to the [org.ktorm.ksp.api.Column.isReferences] property.
     */
    val isReferences: Boolean,

    /**
     * Column definitions for referenced columns.
     */
    var referencesColumn: ColumnDefinition?
) {

    /**
     * Is it a mutable property.
     */
    val isMutable: Boolean = propertyDeclaration.isMutable

    /**
     * Is it a nullable property.
     */
    val isNullable: Boolean = propertyType.nullability != Nullability.NOT_NULL

    /**
     * Is it an Enum type property.
     */
    val isEnum: Boolean = (propertyType.declaration as KSClassDeclaration).classKind == ClassKind.ENUM_CLASS

    /**
     * Non-null type name of the entity property.
     */
    val nonNullPropertyTypeName: TypeName = propertyTypeName.copy(nullable = false)

    override fun toString(): String {
        return "ColumnDefinition(columnName='$columnName', isPrimaryKey=$isPrimaryKey, propertyTypeName=" +
                "$propertyTypeName, entityPropertyName=$entityPropertyName, tablePropertyName=$tablePropertyName, " +
                "sqlType=$sqlType, referencesColumn=$referencesColumn)"
    }
}
