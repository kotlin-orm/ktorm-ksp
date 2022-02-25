package org.ktorm.ksp.compiler.generator

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.ktorm.database.Database
import org.ktorm.dsl.QueryRowSet
import org.ktorm.entity.EntitySequence
import org.ktorm.ksp.compiler.definition.CodeGenerateConfig
import org.ktorm.ksp.compiler.definition.ColumnDefinition
import org.ktorm.ksp.compiler.definition.TableDefinition
import org.ktorm.schema.BaseTable
import org.ktorm.schema.Column
import org.ktorm.schema.Table

public abstract class AbstractTableGenerator(
    context: TableGenerateContext
) {

    protected val bindToFun: MemberName = MemberName("", "bindTo")
    protected val primaryKeyFun: MemberName = MemberName("", "primaryKey")
    protected val ignoreDefinitionProperties: Set<String> = setOf("entityClass", "properties")
    protected abstract val superType: ClassName
    protected val table: TableDefinition = context.table
    protected val logger: KSPLogger = context.logger
    protected val dependencyFiles: MutableSet<KSFile> = context.dependencyFiles
    protected val columnInitializerGenerator: ColumnInitializerGenerator = context.columnInitializerGenerator
    protected val config: CodeGenerateConfig = context.config

    public open fun CodeBlock.Builder.appendTableName() {
        val tableName = when {
            table.tableName.isNotEmpty() -> table.tableName
            config.namingStrategy != null && config.localNamingStrategy != null -> {
                config.localNamingStrategy.toTableName(table.entityClassName.simpleName)
            }
            config.namingStrategy == null -> {
                table.entityClassName.simpleName
            }
            else -> {
                add("tableName=%T.toTableName(%S),", config.namingStrategy, table.entityClassName.simpleName)
                return
            }
        }
        add("tableName=%S,", tableName)
    }

    public open fun generate(): FileSpec {
        val fileBuilder = generateFile()
        val typeBuilder = generateType()
        val properties = generateProperties()
        val functions = generateFunctions()

        typeBuilder.addProperties(properties)
        typeBuilder.addFunctions(functions)
        fileBuilder.addType(typeBuilder.build())

        val topLevelFunctions = generateTopLevelFunctions()
        topLevelFunctions.forEach { fileBuilder.addFunction(it) }
        val topLevelProperties = generateTopLevelProperties()
        topLevelProperties.forEach { fileBuilder.addProperty(it) }

        return fileBuilder.build()
    }

    public open fun generateTopLevelFunctions(): Iterable<FunSpec> {
        return emptyList()
    }

    public open fun generateTopLevelProperties(): Iterable<PropertySpec> {
        //sequence
        val sequenceOf = MemberName("org.ktorm.entity", "sequenceOf", true)
        val tableClassName = table.tableClassName.simpleName
        val sequenceName = tableClassName.substring(0, 1).lowercase() + tableClassName.substring(1)
        val entitySequence = EntitySequence::class.asClassName()
        //EntitySequence<E, T>
        val sequenceType = entitySequence.parameterizedBy(table.entityClassName, table.tableClassName)
        val sequenceProperty = PropertySpec.builder(sequenceName, sequenceType)
            .receiver(Database::class.asClassName())
            .getter(
                FunSpec.getterBuilder()
                    .addStatement("return this.%M(%T)", sequenceOf, table.tableClassName)
                    .build()
            )
            .build()
        return listOf(sequenceProperty)
    }

    public abstract fun generateType(): TypeSpec.Builder

    public open fun generateFunctions(): Iterable<FunSpec> {
        return emptyList()
    }

    public open fun generateProperties(): Iterable<PropertySpec> {
        return table.columns.filter { it.property.simpleName !in ignoreDefinitionProperties }
            .map { generateProperty(it).build() }
    }

    public open fun generateProperty(column: ColumnDefinition): PropertySpec.Builder {
        val ktormColumn = Column::class.asClassName()
        val propertyType = column.propertyTypeName.copy(nullable = false)
        val initializer = generatePropertyInitializer(column, propertyType)
        return PropertySpec.builder(column.property.simpleName, ktormColumn.parameterizedBy(propertyType))
            .initializer(initializer)
    }

    public open fun generateFile(): FileSpec.Builder {
        return FileSpec.builder(table.tableClassName.packageName, table.tableClassName.simpleName)
    }

    public abstract fun generatePropertyInitializer(column: ColumnDefinition, propertyType: TypeName): CodeBlock
}

/**
 * generate fileSpec for [Table]
 */
public open class TableGenerator(context: TableGenerateContext) : AbstractTableGenerator(context) {
    override val superType: ClassName = Table::class.asClassName()

    override fun generateType(): TypeSpec.Builder {
        return TypeSpec.objectBuilder(table.tableClassName).superclass(superType.parameterizedBy(table.entityClassName))
            .addSuperclassConstructorParameter(buildCodeBlock {
                appendTableName()
                add("alias=%S,", table.alias)
                add("catalog=%S,", table.catalog)
                add("schema=%S,", table.schema)
                add("entityClass=%T::class,", table.entityClassName)
            })
    }

    public override fun generatePropertyInitializer(column: ColumnDefinition, propertyType: TypeName): CodeBlock {
        val columnFunction = columnInitializerGenerator.generate(column, dependencyFiles, config)
        return CodeBlock.builder().add(columnFunction)
            .addStatement(".%M { it.%M }", bindToFun, column.property)
            .apply { if (column.isPrimaryKey) addStatement(".%M()", primaryKeyFun) }.build()
    }
}

/**
 * generate fileSpec for [BaseTable]
 */
public open class BaseTableGenerator(context: TableGenerateContext) : AbstractTableGenerator(context) {
    override val superType: ClassName = BaseTable::class.asClassName()

    public companion object {
        private val hashMapClassName = HashMap::class.asClassName()
        private val kParameter = ClassName("kotlin.reflect", "KParameter")
        private val any = ClassName("kotlin", "Any")
        private val primaryConstructor = MemberName("kotlin.reflect.full", "primaryConstructor", true)
    }

    override fun generateType(): TypeSpec.Builder {
        return TypeSpec.objectBuilder(table.tableClassName)
            .superclass(BaseTable::class.asClassName().parameterizedBy(table.entityClassName))
            .addSuperclassConstructorParameter(buildCodeBlock {
                appendTableName()
                add("alias=%S, ", table.alias)
                add("catalog=%S, ", table.catalog)
                add("schema=%S, ", table.schema)
                add("entityClass=%T::class", table.entityClassName)
            })
    }

    public override fun generatePropertyInitializer(column: ColumnDefinition, propertyType: TypeName): CodeBlock {
        val columnFunction = columnInitializerGenerator.generate(column, dependencyFiles, config)
        return buildCodeBlock {
            add(columnFunction)
            if (column.isPrimaryKey) addStatement(".%M()", primaryKeyFun)
        }
    }

    override fun generateFunctions(): Iterable<FunSpec> {
        val row = "row"
        val withReferences = "withReferences"
        val createFun =
            FunSpec.builder("doCreateEntity").addModifiers(KModifier.OVERRIDE).returns(table.entityClassName)
                .addParameter(row, QueryRowSet::class.asTypeName())
                .addParameter(withReferences, Boolean::class.asTypeName()).addCode(buildCodeBlock {
                    val entityClassDeclaration = table.entityClassDeclaration
                    val constructor = entityClassDeclaration.primaryConstructor!!
                    val constructorParameter = constructor.parameters
                    val nonStructuralProperties = table.columns.map { it.property.simpleName }.toMutableSet()
                    // propertyName -> columnMember
                    val columnMap = table.columns.associateBy { it.property.simpleName }
                    val unknownParameters =
                        constructor.parameters.filter { !it.hasDefault && it.name?.asString() !in nonStructuralProperties }
                    if (unknownParameters.isNotEmpty()) {
                        error("unknown constructor parameter for ${table.entityClassName.canonicalName} : ${unknownParameters.map { it.name?.asString() }}")
                    }
                    if (config.allowReflectionCreateEntity && constructorParameter.any { it.hasDefault }) {
                        addStatement("val constructor = %T::class.%M!!", table.entityClassName, primaryConstructor)
                        addStatement(
                            "val parameterMap = %T<%T,%T?>(%L)",
                            hashMapClassName,
                            kParameter,
                            any,
                            constructorParameter.size
                        )
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
                        addStatement("val instance = constructor.callBy(parameterMap)", table.entityClassName)
                    } else {
                        // Create instance with code when construct has no default value parameter
                        add("val instance = %T(", table.entityClassName)
                        logger.info("constructorParameter:${constructorParameter.map { it.name!!.asString() }}")
                        for (parameter in constructorParameter) {
                            val column =
                                table.columns.firstOrNull { it.property.simpleName == parameter.name!!.asString() }
                                    ?: error("Construct parameter not exists in table: ${parameter.name!!.asString()}")

                            val isNullable = column.propertyTypeName.isNullable
                            val notNullOperator = if (isNullable) "" else "!!"
                            add(
                                "%L = %L[%M]%L,",
                                parameter.name!!.asString(),
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
                }).build()
        return listOf(createFun)
    }
}