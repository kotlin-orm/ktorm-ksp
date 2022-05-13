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

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.buildCodeBlock
import org.ktorm.entity.EntitySequence
import org.ktorm.ksp.codegen.TableGenerateContext
import org.ktorm.ksp.codegen.TopLevelFunctionGenerator

private val batchInsertFun = MemberName("org.ktorm.dsl", "batchInsert", true)
private val batchUpdateFun = MemberName("org.ktorm.dsl", "batchUpdate", true)
private val eqFun = MemberName("org.ktorm.dsl", "eq", true)
private val checkNotModifiedFun = MemberName("org.ktorm.ksp.api", "checkIfSequenceModified", true)

public class SequenceAddAllFunctionGenerator : TopLevelFunctionGenerator {
    override fun generate(context: TableGenerateContext, emitter: (FunSpec) -> Unit) {
        val table = context.table
        FunSpec.builder("addAll")
            .addKdoc(
                """
                Batch insert entities into the database, this method will not get the auto-incrementing primary key
                @return the effected row counts for each sub-operation.
            """.trimIndent()
            )
            .receiver(EntitySequence::class.asClassName().parameterizedBy(table.entityClassName, table.tableClassName))
            .addParameter("entities", Iterable::class.asClassName().parameterizedBy(table.entityClassName))
            .returns(IntArray::class.asClassName())
            .addCode(buildCodeBlock {
                addStatement("%M()", checkNotModifiedFun)
                beginControlFlow("return·this.database.%M(%T)", batchInsertFun, table.tableClassName)
                beginControlFlow("for (entity in entities)")
                beginControlFlow("item")
                for (column in table.columns) {
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
                endControlFlow()
                endControlFlow()
                endControlFlow()
            })
            .build()
            .run(emitter)
    }
}

private val andFun = MemberName("org.ktorm.dsl", "and", true)

public class SequenceUpdateAllFunctionGenerator : TopLevelFunctionGenerator {
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
                addStatement("%M()", checkNotModifiedFun)
                beginControlFlow("return·this.database.%M(%T)", batchUpdateFun, table.tableClassName)
                beginControlFlow("for (entity in entities)")
                beginControlFlow("item")
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
                beginControlFlow("where")
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
                            eqFun,
                            column.entityPropertyName.simpleName,
                            if (column.isNullable) "!!" else ""
                        )
                    } else {
                        addStatement(
                            ".%M(it.%L·%M·entity.%L%L)",
                            andFun,
                            column.tablePropertyName.simpleName,
                            eqFun,
                            column.entityPropertyName.simpleName,
                            if (column.isNullable) "!!" else ""
                        )
                    }
                }
                endControlFlow()
                endControlFlow()
                endControlFlow()
                endControlFlow()
            })
            .build()
            .run(emitter)
    }
}
