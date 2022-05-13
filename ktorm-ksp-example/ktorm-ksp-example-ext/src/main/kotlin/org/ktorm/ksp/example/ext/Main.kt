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

package org.ktorm.ksp.example.ext

import org.ktorm.entity.toList
import org.ktorm.ksp.api.CamelCaseToLowerCaseUnderscoresNamingStrategy
import org.ktorm.ksp.api.KtormKspConfig
import org.ktorm.ksp.example.common.DatabaseFactory

@KtormKspConfig(
    namingStrategy = CamelCaseToLowerCaseUnderscoresNamingStrategy::class
)
public class Config

public fun main() {
    val database = DatabaseFactory.database
    database.customers.addAll(
        listOf(
            Customer(null, "customer1", "customer1@ktorm.org", "13800000001"),
            Customer(null, "customer2", "customer2@ktorm.org", "13800000002"),
        )
    )
    val customers = database.customers.toList()
    println(customers)
}
