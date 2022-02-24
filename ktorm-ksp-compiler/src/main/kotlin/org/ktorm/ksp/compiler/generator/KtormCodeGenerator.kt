@file:OptIn(KotlinPoetKspPreview::class)

package org.ktorm.ksp.compiler.generator

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.writeTo
import org.ktorm.ksp.compiler.definition.CodeGenerateConfig
import org.ktorm.ksp.compiler.definition.TableDefinition

public class KtormCodeGenerator {

    public fun generate(
        tables: List<TableDefinition>,
        codeGenerator: CodeGenerator,
        config: CodeGenerateConfig,
        columnInitializerGenerator: ColumnInitializerGenerator,
        logger: KSPLogger,
    ) {
        logger.info("generate tables:${tables.map { it.entityClassName.simpleName }}}")
        val configDependencyFile = config.configDependencyFile
        for (table in tables) {
            val dependencyFiles = mutableSetOf(table.entityFile)
            if (configDependencyFile != null) {
                dependencyFiles.add(configDependencyFile)
            }
            logger.info("generate table:$table")
            val file = generateTable(table, config, columnInitializerGenerator, dependencyFiles, logger)
            logger.info("table dependencyFiles:${dependencyFiles.map { it.location }}")
            file.writeTo(codeGenerator, Dependencies(true, *dependencyFiles.toTypedArray()))
        }
    }

    public fun generateTable(
        table: TableDefinition,
        config: CodeGenerateConfig,
        columnInitializerGenerator: ColumnInitializerGenerator,
        dependencyFiles: MutableSet<KSFile>,
        logger: KSPLogger
    ): FileSpec {
        val generator = when (table.entityClassDeclaration.classKind) {
            ClassKind.INTERFACE -> TableGenerator(table, config, dependencyFiles, columnInitializerGenerator, logger)
            ClassKind.CLASS -> BaseTableGenerator(table, config, dependencyFiles, columnInitializerGenerator, logger)
            else -> error("Unsupported entity type: ${table.entityClassDeclaration.qualifiedName?.asString()}")
        }
        return generator.generate()
    }

}