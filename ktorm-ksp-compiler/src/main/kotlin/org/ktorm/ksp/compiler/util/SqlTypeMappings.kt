package org.ktorm.ksp.compiler.util

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import org.ktorm.ksp.api.EnumSqlTypeFactory
import org.ktorm.schema.*

fun KSType.getSqlType(resolver: Resolver): KSType? {
    val declaration = declaration as KSClassDeclaration
    if (declaration.classKind == ClassKind.ENUM_CLASS) {
        return resolver.getClassDeclarationByName<EnumSqlTypeFactory>()?.asType(emptyList())
    }

    val sqlType = when (declaration.qualifiedName?.asString()) {
        "kotlin.Boolean" -> BooleanSqlType::class
        "kotlin.Int" -> IntSqlType::class
        "kotlin.Short" -> ShortSqlType::class
        "kotlin.Long" -> LongSqlType::class
        "kotlin.Float" -> FloatSqlType::class
        "kotlin.Double" -> DoubleSqlType::class
        "kotlin.String" -> VarcharSqlType::class
        "kotlin.ByteArray" -> BytesSqlType::class
        "java.math.BigDecimal" -> DecimalSqlType::class
        "java.sql.Timestamp" -> TimestampSqlType::class
        "java.sql.Date" -> DateSqlType::class
        "java.sql.Time" -> TimeSqlType::class
        "java.time.InstantSqlType" -> InstantSqlType::class
        "java.time.LocalDateTime" -> LocalDateTimeSqlType::class
        "java.time.LocalDate" -> LocalDateSqlType::class
        "java.time.LocalTime" -> LocalTimeSqlType::class
        "java.time.MonthDay" -> MonthDaySqlType::class
        "java.time.YearMonth" -> YearMonthSqlType::class
        "java.time.Year" -> YearSqlType::class
        "java.util.UUID" -> UuidSqlType::class
        else -> null
    }

    return sqlType?.qualifiedName?.let { resolver.getClassDeclarationByName(it)?.asType(emptyList()) }
}