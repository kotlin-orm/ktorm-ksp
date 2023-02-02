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

@file:OptIn(KotlinPoetKspPreview::class, KspExperimental::class)

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
import org.ktorm.schema.SqlType

public class KtormProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        environment.logger.info("create KtormKspProcessor")
        return KtormProcessor(environment)
    }
}

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
            .filter { it.isReferences }
            .forEach {
                if (it.tableDefinition.ktormEntityType != KtormEntityType.ENTITY_INTERFACE) {
                    error("Wrong references column: ${it.tablePropertyName.canonicalName}, References Column are only allowed for interface entity type")
                }
                val nonNullPropertyTypeName = it.nonNullPropertyTypeName
                val table = entityClassMap[nonNullPropertyTypeName]
                    ?: error(
                        "Wrong references column: ${it.tablePropertyName.canonicalName} , Type $nonNullPropertyTypeName " +
                                "is not an entity type, please check if a @Table annotation is added to type $nonNullPropertyTypeName"
                    )
                if (table.ktormEntityType != KtormEntityType.ENTITY_INTERFACE) {
                    error(
                        "Wrong references column: ${it.tablePropertyName.canonicalName}. Type $nonNullPropertyTypeName is not an interface entity type, " +
                                "References column must be interface entity type"
                    )
                }
                val primaryKeyColumns = table.columns.filter { column -> column.isPrimaryKey }
                if (primaryKeyColumns.isEmpty()) {
                    error("Wrong references column: ${it.tablePropertyName.canonicalName} , Table $nonNullPropertyTypeName must have a primary key")
                }
                if (primaryKeyColumns.size > 1) {
                    error("Wrong references column: ${it.tablePropertyName.canonicalName} , Table $nonNullPropertyTypeName cannot have more than one primary key")
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
                classDeclaration.getAllProperties()
                    .forEach { ksProperty ->
                        val propertyKSType = ksProperty.type.resolve()
                        val propertyName = ksProperty.simpleName.asString()
                        if (ksProperty.isAnnotationPresent(Ignore::class)
                            || propertyName in table.ignoreProperties
                        ) {
                            logger.info(
                                "ignore column: ${tableDef.entityClassName.canonicalName}.$propertyName, " +
                                        "because the configuration specifies to ignore this column"
                            )
                            return@forEach
                        }
                        val parentDeclaration = ksProperty.parentDeclaration
                        if (parentDeclaration is KSClassDeclaration
                            && parentDeclaration.classKind != ClassKind.INTERFACE
                            && !ksProperty.hasBackingField
                        ) {
                            logger.info(
                                "ignore column: ${tableDef.entityClassName.canonicalName}.$propertyName, " +
                                        "because it has no backingField"
                            )
                            return@forEach
                        }
                        if (tableDef.ktormEntityType == KtormEntityType.ENTITY_INTERFACE && propertyName in ignoreInterfaceEntityProperties) {
                            logger.info(
                                "ignore column: ${tableDef.entityClassName.canonicalName}.$propertyName," +
                                        "because it is from 'org.ktorm.entity.Entity' class definition property."
                            )
                            return@forEach
                        }
                        val columnAnnotation = ksProperty.getAnnotationsByType(Column::class).firstOrNull()
                        val ksColumnAnnotation =
                            ksProperty.annotations.firstOrNull { anno -> anno.annotationType.resolve().declaration.qualifiedName?.asString() == columnQualifiedName }
                        val referencesAnnotation = ksProperty.getAnnotationsByType(References::class).firstOrNull()
                        if (columnAnnotation != null && referencesAnnotation != null) {
                            error("Only one of the annotations @Column or @References is allowed to be used alone on the property")
                        }

                        val sqlType =
                            ksColumnAnnotation?.arguments?.firstOrNull { it.name?.asString() == Column::sqlType.name }?.value as KSType?
                        var actualSqlType: ClassName? = null
                        var actualSqlFactoryType: ClassName? = null
                        if (sqlType != null && sqlType.declaration.qualifiedName!!.asString() != Nothing::class.qualifiedName) {
                            val sqlTypeDeclaration = sqlType.declaration as KSClassDeclaration
                            if (sqlTypeDeclaration.classKind != ClassKind.OBJECT) {
                                error(
                                    "wrong entity column declaration: ${entityClassName.canonicalName}." +
                                            "$propertyName, sqlType must be a Kotlin singleton object"
                                )
                            }
                            when {
                                sqlTypeDeclaration.findSuperTypeReference(sqlTypeClassName) != null -> {
                                    actualSqlType = sqlType.toClassName()
                                }
                                sqlTypeDeclaration.findSuperTypeReference(sqlTypeFactoryClassName) != null -> {
                                    actualSqlFactoryType = sqlType.toClassName()
                                }
                                else -> {
                                    error(
                                        "wrong entity column declaration: ${entityClassName.canonicalName}." +
                                                "$propertyName, sqlType must be typed of [$sqlTypeClassName] or " +
                                                "[$sqlTypeFactoryClassName]."
                                    )
                                }
                            }
                        }

                        val isPrimaryKey = ksProperty.getAnnotationsByType(PrimaryKey::class).any()
                        val columnName = columnAnnotation?.name ?: referencesAnnotation?.name ?: ""
                        val tablePropertyName = NameGenerator.generateTablePropertyName(tableClassName, ksProperty)

                        val columnDef = ColumnDefinition(
                            columnName,
                            isPrimaryKey,
                            propertyKSType.toTypeName(),
                            propertyKSType.isInline(),
                            MemberName(entityClassName, propertyName),
                            tablePropertyName,
                            actualSqlType,
                            actualSqlFactoryType,
                            ksProperty,
                            propertyKSType,
                            tableDef,
                            referencesAnnotation != null,
                            null
                        )
                        columnDefs.add(columnDef)
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

    private fun KSType.isInline(): Boolean {
        val cls = declaration as KSClassDeclaration
        return cls.isAnnotationPresent(JvmInline::class) && cls.modifiers.contains(Modifier.VALUE)
    }
}
