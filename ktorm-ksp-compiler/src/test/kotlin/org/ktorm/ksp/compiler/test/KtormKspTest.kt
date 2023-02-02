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

package org.ktorm.ksp.compiler.test

import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import com.tschuchort.compiletesting.SourceFile
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

public class KtormKspTest : BaseKspTest() {

    @Test
    public fun `multi primary key`() {
        val (result1, result2) = twiceCompile(
            SourceFile.kotlin(
                "source.kt",
                """
                import org.ktorm.ksp.api.*
                import org.ktorm.dsl.eq
                import org.ktorm.dsl.and
                import org.ktorm.entity.first
                import org.ktorm.database.Database
                import org.ktorm.entity.toList
                import kotlin.collections.List
                    
                @Table(name = "province")
                data class Province(
                    @PrimaryKey
                    val country:String,
                    @PrimaryKey
                    val province:String,
                    var population:Int
                )

                object TestBridge {
                    @Suppress("MemberVisibilityCanBePrivate")
                    fun testAdd(database:Database) {
                        database.provinces.add(Province("China", "Guangdong", 150000))
                        val provinces = database.provinces.toList()
                        assert(provinces.contains(Province("China", "Guangdong", 150000)))
                    }
                    fun testUpdate(database: Database) {
                        var province = database.provinces.first { (it.country eq "China") and (it.province eq "Hebei") }
                        province.population = 200000
                        database.provinces.update(province)
                        province = database.provinces.first { (it.country eq "China") and (it.province eq "Hebei") }
                        assert(province.population == 200000)
                    }
                }
                """,
            )
        )
        assertThat(result1.exitCode).isEqualTo(ExitCode.OK)
        assertThat(result2.exitCode).isEqualTo(ExitCode.OK)
        useDatabase { database ->
            result2.invokeBridge("testAdd", database)
            result2.invokeBridge("testUpdate", database)
        }
    }

}
