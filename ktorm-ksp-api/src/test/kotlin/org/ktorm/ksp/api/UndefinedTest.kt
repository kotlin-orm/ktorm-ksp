/*
 * Copyright 2022 the original author or authors.
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

package org.ktorm.ksp.api

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.ktorm.entity.Entity

public class UndefinedTest {
    @get:Rule
    public val testName: TestName = TestName()

    private inline fun <reified T : Any> testUndefined(value: T?) {
        val undefined1 = Undefined.of<T>()
        val undefined2 = Undefined.of<T>()

        assert(undefined1 is T)
        assert(undefined2 is T)
        assert(undefined1 !== value)
        assert(undefined2 !== value)
        assert(undefined1 === undefined2)

        println("Undefined Class Name: " + undefined1!!.javaClass.name)
    }

    private fun testUndefinedInt(haveValue: Boolean, value: Int? = Undefined.of()) {
        val undefined = Undefined.of<Int>()
        println("Undefined Class Name: " + undefined!!.javaClass.name)

        if (haveValue) {
            assert(value !== undefined)
        } else {
            assert(value === undefined)
        }
    }

    private fun testUndefinedUInt(haveValue: Boolean, value: UInt? = Undefined.of()) {
        val undefined = Undefined.of<UInt>()
        println("Undefined Class Name: " + undefined!!.javaClass.name)

        if (haveValue) {
            assert((value as Any?) !== (undefined as Any?))
        } else {
            assert((value as Any?) === (undefined as Any?))
        }
    }

    @Before
    public fun before() {
        println("-----------------------------------")
        println("Test Name : " + testName.methodName)
        println("JDK Version: " + System.getProperty("java.version"))
    }

    @Test
    public fun `undefined inlined class`() {
        testUndefinedUInt(haveValue = true, value = 1U)
        testUndefinedUInt(haveValue = true, value = 0U)
        testUndefinedUInt(haveValue = true, value = null)
        testUndefinedUInt(haveValue = false)

        testUndefined(0.toUByte())
        testUndefined(0.toUShort())
        testUndefined(0U)
        testUndefined(0UL)
    }

    @Test
    public fun `undefined primitive type`() {
        testUndefinedInt(haveValue = true, value = 1)
        testUndefinedInt(haveValue = true, value = 0)
        testUndefinedInt(haveValue = true, value = null)
        testUndefinedInt(haveValue = false)

        testUndefined(0.toByte())
        testUndefined(0.toShort())
        testUndefined(0)
        testUndefined(0L)
        testUndefined('0')
        testUndefined(false)
        testUndefined(0f)
        testUndefined(0.0)
    }

    private interface Employee : Entity<Employee>

    public enum class Gender {
        MALE,
        FEMALE
    }

    public abstract class Biology

    @Suppress("unused")
    public abstract class Animal(public val name: String) : Biology()

    @Suppress("unused")
    private class Dog(val age: Int) : Animal("dog")

    private data class Cat(val age: Int) : Animal("cat")

    @Test
    public fun `undefined interface`() {
        testUndefined(Entity.create<Employee>())
        testUndefined<java.io.Serializable>(null)
    }

    @Test
    public fun `undefined abstract class`() {
        testUndefined<Biology>(Dog(0))
        testUndefined<Animal>(Dog(0))
        testUndefined<Number>(0)
    }

    @Test
    public fun `undefined enum`() {
        testUndefined(Gender.MALE)
        testUndefined(Gender.FEMALE)
    }

    @Test
    public fun `undefined class`() {
        testUndefined(Dog(0))
    }

    @Test
    public fun `undefined data class`() {
        testUndefined(Cat(0))
    }

    private class School {
        inner class Teacher

        @Suppress("unused")
        inner class Class(private val name: String)
    }

    @Test
    public fun `undefined inner class`() {
        val school = School()
        val teacher = school.Teacher()
        testUndefined(teacher)
        val aClass = school.Class("A")
        testUndefined(aClass)
    }

    @Test
    public fun `undefined object`() {
        testUndefined(Unit)
    }

    @Test
    public fun `undefined companion object`() {
        testUndefined(Int.Companion)
    }

    @Test
    public fun `undefined function`() {
        testUndefined<(Int) -> String> { it.toString() }
    }

    @Test
    public fun `undefined array`() {
        testUndefined(intArrayOf())
        testUndefined<Array<School>>(arrayOf())
    }
}
