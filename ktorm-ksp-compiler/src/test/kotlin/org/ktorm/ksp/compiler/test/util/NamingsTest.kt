package org.ktorm.ksp.compiler.test.util

import org.junit.Test
import org.ktorm.ksp.compiler.util.CamelCase
import kotlin.test.assertEquals

class NamingsTest {

    @Test
    fun testCamelCase() {
        assertEquals("abc_def", CamelCase.toLowerSnakeCase("abcDef"))
        assertEquals("abc_def", CamelCase.toLowerSnakeCase("AbcDef"))
        assertEquals("ABC_DEF", CamelCase.toUpperSnakeCase("abcDef"))
        assertEquals("ABC_DEF", CamelCase.toUpperSnakeCase("AbcDef"))
        assertEquals("abcDef", CamelCase.toFirstLowerCamelCase("abcDef"))
        assertEquals("abcDef", CamelCase.toFirstLowerCamelCase("AbcDef"))

        assertEquals("abc_def", CamelCase.toLowerSnakeCase("ABCDef"))
        assertEquals("ABC_DEF", CamelCase.toUpperSnakeCase("ABCDef"))
        assertEquals("abcDef", CamelCase.toFirstLowerCamelCase("ABCDef"))

        assertEquals("io_utils", CamelCase.toLowerSnakeCase("IOUtils"))
        assertEquals("IO_UTILS", CamelCase.toUpperSnakeCase("IOUtils"))
        assertEquals("ioUtils", CamelCase.toFirstLowerCamelCase("IOUtils"))

        assertEquals("pwd_utils", CamelCase.toLowerSnakeCase("PWDUtils"))
        assertEquals("PWD_UTILS", CamelCase.toUpperSnakeCase("PWDUtils"))
        assertEquals("pwdUtils", CamelCase.toFirstLowerCamelCase("PWDUtils"))

        assertEquals("pwd_utils", CamelCase.toLowerSnakeCase("PwdUtils"))
        assertEquals("PWD_UTILS", CamelCase.toUpperSnakeCase("PwdUtils"))
        assertEquals("pwdUtils", CamelCase.toFirstLowerCamelCase("PwdUtils"))

        assertEquals("test_io", CamelCase.toLowerSnakeCase("testIO"))
        assertEquals("TEST_IO", CamelCase.toUpperSnakeCase("testIO"))
        assertEquals("testIO", CamelCase.toFirstLowerCamelCase("testIO"))

        assertEquals("test_pwd", CamelCase.toLowerSnakeCase("testPWD"))
        assertEquals("TEST_PWD", CamelCase.toUpperSnakeCase("testPWD"))
        assertEquals("testPWD", CamelCase.toFirstLowerCamelCase("testPWD"))

        assertEquals("test_pwd", CamelCase.toLowerSnakeCase("testPwd"))
        assertEquals("TEST_PWD", CamelCase.toUpperSnakeCase("testPwd"))
        assertEquals("testPwd", CamelCase.toFirstLowerCamelCase("testPwd"))

        assertEquals("a2c_count", CamelCase.toLowerSnakeCase("A2CCount"))
        assertEquals("A2C_COUNT", CamelCase.toUpperSnakeCase("A2CCount"))
        assertEquals("a2cCount", CamelCase.toFirstLowerCamelCase("A2CCount"))
    }
}