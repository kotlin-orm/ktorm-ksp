@file:OptIn(ExperimentalStdlibApi::class, KotlinPoetKspPreview::class, KspExperimental::class)

package org.ktorm.ksp.compiler

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.containingFile
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.isAnnotationPresent
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
import org.ktorm.entity.Entity
import org.ktorm.ksp.api.*
import org.ktorm.ksp.codegen.CodeGenerateConfig
import org.ktorm.ksp.codegen.ColumnInitializerGenerator
import org.ktorm.ksp.codegen.ExtensionGeneratorConfig
import org.ktorm.ksp.codegen.definition.ColumnDefinition
import org.ktorm.ksp.codegen.definition.ConverterDefinition
import org.ktorm.ksp.codegen.definition.KtormEntityType
import org.ktorm.ksp.codegen.definition.TableDefinition
import org.ktorm.ksp.compiler.generator.KtormCodeGenerator

public class KtormProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        environment.logger.info("create ktorm symbolProcessor")
        return KtormProcessor(environment)
    }
}

public class KtormProcessor(
    private val environment: SymbolProcessorEnvironment
) : SymbolProcessor {
    private val logger = environment.logger

    private companion object {
        private val columnQualifiedName = Column::class.qualifiedName!!
        private val ignoreInterfaceEntityProperties: Set<String> = setOf("entityClass", "properties")
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        logger.info("start ktorm ksp processor")
        // parse config
        val configSymbols = resolver.getSymbolsWithAnnotation(KtormKspConfig::class.qualifiedName!!)
        val configRet = configSymbols.filter { !it.ktormValidate() }.toList()
        logger.info("ktormKspConfigSymbols:${configSymbols.toList()}")
        val configClasses = configSymbols.filter { it is KSClassDeclaration && it.ktormValidate() }.toList()
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
        logger.info("config:$config")

        // parse entity
        // entityClassName -> tableClassName
        val entityTableMap = mutableMapOf<ClassName, ClassName>()
        val symbols = resolver.getSymbolsWithAnnotation(Table::class.qualifiedName!!)
        logger.info("symbols:${symbols.toList()}")
        val tableDefinitions = mutableListOf<TableDefinition>()
        val tableRet = symbols.filter { !it.ktormValidate() }.toList()
        symbols.filter { it is KSClassDeclaration && it.ktormValidate() }
            .forEach { it.accept(EntityVisitor(tableDefinitions, entityTableMap), Unit) }
        val entityClassMap = tableDefinitions.associateBy { it.entityClassName }
        logger.info("tableClassNameMap: $entityClassMap")
        tableDefinitions
            .asSequence()
            .flatMap { it.columns }
            .filter { it.isReferences }
            .forEach {
                if (it.tableDefinition.ktormEntityType != KtormEntityType.ENTITY_INTERFACE) {
                    error("Wrong references column: ${it.tablePropertyName.canonicalName}, References Column are only allowed for interface entity type")
                }
                val table = entityClassMap[it.propertyClassName]
                    ?: error("Wrong references column: ${it.tablePropertyName.canonicalName} , Type ${it.propertyClassName} " +
                            "is not an entity type, please check if a @Table annotation is added to type ${it.propertyClassName}")
                if (table.ktormEntityType != KtormEntityType.ENTITY_INTERFACE) {
                    error("Wrong references column: ${it.tablePropertyName.canonicalName}. Type ${it.propertyClassName} is not an interface entity type, " +
                            "References column must be interface entity type")
                }
                val primaryKeyColumns = table.columns.filter { column -> column.isPrimaryKey }
                if (primaryKeyColumns.isEmpty()) {
                    error("Wrong references column: ${it.tablePropertyName.canonicalName} , Table ${it.propertyClassName} must have a primary key")
                }
                if (primaryKeyColumns.size > 1) {
                    error("Wrong references column: ${it.tablePropertyName.canonicalName} , Table ${it.propertyClassName} cannot have more than one primary key")
                }
                it.referencesColumn = primaryKeyColumns.first()
            }
        // start generate
        KtormCodeGenerator().generate(
            tableDefinitions, environment.codeGenerator, config, ColumnInitializerGenerator(config, logger), logger
        )
        return configRet + tableRet
    }

    public inner class KtormKspConfigVisitor(
        private val configBuilder: CodeGenerateConfig.Builder,
    ) : KSVisitorVoid() {

        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
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
            )
            //namingStrategy
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

            // enum converter
            val enumConverterType = argumentMap[KtormKspConfig::enumConverter.name]!!.value as KSType
            if (enumConverterType.toClassName() != Nothing::class.asClassName()) {
                if ((enumConverterType.declaration as KSClassDeclaration).classKind != ClassKind.OBJECT) {
                    error("Wrong KtormKspConfig parameter:${KtormKspConfig::enumConverter.name}, converter must be singleton.")
                }
                configBuilder.enumConverter = ConverterDefinition(
                    enumConverterType.toClassName(), enumConverterType.declaration as KSClassDeclaration
                )
            }

            // single type converter
            @Suppress("UNCHECKED_CAST") val singleTypeConverters =
                argumentMap[KtormKspConfig::singleTypeConverters.name]!!.value as List<KSType>
            if (singleTypeConverters.isNotEmpty()) {
                val singleTypeConverterMap = singleTypeConverters.asSequence()
                    .onEach {
                        if ((it.declaration as KSClassDeclaration).classKind != ClassKind.OBJECT) {
                            error("Wrong KtormKspConfig parameter:${KtormKspConfig::singleTypeConverters.name} value:${it.declaration.qualifiedName!!.asString()} converter must be singleton.")
                        }
                    }.associate {
                        val singleTypeReference =
                            (it.declaration as KSClassDeclaration).findSuperTypeReference(SingleTypeConverter::class.qualifiedName!!)!!
                        val supportType = singleTypeReference.resolve().arguments.first().type!!.resolve().toClassName()
                        val converterDefinition =
                            ConverterDefinition(it.toClassName(), it.declaration as KSClassDeclaration)
                        supportType to converterDefinition
                    }
                configBuilder.singleTypeConverters = singleTypeConverterMap
            }

        }

    }


    public inner class EntityVisitor(
        private val tableDefinitions: MutableList<TableDefinition>,
        private val entityTableMap: MutableMap<ClassName, ClassName>
    ) : KSVisitorVoid() {

        @OptIn(KspExperimental::class, KotlinPoetKspPreview::class)
        override fun visitClassDeclaration(
            classDeclaration: KSClassDeclaration,
            data: Unit,
        ) {
            val entityClassName = classDeclaration.toClassName()
            val ktormEntityType = when (classDeclaration.classKind) {
                ClassKind.INTERFACE -> {
                    val entityQualifiedName = Entity::class.qualifiedName
                    classDeclaration.findSuperTypeReference(entityQualifiedName!!)
                        ?: error("wrong entity class declaration: ${entityClassName.canonicalName}, Entity of interface type must inherit [${entityQualifiedName}]")
                    KtormEntityType.ENTITY_INTERFACE
                }
                ClassKind.CLASS -> KtormEntityType.ANY_KIND_CLASS
                else -> error("wrong entity class declaration: ${entityClassName.canonicalName}, classKind must to be Interface or Class")
            }
            val table = classDeclaration.getAnnotationsByType(Table::class).first()
            val tableClassName = if (table.tableClassName.isEmpty()) {
                ClassName(entityClassName.packageName, entityClassName.simpleName.pluralNoun())
            } else {
                ClassName(entityClassName.packageName, table.tableClassName)
            }
            val tableName = table.tableName

            val columnDefs = mutableListOf<ColumnDefinition>()
            val tableDef = TableDefinition(
                tableName,
                tableClassName,
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
                    if (ksProperty.isAnnotationPresent(Ignore::class) || propertyName in table.ignoreColumns
                        || (tableDef.ktormEntityType == KtormEntityType.ENTITY_INTERFACE && propertyName in ignoreInterfaceEntityProperties)
                    ) {
                        logger.info("ignore column: ${tableDef.entityClassName.canonicalName}.$propertyName")
                        return@forEach
                    }
                    val columnAnnotation = ksProperty.getAnnotationsByType(Column::class).firstOrNull()
                    val ksColumnAnnotation =
                        ksProperty.annotations.firstOrNull { anno -> anno.annotationType.resolve().declaration.qualifiedName?.asString() == columnQualifiedName }
                    //converter
                    val converter =
                        ksColumnAnnotation?.arguments?.firstOrNull { anno -> anno.name?.asString() == Column::converter.name }?.value as KSType?
                    var converterDefinition: ConverterDefinition? = null
                    if (converter != null && converter.toClassName() != Nothing::class.asClassName()) {
                        val converterDeclaration = converter.declaration as KSClassDeclaration
                        if (converterDeclaration.classKind != ClassKind.OBJECT) {
                            error("Wrong converter type:${converter.toClassName()}, converter must be singleton.")
                        }
                        converterDefinition = ConverterDefinition(converter.toClassName(), converterDeclaration)
                    }

                    val isPrimaryKey = ksProperty.getAnnotationsByType(PrimaryKey::class).any()
                    val columnName = columnAnnotation?.columnName ?: ""
                    val tablePropertyName = if (columnAnnotation?.propertyName.isNullOrEmpty()) {
                        MemberName(tableClassName, propertyName)
                    } else {
                        MemberName(tableClassName, columnAnnotation!!.propertyName)
                    }
                    val columnDef = ColumnDefinition(
                        columnName,
                        isPrimaryKey,
                        propertyKSType.toClassName(),
                        MemberName(entityClassName, propertyName),
                        tablePropertyName,
                        converterDefinition,
                        ksProperty,
                        propertyKSType,
                        tableDef,
                        columnAnnotation?.isReferences ?: false,
                        null
                    )
                    columnDefs.add(columnDef)
                }
            entityTableMap[entityClassName] = tableClassName
        }


        internal fun String.pluralNoun(): String {
            when {
                this.endsWith("x") or
                        this.endsWith("s") or
                        this.endsWith("sh") or
                        this.endsWith("ch") -> {
                    return this + "es"
                }
                this.endsWith("y") -> {
                    return this.substring(0, this.length - 1) + "ies"
                }
                this.endsWith("o") -> {
                    return this + "es"
                }
                else -> {
                    return this + "s"
                }

            }
        }
    }
}
