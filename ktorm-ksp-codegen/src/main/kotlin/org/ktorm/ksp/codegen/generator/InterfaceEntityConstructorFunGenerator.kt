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

import com.squareup.kotlinpoet.*
import org.ktorm.ksp.codegen.TableGenerateContext
import org.ktorm.ksp.codegen.TopLevelFunctionGenerator
import org.ktorm.ksp.codegen.definition.ColumnDefinition
import org.ktorm.ksp.codegen.definition.KtormEntityType
import org.ktorm.ksp.codegen.generator.util.ClassNames
import org.ktorm.ksp.codegen.generator.util.MemberNames
import org.ktorm.ksp.codegen.generator.util.primitiveTypes
import org.ktorm.ksp.codegen.generator.util.withControlFlow

public class InterfaceEntityConstructorFunGenerator : TopLevelFunctionGenerator {

    override fun generate(context: TableGenerateContext, emitter: (FunSpec) -> Unit) {
        val table = context.table
        if (table.ktormEntityType != KtormEntityType.ENTITY_INTERFACE) {
            return
        }
        FunSpec.builder(table.entityClassName.simpleName)
            .addAnnotation(
                AnnotationSpec.builder(ClassNames.suppress)
                    .addMember("\"FunctionName\"")
                    .build()
            )
            .returns(table.entityClassName)
            .addParameters(buildParameters(context))
            .addCode(buildCodeBlock {
                addStatement("val·entity·=·%T.create<%T>()", ClassNames.entity, table.entityClassName)
                table.columns
                    .filter { it.isMutable }
                    .forEach { column ->
                        val propertyName = column.entityPropertyName.simpleName
                        withControlFlow(
                            "if·(%L·!==·%M<%T>())",
                            arrayOf(propertyName, MemberNames.undefined, column.parameterType())
                        ) {
                            if (!column.isNullable && column.propertyClassName in primitiveTypes) {
                                addStatement("entity.%1L·=·%1L·?:·error(\"`%1L` should not be null.\")", propertyName)
                            } else {
                                addStatement("entity.%1L·=·%1L", propertyName)
                            }
                        }
                    }
                addStatement("return entity")
            })
            .build()
            .apply(emitter)
    }

    private fun buildParameters(context: TableGenerateContext): List<ParameterSpec> {
        return context.table.columns
            .filter { it.isMutable }
            .map {
                ParameterSpec.builder(it.entityPropertyName.simpleName, it.parameterType())
                    .defaultValue("%M()", MemberNames.undefined)
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