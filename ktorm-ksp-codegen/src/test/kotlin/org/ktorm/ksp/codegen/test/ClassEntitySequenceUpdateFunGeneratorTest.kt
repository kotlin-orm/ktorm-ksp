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

package org.ktorm.ksp.codegen.test

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ktorm.entity.EntitySequence
import org.ktorm.entity.toList
import org.ktorm.ksp.tests.BaseKspTest
import java.lang.reflect.InvocationTargetException

public class ClassEntitySequenceUpdateFunGeneratorTest : BaseKspTest() {

    @Test
    public fun `sequence update function`() {
        val (result1, result2) = twiceCompile(
            SourceFile.kotlin(
                "source.kt",
                """
                import org.ktorm.database.Database
                import org.ktorm.entity.Entity
                import org.ktorm.entity.EntitySequence
                import org.ktorm.entity.toList
                import org.ktorm.ksp.api.*
                import org.ktorm.dsl.eq
                import org.ktorm.entity.filter
                import java.time.LocalDate
                    
                @Table
                data class User(
                    @PrimaryKey
                    var id: Int?,
                    var username: String,
                    var age: Int,
                )

                @KtormKspConfig(namingStrategy = CamelCaseToSnakeCaseNamingStrategy::class)
                class KtormConfig

                object TestBridge {
                    @Suppress("MemberVisibilityCanBePrivate")
                    fun getUsers(database:Database): EntitySequence<User,Users> {
                        return database.users
                    }
                    fun updateUser(database: Database) {
                        val user = getUsers(database).filter { it.id eq 1  }.toList().first()
                        user.username = "tom"
                        getUsers(database).update(user)
                    }
                }
                """,
            )
        )
        assertThat(result1.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        assertThat(result2.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        useDatabase { database ->
            var users = result2.invokeBridge("getUsers", database) as EntitySequence<*, *>
            assertThat(users.sourceTable.tableName).isEqualTo("user")
            assertThat(users.toList().toString())
                .isEqualTo("[User(id=1, username=jack, age=20), User(id=2, username=lucy, age=22), User(id=3, username=mike, age=22)]")

            result2.invokeBridge("updateUser", database)
            users = result2.invokeBridge("getUsers", database) as EntitySequence<*, *>
            assertThat(users.toList().toString())
                .isEqualTo("[User(id=1, username=tom, age=20), User(id=2, username=lucy, age=22), User(id=3, username=mike, age=22)]")
        }
    }

    @Test
    public fun `modified entity sequence call update fun`() {
        val (result1, result2) = twiceCompile(
            SourceFile.kotlin(
                "source.kt",
                """
                import org.ktorm.ksp.api.*
                import org.ktorm.database.Database
                import org.ktorm.entity.toList
                import kotlin.collections.List
                import org.ktorm.dsl.eq
                import org.ktorm.entity.filter
                
                @Table
                data class User(
                    @PrimaryKey
                    var id: Int?,
                    var username: String,
                    var age: Int
                )

                object TestBridge {
                    fun testAdd(database:Database) {
                        val users = database.users.filter { it.id eq 1 }
                        users.update(User(1, "lucy", 10))
                    }
                }
                """,
            )
        )
        assertThat(result1.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        assertThat(result2.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        useDatabase { database ->
            try {
                result2.invokeBridge("testAdd", database)
            } catch (e: InvocationTargetException) {
                assertThat(e.targetException.message)
                    .contains("Please call on the origin sequence returned from database.sequenceOf(table)")
                return
            }
            throw RuntimeException("fail")
        }
    }

}
