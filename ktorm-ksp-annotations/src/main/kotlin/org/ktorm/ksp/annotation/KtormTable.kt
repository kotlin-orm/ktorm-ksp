package org.ktorm.ksp.annotation

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
public annotation class KtormTable(
    val databaseTableName:String = "",
    val tableClassName:String = ""
)