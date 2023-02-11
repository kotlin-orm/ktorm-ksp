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

package org.ktorm.ksp.spi

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import org.ktorm.ksp.spi.definition.TableDefinition

/**
 * Naming strategy for the generated code.
 */
public interface NamingStrategy {

    /**
     * Generate the table name.
     */
    public fun getTableName(cls: KSClassDeclaration): String

    /**
     * Generate the column name.
     */
    public fun getColumnName(cls: KSClassDeclaration, property: KSPropertyDeclaration): String

    /**
     * Generate the reference column name.
     */
    public fun getRefColumnName(cls: KSClassDeclaration, property: KSPropertyDeclaration, referenceTable: TableDefinition): String
}

/**
 * Generating lower snake-case names.
 */
public object LowerSnakeCaseNamingStrategy : NamingStrategy {

    override fun getTableName(cls: KSClassDeclaration): String {
        return cls.simpleName.asString().toSnakeCase()
    }

    override fun getColumnName(cls: KSClassDeclaration, property: KSPropertyDeclaration): String {
        return property.simpleName.asString().toSnakeCase()
    }

    override fun getRefColumnName(cls: KSClassDeclaration, property: KSPropertyDeclaration, referenceTable: TableDefinition): String {
        return property.simpleName.asString().toSnakeCase()
    }

    private fun String.toSnakeCase(): String {
        return replace(Regex("([a-z])([A-Z])"), "$1_$2").replace(Regex("([A-Z])([A-Z][a-z])"), "$1_$2").lowercase()
    }
}

/**
 * Generating upper snake-case names.
 */
public object UpperSnakeCaseNamingStrategy : NamingStrategy {

    override fun getTableName(cls: KSClassDeclaration): String {
        return cls.simpleName.asString().toSnakeCase()
    }

    override fun getColumnName(cls: KSClassDeclaration, property: KSPropertyDeclaration): String {
        return property.simpleName.asString().toSnakeCase()
    }

    override fun getRefColumnName(cls: KSClassDeclaration, property: KSPropertyDeclaration): String {
        return property.simpleName.asString().toSnakeCase()
    }

    private fun String.toSnakeCase(): String {
        return replace(Regex("([a-z])([A-Z])"), "$1_$2").replace(Regex("([A-Z])([A-Z][a-z])"), "$1_$2").lowercase()
    }
}
