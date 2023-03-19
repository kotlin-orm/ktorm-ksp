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

class UpdateFunctionGeneratorTest : BaseTest() {

    @Test
    fun `sequence update function`() = runKotlin("""
        @Table
        data class User(
            @PrimaryKey
            var id: Int?,
            var username: String,
            var age: Int,
        )
        
        fun run() {
            val user = database.users.first { it.id eq 1 }
            assert(user.username == "jack")
            
            user.username = "tom"
            database.users.update(user)
            
            val user0 = database.users.first { it.id eq 1 }
            assert(user0.username == "tom")
        }
    """.trimIndent())

    @Test
    fun `modified entity sequence call update fun`() = runKotlin("""
        @Table
        data class User(
            @PrimaryKey
            var id: Int?,
            var username: String,
            var age: Int
        )
        
        fun run() {
            try {
                val users = database.users.filter { it.id eq 1 }
                users.update(User(1, "lucy", 10))
                throw AssertionError("fail")
            } catch (_: UnsupportedOperationException) {
            }
        }
    """.trimIndent())
}
