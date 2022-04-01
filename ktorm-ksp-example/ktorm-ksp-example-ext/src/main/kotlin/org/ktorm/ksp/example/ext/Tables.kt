package org.ktorm.ksp.example.ext

import org.ktorm.ksp.api.PrimaryKey
import org.ktorm.ksp.api.Table

@Table(schema = "company", tableName = "t_customer")
public data class Customer(
    @PrimaryKey
    public var id: Int?,
    public var name: String,
    public var email: String,
    public var phoneNumber: String,
)
