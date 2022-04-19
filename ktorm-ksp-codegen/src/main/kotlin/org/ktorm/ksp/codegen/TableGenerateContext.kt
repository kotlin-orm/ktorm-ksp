package org.ktorm.ksp.codegen

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSFile
import org.ktorm.ksp.codegen.definition.TableDefinition

/**
 * Context information for generating table code
 */
public data class TableGenerateContext(

    /**
     * The table definition
     */
    val table: TableDefinition,

    /**
     * The global code generate config
     */
    val config: CodeGenerateConfig,

    /**
     * The column initializer generator. For creating column object in the table
     * Example:  int("id").primaryKey()
     */
    val columnInitializerGenerator: ColumnInitializerGenerator,

    /**
     * The ksp logger
     */
    val logger: KSPLogger,

    /**
     * The associated file of the output table code, for incremental update of KSP
     */
    val dependencyFiles: MutableSet<KSFile> = mutableSetOf()
)