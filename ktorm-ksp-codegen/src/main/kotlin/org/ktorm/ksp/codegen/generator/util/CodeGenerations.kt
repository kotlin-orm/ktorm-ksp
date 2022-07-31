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

package org.ktorm.ksp.codegen.generator.util

import com.squareup.kotlinpoet.*
import org.ktorm.entity.Entity
import org.ktorm.expression.*
import org.ktorm.ksp.codegen.TableGenerateContext
import kotlin.reflect.KClass
import kotlin.reflect.KParameter

public object MemberNames {
    public val update: MemberName = MemberName("org.ktorm.dsl", "update", true)
    public val eq: MemberName = MemberName("org.ktorm.dsl", "eq", true)
    public val and: MemberName = MemberName("org.ktorm.dsl", "and", true)
    public val checkNotModified: MemberName =
        MemberName("org.ktorm.ksp.api.EntitySequenceUtil", "checkIfSequenceModified", false)
    public val primaryConstructor: MemberName = MemberName("kotlin.reflect.full", "primaryConstructor", true)
    public val emptyMap: MemberName = MemberName("kotlin.collections", "emptyMap")

    public val bindTo: MemberName = MemberName("", "bindTo")
    public val primaryKey: MemberName = MemberName("", "primaryKey")
    public val references: MemberName = MemberName("", "references")
    public val undefined: MemberName = MemberName("org.ktorm.ksp.api", "undefined")
}

public object ClassNames {
    public val columnAssignmentExpression: ClassName = ColumnAssignmentExpression::class.asClassName()
    public val columnExpression: ClassName = ColumnExpression::class.asClassName()
    public val argumentExpression: ClassName = ArgumentExpression::class.asClassName()
    public val tableExpression: ClassName = TableExpression::class.asClassName()
    public val insertExpression: ClassName = InsertExpression::class.asClassName()
    public val hashMap: ClassName = HashMap::class.asClassName()
    public val kParameter: ClassName = KParameter::class.asClassName()
    public val any: ClassName = Any::class.asClassName()
    public val suppress: ClassName = Suppress::class.asClassName()
    public val entity: ClassName = Entity::class.asClassName()
    public val kClass: ClassName = KClass::class.asClassName()
}

public object SuppressAnnotations {
    public const val localVariableName: String = "\"LocalVariableName\""
    public const val functionName: String = "\"FunctionName\""
    public const val unusedParameter: String = "\"UNUSED_PARAMETER\""
    public const val uncheckedCast: String = "\"UNCHECKED_CAST\""
    public const val leakingThis: String = "\"LeakingThis\""

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
    public fun buildEntityAssignCode(context: TableGenerateContext, entityVar: String): CodeBlock {
        val table = context.table
        return buildCodeBlock {
            table.columns
                .forEach { column ->
                    val propertyName = column.entityPropertyName.simpleName

                    val condition: String
                    if (column.isInlinePropertyType) {
                        condition = "if·((%L·as·Any?)·!==·(%M<%T>()·as·Any?))"
                    } else {
                        condition = "if·(%L·!==·%M<%T>())"
                    }

                    withControlFlow(condition, arrayOf(propertyName, MemberNames.undefined, column.nonNullPropertyTypeName)) {
                        if (!column.isNullable) {
                            addStatement(
                                "%1L[%2S]·=·%2L·?:·error(\"`%2L` should not be null.\")",
                                entityVar,
                                propertyName
                            )
                        } else {
                            addStatement("%1L[%2S]·=·%2L", entityVar, propertyName)
                        }
                    }
                }
            addStatement("return %L", entityVar)
        }
    }

    public fun buildEntityConstructorParameters(
        context: TableGenerateContext,
        nameAllocator: NameAllocator
    ): List<ParameterSpec> {
        return context.table.columns
            .map {
                ParameterSpec.builder(
                    nameAllocator.newName(it.entityPropertyName.simpleName),
                    it.nonNullPropertyTypeName.copy(nullable = true)
                )
                    .defaultValue("%M()", MemberNames.undefined)
                    .build()
            }
    }

    public fun convertDefaultImplementationFunName(functionName: String): String {
        return "$${functionName}\$implementation"
    }
}
