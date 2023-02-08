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

package org.ktorm.ksp.spi.definition

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import org.ktorm.entity.Entity
import org.ktorm.ksp.api.Table
import org.ktorm.ksp.spi.isSubclassOf

/**
 * Table definition metadata.
 */
@OptIn(KspExperimental::class)
public class TableDefinition(_class: KSClassDeclaration, _columns: List<ColumnDefinition>) {
    private val table = _class.getAnnotationsByType(Table::class).first()

    /**
     * The annotated entity class of the table.
     */
    public val entityClass: KSClassDeclaration = _class

    /**
     * The name of the table.
     */
    public val name: String? = table.name.takeIf { it.isNotEmpty() }

    /**
     * The alias of the table.
     */
    public val alias: String? = table.alias.takeIf { it.isNotEmpty() }

    /**
     * The catalog of the table.
     */
    public val catalog: String? = table.catalog.takeIf { it.isNotEmpty() }

    /**
     * The schema of the table.
     */
    public val schema: String? = table.schema.takeIf { it.isNotEmpty() }

    /**
     * The name of the corresponding table class in the generated code.
     */
    public val tableClassName: String? = table.className.takeIf { it.isNotEmpty() }

    /**
     * The name of the corresponding entity sequence in the generated code.
     */
    public val entitySequenceName: String? = table.entitySequenceName.takeIf { it.isNotEmpty() }

    /**
     * Specify properties that should be ignored for generating column definitions.
     */
    public val ignoreProperties: Set<String> = table.ignoreProperties.toSet()

    /**
     * Columns in the table.
     */
    public val columns: List<ColumnDefinition> = _columns

    /**
     * Validate arguments.
     */
    init {
        if (_class.classKind != ClassKind.CLASS && _class.classKind != ClassKind.INTERFACE) {
            val name = _class.qualifiedName?.asString()
            throw IllegalStateException("$name is expected to be a class or interface but actually ${_class.classKind}")
        }

        if (_class.classKind == ClassKind.INTERFACE && !_class.isSubclassOf<Entity<*>>()) {
            val name = _class.qualifiedName?.asString()
            throw IllegalStateException("$name must extends from org.ktorm.entity.Entity")
        }
    }
}
