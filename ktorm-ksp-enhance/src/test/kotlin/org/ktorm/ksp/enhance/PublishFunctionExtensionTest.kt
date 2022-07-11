/*
 *  Copyright 2018-2021 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.ktorm.ksp.enhance

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ktorm.ksp.enhance.kotlin.KtormKspEnhanceComponentRegistrar
import org.ktorm.ksp.tests.BaseTest
import java.lang.reflect.Modifier
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod

public class PublishFunctionExtension : BaseTest() {

    override fun createCompiler(vararg sourceFiles: SourceFile): KotlinCompilation {
        val compiler = super.createCompiler(*sourceFiles)
        compiler.compilerPlugins = listOf(KtormKspEnhanceComponentRegistrar())
        return compiler
    }

    @Test
    public fun `publish function`() {
        val file = SourceFile.kotlin(
            "source.kt", """
            import org.ktorm.ksp.api.PublishFunction
            import kotlin.reflect.KFunction
            import kotlin.reflect.KVisibility
            import kotlin.reflect.full.functions
            
            object Cat {
                @PublishFunction
                private fun name(): String = "cat"
            }

            object TestBridge {
                public fun getFunction(): KFunction<*> {
                    return Cat::class.functions.first { it.name == "name" }
                }
            }
        """
        )
        val result = createCompiler(file).compile()
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        val function = result.invokeBridge("getFunction") as KFunction<*>
        assertThat(Modifier.isPublic(function.javaMethod!!.modifiers)).isTrue
    }

}