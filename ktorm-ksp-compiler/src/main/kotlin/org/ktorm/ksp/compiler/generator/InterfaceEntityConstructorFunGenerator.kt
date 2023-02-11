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

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.buildCodeBlock
import org.ktorm.ksp.compiler.util.ClassNames
import org.ktorm.ksp.compiler.util.CodeFactory
import org.ktorm.ksp.compiler.util.NameAllocator
import org.ktorm.ksp.compiler.util.SuppressAnnotations
import org.ktorm.ksp.compiler.util.SuppressAnnotations.functionName
import org.ktorm.ksp.spi.TableGenerateContext
import org.ktorm.ksp.spi.TopLevelFunctionGenerator
import org.ktorm.ksp.spi.definition.KtormEntityType

public class InterfaceEntityConstructorFunGenerator : TopLevelFunctionGenerator {

    override fun generate(context: TableGenerateContext): List<FunSpec> {
        val table = context.table
        if (table.ktormEntityType != KtormEntityType.ENTITY_INTERFACE) {
            return emptyList()
        }

        val nameAllocator = NameAllocator()
        val funSpec = FunSpec
            .builder(table.entityClassName.simpleName)
            .addKdoc(
                "Create an entity of [%L] and specify the initial values for each properties, " +
                        "properties that doesn't have an initial value will left unassigned. ",
                table.entityClassName.simpleName
            )
            .addAnnotation(SuppressAnnotations.buildSuppress(functionName))
            .returns(table.entityClassName)
            .addParameters(CodeFactory.buildEntityConstructorParameters(context, nameAllocator))
            .addCode(buildCodeBlock {
                val entityVar = nameAllocator.newName("entity")
                addStatement("val·%L·=·%T.create<%T>()", entityVar, ClassNames.entity, table.entityClassName)
                add(CodeFactory.buildEntityAssignCode(context, entityVar, nameAllocator))
            })
            .build()
        return listOf(funSpec)
    }
}
