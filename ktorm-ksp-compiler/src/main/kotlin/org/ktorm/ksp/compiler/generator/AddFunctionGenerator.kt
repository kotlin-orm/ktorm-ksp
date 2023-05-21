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
import org.ktorm.expression.ArgumentExpression
import org.ktorm.expression.ColumnAssignmentExpression
import org.ktorm.expression.InsertExpression
import org.ktorm.ksp.spi.ColumnMetadata
import org.ktorm.ksp.spi.TableMetadata
import org.ktorm.schema.Column

@OptIn(KotlinPoetKspPreview::class)
object AddFunctionGenerator {

    fun generate(table: TableMetadata): FunSpec {
        val primaryKeys = table.columns.filter { it.isPrimaryKey }
        val useGeneratedKey = primaryKeys.size == 1
            && primaryKeys[0].entityProperty.isMutable
            && primaryKeys[0].entityProperty.type.resolve().isMarkedNullable

        val entityClass = table.entityClass.toClassName()
        val tableClass = ClassName(table.entityClass.packageName.asString(), table.tableClassName)

        return FunSpec.builder("add")
            .addKdoc(kdoc(table, useGeneratedKey))
            .receiver(EntitySequence::class.asClassName().parameterizedBy(entityClass, tableClass))
            .addParameter("entity", entityClass)
            .addParameter(ParameterSpec.builder("isDynamic", typeNameOf<Boolean>()).defaultValue("false").build())
            .returns(Int::class.asClassName())
            .addCode(checkForDml())
            .addCode(addAssignmentFun())
            .addCode(addAssignments(table, useGeneratedKey))
            .addCode(createExpression())
            .addCode(executeUpdate(useGeneratedKey, primaryKeys))
            .build()
    }

    private fun kdoc(table: TableMetadata, useGeneratedKey: Boolean): String {
        var kdoc = "" +
            "Insert the given entity into this sequence and return the affected record number. " +
            "If [isDynamic] is set to true, the generated SQL will include only the non-null columns. "

        if (useGeneratedKey) {
            val pk = table.columns.single { it.isPrimaryKey }
            val pkName = table.entityClass.simpleName.asString() + "." + pk.entityProperty.simpleName.asString()

            kdoc += "\n\n" +
                "Note that this function will obtain the generated primary key from the database and fill it into " +
                "the property [${pkName}] after the insertion completes. But this requires us not to set " +
                "the primary key’s value beforehand, otherwise, if you do that, the given value will be " +
                "inserted into the database, and no keys generated."
        }

        return kdoc
    }

    internal fun checkForDml(): CodeBlock {
        val code = """
            val isModified =
                expression.where != null ||
                    expression.groupBy.isNotEmpty() ||
                    expression.having != null ||
                    expression.isDistinct ||
                    expression.orderBy.isNotEmpty() ||
                    expression.offset != null ||
                    expression.limit != null
        
            if (isModified) {
                val msg =
                    "Entity manipulation functions are not supported by this sequence object. " +
                    "Please call on the origin sequence returned from database.sequenceOf(table)"
                throw UnsupportedOperationException(msg)
            }
            
            
        """.trimIndent()

        return CodeBlock.of(code)
    }

    internal fun addAssignmentFun(): CodeBlock {
        val code = """
            fun <T : Any> addAssignment(
                column: %1T<T>,
                value: T?,
                isDynamic: Boolean,
                assignments: MutableList<%2T<*>>
            ) {
                if (isDynamic && value == null) {
                    return
                }
                val expression = %2T(
                    column = column.asExpression(),
                    expression = %3T(value, column.sqlType)
                )
                assignments.add(expression)
            }
            
            
        """.trimIndent()

        return CodeBlock.of(code,
            Column::class.asClassName(),
            ColumnAssignmentExpression::class.asClassName(),
            ArgumentExpression::class.asClassName(),
        )
    }

    private fun addAssignments(table: TableMetadata, useGeneratedKey: Boolean): CodeBlock {
        return buildCodeBlock {
            addStatement(
                "val assignments = %T<%T<*>>()",
                ArrayList::class.asClassName(),
                ColumnAssignmentExpression::class.asClassName()
            )

            for (column in table.columns) {
                val forceDynamic = useGeneratedKey && column.isPrimaryKey && column.entityProperty.type.resolve().isMarkedNullable
                addStatement(
                    "addAssignment(sourceTable.%N, entity.%N, %L, assignments)",
                    column.columnPropertyName,
                    column.entityProperty.simpleName.asString(),
                    if (forceDynamic) "true" else "isDynamic"
                )
            }

            add("\n")

            beginControlFlow("if (assignments.isEmpty())")
            addStatement("return 0")
            endControlFlow()

            add("\n")
        }
    }

    private fun createExpression(): CodeBlock {
        val code = """
            val expression = // AliasRemover.visit(
                %T(table = sourceTable.asExpression(), assignments = assignments)
            // )
            
              
        """.trimIndent()

        return CodeBlock.of(code, InsertExpression::class.asClassName())
    }

    private fun executeUpdate(useGeneratedKey: Boolean, primaryKeys: List<ColumnMetadata>): CodeBlock {
        return buildCodeBlock {
            if (!useGeneratedKey) {
                addStatement("return database.executeUpdate(expression)")
            } else {
                // If the primary key value is manually specified, not obtain the generated key.
                beginControlFlow("if (entity.%N != null)", primaryKeys[0].entityProperty.simpleName.asString())
                addStatement("return database.executeUpdate(expression)")

                // Else obtain the generated key value.
                nextControlFlow("else")
                addNamed(
                    format = """
                        val (effects, rowSet) = database.executeUpdateAndRetrieveKeys(expression)
                        if (rowSet.next()) {
                            val generatedKey = sourceTable.%columnName:N.sqlType.getResult(rowSet, 1)
                            if (generatedKey != null) {
                                if (database.logger.isDebugEnabled()) {
                                    database.logger.debug("Generated Key: ${'$'}generatedKey")
                                }
                                
                                entity.%propertyName:N = generatedKey
                            }
                        }
                        
                        return effects
                        
                    """.trimIndent(),

                    arguments = mapOf(
                        "columnName" to primaryKeys[0].columnPropertyName,
                        "propertyName" to primaryKeys[0].entityProperty.simpleName.asString()
                    )
                )

                endControlFlow()
            }
        }
    }
}
