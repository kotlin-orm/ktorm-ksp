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

package org.ktorm.ksp.ext

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.buildCodeBlock
import org.ktorm.entity.EntitySequence
import org.ktorm.ksp.codegen.TableGenerateContext
import org.ktorm.ksp.codegen.TopLevelFunctionGenerator
import org.ktorm.ksp.codegen.generator.util.MemberNames
import org.ktorm.ksp.codegen.generator.util.withControlFlow

public class SequenceUpdateAllFunctionGenerator : TopLevelFunctionGenerator {

    private val batchUpdate = MemberName("org.ktorm.dsl", "batchUpdate", true)

    override fun generate(context: TableGenerateContext, emitter: (FunSpec) -> Unit) {
        val table = context.table
        val primaryKeyColumns = table.columns.filter { it.isPrimaryKey }
        if (primaryKeyColumns.isEmpty()) {
            context.logger.info(
                "skip the entity sequence updateAll method of table ${table.entityClassName} " +
                        "because it does not have a primary key column"
            )
            return
        }
        FunSpec.builder("updateAll")
            .addKdoc(
                """
                Batch update based on entity primary key
                @return the effected row counts for each sub-operation.
            """.trimIndent()
            )
            .receiver(EntitySequence::class.asClassName().parameterizedBy(table.entityClassName, table.tableClassName))
            .addParameter("entities", Iterable::class.asClassName().parameterizedBy(table.entityClassName))
            .returns(IntArray::class.asClassName())
            .addCode(buildCodeBlock {
                addStatement("%M(this)", MemberNames.checkNotModified)
                withControlFlow("return·this.database.%M(%T)", arrayOf(batchUpdate, table.tableClassName)) {
                    withControlFlow("for (entity in entities)") {
                        withControlFlow("item") {
                            for (column in table.columns) {
                                if (!column.isPrimaryKey) {
                                    if (column.isReferences) {
                                        val primaryKey = column.referencesColumn!!
                                        addStatement(
                                            "set(%T.%L,·entity.%L.%L)",
                                            table.tableClassName,
                                            column.tablePropertyName.simpleName,
                                            column.entityPropertyName.simpleName,
                                            primaryKey.entityPropertyName.simpleName
                                        )
                                    } else {
                                        addStatement(
                                            "set(%T.%L,·entity.%L)",
                                            table.tableClassName,
                                            column.tablePropertyName.simpleName,
                                            column.entityPropertyName.simpleName
                                        )
                                    }
                                }
                            }
                            withControlFlow("where") {
                                primaryKeyColumns.forEachIndexed { index, column ->
                                    if (index == 0) {
                                        val conditionTemperate = if (primaryKeyColumns.size == 1) {
                                            "it.%L·%M·entity.%L%L"
                                        } else {
                                            "(it.%L·%M·entity.%L%L)"
                                        }
                                        addStatement(
                                            conditionTemperate,
                                            column.tablePropertyName.simpleName,
                                            MemberNames.eq,
                                            column.entityPropertyName.simpleName,
                                            if (column.isNullable) "!!" else ""
                                        )
                                    } else {
                                        addStatement(
                                            ".%M(it.%L·%M·entity.%L%L)",
                                            MemberNames.and,
                                            column.tablePropertyName.simpleName,
                                            MemberNames.eq,
                                            column.entityPropertyName.simpleName,
                                            if (column.isNullable) "!!" else ""
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            })
            .build()
            .run(emitter)
    }
}
