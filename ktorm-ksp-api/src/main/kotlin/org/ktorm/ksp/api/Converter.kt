/*
 * Copyright 2018-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ktorm.ksp.api

import org.ktorm.schema.BaseTable
import org.ktorm.schema.Column
import kotlin.reflect.KClass

/**
 * Convert the properties in the entity class to the properties of the [Column] in the table.
 * By default, ktorm-ksp will automatically convert properties in entity classes into appropriate Column properties.
 * The [org.ktorm.ksp.api.Column] or [KtormKspConfig] annotations can override the default type conversion rules.
 *
 * The default supported types:
 * +──────────────────────────+────────────────+────────────────────────+──────────────────+
 * | Kotlin Type              | Function Name  | Underlying SQL Type    | JDBC Type Code   |
 * +──────────────────────────+────────────────+────────────────────────+──────────────────+
 * | kotlin.Boolean           | boolean        | boolean                | Types.BOOLEAN    |
 * | kotlin.Int               | int            | int                    | Types.INTEGER    |
 * | kotlin.Short             | short          | smallint               | Types.SMALLINT   |
 * | kotlin.Long              | long           | bigint                 | Types.BIGINT     |
 * | kotlin.Float             | float          | float                  | Types.FLOAT      |
 * | kotlin.Double            | double         | double                 | Types.DOUBLE     |
 * | kotlin.BigDecimal        | decimal        | decimal                | Types.DECIMAL    |
 * | kotlin.String            | varchar        | varchar                | Types.VARCHAR    |
 * | java.sql.Date            | jdbcDate       | date                   | Types.DATE       |
 * | java.sql.Time            | jdbcTime       | time                   | Types.TIME       |
 * | java.sql.Timestamp       | jdbcTimestamp  | timestamp              | Types.TIMESTAMP  |
 * | java.time.LocalDateTime  | datetime       | datetime               | Types.TIMESTAMP  |
 * | java.time.LocalDate      | date           | date                   | Types.DATE       |
 * | java.time.LocalTime      | time           | time                   | Types.TIME       |
 * | java.time.MonthDay       | monthDay       | varchar                | Types.VARCHAR    |
 * | java.time.YearMonth      | yearMonth      | varchar                | Types.VARCHAR    |
 * | java.time.Year           | year           | int                    | Types.INTEGER    |
 * | java.time.Instant        | timestamp      | timestamp              | Types.TIMESTAMP  |
 * | java.util.UUID           | uuid           | uuid                   | Types.OTHER      |
 * | kotlin.ByteArray         | bytes          | bytes                  | Types.BINARY     |
 * | kotlin.Enum              | enum           | enum                   | Types.VARCHAR    |
 * +──────────────────────────+────────────────+────────────────────────+──────────────────+
 *
 * @see [org.ktorm.ksp.api.Column]
 * @see [KtormKspConfig]
 */
public sealed interface Converter

/**
 * Specify [T] type to convert to Column type.
 * @see [org.ktorm.ksp.api.Column.converter]
 * @see [KtormKspConfig.singleTypeConverters]
 */
public interface SingleTypeConverter<T : Any> : Converter {

    /**
     * Convert [T] type to Column<T> type.
     */
    public fun convert(table: BaseTable<*>, columnName: String, propertyType: KClass<T>): Column<T>
}

/**
 * Specify any type to convert to Column type.
 * @see [org.ktorm.ksp.api.Column.converter]
 */
public interface MultiTypeConverter : Converter {

    /**
     * Convert [T] type to Column<T> type.
     */
    public fun <T : Any> convert(table: BaseTable<*>, columnName: String, propertyType: KClass<T>): Column<T>
}

/**
 * Specify enum type to convert to Column type.
 * @see [org.ktorm.ksp.api.Column.converter]
 * @see [KtormKspConfig.enumConverter]
 */
public interface EnumConverter : Converter {

    /**
     * Convert [E] type to Column<E> type.
     */
    public fun <E : Enum<E>> convert(table: BaseTable<*>, columnName: String, propertyType: KClass<E>): Column<E>
}
