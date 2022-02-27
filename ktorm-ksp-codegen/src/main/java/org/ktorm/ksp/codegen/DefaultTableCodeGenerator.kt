package org.ktorm.ksp.codegen

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.ktorm.database.Database
import org.ktorm.dsl.QueryRowSet
import org.ktorm.entity.EntitySequence
import org.ktorm.ksp.codegen.definition.KtormEntityType
import org.ktorm.ksp.codegen.definition.TableDefinition
import org.ktorm.schema.BaseTable
import org.ktorm.schema.Column
import org.ktorm.schema.Table

public open class DefaultTableTypeGenerator : TableTypeGenerator {

    override fun generate(context: TableGenerateContext, emitter: (TypeSpec.Builder) -> Unit) {
        when (context.table.ktormEntityType) {
            KtormEntityType.INTERFACE -> generateInterfaceEntity(context, emitter)
            KtormEntityType.CLASS -> generateClassEntity(context, emitter)
        }
    }

    protected open fun CodeBlock.Builder.appendTableNameParameter(table: TableDefinition, config: CodeGenerateConfig) {
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


    public open fun generateInterfaceEntity(context: TableGenerateContext, emitter: (TypeSpec.Builder) -> Unit) {
        val table = context.table
        val builder = TypeSpec.objectBuilder(table.tableClassName)
            .superclass(Table::class.asClassName().parameterizedBy(table.entityClassName))
            .addSuperclassConstructorParameter(buildCodeBlock {
                appendTableNameParameter(table, context.config)
                add("alias=%S,", table.alias)
                add("catalog=%S,", table.catalog)
                add("schema=%S,", table.schema)
                add("entityClass=%T::class,", table.entityClassName)
            })
        emitter(builder)
    }

    public open fun generateClassEntity(context: TableGenerateContext, emitter: (TypeSpec.Builder) -> Unit) {
        val table = context.table
        val builder = TypeSpec.objectBuilder(table.tableClassName)
            .superclass(BaseTable::class.asClassName().parameterizedBy(table.entityClassName))
            .addSuperclassConstructorParameter(buildCodeBlock {
                appendTableNameParameter(table, context.config)
                add("alias=%S, ", table.alias)
                add("catalog=%S, ", table.catalog)
                add("schema=%S, ", table.schema)
                add("entityClass=%T::class", table.entityClassName)
            })
        emitter(builder)
    }

}

private val bindToFun: MemberName = MemberName("", "bindTo")
private val primaryKeyFun: MemberName = MemberName("", "primaryKey")

public open class DefaultTablePropertyGenerator : TablePropertyGenerator {

    override fun generate(context: TableGenerateContext, emitter: (PropertySpec) -> Unit) {
        when (context.table.ktormEntityType) {
            KtormEntityType.INTERFACE -> generateInterfaceEntity(context, emitter)
            KtormEntityType.CLASS -> generateClassEntity(context, emitter)
        }
    }

    protected open fun generateInterfaceEntity(context: TableGenerateContext, emitter: (PropertySpec) -> Unit) {
        val (table, config, columnInitializerGenerator, _, dependencyFiles) = context
        table.columns
            .asSequence()
            .map { column ->
                PropertySpec
                    .builder(
                        column.propertyMemberName.simpleName,
                        Column::class.asClassName().parameterizedBy(column.propertyClassName.copy(nullable = false))
                    )
                    .initializer(buildCodeBlock {
                        add(columnInitializerGenerator.generate(column, dependencyFiles, config))
                        addStatement(".%M { it.%M }", bindToFun, column.propertyMemberName)
                        if (column.isPrimaryKey) addStatement(".%M()", primaryKeyFun)
                    })
                    .build()
            }
            .forEach(emitter)
    }

    protected open fun generateClassEntity(context: TableGenerateContext, emitter: (PropertySpec) -> Unit) {
        val (table, config, columnInitializerGenerator, _, dependencyFiles) = context
        table.columns
            .asSequence()
            .map { column ->
                PropertySpec
                    .builder(
                        column.propertyMemberName.simpleName,
                        Column::class.asClassName().parameterizedBy(column.propertyClassName.copy(nullable = false))
                    )
                    .initializer(buildCodeBlock {
                        add(columnInitializerGenerator.generate(column, dependencyFiles, config))
                        if (column.isPrimaryKey) addStatement(".%M()", primaryKeyFun)
                    })
                    .build()
            }
            .forEach(emitter)
    }

}

public class DefaultTableFunctionGenerator : TableFunctionGenerator {

    public companion object {
        private val hashMapClassName = HashMap::class.asClassName()
        private val kParameter = ClassName("kotlin.reflect", "KParameter")
        private val any = ClassName("kotlin", "Any")
        private val primaryConstructor = MemberName("kotlin.reflect.full", "primaryConstructor", true)
    }

    override fun generate(context: TableGenerateContext, emitter: (FunSpec) -> Unit) {
        if (context.table.ktormEntityType != KtormEntityType.CLASS) {
            return
        }
        val (table, config, _, logger, _) = context
        val row = "row"
        val withReferences = "withReferences"
        FunSpec.builder("doCreateEntity").addModifiers(KModifier.OVERRIDE).returns(table.entityClassName)
            .addParameter(row, QueryRowSet::class.asTypeName())
            .addParameter(withReferences, Boolean::class.asTypeName()).addCode(buildCodeBlock {
                val entityClassDeclaration = table.entityClassDeclaration
                val constructor = entityClassDeclaration.primaryConstructor!!
                val constructorParameter = constructor.parameters
                val nonStructuralProperties = table.columns.map { it.propertyMemberName.simpleName }.toMutableSet()
                // propertyName -> columnMember
                val columnMap = table.columns.associateBy { it.propertyMemberName.simpleName }
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
                        addStatement("val value = %L[%M]", row, column.propertyMemberName)
                        // hasDefault
                        if (parameter.hasDefault) {
                            beginControlFlow("if (value != null)")
                            addStatement("parameterMap[parameter] = value")
                            endControlFlow()
                        } else {
                            val notNullOperator = if (column.propertyIsNullable) "" else "!!"
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
                            table.columns.firstOrNull { it.propertyMemberName.simpleName == parameter.name!!.asString() }
                                ?: error("Construct parameter not exists in table: ${parameter.name!!.asString()}")

                        val notNullOperator = if (column.propertyIsNullable) "" else "!!"
                        add(
                            "%L = %L[%M]%L,",
                            parameter.name!!.asString(),
                            row,
                            MemberName(table.tableClassName, column.propertyMemberName.simpleName),
                            notNullOperator
                        )
                        nonStructuralProperties.remove(column.propertyMemberName.simpleName)
                    }
                    addStatement(")")
                }
                //non-structural property
                for (property in nonStructuralProperties) {
                    val column = columnMap[property]!!
                    if (!column.propertyDeclaration.isMutable) {
                        continue
                    }
                    val notNullOperator = if (column.propertyIsNullable) "" else "!!"
                    addStatement(
                        "instance.%L = %L[%M]%L",
                        property,
                        row,
                        MemberName(table.tableClassName, column.propertyMemberName.simpleName),
                        notNullOperator
                    )
                }
                addStatement("return instance")
            })
            .build()
            .run(emitter)
    }

}

public class SequencePropertyGenerator : TopLevelPropertyGenerator {
    override fun generate(context: TableGenerateContext, emitter: (PropertySpec) -> Unit) {
        val table = context.table
        val sequenceOf = MemberName("org.ktorm.entity", "sequenceOf", true)
        val tableClassName = table.tableClassName.simpleName
        val sequenceName = tableClassName.substring(0, 1).lowercase() + tableClassName.substring(1)
        val entitySequence = EntitySequence::class.asClassName()
        //EntitySequence<E, T>
        val sequenceType = entitySequence.parameterizedBy(table.entityClassName, table.tableClassName)
        PropertySpec.builder(sequenceName, sequenceType)
            .receiver(Database::class.asClassName())
            .getter(
                FunSpec.getterBuilder()
                    .addStatement("return this.%M(%T)", sequenceOf, table.tableClassName)
                    .build()
            )
            .build()
            .run(emitter)
    }
}

private val insertFun = MemberName("org.ktorm.dsl", "insert", true)
private val updateFun = MemberName("org.ktorm.dsl", "update", true)
private val eqFun = MemberName("org.ktorm.dsl", "eq", true)

public class ClassEntitySequenceAddFunGenerator : TopLevelFunctionGenerator {

    override fun generate(context: TableGenerateContext, emitter: (FunSpec) -> Unit) {
        if (context.table.ktormEntityType != KtormEntityType.CLASS) {
            return
        }
        val table = context.table
        FunSpec.builder("add")
            .receiver(EntitySequence::class.asClassName().parameterizedBy(table.entityClassName, table.tableClassName))
            .addParameter("entity", table.entityClassName)
            .returns(Int::class.asClassName())
            .addCode(buildCodeBlock {
                beginControlFlow("return this.database.%M(%T)", insertFun, table.tableClassName)
                for (column in table.columns) {
                    if (column.propertyIsNullable) {
                        beginControlFlow("if (entity.%M != null)", column.propertyMemberName)
                        addStatement(
                            "set(%M,entity.%M)",
                            MemberName(table.tableClassName, column.propertyMemberName.simpleName),
                            column.propertyMemberName
                        )
                        endControlFlow()
                    } else {
                        addStatement(
                            "set(%M,entity.%M)",
                            MemberName(table.tableClassName, column.propertyMemberName.simpleName),
                            column.propertyMemberName
                        )
                    }
                }
                endControlFlow()
            })
            .build()
            .run(emitter)
    }

}

public class ClassEntitySequenceUpdateFunGenerator : TopLevelFunctionGenerator {
    override fun generate(context: TableGenerateContext, emitter: (FunSpec) -> Unit) {
        if (context.table.ktormEntityType != KtormEntityType.CLASS) {
            return
        }
        val table = context.table
        FunSpec.builder("update")
            .receiver(EntitySequence::class.asClassName().parameterizedBy(table.entityClassName, table.tableClassName))
            .addParameter("entity", table.entityClassName)
            .returns(Int::class.asClassName())
            .addCode(buildCodeBlock {
                beginControlFlow("return this.database.%M(%T)", updateFun, table.tableClassName)
                for (column in table.columns) {
                    if (!column.isPrimaryKey) {
                        addStatement(
                            "set(%M,entity.%M)",
                            column.columnMemberName,
                            column.propertyMemberName
                        )
                    }
                }
                beginControlFlow("where")
                for (column in table.columns) {
                    if (column.isPrimaryKey) {
                        addStatement("it.%M %M entity.%M", column.columnMemberName, eqFun, column.propertyMemberName)
                    }
                }
                endControlFlow()
                endControlFlow()
            })
            .build()
            .run(emitter)
    }

}