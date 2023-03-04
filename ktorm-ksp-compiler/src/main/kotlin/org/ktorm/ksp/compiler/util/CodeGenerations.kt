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

package org.ktorm.ksp.compiler.util

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.toTypeName
import org.ktorm.entity.Entity
import org.ktorm.expression.*
import org.ktorm.ksp.api.Undefined
import org.ktorm.ksp.spi.TableGenerateContext
import org.ktorm.schema.Column
import org.ktorm.schema.SqlType
import kotlin.reflect.KClass
import kotlin.reflect.KParameter

public object MemberNames {
    public val update: MemberName = MemberName("org.ktorm.dsl", "update", true)
    public val eq: MemberName = MemberName("org.ktorm.dsl", "eq", true)
    public val and: MemberName = MemberName("org.ktorm.dsl", "and", true)
    public val primaryConstructor: MemberName = MemberName("kotlin.reflect.full", "primaryConstructor", true)
    public val emptyMap: MemberName = MemberName("kotlin.collections", "emptyMap")
    public val bindTo: MemberName = MemberName("", "bindTo")
    public val primaryKey: MemberName = MemberName("", "primaryKey")
    public val references: MemberName = MemberName("", "references")
}

public object ClassNames {
    public val columnAssignmentExpression: ClassName = ColumnAssignmentExpression::class.asClassName()
    public val columnExpression: ClassName = ColumnExpression::class.asClassName()
    public val argumentExpression: ClassName = ArgumentExpression::class.asClassName()
    public val tableExpression: ClassName = TableExpression::class.asClassName()
    public val insertExpression: ClassName = InsertExpression::class.asClassName()
    public val updateExpression: ClassName = UpdateExpression::class.asClassName()
    public val hashMap: ClassName = HashMap::class.asClassName()
    public val kParameter: ClassName = KParameter::class.asClassName()
    public val any: ClassName = Any::class.asClassName()
    public val column: ClassName = Column::class.asClassName()
    public val suppress: ClassName = Suppress::class.asClassName()
    public val entity: ClassName = Entity::class.asClassName()
    public val kClass: ClassName = KClass::class.asClassName()
    public val undefined: ClassName = Undefined::class.asClassName()
    public val sqlType: ClassName = SqlType::class.asClassName()
}

public object SuppressAnnotations {
    public const val functionName: String = "\"FunctionName\""
    public const val uncheckedCast: String = "\"UNCHECKED_CAST\""

    public fun buildSuppress(vararg names: String): AnnotationSpec {
        return AnnotationSpec.builder(Suppress::class).addMember(names.joinToString(", ")).build()
    }
}

public inline fun CodeBlock.Builder.withControlFlow(
    controlFlow: String,
    args: Array<Any?> = emptyArray(),
    block: CodeBlock.Builder.() -> Unit
): CodeBlock.Builder = apply {
    beginControlFlow(controlFlow, *args)
    block(this)
    endControlFlow()
}

@OptIn(KotlinPoetKspPreview::class)
public object CodeFactory {

    /**
     * generate code:
     * ```kotlin
     * if (id !== undefined<Int?>()) {
     *     entity.id = id
     * }
     * // other property ...
     * return entity
     * ```
     */
    public fun buildEntityAssignCode(
        context: TableGenerateContext,
        entityVar: String,
        nameAllocator: NameAllocator
    ): CodeBlock {
        return buildCodeBlock {
            for (column in context.table.columns) {
                val propertyName = column.entityProperty.simpleName
                val propertyParameterName = nameAllocator[propertyName]
                val propertyType = column.entityProperty.type.resolve()

                val condition: String
                if (propertyType.isInline()) {
                    condition = "if·((%N·as·Any?)·!==·(%T.of<%T>()·as·Any?))"
                } else {
                    condition = "if·(%N·!==·%T.of<%T>())"
                }

                withControlFlow(
                    condition,
                    arrayOf(propertyParameterName, ClassNames.undefined, propertyType.toTypeName().copy(nullable = false))
                ) {
                    var statement: String
                    if (column.entityProperty.isMutable) {
                        statement = "%1N.%2N·=·%3N"
                    } else {
                        statement = "%1N[%2S]·=·%3N"
                    }

                    if (!propertyType.isMarkedNullable) {
                        statement += "·?:·error(\"`%2L` should not be null.\")"
                    }

                    addStatement(statement, entityVar, propertyName, propertyParameterName)
                }
            }
            addStatement("return %L", entityVar)
        }
    }

    public fun convertDefaultImplementationFunName(functionName: String): String {
        return "$${functionName}\$implementation"
    }

    public fun buildCheckDmlCode(): CodeBlock {
        return CodeBlock.of(
            """
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
        )
    }

    public fun buildAddAssignmentCode(): CodeBlock {
        return CodeBlock.of(
            """
            fun <T : %1T> addAssignment(
                column: %2T<T>,
                value: T?,
                isDynamic: Boolean,
                assignments: MutableList<%3T<*>>
            ) {
                if (isDynamic && value == null) {
                    return
                }
                val expression = %3T(
                    column = column.asExpression(),
                    expression = %4T(value, column.sqlType)
                )
                assignments.add(expression)
            }
            
            
            """.trimIndent(),
            ClassNames.any,
            ClassNames.column,
            ClassNames.columnAssignmentExpression,
            ClassNames.argumentExpression,
        )
    }
}
