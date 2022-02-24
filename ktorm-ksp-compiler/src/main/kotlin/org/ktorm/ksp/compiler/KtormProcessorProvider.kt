@file:OptIn(ExperimentalStdlibApi::class, KotlinPoetKspPreview::class, KspExperimental::class)

package org.ktorm.ksp.compiler

import PrimaryKey
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
import org.ktorm.ksp.annotation.Column
import org.ktorm.ksp.annotation.KtormKspConfig
import org.ktorm.ksp.annotation.Table
import org.ktorm.ksp.compiler.definition.CodeGenerateConfig
import org.ktorm.ksp.compiler.definition.ColumnDefinition
import org.ktorm.ksp.compiler.definition.ConverterDefinition
import org.ktorm.ksp.compiler.definition.TableDefinition
import org.ktorm.ksp.compiler.generator.ColumnInitializerGenerator
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
            configAnnotated.accept(ConverterProviderVisitor(configBuilder), Unit)
            configBuilder.configDependencyFile = configAnnotated.containingFile
        }
        val config = configBuilder.build()
        logger.info("config:$config")

        // parse entity
        val symbols = resolver.getSymbolsWithAnnotation(Table::class.qualifiedName!!)
        logger.info("symbols:${symbols.toList()}")
        val tableDefinitions = mutableListOf<TableDefinition>()
        val tableRet = symbols.filter { !it.ktormValidate() }.toList()
        symbols.filter { it is KSClassDeclaration && it.ktormValidate() }
            .forEach { it.accept(EntityVisitor(tableDefinitions), Unit) }

        // start generate
        KtormCodeGenerator().generate(
            tableDefinitions, environment.codeGenerator, config, ColumnInitializerGenerator(config), logger
        )
        return configRet + tableRet
    }

    public inner class ConverterProviderVisitor(
        private val configBuilder: CodeGenerateConfig.Builder
    ) : KSVisitorVoid() {

        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            val kspConfig = classDeclaration.getAnnotationsByType(KtormKspConfig::class).first()
            configBuilder.allowReflectionCreateEntity = kspConfig.allowReflectionCreateEntity

            val kspConfigAnnotation = classDeclaration.annotations.first {
                it.annotationType.resolve().toClassName() == KtormKspConfig::class.asClassName()
            }
            val argumentMap = kspConfigAnnotation.arguments.associateBy { it.name!!.asString() }

            // enum converter
            val enumConverterType = argumentMap[KtormKspConfig::enumConverter.name]!!.value as KSType
            if (enumConverterType.toClassName() != Nothing::class.asClassName()) {
                if ((enumConverterType.declaration as KSClassDeclaration).classKind != ClassKind.OBJECT) {
                    error("Wrong KtormKspConfig parameter:${KtormKspConfig::enumConverter.name}, converter must be object instance.")
                }
                configBuilder.enumConverter = ConverterDefinition(
                    enumConverterType.toClassName(), enumConverterType.declaration as KSClassDeclaration
                )
            }

            // single type converter
            @Suppress("UNCHECKED_CAST") val singleTypeConverters =
                argumentMap[KtormKspConfig::singleTypeConverters.name]!!.value as List<KSType>
            if (singleTypeConverters.isNotEmpty()) {
                val singleTypeConverterMap = singleTypeConverters.asSequence().onEach {
                    if ((it.declaration as KSClassDeclaration).classKind != ClassKind.OBJECT) {
                        error("Wrong KtormKspConfig parameter:${KtormKspConfig::singleTypeConverters.name}, converter must be object instance.")
                    }
                }.associate {
//                        val typeParameter = it.declaration.typeParameters.first()
//                        logger.info("${it.toClassName()} superType: ${(it.declaration as KSClassDeclaration).superTypes.map { type -> type.toTypeName() }}")
//                        val supportType =
//                            ClassName(typeParameter.packageName.asString(), typeParameter.simpleName.asString())
                    val converterDefinition =
                        ConverterDefinition(it.toClassName(), it.declaration as KSClassDeclaration)
                    it.toClassName() to converterDefinition
                }
                configBuilder.singleTypeConverters = singleTypeConverterMap
            }
        }

    }


    public inner class EntityVisitor(
        private val tableDefinitions: MutableList<TableDefinition>,
    ) : KSVisitorVoid() {

        @OptIn(KspExperimental::class, KotlinPoetKspPreview::class)
        override fun visitClassDeclaration(
            classDeclaration: KSClassDeclaration,
            data: Unit,
        ) {
            val entityClassName = classDeclaration.toClassName()
            val table = classDeclaration.getAnnotationsByType(Table::class).first()
            // todo: custom name style
            val tableClassName = if (table.tableClassName.isEmpty()) {
                ClassName(entityClassName.packageName, entityClassName.simpleName + "s")
            } else {
                ClassName.bestGuess(table.tableClassName)
            }

            val tableName = table.tableName.ifEmpty { entityClassName.simpleName }
            val columnDefs = classDeclaration.getAllProperties().mapNotNull { ksProperty ->
                val propertyKSType = ksProperty.type.resolve()
                val propertyName = ksProperty.simpleName.asString()
                if (ksProperty.isAnnotationPresent(Transient::class) || propertyName in table.ignoreColumns) {
                    return@mapNotNull null
                }
                val columnAnnotation = ksProperty.getAnnotationsByType(Column::class).firstOrNull()
                val ksColumnAnnotation =
                    ksProperty.annotations.firstOrNull { anno -> anno.annotationType.resolve().declaration.qualifiedName?.asString() == columnQualifiedName }
                val converter =
                    ksColumnAnnotation?.arguments?.firstOrNull { anno -> anno.name?.asString() == Column::columnName.name }?.value as KSClassDeclaration?
                var converterDefinition: ConverterDefinition? = null
                if (converter != null && converter.toClassName() != Nothing::class.asClassName()) {
                    if (converter.classKind != ClassKind.OBJECT) {
                        error("Wrong converter type:${converter.toClassName()}, converter must be object instance.")
                    }
                    converterDefinition = ConverterDefinition(converter.toClassName(), converter)
                }
                val isPrimaryKey = ksProperty.getAnnotationsByType(PrimaryKey::class).any()
                // todo: custom name style
                val columnName = columnAnnotation?.columnName ?: propertyName
                ColumnDefinition(
                    columnName,
                    isPrimaryKey,
                    ksProperty,
                    propertyKSType.toClassName(),
                    MemberName(tableClassName, propertyName),
                    converterDefinition
                )
            }.toList()

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
            )
            tableDefinitions.add(tableDef)
        }

    }
}