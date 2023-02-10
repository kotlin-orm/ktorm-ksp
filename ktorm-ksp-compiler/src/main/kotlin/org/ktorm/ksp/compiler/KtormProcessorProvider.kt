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

package org.ktorm.ksp.compiler

import com.google.devtools.ksp.*
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.*
import org.ktorm.entity.Entity
import org.ktorm.ksp.api.*
import org.ktorm.ksp.spi.definition.ColumnDefinition
import org.ktorm.ksp.spi.definition.TableDefinition
import org.ktorm.schema.SqlType
import kotlin.reflect.jvm.jvmName

class KtormProcessorProvider : SymbolProcessorProvider {

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return KtormProcessor(environment)
    }
}

@OptIn(KspExperimental::class)
class KtormProcessor(environment: SymbolProcessorEnvironment) : SymbolProcessor {
    private val logger = environment.logger
    private val options = environment.options
    private val codeGenerator = environment.codeGenerator

    override fun process(resolver: Resolver): List<KSAnnotated> {
        logger.info("Starting ktorm ksp processor.")
        val (symbols, deferral) = resolver.getSymbolsWithAnnotation(Table::class.jvmName).partition { it.validate() }

        val tables = symbols
            .filterIsInstance<KSClassDeclaration>()
            .map { entityClass ->
                parseTableDefinition(entityClass)
            }

        // KtormCodeGenerator.generate(tableDefinitions, environment.codeGenerator, config, logger)
        return deferral
    }

    private fun parseTableDefinition(cls: KSClassDeclaration): TableDefinition {
        if (cls.classKind != ClassKind.CLASS && cls.classKind != ClassKind.INTERFACE) {
            val name = cls.qualifiedName?.asString()
            throw IllegalStateException("$name is expected to be a class or interface but actually ${cls.classKind}")
        }

        if (cls.classKind == ClassKind.INTERFACE && !cls.isSubclassOf<Entity<*>>()) {
            val name = cls.qualifiedName?.asString()
            throw IllegalStateException("$name must extends from org.ktorm.entity.Entity")
        }

        val table = cls.getAnnotationsByType(Table::class).first()
        val tableDef = TableDefinition(
            entityClass = cls,
            name = table.name.takeIf { it.isNotEmpty() },
            alias = table.alias.takeIf { it.isNotEmpty() },
            catalog = table.catalog.takeIf { it.isNotEmpty() },
            schema = table.schema.takeIf { it.isNotEmpty() },
            tableClassName = table.className.takeIf { it.isNotEmpty() },
            entitySequenceName = table.entitySequenceName.takeIf { it.isNotEmpty() },
            ignoreProperties = table.ignoreProperties.toSet(),
            columns = ArrayList()
        )

        for (property in cls.getAllProperties()) {
            val propertyName = property.simpleName.asString()
            if (propertyName in tableDef.ignoreProperties) {
                continue
            }

            if (property.isAnnotationPresent(Ignore::class)) {
                continue
            }

            if (cls.classKind == ClassKind.CLASS && !property.hasBackingField) {
                continue
            }

            if (cls.classKind == ClassKind.INTERFACE && propertyName in setOf("entityClass", "properties")) {
                continue
            }

            // TODO: skip non-abstract properties for interface-based entities.
            (tableDef.columns as MutableList) += parseColumnDefinition(property, tableDef)
        }

        return tableDef
    }

    private fun parseColumnDefinition(property: KSPropertyDeclaration, table: TableDefinition): ColumnDefinition {
        val column = property.getAnnotationsByType(Column::class).firstOrNull()
        val reference = property.getAnnotationsByType(References::class).firstOrNull()

        if (column != null && reference != null) {
            throw IllegalStateException("@Column and @References cannot use together on the same property: $property")
        }

        var referenceTable: TableDefinition? = null
        if (reference != null) {
            // TODO: check circular reference.
            referenceTable = parseTableDefinition(property.type.resolve().declaration as KSClassDeclaration)

            if (table.entityClass.classKind != ClassKind.INTERFACE) {
                throw IllegalStateException("@References can only be used on interface-based entities.")
            }

            if (referenceTable.entityClass.classKind != ClassKind.INTERFACE) {
                val name = referenceTable.entityClass.qualifiedName?.asString()
                throw IllegalStateException("The referenced entity class ($name) should be an interface.")
            }

            // TODO: check if referenced class is marked with @Table (递归)
            // TODO: check if the referenced table has only one primary key.
        }

        val sqlType = property.annotations
            .find { anno -> anno.annotationType.resolve().declaration.qualifiedName?.asString() == Column::class.jvmName }
            ?.let { anno ->
                val argument = anno.arguments.find { it.name?.asString() == Column::sqlType.name }
                val sqlType = argument?.value as KSType?
                sqlType?.takeIf { it.declaration.qualifiedName?.asString() != Nothing::class.jvmName }
            }

        if (sqlType != null) {
            val declaration = sqlType.declaration as KSClassDeclaration
            if (declaration.classKind != ClassKind.OBJECT) {
                val name = declaration.qualifiedName?.asString()
                throw IllegalArgumentException("The sqlType class $name must be a Kotlin singleton object.")
            }

            if (!declaration.isSubclassOf<SqlType<*>>() && !declaration.isSubclassOf<SqlTypeFactory>()) {
                val name = declaration.qualifiedName?.asString()
                throw IllegalArgumentException("The sqlType class $name must be subtype of SqlType or SqlTypeFactory.")
            }
        }

        return ColumnDefinition(
            entityProperty = property,
            table = table,
            name = (column?.name ?: reference?.name ?: "").takeIf { it.isNotEmpty() },
            isPrimaryKey = property.isAnnotationPresent(PrimaryKey::class),
            sqlType = sqlType,
            isReference = reference != null,
            referenceTable = referenceTable,
            tablePropertyName = (column?.propertyName ?: reference?.propertyName ?: "").takeIf { it.isNotEmpty() }
        )
    }
}
