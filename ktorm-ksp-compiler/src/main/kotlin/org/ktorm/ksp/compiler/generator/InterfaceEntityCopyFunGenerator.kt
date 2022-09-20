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
import com.squareup.kotlinpoet.NameAllocator
import com.squareup.kotlinpoet.buildCodeBlock
import org.ktorm.ksp.codegen.TableGenerateContext
import org.ktorm.ksp.codegen.TopLevelFunctionGenerator
import org.ktorm.ksp.codegen.definition.KtormEntityType
import org.ktorm.ksp.compiler.generator.util.CodeFactory

public class InterfaceEntityCopyFunGenerator : TopLevelFunctionGenerator {

    override fun generate(context: TableGenerateContext): List<FunSpec> {
        val table = context.table
        if (table.ktormEntityType != KtormEntityType.ENTITY_INTERFACE) {
            return emptyList()
        }

        val nameAllocator = NameAllocator()

        val funSpec = FunSpec
            .builder("copy")
            .addKdoc(
                "Return a deep copy of this entity (which has the same property values and tracked statuses), " +
                        "and alter the specified property values. "
            )
            .returns(table.entityClassName)
            .receiver(table.entityClassName)
            .addParameters(CodeFactory.buildEntityConstructorParameters(context, nameAllocator))
            .addCode(buildCodeBlock {
                val entityVar = nameAllocator.newName("entity")
                addStatement("val·%L·=·this.copy()", entityVar)
                add(CodeFactory.buildEntityAssignCode(context, entityVar))
            })
            .build()
        return listOf(funSpec)
    }
}
