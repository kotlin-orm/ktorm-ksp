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

package org.ktorm.ksp.codegen.generator

import com.squareup.kotlinpoet.*
import org.ktorm.dsl.QueryRowSet
import org.ktorm.ksp.codegen.*
import org.ktorm.ksp.codegen.definition.KtormEntityType

public class DefaultTableFunctionGenerator : TableFunctionGenerator {

    /**
     * Generate doCreateEntity function for entity of any kind of class.
     */
    override fun generate(context: TableGenerateContext, emitter: (FunSpec) -> Unit) {
        if (context.table.ktormEntityType != KtormEntityType.ANY_KIND_CLASS) {
            return
        }
        val (table, config, _, logger, _) = context
        val row = "row"
        val withReferences = "withReferences"
        FunSpec.builder("doCreateEntity").addModifiers(KModifier.OVERRIDE).returns(table.entityClassName)
            .addParameter(row, QueryRowSet::class.asTypeName())
            .addParameter(withReferences, Boolean::class.asTypeName()).addCode(buildCodeBlock {
                val entityClassDeclaration = table.entityClassDeclaration
                val constructor = entityClassDeclaration.primaryConstructor!!
                val constructorParameters = constructor.parameters
                val constructorParameterNames = constructorParameters.map { it.name!!.asString() }.toSet()
                val nonConstructorParameterNames = table.columns
                    .map { it.entityPropertyName.simpleName }
                    .filter { it !in constructorParameterNames }
                    .toSet()
                // propertyName -> columnMember
                val columnMap = table.columns.associateBy { it.entityPropertyName.simpleName }
                val unknownParameters = constructor.parameters.filter {
                    !it.hasDefault && it.name?.asString() !in constructorParameterNames
                }
                if (unknownParameters.isNotEmpty()) {
                    error(
                        "unknown constructor parameter for ${table.entityClassName.canonicalName} : " +
                                "${unknownParameters.map { it.name?.asString() }}"
                    )
                }
                if (config.allowReflectionCreateEntity && constructorParameters.any { it.hasDefault }) {
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
                        constructorParameters.size
                    )
                    withControlFlow("for (parameter in constructor.parameters)") {
                        withControlFlow("when(parameter.name)") {
                            for (parameter in constructorParameters) {
                                val parameterName = parameter.name!!.asString()
                                val column = columnMap[parameterName]
                                    ?: if (parameter.hasDefault) {
                                        continue
                                    } else {
                                        error("not found column definition: $parameterName")
                                    }
                                withControlFlow("%S -> ", arrayOf(parameterName)) {
                                    addStatement("val value = %L[this.%L]", row, column.tablePropertyName.simpleName)
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
                    if (nonConstructorParameterNames.isEmpty()) {
                        addStatement("return constructor.callBy(parameterMap)", table.entityClassName)
                    } else {
                        addStatement("val entity = constructor.callBy(parameterMap)", table.entityClassName)
                    }
                } else {
                    // Create instance with code when construct has no default value parameter
                    if (nonConstructorParameterNames.isEmpty()) {
                        addStatement(" return·%T(", table.entityClassName)
                    } else {
                        addStatement("val·entity·=·%T(", table.entityClassName)
                    }
                    logger.info("constructorParameter:${constructorParameters.map { it.name!!.asString() }}")
                    withIndent {
                        for (parameter in constructorParameters) {
                            val column =
                                table.columns.firstOrNull {
                                    it.entityPropertyName.simpleName == parameter.name!!.asString()
                                } ?: error(
                                    "Construct parameter not exists in tableDefinition: " +
                                            "${parameter.name!!.asString()}, If the parameter is not a sql column, " +
                                            "add a default value. If the parameter is a sql column, please remove " +
                                            "the Ignore annotation or ignoreColumns in the Table annotation to " +
                                            "remove the parameter"
                                )
                            val notNullOperator = if (column.isNullable) "" else "!!"
                            addStatement(
                                "%L·=·%L[this.%L]%L,",
                                parameter.name!!.asString(),
                                row,
                                column.tablePropertyName.simpleName,
                                notNullOperator
                            )
                        }
                    }
                    addStatement(")")
                }
                context.logger.info("constructorParameter:$constructorParameters")
                if (nonConstructorParameterNames.isNotEmpty()) {
                    // non-structural property
                    for (property in nonConstructorParameterNames) {
                        val column = columnMap[property]!!
                        if (!column.isMutable) {
                            continue
                        }
                        val notNullOperator = if (column.isNullable) "" else "!!"
                        addStatement(
                            "entity.%L·=·%L[this.%L]%L",
                            property,
                            row,
                            column.tablePropertyName.simpleName,
                            notNullOperator
                        )
                    }
                    addStatement("return·entity")
                }
            })
            .build()
            .run(emitter)
    }
}
