package org.ktorm.ksp.compiler.util

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import org.atteo.evo.inflector.English
import org.ktorm.ksp.spi.CodingNamingStrategy
import org.ktorm.ksp.spi.definition.TableDefinition

object DefaultCodingNamingStrategy : CodingNamingStrategy {

    override fun getTableClassName(cls: KSClassDeclaration): String {
        return English.plural(cls.simpleName.asString())
    }

    override fun getEntitySequenceName(cls: KSClassDeclaration): String {
        val name = English.plural(cls.simpleName.asString())
        return name.first().lowercase() + name.substring(1)
    }

    override fun getColumnPropertyName(cls: KSClassDeclaration, property: KSPropertyDeclaration): String {
        return property.simpleName.asString()
    }

    override fun getRefColumnPropertyName(
        cls: KSClassDeclaration,
        property: KSPropertyDeclaration,
        referenceTable: TableDefinition
    ): String {
        return property.simpleName.asString()
    }
}