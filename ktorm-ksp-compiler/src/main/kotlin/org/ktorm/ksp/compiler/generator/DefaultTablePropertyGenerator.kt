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

package org.ktorm.ksp.compiler.generator

import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.buildCodeBlock
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.toTypeName
import org.ktorm.ksp.compiler.util.ColumnInitializerGenerator
import org.ktorm.ksp.compiler.util.MemberNames
import org.ktorm.ksp.spi.TableGenerateContext
import org.ktorm.ksp.spi.TablePropertyGenerator
import org.ktorm.ksp.spi.definition.KtormEntityType
import org.ktorm.schema.Column

@OptIn(KotlinPoetKspPreview::class)
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
                val columnType = if (column.isReference) {
                    Column::class.asClassName().parameterizedBy(column.referencesColumn!!.nonNullPropertyTypeName)
                } else {
                    Column::class.asClassName().parameterizedBy(column.entityProperty.type.resolve().toTypeName().copy(nullable = true))
                }
                val localNamingStrategy = config.localDatabaseNamingStrategy

                val columnName = when {
                    column.name != null -> column.name!!
                    localNamingStrategy != null -> localNamingStrategy.toColumnName(column.entityProperty.simpleName.asString())
                    else -> column.entityProperty.simpleName.asString()
                }

                PropertySpec
                    .builder(column.tablePropertyName!!, columnType)
                    .addKdoc("Column %L. %L", columnName, column.entityProperty.docString?.trimIndent().orEmpty())
                    .initializer(buildCodeBlock {
                        add(ColumnInitializerGenerator.generate(context, column))
                        val params = mutableMapOf(
                            "bindTo" to MemberNames.bindTo,
                            "references" to MemberNames.references,
                            "primaryKey" to MemberNames.primaryKey,
                            "referencesTable" to column.referencesColumn?.tableDefinition?.tableClassName,
                            "entityPropertyName" to column.entityProperty.simpleName.asString()
                        )
                        val code = buildString {
                            if (column.isPrimaryKey) {
                                append(".%primaryKey:M()")
                            }
                            if (column.isReference) {
                                append(".%references:M(%referencesTable:T)·{·it.%entityPropertyName:L·}·")
                            } else {
                                append(".%bindTo:M·{·it.%entityPropertyName:N·}")
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
                val propertyType = column.entityProperty.type.resolve()
                val columnType = Column::class.asClassName().parameterizedBy(propertyType.toTypeName().copy(nullable = true))
                val localNamingStrategy = config.localDatabaseNamingStrategy
                val columnName = when {
                    column.name != null -> column.name!!
                    localNamingStrategy != null -> localNamingStrategy.toColumnName(column.entityProperty.simpleName.asString())
                    else -> column.entityProperty.simpleName.asString()
                }

                PropertySpec
                    .builder(column.tablePropertyName!!, columnType)
                    .addKdoc("Column %L. %L", columnName, column.entityProperty.docString?.trimIndent().orEmpty())
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
