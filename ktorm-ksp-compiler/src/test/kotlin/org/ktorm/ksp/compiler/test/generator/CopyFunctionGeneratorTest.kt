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

import org.junit.Test
import org.ktorm.ksp.compiler.test.BaseTest

class CopyFunctionGeneratorTest : BaseTest() {

    @Test
    fun `interface entity copy function`() = runKotlin("""
        @Table
        interface Employee: Entity<Employee> {
            @PrimaryKey
            var id: Int?
            var name: String
            var job: String
            @Ignore
            var hireDate: LocalDate
        }
        
        fun run() {
            val today = LocalDate.now()
            
            val jack = Employee(name = "jack", job = "programmer", hireDate = today)
            val tom = jack.copy(name = "tom")
            
            assert(tom != jack)
            assert(tom !== jack)
            assert(tom.name == "tom")
            assert(tom.job == "programmer")
            assert(tom.hireDate == today)
            
            with(EntityExtensionsApi()) {
                assert(!tom.hasColumnValue(Employees.id.binding!!))
            }
        }
    """.trimIndent())
}
