package org.ktorm.ksp.ext

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.buildCodeBlock
import org.ktorm.entity.EntitySequence
import org.ktorm.ksp.codegen.TableGenerateContext
import org.ktorm.ksp.codegen.TopLevelFunctionGenerator
import org.ktorm.ksp.codegen.withIndent

private val batchInsertFun = MemberName("org.ktorm.dsl", "batchInsert", true)
private val batchUpdateFun = MemberName("org.ktorm.dsl", "batchUpdate", true)
private val eqFun = MemberName("org.ktorm.dsl", "eq", true)

public class SequenceAddAllFunctionGenerator : TopLevelFunctionGenerator {
    override fun generate(context: TableGenerateContext, emitter: (FunSpec) -> Unit) {
        val table = context.table
        FunSpec.builder("addAll")
            .addKdoc("""
                Batch insert entities into the database, this method will not get the auto-incrementing primary key
                @return the effected row counts for each sub-operation.
            """.trimIndent())
            .receiver(EntitySequence::class.asClassName().parameterizedBy(table.entityClassName, table.tableClassName))
            .addParameter("entities", Iterable::class.asClassName().parameterizedBy(table.entityClassName))
            .returns(IntArray::class.asClassName())
            .addCode(buildCodeBlock {
                add("return·this.database.%M(%T)·{\n", batchInsertFun, table.tableClassName)
                withIndent(3) {
                    beginControlFlow("for (entity in entities)")
                    beginControlFlow("item")
                    for (column in table.columns) {
                        if (column.isReferences) {
                            val primaryKey = column.referencesColumn!!
                            addStatement(
                                "set(%T.%L,·entity.%L.%L)",
                                table.tableClassName,
                                column.tablePropertyName.simpleName,
                                column.entityPropertyName.simpleName,
                                primaryKey.entityPropertyName.simpleName
                            )
                        } else {
                            addStatement(
                                "set(%T.%L,·entity.%L)",
                                table.tableClassName,
                                column.tablePropertyName.simpleName,
                                column.entityPropertyName.simpleName
                            )
                        }
                    }
                    endControlFlow()
                    endControlFlow()
                }
                add("    }")
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
            .addKdoc("""
                Batch update based on entity primary key
                @return the effected row counts for each sub-operation.
            """.trimIndent())
            .receiver(EntitySequence::class.asClassName().parameterizedBy(table.entityClassName, table.tableClassName))
            .addParameter("entities", Iterable::class.asClassName().parameterizedBy(table.entityClassName))
            .returns(IntArray::class.asClassName())
            .addCode(buildCodeBlock {
                add("return·this.database.%M(%T)·{\n", batchUpdateFun, table.tableClassName)
                withIndent(3) {
                        beginControlFlow("for (entity in entities)")
                        beginControlFlow("item")
                        for (column in table.columns) {
                            if (!column.isPrimaryKey) {
                                if (column.isReferences) {
                                    val primaryKey = column.referencesColumn!!
                                    addStatement(
                                        "set(%T.%L,·entity.%L.%L)",
                                        table.tableClassName,
                                        column.tablePropertyName.simpleName,
                                        column.entityPropertyName.simpleName,
                                        primaryKey.entityPropertyName.simpleName
                                    )
                                } else {
                                    addStatement(
                                        "set(%T.%L,·entity.%L)",
                                        table.tableClassName,
                                        column.tablePropertyName.simpleName,
                                        column.entityPropertyName.simpleName
                                    )
                                }
                            }
                        }
                        beginControlFlow("where")
                        primaryKeyColumns.forEachIndexed { index, column ->
                            if (index == 0) {
                                val conditionTemperate = if (primaryKeyColumns.size == 1) {
                                    "it.%L·%M·entity.%L%L"
                                } else {
                                    "(it.%L·%M·entity.%L%L)"
                                }
                                addStatement(
                                    conditionTemperate,
                                    column.tablePropertyName.simpleName,
                                    eqFun,
                                    column.entityPropertyName.simpleName,
                                    if (column.isNullable) "!!" else ""
                                )
                            } else {
                                addStatement(
                                    ".%M(it.%L·%M·entity.%L%L)",
                                    andFun,
                                    column.tablePropertyName.simpleName,
                                    eqFun,
                                    column.entityPropertyName.simpleName,
                                    if (column.isNullable) "!!" else ""
                                )
                            }
                        }
                        endControlFlow()
                        endControlFlow()
                        endControlFlow()
                }
                add("    }")
            })
            .build()
            .run(emitter)
    }
}
