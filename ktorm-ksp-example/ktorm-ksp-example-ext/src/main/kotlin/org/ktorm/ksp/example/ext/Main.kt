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
