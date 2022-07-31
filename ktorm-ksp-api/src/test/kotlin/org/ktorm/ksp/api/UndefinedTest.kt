/*
 * Copyright 2018-2021 the original author or authors.
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

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ktorm.entity.Entity

public class UndefinedTest {

    private inline fun <reified T : Any> undefinedValueTest(value: T?) {
        val undefined1 = undefined<T>()
        val undefined2 = undefined<T>()
        assertThat(undefined1 !== value).isTrue
        assertThat(undefined2 !== value).isTrue
        assertThat(undefined1 === undefined2).isTrue
        println(undefined1!!.javaClass.name)
    }

    private fun testUndefinedInt(haveValue: Boolean, value: Int? = undefined()) {
        if (haveValue) {
            assert(value !== undefined<Int>())
        } else {
            assert(value === undefined<Int>())
        }
    }

    private fun testUndefinedUInt(haveValue: Boolean, value: UInt? = undefined()) {
        if (haveValue) {
            assert((value as Any?) !== (undefined<UInt>() as Any?))
        } else {
            assert((value as Any?) === (undefined<UInt>() as Any?))
        }
    }

    @Test
    public fun `undefined inlined class type`() {
        testUndefinedUInt(haveValue = true, value = 1U)
        testUndefinedUInt(haveValue = true, value = 0U)
        testUndefinedUInt(haveValue = true, value = null)
        testUndefinedUInt(haveValue = false)
    }

    @Test
    public fun `undefined boxed primitive type`() {
        testUndefinedInt(haveValue = true, value = 1)
        testUndefinedInt(haveValue = true, value = 0)
        testUndefinedInt(haveValue = true, value = null)
        testUndefinedInt(haveValue = false)

        undefinedValueTest(0.toByte())
        undefinedValueTest(0.toShort())
        undefinedValueTest(0)
        undefinedValueTest(0L)
        undefinedValueTest('0')
        undefinedValueTest(false)
        undefinedValueTest(0f)
        undefinedValueTest(0.0)
    }

    private interface Employee : Entity<Employee>

    public enum class Gender {
        MALE,
        FEMALE
    }

    private abstract class Biology

    @Suppress("unused")
    private abstract class Animal(val name: String) : Biology()

    @Suppress("unused")
    private class Dog(val age: Int) : Animal("dog")

    private data class Cat(val age: Int) : Animal("cat")

    @Test
    public fun `undefined interface`() {
        undefinedValueTest(Entity.create<Employee>())
        undefinedValueTest<java.io.Serializable>(null)
    }

    @Test
    public fun `undefined abstract class`() {
        undefinedValueTest<Biology>(Dog(0))
        undefinedValueTest<Animal>(Dog(0))
        undefinedValueTest<Number>(0)
    }

    @Test
    public fun `undefined enum`() {
        undefinedValueTest(Gender.MALE)
    }

    @Test
    public fun `undefined class`() {
        undefinedValueTest(Dog(0))
    }

    @Test
    public fun `undefined data class`() {
        undefinedValueTest(Cat(0))
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
        undefinedValueTest(teacher)
        val aClass = school.Class("A")
        undefinedValueTest(aClass)
    }

    @Test
    public fun `undefined object`() {
        undefinedValueTest(Unit)
    }

    @Test
    public fun `undefined companion object`() {
        undefinedValueTest(Int.Companion)
    }

    @Test
    public fun `undefined function`() {
        undefinedValueTest<(Int) -> String> { it.toString() }
    }

    @Test
    public fun `undefined array`() {
        undefinedValueTest(intArrayOf())
        undefinedValueTest<Array<School>>(arrayOf())
    }
}
