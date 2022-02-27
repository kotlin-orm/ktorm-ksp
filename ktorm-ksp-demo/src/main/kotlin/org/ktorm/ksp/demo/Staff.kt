package org.ktorm.ksp.demo

import org.ktorm.entity.Entity
import org.ktorm.ksp.api.Column
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
    public val id: Int,
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
public interface Student : Entity<Student> {
    @PrimaryKey
    public var id: Int
    public var name: String

    @Column(isReferences = true, columnName = "schoolId")
    public var school: School
}

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