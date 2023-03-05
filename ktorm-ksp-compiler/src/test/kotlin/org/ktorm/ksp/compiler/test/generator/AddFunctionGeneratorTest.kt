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

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ktorm.entity.EntitySequence
import org.ktorm.entity.toList
import org.ktorm.ksp.compiler.test.BaseKspTest
import java.lang.reflect.InvocationTargetException

public class AddFunctionGeneratorTest : BaseKspTest() {

    @Test
    public fun `sequence add function`() {
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
                    var id: Int?,
                    var username: String,
                    var age: Int,
                )

                @KtormKspConfig(namingStrategy = CamelCaseToSnakeCaseNamingStrategy::class)
                class KtormConfig

                object TestBridge {
                    fun getUsers(database:Database): EntitySequence<User,Users> {
                        return database.users
                    }
                    fun addUser(database: Database) {
                        database.users.add(User(null,"test",100))
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
            result2.invokeBridge("addUser", database)
            users = result2.invokeBridge("getUsers", database) as EntitySequence<*, *>
            assertThat(users.toList().toString())
                .isEqualTo("[User(id=1, username=jack, age=20), User(id=2, username=lucy, age=22), User(id=3, username=mike, age=22), User(id=4, username=test, age=100)]")
        }
    }

    @Test
    public fun `modified entity sequence call add fun`() {
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
                        users.add(User(null, "lucy", 10))
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
