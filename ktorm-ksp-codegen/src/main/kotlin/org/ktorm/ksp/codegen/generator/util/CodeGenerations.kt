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

package org.ktorm.ksp.codegen.generator.util

import com.squareup.kotlinpoet.*
import org.ktorm.entity.Entity
import org.ktorm.expression.*
import org.ktorm.ksp.codegen.TableGenerateContext
import org.ktorm.ksp.codegen.definition.ColumnDefinition
import kotlin.reflect.KClass
import kotlin.reflect.KParameter

public val primitiveTypes: List<TypeName> = listOf(
    Byte::class.asTypeName(),
    Int::class.asTypeName(),
    Short::class.asTypeName(),
    Long::class.asTypeName(),
    Char::class.asTypeName(),
    Boolean::class.asTypeName(),
    Float::class.asTypeName(),
    Double::class.asTypeName()
)

public object MemberNames {
    public val update: MemberName = MemberName("org.ktorm.dsl", "update", true)
    public val eq: MemberName = MemberName("org.ktorm.dsl", "eq", true)
    public val and: MemberName = MemberName("org.ktorm.dsl", "and", true)
    public val primaryConstructor: MemberName = MemberName("kotlin.reflect.full", "primaryConstructor", true)
    public val emptyMap: MemberName = MemberName("kotlin.collections", "emptyMap")

    public val bindTo: MemberName = MemberName("", "bindTo")
    public val primaryKey: MemberName = MemberName("", "primaryKey")
    public val references: MemberName = MemberName("", "references")
    public val undefined: MemberName = MemberName("org.ktorm.ksp.api.EntityUtil", "undefined")
    public val getValueOrUndefined: MemberName = MemberName("org.ktorm.ksp.api.EntityUtil", "getValueOrUndefined")
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
    public const val functionName: String = "\"FunctionName\""

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
                .filter { it.isMutable }
                .forEach { column ->
                    val propertyName = column.entityPropertyName.simpleName
                    withControlFlow(
                        "if·(%L·!==·%M<%T>())",
                        arrayOf(propertyName, MemberNames.undefined, column.constructorParameterType())
                    ) {
                        if (!column.isNullable && column.nonNullPropertyTypeName in primitiveTypes) {
                            addStatement(
                                "%1L.%2L·=·%2L·?:·error(\"`%1L` should not be null.\")",
                                entityVar,
                                propertyName
                            )
                        } else {
                            addStatement("%1L.%2L·=·%2L", entityVar, propertyName)
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
            .filter { it.isMutable }
            .map {
                ParameterSpec.builder(
                    nameAllocator.newName(it.entityPropertyName.simpleName),
                    it.constructorParameterType()
                )
                    .defaultValue("%M()", MemberNames.undefined)
                    .build()
            }
    }

    private fun ColumnDefinition.constructorParameterType(): TypeName {
        return if (nonNullPropertyTypeName in primitiveTypes) {
            nonNullPropertyTypeName.copy(nullable = true)
        } else {
            propertyTypeName
        }
    }

    public fun convertDefaultImplementationFunName(functionName: String): String {
        return "$${functionName}\$implementation"
    }
}
