package org.ktorm.ksp.demo

import org.ktorm.ksp.api.DefaultGenerator
import org.ktorm.ksp.api.KtormKspConfig
import org.ktorm.ksp.api.LowerCaseCamelCaseToUnderscoresNamingStrategy
import org.ktorm.ksp.api.NamingStrategy

@KtormKspConfig(
    allowReflectionCreateEntity = true,
    enumConverter = IntEnumConverter::class,
    singleTypeConverters = [CustomStringConverter::class],
    namingStrategy = MyNamingStrategy::class,
    defaultGenerator = DefaultGenerator(
        enableSequenceOf = false,
        enableClassEntitySequenceAddFun = false,
        enableClassEntitySequenceUpdateFun = true
    )
)
public class KtormConfig {
}

public object  MyNamingStrategy: NamingStrategy {
    override fun toTableName(entityClassName: String): String {
        return "t_" + LowerCaseCamelCaseToUnderscoresNamingStrategy.toColumnName(entityClassName)
    }

    override fun toColumnName(propertyName: String): String {
        return "c_" + LowerCaseCamelCaseToUnderscoresNamingStrategy.toColumnName(propertyName)
    }

}