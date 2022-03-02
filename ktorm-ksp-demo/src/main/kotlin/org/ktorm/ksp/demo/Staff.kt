package org.ktorm.ksp.demo

import org.ktorm.entity.Entity
import org.ktorm.ksp.api.PrimaryKey
import org.ktorm.ksp.api.Table
import java.time.LocalDate

@Table
public interface Staff : Entity<Staff> {
    @PrimaryKey
    public val id: Int
    public val name: String
    public val age: Int
    public val birthday: LocalDate

}

@Table
public data class Box(
    @PrimaryKey
    public val id: Int,
    public val name: String
)

@Table
public data class Employee(
    @PrimaryKey
    public var id: Int?,
    public val name: String,
    public val age: Int?,
    public val birthday: LocalDate = LocalDate.now(),
    public val gender: Gender,
)


@Table
public data class Department(
    @PrimaryKey
    public val id: Int,
    public val name: String
)

@Table
public data class Student(
    @PrimaryKey
    public var id: Int?,
    public var name: String,
    public var age: Int
)

@Table
public interface School : Entity<School> {
    @PrimaryKey
    public var id: Int
    public var name: String
}

public enum class Gender {
    MALE,
    FEMALE
}