package org.ktorm.ksp.codegen

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.buildCodeBlock
import org.ktorm.ksp.codegen.definition.KtormEntityType
import org.ktorm.ksp.codegen.definition.TableDefinition
import org.ktorm.schema.BaseTable
import org.ktorm.schema.Table

public interface TableTypeGenerator: TableCodeGenerator<TypeSpec.Builder>

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

public class DefaultTableTypeGenerator: TableTypeGenerator {

    override fun generate(context: TableGenerateContext, emitter: (TypeSpec.Builder) -> Unit) {
        when(context.table.ktormEntityType) {
            KtormEntityType.INTERFACE -> generateInterfaceEntity(context, emitter)
            KtormEntityType.CLASS -> generateClassEntity(context, emitter)
        }
    }

    private fun generateInterfaceEntity(context: TableGenerateContext, emitter: (TypeSpec.Builder) -> Unit) {
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

    private fun generateClassEntity(context: TableGenerateContext, emitter: (TypeSpec.Builder) -> Unit) {
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
