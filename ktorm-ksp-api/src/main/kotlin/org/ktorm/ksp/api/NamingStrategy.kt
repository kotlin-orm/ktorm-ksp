/*
 * Copyright 2022-2023 the original author or authors.
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

/**
 * Mapping entity classes and their properties to tableName and columnName in SQL.
 */
public interface NamingStrategy {

    /**
     * Convert entity class name to table name in SQL, The table name will be set to the tableName property
     * in the [BaseTable].
     * @see [BaseTable.tableName]
     * @param entityClassName the entity class name
     * @return table name in SQL
     */
    public fun toTableName(entityClassName: String): String

    /**
     * Convert the property name in the entity class to the column name in SQL, The column name will be set in the
     * name property in [org.ktorm.schema.Column].
     * @see [org.ktorm.schema.Column.name]
     * @param propertyName the property name in the entity class
     * @return column name in SQL
     */
    public fun toColumnName(propertyName: String): String
}

/**
 * Lowercase underscore camel case naming style.
 *
 * Example:
 * User -> user
 * UserProfile -> user_profile
 * UserProfileVisibility -> user_profile_visibility
 */
public object CamelCaseToSnakeCaseNamingStrategy : NamingStrategy {

    override fun toTableName(entityClassName: String): String {
        return entityClassName.camelCase()
    }

    override fun toColumnName(propertyName: String): String {
        return propertyName.camelCase()
    }

    private fun String.camelCase(): String {
        return replace(Regex("([a-z])([A-Z])"), "$1_$2").replace(Regex("([A-Z])([A-Z][a-z])"), "$1_$2").lowercase()
    }
}
