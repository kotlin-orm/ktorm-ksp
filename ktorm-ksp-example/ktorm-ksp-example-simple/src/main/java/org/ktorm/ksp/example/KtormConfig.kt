package org.ktorm.ksp.example

import org.ktorm.ksp.api.KtormKspConfig

@KtormKspConfig(
    namingStrategy = MyNamingStrategy::class,
    singleTypeConverters = [LocationWrapperConverter::class]
)
public class KtormConfig

