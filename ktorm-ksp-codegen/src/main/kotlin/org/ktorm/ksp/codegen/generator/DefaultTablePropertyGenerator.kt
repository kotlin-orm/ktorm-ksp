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

package org.ktorm.ksp.codegen.generator

import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.buildCodeBlock
import org.ktorm.ksp.codegen.TableGenerateContext
import org.ktorm.ksp.codegen.TablePropertyGenerator
import org.ktorm.ksp.codegen.definition.KtormEntityType
import org.ktorm.ksp.codegen.generator.util.MemberNames
import org.ktorm.schema.Column

public open class DefaultTablePropertyGenerator : TablePropertyGenerator {

    override fun generate(context: TableGenerateContext, emitter: (PropertySpec) -> Unit) {
        when (context.table.ktormEntityType) {
            KtormEntityType.ENTITY_INTERFACE -> generateEntityInterfaceEntity(context, emitter)
            KtormEntityType.ANY_KIND_CLASS -> generateAnyKindClassEntity(context, emitter)
        }
    }

    protected open fun generateEntityInterfaceEntity(context: TableGenerateContext, emitter: (PropertySpec) -> Unit) {
        val (table, config, columnInitializerGenerator, _, dependencyFiles) = context
        table.columns
            .asSequence()
            .map { column ->
                val columnType = if (column.isReferences) {
                    Column::class.asClassName()
                        .parameterizedBy(column.referencesColumn!!.nonNullPropertyTypeName)
                } else {
                    Column::class.asClassName().parameterizedBy(column.nonNullPropertyTypeName)
                }
                PropertySpec.Companion.builder(
                    column.tablePropertyName.simpleName,
                    columnType
                )
                    .initializer(buildCodeBlock {
                        add(columnInitializerGenerator.generate(column, dependencyFiles, config))
                        val params = mutableMapOf(
                            "bindTo" to MemberNames.bindTo,
                            "references" to MemberNames.references,
                            "primaryKey" to MemberNames.primaryKey,
                            "referencesTable" to column.referencesColumn?.tableDefinition?.tableClassName,
                            "entityPropertyName" to column.entityPropertyName.simpleName
                        )
                        val code = buildString {
                            if (column.isReferences) {
                                append(".%references:M(%referencesTable:T)·{·it.%entityPropertyName:L·}·")
                            } else {
                                append(".%bindTo:M·{·it.%entityPropertyName:L·}")
                            }
                            if (column.isPrimaryKey) {
                                append(".%primaryKey:M()")
                            }
                        }
                        addNamed(code, params)
                    })
                    .build()
            }
            .forEach(emitter)
    }

    protected open fun generateAnyKindClassEntity(context: TableGenerateContext, emitter: (PropertySpec) -> Unit) {
        val (table, config, columnInitializerGenerator, _, dependencyFiles) = context
        table.columns
            .asSequence()
            .map { column ->
                PropertySpec.Companion.builder(
                    column.tablePropertyName.simpleName,
                    Column::class.asClassName().parameterizedBy(column.nonNullPropertyTypeName)
                )
                    .initializer(buildCodeBlock {
                        add(columnInitializerGenerator.generate(column, dependencyFiles, config))
                        if (column.isPrimaryKey) add(".%M()", MemberNames.primaryKey)
                    })
                    .build()
            }
            .forEach(emitter)
    }
}
