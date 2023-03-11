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

import org.junit.Test

class KtormKspTest : BaseTest() {

    @Test
    fun `multi primary key`() {
        runKotlin("""
            @Table(name = "province")
            data class Province(
                @PrimaryKey
                val country:String,
                @PrimaryKey
                val province:String,
                var population:Int
            )
            
            fun run() {
                database.provinces.add(Province("China", "Guangdong", 150000))
                assert(database.provinces.toList().contains(Province("China", "Guangdong", 150000)))
                
                var province = database.provinces.first { (it.country eq "China") and (it.province eq "Hebei") }
                province.population = 200000
                database.provinces.update(province)
                
                province = database.provinces.first { (it.country eq "China") and (it.province eq "Hebei") }
                assert(province.population == 200000)
            }
        """.trimIndent())
    }
}
