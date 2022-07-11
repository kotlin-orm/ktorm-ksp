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

package org.ktorm.ksp.ext

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import com.tschuchort.compiletesting.SourceFile
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ktorm.ksp.enhance.kotlin.KtormKspEnhanceComponentRegistrar
import org.ktorm.ksp.tests.BaseKspTest
import java.time.LocalDate

public class InterfaceEntityConstructorFunGeneratorTest : BaseKspTest() {

    override fun createKspCompiler(vararg sourceFiles: SourceFile, useKsp: Boolean): KotlinCompilation {
        val compiler = super.createKspCompiler(*sourceFiles, useKsp = useKsp)
        if (!useKsp) {
            compiler.compilerPlugins = listOf(KtormKspEnhanceComponentRegistrar())
        }
        return compiler
    }

    @Test
    public fun `generate test`() {
        val (result1, result2) = twiceCompile(
            SourceFile.kotlin(
                "source.kt", """
            import org.ktorm.entity.Entity
            import java.time.LocalDate 
            import org.ktorm.ksp.api.PrimaryKey
            import org.ktorm.ksp.api.Table

            @Table
            public interface User: Entity<User> {
                @PrimaryKey
                var id: Int?
                var username: String?
                var age: Int
                var birthday: LocalDate
            }
            
            object TestBridge {
                public fun callNoArgs(): String {
                    return User().toString()
                }

                public fun callOneArgs(): String {
                    return User(id = null).toString()
                }

                public fun callAllArgs(): String {
                    return User(id = null, username = "jack", age = 10, birthday = LocalDate.MIN).toString()
                }

                public fun userWrapper(): String {
                    return UserWrapper(User()).toString()
                }
            }
        """
            )
        )

        assertThat(result1.exitCode).isEqualTo(ExitCode.OK)
        assertThat(result2.exitCode).isEqualTo(ExitCode.OK)
        val noArgs = result2.invokeBridge("callNoArgs")
        assertThat(noArgs).isEqualTo("User{}")
        val oneArgs = result2.invokeBridge("callOneArgs")
        assertThat(oneArgs).isEqualTo("User{id=null}")
        val allArgs = result2.invokeBridge("callAllArgs")
        assertThat(allArgs).isEqualTo("User{id=null, username=jack, age=10, birthday=${LocalDate.MIN}}")
    }

    @Test
    public fun `multi property generate test`() {
        val (result1, result2) = twiceCompile(
            SourceFile.kotlin(
                "source.kt", """
            import org.ktorm.entity.Entity
            import org.ktorm.ksp.api.Table

            @Table
            public interface User: Entity<User> {
                var arg1: String
                var arg2: String
                var arg3: String
                var arg4: String
                var arg5: String
                var arg6: String
                var arg7: String
                var arg8: String
                var arg9: String
                var arg10: String
                var arg11: String
                var arg12: String
                var arg13: String
                var arg14: String
                var arg15: String
                var arg16: String
                var arg17: String
                var arg18: String
                var arg19: String
                var arg20: String
                var arg21: String
                var arg22: String
                var arg23: String
                var arg24: String
                var arg25: String
                var arg26: String
                var arg27: String
                var arg28: String
                var arg29: String
                var arg30: String
                var arg31: String
                var arg32: String
                var arg33: String
            }


            object TestBridge {
                public fun callNoArgs(): String {
                    return User().toString()
                }

                public fun callOneArgs(): String {
                    return User(arg1 = "arg1").toString()
                }

                public fun callAllArgs(): String {
                    return User(
                        arg1 = "arg1",
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
                        arg33 = "arg33",
                    ).toString()
                }
            }
        """
            )
        )

        assertThat(result1.exitCode).isEqualTo(ExitCode.OK)
        assertThat(result2.exitCode).isEqualTo(ExitCode.OK)
        val noArgs = result2.invokeBridge("callNoArgs")
        assertThat(noArgs).isEqualTo("User{}")
        val oneArgs = result2.invokeBridge("callOneArgs")
        assertThat(oneArgs).isEqualTo("User{arg1=arg1}")
        val allArgs = result2.invokeBridge("callAllArgs")
        assertThat(allArgs).isEqualTo(
            "User{arg1=arg1, arg2=arg2, arg3=arg3, arg4=arg4, arg5=arg5, arg6=arg6, " +
                    "arg7=arg7, arg8=arg8, arg9=arg9, arg10=arg10, arg11=arg11, arg12=arg12, arg13=arg13, arg14=arg14," +
                    " arg15=arg15, arg16=arg16, arg17=arg17, arg18=arg18, arg19=arg19, arg20=arg20, arg21=arg21," +
                    " arg22=arg22, arg23=arg23, arg24=arg24, arg25=arg25, arg26=arg26, arg27=arg27, arg28=arg28," +
                    " arg29=arg29, arg30=arg30, arg31=arg31, arg32=arg32, arg33=arg33}"
        )
    }

}