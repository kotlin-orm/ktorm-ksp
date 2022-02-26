package org.ktorm.ksp.codegen

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.buildCodeBlock
import org.ktorm.entity.EntitySequence
import org.ktorm.ksp.codegen.definition.KtormEntityType


private val insertFun = MemberName("org.ktorm.dsl", "insert", true)
private val updateFun = MemberName("org.ktorm.dsl", "update", true)
private val eqFun = MemberName("org.ktorm.dsl", "eq", true)

public interface TopLevelFunctionGenerator : TableCodeGenerator<FunSpec>

// todo Add an annotation attribute to configure whether to enable, the generator is enabled by default
public class EntitySequenceAddFunGenerator : TopLevelFunctionGenerator {

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

// todo Add an annotation attribute to configure whether to enable, the generator is enabled by default
public class EntitySequenceUpdateFunGenerator : TopLevelFunctionGenerator {
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