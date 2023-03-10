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

package org.ktorm.ksp.example

import org.ktorm.entity.Entity
import org.ktorm.ksp.api.*
import java.io.Serializable
import java.time.LocalDate

enum class Gender {
    MALE,
    FEMALE
}

@Table
interface Department : Entity<Department> {
    companion object : Entity.Factory<Department>()

    @PrimaryKey
    val id: Int
    var name: String

    @Column(sqlType = LocationWrapperSqlType::class)
    var location: LocationWrapper

    @Column(name = "mixedCase")
    var mixedCase: String?
}

/**
 * This is the kdoc for Employee.
 *
 * This is the second line.
 */
@Table
interface Employee : Entity<Employee> {
    companion object : Entity.Factory<Employee>()

    /**
     * This is the kdoc for id.
     *
     * This is the second line.
     */
    @PrimaryKey
    var id: Int
    var name: String
    var job: String
    var managerId: Int?
    var hireDate: LocalDate
    var salary: Long

    @Column(sqlType = UIntSqlType::class)
    var age: UInt

    @Column(sqlType = IntEnumSqlTypeFactory::class)
    var gender: Gender?

    @References
    var department: Department

    @Ignore
    val upperName: String
        get() = name.uppercase()
}

@Table(schema = "company")
data class Customer(
    @PrimaryKey
    var id: Int?,
    @PrimaryKey
    var name: String,
    var email: String?,
    var phoneNumber: String,
)

data class LocationWrapper(val underlying: String = "") : Serializable

@Table
data class Student(
    @PrimaryKey
    var id: Int?,
    var name: String?,
    var age: Int
)
