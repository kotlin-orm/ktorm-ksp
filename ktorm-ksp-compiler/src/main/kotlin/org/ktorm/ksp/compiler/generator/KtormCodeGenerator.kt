@file:OptIn(KotlinPoetKspPreview::class)

package org.ktorm.ksp.compiler.generator

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.writeTo
import org.ktorm.ksp.compiler.definition.CodeGenerateConfig
import org.ktorm.ksp.compiler.definition.KtormEntityType
import org.ktorm.ksp.compiler.definition.TableDefinition

public class KtormCodeGenerator {

    public fun generate(
        tables: List<TableDefinition>,
        codeGenerator: CodeGenerator,
        config: CodeGenerateConfig,
        columnInitializerGenerator: ColumnInitializerGenerator,
        logger: KSPLogger,
    ) {
        logger.info("generate tables:${tables.map { it.entityClassName.simpleName }}")
        logger.info("code generator config:${config}")
        val configDependencyFile = config.configDependencyFile
        for (table in tables) {
            val dependencyFiles = mutableSetOf(table.entityFile)
            if (configDependencyFile != null) {
                dependencyFiles.add(configDependencyFile)
            }
            logger.info("generate table:$table")
            val context = TableGenerateContext(table, config, columnInitializerGenerator, logger, dependencyFiles)
            val file = generateTable(context)
            logger.info("table dependencyFiles:${dependencyFiles.map { it.location }}")
            file.writeTo(codeGenerator, Dependencies(true, *dependencyFiles.toTypedArray()))
        }
    }

    public fun generateTable(context: TableGenerateContext): FileSpec {
        val generator = when (context.table.ktormEntityType) {
            KtormEntityType.INTERFACE -> TableGenerator(context)
            KtormEntityType.CLASS -> BaseTableGenerator(context)
        }
        return generator.generate()
    }

}

public data class TableGenerateContext(
    val table: TableDefinition,
    val config: CodeGenerateConfig,
    val columnInitializerGenerator: ColumnInitializerGenerator,
    val logger: KSPLogger,
    val dependencyFiles: MutableSet<KSFile> = mutableSetOf()
)