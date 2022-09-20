/*
 * Copyright 2022 the original author or authors.
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

import org.ktorm.entity.Entity
import org.ktorm.schema.BaseTable

/**
 * The specified class is an entity, and ksp will generate the corresponding [org.ktorm.schema.Table] or [BaseTable]
 * object This class can be an interface based on [Entity] and will generate [org.ktorm.schema.Table] objects.
 * It can also be a normal class/data class that will generate a [BaseTable] object
 *
 * For the above two different entity classes, the generated Table will also be different.
 * Among them, the [BaseTable] object generated based on the class will generate the [BaseTable.createEntity] method
 * implementation by default. And based on the [Entity] interface implementation [org.ktorm.schema.Table] does not
 * generate an implementation of this method. The generated column property definitions are also different.
 * For details, please refer to the description in [Column].
 *
 * By default, the class name of [BaseTable] or [org.ktorm.schema.Table] is generated, and the plural of nouns
 * is converted based on the class name. The generated class name can be modified by assigning the [tableClassName]
 * property
 *
 * @see BaseTable
 * @see org.ktorm.schema.Table
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
public annotation class Table(

    /**
     * Table names in SQL, Corresponds to [org.ktorm.schema.BaseTable.tableName] property, If the value is an
     * empty string, the default value will be used. The [KtormKspConfig.namingStrategy] property can affect the
     * default table name generation rules, but the [tableName] property has the highest priority
     */
    val tableName: String = "",

    /**
     * Specifies the class name of the generated table class. By default, the noun plural is converted from
     * the class name of the entity class.
     */
    val tableClassName: String = "",

    /**
     * Specify the table alias, corresponding to the [BaseTable.alias] property.
     */
    val alias: String = "",

    /**
     * Specify the table catalog, corresponding to the [BaseTable.catalog] property.
     */
    val catalog: String = "",

    /**
     * Specify the table schema, corresponding to the [BaseTable.schema] property.
     */
    val schema: String = "",

    /**
     * Specifies to ignore properties that do not generate column definitions.
     */
    val ignoreColumns: Array<String> = [],

    /**
     * Specify the sequence name，By default, the first character lowercase of the [tableClassName].
     */
    val sequenceName: String = "",
)
