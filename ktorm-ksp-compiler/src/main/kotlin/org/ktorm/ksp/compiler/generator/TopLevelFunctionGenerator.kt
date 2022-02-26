package org.ktorm.ksp.compiler.generator

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.buildCodeBlock
import org.ktorm.entity.EntitySequence


private val insertFun = MemberName("org.ktorm.dsl", "insert", true)

public class EntitySequenceAddFunGenerator : TableCodeGenerator<FunSpec> {

    override fun generate(context: TableGenerateContext, emitter: (FunSpec) -> Unit) {
        val table = context.table
        FunSpec.builder("add")
            .receiver(EntitySequence::class.asClassName().parameterizedBy(table.entityClassName, table.tableClassName))
            .addParameter("entity", table.entityClassName)
            .returns(Int::class.asClassName())
            .addCode(buildCodeBlock {
                beginControlFlow("return this.database.%M(%T)", insertFun, table.tableClassName)
                for (column in table.columns) {
                    if (column.propertyIsNullable) {
                        beginControlFlow("if (entity.%M != null)", column.property)
                        addStatement(
                            "set(%M,entity.%M)",
                            MemberName(table.tableClassName, column.property.simpleName),
                            column.property
                        )
                        endControlFlow()
                    } else {
                        addStatement(
                            "set(%M,entity.%M)",
                            MemberName(table.tableClassName, column.property.simpleName),
                            column.property
                        )
                    }
                }
                endControlFlow()
            })
            .build()
            .run(emitter)
    }

}