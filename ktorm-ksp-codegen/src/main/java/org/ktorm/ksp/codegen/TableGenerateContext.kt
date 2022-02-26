package org.ktorm.ksp.codegen

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSFile
import org.ktorm.ksp.codegen.definition.TableDefinition

public data class TableGenerateContext(
    val table: TableDefinition,
    val config: CodeGenerateConfig,
    val columnInitializerGenerator: ColumnInitializerGenerator,
    val logger: KSPLogger,
    val dependencyFiles: MutableSet<KSFile> = mutableSetOf()
)