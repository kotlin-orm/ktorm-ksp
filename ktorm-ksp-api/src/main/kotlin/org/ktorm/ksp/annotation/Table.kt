package org.ktorm.ksp.annotation

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
public annotation class Table(
    val tableName: String = "",
    val tableClassName: String = "",
    val alias: String = "",
    val catalog: String = "",
    val schema: String = "",
    val ignoreColumns: Array<String> = []
)