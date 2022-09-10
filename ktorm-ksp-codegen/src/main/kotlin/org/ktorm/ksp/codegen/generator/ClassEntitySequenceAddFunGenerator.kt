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

package org.ktorm.ksp.codegen.generator

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.buildCodeBlock
import org.ktorm.entity.EntitySequence
import org.ktorm.ksp.codegen.TableGenerateContext
import org.ktorm.ksp.codegen.TopLevelFunctionGenerator
import org.ktorm.ksp.codegen.definition.KtormEntityType
import org.ktorm.ksp.codegen.generator.util.ClassNames
import org.ktorm.ksp.codegen.generator.util.SuppressAnnotations

/**
 * Generate add extend function to [EntitySequence].
 * e.g:
 * ```kotlin
 * public fun EntitySequence<Customer, Customers>.add(entity: Customer): Int {
 *      // Ignore code
 * }
 * ```
 */
public class ClassEntitySequenceAddFunGenerator : TopLevelFunctionGenerator {

    override fun generate(context: TableGenerateContext, emitter: (FunSpec) -> Unit) {
        val table = context.table
        if (table.ktormEntityType != KtormEntityType.ANY_KIND_CLASS) {
            return
        }

        val primaryKeys = table.columns.filter { it.isPrimaryKey }
        val useGeneratedKey = primaryKeys.size == 1 && primaryKeys[0].isMutable && primaryKeys[0].isNullable

        var kdoc = "Insert the given entity into this sequence and return the affected record number. "
        if (useGeneratedKey) {
            kdoc += "\n\n" +
                "Note that this function will obtain the generated key from the database and fill it into " +
                "the corresponding property after the insertion completes. But this requires us not to set " +
                "the primary key’s value beforehand, otherwise, if you do that, the given value will be " +
                "inserted into the database, and no keys generated."
        }

        FunSpec.builder("add")
            .receiver(EntitySequence::class.asClassName().parameterizedBy(table.entityClassName, table.tableClassName))
            .addParameter("entity", table.entityClassName)
            .returns(Int::class.asClassName())
            .addAnnotation(SuppressAnnotations.buildSuppress(SuppressAnnotations.uncheckedCast))
            .addKdoc(kdoc)
            .addCode(buildCodeBlock {
                add("""
                    val isModified = expression.where != null
                        || expression.groupBy.isNotEmpty()
                        || expression.having != null
                        || expression.isDistinct
                        || expression.orderBy.isNotEmpty()
                        || expression.offset != null
                        || expression.limit != null
                
                    if (isModified) {
                        val msg = "" +
                            "Entity manipulation functions are not supported by this sequence object. " +
                            "Please call on the origin sequence returned from database.sequenceOf(table)"
                        throw UnsupportedOperationException(msg)
                    }
                    
                    
                """.trimIndent())

                addStatement("val assignments = LinkedHashMap<Column<*>, Any?>()")

                for (column in table.columns) {
                    if (column.isNullable) {
                        addStatement("entity.%L?.let { assignments[sourceTable.%L] = it }",
                            column.entityPropertyName.simpleName, column.tablePropertyName.simpleName)
                    } else {
                        addStatement("entity.%L.let { assignments[sourceTable.%L] = it }",
                            column.entityPropertyName.simpleName, column.tablePropertyName.simpleName)
                    }
                }

                addNamed(
                    format = """
                        
                        val expression = // AliasRemover.visit(
                            %insertExpression:T(
                                table = sourceTable.asExpression(),
                                assignments = assignments.map { (col, argument) ->
                                    %columnAssignmentExpression:T(
                                        column = col.asExpression() as %columnExpression:T<Any>,
                                        expression = %argumentExpression:T(argument, col.sqlType as %sqlType:T<Any>)
                                    )
                                }
                            )
                        // )
                        
                        
                    """.trimIndent(),

                    arguments = mapOf(
                        "insertExpression" to ClassNames.insertExpression,
                        "columnAssignmentExpression" to ClassNames.columnAssignmentExpression,
                        "columnExpression" to ClassNames.columnExpression,
                        "argumentExpression" to ClassNames.argumentExpression,
                        "sqlType" to ClassNames.sqlType
                    )
                )

                if (!useGeneratedKey) {
                    addStatement("return database.executeUpdate(expression)")
                } else {
                    // If the primary key value is manually specified, not obtain the generated key.
                    beginControlFlow("if (entity.%L != null)", primaryKeys[0].entityPropertyName.simpleName)
                    addStatement("return database.executeUpdate(expression)")

                    // Else obtain the generated key value.
                    nextControlFlow("else")
                    addNamed(
                        format = """
                            val (effects, rowSet) = database.executeUpdateAndRetrieveKeys(expression)
                            if (rowSet.next()) {
                                val generatedKey = sourceTable.%columnName:L.sqlType.getResult(rowSet, 1)
                                if (generatedKey != null) {
                                    if (database.logger.isDebugEnabled()) {
                                        database.logger.debug("Generated Key: ${'$'}generatedKey")
                                    }
                                    
                                    entity.%propertyName:L = generatedKey
                                }
                            }
                            
                            return effects
                            
                        """.trimIndent(),

                        arguments = mapOf(
                            "columnName" to primaryKeys[0].tablePropertyName.simpleName,
                            "propertyName" to primaryKeys[0].entityPropertyName.simpleName
                        )
                    )

                    endControlFlow()
                }
            })
            .build()
            .run(emitter)
    }
}
