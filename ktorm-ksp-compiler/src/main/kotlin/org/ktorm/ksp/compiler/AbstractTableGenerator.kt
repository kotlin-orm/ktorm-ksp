package org.ktorm.ksp.compiler

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.ktorm.dsl.QueryRowSet
import org.ktorm.schema.BaseTable
import org.ktorm.schema.Column
import org.ktorm.schema.Table

public abstract class AbstractTableGenerator(
    public val table: TableDefinition,
    public val configuration: KtormKspConfiguration
) {

    protected val bindToFun: MemberName = MemberName("", "bindTo")
    protected val primaryKeyFun: MemberName = MemberName("", "primaryKey")
    protected val ignoreDefinitionProperties: Set<String> = setOf("entityClass", "properties")
    protected val columnFunctionGenerator: ColumnFunctionGenerator = ColumnFunctionGenerator()
    protected abstract val superType: ClassName


    public open fun generate(): FileSpec {
        val fileBuilder = generateFile()
        val typeBuilder = generateType()
        val properties = generateProperties()
        val functions = generateFunctions()

        typeBuilder.addProperties(properties)
        typeBuilder.addFunctions(functions)
        fileBuilder.addType(typeBuilder.build())
        return fileBuilder.build()
    }

    public abstract fun generateType(): TypeSpec.Builder

    public open fun generateFunctions(): Iterable<FunSpec> {
        return emptyList()
    }

    public open fun generateProperties(): Iterable<PropertySpec> {
        return table.columns
            .filter { it.property.simpleName !in ignoreDefinitionProperties }
            .map { generateProperty(it).build() }
    }

    public open fun generateProperty(column: ColumnDefinition): PropertySpec.Builder {
        val ktormColumn = Column::class.asClassName()
        val (columnName, _, _, propertyTypeName, _) = column
        val propertyType = propertyTypeName.copy(nullable = false)
        val initializer = generatePropertyInitializer(column, propertyType)
        return PropertySpec.builder(columnName, ktormColumn.parameterizedBy(propertyType)).initializer(initializer)
    }

    public open fun generateFile(): FileSpec.Builder {
        return FileSpec.builder(table.tableClassName.packageName, table.tableClassName.simpleName)
    }

    public abstract fun generatePropertyInitializer(column: ColumnDefinition, propertyType: TypeName): CodeBlock
}

/**
 * generate typeSpec for [Table]
 */
public open class TableGenerator(table: TableDefinition, configuration: KtormKspConfiguration) :
    AbstractTableGenerator(table, configuration) {
    override val superType: ClassName = Table::class.asTypeName()

    override fun generateType(): TypeSpec.Builder {
        return TypeSpec.objectBuilder(table.tableClassName)
            .superclass(superType.parameterizedBy(table.entityClassName))
            .addSuperclassConstructorParameter(
                CodeBlock
                    .builder()
                    .add("tableName=%S,", table.tableName)
                    .add("alias=%S,", table.alias)
                    .add("catalog=%S,", table.catalog)
                    .add("schema=%S,", table.schema)
                    .add("entityClass=%T::class,", table.entityClassName)
                    .build()
            )
    }

    public override fun generatePropertyInitializer(column: ColumnDefinition, propertyType: TypeName): CodeBlock {
        val (columnName, isPrimaryKey, _, _, property) = column
        val columnFunction = columnFunctionGenerator.genrate(property, propertyType)
        val params = mapOf(
            "columnName" to columnName,
            "bindTo" to bindToFun,
            "primaryKey" to primaryKeyFun
        )
        return CodeBlock.builder()
            .add(columnFunction)
            .addNamed(".%bindTo:M { it.%columnName:L }", params)
            .apply { if (isPrimaryKey) addNamed(".%primaryKey:M()", params) }
            .build()
    }
}

/**
 * generate typeSpec for [BaseTable]
 */
public open class BaseTableGenerator(table: TableDefinition, configuration: KtormKspConfiguration) :
    AbstractTableGenerator(table, configuration) {
    override val superType: ClassName = BaseTable::class.asTypeName()

    public companion object {
        private val mutableMapOfFun = MemberName("kotlin.collections", "mutableMapOf", false)
        private val kParameter = ClassName("kotlin.reflect", "KParameter")
        private val any = ClassName("kotlin", "Any")
        private val primaryConstructor = MemberName("kotlin.reflect.full", "primaryConstructor", true)
    }


    override fun generateType(): TypeSpec.Builder {
        return TypeSpec.objectBuilder(table.tableClassName)
            .superclass(superType.parameterizedBy(table.entityClassName))
            .addSuperclassConstructorParameter(
                CodeBlock
                    .builder()
                    .add("tableName=%S, ", table.tableName)
                    .add("alias=%S, ", table.alias)
                    .add("catalog=%S, ", table.catalog)
                    .add("schema=%S, ", table.schema)
                    .add("entityClass=%T::class", table.entityClassName)
                    .build()
            )
    }

    public override fun generatePropertyInitializer(column: ColumnDefinition, propertyType: TypeName): CodeBlock {
        val (columnName, isPrimaryKey, _, _, property) = column
        val columnFunction = columnFunctionGenerator.genrate(property, propertyType)
        val params = mapOf(
            "columnName" to columnName,
            "bindTo" to bindToFun,
            "primaryKey" to primaryKeyFun
        )
        return CodeBlock.builder()
            .add(columnFunction)
            .apply { if (isPrimaryKey) addNamed(".%primaryKey:M()", params) }
            .build()
    }

    override fun generateFunctions(): Iterable<FunSpec> {
        val row = "row"
        val withReferences = "withReferences"
        val createFun = FunSpec.builder("doCreateEntity")
            .addModifiers(KModifier.OVERRIDE)
            .returns(table.entityClassName)
            .addParameter(row, QueryRowSet::class.asTypeName())
            .addParameter(withReferences, Boolean::class.asTypeName())
            .addCode(
                CodeBlock.builder()
                    .apply {
                        val entityClassDeclaration = table.entityClassDeclaration
                        val constructor = entityClassDeclaration.primaryConstructor!!
                        val constructorParameter = constructor.parameters
                        val nonStructuralProperties = table.columns.map { it.property.simpleName }.toMutableSet()
                        // propertyName -> columnMember
                        val columnMap = table.columns.associateBy { it.property.simpleName }
                        val unknownParameters =
                            constructor.parameters.filter { !it.hasDefault && it.name?.asString() !in nonStructuralProperties }
                        if (unknownParameters.isNotEmpty()) {
                            throw IllegalArgumentException("unknown constructor parameter for ${table.entityClassName.canonicalName} : ${unknownParameters.map { it.name?.asString() }}")
                        }
                        addStatement("val instance: %T", table.entityClassName)
                        if (configuration.allowReflectionCreateEntity && constructor.parameters.any { it.hasDefault }) {
                            addStatement("val constructor = %T::class.%M!!", table.entityClassName, primaryConstructor)
                            addStatement("val parameterMap = %M<%T,%T?>()", mutableMapOfFun, kParameter, any)
                            beginControlFlow("for (parameter in constructor.parameters)")
                            beginControlFlow("when(parameter.name)")
                            for (parameter in constructor.parameters) {
                                val parameterName = parameter.name!!.asString()
                                beginControlFlow("%S -> ", parameterName)
                                val column = columnMap[parameterName]!!
                                addStatement("val value = %L[%M]", row, column.property)
                                // hasDefault
                                if (parameter.hasDefault) {
                                    beginControlFlow("if (value != null)")
                                    addStatement("parameterMap[parameter] = value")
                                    endControlFlow()
                                } else {
                                    val isNullable = column.propertyTypeName.isNullable
                                    val notNullOperator = if (isNullable) "" else "!!"
                                    addStatement("parameterMap[parameter] = value%L", notNullOperator)
                                }
                                endControlFlow()
                                nonStructuralProperties.remove(parameterName)
                            }
                            endControlFlow()
                            endControlFlow()
                            addStatement(
                                "instance = constructor.callBy(parameterMap)",
                                table.entityClassName,
                                primaryConstructor
                            )
                        } else {
                            // Create instance with code when construct has no default value parameter
                            add("instance = %T(", table.entityClassName)
                            for (parameter in constructorParameter) {
                                val column =
                                    table.columns.firstOrNull { it.property.simpleName == parameter.name!!.asString() }
                                        ?: throw IllegalStateException("Construct parameter not exists in table: ${parameter.name!!.asString()}")

                                val isNullable = column.propertyTypeName.isNullable
                                val notNullOperator = if (isNullable) "" else "!!"
                                addStatement(
                                    "%L = %L[%M]%L,",
                                    parameter.name,
                                    row,
                                    MemberName(table.tableClassName, column.property.simpleName),
                                    notNullOperator
                                )
                                nonStructuralProperties.remove(column.property.simpleName)
                            }
                            addStatement(")")
                        }
                        //non-structural property
                        for (property in nonStructuralProperties) {
                            val column = columnMap[property]!!
                            if (!column.propertyDeclaration.isMutable) {
                                continue
                            }
                            val isNullable = column.propertyTypeName.isNullable
                            val notNullOperator = if (isNullable) "" else "!!"
                            addStatement(
                                "instance.%L = %L[%M]%L",
                                property,
                                row,
                                MemberName(table.tableClassName, column.property.simpleName),
                                notNullOperator
                            )
                        }
                        addStatement("return instance")
                    }
                    .build()
            )
            .build()
        return listOf(createFun)
    }

}