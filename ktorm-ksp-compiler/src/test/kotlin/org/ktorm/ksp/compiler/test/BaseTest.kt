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

package org.ktorm.ksp.compiler.test

import com.tschuchort.compiletesting.*
import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.junit.After
import org.junit.Before
import org.ktorm.database.Database
import org.ktorm.database.use
import org.ktorm.ksp.compiler.KtormProcessorProvider
import org.ktorm.logging.ConsoleLogger
import org.ktorm.logging.LogLevel
import java.lang.reflect.InvocationTargetException

abstract class BaseTest {
    lateinit var database: Database

    @Before
    fun init() {
        database = Database.connect(
            url = "jdbc:h2:mem:ktorm;DB_CLOSE_DELAY=-1",
            logger = ConsoleLogger(threshold = LogLevel.TRACE),
            alwaysQuoteIdentifiers = true
        )

        execSqlScript("init-data.sql")
    }

    @After
    fun destroy() {
        execSqlScript("drop-data.sql")
    }

    private fun execSqlScript(filename: String) {
        database.useConnection { conn ->
            conn.createStatement().use { statement ->
                javaClass.classLoader
                    ?.getResourceAsStream(filename)
                    ?.bufferedReader()
                    ?.use { reader ->
                        for (sql in reader.readText().split(';')) {
                            if (sql.any { it.isLetterOrDigit() }) {
                                statement.executeUpdate(sql)
                            }
                        }
                    }
            }
        }
    }

    protected fun runKotlin(@Language("kotlin") code: String, vararg options: Pair<String, String>) {
        val result = compile(code, mapOf(*options))
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)

        try {
            val cls = result.classLoader.loadClass("SourceKt")
            cls.getMethod("setDatabase", Database::class.java).invoke(null, database)
            cls.getMethod("run").invoke(null)
        } catch (e: InvocationTargetException) {
            throw e.targetException
        }
    }

    private fun compile(@Language("kotlin") code: String, options: Map<String, String>): KotlinCompilation.Result {
        @Language("kotlin")
        val header = """
            import java.math.*
            import java.sql.*
            import java.time.*
            import java.util.*
            import kotlin.reflect.*
            import kotlin.reflect.jvm.*
            import org.ktorm.database.*
            import org.ktorm.dsl.*
            import org.ktorm.entity.*
            import org.ktorm.ksp.api.*
            
            lateinit var database: Database
            
            
        """.trimIndent()

        val source = header + code
        printFile(source, "Source.kt")

        val compilation = createCompilation(SourceFile.kotlin("Source.kt", source), options)
        val result = compilation.compile()

        for (file in compilation.kspSourcesDir.walk()) {
            if (file.isFile) {
                printFile(file.readText(), "Generated file: ${file.absolutePath}")
            }
        }

        return result
    }

    private fun createCompilation(source: SourceFile, options: Map<String, String>): KotlinCompilation {
        return KotlinCompilation().apply {
            sources = listOf(source)
            verbose = false
            messageOutputStream = System.out
            inheritClassPath = true
            allWarningsAsErrors = true
            symbolProcessorProviders = listOf(KtormProcessorProvider())
            kspIncremental = true
            kspWithCompilation = true
            kspArgs += options
        }
    }

    private fun printFile(text: String, title: String) {
        val lines = text.lines()
        val gutterSize = lines.size.toString().count()

        println("${"#".repeat(gutterSize + 2)}-----------------------------------------")
        println("${"#".repeat(gutterSize + 2)} $title")
        println("${"#".repeat(gutterSize + 2)}-----------------------------------------")

        for ((i, line) in lines.withIndex()) {
            println(String.format("#%${gutterSize}d| %s", i + 1, line))
        }
    }
}
