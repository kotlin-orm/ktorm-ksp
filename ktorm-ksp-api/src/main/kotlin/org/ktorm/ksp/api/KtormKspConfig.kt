package org.ktorm.ksp.api

import kotlin.reflect.KClass

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
public annotation class KtormKspConfig(
    val allowReflectionCreateEntity: Boolean = true,
    val enumConverter: KClass<out EnumConverter> = Nothing::class,
    val singleTypeConverters: Array<KClass<out SingleTypeConverter<*>>> = []
)