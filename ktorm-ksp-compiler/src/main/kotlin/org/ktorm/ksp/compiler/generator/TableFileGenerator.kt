package org.ktorm.ksp.compiler.generator

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import org.ktorm.schema.BaseTable
import org.ktorm.schema.Table

public abstract class AbstractTableFileGenerator(
    public val context: TableGenerateContext
) {

    protected abstract val typeGenerator: TableCodeGenerator<TypeSpec.Builder>
    protected abstract val propertyGenerator: List<TableCodeGenerator<PropertySpec>>
    protected abstract val functionGenerator: List<TableCodeGenerator<FunSpec>>
    protected abstract val topLevelPropertyGenerator: List<TableCodeGenerator<PropertySpec>>
    protected abstract val topLevelFunctionGenerator: List<TableCodeGenerator<FunSpec>>


    private fun <T : Any> Iterable<TableCodeGenerator<T>>.forEachGenerate(action: (T) -> Unit) {
        forEach {
            it.generate(context, action)
        }
    }

    public open fun generate(): FileSpec {
        val fileBuilder = generateFile()
        typeGenerator.generate(context) { typeBuilder ->
            propertyGenerator.forEachGenerate { typeBuilder.addProperty(it) }
            functionGenerator.forEachGenerate { typeBuilder.addFunction(it) }
            fileBuilder.addType(typeBuilder.build())
        }
        topLevelFunctionGenerator.forEachGenerate { fileBuilder.addFunction(it) }
        topLevelPropertyGenerator.forEachGenerate { fileBuilder.addProperty(it) }
        return fileBuilder.build()
    }

    public open fun generateFile(): FileSpec.Builder {
        val table = context.table
        return FileSpec.builder(table.tableClassName.packageName, table.tableClassName.simpleName)
    }
}

/**
 * generate fileSpec for [Table]
 */
public open class TableFileGenerator(context: TableGenerateContext) : AbstractTableFileGenerator(context) {
    override val typeGenerator: TableCodeGenerator<TypeSpec.Builder> = TableTypeGenerator()
    override val propertyGenerator: List<TableCodeGenerator<PropertySpec>> = listOf(TablePropertyGenerator())
    override val functionGenerator: List<TableCodeGenerator<FunSpec>> = emptyList()
    override val topLevelPropertyGenerator: List<TableCodeGenerator<PropertySpec>> = listOf(SequencePropertyGenerator())
    override val topLevelFunctionGenerator: List<TableCodeGenerator<FunSpec>> = emptyList()
}

/**
 * generate fileSpec for [BaseTable]
 */
public open class BaseTableFileGenerator(context: TableGenerateContext) : AbstractTableFileGenerator(context) {
    override val typeGenerator: TableCodeGenerator<TypeSpec.Builder> = BaseTableTypeGenerator()
    override val propertyGenerator: List<TableCodeGenerator<PropertySpec>> = listOf(BaseTablePropertyGenerator())
    override val functionGenerator: List<TableCodeGenerator<FunSpec>> = listOf(BaseTableCreateEntityGenerator())
    override val topLevelPropertyGenerator: List<TableCodeGenerator<PropertySpec>> = listOf(SequencePropertyGenerator())
    override val topLevelFunctionGenerator: List<TableCodeGenerator<FunSpec>> = emptyList()
}