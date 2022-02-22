package org.ktorm.ksp.demo

import org.ktorm.entity.Entity
import org.ktorm.ksp.annotation.KtormTable
import java.time.LocalDate

@KtormTable
public interface Staff : Entity<Staff> {
    public val id: Int
    public val name: String
    public val age: Int
    public val birthday: LocalDate
}