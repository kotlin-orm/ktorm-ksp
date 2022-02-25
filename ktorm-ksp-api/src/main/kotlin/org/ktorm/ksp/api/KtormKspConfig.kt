package org.ktorm.ksp.api

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
public annotation class KtormKspConfig(
    val allowReflectionCreateEntity: Boolean = true,
    val enumConverter: KClass<out EnumConverter> = Nothing::class,
    val singleTypeConverters: Array<KClass<out SingleTypeConverter<*>>> = [],
    val namingStrategy: KClass<out NamingStrategy> = Nothing::class
)
