package org.ktorm.ksp.compiler

import com.squareup.kotlinpoet.*
import java.math.BigDecimal
import java.sql.Time
import java.sql.Timestamp
import java.time.*
import java.util.*

public open class KtormColumnFunctions {

    private val defaultFunctions = mapOf<TypeName, MemberName>(
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
    )

    public open fun getColumnCode(
        propertyName: MemberName,
        propertyTypeName: TypeName,
    ): CodeBlock {
        //default
        val defaultFunction = defaultFunctions[propertyTypeName]
        if (defaultFunction != null) {
            return buildCodeBlock {
                add("%M(%S)", defaultFunction, propertyName.simpleName)
            }
        }
        throw RuntimeException("Cannot find column generate function, propertyName:$propertyName propertyTypeName:$propertyTypeName")
    }

}