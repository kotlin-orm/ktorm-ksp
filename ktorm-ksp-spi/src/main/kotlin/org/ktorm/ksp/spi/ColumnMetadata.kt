package org.ktorm.ksp.spi

import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType

/**
 * Column definition metadata.
 */
public data class ColumnMetadata(

    /**
     * The annotated entity property of the column.
     */
    val entityProperty: KSPropertyDeclaration,

    /**
     * The belonging table.
     */
    val table: TableMetadata,

    /**
     * The name of the column.
     */
    val name: String,

    /**
     * Check if the column is a primary key.
     */
    val isPrimaryKey: Boolean,

    /**
     * The SQL type of the column.
     */
    val sqlType: KSType,

    /**
     * Check if the column is a reference column.
     */
    val isReference: Boolean,

    /**
     * The referenced table of the column.
     */
    val referenceTable: TableMetadata?,

    /**
     * The name of the corresponding column property in the generated table object.
     */
    val tablePropertyName: String
)