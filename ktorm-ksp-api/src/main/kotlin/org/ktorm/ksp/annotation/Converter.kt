package org.ktorm.ksp.annotation

import org.ktorm.schema.BaseTable
import org.ktorm.schema.Column
import org.ktorm.schema.Table
import kotlin.reflect.KClass

public sealed interface Converter

/**
 * Used to create [Column]  of [T] type in the [Table]
 * Subclass must be object instance
 */
public interface SingleTypeConverter<T : Any> : Converter {
    public fun convert(table: BaseTable<*>, columnName: String, propertyType: KClass<T>): Column<T>
}


/**
 * Used to create [Column] or any type in the [Table]
 * Subclass must be object instance
 */
public interface MultiTypeConverter : Converter {
    public fun <T : Any> convert(table: BaseTable<*>, columnName: String, propertyType: KClass<T>): Column<T>
}

/**
 * Used to create [Column]  of enum type in the [Table]
 * Subclass must be object instance
 */
public interface EnumConverter : Converter {
    public fun <E : Enum<E>> convert(table: BaseTable<*>, columnName: String, propertyType: KClass<E>): Column<E>
}