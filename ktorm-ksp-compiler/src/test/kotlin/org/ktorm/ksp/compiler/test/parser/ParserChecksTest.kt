package org.ktorm.ksp.compiler.test.parser

import org.junit.Test
import org.ktorm.ksp.compiler.test.BaseTest

/**
 * Created by vince at Apr 16, 2023.
 */
class ParserChecksTest : BaseTest() {

    @Test
    fun testEnumClass() = kspFailing("Gender is expected to be a class or interface but actually ENUM_CLASS", """
        @Table
        enum class Gender { MALE, FEMALE }
    """.trimIndent())

    @Test
    fun testInterfaceNotExtendingEntity() = kspFailing("User must extends from org.ktorm.entity.Entity", """
        @Table
        interface User { 
            val id: Int
            val name: String
        }
    """.trimIndent())
}