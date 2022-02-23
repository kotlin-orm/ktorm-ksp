package org.ktorm.ksp.demo

import Id
import org.ktorm.entity.Entity
import org.ktorm.ksp.annotation.Table
import java.time.LocalDate

@Table
public interface Staff : Entity<Staff> {
    @Id
    public val id: Int
    public val name: String
    public val age: Int
    public val birthday: LocalDate
}

@Table
public data class Employee(
    @Id
    public val id: Int,
    public val name: String,
    public val age: Int,
    public val birthday: LocalDate = LocalDate.now()
) {
    @Transient
    public var createTime: LocalDate = LocalDate.now()
}

@Table
public class Job {
    @Id
    public var id: Int? = null
    public var name: String? = null
    @Transient
    public var createTime: LocalDate = LocalDate.now()

}
