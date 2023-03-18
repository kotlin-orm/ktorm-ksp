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

package org.ktorm.ksp.compiler.test.generator

import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import com.tschuchort.compiletesting.SourceFile
import org.assertj.core.api.Assertions
import org.junit.Test
import org.ktorm.entity.EntitySequence
import org.ktorm.entity.toList
import org.ktorm.ksp.compiler.test.BaseTest
import org.ktorm.schema.*

class DefaultTablePropertyGeneratorTest : BaseTest() {

//    @Test
//    public fun `custom sqlType`() {
//        val (result1, result2) = twiceCompile(
//            SourceFile.kotlin(
//                "source.kt",
//                """
//                import org.ktorm.ksp.api.*
//                import org.ktorm.schema.SqlType
//                import java.sql.Types
//                import kotlin.reflect.jvm.jvmErasure
//                import java.sql.PreparedStatement
//                import java.sql.ResultSet
//                import kotlin.reflect.KProperty1
//
//                @Table
//                data class User(
//                    @PrimaryKey
//                    var id: Int?,
//                    @Column(sqlType = LocationWrapperSqlType::class)
//                    var location: LocationWrapper,
//                    @Column(sqlType = IntEnumSqlTypeFactory::class)
//                    var gender: Gender?,
//                    var age: Int,
//                )
//
//                enum class Gender {
//                    MALE,
//                    FEMALE
//                }
//
//                data class LocationWrapper(val underlying: String = "")
//
//                object LocationWrapperSqlType : SqlType<LocationWrapper>(Types.VARCHAR, "varchar") {
//
//                    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: LocationWrapper) {
//                        ps.setString(index, parameter.underlying)
//                    }
//
//                    override fun doGetResult(rs: ResultSet, index: Int): LocationWrapper? {
//                        return rs.getString(index)?.let { LocationWrapper(it) }
//                    }
//                }
//
//                object IntEnumSqlTypeFactory : SqlTypeFactory {
//
//                    @Suppress("UNCHECKED_CAST")
//                    override fun <T : Any> createSqlType(property: KProperty1<*, T?>): SqlType<T> {
//                        val returnType = property.returnType.jvmErasure.java
//                        if (returnType.isEnum) {
//                            return IntEnumSqlType(returnType as Class<out Enum<*>>) as SqlType<T>
//                        } else {
//                            throw IllegalArgumentException("The property is required to be typed of enum but actually: ${"$"}returnType")
//                        }
//                    }
//
//                    private class IntEnumSqlType<E : Enum<E>>(val enumClass: Class<E>) : SqlType<E>(Types.INTEGER, "int") {
//
//                        override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: E) {
//                            ps.setInt(index, parameter.ordinal)
//                        }
//
//                        override fun doGetResult(rs: ResultSet, index: Int): E? {
//                            return enumClass.enumConstants[rs.getInt(index)]
//                        }
//                    }
//                }
//                """,
//            )
//        )
//        Assertions.assertThat(result1.exitCode).isEqualTo(ExitCode.OK)
//        Assertions.assertThat(result2.exitCode).isEqualTo(ExitCode.OK)
//
//        val users = result2.getBaseTable("Users")
//        Assertions.assertThat(users["location"].sqlType.javaClass.canonicalName).isEqualTo("LocationWrapperSqlType")
//        Assertions.assertThat(users["gender"].sqlType.javaClass.canonicalName)
//            .isEqualTo("IntEnumSqlTypeFactory.IntEnumSqlType")
//    }
//
//    @Test
//    public fun `custom sqlType keyword identifier`() {
//        val (result1, result2) = twiceCompile(
//            SourceFile.kotlin(
//                "source.kt",
//                """
//                import org.ktorm.ksp.api.*
//                import org.ktorm.schema.SqlType
//                import java.sql.Types
//                import kotlin.reflect.jvm.jvmErasure
//                import java.sql.PreparedStatement
//                import java.sql.ResultSet
//                import kotlin.reflect.KProperty1
//
//                @Table
//                data class User(
//                    @PrimaryKey
//                    var id: Int?,
//                    @Column(sqlType = LocationWrapperSqlType::class)
//                    var `class`: LocationWrapper,
//                    @Column(sqlType = LocationWrapperSqlType::class)
//                    var operator: LocationWrapper,
//                )
//
//                data class LocationWrapper(val underlying: String = "")
//
//                object LocationWrapperSqlType : SqlType<LocationWrapper>(Types.VARCHAR, "varchar") {
//
//                    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: LocationWrapper) {
//                        ps.setString(index, parameter.underlying)
//                    }
//
//                    override fun doGetResult(rs: ResultSet, index: Int): LocationWrapper? {
//                        return rs.getString(index)?.let { LocationWrapper(it) }
//                    }
//                }
//                """,
//            )
//        )
//        Assertions.assertThat(result1.exitCode).isEqualTo(ExitCode.OK)
//        Assertions.assertThat(result2.exitCode).isEqualTo(ExitCode.OK)
//    }
//
//    @Test
//    public fun `generics column`() {
//        val (result1, result2) = twiceCompile(
//            SourceFile.kotlin(
//                "source.kt",
//                """
//                import org.ktorm.database.Database
//                import org.ktorm.entity.Entity
//                import org.ktorm.entity.EntitySequence
//                import org.ktorm.schema.SqlType
//                import org.ktorm.ksp.api.*
//                import java.sql.*
//                import java.time.LocalDate
//                import org.ktorm.schema.BaseTable
//                import org.ktorm.schema.varchar
//                import kotlin.reflect.KClass
//
//                @Table
//                data class User(
//                    @PrimaryKey
//                    var id: Int?,
//                    @Column(sqlType = ValueWrapperSqlType::class)
//                    var username: ValueWrapper<String>,
//                    var age: Int,
//                )
//
//                @KtormKspConfig(namingStrategy = CamelCaseToSnakeCaseNamingStrategy::class)
//                class KtormConfig
//
//                data class ValueWrapper<T>(var value: T)
//
//                object ValueWrapperSqlType : SqlType<ValueWrapper<String>>(Types.VARCHAR, "varchar") {
//
//                    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: ValueWrapper<String>) {
//                        ps.setString(index, parameter.value)
//                    }
//
//                    override fun doGetResult(rs: ResultSet, index: Int): ValueWrapper<String> {
//                        return ValueWrapper(rs.getString(index))
//                    }
//                }
//
//                object TestBridge {
//                    fun getUsers(database:Database): EntitySequence<User,Users> {
//                        return database.users
//                    }
//                }
//                """,
//            )
//        )
//        Assertions.assertThat(result1.exitCode).isEqualTo(ExitCode.OK)
//        Assertions.assertThat(result2.exitCode).isEqualTo(ExitCode.OK)
//        useDatabase { database ->
//            val users = result2.invokeBridge("getUsers", database) as EntitySequence<*, *>
//            Assertions.assertThat(users.toList().toString())
//                .isEqualTo("[User(id=1, username=ValueWrapper(value=jack), age=20), User(id=2, username=ValueWrapper(value=lucy), age=22), User(id=3, username=ValueWrapper(value=mike), age=22)]")
//        }
//    }

}
