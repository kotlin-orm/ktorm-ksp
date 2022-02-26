package org.ktorm.ksp.codegen

import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.buildCodeBlock
import org.ktorm.ksp.codegen.definition.KtormEntityType
import org.ktorm.schema.Column

private val bindToFun: MemberName = MemberName("", "bindTo")
private val primaryKeyFun: MemberName = MemberName("", "primaryKey")

public interface TablePropertyGenerator : TableCodeGenerator<PropertySpec>

public open class DefaultTablePropertyGenerator : TablePropertyGenerator {

    override fun generate(context: TableGenerateContext, emitter: (PropertySpec) -> Unit) {
        when (context.table.ktormEntityType) {
            KtormEntityType.INTERFACE -> generateInterfaceEntity(context, emitter)
            KtormEntityType.CLASS -> generateClassEntity(context, emitter)
        }
    }

    private fun generateInterfaceEntity(context: TableGenerateContext, emitter: (PropertySpec) -> Unit) {
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

    private fun generateClassEntity(context: TableGenerateContext, emitter: (PropertySpec) -> Unit) {
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
