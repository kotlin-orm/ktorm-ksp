package org.ktorm.ksp.api

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
public annotation class KtormKspConfig(
    val allowReflectionCreateClassEntity: Boolean = true,
    val enumConverter: KClass<out EnumConverter> = Nothing::class,
    val singleTypeConverters: Array<KClass<out SingleTypeConverter<*>>> = [],
    val namingStrategy: KClass<out NamingStrategy> = Nothing::class,
    val defaultGenerator: DefaultGenerator = DefaultGenerator()
)

@Retention(AnnotationRetention.SOURCE)
public annotation class DefaultGenerator(

    /**
     * ```kotlin
     * val Database.employees: EntitySequence<Employee,Employees>
     *     get() = this.sequenceOf(Employees)
     * ```
     */
    val enableSequenceOf: Boolean = true,

    /**
     * ```kotlin
     * fun EntitySequence<Employee,Employees>.update(employee)
     * ```
     */
    val enableClassEntitySequenceAddFun: Boolean = true,

    /**
     * ```kotlin
     * fun EntitySequence<Employee,Employees>.update(employee)
     * ```
     */
    val enableClassEntitySequenceUpdateFun: Boolean = true
)
