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

package org.ktorm.ksp.compiler.test.config

import org.junit.Test
import org.ktorm.ksp.compiler.test.BaseTest

class SqlTypeTest : BaseTest() {

    @Test
    fun testCustomSqlType() = runKotlin("""
        @Table
        data class User(
            @PrimaryKey
            var id: Int,
            @Column(sqlType = UsernameSqlType::class)
            var username: Username,
            var age: Int,
            var gender: Int
        )
        
        data class Username(
            val firstName:String,
            val lastName:String
        )
        
        object UsernameSqlType : org.ktorm.schema.SqlType<Username>(Types.VARCHAR, "varchar") {
            override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: Username) {
                ps.setString(index, parameter.firstName + "#" + parameter.lastName)
            }
        
            override fun doGetResult(rs: ResultSet, index: Int): Username? {
                val (firstName, lastName) = rs.getString(index)?.split("#") ?: return null
                return Username(firstName, lastName)
            }
        }
        
        fun run() {
            assert(Users.username.sqlType == UsernameSqlType)
            assert(Users.username.sqlType.typeCode == Types.VARCHAR)
            assert(Users.username.sqlType.typeName == "varchar")
            
            database.users.add(User(100, Username("Vincent", "Lau"), 28, 0))
            assert(database.users.first { it.id eq 100 } == User(100, Username("Vincent", "Lau"), 28, 0))
        }
    """.trimIndent())

    @Test
    fun testCustomSqlTypeFactory() = runKotlin("""
        @Table
        data class User(
            @PrimaryKey
            var id: Int,
            var username: String,
            var age: Int,
            @Column(sqlType = IntEnumSqlTypeFactory::class)
            var gender: Gender
        )
        
        enum class Gender {
            MALE,
            FEMALE
        }
        
        object IntEnumSqlTypeFactory : SqlTypeFactory {
        
            @Suppress("UNCHECKED_CAST")
            override fun <T : Any> createSqlType(property: KProperty1<*, T?>): org.ktorm.schema.SqlType<T> {
                val returnType = property.returnType.jvmErasure.java
                if (returnType.isEnum) {
                    return IntEnumSqlType(returnType as Class<out Enum<*>>) as org.ktorm.schema.SqlType<T>
                } else {
                    throw IllegalArgumentException("The property is required to be typed of enum but actually: ${"$"}returnType")
                }
            }
        
            private class IntEnumSqlType<E : Enum<E>>(val enumClass: Class<E>) : org.ktorm.schema.SqlType<E>(Types.INTEGER, "int") {
                override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: E) {
                    ps.setInt(index, parameter.ordinal)
                }
        
                override fun doGetResult(rs: ResultSet, index: Int): E? {
                    return enumClass.enumConstants[rs.getInt(index)]
                }
            }
        }
        
        fun run() {
            assert(Users.gender.sqlType::class.simpleName == "IntEnumSqlType")
            assert(Users.gender.sqlType.typeCode == Types.INTEGER)
            assert(Users.gender.sqlType.typeName == "int")
            
            database.users.add(User(100, "Vincent", 28, Gender.MALE))
            assert(database.users.first { it.id eq 100 } == User(100, "Vincent", 28, Gender.MALE))
        }
    """.trimIndent())
}
