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
    public fun getTableClassName(c: KSClassDeclaration): String

    /**
     * Generate the entity sequence name.
     */
    public fun getEntitySequenceName(c: KSClassDeclaration): String

    /**
     * Generate the column property name.
     */
    public fun getColumnPropertyName(c: KSClassDeclaration, prop: KSPropertyDeclaration): String

    /**
     * Generate the reference column property name.
     */
    public fun getRefColumnPropertyName(c: KSClassDeclaration, prop: KSPropertyDeclaration, ref: TableMetadata): String
}