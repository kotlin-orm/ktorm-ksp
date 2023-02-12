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

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate
import org.ktorm.ksp.api.Table
import org.ktorm.ksp.compiler.util.MetadataParser
import kotlin.reflect.jvm.jvmName

class KtormProcessorProvider : SymbolProcessorProvider {

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return object : SymbolProcessor {
            override fun process(resolver: Resolver): List<KSAnnotated> {
                val symbols = resolver.getSymbolsWithAnnotation(Table::class.jvmName)
                val (validSymbols, deferral) = symbols.partition { it.validate() }

                val parser = MetadataParser(resolver, environment)
                val tables = validSymbols
                    .filterIsInstance<KSClassDeclaration>()
                    .map { entityClass ->
                        parser.parseTableMetadata(entityClass)
                    }

                // KtormCodeGenerator.generate(tableDefinitions, environment.codeGenerator, config, logger)
                return deferral
            }
        }
    }
}
