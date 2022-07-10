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

package org.ktorm.ksp.ext

import com.squareup.kotlinpoet.*
import org.ktorm.ksp.codegen.TableGenerateContext
import org.ktorm.ksp.codegen.generator.util.MemberNames
import org.ktorm.ksp.codegen.generator.util.withControlFlow
import kotlin.math.ceil

public object InterfaceEntityEnhanceCodes {

    /**
     * generate code:
     * ```kotlin
     * if (flag and 1 == 0) {
     *     entity.id = id
     * }
     * // other property ...
     * return entity
     * ```
     */
    public fun buildEntityAssignCode(
        context: TableGenerateContext,
        nameAllocator: NameAllocator,
        entityVar: String,
        flagVars: List<String>
    ): CodeBlock {
        val table = context.table
        return buildCodeBlock {
            table.columns
                .filter { it.isMutable }
                .forEachIndexed { index, column ->
                    val propertyName = column.entityPropertyName.simpleName
                    val bit = 1 shl (index % 32)
                    val flagVar = flagVars[index / 32]
                    withControlFlow("if·(%L·and·%L·!=·0)", arrayOf(flagVar, bit)) {
                        val notNullOperator = if (column.isNullable) "" else "!!"
                        addStatement(
                            "%L.%L·=·%L%L",
                            entityVar,
                            propertyName,
                            nameAllocator[propertyName],
                            notNullOperator
                        )
                    }
                }
            addStatement("return %L", entityVar)
        }
    }

    public fun buildVirtualConstructorParameters(
        context: TableGenerateContext,
        nameAllocator: NameAllocator
    ): List<ParameterSpec> {
        return context.table.columns
            .filter { it.isMutable }
            .map {
                ParameterSpec.builder(
                    nameAllocator.newName(it.entityPropertyName.simpleName),
                    it.propertyClassName.copy(nullable = it.isNullable)
                )
                    .defaultValue("%M()", MemberNames.undefined)
                    .build()
            }
    }

    public fun buildImplementationConstructorParameters(
        context: TableGenerateContext,
        nameAllocator: NameAllocator,
        flagVars: MutableList<String>
    ): List<ParameterSpec> {
        val parameters = context.table.columns
            .filter { it.isMutable }
            .map {
                ParameterSpec.builder(
                    nameAllocator.newName(it.entityPropertyName.simpleName, it.entityPropertyName.simpleName),
                    it.propertyClassName.copy(nullable = true)
                ).build()
            }
            .toMutableList()
        val flagCount = ceil(parameters.size / 32.0).toInt()
        for (i in 1..flagCount) {
            val flagVar = if (flagCount == 1) {
                "flag"
            } else {
                "flag$i"
            }
            flagVars.add(flagVar)
            parameters.add(ParameterSpec.builder(flagVar, Int::class.asClassName()).build())
        }
        return parameters
    }

    public fun toImplementationFunName(functionName: String): String {
        return "$${functionName}\$implementation"
    }
}