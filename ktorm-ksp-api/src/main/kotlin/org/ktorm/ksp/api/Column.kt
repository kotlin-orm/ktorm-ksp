package org.ktorm.ksp.api

import kotlin.reflect.KClass

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
public annotation class Column(
    val columnName: String = "",
    val converter: KClass<out Converter> = Nothing::class
)