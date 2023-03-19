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

import org.junit.Test
import org.ktorm.ksp.compiler.test.BaseTest

class TableClassGeneratorTest : BaseTest() {

    @Test
    fun `dataClass entity`() = runKotlin("""
        @Table
        data class User(
            @PrimaryKey
            var id: Int? = null,
            var username: String,
            var age: Int = 0
        )
        
        fun run() {
            assert(Users.tableName == "user")
            assert(Users.columns.map { it.name }.toSet() == setOf("id", "username", "age"))
        }
    """.trimIndent())

    @Test
    fun `data class keyword identifier`() = runKotlin("""
        @Table
        data class User(
            @PrimaryKey
            var id: Int,
            var `class`: String,
            var operator: String,
        ) {
            var `interface`: String = ""
            var constructor: String = ""
        }
        
        fun run() {
            assert(Users.tableName == "user")
            assert(Users.columns.map { it.name }.toSet() == setOf("id", "class", "operator", "interface", "constructor"))
        }
    """.trimIndent())

    @Test
    fun `table annotation`() = runKotlin("""
        @Table(
            name = "t_user", 
            alias = "t_user_alias", 
            catalog = "catalog", 
            schema = "schema", 
            className = "UserTable", 
            entitySequenceName = "userTable", 
            ignoreProperties = ["age"]
        )
        data class User(
            @PrimaryKey
            var id: Int,
            var username: String,
        ) {
            var age: Int = 10
        }
        
        fun run() {
            assert(UserTable.tableName == "t_user")
            assert(UserTable.alias == "t_user_alias")
            assert(UserTable.catalog == "catalog")
            assert(UserTable.schema == "schema")
            assert(UserTable.columns.map { it.name }.toSet() == setOf("id", "username"))
            println(database.userTable)
        }
    """.trimIndent())

    @Test
    fun `data class constructor with default parameters column`() = runKotlin("""
        @Table
        data class User(
            @PrimaryKey
            var id: Int,
            var username: String,
            var phone: String? = "12345"
        )
        
        fun run() {
            val user = database.users.first { it.id eq 1 }
            assert(user.username == "jack")
            assert(user.phone == null)
        }
    """.trimIndent())

    @Test
    fun `data class constructor with default parameters column allowing reflection`() = runKotlin("""
        @Table
        data class User(
            @PrimaryKey
            var id: Int,
            var username: String,
            var phone: String? = "12345"
        )
        
        fun run() {
            val user = database.users.first { it.id eq 1 }
            assert(user.username == "jack")
            assert(user.phone == "12345")
        }
    """.trimIndent(), "ktorm.allowReflection" to "true")

    @Test
    fun `ignore properties`() = runKotlin("""
        @Table(ignoreProperties = ["email"])
        data class User(
            @PrimaryKey
            var id: Int,
            var age: Int,
            @Ignore
            var username: String = ""
        ) {
            var email: String = ""
        }
        
        fun run() {
            assert(Users.tableName == "user")
            assert(Users.columns.map { it.name }.toSet() == setOf("id", "age"))
        }
    """.trimIndent())
//
//    @Test
//    public fun `column has no backingField`() {
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
//                    var age: Int
//                ) {
//                    val username: String
//                        get() = "username"
//                }
//                """,
//            )
//        )
//        assertThat(result1.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
//        assertThat(result2.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
//        val baseTable = result2.getBaseTable("Users")
//        assertThat(baseTable.columns.none { it.name == "username" })
//    }
//
//    @Test
//    public fun `column definition does not contain @References`() {
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
//                    @Column(name = "c_username", propertyName = "p_username", sqlType = MyStringSqlType::class)
//                    var username: String,
//                    var age: Int
//                )
//
//                object MyStringSqlType : SqlType<String>(Types.VARCHAR, "varchar") {
//
//                    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: String) {
//                        ps.setString(index, parameter.lowercase())
//                    }
//
//                    override fun doGetResult(rs: ResultSet, index: Int): String? {
//                        return rs.getString(index)?.uppercase()
//                    }
//                }
//                """,
//            )
//        )
//        assertThat(result1.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
//        assertThat(result2.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
//        val baseTable = result2.getBaseTable("Users")
//        assertThat(baseTable.columns.any { it.name == "c_username" })
//    }
//
//    @Test
//    public fun `column reference`() {
//        val (result1, result2) = twiceCompile(
//            SourceFile.kotlin(
//                "source.kt",
//                """
//                import org.ktorm.ksp.api.*
//                import org.ktorm.entity.Entity
//                import org.ktorm.schema.varchar
//                import org.ktorm.schema.BaseTable
//                import kotlin.reflect.KClass
//
//                @Table
//                interface User: Entity<User> {
//                    @PrimaryKey
//                    var id: Int
//                    var username: String
//                    var age: Int
//                    @References
//                    var firstSchool: School
//                    @References("second_school_id")
//                    var secondSchool: School
//                }
//
//                @Table
//                interface School: Entity<School> {
//                    @PrimaryKey
//                    var id: Int
//                    var schoolName: String
//                }
//                """,
//            )
//        )
//        assertThat(result1.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
//        assertThat(result2.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
//        val baseTable = result2.getBaseTable("Users")
//
//        val firstSchool = baseTable.columns.firstOrNull { it.name == "firstSchoolId" }
//        assertThat(firstSchool).isNotNull
//        assertThat(firstSchool!!.referenceTable).isNotNull
//        val firstSchoolReferenceTable = firstSchool.referenceTable
//        assertThat(firstSchoolReferenceTable!!.tableName).isEqualTo("School")
//
//        val secondSchool = baseTable.columns.firstOrNull { it.name == "second_school_id" }
//        assertThat(secondSchool).isNotNull
//        assertThat(secondSchool!!.referenceTable).isNotNull
//        val secondSchoolReferenceTable = firstSchool.referenceTable
//        assertThat(secondSchoolReferenceTable!!.tableName).isEqualTo("School")
//    }
//
//    @Test
//    public fun `column reference is data class`() {
//        val (result, _) = twiceCompile(
//            SourceFile.kotlin(
//                "source.kt",
//                """
//                import org.ktorm.ksp.api.*
//                import org.ktorm.entity.Entity
//                import org.ktorm.schema.varchar
//                import org.ktorm.schema.BaseTable
//                import kotlin.reflect.KClass
//
//                @Table
//                interface User: Entity<User> {
//                    @PrimaryKey
//                    var id: Int
//                    var username: String
//                    var age: Int
//                    @References("school_id")
//                    var school: School
//                }
//
//                @Table
//                data class School(
//                    @PrimaryKey
//                    var id: Int,
//                    var schoolName: String
//                )
//                """,
//            )
//        )
//        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
//        assertThat(result.messages).contains("References column must be interface entity type")
//    }
//
//    @Test
//    public fun `column reference not an entity type`() {
//        val result = compile(
//            SourceFile.kotlin(
//                "source.kt",
//                """
//                import org.ktorm.ksp.api.*
//                import org.ktorm.entity.Entity
//                import org.ktorm.schema.varchar
//                import org.ktorm.schema.BaseTable
//                import kotlin.reflect.KClass
//
//                @Table
//                interface User: Entity<User> {
//                    @PrimaryKey
//                    var id: Int
//                    var username: String
//                    var age: Int
//                    @References("school_id")
//                    var school: School
//                }
//
//                data class School(
//                    var id: Int,
//                    var schoolName: String
//                )
//                """,
//            )
//        )
//        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
//        assertThat(result.messages).contains("is not an entity type")
//    }
//
//    @Test
//    public fun `interface entity`() {
//        val result = compile(
//            SourceFile.kotlin(
//                "source.kt",
//                """
//                import org.ktorm.entity.Entity
//                import org.ktorm.ksp.api.PrimaryKey
//                import org.ktorm.ksp.api.Table
//                @Table
//                interface User: Entity<User> {
//                    @PrimaryKey
//                    var id: Int
//                    var username: String
//                    var age: Int
//                }
//                """
//            )
//        )
//        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
//    }
//
//    @Test
//    public fun `interface entity keyword identifier`() {
//        val (result1, result2) = twiceCompile(
//            SourceFile.kotlin(
//                "source.kt",
//                """
//                import org.ktorm.entity.Entity
//                import org.ktorm.ksp.api.PrimaryKey
//                import org.ktorm.ksp.api.Table
//                @Table
//                interface User: Entity<User> {
//                    @PrimaryKey
//                    var id: Int
//                    var `class`: String
//                    var operator: String
//                }
//                """
//            )
//        )
//        assertThat(result1.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
//        assertThat(result2.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
//    }
//
//    @Test
//    public fun `column reference in data class`() {
//        val result = compile(
//            SourceFile.kotlin(
//                "source.kt",
//                """
//                import org.ktorm.ksp.api.*
//                import org.ktorm.schema.varchar
//                import org.ktorm.schema.BaseTable
//                import kotlin.reflect.KClass
//
//                @Table
//                data class User(
//                    @PrimaryKey
//                    var id: Int,
//                    var username: String,
//                    var age: Int,
//                    @References("school_id")
//                    var school: School
//                )
//
//                @Table
//                interface School: Entity<School> {
//                    @PrimaryKey
//                    var id: Int
//                    var schoolName: String
//                }
//                """,
//            )
//        )
//        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
//        assertThat(result.messages).contains("References Column are only allowed for interface entity type")
//    }
//
//    @Test
//    public fun `disable allowReflectionCreateClassEntity`() {
//        val (result1, result2) = twiceCompile(
//            SourceFile.kotlin(
//                "source.kt",
//                """
//                import org.ktorm.ksp.api.*
//
//                @Table
//                data class User(
//                    @PrimaryKey
//                    var id: Int,
//                    var username: String = "",
//                )
//
//                @KtormKspConfig(
//                    allowReflectionCreateClassEntity = false
//                )
//                class KtormConfig
//                """,
//            )
//        )
////        {
////            assertThat(it).doesNotContain("constructor.callBy(parameterMap)")
////        }
//        assertThat(result1.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
//        assertThat(result2.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
//    }
//
//    @Test
//    public fun `sqlType not a subclass of SqlType and SqlTypeFactory`() {
//        val result = compile(
//            SourceFile.kotlin(
//                "source.kt",
//                """
//                import org.ktorm.ksp.api.*
//
//                @Table
//                data class User(
//                     @PrimaryKey
//                    var id: Int?,
//                    @Column(sqlType = LocationWrapperSqlType::class)
//                    var location: LocationWrapper,
//                    var age: Int,
//                )
//
//                data class LocationWrapper(val underlying: String = "") : Serializable
//
//                object LocationWrapperSqlType
//                """,
//            )
//        )
//        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
//        assertThat(result.messages).contains(
//            "sqlType must be typed of [${SqlType::class.qualifiedName}] or " +
//                    "[${SqlTypeFactory::class.qualifiedName}]."
//        )
//    }
//
//    @Test
//    public fun `sqlType not a singleton object`() {
//        val result = compile(
//            SourceFile.kotlin(
//                "source.kt",
//                """
//                import org.ktorm.ksp.api.*
//
//                @Table
//                data class User(
//                    @PrimaryKey
//                    var id: Int?,
//                    @Column(sqlType = LocationWrapperSqlType::class)
//                    var location: LocationWrapper,
//                    var age: Int,
//                )
//
//                data class LocationWrapper(val underlying: String = "") : Serializable
//
//                class LocationWrapperSqlType : SqlType<LocationWrapper>(Types.VARCHAR, "varchar") {
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
//        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
//        assertThat(result.messages).contains("sqlType must be a Kotlin singleton object")
//    }

}
