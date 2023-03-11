///*
// * Copyright 2022-2023 the original author or authors.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package org.ktorm.ksp.compiler.test.generator
//
//import com.tschuchort.compiletesting.KotlinCompilation
//import com.tschuchort.compiletesting.SourceFile
//import org.assertj.core.api.Assertions.assertThat
//import org.junit.Test
//
//public class InterfaceEntityComponentFunGeneratorTest : BaseKspTest() {
//
//    @Test
//    public fun `interface entity component function`() {
//        val (result1, result2) = twiceCompile(
//            SourceFile.kotlin(
//                "source.kt",
//                """
//                import org.ktorm.database.Database
//                import org.ktorm.entity.Entity
//                import org.ktorm.entity.EntitySequence
//                import org.ktorm.ksp.api.*
//                import java.time.LocalDate
//
//                @Table
//                interface Employee: Entity<Employee> {
//                    @PrimaryKey
//                    var id: Int?
//                    var name: String
//                    var job: String
//                    var hireDate: LocalDate
//                }
//
//                object TestBridge {
//                    fun testComponentN(database: Database) {
//                        val date = LocalDate.now()
//                        val employee = Entity.create<Employee>()
//                        employee.id = 1
//                        employee.name = "name"
//                        employee.job = "job"
//                        employee.hireDate = date
//                        val (id, name, job, hireDate) = employee
//                        assert(id == 1)
//                        assert(name == "name")
//                        assert(job == "job")
//                        assert(date == date)
//                    }
//                }
//                """,
//            )
//        )
//
//        assertThat(result1.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
//        assertThat(result2.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
//        useDatabase { database ->
//            result2.invokeBridge("testComponentN", database)
//        }
//    }
//
//}
