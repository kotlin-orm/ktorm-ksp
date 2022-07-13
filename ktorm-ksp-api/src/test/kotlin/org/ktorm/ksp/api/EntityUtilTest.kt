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
import org.ktorm.ksp.api.EntityUtil.undefined

public class EntityUtilTest {

    private inline fun <reified T> undefinedValueTest(value: T) {
        val intUndefined1 = undefined<T>()
        val intUndefined2 = undefined<T>()
        assertThat(intUndefined1 !== value).isTrue
        assertThat(intUndefined2 !== value).isTrue
        assertThat(intUndefined1 === intUndefined2).isTrue
    }

    @Test
    public fun `undefined primitive type`() {
        undefinedValueTest<Byte?>(0)
        undefinedValueTest<Short?>(0)
        undefinedValueTest<Int?>(0)
        undefinedValueTest<Long?>(0)
        undefinedValueTest<Char?>('0')
        undefinedValueTest<Boolean?>(false)
        undefinedValueTest<Float?>(0f)
        undefinedValueTest<Double?>(0.0)
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
        undefinedValueTest<Employee>(Entity.create())
    }

    @Test
    public fun `undefined abstract class`() {
        undefinedValueTest<Biology>(Dog(0))
        try {
            undefined<Animal>()
        } catch (e: CreateUndefinedException) {
            return
        }
        error("fail")
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
}
