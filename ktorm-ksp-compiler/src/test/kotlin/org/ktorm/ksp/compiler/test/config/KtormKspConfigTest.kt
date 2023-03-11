///*
// * Copyright 2022-2023 the original author or authors.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package org.ktorm.ksp.compiler.test.config
//
//import com.tschuchort.compiletesting.KotlinCompilation
//import com.tschuchort.compiletesting.SourceFile
//import org.assertj.core.api.Assertions.assertThat
//import org.junit.Test
//import org.ktorm.schema.IntSqlType
//import org.ktorm.schema.VarcharSqlType
//
//public class KtormKspConfigTest : BaseKspTest() {
//
//    @Test
//    public fun `singleton single type converter in ktormKspConfig`() {
//        val (result1, result2) = twiceCompile(
//            SourceFile.kotlin(
//                "source.kt",
//                """
//                import org.ktorm.ksp.api.*
//                import org.ktorm.schema.SqlType
//                import java.sql.*
//                import kotlin.reflect.KClass
//
//                @Table
//                data class User(
//                    @PrimaryKey
//                    var id: Int,
//                    @Column(sqlType = UsernameSqlType::class)
//                    var username: Username,
//                    var age: Int,
//
//                )
//
//                data class Username(
//                    val firstName:String,
//                    val lastName:String
//                )
//
//                object UsernameSqlType : SqlType<Username>(Types.VARCHAR, "varchar") {
//
//                    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: Username) {
//                        ps.setString(index, parameter.firstName + "#" + parameter.lastName)
//                    }
//
//                    override fun doGetResult(rs: ResultSet, index: Int): Username? {
//                        val (firstName, lastName) = rs.getString(index)?.split("#") ?: return null
//                        return Username(firstName, lastName)
//                    }
//                }
//                """,
//            )
//        )
//        assertThat(result1.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
//        assertThat(result2.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
//        val baseTable = result2.getBaseTable("Users")
//        val column = baseTable.columns.firstOrNull { it.name == "username" }
//        assertThat(column).isNotNull
//        assertThat(column!!.sqlType.typeCode).isEqualTo(VarcharSqlType.typeCode)
//    }
//
//    @Test
//    public fun `singleton enum converter in ktormKspConfig`() {
//        val (result1, result2) = twiceCompile(
//            SourceFile.kotlin(
//                "source.kt",
//                """
//                import org.ktorm.ksp.api.*
//                import org.ktorm.schema.SqlType
//                import java.sql.*
//                import kotlin.reflect.KProperty1
//                import kotlin.reflect.jvm.jvmErasure
//
//                @Table
//                data class User(
//                    @PrimaryKey
//                    var id: Int,
//                    var username: String,
//                    var age: Int,
//                    @Column(sqlType = IntEnumSqlTypeFactory::class)
//                    var gender: Gender
//                )
//
//                enum class Gender {
//                    MALE,
//                    FEMALE
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
//        assertThat(result1.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
//        assertThat(result2.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
//        val baseTable = result2.getBaseTable("Users")
//        val column = baseTable.columns.firstOrNull { it.name == "gender" }
//        assertThat(column).isNotNull
//        assertThat(column!!.sqlType.typeCode).isEqualTo(IntSqlType.typeCode)
//    }
//}
