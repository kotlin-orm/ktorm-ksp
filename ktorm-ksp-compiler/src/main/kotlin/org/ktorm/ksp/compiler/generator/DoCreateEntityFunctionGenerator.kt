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

import com.squareup.kotlinpoet.*
import org.ktorm.dsl.QueryRowSet
import org.ktorm.ksp.codegen.TableFunctionGenerator
import org.ktorm.ksp.codegen.TableGenerateContext
import org.ktorm.ksp.codegen.definition.KtormEntityType
import org.ktorm.ksp.compiler.generator.util.ClassNames
import org.ktorm.ksp.compiler.generator.util.MemberNames
import org.ktorm.ksp.compiler.generator.util.withControlFlow

public class DoCreateEntityFunctionGenerator : TableFunctionGenerator {

    /**
     * Generate doCreateEntity function for entity of any kind of class.
     */
    override fun generate(context: TableGenerateContext): List<FunSpec> {
        if (context.table.ktormEntityType != KtormEntityType.ANY_KIND_CLASS) {
            return emptyList()
        }

        val (table, config, logger, _) = context

        val funSpec = FunSpec.builder("doCreateEntity")
            .addKdoc("Create an entity object from the specific row of query results.")
            .addModifiers(KModifier.OVERRIDE)
            .returns(table.entityClassName)
            .addParameter("row", QueryRowSet::class.asTypeName())
            .addParameter("withReferences", Boolean::class.asTypeName())
            .addCode(buildCodeBlock {
                val entityClassDeclaration = table.entityClassDeclaration
                val constructor = entityClassDeclaration.primaryConstructor!!
                val constructorParameters = constructor.parameters
                val constructorParameterNames = constructorParameters.map { it.name!!.asString() }.toSet()
                val nonConstructorColumnPropertyNames = table.columns
                    .map { it.entityPropertyName.simpleName }
                    .filter { it !in constructorParameterNames }
                    .toSet()
                // propertyName -> columnMember
                val columnMap = table.columns.associateBy { it.entityPropertyName.simpleName }
                // Constructor parameters must be column or have default value
                val unknownParameters = constructor.parameters.filter {
                    !it.hasDefault && it.name?.asString() !in columnMap.keys
                }
                if (unknownParameters.isNotEmpty()) {
                    error(
                        "Construct parameter not exists in tableDefinition: " +
                                "${unknownParameters.map { it.name!!.asString() }}, If the parameter is " +
                                "not a sql column, add a default value. If the parameter is a sql column, " +
                                "please remove the Ignore annotation or ignoreProperties in the Table annotation " +
                                "to remove the parameter"
                    )
                }
                val constructorColumnParameters =
                    constructorParameters.filter { it.name!!.asString() in columnMap.keys }

                logger.info(
                    "constructorColumnParameters:$constructorColumnParameters " +
                            "nonConstructorColumnProperties: $nonConstructorColumnPropertyNames"
                )

                if (config.allowReflectionCreateEntity && constructorColumnParameters.any { it.hasDefault }) {
                    // Create an instance using reflection
                    addStatement(
                        "val constructor = %T::class.%M!!",
                        table.entityClassName,
                        MemberNames.primaryConstructor
                    )
                    addStatement(
                        "val parameterMap = %T<%T,%T?>(%L)",
                        ClassNames.hashMap,
                        ClassNames.kParameter,
                        ClassNames.any,
                        constructorColumnParameters.size
                    )
                    withControlFlow("for (parameter in constructor.parameters)") {
                        withControlFlow("when(parameter.name)") {
                            for (parameter in constructorColumnParameters) {
                                val parameterName = parameter.name!!.asString()
                                val column = columnMap[parameterName]!!
                                withControlFlow("%S -> ", arrayOf(parameterName)) {
                                    addStatement("val value = row[this.%N]", column.tablePropertyName.simpleName)
                                    // hasDefault
                                    if (parameter.hasDefault) {
                                        withControlFlow("if (value != null)") {
                                            addStatement("parameterMap[parameter] = value")
                                        }
                                    } else {
                                        val notNullOperator = if (column.isNullable) "" else "!!"
                                        addStatement("parameterMap[parameter] = value%L", notNullOperator)
                                    }
                                }
                            }
                        }
                    }
                    if (nonConstructorColumnPropertyNames.isEmpty()) {
                        addStatement("return constructor.callBy(parameterMap)", table.entityClassName)
                    } else {
                        addStatement("val entity = constructor.callBy(parameterMap)", table.entityClassName)
                    }
                } else {
                    // Create an instance using the constructor
                    if (constructorColumnParameters.isEmpty()) {
                        // nonConstructorColumnPropertyNames will not be empty
                        addStatement("val·entity·=·%T()", table.entityClassName)
                    } else {
                        if (nonConstructorColumnPropertyNames.isEmpty()) {
                            addStatement(" return·%T(", table.entityClassName)
                        } else {
                            addStatement("val·entity·=·%T(", table.entityClassName)
                        }
                        withIndent {
                            for (parameter in constructorColumnParameters) {
                                val column = table.columns.first {
                                    it.entityPropertyName.simpleName == parameter.name!!.asString()
                                }
                                val notNullOperator = if (column.isNullable) "" else "!!"
                                addStatement(
                                    "%N·=·row[this.%N]%L,",
                                    parameter.name!!.asString(),
                                    column.tablePropertyName.simpleName,
                                    notNullOperator
                                )
                            }
                        }
                        addStatement(")")
                    }
                }
                if (nonConstructorColumnPropertyNames.isNotEmpty()) {
                    // non-structural property
                    for (property in nonConstructorColumnPropertyNames) {
                        val column = columnMap[property]!!
                        if (!column.isMutable) {
                            continue
                        }
                        val notNullOperator = if (column.isNullable) "" else "!!"
                        addStatement(
                            "entity.%N·=·row[this.%N]%L",
                            property,
                            column.tablePropertyName.simpleName,
                            notNullOperator
                        )
                    }
                    addStatement("return·entity")
                }
            })
            .build()
        return listOf(funSpec)
    }
}
