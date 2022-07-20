/*
 *  Copyright 2018-2021 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.ktorm.ksp.codegen.test

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.assertj.core.api.Assertions
import org.junit.Test
import org.ktorm.entity.EntitySequence
import org.ktorm.entity.toList
import org.ktorm.ksp.tests.BaseKspTest
import org.ktorm.schema.*

public class DefaultTablePropertyGeneratorTest : BaseKspTest() {

    @Test
    public fun `default column initializer`() {
        val (result1, result2) = twiceCompile(
            SourceFile.kotlin(
                "source.kt",
                """
                import org.ktorm.database.Database
                import org.ktorm.entity.Entity
                import org.ktorm.entity.EntitySequence
                import org.ktorm.ksp.api.*
                import java.math.BigDecimal
                import java.sql.Time
                import java.sql.Date
                import java.sql.Timestamp
                import java.time.*
                import java.util.UUID
                    
                @Table
                @Suppress("ArrayInDataClass")
                data class User(
                    val int: Int,
                    val string: String,
                    val boolean: Boolean,
                    val long: Long,
                    val short: Short,
                    val double: Double,
                    val float: Float,
                    val bigDecimal: BigDecimal,
                    val date: Date,
                    val time: Time,
                    val timestamp: Timestamp,
                    val localDateTime: LocalDateTime,
                    val localDate: LocalDate,
                    val localTime: LocalTime,
                    val monthDay: MonthDay,
                    val yearMonth: YearMonth,
                    val year: Year,
                    val instant: Instant,
                    val uuid: UUID,
                    val byteArray: ByteArray,
                    val gender: Gender
                )

                enum class Gender {
                    MALE,
                    FEMALE
                }
                """,
            )
        )
        Assertions.assertThat(result1.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        Assertions.assertThat(result2.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        val table = result2.getBaseTable("Users")
        table.columns.forEach {
            when (it.name) {
                "int" -> Assertions.assertThat(it.sqlType).isEqualTo(IntSqlType)
                "string" -> Assertions.assertThat(it.sqlType).isEqualTo(VarcharSqlType)
                "boolean" -> Assertions.assertThat(it.sqlType).isEqualTo(BooleanSqlType)
                "long" -> Assertions.assertThat(it.sqlType).isEqualTo(LongSqlType)
                "short" -> Assertions.assertThat(it.sqlType).isEqualTo(ShortSqlType)
                "double" -> Assertions.assertThat(it.sqlType).isEqualTo(DoubleSqlType)
                "float" -> Assertions.assertThat(it.sqlType).isEqualTo(FloatSqlType)
                "bigDecimal" -> Assertions.assertThat(it.sqlType).isEqualTo(DecimalSqlType)
                "date" -> Assertions.assertThat(it.sqlType).isEqualTo(DateSqlType)
                "time" -> Assertions.assertThat(it.sqlType).isEqualTo(TimeSqlType)
                "timestamp" -> Assertions.assertThat(it.sqlType).isEqualTo(TimestampSqlType)
                "localDateTime" -> Assertions.assertThat(it.sqlType).isEqualTo(LocalDateTimeSqlType)
                "localDate" -> Assertions.assertThat(it.sqlType).isEqualTo(LocalDateSqlType)
                "localTime" -> Assertions.assertThat(it.sqlType).isEqualTo(LocalTimeSqlType)
                "monthDay" -> Assertions.assertThat(it.sqlType).isEqualTo(MonthDaySqlType)
                "yearMonth" -> Assertions.assertThat(it.sqlType).isEqualTo(YearMonthSqlType)
                "year" -> Assertions.assertThat(it.sqlType).isEqualTo(YearSqlType)
                "instant" -> Assertions.assertThat(it.sqlType).isEqualTo(InstantSqlType)
                "uuid" -> Assertions.assertThat(it.sqlType).isEqualTo(UuidSqlType)
                "byteArray" -> Assertions.assertThat(it.sqlType).isEqualTo(BytesSqlType)
                "gender" -> Assertions.assertThat(it.sqlType).isInstanceOf(EnumSqlType::class.java)
            }
        }
    }

    @Test
    public fun `generics column`() {
        val (result1, result2) = twiceCompile(
            SourceFile.kotlin(
                "source.kt",
                """
                import org.ktorm.database.Database
                import org.ktorm.entity.Entity
                import org.ktorm.entity.EntitySequence
                import org.ktorm.ksp.api.*
                import java.time.LocalDate
                import org.ktorm.schema.BaseTable
                import org.ktorm.schema.varchar
                import kotlin.reflect.KClass
                    
                @Table
                data class User(
                    @PrimaryKey
                    var id: Int?,
                    @Column(converter = StringValueWrapperConverter::class)
                    var username: ValueWrapper<String>,
                    var age: Int,
                )

                @KtormKspConfig(namingStrategy = CamelCaseToSnakeCaseNamingStrategy::class)
                class KtormConfig

                data class ValueWrapper<T>(var value: T)
                
                object StringValueWrapperConverter : SingleTypeConverter<ValueWrapper<String>> {
                    override fun convert(
                        table: BaseTable<*>,
                        columnName: String,
                        propertyType: KClass<ValueWrapper<String>>
                    ): org.ktorm.schema.Column<ValueWrapper<String>> {
                        return with(table) {
                            varchar(columnName).transform({ ValueWrapper(it) }, { it.value })
                        }
                    }
                }

                object TestBridge {
                    fun getUsers(database:Database): EntitySequence<User,Users> {
                        return database.users
                    }
                }
                """,
            )
        )
        Assertions.assertThat(result1.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        Assertions.assertThat(result2.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        useDatabase { database ->
            val users = result2.invokeBridge("getUsers", database) as EntitySequence<*, *>
            Assertions.assertThat(users.toList().toString())
                .isEqualTo("[User(id=1, username=ValueWrapper(value=jack), age=20), User(id=2, username=ValueWrapper(value=lucy), age=22), User(id=3, username=ValueWrapper(value=mike), age=22)]")
        }
    }

}
