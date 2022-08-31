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

package org.ktorm.ksp.ext.test

import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import com.tschuchort.compiletesting.SourceFile
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ktorm.ksp.tests.BaseKspTest
import java.lang.reflect.InvocationTargetException

public class KtormKspExtTest : BaseKspTest() {

    @Test
    public fun `sequence addAll function`() {
        val (result1, result2) = twiceCompile(
            SourceFile.kotlin(
                "source.kt",
                """
                import org.ktorm.database.Database
                import org.ktorm.entity.Entity
                import org.ktorm.entity.EntitySequence
                import org.ktorm.entity.toList
                import org.ktorm.ksp.api.*
                import java.time.LocalDate
                import kotlin.collections.any
                    
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
                    fun test(database: Database) {
                        var users = database.users.toList()
                        assert(!users.any { it.username == "test1" })
                        assert(!users.any { it.username == "test2" })
                        database.users.addAll(
                            listOf(
                                User(null,"test1",100),     
                                User(null,"test2",101),
                            ) 
                        )
                        users = database.users.toList()
                        assert(users.any { it.username == "test1" })
                        assert(users.any { it.username == "test2" })
                    }
                }
                """,
            )
        )
        assertThat(result1.exitCode).isEqualTo(ExitCode.OK)
        assertThat(result2.exitCode).isEqualTo(ExitCode.OK)
        useDatabase { database ->
            result2.invokeBridge("test", database)
        }
    }

    @Test
    public fun `sequence updateAll function`() {
        val (result1, result2) = twiceCompile(
            SourceFile.kotlin(
                "source.kt",
                """
                import org.ktorm.database.Database
                import org.ktorm.entity.Entity
                import org.ktorm.entity.EntitySequence
                import org.ktorm.entity.toList
                import org.ktorm.ksp.api.*
                import java.time.LocalDate
                import kotlin.collections.any
                    
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
                    fun test(database: Database) {
                        var users = database.users.toList()
                        assert(!users.any { it.username == "test1" })
                        assert(!users.any { it.username == "test2" })
                        val updateList = listOf(users[0],users[1])
                        updateList[0].username = "test1"
                        updateList[1].username = "test2"
                        database.users.updateAll(updateList)
                        users = database.users.toList()
                        assert(users.any { it.username == "test1" })
                        assert(users.any { it.username == "test2" })
                    }
                }
                """,
            )
        )
        assertThat(result1.exitCode).isEqualTo(ExitCode.OK)
        assertThat(result2.exitCode).isEqualTo(ExitCode.OK)
        useDatabase { database ->
            result2.invokeBridge("test", database)
        }
    }

    @Test
    public fun `modified entity sequence call addAll fun`() {
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
                    fun testAddAll(database:Database) {
                        val users = database.users.filter { it.id eq 1 }
                        users.addAll(
                            listOf(
                                User(null, "test1", 10),
                                User(null, "test2", 10),
                            )
                        )
                    }
                }
                """,
            )
        )
        assertThat(result1.exitCode).isEqualTo(ExitCode.OK)
        assertThat(result2.exitCode).isEqualTo(ExitCode.OK)
        useDatabase { database ->
            try {
                result2.invokeBridge("testAddAll", database)
            } catch (e: InvocationTargetException) {
                assertThat(e.targetException.message).contains("Please call on the origin sequence returned from database.sequenceOf(table)")
                return
            }
            throw RuntimeException("fail")
        }
    }

    @Test
    public fun `modified entity sequence call updateAll fun`() {
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
                    fun testUpdateAll(database:Database) {
                        val users = database.users.filter { it.id eq 1 }
                        users.updateAll(
                            listOf(
                                User(1, "test1", 10),
                                User(2, "test2", 10),
                            )
                        )
                    }
                }
                """,
            )
        )
        assertThat(result1.exitCode).isEqualTo(ExitCode.OK)
        assertThat(result2.exitCode).isEqualTo(ExitCode.OK)
        useDatabase { database ->
            try {
                result2.invokeBridge("testUpdateAll", database)
            } catch (e: InvocationTargetException) {
                assertThat(e.targetException.message).contains("Please call on the origin sequence returned from database.sequenceOf(table)")
                return
            }
            throw RuntimeException("fail")
        }
    }


    @Test
    public fun `multi primary key sequence addAll function`() {
        val (result1, result2) = twiceCompile(
            SourceFile.kotlin(
                "source.kt",
                """
                import org.ktorm.database.Database
                import org.ktorm.entity.Entity
                import org.ktorm.entity.EntitySequence
                import org.ktorm.entity.toList
                import org.ktorm.ksp.api.*
                import java.time.LocalDate
                import kotlin.collections.any
                    
                @Table(name = "province")
                data class Province(
                    @PrimaryKey
                    val country:String,
                    @PrimaryKey
                    val province:String,
                    var population:Int
                )

                object TestBridge {
                    fun testAddAll(database:Database) {
                        database.provinces.addAll(
                            listOf(
                                Province("China", "Guangdong", 150000),
                                Province("China", "Fujian", 130000),
                            ) 
                        )
                        val provinces = database.provinces.toList()
                        assert(provinces.contains(Province("China", "Guangdong", 150000)))
                        assert(provinces.contains(Province("China", "Fujian", 130000)))
                    }
                }

                @KtormKspConfig(namingStrategy = CamelCaseToSnakeCaseNamingStrategy::class)
                class KtormConfig
                """,
            )
        )
        assertThat(result1.exitCode).isEqualTo(ExitCode.OK)
        assertThat(result2.exitCode).isEqualTo(ExitCode.OK)
        useDatabase { database ->
            result2.invokeBridge("testAddAll", database)
        }
    }

    @Test
    public fun `multi primary key sequence updateAll function`() {
        val (result1, result2) = twiceCompile(
            SourceFile.kotlin(
                "source.kt",
                """
                import org.ktorm.database.Database
                import org.ktorm.entity.Entity
                import org.ktorm.entity.EntitySequence
                import org.ktorm.entity.toList
                import org.ktorm.ksp.api.*
                import java.time.LocalDate
                import kotlin.collections.any
                    
                @Table(name = "province")
                data class Province(
                    @PrimaryKey
                    val country:String,
                    @PrimaryKey
                    val province:String,
                    var population:Int
                )

                object TestBridge {
                    fun testUpdateAll(database:Database) {
                        var provinces = database.provinces.toList()
                        assert(provinces.contains(Province("China", "Hebei", 130000)))
                        assert(provinces.contains(Province("China", "Henan", 140000)))
                        database.provinces.updateAll(
                            listOf(
                                Province("China", "Hebei", 230000),
                                Province("China", "Henan", 240000),
                            ) 
                        )
                        provinces = database.provinces.toList()
                        assert(provinces.contains(Province("China", "Hebei", 230000)))
                        assert(provinces.contains(Province("China", "Henan", 240000)))
                    }
                }

                @KtormKspConfig(namingStrategy = CamelCaseToSnakeCaseNamingStrategy::class)
                class KtormConfig
                """,
            )
        )
        assertThat(result1.exitCode).isEqualTo(ExitCode.OK)
        assertThat(result2.exitCode).isEqualTo(ExitCode.OK)
        useDatabase { database ->
            result2.invokeBridge("testUpdateAll", database)
        }
    }
}
