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
import org.ktorm.ksp.codegen.generator.util.MemberNames
import org.ktorm.ksp.codegen.generator.util.withControlFlow

public class SequenceAddAllFunctionGenerator : TopLevelFunctionGenerator {

    private val batchInsert = MemberName("org.ktorm.dsl", "batchInsert", true)

    override fun generate(context: TableGenerateContext): List<FunSpec> {
        val table = context.table
        val funSpec = FunSpec.builder("addAll")
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
                addStatement("%M(this)", MemberNames.checkNotModified)
                withControlFlow("return·this.database.%M(%T)", arrayOf(batchInsert, table.tableClassName)) {
                    withControlFlow("for (entity in entities)") {
                        withControlFlow("item") {
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
                        }
                    }
                }
            })
            .build()

        return listOf(funSpec)
    }
}
