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
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import org.ktorm.entity.Entity
import org.ktorm.ksp.api.Undefined
import org.ktorm.ksp.compiler.util.*
import org.ktorm.ksp.spi.TableMetadata

@OptIn(KotlinPoetKspPreview::class)
object PseudoConstructorFunctionGenerator {

    fun generate(table: TableMetadata): FunSpec {
        return FunSpec.builder(table.entityClass.simpleName.asString())
            .addKdoc(
                "Create an entity of [%L] and specify the initial values for each property, " +
                "properties that doesn't have an initial value will leave unassigned. ",
                table.entityClass.simpleName.asString()
            )
            .addParameters(buildParameters(table))
            .returns(table.entityClass.toClassName())
            .addCode(buildFunctionBody(table))
            .build()
    }

    internal fun buildParameters(table: TableMetadata): List<ParameterSpec> {
        return table.columns.map { column ->
            val propName = column.entityProperty.simpleName.asString()
            val propType = column.entityProperty.type.resolve().makeNullable().toTypeName()

            ParameterSpec.builder(propName, propType)
                .defaultValue("%T.of()", Undefined::class.asClassName())
                .build()
        }
    }

    internal fun buildFunctionBody(table: TableMetadata, isCopy: Boolean = false): CodeBlock = buildCodeBlock {
        if (isCopy) {
            addStatement("val·entity·=·this.copy()")
        } else {
            addStatement("val·entity·=·%T.create<%T>()", Entity::class.asClassName(), table.entityClass.toClassName())
        }

        for (column in table.columns) {
            val propName = column.entityProperty.simpleName.asString()
            val propType = column.entityProperty.type.resolve()

            if (propType.isInline()) {
                beginControlFlow(
                    "if·((%N·as·Any?)·!==·(%T.of<%T>()·as·Any?))",
                    propName, Undefined::class.asClassName(), propType.makeNotNullable().toTypeName()
                )
            } else {
                beginControlFlow(
                    "if·(%N·!==·%T.of<%T>())",
                    propName, Undefined::class.asClassName(), propType.makeNotNullable().toTypeName()
                )
            }

            var statement: String
            if (column.entityProperty.isMutable) {
                statement = "entity.%1N·=·%1N"
            } else {
                statement = "entity[%1S]·=·%1N"
            }

            if (!propType.isMarkedNullable) {
                statement += "·?:·error(\"`%1L` should not be null.\")"
            }

            addStatement(statement, propName)
            endControlFlow()
        }

        addStatement("return entity")
    }
}
