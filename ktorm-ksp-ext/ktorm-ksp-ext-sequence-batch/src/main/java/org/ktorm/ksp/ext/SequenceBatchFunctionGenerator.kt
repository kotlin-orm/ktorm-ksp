package org.ktorm.ksp.ext

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.buildCodeBlock
import org.ktorm.entity.EntitySequence
import org.ktorm.ksp.codegen.TableGenerateContext
import org.ktorm.ksp.codegen.TopLevelFunctionGenerator

private val batchInsertFun = MemberName("org.ktorm.dsl", "batchInsert", true)
private val batchUpdateFun = MemberName("org.ktorm.dsl", "batchUpdate", true)
private val eqFun = MemberName("org.ktorm.dsl", "eq", true)

// todo batch fun not support reference column

public class SequenceAddAllFunctionGenerator : TopLevelFunctionGenerator {
    override fun generate(context: TableGenerateContext, emitter: (FunSpec) -> Unit) {
        val table = context.table
        FunSpec.builder("addAll")
            .receiver(EntitySequence::class.asClassName().parameterizedBy(table.entityClassName, table.tableClassName))
            .addParameter("entities", Iterable::class.asClassName().parameterizedBy(table.entityClassName))
            .returns(IntArray::class.asClassName())
            .addCode(buildCodeBlock {
                beginControlFlow("return this.database.%M(%T)", batchInsertFun, table.tableClassName)
                beginControlFlow("for (entity in entities)")
                beginControlFlow("item")
                for (column in table.columns) {
                    if (column.isReferences) {
                        val primaryKey  = column.referencesColumn!!
                        addStatement(
                            "set(%M,entity.%L.%L)",
                            column.tablePropertyName,
                            column.entityPropertyName.simpleName,
                            primaryKey.entityPropertyName.simpleName
                        )
                    } else {
                        addStatement(
                            "set(%M,entity.%L)",
                            column.tablePropertyName,
                            column.entityPropertyName.simpleName
                        )
                    }
                }
                endControlFlow()
                endControlFlow()
                endControlFlow()
            })
            .build()
            .run(emitter)
    }
}

public class SequenceUpdateAllFunctionGenerator : TopLevelFunctionGenerator {
    override fun generate(context: TableGenerateContext, emitter: (FunSpec) -> Unit) {
        val table = context.table
        FunSpec.builder("updateAll")
            .receiver(EntitySequence::class.asClassName().parameterizedBy(table.entityClassName, table.tableClassName))
            .addParameter("entities", Iterable::class.asClassName().parameterizedBy(table.entityClassName))
            .returns(IntArray::class.asClassName())
            .addCode(buildCodeBlock {
                beginControlFlow("return this.database.%M(%T)", batchUpdateFun, table.tableClassName)
                beginControlFlow("for (entity in entities)")
                beginControlFlow("item")
                for (column in table.columns) {
                    if (!column.isPrimaryKey) {
                        if (column.isReferences) {
                            val primaryKey  = column.referencesColumn!!
                            addStatement(
                                "set(%M,entity.%L.%L)",
                                column.tablePropertyName,
                                column.entityPropertyName.simpleName,
                                primaryKey.entityPropertyName.simpleName
                            )
                        } else {
                            addStatement(
                                "set(%M,entity.%L)",
                                column.tablePropertyName,
                                column.entityPropertyName.simpleName
                            )
                        }
                    }
                }
                beginControlFlow("where")
                for (column in table.columns) {
                    if (column.isPrimaryKey) {
                        addStatement(
                            "%M %M entity.%L%L",
                            column.tablePropertyName,
                            eqFun,
                            column.entityPropertyName.simpleName,
                            if (column.isNullable) "!!" else ""
                        )
                    }
                }
                endControlFlow()
                endControlFlow()
                endControlFlow()
                endControlFlow()
            })
            .build()
            .run(emitter)
    }
}