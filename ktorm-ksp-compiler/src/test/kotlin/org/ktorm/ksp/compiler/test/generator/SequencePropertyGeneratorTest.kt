/*
 * Copyright 2022 the original author or authors.
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

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ktorm.entity.EntitySequence
import org.ktorm.entity.toList
import org.ktorm.ksp.tests.BaseKspTest

public class SequencePropertyGeneratorTest : BaseKspTest() {

    @Test
    public fun `sequenceOf function`() {
        val (result1, result2) = twiceCompile(
            SourceFile.kotlin(
                "source.kt",
                """
                import org.ktorm.database.Database
                import org.ktorm.entity.Entity
                import org.ktorm.entity.EntitySequence
                import org.ktorm.ksp.api.*
                import java.time.LocalDate

                @Table
                data class User(
                    @PrimaryKey
                    var id: Int,
                    var username: String,
                    var age: Int
                )

                @Table
                interface Employee: Entity<Employee> {
                    @PrimaryKey
                    var id: Int
                    var name: String    
                    var job: String
                    var hireDate: LocalDate
                }
                
                @KtormKspConfig(namingStrategy = CamelCaseToSnakeCaseNamingStrategy::class)
                class KtormConfig

                object TestBridge {
                    fun getUsers(database:Database): EntitySequence<User,Users> {
                        return database.users
                    }
                    fun getEmployees(database: Database): EntitySequence<Employee,Employees> {
                        return database.employees
                    }
                }
                """,
            )
        )
        assertThat(result1.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        assertThat(result2.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        useDatabase { database ->
            val users = result2.invokeBridge("getUsers", database) as EntitySequence<*, *>
            assertThat(users.sourceTable.tableName).isEqualTo("user")
            val toList = users.toList()
            assertThat(toList.toString())
                .isEqualTo("[User(id=1, username=jack, age=20), User(id=2, username=lucy, age=22), User(id=3, username=mike, age=22)]")
            val employees = result2.invokeBridge("getEmployees", database) as EntitySequence<*, *>
            assertThat(employees.sourceTable.tableName).isEqualTo("employee")
            assertThat(employees.toList().toString())
                .isEqualTo("[Employee{id=1, name=vince, job=engineer, hireDate=2018-01-01}, Employee{id=2, name=marry, job=trainee, hireDate=2019-01-01}, Employee{id=3, name=tom, job=director, hireDate=2018-01-01}, Employee{id=4, name=penny, job=assistant, hireDate=2019-01-01}]")
        }
    }

    @Test
    public fun `custom sequence name`() {
        val (result1, result2) = twiceCompile(
            SourceFile.kotlin(
                "source.kt",
                """
                import org.ktorm.database.Database
                import org.ktorm.entity.Entity
                import org.ktorm.entity.EntitySequence
                import org.ktorm.ksp.api.*
                import java.time.LocalDate

                @Table(entitySequenceName = "aUsers")
                data class User(
                    @PrimaryKey
                    var id: Int,
                    var username: String,
                    var age: Int
                )

                @KtormKspConfig(namingStrategy = CamelCaseToSnakeCaseNamingStrategy::class)
                class KtormConfig

                object TestBridge {
                    fun getUsers(database:Database): EntitySequence<User,Users> {
                        return database.aUsers
                    }
                }
                """,
            )
        )
        assertThat(result1.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        assertThat(result2.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        useDatabase { database ->
            val users = result2.invokeBridge("getUsers", database) as EntitySequence<*, *>
            assertThat(users.sourceTable.tableName).isEqualTo("user")
            val toList = users.toList()
            assertThat(toList.toString())
                .isEqualTo("[User(id=1, username=jack, age=20), User(id=2, username=lucy, age=22), User(id=3, username=mike, age=22)]")
        }
    }

}
