/*
 * Copyright 2018-2021 the original author or authors.
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

package org.ktorm.ksp.compiler.test.config

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ktorm.ksp.tests.BaseKspTest
import org.ktorm.schema.IntSqlType
import org.ktorm.schema.VarcharSqlType

public class KtormKspConfigTest : BaseKspTest() {

    @Test
    public fun `singleton single type converter in ktormKspConfig`() {
        val (result1, result2) = twiceCompile(
            SourceFile.kotlin(
                "source.kt",
                """
                import org.ktorm.ksp.api.*
                import org.ktorm.schema.varchar
                import org.ktorm.schema.BaseTable
                import kotlin.reflect.KClass
                
                @Table
                data class User(
                    @PrimaryKey
                    var id: Int,
                    var username: Username,
                    var age: Int,
                    
                )
                
                data class Username(
                    val firstName:String,
                    val lastName:String
                )

                @KtormKspConfig(
                    singleTypeConverters = [UsernameConverter::class]
                )
                class KtormConfig

                object UsernameConverter: SingleTypeConverter<Username> {
                    public override fun convert(table: BaseTable<*>, columnName: String, propertyType: KClass<Username>): org.ktorm.schema.Column<Username> {
                        return with(table) {
                            varchar(columnName).transform({
                                val spilt = it.split("#")
                                Username(spilt[0],spilt[1])
                            },{
                                it.firstName +"#" + it.lastName
                            })
                        }
                    }
                }
                """,
            )
        ) {
            assertThat(it).contains("UsernameConverter.convert")
        }
        assertThat(result1.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        assertThat(result2.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        val baseTable = result2.getBaseTable("Users")
        val column = baseTable.columns.firstOrNull { it.name == "username" }
        assertThat(column).isNotNull
        assertThat(column!!.sqlType.typeCode).isEqualTo(VarcharSqlType.typeCode)
    }

    @Test
    public fun `non singleton single type converter in ktormKspConfig`() {
        val result = compile(
            SourceFile.kotlin(
                "source.kt",
                """
                import org.ktorm.ksp.api.*
                import org.ktorm.schema.varchar
                import org.ktorm.schema.BaseTable
                import kotlin.reflect.KClass
                
                @Table
                data class User(
                    @PrimaryKey
                    var id: Int,
                    var username: Username,
                    var age: Int,
                    
                )
                
                data class Username(
                    val firstName:String,
                    val lastName:String
                )

                @KtormKspConfig(
                    singleTypeConverters = [UsernameConverter::class]
                )
                class KtormConfig

                class UsernameConverter: SingleTypeConverter<Username> {
                    public override fun convert(table: BaseTable<*>, columnName: String, propertyType: KClass<Username>): org.ktorm.schema.Column<Username> {
                        return with(table) {
                            varchar(columnName).transform({
                                val spilt = it.split("#")
                                Username(spilt[0],spilt[1])
                            },{
                                it.firstName +"#" + it.lastName
                            })
                        }
                    }
                }
                """,
            )
        )
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains("converter must be singleton")
    }

    @Test
    public fun `singleton enum converter in ktormKspConfig`() {
        val (result1, result2) = twiceCompile(
            SourceFile.kotlin(
                "source.kt",
                """
                import org.ktorm.ksp.api.*
                import org.ktorm.schema.varchar
                import org.ktorm.schema.BaseTable
                import org.ktorm.schema.Column
                import org.ktorm.schema.int
                import kotlin.reflect.KClass
                
                @Table
                data class User(
                    @PrimaryKey
                    var id: Int,
                    var username: String,
                    var age: Int,
                    @org.ktorm.ksp.api.Column(converter = IntEnumConverter::class)
                    var gender: Gender
                )
                
                enum class Gender {
                    MALE,
                    FEMALE
                }               

                object IntEnumConverter: EnumConverter {
                    override fun <E : Enum<E>> convert(table: BaseTable<*>, columnName: String, propertyType: KClass<E>): Column<E>{
                        val values = propertyType.java.enumConstants
                        return with(table) {
                            int(columnName).transform( {values[it]} , {it.ordinal} )
                        }
                    }
                }
                """,
            )
        ) {
            println(it)
            assertThat(it).contains("IntEnumConverter.convert")
        }
        assertThat(result1.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        assertThat(result2.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        val baseTable = result2.getBaseTable("Users")
        val column = baseTable.columns.firstOrNull { it.name == "gender" }
        assertThat(column).isNotNull
        assertThat(column!!.sqlType.typeCode).isEqualTo(IntSqlType.typeCode)
    }

    @Test
    public fun `non singleton enum converter in ktormKspConfig`() {
        val result = compile(
            SourceFile.kotlin(
                "source.kt",
                """
                import org.ktorm.ksp.api.*
                import org.ktorm.schema.varchar
                import org.ktorm.schema.BaseTable
                import org.ktorm.schema.Column
                import org.ktorm.schema.int
                import kotlin.reflect.KClass
                
                @Table
                data class User(
                    @PrimaryKey
                    var id: Int,
                    var username: String,
                    var age: Int,
                    var gender: Gender
                )
                
                enum class Gender {
                    MALE,
                    FEMALE
                }               

                @KtormKspConfig(
                    enumConverter = IntEnumConverter::class
                )
                class KtormConfig

                class IntEnumConverter: EnumConverter {
                    override fun <E : Enum<E>> convert(table: BaseTable<*>, columnName: String, propertyType: KClass<E>): Column<E>{
                        val values = propertyType.java.enumConstants
                        return with(table) {
                            int(columnName).transform( {values[it]} , {it.ordinal} )
                        }
                    }
                }
                """,
            )
        )
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains("converter must be singleton")
    }


}