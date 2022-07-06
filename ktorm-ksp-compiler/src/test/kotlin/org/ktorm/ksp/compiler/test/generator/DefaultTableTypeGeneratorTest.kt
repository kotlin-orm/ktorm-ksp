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

package org.ktorm.ksp.compiler.test.generator

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ktorm.ksp.compiler.test.BaseTest

public class DefaultTableTypeGeneratorTest : BaseTest() {

    @Test
    public fun `dataClass entity`() {
        val result = compile(
            SourceFile.kotlin(
                "source.kt",
                """
                import org.ktorm.ksp.api.PrimaryKey
                import org.ktorm.ksp.api.Table
                
                @Table
                data class User(
                    @PrimaryKey
                    var id: Int? = null,
                    var username: String,
                    var age: Int = 0
                )
                """
            )
        )
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    public fun `table annotation`() {
        val (result1, result2) = twiceCompile(
            SourceFile.kotlin(
                "source.kt",
                """
                import org.ktorm.ksp.api.PrimaryKey
                import org.ktorm.ksp.api.Table
                
                @Table(tableName = "t_user","UserTable","t_user_alias","catalog","schema", ["age"])
                data class User(
                    @PrimaryKey
                    var id: Int,
                    var username: String,
                ) {
                    var age: Int = 10
                }
                """,
            )
        )
        assertThat(result1.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        assertThat(result2.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        val baseTable = result2.getBaseTable("UserTable")
        assertThat(baseTable.tableName).isEqualTo("t_user")
        assertThat(baseTable.alias).isEqualTo("t_user_alias")
        assertThat(baseTable.catalog).isEqualTo("catalog")
        assertThat(baseTable.columns.map { it.name }.toSet()).isEqualTo(setOf("id", "username"))
    }

    @Test
    public fun `test non constructor properties with data class`() {
        val (result1, result2) = twiceCompile(
            SourceFile.kotlin(
                "source.kt",
                """
                import org.ktorm.ksp.api.PrimaryKey
                import org.ktorm.ksp.api.Table
                
                @Table(tableName = "t_user","UserTable","t_user_alias","catalog","schema")
                data class User(
                    @PrimaryKey
                    var id: Int,
                    var username: String,
                ) {
                    var age: Int = 10
                }
                """,
            )
        )
        assertThat(result1.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        assertThat(result2.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        val baseTable = result2.getBaseTable("UserTable")
        assertThat(baseTable.tableName).isEqualTo("t_user")
        assertThat(baseTable.alias).isEqualTo("t_user_alias")
        assertThat(baseTable.catalog).isEqualTo("catalog")
        assertThat(baseTable.columns.map { it.name }.toSet()).isEqualTo(setOf("id", "username", "age"))
    }

    @Test
    public fun `data class constructor with default parameters column`() {
        val (result1, result2) = twiceCompile(
            SourceFile.kotlin(
                "source.kt",
                """
                import org.ktorm.ksp.api.PrimaryKey
                import org.ktorm.ksp.api.Table
                
                @Table
                data class User(
                    @PrimaryKey
                    var id: Int,
                    var username: String = "lucy",
                )
                """,
            )
        ) {
            // use reflection create instance
            assertThat(it).contains("public open class Users", "constructor.callBy(parameterMap)")
        }
        assertThat(result1.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        assertThat(result2.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        result2.getBaseTable("Users")
    }

    @Test
    public fun `data class constructor with parameters not column`() {
        val result = compile(
            SourceFile.kotlin(
                "source.kt",
                """
                import org.ktorm.ksp.api.Ignore
                import org.ktorm.ksp.api.PrimaryKey
                import org.ktorm.ksp.api.Table
                
                @Table
                data class User(
                    @PrimaryKey
                    var id: Int,
                    @Ignore
                    var username: String,
                    var age: Int
                )
                """,
            )
        )
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains("Construct parameter not exists in tableDefinition")
    }

    @Test
    public fun `column definition does not contain isReference parameters`() {
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
                    @Column(columnName = "c_username", propertyName = "p_username", converter = MyStringConverter::class)
                    var username: String,
                    var age: Int
                )

                object MyStringConverter: SingleTypeConverter<String> {
                    public override fun convert(table: BaseTable<*>, columnName: String, propertyType: KClass<String>): org.ktorm.schema.Column<String> {
                        return with(table) {
                            varchar(columnName).transform({it.uppercase()},{it.lowercase()})
                        }
                    }
                }
                """,
            )
        ) {
            it.contains("""MyStringConverter.convert(this,"c_username",String::class)""")
            it.contains("""val p_username: Column<String>""")
        }
        assertThat(result1.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        assertThat(result2.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        val baseTable = result2.getBaseTable("Users")
        assertThat(baseTable.columns.any { it.name == "c_username" })
    }

    @Test
    public fun `column reference`() {
        val (result1, result2) = twiceCompile(
            SourceFile.kotlin(
                "source.kt",
                """
                import org.ktorm.ksp.api.*
                import org.ktorm.entity.Entity 
                import org.ktorm.schema.varchar
                import org.ktorm.schema.BaseTable
                import kotlin.reflect.KClass
                
                @Table
                interface User: Entity<User> {
                    @PrimaryKey
                    var id: Int
                    var username: String
                    var age: Int
                    @Column(isReferences = true)
                    var school: School
                }

                @Table
                interface School: Entity<School> {
                    @PrimaryKey
                    var id: Int
                    var schoolName: String
                }
                """,
            )
        ) {
            println(it)
        }
        assertThat(result1.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        assertThat(result2.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        val baseTable = result2.getBaseTable("Users")
        val school = baseTable.columns.firstOrNull { it.name == "school" }
        assertThat(school).isNotNull
        assertThat(school!!.referenceTable).isNotNull
        val referenceTable = school.referenceTable
        assertThat(referenceTable!!.tableName).isEqualTo("School")
    }

    @Test
    public fun `column reference is data class`() {
        val (result, _) = twiceCompile(
            SourceFile.kotlin(
                "source.kt",
                """
                import org.ktorm.ksp.api.*
                import org.ktorm.entity.Entity
                import org.ktorm.schema.varchar
                import org.ktorm.schema.BaseTable
                import kotlin.reflect.KClass
                
                @Table
                interface User: Entity<User> {
                    @PrimaryKey
                    var id: Int
                    var username: String
                    var age: Int
                    @Column(isReferences = true)
                    var school: School
                }

                @Table
                data class School(
                    @PrimaryKey
                    var id: Int,
                    var schoolName: String
                )
                """,
            )
        ) {
            println(it)
        }
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains("References column must be interface entity type")
    }

    @Test
    public fun `column reference not an entity type`() {
        val result = compile(
            SourceFile.kotlin(
                "source.kt",
                """
                import org.ktorm.ksp.api.*
                import org.ktorm.entity.Entity
                import org.ktorm.schema.varchar
                import org.ktorm.schema.BaseTable
                import kotlin.reflect.KClass
                
                @Table
                interface User: Entity<User> {
                    @PrimaryKey
                    var id: Int
                    var username: String
                    var age: Int
                    @Column(isReferences = true)
                    var school: School
                }

                data class School(
                    var id: Int,
                    var schoolName: String
                )
                """,
            )
        )
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains("is not an entity type")
    }

    @Test
    public fun `interface entity`() {
        val result = compile(
            SourceFile.kotlin(
                "source.kt",
                """
                import org.ktorm.entity.Entity
                import org.ktorm.ksp.api.PrimaryKey
                import org.ktorm.ksp.api.Table
                @Table
                interface User: Entity<User> {
                    @PrimaryKey
                    var id: Int
                    var username: String
                    var age: Int 
                }
                """
            )
        )
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    public fun `column reference in data class`() {
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
                    var username: String,
                    var age: Int,
                    @Column(isReferences = true)
                    var school: School
                )

                @Table
                interface School: Entity<School> {
                    @PrimaryKey
                    var id: Int
                    var schoolName: String
                }
                """,
            )
        )
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains("References Column are only allowed for interface entity type")
    }

    @Test
    public fun `disable allowReflectionCreateClassEntity`() {
        val (result1, result2) = twiceCompile(
            SourceFile.kotlin(
                "source.kt",
                """
                import org.ktorm.ksp.api.*
                
                @Table
                data class User(
                    @PrimaryKey
                    var id: Int,
                    var username: String = "",
                )
                
                @KtormKspConfig(
                    allowReflectionCreateClassEntity = false
                )
                class KtormConfig
                """,
            )
        ) {
            assertThat(it).doesNotContain("constructor.callBy(parameterMap)")
        }
        assertThat(result1.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        assertThat(result2.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

}