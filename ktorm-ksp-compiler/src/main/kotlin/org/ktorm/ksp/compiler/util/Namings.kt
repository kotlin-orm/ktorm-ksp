package org.ktorm.ksp.compiler.util

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import org.atteo.evo.inflector.English
import org.ktorm.ksp.spi.CodingNamingStrategy
import org.ktorm.ksp.spi.DatabaseNamingStrategy
import org.ktorm.ksp.spi.TableMetadata

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

object UpperSnakeCaseDatabaseNamingStrategy : DatabaseNamingStrategy {

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
        return replace(Regex("([a-z])([A-Z])"), "$1_$2").replace(Regex("([A-Z])([A-Z][a-z])"), "$1_$2").uppercase()
    }
}

object DefaultCodingNamingStrategy : CodingNamingStrategy {

    override fun getTableClassName(c: KSClassDeclaration): String {
        return English.plural(c.simpleName.asString())
    }

    override fun getEntitySequenceName(c: KSClassDeclaration): String {
        // TODO: CDPService --> cdpServices
        val name = English.plural(c.simpleName.asString())
        return name.first().lowercase() + name.substring(1)
    }

    override fun getColumnPropertyName(c: KSClassDeclaration, prop: KSPropertyDeclaration): String {
        return prop.simpleName.asString()
    }

    override fun getRefColumnPropertyName(
        c: KSClassDeclaration, prop: KSPropertyDeclaration, ref: TableMetadata
    ): String {
        return prop.simpleName.asString()
    }
}
