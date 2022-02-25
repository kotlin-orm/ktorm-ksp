package org.ktorm.ksp.api

/**
 * Specifies the primary key of an entity. The primaryKey() call will be added to the generated Column
 *
 * Example:
 *
 * ```kotlin
 * @Table
 * data class User (
 *  @PrimaryKey
 *  val id:Int
 * )
 * ```
 *
 * GenerateCode:
 *
 * ```kotlin
 * object Users: BaseTable<User>(...) {
 *      val id: Column<Int> = int("id").primaryKey()
 * }
 * ```
 *
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
public annotation class PrimaryKey