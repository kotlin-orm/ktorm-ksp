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

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.toClassName
import org.ktorm.entity.EntitySequence
import org.ktorm.expression.ColumnAssignmentExpression
import org.ktorm.expression.UpdateExpression
import org.ktorm.ksp.compiler.util.*
import org.ktorm.ksp.spi.TableMetadata

@OptIn(KotlinPoetKspPreview::class)
object UpdateFunctionGenerator {

    fun generate(table: TableMetadata): FunSpec {
        val kdoc = "" +
            "Update the given entity to the database and return the affected record number. " +
            "If [isDynamic] is set to true, the generated SQL will include only the non-null columns. "

        val entityClass = table.entityClass.toClassName()
        val tableClass = ClassName(table.entityClass.packageName.asString(), table.tableClassName)

        return FunSpec.builder("update")
            .addKdoc(kdoc)
            .addAnnotation(AnnotationSpec.builder(Suppress::class).addMember("%S", "UNCHECKED_CAST").build())
            .receiver(EntitySequence::class.asClassName().parameterizedBy(entityClass, tableClass))
            .addParameter("entity", entityClass)
            .addParameter(ParameterSpec.builder("isDynamic", typeNameOf<Boolean>()).defaultValue("false").build())
            .returns(Int::class.asClassName())
            .addCode(AddFunctionGenerator.checkForDml())
            .addCode(AddFunctionGenerator.addAssignmentFun())
            .addCode(addAssignments(table))
            .addCode(buildConditions(table))
            .addCode(createExpression())
            .addStatement("return database.executeUpdate(expression)")
            .build()
    }

    private fun addAssignments(table: TableMetadata): CodeBlock {
        return buildCodeBlock {
            addStatement(
                "val assignments = %T<%T<*>>()",
                ArrayList::class.asClassName(),
                ColumnAssignmentExpression::class.asClassName()
            )

            for (column in table.columns) {
                if (column.isPrimaryKey) {
                    continue
                }

                addStatement(
                    "addAssignment(sourceTable.%N, entity.%N, isDynamic, assignments)",
                    column.columnPropertyName,
                    column.entityProperty.simpleName.asString(),
                )
            }

            add("\n")

            beginControlFlow("if (assignments.isEmpty())")
            addStatement("return 0")
            endControlFlow()

            add("\n")
        }
    }

    private fun buildConditions(table: TableMetadata): CodeBlock {
        return buildCodeBlock {
            val primaryKeys = table.columns.filter { it.isPrimaryKey }

            if (primaryKeys.size == 1) {
                val pk = primaryKeys[0]
                if (pk.entityProperty.type.resolve().isMarkedNullable) {
                    addStatement(
                        "val conditions = sourceTable.%N·%M·entity.%N!!",
                        pk.columnPropertyName,
                        MemberName("org.ktorm.dsl", "eq", true),
                        pk.entityProperty.simpleName.asString()
                    )
                } else {
                    addStatement(
                        "val conditions = sourceTable.%N·%M·entity.%N",
                        pk.columnPropertyName,
                        MemberName("org.ktorm.dsl", "eq", true),
                        pk.entityProperty.simpleName.asString()
                    )
                }
            } else {
                add("«val conditions = ")

                for ((i, pk) in primaryKeys.withIndex()) {
                    if (pk.entityProperty.type.resolve().isMarkedNullable) {
                        add(
                            "(sourceTable.%N·%M·entity.%N!!)",
                            pk.columnPropertyName,
                            MemberName("org.ktorm.dsl", "eq", true),
                            pk.entityProperty.simpleName.asString()
                        )
                    } else {
                        add(
                            "(sourceTable.%N·%M·entity.%N)",
                            pk.columnPropertyName,
                            MemberName("org.ktorm.dsl", "eq", true),
                            pk.entityProperty.simpleName.asString()
                        )
                    }

                    if (i != primaryKeys.lastIndex) {
                        add("·%M·", MemberName("org.ktorm.dsl", "and", true))
                    }
                }

                add("\n»")
            }
        }
    }

    private fun createExpression(): CodeBlock {
        val code = """
            val expression = // AliasRemover.visit(
                %T(table = sourceTable.asExpression(), assignments = assignments, where = conditions)
            // )
            
              
        """.trimIndent()

        return CodeBlock.of(code, UpdateExpression::class.asClassName())
    }
}
