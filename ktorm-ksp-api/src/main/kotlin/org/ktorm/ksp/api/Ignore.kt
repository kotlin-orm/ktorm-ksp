package org.ktorm.ksp.api

/**
 * Specifies to ignore this property and not generate column definitions
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
public annotation class Ignore