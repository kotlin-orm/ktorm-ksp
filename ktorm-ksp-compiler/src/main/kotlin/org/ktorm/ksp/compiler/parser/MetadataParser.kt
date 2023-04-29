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

package org.ktorm.ksp.compiler.parser

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.symbol.ClassKind.*
import org.ktorm.entity.Entity
import org.ktorm.ksp.api.*
import org.ktorm.ksp.compiler.util.*
import org.ktorm.ksp.spi.CodingNamingStrategy
import org.ktorm.ksp.spi.ColumnMetadata
import org.ktorm.ksp.spi.DatabaseNamingStrategy
import org.ktorm.ksp.spi.TableMetadata
import org.ktorm.schema.SqlType
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.jvm.jvmName

@OptIn(KspExperimental::class)
class MetadataParser(_resolver: Resolver, _environment: SymbolProcessorEnvironment) {
    private val resolver = _resolver
    private val options = _environment.options
    private val databaseNamingStrategy = loadDatabaseNamingStrategy()
    private val codingNamingStrategy = loadCodingNamingStrategy()
    private val tablesCache = HashMap<String, TableMetadata>()

    private fun loadDatabaseNamingStrategy(): DatabaseNamingStrategy {
        val name = options["ktorm.dbNamingStrategy"] ?: "lower-snake-case"
        if (name == "lower-snake-case") {
            return LowerSnakeCaseDatabaseNamingStrategy
        }
        if (name == "upper-snake-case") {
            return UpperSnakeCaseDatabaseNamingStrategy
        }

        try {
            val cls = Class.forName(name)
            return (cls.kotlin.objectInstance ?: cls.getDeclaredConstructor().newInstance()) as DatabaseNamingStrategy
        } catch (e: InvocationTargetException) {
            throw e.targetException
        }
    }

    private fun loadCodingNamingStrategy(): CodingNamingStrategy {
        val name = options["ktorm.codingNamingStrategy"] ?: return DefaultCodingNamingStrategy

        try {
            val cls = Class.forName(name)
            return (cls.kotlin.objectInstance ?: cls.getDeclaredConstructor().newInstance()) as CodingNamingStrategy
        } catch (e: InvocationTargetException) {
            throw e.targetException
        }
    }

    fun parseTableMetadata(cls: KSClassDeclaration): TableMetadata {
        val r = tablesCache[cls.qualifiedName!!.asString()]
        if (r != null) {
            return r
        }

        if (cls.classKind != CLASS && cls.classKind != INTERFACE) {
            val name = cls.qualifiedName!!.asString()
            throw IllegalStateException("$name is expected to be a class or interface but actually ${cls.classKind}")
        }

        if (cls.classKind == INTERFACE && !cls.isSubclassOf<Entity<*>>()) {
            val name = cls.qualifiedName!!.asString()
            throw IllegalStateException("$name must extends from org.ktorm.entity.Entity")
        }

        val table = cls.getAnnotationsByType(Table::class).first()
        val tableDef = TableMetadata(
            entityClass = cls,
            name = table.name.ifEmpty { databaseNamingStrategy.getTableName(cls) },
            alias = table.alias.takeIf { it.isNotEmpty() },
            catalog = table.catalog.ifEmpty { options["ktorm.catalog"] }?.takeIf { it.isNotEmpty() },
            schema = table.schema.ifEmpty { options["ktorm.schema"] }?.takeIf { it.isNotEmpty() },
            tableClassName = table.className.ifEmpty { codingNamingStrategy.getTableClassName(cls) },
            entitySequenceName = table.entitySequenceName.ifEmpty { codingNamingStrategy.getEntitySequenceName(cls) },
            ignoreProperties = table.ignoreProperties.toSet(),
            columns = ArrayList()
        )

        for (property in tableDef.getProperties()) {
            if (property.isAnnotationPresent(References::class)) {
                (tableDef.columns as MutableList) += parseRefColumnMetadata(property, tableDef)
            } else {
                (tableDef.columns as MutableList) += parseColumnMetadata(property, tableDef)
            }
        }

        tablesCache[cls.qualifiedName!!.asString()] = tableDef
        return tableDef
    }

    private fun TableMetadata.getProperties(): Sequence<KSPropertyDeclaration> {
        val classKind = entityClass.classKind

        val constructorParams = HashSet<String>()
        if (classKind == CLASS) {
            entityClass.primaryConstructor?.parameters?.mapTo(constructorParams) { it.name!!.asString() }
        }

        return entityClass.getAllProperties()
            .filterNot { it.simpleName.asString() in ignoreProperties }
            .filterNot { it.isAnnotationPresent(Ignore::class) }
            .filterNot { classKind == CLASS && !it.hasBackingField }
            .filterNot { classKind == INTERFACE && !it.isAbstract() }
            .filterNot { classKind == INTERFACE && it.simpleName.asString() in setOf("entityClass", "properties") }
            .sortedByDescending { it.simpleName.asString() in constructorParams }
    }

    private fun parseColumnMetadata(property: KSPropertyDeclaration, table: TableMetadata): ColumnMetadata {
        val column = property.getAnnotationsByType(Column::class).firstOrNull()

        var name = column?.name
        if (name.isNullOrEmpty()) {
            name = databaseNamingStrategy.getColumnName(table.entityClass, property)
        }

        var propertyName = column?.propertyName
        if (propertyName.isNullOrEmpty()) {
            propertyName = codingNamingStrategy.getColumnPropertyName(table.entityClass, property)
        }

        return ColumnMetadata(
            entityProperty = property,
            table = table,
            name = name,
            isPrimaryKey = property.isAnnotationPresent(PrimaryKey::class),
            sqlType = parseColumnSqlType(property),
            isReference = false,
            referenceTable = null,
            columnPropertyName = propertyName
        )
    }

    private fun parseColumnSqlType(property: KSPropertyDeclaration): KSType {
        val annotation = property.annotations.find { anno ->
            val annoType = anno.annotationType.resolve()
            annoType.declaration.qualifiedName?.asString() == Column::class.jvmName
        }

        val argument = annotation?.arguments?.find { it.name?.asString() == Column::sqlType.name }

        var sqlType = argument?.value as KSType?
        if (sqlType?.declaration?.qualifiedName?.asString() == Nothing::class.jvmName) {
            sqlType = null
        }

        if (sqlType == null) {
            sqlType = property.getSqlType(resolver)
        }

        if (sqlType == null) {
            val name = property.qualifiedName?.asString()
            throw IllegalStateException("Cannot infer sqlType for property: $name, please specify manually.")
        }

        val declaration = sqlType.declaration as KSClassDeclaration
        if (declaration.classKind != OBJECT) {
            val name = declaration.qualifiedName?.asString()
            throw IllegalArgumentException("The sqlType class $name must be a Kotlin singleton object.")
        }

        if (!declaration.isSubclassOf<SqlType<*>>() && !declaration.isSubclassOf<SqlTypeFactory>()) {
            val name = declaration.qualifiedName?.asString()
            throw IllegalArgumentException("The sqlType class $name must be subtype of SqlType or SqlTypeFactory.")
        }

        return sqlType
    }

    private fun parseRefColumnMetadata(property: KSPropertyDeclaration, table: TableMetadata): ColumnMetadata {
        if (property.isAnnotationPresent(Column::class)) {
            throw IllegalStateException("@Column and @References cannot use together on the same property: $property")
        }

        if (table.entityClass.classKind != INTERFACE) {
            throw IllegalStateException("@References can only be used on interface-based entities.")
        }

        // TODO: check circular reference.
        val reference = property.getAnnotationsByType(References::class).first()
        val referenceTable = parseTableMetadata(property.type.resolve().declaration as KSClassDeclaration)

        if (referenceTable.entityClass.classKind != INTERFACE) {
            val n = referenceTable.entityClass.qualifiedName?.asString()
            throw IllegalStateException("The referenced entity class ($n) must be an interface.")
        }

        if (!referenceTable.entityClass.isAnnotationPresent(Table::class)) {
            val n = referenceTable.entityClass.qualifiedName?.asString()
            throw IllegalStateException("The referenced entity class ($n) must be annotated with @Table.")
        }

        val primaryKeys = referenceTable.columns.filter { it.isPrimaryKey }
        if (primaryKeys.isEmpty()) {
            throw IllegalStateException("Table `${referenceTable.name}` doesn't have a primary key.")
        }

        if (primaryKeys.size > 1) {
            throw IllegalStateException("Reference table '${referenceTable.name}' cannot have compound primary keys.")
        }

        var name = reference.name
        if (name.isEmpty()) {
            name = databaseNamingStrategy.getRefColumnName(table.entityClass, property, referenceTable)
        }

        var propertyName = reference.propertyName
        if (propertyName.isEmpty()) {
            propertyName = codingNamingStrategy.getRefColumnPropertyName(table.entityClass, property, referenceTable)
        }

        return ColumnMetadata(
            entityProperty = property,
            table = table,
            name = name,
            isPrimaryKey = property.isAnnotationPresent(PrimaryKey::class),
            sqlType = primaryKeys[0].sqlType,
            isReference = true,
            referenceTable = referenceTable,
            columnPropertyName = propertyName
        )
    }
}
