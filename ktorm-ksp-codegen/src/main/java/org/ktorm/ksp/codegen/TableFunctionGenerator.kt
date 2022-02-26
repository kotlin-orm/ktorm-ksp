package org.ktorm.ksp.codegen

import com.squareup.kotlinpoet.*
import org.ktorm.dsl.QueryRowSet
import org.ktorm.ksp.codegen.definition.KtormEntityType

public interface TableFunctionGenerator : TableCodeGenerator<FunSpec>

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