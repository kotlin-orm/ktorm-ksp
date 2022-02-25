package org.ktorm.ksp.compiler.generator

import com.squareup.kotlinpoet.*
import org.ktorm.dsl.QueryRowSet

public class BaseTableCreateEntityGenerator: TableCodeGenerator<FunSpec> {

    public companion object {
        private val hashMapClassName = HashMap::class.asClassName()
        private val kParameter = ClassName("kotlin.reflect", "KParameter")
        private val any = ClassName("kotlin", "Any")
        private val primaryConstructor = MemberName("kotlin.reflect.full", "primaryConstructor", true)
    }

    override fun generate(context: TableGenerateContext, emitter: (FunSpec) -> Unit) {
        val (table, config, _, logger, _) = context
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
        emitter(createFun)
    }

}