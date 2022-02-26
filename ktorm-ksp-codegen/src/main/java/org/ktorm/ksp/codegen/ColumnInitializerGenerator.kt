package org.ktorm.ksp.codegen

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
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

        var columnName = column.sqlColumnName
        if (columnName.isEmpty()) {
            if (config.localNamingStrategy != null) {
                columnName = config.localNamingStrategy.toColumnName(column.propertyMemberName.simpleName)
            } else if (config.namingStrategy == null) {
                columnName = column.propertyMemberName.simpleName
            }
        }

        val isEnum =
            (column.propertyDeclaration.type.resolve().declaration as KSClassDeclaration).classKind == ClassKind.ENUM_CLASS
        val converterDefinition = when {
            column.converterDefinition != null -> column.converterDefinition
            isEnum && enumConverterDefinition != null -> enumConverterDefinition
            !isEnum && singleTypeConverterMap.containsKey(column.propertyClassName) -> singleTypeConverterMap[column.propertyClassName]
            else -> null
        }
        if (converterDefinition != null) {
            dependencyFiles.add(converterDefinition.converterClassDeclaration.containingFile!!)
            return buildCodeBlock {
                if (columnName.isNullOrEmpty()) {
                    addStatement(
                        "%T.convert(this,%T.toColumnName(%S),%T::class)",
                        converterDefinition.converterName,
                        config.namingStrategy,
                        column.propertyMemberName.simpleName,
                        column.propertyClassName
                    )
                } else {
                    addStatement("%T.convert(this,%S,%T::class)", converterDefinition.converterName, columnName, column.propertyClassName)
                }
            }
        }
        // default enum initializer
        if (isEnum) {
            return buildCodeBlock {
                if (columnName.isNullOrEmpty()) {
                    addStatement(
                        "%M(%T.toColumnName(%S))",
                        defaultEnumInitializer,
                        config.namingStrategy,
                        column.propertyMemberName.simpleName
                    )
                } else {
                    addStatement("%M(%S)", defaultEnumInitializer, columnName)
                }
            }
        }
        // default initializer
        val defaultFunction = defaultInitializerMap[column.propertyClassName]
        if (defaultFunction != null) {
            return buildCodeBlock {
                if (columnName.isNullOrEmpty()) {
                    addStatement(
                        "%M(%T.toColumnName(%S))",
                        defaultFunction,
                        config.namingStrategy,
                        column.propertyMemberName.simpleName
                    )
                } else {
                    addStatement("%M(%S)", defaultFunction, columnName)
                }
            }
        }
        error("Cannot find column generate function, property:${column.propertyMemberName.canonicalName} propertyTypeName:${column.propertyClassName.canonicalName}")
    }

}