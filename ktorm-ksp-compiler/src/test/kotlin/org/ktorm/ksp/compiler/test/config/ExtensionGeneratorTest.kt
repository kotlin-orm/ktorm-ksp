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

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ktorm.ksp.compiler.test.BaseTest

public class ExtensionGeneratorTest : BaseTest() {

    @Test
    public fun `enable sequenceOf for extensionGenerator`() {
        val (result1, result2) = twiceCompile(
            SourceFile.kotlin(
                "source.kt",
                """
                import org.ktorm.ksp.api.*
                    
                @Table
                data class User(
                    @PrimaryKey
                    var id: Int?,
                    var username: String,
                    var age: Int,
                )
                
                @KtormKspConfig(
                    extension = ExtensionGenerator(enableSequenceOf = true)
                )
                class KtormConfig
                """,
            )
        ) {
            val sequenceLine = it.lineSequence()
                .filter { str -> str.contains("public val Database.users: EntitySequence<User, Users>") }.toList()
            assertThat(sequenceLine.size).isEqualTo(1)
        }
        assertThat(result1.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        assertThat(result2.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    public fun `enable sequence add function for extensionGenerator`() {
        val (result1, result2) = twiceCompile(
            SourceFile.kotlin(
                "source.kt",
                """
                import org.ktorm.ksp.api.*
                    
                @Table
                data class User(
                    @PrimaryKey
                    var id: Int?,
                    var username: String,
                    var age: Int,
                )
                
                @KtormKspConfig(
                    extension = ExtensionGenerator(enableClassEntitySequenceAddFun = true)
                )
                class KtormConfig
                """,
            )
        ) {
            val sequenceLine =
                it.lineSequence().filter { str -> str.contains("public fun EntitySequence<User, Users>.add") }.toList()
            assertThat(sequenceLine.size).isEqualTo(1)
        }
        assertThat(result1.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        assertThat(result2.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    public fun `enable sequence update function for extensionGenerator`() {
        val (result1, result2) = twiceCompile(
            SourceFile.kotlin(
                "source.kt",
                """
                import org.ktorm.ksp.api.*
                    
                @Table
                data class User(
                    @PrimaryKey
                    var id: Int?,
                    var username: String,
                    var age: Int,
                )
                
                @KtormKspConfig(
                    extension = ExtensionGenerator(enableClassEntitySequenceUpdateFun = true)
                )
                class KtormConfig
                """,
            )
        ) {
            val sequenceLine =
                it.lineSequence().filter { str -> str.contains("public fun EntitySequence<User, Users>.update") }
                    .toList()
            assertThat(sequenceLine.size).isEqualTo(1)
        }
        assertThat(result1.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        assertThat(result2.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    public fun `enable interface entity simulation data class for extensionGenerator`() {
        val (result1, result2) = twiceCompile(
            SourceFile.kotlin(
                "source.kt",
                """
                import org.ktorm.database.Database
                import org.ktorm.entity.Entity
                import org.ktorm.entity.EntityExtensionsApi
                import org.ktorm.entity.EntitySequence
                import org.ktorm.ksp.api.*
                import java.time.LocalDate

                @Table
                interface Employee: Entity<Employee> {
                    @PrimaryKey
                    var id: Int?
                    var name: String    
                    var job: String
                    var hireDate: LocalDate
                }
                
                @KtormKspConfig(
                    extension = ExtensionGenerator(enableInterfaceEntitySimulationDataClass = true)
                )
                class KtormConfig
                """,
            )
        ) {
            var hasConstructor = false
            var hasComponent = false
            var hasCopy = false
            it.lineSequence()
                .forEach { line ->
                    if (line.contains("public fun Employee")) {
                        hasConstructor = true
                    }
                    if (line.contains("public operator fun Employee.component1")) {
                        hasComponent = true
                    }
                    if (line.contains("public fun Employee.copy")) {
                        hasCopy = true
                    }
                }
            assertThat(hasConstructor).isEqualTo(true)
            assertThat(hasComponent).isEqualTo(true)
            assertThat(hasCopy).isEqualTo(true)
        }
        assertThat(result1.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        assertThat(result2.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    public fun `disable sequenceOf for extensionGenerator`() {
        val (result1, result2) = twiceCompile(
            SourceFile.kotlin(
                "source.kt",
                """
                import org.ktorm.ksp.api.*
                    
                @Table
                data class User(
                    @PrimaryKey
                    var id: Int?,
                    var username: String,
                    var age: Int,
                )
                
                @KtormKspConfig(
                    extension = ExtensionGenerator(enableSequenceOf = false)
                )
                class KtormConfig
                """,
            )
        ) {
            val sequenceLine = it.lineSequence()
                .filter { str -> str.contains("public val Database.users: EntitySequence<User, Users>") }.toList()
            assertThat(sequenceLine.size).isEqualTo(0)
        }
        assertThat(result1.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        assertThat(result2.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    public fun `disable sequence add function for extensionGenerator`() {
        val (result1, result2) = twiceCompile(
            SourceFile.kotlin(
                "source.kt",
                """
                import org.ktorm.ksp.api.*
                    
                @Table
                data class User(
                    @PrimaryKey
                    var id: Int?,
                    var username: String,
                    var age: Int,
                )
                
                @KtormKspConfig(
                    extension = ExtensionGenerator(enableClassEntitySequenceAddFun = false)
                )
                class KtormConfig
                """,
            )
        ) {
            val sequenceLine =
                it.lineSequence().filter { str -> str.contains("public fun EntitySequence<User, Users>.add") }.toList()
            assertThat(sequenceLine.size).isEqualTo(0)
        }
        assertThat(result1.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        assertThat(result2.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    public fun `disable sequence update function for extensionGenerator`() {
        val (result1, result2) = twiceCompile(
            SourceFile.kotlin(
                "source.kt",
                """
                import org.ktorm.ksp.api.*
                    
                @Table
                data class User(
                    @PrimaryKey
                    var id: Int?,
                    var username: String,
                    var age: Int,
                )
                
                @KtormKspConfig(
                    extension = ExtensionGenerator(enableClassEntitySequenceUpdateFun = false)
                )
                class KtormConfig
                """,
            )
        ) {
            val sequenceLine =
                it.lineSequence().filter { str -> str.contains("public fun EntitySequence<User, Users>.update") }
                    .toList()
            assertThat(sequenceLine.size).isEqualTo(0)
        }
        assertThat(result1.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        assertThat(result2.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    public fun `disable interface entity simulation data class for extensionGenerator`() {
        val (result1, result2) = twiceCompile(
            SourceFile.kotlin(
                "source.kt",
                """
                import org.ktorm.database.Database
                import org.ktorm.entity.Entity
                import org.ktorm.entity.EntityExtensionsApi
                import org.ktorm.entity.EntitySequence
                import org.ktorm.ksp.api.*
                import java.time.LocalDate

                @Table
                interface Employee: Entity<Employee> {
                    @PrimaryKey
                    var id: Int?
                    var name: String    
                    var job: String
                    var hireDate: LocalDate
                }
                
                @KtormKspConfig(
                    extension = ExtensionGenerator(enableInterfaceEntitySimulationDataClass = false)
                )
                class KtormConfig
                """,
            )
        ) {
            var hasConstructor = false
            var hasComponent = false
            var hasCopy = false
            it.lineSequence()
                .forEach { line ->
                    if (line.contains("public fun Employee")) {
                        hasConstructor = true
                    }
                    if (line.contains("public operator fun Employee.component1")) {
                        hasComponent = true
                    }
                    if (line.contains("public fun Employee.copy")) {
                        hasCopy = true
                    }
                }
            assertThat(hasConstructor).isEqualTo(false)
            assertThat(hasComponent).isEqualTo(false)
            assertThat(hasCopy).isEqualTo(false)
        }
        assertThat(result1.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        assertThat(result2.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

}