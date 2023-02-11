package org.ktorm.ksp.compiler

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import org.ktorm.entity.Entity
import org.ktorm.ksp.api.*
import org.ktorm.ksp.compiler.util.DefaultCodingNamingStrategy
import org.ktorm.ksp.compiler.util.LowerSnakeCaseDatabaseNamingStrategy
import org.ktorm.ksp.compiler.util.isSubclassOf
import org.ktorm.ksp.spi.definition.ColumnDefinition
import org.ktorm.ksp.spi.definition.TableDefinition
import org.ktorm.schema.SqlType
import kotlin.reflect.jvm.jvmName

@OptIn(KspExperimental::class)
class KtormProcessor(environment: SymbolProcessorEnvironment) : SymbolProcessor {
    private val logger = environment.logger
    private val options = environment.options
    private val codeGenerator = environment.codeGenerator
    private val databaseNamingStrategy = LowerSnakeCaseDatabaseNamingStrategy
    private val codingNamingStrategy = DefaultCodingNamingStrategy
    private val tablesCache = HashMap<String, TableDefinition>()

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
        val r = tablesCache[cls.qualifiedName!!.asString()]
        if (r != null) {
            return r
        }

        if (cls.classKind != ClassKind.CLASS && cls.classKind != ClassKind.INTERFACE) {
            val name = cls.qualifiedName!!.asString()
            throw IllegalStateException("$name is expected to be a class or interface but actually ${cls.classKind}")
        }

        if (cls.classKind == ClassKind.INTERFACE && !cls.isSubclassOf<Entity<*>>()) {
            val name = cls.qualifiedName!!.asString()
            throw IllegalStateException("$name must extends from org.ktorm.entity.Entity")
        }

        val table = cls.getAnnotationsByType(Table::class).first()
        val tableDef = TableDefinition(
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

        for (property in cls.getAllProperties()) {
            if (shouldSkip(property, tableDef)) {
                continue
            }

            if (property.isAnnotationPresent(References::class)) {
                (tableDef.columns as MutableList) += parseRefColumnDefinition(property, tableDef)
            } else {
                (tableDef.columns as MutableList) += parseColumnDefinition(property, tableDef)
            }
        }

        tablesCache[cls.qualifiedName!!.asString()] = tableDef
        return tableDef
    }

    private fun shouldSkip(property: KSPropertyDeclaration, table: TableDefinition): Boolean {
        val propertyName = property.simpleName.asString()
        if (propertyName in table.ignoreProperties) {
            return true
        }

        if (property.isAnnotationPresent(Ignore::class)) {
            return true
        }

        if (table.entityClass.classKind == ClassKind.CLASS && !property.hasBackingField) {
            return true
        }

        if (table.entityClass.classKind == ClassKind.INTERFACE && propertyName in setOf("entityClass", "properties")) {
            return true
        }

        // TODO: skip non-abstract properties for interface-based entities.
        return false
    }

    private fun parseColumnDefinition(property: KSPropertyDeclaration, table: TableDefinition): ColumnDefinition {
        val column = property.getAnnotationsByType(Column::class).firstOrNull()

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
            name = (column?.name ?: "").ifEmpty { databaseNamingStrategy.getColumnName(table.entityClass, property) },
            isPrimaryKey = property.isAnnotationPresent(PrimaryKey::class),
            sqlType = sqlType,
            isReference = false,
            referenceTable = null,
            tablePropertyName = (column?.propertyName ?: "").ifEmpty { codingNamingStrategy.getColumnPropertyName(table.entityClass, property) }
        )
    }

    private fun parseRefColumnDefinition(property: KSPropertyDeclaration, table: TableDefinition): ColumnDefinition {
        val column = property.getAnnotationsByType(Column::class).firstOrNull()
        if (column != null) {
            throw IllegalStateException("@Column and @References cannot use together on the same property: $property")
        }

        val reference = property.getAnnotationsByType(References::class).first()
        // TODO: check circular reference.
        val referenceTable = parseTableDefinition(property.type.resolve().declaration as KSClassDeclaration)

        if (table.entityClass.classKind != ClassKind.INTERFACE) {
            throw IllegalStateException("@References can only be used on interface-based entities.")
        }

        if (referenceTable.entityClass.classKind != ClassKind.INTERFACE) {
            val name = referenceTable.entityClass.qualifiedName?.asString()
            throw IllegalStateException("The referenced entity class ($name) should be an interface.")
        }

        // TODO: check if referenced class is marked with @Table (递归)
        // TODO: check if the referenced table has only one primary key.

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
            name = reference.name.ifEmpty { databaseNamingStrategy.getRefColumnName(table.entityClass, property, referenceTable) },
            isPrimaryKey = property.isAnnotationPresent(PrimaryKey::class),
            sqlType = sqlType,
            isReference = true,
            referenceTable = referenceTable,
            tablePropertyName = reference.propertyName.ifEmpty { codingNamingStrategy.getRefColumnPropertyName(table.entityClass, property, referenceTable) }
        )
    }
}
