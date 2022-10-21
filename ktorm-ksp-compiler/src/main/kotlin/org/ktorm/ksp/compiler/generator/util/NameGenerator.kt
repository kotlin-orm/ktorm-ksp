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

package org.ktorm.ksp.compiler.generator.util

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.toClassName
import org.atteo.evo.inflector.English
import org.ktorm.ksp.api.Column
import org.ktorm.ksp.api.References
import org.ktorm.ksp.api.Table
import org.ktorm.ksp.codegen.TableGenerateContext
import org.ktorm.ksp.codegen.definition.ColumnDefinition

public object NameGenerator {

    @OptIn(KspExperimental::class, KotlinPoetKspPreview::class)
    public fun generateTableClassName(entityClass: KSClassDeclaration): ClassName {
        val entityClassName = entityClass.toClassName()
        val table = entityClass.getAnnotationsByType(Table::class).first()
        val tableClassName = table.className.ifEmpty { English.plural(entityClassName.simpleName) }
        return ClassName(entityClassName.packageName, tableClassName)
    }

    @OptIn(KspExperimental::class)
    public fun generateTablePropertyName(tableClassName: ClassName, property: KSPropertyDeclaration): MemberName {
        val columnAnnotation = property.getAnnotationsByType(Column::class).firstOrNull()
        val referencesAnnotation = property.getAnnotationsByType(References::class).firstOrNull()
        val propertyName = property.simpleName.asString()

        val tablePropertyName = if (!columnAnnotation?.propertyName.isNullOrEmpty()) {
            columnAnnotation!!.propertyName
        } else if (!referencesAnnotation?.propertyName.isNullOrEmpty()) {
            referencesAnnotation!!.propertyName
        } else {
            propertyName
        }
        return MemberName(tableClassName, tablePropertyName)
    }

    public fun generateSqlTableName(context: TableGenerateContext): CodeBlock {
        val (table, config, _, _) = context
        val localNamingStrategy = config.localNamingStrategy
        return when {
            table.tableName.isNotEmpty() -> {
                CodeBlock.of("%S", table.tableName)
            }
            config.namingStrategy == null -> {
                CodeBlock.of("%S", table.entityClassName.simpleName)
            }
            localNamingStrategy != null -> {
                CodeBlock.of("%S", localNamingStrategy.toTableName(table.entityClassName.simpleName))
            }
            else -> {
                CodeBlock.of("%T.toTableName(%S)", config.namingStrategy, table.entityClassName.simpleName)
            }
        }
    }

    public fun generateSqlColumnName(context: TableGenerateContext, column: ColumnDefinition): CodeBlock {
        if (column.columnName.isNotEmpty()) {
            return CodeBlock.of("%S", column.columnName)
        }

        val propertyName = if (column.isReferences) {
            val referencesColumnName = column.referencesColumn!!.entityPropertyName.simpleName
            // virtualPropertyName
            buildString {
                append(column.entityPropertyName.simpleName)
                append(referencesColumnName.substring(0, 1).uppercase())
                if (referencesColumnName.length > 1) {
                    append(referencesColumnName.substring(1))
                }
            }
        } else {
            column.entityPropertyName.simpleName
        }

        val localNamingStrategy = context.config.localNamingStrategy
        val namingStrategy = context.config.namingStrategy
        return when {
            localNamingStrategy != null -> CodeBlock.of("%S", localNamingStrategy.toColumnName(propertyName))
            namingStrategy != null -> CodeBlock.of("%T.toColumnName(%S)", context.config.namingStrategy, propertyName)
            else -> CodeBlock.of("%S", propertyName)
        }
    }

}
