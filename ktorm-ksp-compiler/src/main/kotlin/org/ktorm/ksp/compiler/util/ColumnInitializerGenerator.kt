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

@file:OptIn(KotlinPoetKspPreview::class)

package org.ktorm.ksp.compiler.util

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import org.ktorm.ksp.spi.TableGenerateContext
import org.ktorm.ksp.spi.ColumnMetadata
import java.math.BigDecimal
import java.sql.Date
import java.sql.Time
import java.sql.Timestamp
import java.time.*
import java.util.*

/**
 *
 * default support type
 * +──────────────────────────+────────────────+────────────────────────+──────────────────+
 * | Kotlin Type              | Function Name  | Underlying SQL Type    | JDBC Type Code   |
 * +──────────────────────────+────────────────+────────────────────────+──────────────────+
 * | kotlin.Boolean           | boolean        | boolean                | Types.BOOLEAN    |
 * | kotlin.Int               | int            | int                    | Types.INTEGER    |
 * | kotlin.Short             | short          | smallint               | Types.SMALLINT   |
 * | kotlin.Long              | long           | bigint                 | Types.BIGINT     |
 * | kotlin.Float             | float          | float                  | Types.FLOAT      |
 * | kotlin.Double            | double         | double                 | Types.DOUBLE     |
 * | kotlin.BigDecimal        | decimal        | decimal                | Types.DECIMAL    |
 * | kotlin.String            | varchar        | varchar                | Types.VARCHAR    |
 * | java.sql.Date            | jdbcDate       | date                   | Types.DATE       |
 * | java.sql.Time            | jdbcTime       | time                   | Types.TIME       |
 * | java.sql.Timestamp       | jdbcTimestamp  | timestamp              | Types.TIMESTAMP  |
 * | java.time.LocalDateTime  | datetime       | datetime               | Types.TIMESTAMP  |
 * | java.time.LocalDate      | date           | date                   | Types.DATE       |
 * | java.time.LocalTime      | time           | time                   | Types.TIME       |
 * | java.time.MonthDay       | monthDay       | varchar                | Types.VARCHAR    |
 * | java.time.YearMonth      | yearMonth      | varchar                | Types.VARCHAR    |
 * | java.time.Year           | year           | int                    | Types.INTEGER    |
 * | java.time.Instant        | timestamp      | timestamp              | Types.TIMESTAMP  |
 * | java.util.UUID           | uuid           | uuid                   | Types.OTHER      |
 * | kotlin.ByteArray         | bytes          | bytes                  | Types.BINARY     |
 * | kotlin.Enum              | enum           | enum                   | Types.VARCHAR    |
 * +──────────────────────────+────────────────+────────────────────────+──────────────────+
 */
public object ColumnInitializerGenerator {
    private val enumSqlTypeFunction = MemberName("org.ktorm.schema", "enum", true)
    private val sqlTypeFunctions = mapOf<TypeName, MemberName>(
        Int::class.asTypeName() to MemberName("org.ktorm.schema", "int", true),
        String::class.asTypeName() to MemberName("org.ktorm.schema", "varchar", true),
        Boolean::class.asTypeName() to MemberName("org.ktorm.schema", "boolean", true),
        Long::class.asTypeName() to MemberName("org.ktorm.schema", "long", true),
        Short::class.asTypeName() to MemberName("org.ktorm.schema", "short", true),
        Double::class.asTypeName() to MemberName("org.ktorm.schema", "double", true),
        Float::class.asTypeName() to MemberName("org.ktorm.schema", "float", true),
        BigDecimal::class.asTypeName() to MemberName("org.ktorm.schema", "decimal", true),
        Date::class.asTypeName() to MemberName("org.ktorm.schema", "jdbcDate", true),
        Time::class.asTypeName() to MemberName("org.ktorm.schema", "jdbcTime", true),
        Timestamp::class.asTypeName() to MemberName("org.ktorm.schema", "jdbcTimestamp", true),
        LocalDateTime::class.asTypeName() to MemberName("org.ktorm.schema", "datetime", true),
        LocalDate::class.asTypeName() to MemberName("org.ktorm.schema", "date", true),
        LocalTime::class.asTypeName() to MemberName("org.ktorm.schema", "time", true),
        MonthDay::class.asTypeName() to MemberName("org.ktorm.schema", "monthDay", true),
        YearMonth::class.asTypeName() to MemberName("org.ktorm.schema", "yearMonth", true),
        Year::class.asTypeName() to MemberName("org.ktorm.schema", "year", true),
        Instant::class.asTypeName() to MemberName("org.ktorm.schema", "timestamp", true),
        UUID::class.asTypeName() to MemberName("org.ktorm.schema", "uuid", true),
        ByteArray::class.asTypeName() to MemberName("org.ktorm.schema", "bytes", true)
    )

    /**
     * Generate column initializer code.
     */
    public fun generate(
        context: TableGenerateContext,
        column: ColumnMetadata
    ): CodeBlock {
        val (_, _, logger, dependencyFiles) = context
        logger.info("generate column:$column")
        if (column.isReferences) {
            val referenceColumn = column.referencesColumn!!
            dependencyFiles.add(referenceColumn.propertyDeclaration.containingFile!!)
        }
        val columnName = NameGenerator.generateSqlColumnName(context, column)
        val targetColumn = if (column.isReferences) column.referencesColumn!! else column
        val (_, _, propertyTypeName, _, entityPropertyName, _, sqlType, sqlTypeFactory) = targetColumn
        val isEnum = targetColumn.isEnum
        val nonNullPropertyTypeName = targetColumn.nonNullPropertyTypeName

        if (sqlType != null) {
            return CodeBlock.of("registerColumn(%L,·%T)", columnName, sqlType)
        }

        if (sqlTypeFactory != null) {
            return CodeBlock.of(
                "registerColumn(%L,·%T.createSqlType(%L))",
                columnName,
                sqlTypeFactory,
                entityPropertyName.reference()
            )
        }

        // default enum initializer
        if (isEnum) {
            return CodeBlock.of("%M<%T>(%L)", enumSqlTypeFunction, nonNullPropertyTypeName, columnName)
        }

        // default initializer
        val defaultFunction = sqlTypeFunctions[nonNullPropertyTypeName]
        if (defaultFunction != null) {
            return CodeBlock.of("%M(%L)", defaultFunction, columnName)
        }

        error(
            "Cannot find column generate function, property:${entityPropertyName.canonicalName} " +
                    "propertyTypeName:$propertyTypeName"
        )
    }
}
