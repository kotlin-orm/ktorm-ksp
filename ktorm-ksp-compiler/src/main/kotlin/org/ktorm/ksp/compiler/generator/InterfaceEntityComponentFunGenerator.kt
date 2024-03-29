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

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import org.ktorm.ksp.codegen.TableGenerateContext
import org.ktorm.ksp.codegen.TopLevelFunctionGenerator
import org.ktorm.ksp.codegen.definition.KtormEntityType

public class InterfaceEntityComponentFunGenerator : TopLevelFunctionGenerator {

    override fun generate(context: TableGenerateContext): List<FunSpec> {
        val table = context.table
        if (table.ktormEntityType != KtormEntityType.ENTITY_INTERFACE) {
            return emptyList()
        }

        return table.columns.mapIndexed { i, column ->
            FunSpec.builder("component${i + 1}")
                .addKdoc(
                    "Return the value of [%L.%L]. ",
                    table.entityClassName.simpleName,
                    column.entityPropertyName.simpleName
                )
                .addModifiers(KModifier.OPERATOR)
                .returns(column.propertyTypeName)
                .receiver(table.entityClassName)
                .addCode("return·this.%L", column.entityPropertyName.simpleName)
                .build()
        }
    }
}
