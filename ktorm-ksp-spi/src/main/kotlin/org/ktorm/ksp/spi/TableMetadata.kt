package org.ktorm.ksp.spi

import com.google.devtools.ksp.symbol.KSClassDeclaration

/**
 * Table definition metadata.
 */
public data class TableMetadata(

    /**
     * The annotated entity class of the table.
     */
    val entityClass: KSClassDeclaration,

    /**
     * The name of the table.
     */
    val name: String,

    /**
     * The alias of the table.
     */
    val alias: String?,

    /**
     * The catalog of the table.
     */
    val catalog: String?,

    /**
     * The schema of the table.
     */
    val schema: String?,

    /**
     * The name of the corresponding table class in the generated code.
     */
    val tableClassName: String,

    /**
     * The name of the corresponding entity sequence in the generated code.
     */
    val entitySequenceName: String,

    /**
     * Properties that should be ignored for generating column definitions.
     */
    val ignoreProperties: Set<String>,

    /**
     * Columns in the table.
     */
    val columns: List<ColumnMetadata>
)