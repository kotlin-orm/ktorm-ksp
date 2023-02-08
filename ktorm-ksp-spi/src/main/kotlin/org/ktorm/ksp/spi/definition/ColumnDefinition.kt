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

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.symbol.*
import org.ktorm.ksp.api.Column
import org.ktorm.ksp.api.PrimaryKey
import org.ktorm.ksp.api.References
import org.ktorm.ksp.api.SqlTypeFactory
import org.ktorm.ksp.spi.isSubclassOf
import org.ktorm.schema.SqlType
import kotlin.reflect.jvm.jvmName

/**
 * Column definition metadata.
 */
@OptIn(KspExperimental::class)
public class ColumnDefinition(_property: KSPropertyDeclaration, _table: TableDefinition) {
    private val column = _property.getAnnotationsByType(Column::class).firstOrNull()
    private val reference = _property.getAnnotationsByType(References::class).firstOrNull()
    private val primaryKey = _property.getAnnotationsByType(PrimaryKey::class).firstOrNull()

    /**
     * The annotated entity property of the column.
     */
    public val entityProperty: KSPropertyDeclaration = _property

    /**
     * The belonging table.
     */
    public val table: TableDefinition = _table

    /**
     * The name of the column.
     */
    public val name: String? = (column?.name ?: reference?.name ?: "").takeIf { it.isNotEmpty() }

    /**
     * Check if the column is a primary key.
     */
    public val isPrimaryKey: Boolean = primaryKey != null

    /**
     * Check if the column is a reference column.
     */
    public val isReference: Boolean = reference != null

    /**
     * The SQL type of the column.
     *
     * The specified class might be a subclass of [SqlType] or [SqlTypeFactory].
     */
    public val sqlType: KSType? = _property.annotations
        .find { anno -> anno.annotationType.resolve().declaration.qualifiedName?.asString() == Column::class.jvmName }
        ?.let { anno ->
            val argument = anno.arguments.find { it.name?.asString() == Column::sqlType.name }
            val sqlType = argument?.value as KSType?
            sqlType?.takeIf { it.declaration.qualifiedName?.asString() != Nothing::class.jvmName }
        }

    /**
     * The name of the corresponding column property in the generated table object.
     */
    public val tablePropertyName: String? =
        (column?.propertyName ?: reference?.propertyName ?: "").takeIf { it.isNotEmpty() }

    /**
     * Validate arguments.
     */
    init {
        if (column != null && reference != null) {
            throw IllegalStateException("@Column and @References cannot use together on the same property: $_property")
        }

        if (reference != null) {
            if (table.ktormEntityType != KtormEntityType.ENTITY_INTERFACE) {
                throw IllegalStateException("@References can only be used on entities based on Entity interface.")
            }

            // TODO: check referenced entity class.
        }

        if (sqlType != null) {
            val declaration = sqlType.declaration as KSClassDeclaration
            if (declaration.classKind != ClassKind.OBJECT) {
                val name = declaration.qualifiedName?.asString()
                throw IllegalArgumentException("The sqlType class $name must be a Kotlin singleton object.")
            }

            if (!declaration.isSubclassOf<SqlType<*>>() && !declaration.isSubclassOf<SqlTypeFactory>()) {
                val name = declaration.qualifiedName?.asString()
                throw IllegalArgumentException("The sqlType class $name must be subtype of SqlType or SqlTypeFactory.")
            }
        }
    }
}
