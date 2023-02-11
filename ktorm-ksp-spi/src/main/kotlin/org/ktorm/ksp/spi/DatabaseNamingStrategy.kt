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
public interface DatabaseNamingStrategy {

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
