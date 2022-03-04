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

private val andFun = MemberName("org.ktorm.dsl", "and", true)

public class SequenceUpdateAllFunctionGenerator : TopLevelFunctionGenerator {
    override fun generate(context: TableGenerateContext, emitter: (FunSpec) -> Unit) {
        val table = context.table
        val primaryKeyColumns = table.columns.filter { it.isPrimaryKey }
        if (primaryKeyColumns.isEmpty()) {
            context.logger.info("skip the entity sequence updateAll method of table ${table.entityClassName} because it does not have a primary key column")
            return
        }
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
                primaryKeyColumns.forEachIndexed { index, column ->
                    if (index == 0) {
                        val conditionTemperate = if (primaryKeyColumns.size == 1) {
                            "it.%M %M entity.%L%L"
                        } else {
                            "(it.%M %M entity.%L%L)"
                        }
                        addStatement(
                            conditionTemperate,
                            column.tablePropertyName,
                            eqFun,
                            column.entityPropertyName.simpleName,
                            if (column.isNullable) "!!" else ""
                        )
                    } else {
                        addStatement(
                            ".%M(it.%M %M entity.%L%L)",
                            andFun,
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
