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

package org.ktorm.ksp.codegen.test

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ktorm.ksp.tests.BaseKspTest

public class InterfaceEntityCopyFunGeneratorTest : BaseKspTest() {

    @Test
    public fun `interface entity copy function`() {
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
                
                object TestBridge {
                    fun testCopy(database: Database) {
                        val date = LocalDate.now()
                        val jack = Employee(name = "jack", job = "programmer", hireDate = date)
                        val tom = jack.copy(name = "tom")
                        assert(jack != tom)
                        assert(tom.name == "tom")
                        assert(tom.job == "programmer")
                        assert(jack.hireDate == date)
                        with(EntityExtensionsApi()) {
                            assert(!tom.hasColumnValue(Employees.id.binding!!))
                        }
                    }
                }
                """,
            )
        )

        assertThat(result1.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        assertThat(result2.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        useDatabase { database ->
            result2.invokeBridge("testCopy", database)
        }
    }

}
