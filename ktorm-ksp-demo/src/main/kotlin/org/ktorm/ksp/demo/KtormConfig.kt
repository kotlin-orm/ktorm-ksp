package org.ktorm.ksp.demo

import org.ktorm.ksp.api.KtormKspConfig

@KtormKspConfig(
    allowReflectionCreateEntity = true,
    enumConverter = IntEnumConverter::class,
    singleTypeConverters = [StringConverter::class]
)
public class KtormConfig {
}