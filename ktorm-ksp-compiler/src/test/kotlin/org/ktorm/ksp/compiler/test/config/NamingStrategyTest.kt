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

package org.ktorm.ksp.compiler.test.config

import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import com.tschuchort.compiletesting.SourceFile
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ktorm.ksp.compiler.test.BaseTest

public class NamingStrategyTest : BaseTest() {

    @Test
    public fun lowerCaseCamelCaseToUnderscoresNamingStrategy() {
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
                data class UserProfile(
                    @PrimaryKey
                    var id: Int,
                    var publicEmail: String,
                    var profilePicture: Int
                )
               
                @KtormKspConfig(
                    namingStrategy = CamelCaseToSnakeCaseNamingStrategy::class
                )
                class KtormConfig
                
                """,
            )
        )
        assertThat(result1.exitCode).isEqualTo(ExitCode.OK)
        assertThat(result2.exitCode).isEqualTo(ExitCode.OK)
        val baseTable = result2.getBaseTable("UserProfiles")
        assertThat(baseTable.tableName).isEqualTo("user_profile")
        assertThat(baseTable.columns.map { it.name }.toSet()).isEqualTo(setOf("id", "public_email", "profile_picture"))
    }

    @Test
    public fun `custom singleton NamingStrategy`() {
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
                data class UserProfile(
                    @PrimaryKey
                    var id: Int,
                    var publicEmail: String,
                    var profilePicture: Int
                )
               
                @KtormKspConfig(
                    namingStrategy = CustomNamingStrategy::class
                )
                class KtormConfig

                object CustomNamingStrategy: NamingStrategy {
                    override fun toTableName(entityClassName: String): String {
                        return "t_${'$'}entityClassName"
                    }
                    override fun toColumnName(propertyName: String): String {
                        return "c_${'$'}propertyName"
                    }
                }
                
                """,
            )
        )
        assertThat(result1.exitCode).isEqualTo(ExitCode.OK)
        assertThat(result2.exitCode).isEqualTo(ExitCode.OK)
        val baseTable = result2.getBaseTable("UserProfiles")
        assertThat(baseTable.tableName).isEqualTo("t_UserProfile")
        assertThat(baseTable.columns.map { it.name }.toSet()).isEqualTo(
            setOf(
                "c_id",
                "c_publicEmail",
                "c_profilePicture"
            )
        )
    }


    @Test
    public fun `custom non singleton NamingStrategy`() {
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
                data class UserProfile(
                    @PrimaryKey
                    var id: Int,
                    var publicEmail: String,
                    var profilePicture: Int
                )
               
                @KtormKspConfig(
                    namingStrategy = CustomNamingStrategy::class
                )
                class KtormConfig

                class CustomNamingStrategy: NamingStrategy {
                    override fun toTableName(entityClassName: String): String {
                        return "t_${'$'}entityClassName"
                    }
                    override fun toColumnName(propertyName: String): String {
                        return "c_${'$'}propertyName"
                    }
                }
                
                """,
            )
        )
        assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains("namingStrategy must be singleton")
    }

}