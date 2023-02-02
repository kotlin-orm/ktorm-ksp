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

import com.facebook.ktfmt.format.Formatter
import com.facebook.ktfmt.format.FormattingOptions
import com.facebook.ktfmt.format.FormattingOptions.Style.GOOGLE
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.squareup.kotlinpoet.FileSpec
import org.ktorm.ksp.spi.CodeGenerateConfig
import org.ktorm.ksp.spi.TableGenerateContext
import org.ktorm.ksp.spi.definition.TableDefinition

public object KtormCodeGenerator {

    public fun generate(
        tables: List<TableDefinition>,
        codeGenerator: CodeGenerator,
        config: CodeGenerateConfig,
        logger: KSPLogger,
    ) {
        val tableFileGenerator = TableFileGenerator(config, logger)
        val configDependencyFile = config.configDependencyFile

        for (table in tables) {
            val dependencyFiles = mutableSetOf(table.entityFile)
            if (configDependencyFile != null) {
                dependencyFiles.add(configDependencyFile)
            }

            // Generate file spec via kotlinpoet.
            val context = TableGenerateContext(table, config, logger, dependencyFiles)
            val fileSpec = tableFileGenerator.generate(context)

            // Beautify the generated code via facebook ktfmt.
            val formattedCode = formatCode(fileSpec, logger)

            // Output the formatted code.
            val dependencies = Dependencies(true, *dependencyFiles.toTypedArray())
            val file = codeGenerator.createNewFile(dependencies, fileSpec.packageName, fileSpec.name)
            file.writer(Charsets.UTF_8).use { it.write(formattedCode) }
        }
    }

    private fun formatCode(fileSpec: FileSpec, logger: KSPLogger): String {
        // Use the Kotlin official code style.
        val options = FormattingOptions(style = GOOGLE, maxWidth = 120, blockIndent = 4)

        // Remove tailing commas in parameter lists.
        val code = fileSpec.toString().replace(Regex(""",\s*\)"""), ")")

        try {
            return Formatter.format(options, code)
        } catch (e: Exception) {
            logger.exception(e)
            return code
        }
    }
}
