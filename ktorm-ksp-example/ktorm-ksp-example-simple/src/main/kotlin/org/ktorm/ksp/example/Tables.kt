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
    @Column(converter = IntEnumConverter::class)
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
