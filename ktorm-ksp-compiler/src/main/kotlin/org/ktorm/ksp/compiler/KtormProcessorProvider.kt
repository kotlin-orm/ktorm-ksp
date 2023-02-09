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

package org.ktorm.ksp.compiler

import com.google.devtools.ksp.*
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.*
import org.ktorm.ksp.api.*
import org.ktorm.ksp.spi.definition.ColumnDefinition
import org.ktorm.ksp.spi.definition.TableDefinition
import kotlin.reflect.jvm.jvmName

class KtormProcessorProvider : SymbolProcessorProvider {

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return KtormProcessor(environment)
    }
}

@OptIn(KspExperimental::class)
class KtormProcessor(environment: SymbolProcessorEnvironment) : SymbolProcessor {
    private val logger = environment.logger
    private val options = environment.options
    private val codeGenerator = environment.codeGenerator

    override fun process(resolver: Resolver): List<KSAnnotated> {
        logger.info("Starting ktorm ksp processor.")
        val (symbols, deferral) = resolver.getSymbolsWithAnnotation(Table::class.jvmName).partition { it.validate() }

        val tables = symbols
            .filterIsInstance<KSClassDeclaration>()
            .map { entityClass ->
                val columns = ArrayList<ColumnDefinition>()
                TableDefinition(entityClass, columns).also { table ->
                    for (property in entityClass.getAllProperties()) {
                        val propertyName = property.simpleName.asString()
                        if (property.isAnnotationPresent(Ignore::class) || propertyName in table.ignoreProperties) {
                            continue
                        }

                        val parent = property.parentDeclaration
                        if (parent is KSClassDeclaration && parent.classKind == ClassKind.CLASS && !property.hasBackingField) {
                            continue
                        }

                        // TODO: skip properties in Entity interface.
                        columns += ColumnDefinition(property, table)
                    }
                }
            }

        // TODO: check if referenced class is an interface entity (递归)
        // TODO: check if referenced class is marked with @Table (递归)
        // TODO: check if the referenced table has only one primary key.
        // KtormCodeGenerator.generate(tableDefinitions, environment.codeGenerator, config, logger)
        return deferral
    }
}
