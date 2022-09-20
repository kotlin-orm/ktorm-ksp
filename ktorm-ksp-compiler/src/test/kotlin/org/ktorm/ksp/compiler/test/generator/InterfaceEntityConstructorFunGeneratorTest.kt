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
import org.ktorm.ksp.tests.BaseKspTest

public class InterfaceEntityConstructorFunGeneratorTest : BaseKspTest() {

    @Test
    public fun `interface entity constructor function`() {
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
                interface Employee: Entity<Employee> {
                    @PrimaryKey
                    var id: Int?
                    var name: String    
                    var job: String
                    var hireDate: LocalDate
                }
                
                object TestBridge {
                    fun createEmployee1(database: Database): Employee {
                        return Employee()
                    }
                    
                    fun createEmployee2(database: Database): Employee {
                        return Employee(id = null)
                    }

                    fun createEmployee3(database: Database): Employee {
                        return Employee(id = null, name = "")
                    }
                }
                """,
            )
        )

        assertThat(result1.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        assertThat(result2.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        useDatabase { database ->
            val employee1 = result2.invokeBridge("createEmployee1", database) as Any
            assertThat(employee1.toString()).isEqualTo("Employee{}")
            val employee2 = result2.invokeBridge("createEmployee2", database) as Any
            assertThat(employee2.toString()).isEqualTo("Employee{id=null}")
            val employee3 = result2.invokeBridge("createEmployee3", database) as Any
            assertThat(employee3.toString()).isEqualTo("Employee{id=null, name=}")
        }
    }

}
