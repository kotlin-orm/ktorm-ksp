/*
 * Copyright 2022-2023 the original author or authors.
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


package org.ktorm.ksp.compiler

import com.google.devtools.ksp.*
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import org.ktorm.entity.Entity
import org.ktorm.ksp.api.*
import org.ktorm.ksp.compiler.generator.KtormCodeGenerator
import org.ktorm.ksp.compiler.generator.util.NameGenerator
import org.ktorm.ksp.spi.CodeGenerateConfig
import org.ktorm.ksp.spi.ExtensionGeneratorConfig
import org.ktorm.ksp.spi.definition.ColumnDefinition
import org.ktorm.ksp.spi.definition.KtormEntityType
import org.ktorm.ksp.spi.definition.TableDefinition
import org.ktorm.ksp.spi.findSuperTypeReference
import org.ktorm.schema.SqlType

public class KtormProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        environment.logger.info("create KtormKspProcessor")
        return KtormProcessor(environment)
    }
}

@OptIn(KotlinPoetKspPreview::class, KspExperimental::class)
public class KtormProcessor(
    private val environment: SymbolProcessorEnvironment,
) : SymbolProcessor {
    private val logger = environment.logger

    private companion object {
        private val columnQualifiedName = Column::class.qualifiedName!!
        private val ignoreInterfaceEntityProperties: Set<String> = setOf("entityClass", "properties")
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        logger.info("start ktorm ksp processor")
        // process config and entity class
        val (config, configRets) = processKtormKspConfig(resolver)
        val (tableDefinitions, tableRets) = processEntity(resolver)
        // start generate
        KtormCodeGenerator.generate(tableDefinitions, environment.codeGenerator, config, logger)
        return configRets + tableRets
    }

    private fun processKtormKspConfig(resolver: Resolver): Pair<CodeGenerateConfig, List<KSAnnotated>> {
        logger.info("start process KtormKspConfig")
        val configSymbols = resolver.getSymbolsWithAnnotation(KtormKspConfig::class.qualifiedName!!)
        val configRet = configSymbols.filter { !it.validate() }.toList()
        logger.info("KtormKspConfigSymbols:${configSymbols.toList()}")
        val configClasses = configSymbols.filter { it is KSClassDeclaration && it.validate() }.toList()
        if (configClasses.size > 1) {
            error("@KtormKspConfig can only be added to a class")
        }
        val configBuilder = CodeGenerateConfig.Builder()
        val configAnnotated = configClasses.firstOrNull()
        if (configAnnotated != null) {
            configAnnotated.accept(KtormKspConfigVisitor(configBuilder), Unit)
            configBuilder.configDependencyFile = configAnnotated.containingFile
        }
        val config = configBuilder.build()
        logger.info("CodeGenerateConfig:$config")
        return config to configRet
    }

    private fun processEntity(resolver: Resolver): Pair<List<TableDefinition>, List<KSAnnotated>> {
        logger.info("start process entity")
        val symbols = resolver.getSymbolsWithAnnotation(Table::class.qualifiedName!!)
        logger.info("entity symbols:${symbols.toList()}")
        val tableDefinitions = mutableListOf<TableDefinition>()
        val tableRet = symbols.filter { !it.validate() }.toList()
        symbols.filter { it is KSClassDeclaration && it.validate() }
            .forEach { it.accept(EntityVisitor(tableDefinitions), Unit) }
        // entityClassName -> tableDefinition
        val entityClassMap = tableDefinitions.associateBy { it.entityClassName }
        logger.info("tableClassNameMap: $entityClassMap")
        // references columns
        tableDefinitions
            .asSequence()
            .flatMap { it.columns }
            .filter { it.isReference }
            .forEach {
                if (it.table.ktormEntityType != KtormEntityType.ENTITY_INTERFACE) {
                    error("Wrong references column: ${it.tablePropertyName}, References Column are only allowed for interface entity type")
                }
                val nonNullPropertyTypeName = it.nonNullPropertyTypeName
                val table = entityClassMap[nonNullPropertyTypeName]
                    ?: error(
                        "Wrong references column: ${it.tablePropertyName} , Type $nonNullPropertyTypeName " +
                                "is not an entity type, please check if a @Table annotation is added to type $nonNullPropertyTypeName"
                    )
                if (table.ktormEntityType != KtormEntityType.ENTITY_INTERFACE) {
                    error(
                        "Wrong references column: ${it.tablePropertyName}. Type $nonNullPropertyTypeName is not an interface entity type, " +
                                "References column must be interface entity type"
                    )
                }
                val primaryKeyColumns = table.columns.filter { column -> column.isPrimaryKey }
                if (primaryKeyColumns.isEmpty()) {
                    error("Wrong references column: ${it.tablePropertyName} , Table $nonNullPropertyTypeName must have a primary key")
                }
                if (primaryKeyColumns.size > 1) {
                    error("Wrong references column: ${it.tablePropertyName} , Table $nonNullPropertyTypeName cannot have more than one primary key")
                }
                it.referencesColumn = primaryKeyColumns.first()
            }
        return tableDefinitions to tableRet
    }

    public inner class KtormKspConfigVisitor(
        private val configBuilder: CodeGenerateConfig.Builder,
    ) : KSVisitorVoid() {

        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            logger.info("KtormKspConfigVisitor visitClassDeclaration: ${classDeclaration.toClassName()}")
            try {
                val kspConfig = classDeclaration.getAnnotationsByType(KtormKspConfig::class).first()
                val kspConfigAnnotation = classDeclaration.annotations.first {
                    it.annotationType.resolve().toClassName() == KtormKspConfig::class.asClassName()
                }
                val argumentMap = kspConfigAnnotation.arguments.associateBy { it.name!!.asString() }
                configBuilder.allowReflectionCreateEntity = kspConfig.allowReflectionCreateClassEntity
                configBuilder.extension = ExtensionGeneratorConfig(
                    kspConfig.extension.enableSequenceOf,
                    kspConfig.extension.enableClassEntitySequenceAddFun,
                    kspConfig.extension.enableClassEntitySequenceUpdateFun,
                    kspConfig.extension.enableInterfaceEntitySimulationDataClass
                )
                // namingStrategy
                val namingStrategyType = argumentMap[KtormKspConfig::namingStrategy.name]!!.value as KSType
                if (namingStrategyType.toClassName() != Nothing::class.asClassName()) {
                    if ((namingStrategyType.declaration as KSClassDeclaration).classKind != ClassKind.OBJECT) {
                        error("Wrong KtormKspConfig parameter:${KtormKspConfig::namingStrategy.name}, namingStrategy must be singleton.")
                    }
                    configBuilder.namingStrategy = namingStrategyType.toClassName()
                    try {
                        @Suppress("KotlinConstantConditions")
                        configBuilder.localNamingStrategy =
                            Class.forName(namingStrategyType.declaration.qualifiedName!!.asString()).kotlin.objectInstance as NamingStrategy
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            } catch (e: Exception) {
                logger.error(
                    "KtormKspConfigVisitor visitClassDeclaration error. className:" +
                            "${classDeclaration.toClassName()} file:${classDeclaration.containingFile?.filePath}"
                )
                throw e
            }
        }
    }

    public inner class EntityVisitor(
        private val tableDefinitions: MutableList<TableDefinition>
    ) : KSVisitorVoid() {

        private val sqlTypeClassName = SqlType::class.qualifiedName!!
        private val sqlTypeFactoryClassName = SqlTypeFactory::class.qualifiedName!!


        @OptIn(KspExperimental::class, KotlinPoetKspPreview::class)
        override fun visitClassDeclaration(
            classDeclaration: KSClassDeclaration,
            data: Unit,
        ) {
            val entityClassName = classDeclaration.toClassName()
            logger.info("EntityVisitor visitClassDeclaration: $entityClassName")
            try {
                val ktormEntityType = when (classDeclaration.classKind) {
                    ClassKind.INTERFACE -> {
                        val entityQualifiedName = Entity::class.qualifiedName
                        classDeclaration.findSuperTypeReference(entityQualifiedName!!)
                            ?: error("wrong entity class declaration: ${entityClassName.canonicalName}, Entity of interface type must inherit [$entityQualifiedName]")
                        KtormEntityType.ENTITY_INTERFACE
                    }

                    ClassKind.CLASS -> KtormEntityType.ANY_KIND_CLASS
                    else -> error("wrong entity class declaration: ${entityClassName.canonicalName}, classKind must to be Interface or Class")
                }
                val table = classDeclaration.getAnnotationsByType(Table::class).first()
                val tableClassName = NameGenerator.generateTableClassName(classDeclaration)
                val tableName = table.name

                val columnDefs = mutableListOf<ColumnDefinition>()
                val tableDef = TableDefinition(
                    tableName,
                    tableClassName,
                    table.entitySequenceName,
                    table.alias,
                    table.catalog,
                    table.schema,
                    entityClassName,
                    columnDefs,
                    classDeclaration.containingFile!!,
                    classDeclaration,
                    ktormEntityType
                )
                tableDefinitions.add(tableDef)

                // parse column definition
                for (property in classDeclaration.getAllProperties()) {
                    val propertyName = property.simpleName.asString()
                    if (property.isAnnotationPresent(Ignore::class) || propertyName in table.ignoreProperties) {
                        continue
                    }

                    val parent = property.parentDeclaration
                    if (parent is KSClassDeclaration && parent.classKind == ClassKind.CLASS && !property.hasBackingField) {
                        continue
                    }

                    if (tableDef.ktormEntityType == KtormEntityType.ENTITY_INTERFACE && propertyName in ignoreInterfaceEntityProperties) {
                        continue
                    }

                    columnDefs.add(ColumnDefinition(property, tableDef))
                }

            } catch (e: Exception) {
                logger.error(
                    "EntityVisitor visitClassDeclaration error. className:" +
                            "${classDeclaration.toClassName()} file:${classDeclaration.containingFile?.filePath}"
                )
                throw e
            }
        }
    }
}
