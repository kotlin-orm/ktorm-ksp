package org.ktorm.ksp.compiler.test.parser

import org.junit.Test
import org.ktorm.ksp.compiler.test.BaseTest

/**
 * Created by vince at Apr 16, 2023.
 */
class MetadataParserTest : BaseTest() {

    @Test
    fun testEnumClass() = kspFailing("Gender is expected to be a class or interface but actually ENUM_CLASS.", """
        @Table
        enum class Gender { MALE, FEMALE }
    """.trimIndent())

    @Test
    fun testInterfaceNotExtendingEntity() = kspFailing("User must extends from org.ktorm.entity.Entity.", """
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

    @Test
    fun testReferencesWithColumn() = kspFailing("Parse ref column error for property User.profile: @Column and @References cannot be used together.", """
        @Table
        interface User : Entity<User> {
            val id: Int
            @References
            @Column
            val profile: Profile
        }
        
        @Table
        interface Profile : Entity<Profile> {
            val id: Int
            val name: String
        }
    """.trimIndent())

    @Test
    fun testReferencesFromClassEntity() = kspFailing("Parse ref column error for property User.profile: @References can only be used in interface-based entities", """
        @Table
        class User(
            val id: Int,
            @References
            val profile: Profile
        )
        
        @Table
        interface Profile : Entity<Profile> {
            val id: Int
            val name: String
        }
    """.trimIndent())

    @Test
    fun testReferencesToClassEntity() = kspFailing("Parse ref column error for property User.profile: the referenced entity class must be an interface", """
        @Table
        interface User : Entity<User> {
            val id: Int
            @References
            val profile: Profile
        }
        
        @Table
        class Profile(
            val id: Int,
            val name: String
        )
    """.trimIndent())

    @Test
    fun testReferencesToNonTableClass() = kspFailing("Parse ref column error for property User.profile: the referenced entity class must be annotated with @Table", """
        @Table
        interface User : Entity<User> {
            val id: Int
            @References
            val profile: Profile
        }
        
        interface Profile : Entity<Profile> {
            val id: Int
            val name: String
        }
    """.trimIndent())

    @Test
    fun testReferencesNoPrimaryKeys() = kspFailing("Parse ref column error for property User.profile: the referenced table doesn't have a primary key", """
        @Table
        interface User : Entity<User> {
            val id: Int
            @References
            val profile: Profile
        }
        
        @Table
        interface Profile : Entity<Profile> {
            val id: Int
            val name: String
        }
    """.trimIndent())

    @Test
    fun testReferencesWithCompoundPrimaryKeys() = kspFailing("Parse ref column error for property User.profile: the referenced table cannot have compound primary keys", """
        @Table
        interface User : Entity<User> {
            val id: Int
            @References
            val profile: Profile
        }
        
        @Table
        interface Profile : Entity<Profile> {
            @PrimaryKey
            val id: Int
            @PrimaryKey
            val name: String
        }
    """.trimIndent())

    @Test
    fun testCircularReference() = kspFailing("Circular reference is not allowed, current table: User, reference route: Profile --> Operator --> User", """
        @Table
        interface User : Entity<User> {
            @PrimaryKey
            val id: Int
            @References
            val profile: Profile
        }
        
        @Table
        interface Profile : Entity<Profile> {
            @PrimaryKey
            val id: Int
            @References
            val operator: Operator
        }
        
        @Table
        interface Operator : Entity<Operator> {
            @PrimaryKey
            val id: Int
            @References
            val user: User
        }
    """.trimIndent())

    @Test
    fun testSelfReference() = kspFailing("Circular reference is not allowed, current table: User, reference route: User", """
        @Table
        interface User : Entity<User> {
            @PrimaryKey
            val id: Int
            @References
            val manager: User
        }
    """.trimIndent())
}
