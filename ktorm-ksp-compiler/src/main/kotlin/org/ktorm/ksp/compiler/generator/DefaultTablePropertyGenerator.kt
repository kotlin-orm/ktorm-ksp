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

package org.ktorm.ksp.compiler.generator

import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.buildCodeBlock
import org.ktorm.ksp.codegen.TableGenerateContext
import org.ktorm.ksp.codegen.TablePropertyGenerator
import org.ktorm.ksp.codegen.definition.KtormEntityType
import org.ktorm.ksp.compiler.generator.util.ColumnInitializerGenerator
import org.ktorm.ksp.compiler.generator.util.MemberNames
import org.ktorm.schema.Column

public open class DefaultTablePropertyGenerator : TablePropertyGenerator {

    override fun generate(context: TableGenerateContext): List<PropertySpec> {
        return when (context.table.ktormEntityType) {
            KtormEntityType.ENTITY_INTERFACE -> generateEntityInterfaceEntity(context)
            KtormEntityType.ANY_KIND_CLASS -> generateAnyKindClassEntity(context)
        }
    }

    protected open fun generateEntityInterfaceEntity(context: TableGenerateContext): List<PropertySpec> {
        val (table, config, _, _) = context
        return table.columns
            .asSequence()
            .map { column ->
                val columnType = if (column.isReferences) {
                    Column::class.asClassName().parameterizedBy(column.referencesColumn!!.nonNullPropertyTypeName)
                } else {
                    Column::class.asClassName().parameterizedBy(column.nonNullPropertyTypeName)
                }
                val localNamingStrategy = config.localNamingStrategy

                val columnName = when {
                    column.columnName.isNotEmpty() -> column.columnName
                    localNamingStrategy != null -> localNamingStrategy.toColumnName(column.entityPropertyName.simpleName)
                    else -> column.entityPropertyName.simpleName
                }

                PropertySpec
                    .builder(column.tablePropertyName.simpleName, columnType)
                    .addKdoc("Column %L. %L", columnName, column.propertyDeclaration.docString?.trimIndent().orEmpty())
                    .initializer(buildCodeBlock {
                        add(ColumnInitializerGenerator.generate(context, column))
                        val params = mutableMapOf(
                            "bindTo" to MemberNames.bindTo,
                            "references" to MemberNames.references,
                            "primaryKey" to MemberNames.primaryKey,
                            "referencesTable" to column.referencesColumn?.tableDefinition?.tableClassName,
                            "entityPropertyName" to column.entityPropertyName.simpleName
                        )
                        val code = buildString {
                            if (column.isPrimaryKey) {
                                append(".%primaryKey:M()")
                            }
                            if (column.isReferences) {
                                append(".%references:M(%referencesTable:T)·{·it.%entityPropertyName:L·}·")
                            } else {
                                append(".%bindTo:M·{·it.%entityPropertyName:L·}")
                            }
                        }
                        addNamed(code, params)
                    })
                    .build()
            }
            .toList()
    }

    protected open fun generateAnyKindClassEntity(context: TableGenerateContext): List<PropertySpec> {
        val (table, config, _, _) = context
        return table.columns
            .asSequence()
            .map { column ->
                val columnType = Column::class.asClassName().parameterizedBy(column.nonNullPropertyTypeName)
                val localNamingStrategy = config.localNamingStrategy
                val columnName = when {
                    column.columnName.isNotEmpty() -> column.columnName
                    localNamingStrategy != null -> localNamingStrategy.toColumnName(column.entityPropertyName.simpleName)
                    else -> column.entityPropertyName.simpleName
                }

                PropertySpec
                    .builder(column.tablePropertyName.simpleName, columnType)
                    .addKdoc("Column %L. %L", columnName, column.propertyDeclaration.docString?.trimIndent().orEmpty())
                    .initializer(buildCodeBlock {
                        add(ColumnInitializerGenerator.generate(context, column))
                        if (column.isPrimaryKey) {
                            add(".%M()", MemberNames.primaryKey)
                        }
                    })
                    .build()
            }
            .toList()
    }
}
