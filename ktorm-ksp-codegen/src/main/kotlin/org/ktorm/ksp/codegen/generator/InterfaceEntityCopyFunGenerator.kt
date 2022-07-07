/*
 *  Copyright 2018-2021 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.ktorm.ksp.codegen.generator

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.buildCodeBlock
import org.ktorm.ksp.codegen.TableGenerateContext
import org.ktorm.ksp.codegen.TopLevelFunctionGenerator
import org.ktorm.ksp.codegen.definition.ColumnDefinition
import org.ktorm.ksp.codegen.definition.KtormEntityType
import org.ktorm.ksp.codegen.definition.TableDefinition
import org.ktorm.ksp.codegen.generator.util.MemberNames
import org.ktorm.ksp.codegen.generator.util.primitiveTypes

public class InterfaceEntityCopyFunGenerator : TopLevelFunctionGenerator {

    override fun generate(context: TableGenerateContext, emitter: (FunSpec) -> Unit) {
        val table = context.table
        if (table.ktormEntityType != KtormEntityType.ENTITY_INTERFACE) {
            return
        }
        FunSpec.builder("copy")
            .returns(table.entityClassName)
            .receiver(table.entityClassName)
            .addParameters(buildParameters(table))
            .addCode(buildCodeBlock {
                val format = buildString {
                    append("return·%L(")
                    append(
                        table.columns
                            .filter { it.isMutable }
                            .joinToString(", ") {
                                val propertyName = it.entityPropertyName.simpleName
                                "$propertyName·=·$propertyName"
                            }
                    )
                    append(")")
                }
                addStatement(format, table.entityClassName.simpleName)
            })
            .build()
            .run(emitter)
    }

    private fun buildParameters(table: TableDefinition): List<ParameterSpec> {
        return table.columns
            .filter { it.isMutable }
            .map {
                MemberNames
                ParameterSpec.builder(it.entityPropertyName.simpleName, it.parameterType())
                    .defaultValue(
                        "%M(this, %T.%L.binding!!)",
                        MemberNames.getValueOrUndefined,
                        table.tableClassName,
                        it.tablePropertyName.simpleName
                    )
                    .build()
            }
    }

    private fun ColumnDefinition.parameterType(): TypeName {
        val type = this.propertyClassName
        return if (type in primitiveTypes) {
            type.copy(nullable = true)
        } else {
            type.copy(this.isNullable)
        }
    }
}