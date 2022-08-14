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

@file:OptIn(KotlinPoetKspPreview::class)

package org.ktorm.ksp.compiler.generator

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.writeTo
import org.ktorm.ksp.codegen.CodeGenerateConfig
import org.ktorm.ksp.codegen.ColumnInitializerGenerator
import org.ktorm.ksp.codegen.TableGenerateContext
import org.ktorm.ksp.codegen.definition.TableDefinition

public object KtormCodeGenerator {

    public fun generate(
        tables: List<TableDefinition>,
        codeGenerator: CodeGenerator,
        config: CodeGenerateConfig,
        columnInitializerGenerator: ColumnInitializerGenerator,
        logger: KSPLogger,
    ) {
        logger.info("generate tables:${tables.map { it.entityClassName.simpleName }}")
        logger.info("code generator config:$config")
        val tableFileGenerator = TableFileGenerator(config, logger)
        val configDependencyFile = config.configDependencyFile
        for (table in tables) {
            val dependencyFiles = mutableSetOf(table.entityFile)
            if (configDependencyFile != null) {
                dependencyFiles.add(configDependencyFile)
            }
            logger.info("generate table:$table")
            val context = TableGenerateContext(table, config, columnInitializerGenerator, logger, dependencyFiles)
            val file = tableFileGenerator.generate(context)
            logger.info("table dependencyFiles:${dependencyFiles.map { it.location }}")
            file.writeTo(codeGenerator, Dependencies(true, *dependencyFiles.toTypedArray()))
        }
    }
}
