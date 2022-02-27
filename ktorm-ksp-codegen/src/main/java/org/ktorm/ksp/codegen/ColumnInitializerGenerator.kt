package org.ktorm.ksp.codegen

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import org.ktorm.ksp.codegen.definition.ColumnDefinition
import org.ktorm.ksp.codegen.definition.ConverterDefinition
import java.math.BigDecimal
import java.sql.Time
import java.sql.Timestamp
import java.time.*
import java.util.*

public open class ColumnInitializerGenerator(
    config: CodeGenerateConfig,
    private val logger: KSPLogger
) {

    private val enumConverterDefinition: ConverterDefinition? = config.enumConverter
    private val singleTypeConverterMap: Map<ClassName, ConverterDefinition> = config.singleTypeConverters

    private val defaultEnumInitializer = MemberName("org.ktorm.schema", "enum")
    private val defaultInitializerMap = mapOf<TypeName, MemberName>(
        Int::class.asTypeName() to MemberName("org.ktorm.schema", "int", true),
        String::class.asTypeName() to MemberName("org.ktorm.schema", "varchar", true),
        Boolean::class.asTypeName() to MemberName("org.ktorm.schema", "boolean", true),
        Long::class.asTypeName() to MemberName("org.ktorm.schema", "long", true),
        Short::class.asTypeName() to MemberName("org.ktorm.schema", "short", true),
        Double::class.asTypeName() to MemberName("org.ktorm.schema", "double", true),
        Float::class.asTypeName() to MemberName("org.ktorm.schema", "float", true),
        BigDecimal::class.asTypeName() to MemberName("org.ktorm.schema", "decimal", true),
        Date::class.asTypeName() to MemberName("org.ktorm.schema", "date", true),
        Time::class.asTypeName() to MemberName("org.ktorm.schema", "time", true),
        Timestamp::class.asTypeName() to MemberName("org.ktorm.schema", "timestamp", true),
        LocalDateTime::class.asTypeName() to MemberName("org.ktorm.schema", "datetime", true),
        LocalDate::class.asTypeName() to MemberName("org.ktorm.schema", "date", true),
        LocalTime::class.asTypeName() to MemberName("org.ktorm.schema", "time", true),
        MonthDay::class.asTypeName() to MemberName("org.ktorm.schema", "monthDay", true),
        YearMonth::class.asTypeName() to MemberName("org.ktorm.schema", "yearMonth", true),
        Year::class.asTypeName() to MemberName("org.ktorm.schema", "year", true),
        Instant::class.asTypeName() to MemberName("org.ktorm.schema", "timestamp", true),
        UUID::class.asTypeName() to MemberName("org.ktorm.schema", "uuid", true),
        Byte::class.asTypeName() to MemberName("org.ktorm.schema", "bytes", true)
    )

    @OptIn(KotlinPoetKspPreview::class)
    public open fun generate(
        column: ColumnDefinition,
        dependencyFiles: MutableSet<KSFile>,
        config: CodeGenerateConfig
    ): CodeBlock {
        logger.info("generate column:$column")
        if (column.isReferences) {
            val referenceColumn =  column.referencesColumn!!
            dependencyFiles.add(referenceColumn.propertyDeclaration.containingFile!!)
            return doGenerate(
                column.columnName,
                config,
                referenceColumn.entityPropertyName,
                referenceColumn.converterDefinition,
                referenceColumn.isEnum,
                referenceColumn.propertyClassName,
                dependencyFiles
            )
        } else {
            return doGenerate(
                column.columnName,
                config,
                column.entityPropertyName,
                column.converterDefinition,
                column.isEnum,
                column.propertyClassName,
                dependencyFiles
            )
        }
    }

    private fun doGenerate(
        columnName: String,
        config: CodeGenerateConfig,
        entityPropertyName: MemberName,
        converterDefinition: ConverterDefinition?,
        isEnum: Boolean,
        propertyClassName: ClassName,
        dependencyFiles: MutableSet<KSFile>
    ): CodeBlock {
        var actualColumnName = columnName
        if (columnName.isEmpty()) {
            if (config.localNamingStrategy != null) {
                actualColumnName = config.localNamingStrategy.toColumnName(entityPropertyName.simpleName)
            } else if (config.namingStrategy == null) {
                actualColumnName = entityPropertyName.simpleName
            }
        }

        val actualConverterDefinition = when {
            converterDefinition != null -> converterDefinition
            isEnum && enumConverterDefinition != null -> enumConverterDefinition
            !isEnum && singleTypeConverterMap.containsKey(propertyClassName) -> singleTypeConverterMap[propertyClassName]
            else -> null
        }
        if (actualConverterDefinition != null) {
            dependencyFiles.add(actualConverterDefinition.converterClassDeclaration.containingFile!!)
            return buildCodeBlock {
                if (actualColumnName.isEmpty()) {
                    addStatement(
                        "%T.convert(this,%T.toColumnName(%S),%T::class)",
                        actualConverterDefinition.converterName,
                        config.namingStrategy,
                        entityPropertyName.simpleName,
                        propertyClassName
                    )
                } else {
                    addStatement(
                        "%T.convert(this,%S,%T::class)",
                        actualConverterDefinition.converterName,
                        actualColumnName,
                        propertyClassName
                    )
                }
            }
        }
        // default enum initializer
        if (isEnum) {
            return buildCodeBlock {
                if (actualColumnName.isEmpty()) {
                    addStatement(
                        "%M(%T.toColumnName(%S))",
                        defaultEnumInitializer,
                        config.namingStrategy,
                        entityPropertyName.simpleName
                    )
                } else {
                    addStatement("%M(%S)", defaultEnumInitializer, actualColumnName)
                }
            }
        }
        // default initializer
        val defaultFunction = defaultInitializerMap[propertyClassName]
        if (defaultFunction != null) {
            return buildCodeBlock {
                if (actualColumnName.isEmpty()) {
                    addStatement(
                        "%M(%T.toColumnName(%S))",
                        defaultFunction,
                        config.namingStrategy,
                        entityPropertyName.simpleName
                    )
                } else {
                    addStatement("%M(%S)", defaultFunction, actualColumnName)
                }
            }
        }
        error("Cannot find column generate function, property:${entityPropertyName.canonicalName} propertyTypeName:${propertyClassName.canonicalName}")
    }

}