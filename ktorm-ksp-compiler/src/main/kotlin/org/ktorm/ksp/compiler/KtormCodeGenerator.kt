@file:OptIn(KotlinPoetKspPreview::class)

package org.ktorm.ksp.compiler

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.writeTo

public class KtormCodeGenerator {

    public fun generate(
        tables: List<TableDefinition>,
        codeGenerator: CodeGenerator,
        logger: KSPLogger,
    ) {
        logger.info("generate tables:${tables.map { it.entityClassName.simpleName }}}")
        for (table in tables) {
            val file = generateTable(table)
            file.writeTo(codeGenerator, Dependencies(true, table.entityFile))
        }
    }

    public fun generateTable(table: TableDefinition): FileSpec {
        val generator = when (table.entityClassDeclaration.classKind) {
            ClassKind.INTERFACE -> TableGenerator(table)
            ClassKind.CLASS -> BaseTableGenerator(table)
            else -> throw IllegalArgumentException("Unsupported entity type: ${table.entityClassDeclaration.qualifiedName?.asString()}")
        }
        return generator.generate()
    }

}