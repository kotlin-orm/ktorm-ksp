package org.ktorm.ksp.compiler.util

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import org.ktorm.ksp.spi.DatabaseNamingStrategy
import org.ktorm.ksp.spi.TableMetadata

/**
 * Generating upper snake-case names.
 */
object UpperSnakeCaseDatabaseNamingStrategy : DatabaseNamingStrategy {

    override fun getTableName(cls: KSClassDeclaration): String {
        return cls.simpleName.asString().toSnakeCase()
    }

    override fun getColumnName(cls: KSClassDeclaration, property: KSPropertyDeclaration): String {
        return property.simpleName.asString().toSnakeCase()
    }

    override fun getRefColumnName(cls: KSClassDeclaration, property: KSPropertyDeclaration, referenceTable: TableMetadata): String {
        return property.simpleName.asString().toSnakeCase()
    }

    private fun String.toSnakeCase(): String {
        return replace(Regex("([a-z])([A-Z])"), "$1_$2").replace(Regex("([A-Z])([A-Z][a-z])"), "$1_$2").uppercase()
    }
}
