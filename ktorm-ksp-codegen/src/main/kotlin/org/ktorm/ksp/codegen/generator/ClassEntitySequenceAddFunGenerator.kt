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
        if (context.table.ktormEntityType != KtormEntityType.ANY_KIND_CLASS) {
            return
        }
        val table = context.table
        val kdocBuilder = StringBuilder("Insert entity into database")
        FunSpec.builder("add")
            .receiver(EntitySequence::class.asClassName().parameterizedBy(table.entityClassName, table.tableClassName))
            .addParameter("entity", table.entityClassName)
            .returns(Int::class.asClassName())
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

                addStatement("val assignments = ArrayList<ColumnAssignmentExpression<*>>(%L)", table.columns.size)
                for (column in table.columns) {
                    if (column.isNullable) {
                        beginControlFlow("if (entity.%L != null)", column.entityPropertyName.simpleName)
                    }
                    val params = mapOf(
                        "columnAssignmentExpr" to ClassNames.columnAssignmentExpression,
                        "columnExpr" to ClassNames.columnExpression,
                        "argumentExpr" to ClassNames.argumentExpression,
                        "table" to table.tableClassName,
                        "entityProperty" to column.entityPropertyName.simpleName,
                        "tableProperty" to column.tablePropertyName.simpleName
                    )
                    addNamed(
                        """
                            assignments += %columnAssignmentExpr:T(
                                column = %columnExpr:T(null, %table:T.%tableProperty:L.name, %table:T.%tableProperty:L.sqlType),
                                expression = %argumentExpr:T(entity.%entityProperty:L, %table:T.%tableProperty:L.sqlType)
                            )
                            
                        """.trimIndent(),
                        params
                    )
                    if (column.isNullable) {
                        endControlFlow()
                    }
                }
                val params = mapOf(
                    "insertExpr" to ClassNames.insertExpression,
                    "tableExpr" to ClassNames.tableExpression,
                    "table" to table.tableClassName,
                )
                addNamed(
                    """
                        val expression = %insertExpr:T(
                            table = %tableExpr:T(%table:T.tableName, null, %table:T.catalog, %table:T.schema),
                            assignments = assignments
                        )
                        
                    """.trimIndent(), params
                )
                val primaryKeys = table.columns.filter { it.isPrimaryKey }
                if (primaryKeys.size == 1 && primaryKeys.first().isMutable) {
                    val primaryKey = primaryKeys.first()
                    kdocBuilder.append(
                        ", And try to get the auto-incrementing primary key and assign it to the " +
                                "${primaryKey.entityPropertyName.simpleName} property"
                    )
                    if (primaryKey.isNullable) {
                        beginControlFlow("if (entity.%L == null)", primaryKey.entityPropertyName.simpleName)
                    }
                    add(
                        """
                        val (effects, rowSet) = database.executeUpdateAndRetrieveKeys(expression)
                        if (rowSet.next()) {
                            val generatedKey = %T.%L.sqlType.getResult(rowSet, 1)
                            if (generatedKey != null) {
                                if (database.logger.isDebugEnabled()) {
                                    database.logger.debug("Generated Key: ${'$'}generatedKey")
                                }
                                entity.%L = generatedKey
                            }
                        }
                        return effects
                        
                    """.trimIndent(),
                        table.tableClassName,
                        primaryKey.tablePropertyName.simpleName,
                        primaryKey.entityPropertyName.simpleName
                    )
                    if (primaryKey.isNullable) {
                        endControlFlow()
                        addStatement("return database.executeUpdate(expression)")
                    }
                } else {
                    addStatement("return database.executeUpdate(expression)")
                }
                kdocBuilder.appendLine()
                kdocBuilder.append("@return the effected row count.")
            })
            .addKdoc(kdocBuilder.toString())
            .build()
            .run(emitter)
    }
}
