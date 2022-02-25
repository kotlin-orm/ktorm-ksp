package org.ktorm.ksp.compiler.generator

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.buildCodeBlock
import org.ktorm.ksp.compiler.definition.CodeGenerateConfig
import org.ktorm.ksp.compiler.definition.TableDefinition
import org.ktorm.schema.BaseTable
import org.ktorm.schema.Table

private fun CodeBlock.Builder.appendTableNameParameter(table: TableDefinition, config: CodeGenerateConfig) {
    val tableName = when {
        table.tableName.isNotEmpty() -> table.tableName
        config.namingStrategy != null && config.localNamingStrategy != null -> {
            config.localNamingStrategy.toTableName(table.entityClassName.simpleName)
        }
        config.namingStrategy == null -> {
            table.entityClassName.simpleName
        }
        else -> {
            add("tableName=%T.toTableName(%S),", config.namingStrategy, table.entityClassName.simpleName)
            return
        }
    }
    add("tableName=%S,", tableName)
}

public class BaseTableTypeGenerator : TableCodeGenerator<TypeSpec.Builder> {

    override fun generate(context: TableGenerateContext, emitter: (TypeSpec.Builder) -> Unit) {
        val table = context.table
        val builder = TypeSpec.objectBuilder(table.tableClassName)
            .superclass(BaseTable::class.asClassName().parameterizedBy(table.entityClassName))
            .addSuperclassConstructorParameter(buildCodeBlock {
                appendTableNameParameter(table, context.config)
                add("alias=%S, ", table.alias)
                add("catalog=%S, ", table.catalog)
                add("schema=%S, ", table.schema)
                add("entityClass=%T::class", table.entityClassName)
            })
        emitter(builder)
    }
}

public class TableTypeGenerator : TableCodeGenerator<TypeSpec.Builder> {

    override fun generate(context: TableGenerateContext, emitter: (TypeSpec.Builder) -> Unit) {
        val table = context.table
        val builder = TypeSpec.objectBuilder(table.tableClassName)
            .superclass(Table::class.asClassName().parameterizedBy(table.entityClassName))
            .addSuperclassConstructorParameter(buildCodeBlock {
                appendTableNameParameter(table, context.config)
                add("alias=%S,", table.alias)
                add("catalog=%S,", table.catalog)
                add("schema=%S,", table.schema)
                add("entityClass=%T::class,", table.entityClassName)
            })
        emitter(builder)
    }
}