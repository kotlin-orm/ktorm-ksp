package org.ktorm.ksp.example

import org.ktorm.database.Database
import org.ktorm.entity.toList
import org.ktorm.ksp.example.common.DatabaseFactory

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





