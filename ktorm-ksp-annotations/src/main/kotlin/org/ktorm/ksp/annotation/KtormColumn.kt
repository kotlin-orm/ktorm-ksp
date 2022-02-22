package org.ktorm.ksp.annotation

import org.ktorm.schema.Table
import kotlin.reflect.KClass

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
public annotation class KtormColumn(
    val columnName: String = "",
    val isPrimaryKey: Boolean = false,
    val superTableClass: KClass<*> = Table::class,
    val ignore:Boolean =false
)