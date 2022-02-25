package org.ktorm.ksp.compiler.generator

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import org.ktorm.ksp.compiler.definition.CodeGenerateConfig
import org.ktorm.ksp.compiler.definition.ColumnDefinition
import org.ktorm.ksp.compiler.definition.ConverterDefinition
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
        Byte::class.asTypeName() to MemberName("org.ktorm.schema","bytes",true)
    )

    @OptIn(KotlinPoetKspPreview::class)
    public open fun generate(column: ColumnDefinition, dependencyFiles: MutableSet<KSFile>): CodeBlock {
        logger.info("generate column:${column.property.simpleName}")
        val isEnum = (column.propertyDeclaration.type.resolve().declaration as KSClassDeclaration).classKind == ClassKind.ENUM_CLASS
        val converterDefinition = when {
            column.converterDefinition != null -> column.converterDefinition
            isEnum && enumConverterDefinition != null -> enumConverterDefinition
            !isEnum && singleTypeConverterMap.containsKey(column.propertyTypeName) -> singleTypeConverterMap[column.propertyTypeName]
            else -> null
        }
        if (converterDefinition != null) {
            dependencyFiles.add(converterDefinition.converterClassDeclaration.containingFile!!)
            return buildCodeBlock {
                add(
                    "%T.convert(this, %S, %T::class)",
                    converterDefinition.converterName,
                    column.columnName,
                    column.propertyTypeName
                )
            }
        }
        // default initializer
        if (isEnum) {
            return buildCodeBlock {
                add("%M(%S)", defaultEnumInitializer, column.columnName)
            }
        }
        // default initializer
        val defaultFunction = defaultInitializerMap[column.propertyTypeName]
        if (defaultFunction != null) {
            return buildCodeBlock {
                add("%M(%S)", defaultFunction, column.columnName)
            }
        }
        error("Cannot find column generate function, property:${column.property.canonicalName} propertyTypeName:${column.propertyTypeName.canonicalName}")
    }

}