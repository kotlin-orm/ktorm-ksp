package org.ktorm.ksp.annotation

import kotlin.reflect.KClass

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
public annotation class Column(
    val columnName: String = "",
    val converter: KClass<out Converter> = Nothing::class
)