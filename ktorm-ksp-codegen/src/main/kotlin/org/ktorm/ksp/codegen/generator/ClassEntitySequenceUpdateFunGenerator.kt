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

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.buildCodeBlock
import org.ktorm.entity.EntitySequence
import org.ktorm.ksp.codegen.TableGenerateContext
import org.ktorm.ksp.codegen.TopLevelFunctionGenerator
import org.ktorm.ksp.codegen.definition.KtormEntityType
import org.ktorm.ksp.codegen.generator.util.MemberNames
import org.ktorm.ksp.codegen.generator.util.withControlFlow

/**
 * Generate update extend function to [EntitySequence].
 * e.g:
 * ```kotlin
 * public fun EntitySequence<Customer, Customers>.update(entity: Customer): Int {
 *      // Ignore code
 * }
 * ```
 */
public class ClassEntitySequenceUpdateFunGenerator : TopLevelFunctionGenerator {

    /**
     * Generate entity sequence update function.
     */
    override fun generate(context: TableGenerateContext, emitter: (FunSpec) -> Unit) {
        if (context.table.ktormEntityType != KtormEntityType.ANY_KIND_CLASS) {
            return
        }
        val table = context.table
        val primaryKeyColumns = table.columns.filter { it.isPrimaryKey }
        if (primaryKeyColumns.isEmpty()) {
            context.logger.info(
                "skip the entity sequence update method of table " +
                        "${table.entityClassName} because it does not have a primary key column"
            )
            return
        }
        FunSpec.builder("update")
            .receiver(EntitySequence::class.asClassName().parameterizedBy(table.entityClassName, table.tableClassName))
            .addParameter("entity", table.entityClassName)
            .returns(Int::class.asClassName())
            .addKdoc(
                """
                Update entity by primary key
                @return the effected row count. 
            """.trimIndent()
            )
            .addCode(buildCodeBlock {
                addStatement("%M(this)", MemberNames.checkNotModified)
                withControlFlow("return·this.database.%M(%T)", arrayOf(MemberNames.update, table.tableClassName)) {
                    for (column in table.columns) {
                        if (!column.isPrimaryKey) {
                            addStatement(
                                "set(%T.%L,·entity.%L)",
                                column.tableDefinition.tableClassName,
                                column.tablePropertyName.simpleName,
                                column.entityPropertyName.simpleName
                            )
                        }
                    }
                    withControlFlow("where") {
                        primaryKeyColumns.forEachIndexed { index, column ->
                            if (index == 0) {
                                val conditionTemperate = if (primaryKeyColumns.size == 1) {
                                    "%T.%L·%M·entity.%L%L"
                                } else {
                                    "(%T.%L·%M·entity.%L%L)"
                                }
                                addStatement(
                                    conditionTemperate,
                                    column.tableDefinition.tableClassName,
                                    column.tablePropertyName.simpleName,
                                    MemberNames.eq,
                                    column.entityPropertyName.simpleName,
                                    if (column.isNullable) "!!" else ""
                                )
                            } else {
                                addStatement(
                                    ".%M(%T.%L·%M·entity.%L%L)",
                                    MemberNames.and,
                                    column.tableDefinition.tableClassName,
                                    column.tablePropertyName.simpleName,
                                    MemberNames.eq,
                                    column.entityPropertyName.simpleName,
                                    if (column.isNullable) "!!" else ""
                                )
                            }
                        }
                    }
                }
            })
            .build()
            .run(emitter)
    }
}
