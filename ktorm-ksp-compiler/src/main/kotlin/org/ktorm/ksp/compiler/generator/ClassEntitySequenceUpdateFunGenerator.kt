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

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.ktorm.entity.EntitySequence
import org.ktorm.expression.ColumnAssignmentExpression
import org.ktorm.ksp.compiler.generator.util.*
import org.ktorm.ksp.spi.TableGenerateContext
import org.ktorm.ksp.spi.TopLevelFunctionGenerator
import org.ktorm.ksp.spi.definition.ColumnDefinition
import org.ktorm.ksp.spi.definition.KtormEntityType
import org.ktorm.ksp.spi.definition.TableDefinition

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
    override fun generate(context: TableGenerateContext): List<FunSpec> {
        val table = context.table
        if (table.ktormEntityType != KtormEntityType.ANY_KIND_CLASS) {
            return emptyList()
        }

        val primaryKeys = table.columns.filter { it.isPrimaryKey }
        if (primaryKeys.isEmpty()) {
            return emptyList()
        }

        val kdoc = "" +
                "Update the given entity to the database and return the affected record number. " +
                "If [isDynamic] is set to true, the generated SQL will include only the non-null columns. "

        val funSpec = FunSpec.builder("update")
            .receiver(EntitySequence::class.asClassName().parameterizedBy(table.entityClassName, table.tableClassName))
            .addParameter("entity", table.entityClassName)
            .addParameter(ParameterSpec.builder("isDynamic", typeNameOf<Boolean>()).defaultValue("false").build())
            .returns(Int::class.asClassName())
            .addAnnotation(SuppressAnnotations.buildSuppress(SuppressAnnotations.uncheckedCast))
            .addKdoc(kdoc)
            .addCode(CodeFactory.buildCheckDmlCode())
            .addCode(CodeFactory.buildAddAssignmentCode())
            .addCode(buildAssignmentsCode(table))
            .addCode(buildConditionsCode(primaryKeys))
            .addCode(buildExpressionCode())
            .addStatement("return database.executeUpdate(expression)")
            .build()
        return listOf(funSpec)
    }

    private fun buildAssignmentsCode(table: TableDefinition): CodeBlock {
        return buildCodeBlock {
            val targetColumns = table.columns.filter { !it.isPrimaryKey }

            addStatement(
                "val assignments = %T<%T<*>>(%L)",
                ArrayList::class.asClassName(),
                ColumnAssignmentExpression::class.asClassName(),
                targetColumns.size
            )

            for (column in targetColumns) {
                addStatement(
                    "addAssignment(sourceTable.%N, entity.%N, isDynamic, assignments)",
                    column.tablePropertyName.simpleName,
                    column.entityPropertyName.simpleName,
                )
            }
            add("\n")

            withControlFlow("if (assignments.isEmpty())") {
                addStatement("return 0")
            }

            add("\n")
        }
    }

    private fun buildConditionsCode(primaryKeys: List<ColumnDefinition>): CodeBlock {
        return buildCodeBlock {
            if (primaryKeys.size == 1) {
                val pk = primaryKeys[0]
                if (pk.isNullable) {
                    addStatement(
                        "val conditions = sourceTable.%N·%M·entity.%N!!",
                        pk.tablePropertyName.simpleName, MemberNames.eq, pk.entityPropertyName.simpleName
                    )
                } else {
                    addStatement(
                        "val conditions = sourceTable.%N·%M·entity.%N",
                        pk.tablePropertyName.simpleName, MemberNames.eq, pk.entityPropertyName.simpleName
                    )
                }
            } else {
                add("«val conditions = ")

                for ((i, pk) in primaryKeys.withIndex()) {
                    if (pk.isNullable) {
                        add(
                            "(sourceTable.%N·%M·entity.%N!!)",
                            pk.tablePropertyName.simpleName, MemberNames.eq, pk.entityPropertyName.simpleName
                        )
                    } else {
                        add(
                            "(sourceTable.%N·%M·entity.%N)",
                            pk.tablePropertyName.simpleName, MemberNames.eq, pk.entityPropertyName.simpleName
                        )
                    }

                    if (i != primaryKeys.lastIndex) {
                        add("·%M·", MemberNames.and)
                    }
                }

                add("\n»")
            }
        }
    }

    private fun buildExpressionCode(): CodeBlock {
        return CodeBlock.of(
            """
            val expression = // AliasRemover.visit(
                %T(table = sourceTable.asExpression(), assignments = assignments, where = conditions)
            // )
            
              
            """.trimIndent(),
            ClassNames.updateExpression
        )
    }
}
