package org.ktorm.ksp.compiler.util

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import org.atteo.evo.inflector.English
import org.ktorm.ksp.spi.CodingNamingStrategy
import org.ktorm.ksp.spi.TableMetadata

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
