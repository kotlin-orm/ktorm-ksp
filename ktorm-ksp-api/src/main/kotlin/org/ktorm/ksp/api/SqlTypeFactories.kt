package org.ktorm.ksp.api

import org.ktorm.jackson.JsonSqlType
import org.ktorm.jackson.sharedObjectMapper
import org.ktorm.schema.EnumSqlType
import org.ktorm.schema.SqlType
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.javaType
import kotlin.reflect.jvm.jvmErasure

/**
 * Type factory object that creates [EnumSqlType] instances.
 */
public object EnumSqlTypeFactory : SqlTypeFactory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> createSqlType(property: KProperty1<*, T?>): SqlType<T> {
        val returnType = property.returnType.jvmErasure.java
        if (returnType.isEnum) {
            return EnumSqlType(returnType as Class<out Enum<*>>) as SqlType<T>
        } else {
            throw IllegalArgumentException("The property is required to be typed of enum but actually: $returnType")
        }
    }
}

/**
 * Type factory object that creates [JsonSqlType] instances.
 */
public object JsonSqlTypeFactory : SqlTypeFactory {

    override fun <T : Any> createSqlType(property: KProperty1<*, T?>): SqlType<T> {
        return JsonSqlType(sharedObjectMapper, sharedObjectMapper.constructType(property.returnType.javaType))
    }
}
