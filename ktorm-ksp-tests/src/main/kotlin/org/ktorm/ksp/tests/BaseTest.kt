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

package org.ktorm.ksp.tests

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.ktorm.database.Database
import org.ktorm.logging.ConsoleLogger
import org.ktorm.logging.LogLevel
import org.ktorm.schema.BaseTable
import org.ktorm.schema.Table
import kotlin.reflect.full.functions

public abstract class BaseTest {

    @Rule
    @JvmField
    public val temporaryFolder: TemporaryFolder = TemporaryFolder()

    protected open fun createCompiler(vararg sourceFiles: SourceFile): KotlinCompilation {
        return KotlinCompilation().apply {
            workingDir = temporaryFolder.root
            sources = sourceFiles.toList()
            inheritClassPath = true
            messageOutputStream = System.out
        }
    }

    protected fun KotlinCompilation.Result.getBaseTable(className: String): BaseTable<*> {
        val cls = classLoader.loadClass("$className\$Companion")
        return cls.kotlin.objectInstance as BaseTable<*>
    }

    protected fun KotlinCompilation.Result.getTable(className: String): Table<*> {
        val cls = classLoader.loadClass("$className\$Companion")
        return cls.kotlin.objectInstance as Table<*>
    }

    protected inline fun useDatabase(action: (Database) -> Unit) {
        Database.connect(
            url = "jdbc:h2:mem:ktorm;",
            driver = "org.h2.Driver",
            logger = ConsoleLogger(threshold = LogLevel.TRACE),
            alwaysQuoteIdentifiers = true
        ).apply {
            this.useConnection {
                it.createStatement().use { statement ->
                    val sql =
                        BaseTest::class.java.classLoader.getResourceAsStream("init-data.sql")!!.bufferedReader()
                            .readText()
                    statement.executeUpdate(sql)
                }
                action(this)
            }
        }
    }

    private fun Any.reflectionInvoke(methodName: String, vararg args: Any?): Any? {
        return this::class.functions.first { it.name == methodName }.call(this, *args)
    }

    protected fun KotlinCompilation.Result.invokeBridge(methodName: String, vararg args: Any?): Any? {
        val bridgeClass = this.classLoader.loadClass("TestBridge")
        val bridge = bridgeClass.kotlin.objectInstance!!
        return bridge.reflectionInvoke(methodName, *args)
    }
}
