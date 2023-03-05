package org.ktorm.ksp.compiler.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import org.ktorm.entity.Entity
import org.ktorm.ksp.api.Undefined
import org.ktorm.ksp.compiler.util.*
import org.ktorm.ksp.spi.TableMetadata

@OptIn(KotlinPoetKspPreview::class)
object PseudoConstructorFunctionGenerator {

    fun generate(table: TableMetadata): FunSpec {
        return FunSpec.builder(table.entityClass.simpleName.asString())
            .addKdoc(
                "Create an entity of [%L] and specify the initial values for each property, " +
                "properties that doesn't have an initial value will leave unassigned. ",
                table.entityClass.simpleName.asString()
            )
            .addParameters(buildParameters(table))
            .returns(table.entityClass.toClassName())
            .addCode(buildFunctionBody(table))
            .build()
    }

    internal fun buildParameters(table: TableMetadata): List<ParameterSpec> {
        return table.columns.map { column ->
            val propName = column.entityProperty.simpleName.asString()
            val propType = column.entityProperty.type.resolve().makeNullable().toTypeName()

            ParameterSpec.builder(propName, propType)
                .defaultValue("%T.of()", Undefined::class.asClassName())
                .build()
        }
    }

    internal fun buildFunctionBody(table: TableMetadata, isCopy: Boolean = false): CodeBlock = buildCodeBlock {
        if (isCopy) {
            addStatement("val·entity·=·this.copy()")
        } else {
            addStatement("val·entity·=·%T.create<%T>()", Entity::class.asClassName(), table.entityClass.toClassName())
        }

        for (column in table.columns) {
            val propName = column.entityProperty.simpleName.asString()
            val propType = column.entityProperty.type.resolve()

            if (propType.isInline()) {
                beginControlFlow(
                    "if·((%N·as·Any?)·!==·(%T.of<%T>()·as·Any?))",
                    propName, Undefined::class.asClassName(), propType.makeNotNullable().toTypeName()
                )
            } else {
                beginControlFlow(
                    "if·(%N·!==·%T.of<%T>())",
                    propName, Undefined::class.asClassName(), propType.makeNotNullable().toTypeName()
                )
            }

            var statement: String
            if (column.entityProperty.isMutable) {
                statement = "entity.%1N·=·%1N"
            } else {
                statement = "entity[%1S]·=·%1N"
            }

            if (!propType.isMarkedNullable) {
                statement += "·?:·error(\"`%1L` should not be null.\")"
            }

            addStatement(statement, propName)
            endControlFlow()
        }

        addStatement("return entity")
    }
}
