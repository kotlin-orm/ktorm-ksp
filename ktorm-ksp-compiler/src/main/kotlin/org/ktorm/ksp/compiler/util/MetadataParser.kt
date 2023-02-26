package org.ktorm.ksp.compiler.util

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.*
import org.ktorm.entity.Entity
import org.ktorm.ksp.api.*
import org.ktorm.ksp.spi.CodingNamingStrategy
import org.ktorm.ksp.spi.ColumnMetadata
import org.ktorm.ksp.spi.DatabaseNamingStrategy
import org.ktorm.ksp.spi.TableMetadata
import org.ktorm.schema.SqlType
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

        val cls = Class.forName(name)
        return (cls.kotlin.objectInstance ?: cls.newInstance()) as DatabaseNamingStrategy
    }

    private fun loadCodingNamingStrategy(): CodingNamingStrategy {
        val name = options["ktorm.codingNamingStrategy"]
        if (name == null) {
            return DefaultCodingNamingStrategy
        } else {
            val cls = Class.forName(name)
            return (cls.kotlin.objectInstance ?: cls.newInstance()) as CodingNamingStrategy
        }
    }

    fun parseTableMetadata(cls: KSClassDeclaration): TableMetadata {
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

        for (property in cls.getAllProperties()) {
            if (shouldSkip(property, tableDef)) {
                continue
            }

            if (property.isAnnotationPresent(References::class)) {
                (tableDef.columns as MutableList) += parseRefColumnMetadata(property, tableDef)
            } else {
                (tableDef.columns as MutableList) += parseColumnMetadata(property, tableDef)
            }
        }

        tablesCache[cls.qualifiedName!!.asString()] = tableDef
        return tableDef
    }

    private fun shouldSkip(property: KSPropertyDeclaration, table: TableMetadata): Boolean {
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
            throw IllegalStateException("Cannot infer sqlType for property: $property, please specify manually.")
        }

        val declaration = sqlType.declaration as KSClassDeclaration
        if (declaration.classKind != ClassKind.OBJECT) {
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

        if (table.entityClass.classKind != ClassKind.INTERFACE) {
            throw IllegalStateException("@References can only be used on interface-based entities.")
        }

        // TODO: check circular reference.
        val reference = property.getAnnotationsByType(References::class).first()
        val referenceTable = parseTableMetadata(property.type.resolve().declaration as KSClassDeclaration)

        var name = reference.name
        if (name.isEmpty()) {
            name = databaseNamingStrategy.getRefColumnName(table.entityClass, property, referenceTable)
        }

        var propertyName = reference.propertyName
        if (propertyName.isEmpty()) {
            propertyName = codingNamingStrategy.getRefColumnPropertyName(table.entityClass, property, referenceTable)
        }

        if (referenceTable.entityClass.classKind != ClassKind.INTERFACE) {
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
