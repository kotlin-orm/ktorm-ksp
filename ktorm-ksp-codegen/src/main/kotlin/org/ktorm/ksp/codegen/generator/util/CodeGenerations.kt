/*
 *  Copyright 2018-2021 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.ktorm.ksp.codegen.generator.util

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.asClassName
import org.ktorm.entity.Entity
import org.ktorm.expression.*
import kotlin.reflect.KParameter

public val primitiveTypes: List<ClassName> = listOf(
    Byte::class.asClassName(),
    Int::class.asClassName(),
    Short::class.asClassName(),
    Long::class.asClassName(),
    Char::class.asClassName(),
    Boolean::class.asClassName(),
    Float::class.asClassName(),
    Double::class.asClassName()
)

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
    public val entityImplementation: ClassName = ClassName("org.ktorm.entity", "EntityImplementation")
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
