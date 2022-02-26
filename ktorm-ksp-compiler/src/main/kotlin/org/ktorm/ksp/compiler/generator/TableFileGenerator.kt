package org.ktorm.ksp.compiler.generator

import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.squareup.kotlinpoet.FileSpec
import org.ktorm.ksp.codegen.*
import java.util.*

public open class TableFileGenerator(
    protected val context: TableGenerateContext
) {

    public companion object {

        public val typeGenerator: TableTypeGenerator = getOneOrNullService() ?: DefaultTableTypeGenerator()
        public val propertyGenerator: TablePropertyGenerator = getOneOrNullService() ?: DefaultTablePropertyGenerator()
        public val functionGenerator: TableFunctionGenerator = getOneOrNullService() ?: DefaultTableFunctionGenerator()
        public val topLevelPropertyGenerator: List<TopLevelPropertyGenerator> = getAllService()
        public val topLevelFunctionGenerator: List<TopLevelFunctionGenerator> = getAllService()
        public val symbolProcessorProviders: List<SymbolProcessorProvider> = getAllService()

        private inline fun <reified T> getOneOrNullService(): T? {
            val services = ServiceLoader.load(T::class.java, TableFileGenerator::class.java.classLoader).toList()
            if (services.isEmpty()) return null
            if (services.size > 1) error("Service ${T::class.java.canonicalName} cannot be more than one")
            return services.first()
        }

        private inline fun <reified T> getAllService(): List<T> {
            return ServiceLoader.load(T::class.java, TableFileGenerator::class.java.classLoader).toList()
        }

    }

    private fun <T : Any> Iterable<TableCodeGenerator<T>>.forEachGenerate(action: (T) -> Unit) {
        forEach {
            it.generate(context, action)
        }
    }

    public open fun generate(): FileSpec {
        context.logger.info("typeGenerator: ${typeGenerator::class.simpleName}")
        context.logger.info("propertyGenerator: ${propertyGenerator::class.simpleName}")
        context.logger.info("functionGenerator: ${functionGenerator::class.simpleName}")
        context.logger.info("topLevelPropertyGenerator: ${topLevelPropertyGenerator.map { it::class.simpleName }}")
        context.logger.info("topLevelFunctionGenerator: ${topLevelFunctionGenerator.map { it::class.simpleName }}")
        val fileBuilder = generateFile()
        typeGenerator.generate(context) { typeBuilder ->
            propertyGenerator.generate(context) { typeBuilder.addProperty(it) }
            functionGenerator.generate(context) { typeBuilder.addFunction(it) }
            fileBuilder.addType(typeBuilder.build())
        }
        topLevelFunctionGenerator.forEachGenerate { fileBuilder.addFunction(it) }
        topLevelPropertyGenerator.forEachGenerate { fileBuilder.addProperty(it) }
        return fileBuilder.build()
    }

    protected open fun generateFile(): FileSpec.Builder {
        val table = context.table
        return FileSpec.builder(table.tableClassName.packageName, table.tableClassName.simpleName)
    }
}