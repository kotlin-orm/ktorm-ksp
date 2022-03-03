package org.ktorm.ksp.example

import org.ktorm.ksp.api.LowerCaseCamelCaseToUnderscoresNamingStrategy
import org.ktorm.ksp.api.NamingStrategy

public object MyNamingStrategy: NamingStrategy {
    override fun toTableName(entityClassName: String): String {
         return "t_" + LowerCaseCamelCaseToUnderscoresNamingStrategy.toTableName(entityClassName)
    }

    override fun toColumnName(propertyName: String): String {
        return LowerCaseCamelCaseToUnderscoresNamingStrategy.toColumnName(propertyName)
    }
}