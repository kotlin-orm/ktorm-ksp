package org.ktorm.ksp.api

import kotlin.reflect.KClass

/**
 * Specifies the mapped [org.ktorm.schema.Column] for a table property . If no Column annotation is specified, the
 * default values apply.
 *
 * @see [org.ktorm.schema.Column]
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
public annotation class Column(

    /**
     * Column names in SQL, Corresponds to [org.ktorm.schema.Column.name] property. If the value is an empty string,
     * the default value will be used
     */
    val columnName: String = "",

    /**
     * Column converter，Used to declare the column property in the generated table, value must be an object instance
     * or nothing. If the value is a Nothing::class, will try to find the appropriate column type automatically
     *
     * @see [org.ktorm.schema.Column]
     * @see [Converter]
     */
    val converter: KClass<out Converter> = Nothing::class,

    /**
     * property names in generate [Table]. If the value is an empty string, will use the name of the property
     * to which this annotation is added
     */
    val propertyName: String = "",

    val isReferences: Boolean = false
)