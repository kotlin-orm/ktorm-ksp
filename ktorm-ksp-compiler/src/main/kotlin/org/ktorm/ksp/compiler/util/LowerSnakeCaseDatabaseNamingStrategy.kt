package org.ktorm.ksp.compiler.util

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import org.ktorm.ksp.spi.DatabaseNamingStrategy
import org.ktorm.ksp.spi.TableMetadata

/**
 * Generating lower snake-case names.
 */
object LowerSnakeCaseDatabaseNamingStrategy : DatabaseNamingStrategy {

    override fun getTableName(c: KSClassDeclaration): String {
        return c.simpleName.asString().toSnakeCase()
    }

    override fun getColumnName(c: KSClassDeclaration, prop: KSPropertyDeclaration): String {
        return prop.simpleName.asString().toSnakeCase()
    }

    override fun getRefColumnName(c: KSClassDeclaration, prop: KSPropertyDeclaration, ref: TableMetadata): String {
        return prop.simpleName.asString().toSnakeCase()
    }

    private fun String.toSnakeCase(): String {
        return replace(Regex("([a-z])([A-Z])"), "$1_$2").replace(Regex("([A-Z])([A-Z][a-z])"), "$1_$2").lowercase()
    }
}
