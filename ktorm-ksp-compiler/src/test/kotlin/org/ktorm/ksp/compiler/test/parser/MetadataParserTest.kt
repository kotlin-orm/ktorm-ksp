package org.ktorm.ksp.compiler.test.parser

import org.junit.Test
import org.ktorm.ksp.compiler.test.BaseTest

/**
 * Created by vince at Apr 16, 2023.
 */
class MetadataParserTest : BaseTest() {

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

    @Test
    fun testClassIgnoreProperties() = runKotlin("""
        @Table(ignoreProperties = ["name"])
        class User(
            val id: Int, 
            val name: String? = null
        )
        
        fun run() {
            assert(Users.columns.map { it.name }.toSet() == setOf("id"))
        }
    """.trimIndent())

    @Test
    fun testInterfaceIgnoreProperties() = runKotlin("""
        @Table(ignoreProperties = ["name"])
        interface User : Entity<User> {
            val id: Int
            val name: String
        }
        
        fun run() {
            assert(Users.columns.map { it.name }.toSet() == setOf("id"))
        }
    """.trimIndent())

    @Test
    fun testClassIgnoreAnnotation() = runKotlin("""
        @Table
        class User(
            val id: Int, 
            @Ignore
            val name: String? = null
        )
        
        fun run() {
            assert(Users.columns.map { it.name }.toSet() == setOf("id"))
        }
    """.trimIndent())

    @Test
    fun testInterfaceIgnoreAnnotation() = runKotlin("""
        @Table
        interface User : Entity<User> {
            val id: Int
            @Ignore
            val name: String
        }
        
        fun run() {
            assert(Users.columns.map { it.name }.toSet() == setOf("id"))
        }
    """.trimIndent())

    @Test
    fun testClassPropertiesWithoutBackingField() = runKotlin("""
        @Table
        class User(val id: Int) {
            val name: String get() = "vince"
        }
        
        fun run() {
            assert(Users.columns.map { it.name }.toSet() == setOf("id"))
        }
    """.trimIndent())

    @Test
    fun testClassPropertiesOrder() = runKotlin("""
        @Table
        class User(var id: Int) {
            var name: String? = null
        }
        
        fun run() {
            assert(Users.columns.map { it.name }[0] == "id")
            assert(Users.columns.map { it.name }[1] == "name")
        }
    """.trimIndent())

    @Test
    fun testInterfaceNonAbstractProperties() = runKotlin("""
        @Table
        interface User : Entity<User> {
            val id: Int
            val name: String get() = "vince"
        }
        
        fun run() {
            assert(Users.columns.map { it.name }.toSet() == setOf("id"))
        }
    """.trimIndent())

    @Test
    fun testSqlTypeInferError() = kspFailing("Parse sqlType error for property User.name: cannot infer sqlType, please specify manually.", """
        @Table
        interface User : Entity<User> {
            val name: java.lang.StringBuilder
        }
    """.trimIndent())

    @Test
    fun testSqlTypeShouldBeObject() = kspFailing("Parse sqlType error for property User.name: the sqlType class must be a Kotlin singleton object.", """
        @Table
        interface User : Entity<User> {
            val id: Int
            @Column(sqlType = org.ktorm.schema.EnumSqlType::class)
            val name: String
        }
    """.trimIndent())

    @Test
    fun testSqlTypeShouldBeSqlType() = kspFailing("Parse sqlType error for property User.name: the sqlType class must be subtype of SqlType/SqlTypeFactory.", """
        @Table
        interface User : Entity<User> {
            val id: Int
            @Column(sqlType = Test::class)
            val name: String
        }
        
        object Test
    """.trimIndent())
}