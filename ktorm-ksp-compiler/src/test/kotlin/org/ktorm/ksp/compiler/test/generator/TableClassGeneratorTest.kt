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

    @Test
    fun `column has no backingField`() = runKotlin("""
        @Table
        data class User(
            @PrimaryKey
            var id: Int,
            var age: Int
        ) {
            val username: String get() = "username"
        }
        
        fun run() {
            assert(Users.tableName == "user")
            assert(Users.columns.map { it.name }.toSet() == setOf("id", "age"))
        }
    """.trimIndent())

    @Test
    fun `column reference`() = runKotlin("""
        @Table
        interface User: Entity<User> {
            @PrimaryKey
            var id: Int
            var username: String
            var age: Int
            @References
            var firstSchool: School
            @References("second_school_identity")
            var secondSchool: School
        }
        
        @Table
        interface School: Entity<School> {
            @PrimaryKey
            var id: Int
            var schoolName: String
        }
        
        fun run() {
            assert(Users.firstSchoolId.referenceTable is Schools)
            assert(Users.firstSchoolId.name == "first_school_id")
            assert(Users.secondSchoolId.referenceTable is Schools)
            assert(Users.secondSchoolId.name == "second_school_identity")
        }
    """.trimIndent())

    @Test
    fun `interface entity`() = runKotlin("""
        @Table
        interface User: Entity<User> {
            @PrimaryKey
            var id: Int
            var username: String
            var age: Int
        }
        
        fun run() {
            assert(Users.tableName == "user")
            assert(Users.columns.map { it.name }.toSet() == setOf("id", "username", "age"))
        }
    """.trimIndent())

    @Test
    fun `interface entity keyword identifier`() = runKotlin("""
        @Table
        interface User: Entity<User> {
            @PrimaryKey
            var id: Int
            var `class`: String
            var operator: String
        }
        
        fun run() {
            assert(Users.tableName == "user")
            assert(Users.columns.map { it.name }.toSet() == setOf("id", "class", "operator"))
        }
    """.trimIndent())
}
