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

package org.ktorm.ksp.compiler.test.config

import org.junit.Test
import org.ktorm.ksp.compiler.test.BaseTest

class DatabaseNamingStrategyTest : BaseTest() {

    @Test
    fun testDefaultNaming() = runKotlin("""
        @Table
        interface UserProfile: Entity<UserProfile> {
            @PrimaryKey
            var id: Int
            var publicEmail: String
            var profilePicture: Int
            @References
            var company: Company
        }
        
        @Table
        interface Company: Entity<Company> {
            @PrimaryKey
            var id: Int
            var name: String
        }
        
        fun run() {
            assert(Companies.tableName == "company")
            assert(Companies.columns.map { it.name }.toSet() == setOf("id", "name"))
            assert(UserProfiles.tableName == "user_profile")
            assert(UserProfiles.columns.map { it.name }.toSet() == setOf("id", "public_email", "profile_picture", "company_id"))
        }
    """.trimIndent())

    @Test
    fun testUpperCamelCaseNamingByAlias() = runKotlin("""
        @Table
        interface UserProfile: Entity<UserProfile> {
            @PrimaryKey
            var id: Int
            var publicEmail: String
            var profilePicture: Int
            @References
            var company: Company
        }
        
        @Table
        interface Company: Entity<Company> {
            @PrimaryKey
            var id: Int
            var name: String
        }
        
        fun run() {
            assert(Companies.tableName == "COMPANY")
            assert(Companies.columns.map { it.name }.toSet() == setOf("ID", "NAME"))
            assert(UserProfiles.tableName == "USER_PROFILE")
            assert(UserProfiles.columns.map { it.name }.toSet() == setOf("ID", "PUBLIC_EMAIL", "PROFILE_PICTURE", "COMPANY_ID"))
        }
    """.trimIndent(), "ktorm.dbNamingStrategy" to "upper-snake-case")

    @Test
    fun testUpperCamelCaseNamingByClassName() = runKotlin("""
        @Table
        interface UserProfile: Entity<UserProfile> {
            @PrimaryKey
            var id: Int
            var publicEmail: String
            var profilePicture: Int
            @References
            var company: Company
        }
        
        @Table
        interface Company: Entity<Company> {
            @PrimaryKey
            var id: Int
            var name: String
        }
        
        fun run() {
            assert(Companies.tableName == "COMPANY")
            assert(Companies.columns.map { it.name }.toSet() == setOf("ID", "NAME"))
            assert(UserProfiles.tableName == "USER_PROFILE")
            assert(UserProfiles.columns.map { it.name }.toSet() == setOf("ID", "PUBLIC_EMAIL", "PROFILE_PICTURE", "COMPANY_ID"))
        }
    """.trimIndent(), "ktorm.dbNamingStrategy" to "org.ktorm.ksp.compiler.util.UpperSnakeCaseDatabaseNamingStrategy")
}
