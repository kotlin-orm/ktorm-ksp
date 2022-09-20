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

package org.ktorm.ksp.compiler.test

import com.tschuchort.compiletesting.*
import org.ktorm.ksp.compiler.KtormProcessorProvider
import java.io.File

public abstract class BaseKspTest : BaseTest() {

    protected open fun createKspCompiler(vararg sourceFiles: SourceFile, useKsp: Boolean = true): KotlinCompilation {
        return KotlinCompilation().apply {
            workingDir = temporaryFolder.root
            sources = sourceFiles.toList()
            if (useKsp) {
                symbolProcessorProviders = listOf(KtormProcessorProvider())
            }
            inheritClassPath = true
            messageOutputStream = System.out
            kspIncremental = true
        }
    }

    /**
     * The first compilation uses ksp to generate code.
     * The second compilation verifies the code generated by ksp.
     */
    protected fun twiceCompile(
        vararg sourceFiles: SourceFile,
        sourceFileBlock: (String) -> Unit = {},
    ): Pair<KotlinCompilation.Result, KotlinCompilation.Result> {
        val compiler1 = createKspCompiler(*sourceFiles)
        val result1 = compiler1.compile()
        val result2 =
            createKspCompiler(
                *(compiler1.kspGeneratedSourceFiles + sourceFiles).toTypedArray(),
                useKsp = false
            ).compile()
        compiler1.kspGeneratedFiles.forEach { sourceFileBlock(it.readText()) }
        return result1 to result2
    }

    protected fun compile(
        vararg sourceFiles: SourceFile,
        printKspGenerateFile: Boolean = false
    ): KotlinCompilation.Result {
        val compilation = createKspCompiler(*sourceFiles)
        val result = compilation.compile()
        if (printKspGenerateFile) {
            compilation.kspSourcesDir.walkTopDown()
                .filter { it.extension == "kt" }
                .forEach { println(it.readText()) }
        }
        return result
    }

    protected val KotlinCompilation.kspGeneratedSourceFiles: List<SourceFile>
        get() = kspSourcesDir.resolve("kotlin")
            .walk()
            .filter { it.isFile }
            .map { SourceFile.fromPath(it.absoluteFile) }
            .toList()

    protected val KotlinCompilation.kspGeneratedFiles: List<File>
        get() = kspSourcesDir.resolve("kotlin")
            .walk()
            .filter { it.isFile }
            .toList()
}
