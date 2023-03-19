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

class PseudoConstructorFunctionGeneratorTest : BaseTest() {

    @Test
    fun `interface entity constructor function`() = runKotlin("""
        @Table
        interface Employee: Entity<Employee> {
            @PrimaryKey
            var id: Int?
            var name: String
            var job: String
            var hireDate: LocalDate
        }
        
        fun run() {
            assert(Employee().toString() == "Employee()")
            assert(Employee(id = null).toString() == "Employee(id=null)")
            assert(Employee(id = null, name = "").toString() == "Employee(id=null, name=)")
        }
    """.trimIndent())
}
