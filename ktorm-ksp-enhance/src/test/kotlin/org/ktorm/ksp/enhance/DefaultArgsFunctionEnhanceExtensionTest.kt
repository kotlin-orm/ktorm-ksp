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

@file:Suppress("RemoveRedundantBackticks")

package org.ktorm.ksp.enhance

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import com.tschuchort.compiletesting.SourceFile
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ktorm.ksp.enhance.kotlin.KtormKspEnhanceComponentRegistrar
import org.ktorm.ksp.tests.BaseTest

public class DefaultArgsFunctionEnhanceExtensionTest : BaseTest() {

    override fun createCompiler(vararg sourceFiles: SourceFile): KotlinCompilation {
        val compiler = super.createCompiler(*sourceFiles)
        compiler.compilerPlugins = listOf(KtormKspEnhanceComponentRegistrar())
        return compiler
    }

    @Test
    public fun `default args function enhance`() {
        val file = SourceFile.kotlin(
            "source.kt", """
            import org.ktorm.ksp.api.KtormKspDefaultArgsVirtualFunction
            import org.ktorm.ksp.api.KtormKspDefaultArgsImplementationFunction
            
            @KtormKspDefaultArgsVirtualFunction
            fun user(name: String = error("undefined"), age: Int = error("undefined")): String {
                throw IllegalArgumentException()
            } 
            
            @KtormKspDefaultArgsImplementationFunction
            fun `${'$'}user${'$'}implementation`(name: String?, age:Int?, flag:Int): String {
                val map = mutableMapOf<String, Any?>()
                if (flag and 1 != 0) {
                    map["name"] = name
                }
                if (flag and 2 != 0) {
                    map["age"] = age
                }
                return map.toString()
            }

            object TestBridge {
                fun callNoArgs(): String {
                    return user()
                }

                fun callAllArgs(): String {
                    return user("jack", 10)
                }
                
                fun callOneArgs(): String {
                    return user("jack")
                }
            }
        """
        )
        val compiler = createCompiler(file)
        val result = compiler.compile()
        assertThat(result.exitCode).isEqualTo(ExitCode.OK)
        val noArgs = result.invokeBridge("callNoArgs") as String
        assertThat(noArgs).isEqualTo("{}")
        val oneArgs = result.invokeBridge("callOneArgs") as String
        assertThat(oneArgs).isEqualTo("{name=jack}")
        val allArgs = result.invokeBridge("callAllArgs") as String
        assertThat(allArgs).isEqualTo("{name=jack, age=10}")
    }

    @Test
    public fun `multi args`() {
        val file = SourceFile.kotlin(
            "source.kt", """
            import org.ktorm.ksp.api.KtormKspDefaultArgsVirtualFunction
            import org.ktorm.ksp.api.KtormKspDefaultArgsImplementationFunction
            
            @KtormKspDefaultArgsVirtualFunction
            fun user(arg1: String = error("undefined"),
                     arg2: String = error("undefined"),
                     arg3: String = error("undefined"),
                     arg4: String = error("undefined"),
                     arg5: String = error("undefined"),
                     arg6: String = error("undefined"),
                     arg7: String = error("undefined"),
                     arg8: String = error("undefined"),
                     arg9: String = error("undefined"),
                     arg10: String = error("undefined"),
                     arg11: String = error("undefined"),
                     arg12: String = error("undefined"),
                     arg13: String = error("undefined"),
                     arg14: String = error("undefined"),
                     arg15: String = error("undefined"),
                     arg16: String = error("undefined"),
                     arg17: String = error("undefined"),
                     arg18: String = error("undefined"),
                     arg19: String = error("undefined"),
                     arg20: String = error("undefined"),
                     arg21: String = error("undefined"),
                     arg22: String = error("undefined"),
                     arg23: String = error("undefined"),
                     arg24: String = error("undefined"),
                     arg25: String = error("undefined"),
                     arg26: String = error("undefined"),
                     arg27: String = error("undefined"),
                     arg28: String = error("undefined"),
                     arg29: String = error("undefined"),
                     arg30: String = error("undefined"),
                     arg31: String = error("undefined"),
                     arg32: String = error("undefined"),
                     arg33: String = error("undefined")): String {
                throw IllegalArgumentException()
            } 

            @KtormKspDefaultArgsImplementationFunction
            fun `${'$'}user${'$'}implementation`(
                     arg1: String?,
                     arg2: String?,
                     arg3: String?,
                     arg4: String?,
                     arg5: String?,
                     arg6: String?,
                     arg7: String?,
                     arg8: String?,
                     arg9: String?,
                     arg10: String?,
                     arg11: String?,
                     arg12: String?,
                     arg13: String?,
                     arg14: String?,
                     arg15: String?,
                     arg16: String?,
                     arg17: String?,
                     arg18: String?,
                     arg19: String?,
                     arg20: String?,
                     arg21: String?,
                     arg22: String?,
                     arg23: String?,
                     arg24: String?,
                     arg25: String?,
                     arg26: String?,
                     arg27: String?,
                     arg28: String?,
                     arg29: String?,
                     arg30: String?,
                     arg31: String?,
                     arg32: String?,
                     arg33: String?, 
                     flag1:Int,
                     flag2:Int): String {
                val map = mutableMapOf<String, Any?>()
                if (flag1 and 1 != 0) { map["arg1"] = arg1 }
                if (flag1 and 2 != 0) { map["arg2"] = arg2 }
                if (flag1 and 4 != 0) { map["arg3"] = arg3 }
                if (flag1 and 8 != 0) { map["arg4"] = arg4 }
                if (flag1 and 16 != 0) { map["arg5"] = arg5 }
                if (flag1 and 32 != 0) { map["arg6"] = arg6 }
                if (flag1 and 64 != 0) { map["arg7"] = arg7 }
                if (flag1 and 128 != 0) { map["arg8"] = arg8 }
                if (flag1 and 256 != 0) { map["arg9"] = arg9 }
                if (flag1 and 512 != 0) { map["arg10"] = arg10 }
                if (flag1 and 1024 != 0) { map["arg11"] = arg11 }
                if (flag1 and 2048 != 0) { map["arg12"] = arg12 }
                if (flag1 and 4096 != 0) { map["arg13"] = arg13 }
                if (flag1 and 8192 != 0) { map["arg14"] = arg14 }
                if (flag1 and 16384 != 0) { map["arg15"] = arg15 }
                if (flag1 and 32768 != 0) { map["arg16"] = arg16 }
                if (flag1 and 65536 != 0) { map["arg17"] = arg17 }
                if (flag1 and 131072 != 0) { map["arg18"] = arg18 }
                if (flag1 and 262144 != 0) { map["arg19"] = arg19 }
                if (flag1 and 524288 != 0) { map["arg20"] = arg20 }
                if (flag1 and 1048576 != 0) { map["arg21"] = arg21 }
                if (flag1 and 2097152 != 0) { map["arg22"] = arg22 }
                if (flag1 and 4194304 != 0) { map["arg23"] = arg23 }
                if (flag1 and 8388608 != 0) { map["arg24"] = arg24 }
                if (flag1 and 16777216 != 0) { map["arg25"] = arg25 }
                if (flag1 and 33554432 != 0) { map["arg26"] = arg26 }
                if (flag1 and 67108864 != 0) { map["arg27"] = arg27 }
                if (flag1 and 134217728 != 0) { map["arg28"] = arg28 }
                if (flag1 and 268435456 != 0) { map["arg29"] = arg29 }
                if (flag1 and 536870912 != 0) { map["arg30"] = arg30 }
                if (flag1 and 1073741824 != 0) { map["arg31"] = arg31 }
                if (flag1 and -2147483648 != 0) { map["arg32"] = arg32 }
                if (flag2 and 1 != 0) { map["arg33"] = arg33 }
                return map.toString()
            }

            object TestBridge {
                fun callNoArgs(): String {
                    return user()
                }
                fun callOneArgs(): String {
                    return user(arg1 = "arg1")
                }
                fun callAllArgs(): String {
                    return user(arg1 = "arg1",
                                arg2 = "arg2",
                                arg3 = "arg3",
                                arg4 = "arg4",
                                arg5 = "arg5",
                                arg6 = "arg6",
                                arg7 = "arg7",
                                arg8 = "arg8",
                                arg9 = "arg9",
                                arg10 = "arg10",
                                arg11 = "arg11",
                                arg12 = "arg12",
                                arg13 = "arg13",
                                arg14 = "arg14",
                                arg15 = "arg15",
                                arg16 = "arg16",
                                arg17 = "arg17",
                                arg18 = "arg18",
                                arg19 = "arg19",
                                arg20 = "arg20",
                                arg21 = "arg21",
                                arg22 = "arg22",
                                arg23 = "arg23",
                                arg24 = "arg24",
                                arg25 = "arg25",
                                arg26 = "arg26",
                                arg27 = "arg27",
                                arg28 = "arg28",
                                arg29 = "arg29",
                                arg30 = "arg30",
                                arg31 = "arg31",
                                arg32 = "arg32",
                                arg33 = "arg33")
                }
            }
        """
        )
        val compiler = createCompiler(file)
        val result = compiler.compile()
        assertThat(result.exitCode).isEqualTo(ExitCode.OK)
        val noArgs = result.invokeBridge("callNoArgs") as String
        val oneArgs = result.invokeBridge("callOneArgs") as String
        val allArgs = result.invokeBridge("callAllArgs") as String
        assertThat(noArgs).isEqualTo("{}")
        assertThat(oneArgs).isEqualTo("{arg1=arg1}")
        assertThat(allArgs).isEqualTo("{arg1=arg1, arg2=arg2, arg3=arg3, arg4=arg4, arg5=arg5, arg6=arg6, arg7=arg7, arg8=arg8, arg9=arg9, arg10=arg10, arg11=arg11, arg12=arg12, arg13=arg13, arg14=arg14, arg15=arg15, arg16=arg16, arg17=arg17, arg18=arg18, arg19=arg19, arg20=arg20, arg21=arg21, arg22=arg22, arg23=arg23, arg24=arg24, arg25=arg25, arg26=arg26, arg27=arg27, arg28=arg28, arg29=arg29, arg30=arg30, arg31=arg31, arg32=arg32, arg33=arg33}")
    }

    @Suppress("unused")
    @Test
    public fun `extension receiver or args`() {
        val file = SourceFile.kotlin(
            "source.kt", """
            import org.ktorm.ksp.api.KtormKspDefaultArgsVirtualFunction
            import org.ktorm.ksp.api.KtormKspDefaultArgsImplementationFunction
            @KtormKspDefaultArgsVirtualFunction
            fun Any.user(name: String = error("undefined"), age: Int = error("undefined")): String {
                throw IllegalArgumentException()
            } 

            @KtormKspDefaultArgsImplementationFunction
            fun Any.`${'$'}user${'$'}implementation`(name: String?, age:Int?, flag:Int): String {
                val map = mutableMapOf<String, Any?>()
                if (flag and 1 != 0) {
                    map["name"] = name
                }
                if (flag and 2 != 0) {
                    map["age"] = age
                }
                return map.toString()
            }

            object TestBridge {
                fun extension() {
                    var user = Any().user()
                    assert(user == "{}")
                    user = Any().user(name = "jack")
                    assert(user == "{name=jack}")
                    user = Any().user(name = "jack", age = 10)
                    assert(user == "{name=jack, age=10}")
                }
                fun arg() {
                    var user = StringBuilder(user()).toString()
                    assert(user == "{}")
                    user = StringBuilder(user(name = "jack")).toString()
                    assert(user == "{name=jack}")
                    user = StringBuilder(user(name = "jack", age = 10)).toString()
                    assert(user == "{name=jack, age=10}")
                }
            }
        """
        )
        val compiler = createCompiler(file)
        val result = compiler.compile()
        assertThat(result.exitCode).isEqualTo(ExitCode.OK)
        result.invokeBridge("extension")
        result.invokeBridge("arg")
    }

    @Test
    public fun `dispatch receiver`() {
        val file = SourceFile.kotlin(
            "source.kt", """
            import org.ktorm.ksp.api.KtormKspDefaultArgsVirtualFunction
            import org.ktorm.ksp.api.KtormKspDefaultArgsImplementationFunction
            
            

            object TestBridge {
                
                @KtormKspDefaultArgsVirtualFunction
                fun user(name: String = error("undefined"), age: Int = error("undefined")): String {
                    throw IllegalArgumentException()
                } 
    
                @KtormKspDefaultArgsImplementationFunction
                fun `${'$'}user${'$'}implementation`(name: String?, age:Int?, flag:Int): String {
                    val map = mutableMapOf<String, Any?>()
                    if (flag and 1 != 0) {
                        map["name"] = name
                    }
                    if (flag and 2 != 0) {
                        map["age"] = age
                    }
                    return map.toString()
                }

                fun callNoArgs(): String {
                    return user()
                }

                fun callAllArgs(): String {
                    return user("jack", 10)
                }
                
                fun callOneArgs(): String {
                    return user("jack")
                }
            }
        """
        )
        val compiler = createCompiler(file)
        val result = compiler.compile()
        assertThat(result.exitCode).isEqualTo(ExitCode.OK)
        val noArgs = result.invokeBridge("callNoArgs") as String
        assertThat(noArgs).isEqualTo("{}")
        val oneArgs = result.invokeBridge("callOneArgs") as String
        assertThat(oneArgs).isEqualTo("{name=jack}")
        val allArgs = result.invokeBridge("callAllArgs") as String
        assertThat(allArgs).isEqualTo("{name=jack, age=10}")
    }
}
