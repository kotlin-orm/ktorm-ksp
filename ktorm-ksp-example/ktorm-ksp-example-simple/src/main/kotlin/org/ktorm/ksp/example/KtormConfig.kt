package org.ktorm.ksp.example

import org.ktorm.ksp.api.CamelCaseToLowerCaseUnderscoresNamingStrategy
import org.ktorm.ksp.api.KtormKspConfig

@KtormKspConfig(
    namingStrategy = CamelCaseToLowerCaseUnderscoresNamingStrategy::class,
            singleTypeConverters = [LocationWrapperConverter::class]
)
public class KtormConfig

