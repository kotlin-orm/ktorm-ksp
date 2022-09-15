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

package org.ktorm.ksp.example

import org.ktorm.database.Database
import org.ktorm.entity.toList

public val database: Database = DatabaseFactory.database

public fun main() {
    query()
    add()
    update()
}

public fun query() {
    for (employee in database.employees) {
        println(employee)
    }
    for (customer in database.customers) {
        println(customer)
    }
}

public fun add() {
    val customer = Customer(
        null,
        "mike",
        "mike@email.com",
        "12300000000"
    )
    database.customers.add(customer)
    println("add customer generated key: ${customer.id}")
}

public fun update() {
    val customer = database.customers.toList().first()
    customer.name = "jack"
    database.customers.update(customer)
}





