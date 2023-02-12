package org.ktorm.ksp.spi

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

/**
 * Naming strategy for Kotlin symbols in the generated code.
 */
public interface CodingNamingStrategy {

    /**
     * Generate the table class name.
     */
    public fun getTableClassName(cls: KSClassDeclaration): String

    /**
     * Generate the entity sequence name.
     */
    public fun getEntitySequenceName(cls: KSClassDeclaration): String

    /**
     * Generate the column property name.
     */
    public fun getColumnPropertyName(cls: KSClassDeclaration, prop: KSPropertyDeclaration): String

    /**
     * Generate the reference column property name.
     */
    public fun getRefColumnPropertyName(cls: KSClassDeclaration, prop: KSPropertyDeclaration, refTable: TableMetadata): String
}