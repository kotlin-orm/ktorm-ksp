/*
 * Copyright 2018-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ktorm.ksp.compiler.generator

import com.google.devtools.ksp.processing.KSPLogger
import com.squareup.kotlinpoet.FileSpec
import org.ktorm.ksp.codegen.*
import org.ktorm.ksp.codegen.generator.*
import java.util.*

public class TableFileGenerator(config: CodeGenerateConfig, logger: KSPLogger) {

    private val typeGenerator: TableTypeGenerator = getOneOrNullService() ?: DefaultTableTypeGenerator()
    private val propertyGenerator: TablePropertyGenerator = getOneOrNullService() ?: DefaultTablePropertyGenerator()
    private val functionGenerator: TableFunctionGenerator = getOneOrNullService() ?: DefaultTableFunctionGenerator()
    private val topLevelPropertyGenerator: MutableSet<TopLevelPropertyGenerator> =
        getAllService<TopLevelPropertyGenerator>().toMutableSet()
    private val topLevelFunctionGenerator: MutableSet<TopLevelFunctionGenerator> =
        getAllService<TopLevelFunctionGenerator>().toMutableSet()

    init {
        val extensionGenerator = config.extensionGenerator
        if (extensionGenerator.enableSequenceOf) {
            topLevelPropertyGenerator.add(SequencePropertyGenerator())
        }
        if (extensionGenerator.enableClassEntitySequenceAddFun) {
            topLevelFunctionGenerator.add(ClassEntitySequenceAddFunGenerator())
        }
        if (extensionGenerator.enableClassEntitySequenceUpdateFun) {
            topLevelFunctionGenerator.add(ClassEntitySequenceUpdateFunGenerator())
        }
        if (extensionGenerator.enableInterfaceEntitySimulationDataClass) {
            topLevelFunctionGenerator.add(InterfaceEntityConstructorFunGenerator())
            topLevelFunctionGenerator.add(InterfaceEntityComponentFunGenerator())
            topLevelFunctionGenerator.add(InterfaceEntityCopyFunGenerator())
        }
        logger.info("typeGenerator: ${typeGenerator::class.simpleName}")
        logger.info("propertyGenerator: ${propertyGenerator::class.simpleName}")
        logger.info("functionGenerator: ${functionGenerator::class.simpleName}")
        logger.info("topLevelPropertyGenerator: ${topLevelPropertyGenerator.map { it::class.simpleName }}")
        logger.info("topLevelFunctionGenerator: ${topLevelFunctionGenerator.map { it::class.simpleName }}")
    }

    private inline fun <reified T> getOneOrNullService(): T? {
        val services = ServiceLoader.load(T::class.java, TableFileGenerator::class.java.classLoader).toSet()
        if (services.isEmpty()) return null
        if (services.size > 1) error("Service ${T::class.java.canonicalName} cannot be more than one")
        return services.first()
    }

    private inline fun <reified T> getAllService(): Set<T> {
        return ServiceLoader.load(T::class.java, TableFileGenerator::class.java.classLoader).toSet()
    }

    private fun <T : Any> Iterable<TableCodeGenerator<T>>.forEachGenerate(
        context: TableGenerateContext,
        action: (T) -> Unit
    ) {
        forEach {
            it.generate(context, action)
        }
    }

    public fun generate(context: TableGenerateContext): FileSpec {
        val fileBuilder = generateFile(context)
        typeGenerator.generate(context) { typeBuilder ->
            propertyGenerator.generate(context) { typeBuilder.addProperty(it) }
            functionGenerator.generate(context) { typeBuilder.addFunction(it) }
            fileBuilder.addType(typeBuilder.build())
        }
        topLevelFunctionGenerator.forEachGenerate(context) { fileBuilder.addFunction(it) }
        topLevelPropertyGenerator.forEachGenerate(context) { fileBuilder.addProperty(it) }
        return fileBuilder.build()
    }

    private fun generateFile(context: TableGenerateContext): FileSpec.Builder {
        val table = context.table
        return FileSpec.builder(table.tableClassName.packageName, table.tableClassName.simpleName)
            .addFileComment("auto-generated code, don't modify it")
            .indent("    ")
    }
}
