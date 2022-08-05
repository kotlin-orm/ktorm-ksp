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

package org.ktorm.ksp.example

import org.ktorm.entity.Entity
import org.ktorm.ksp.api.Column
import org.ktorm.ksp.api.Ignore
import org.ktorm.ksp.api.PrimaryKey
import org.ktorm.ksp.api.Table
import java.io.Serializable
import java.time.LocalDate

public enum class Gender {
    MALE,
    FEMALE
}

@Table
public interface Department : Entity<Department> {
    public companion object : Entity.Factory<Department>()

    @PrimaryKey
    public val id: Int
    public var name: String
    @Column(sqlType = LocationWrapperSqlType::class)
    public var location: LocationWrapper
    @Column(columnName = "mixedCase")
    public var mixedCase: String?
}

@Table
public interface Employee : Entity<Employee> {
    public companion object : Entity.Factory<Employee>()

    @PrimaryKey
    public var id: Int
    public var name: String
    public var job: String
    public var managerId: Int?
    public var hireDate: LocalDate
    public var salary: Long

    @Column(sqlType = UIntSqlType::class)
    public var age: UInt

    @Column(sqlType = IntEnumSqlTypeFactory::class)
    public var gender: Gender?

    @Column(isReferences = true, columnName = "department_id")
    public var department: Department

    @Ignore
    public val upperName: String
        get() = name.uppercase()
}


@Table(schema = "company")
public data class Customer(
    @PrimaryKey
    public var id: Int?,
    @PrimaryKey
    public var name: String,
    public var email: String,
    public var phoneNumber: String,
)

public data class LocationWrapper(val underlying: String = "") : Serializable


@Table
public data class Student(
    @PrimaryKey
    public var id: Int?,
    public var name: String,
    public var age: Int
)
