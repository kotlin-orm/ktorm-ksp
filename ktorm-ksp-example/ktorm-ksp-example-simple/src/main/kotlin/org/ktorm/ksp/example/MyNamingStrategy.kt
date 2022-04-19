package org.ktorm.ksp.example

import org.ktorm.ksp.api.CamelCaseToLowerCaseUnderscoresNamingStrategy
import org.ktorm.ksp.api.NamingStrategy

public object MyNamingStrategy: NamingStrategy {
    override fun toTableName(entityClassName: String): String {
         return "t_" + CamelCaseToLowerCaseUnderscoresNamingStrategy.toTableName(entityClassName)
    }

    override fun toColumnName(propertyName: String): String {
        return CamelCaseToLowerCaseUnderscoresNamingStrategy.toColumnName(propertyName)
    }
}