package org.ktorm.ksp.compiler.generator

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import org.ktorm.ksp.spi.TableMetadata

@OptIn(KotlinPoetKspPreview::class)
object ComponentFunctionGenerator {

    fun generate(table: TableMetadata): List<FunSpec> {
        return table.columns.mapIndexed { i, column ->
            FunSpec.builder("component${i + 1}")
                .addKdoc(
                    "Return the value of [%L.%L]. ",
                    table.entityClass.simpleName.asString(),
                    column.entityProperty.simpleName.asString()
                )
                .addModifiers(KModifier.OPERATOR)
                .receiver(table.entityClass.toClassName())
                .returns(column.entityProperty.type.resolve().toTypeName())
                .addCode("return·this.%N", column.entityProperty.simpleName.asString())
                .build()
        }
    }
}
