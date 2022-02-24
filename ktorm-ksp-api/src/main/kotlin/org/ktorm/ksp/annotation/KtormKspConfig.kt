package org.ktorm.ksp.annotation

import kotlin.reflect.KClass

public annotation class KtormKspConfig(
    val allowReflectionCreateEntity: Boolean = true,
    val enumConverter: KClass<out EnumConverter> = Nothing::class,
    val singleTypeConverters: Array<KClass<out SingleTypeConverter<*>>> = []
)