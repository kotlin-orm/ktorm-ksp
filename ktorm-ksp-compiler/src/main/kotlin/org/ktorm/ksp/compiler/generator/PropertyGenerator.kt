package org.ktorm.ksp.compiler.generator

import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.buildCodeBlock
import org.ktorm.schema.Column

private val bindToFun: MemberName = MemberName("", "bindTo")
private val primaryKeyFun: MemberName = MemberName("", "primaryKey")

public class InterfaceEntityTablePropertyGenerator : TableCodeGenerator<PropertySpec> {

    private val ignoreDefinitionProperties: Set<String> = setOf("entityClass", "properties")

    override fun generate(context: TableGenerateContext, emitter: (PropertySpec) -> Unit) {
        val (table, config, columnInitializerGenerator, _, dependencyFiles) = context
        table.columns
            .asSequence()
            .filter { it.property.simpleName !in ignoreDefinitionProperties }
            .map { column ->
                PropertySpec
                    .builder(
                        column.property.simpleName,
                        Column::class.asClassName().parameterizedBy(column.propertyClassName.copy(nullable = false))
                    )
                    .initializer(buildCodeBlock {
                        add(columnInitializerGenerator.generate(column, dependencyFiles, config))
                        addStatement(".%M { it.%M }", bindToFun, column.property)
                        if (column.isPrimaryKey) addStatement(".%M()", primaryKeyFun)
                    })
                    .build()
            }
            .forEach(emitter)
    }

}

public class ClassEntityTablePropertyGenerator : TableCodeGenerator<PropertySpec> {

    override fun generate(context: TableGenerateContext, emitter: (PropertySpec) -> Unit) {
        val (table, config, columnInitializerGenerator, _, dependencyFiles) = context
        table.columns
            .asSequence()
            .map { column ->
                PropertySpec
                    .builder(
                        column.property.simpleName,
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