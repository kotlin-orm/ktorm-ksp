package org.ktorm.ksp.compiler.test

import com.tschuchort.compiletesting.*
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.ktorm.ksp.compiler.KtormProcessorProvider
import org.ktorm.schema.BaseTable
import java.io.File

public class KtormKspTest {

    @Rule
    @JvmField
    public val temporaryFolder: TemporaryFolder = TemporaryFolder()

    @Test
    public fun dataClassEntity() {
        val result = compile(
            SourceFile.kotlin(
                "source.kt",
                """
                import org.ktorm.ksp.api.PrimaryKey
                import org.ktorm.ksp.api.Table
                
                @Table
                data class User(
                    @PrimaryKey
                    var id: Int? = null,
                    var username: String,
                    var age: Int = 0
                )
                """
            )
        )
        assertThat(result.exitCode).isEqualTo(ExitCode.OK)
    }

    @Test
    public fun tableAnnotation() {
        val (result1, result2) = twiceCompile(
            SourceFile.kotlin(
                "source.kt",
                """
                import org.ktorm.ksp.api.PrimaryKey
                import org.ktorm.ksp.api.Table
                
                @Table(tableName = "t_user","UserTable","t_user_alias","catalog","schema", ["age"])
                data class User(
                    @PrimaryKey
                    var id: Int,
                    var username: String,
                ) {
                    var age: Int = 10
                }
                """,
            )
        )
        assertThat(result1.exitCode).isEqualTo(ExitCode.OK)
        assertThat(result2.exitCode).isEqualTo(ExitCode.OK)
        val baseTable = result2.getBaseTable("UserTable")
        assertThat(baseTable.tableName).isEqualTo("t_user")
        assertThat(baseTable.alias).isEqualTo("t_user_alias")
        assertThat(baseTable.catalog).isEqualTo("catalog")
        assertThat(baseTable.columns.map { it.name }.toSet()).isEqualTo(setOf("id", "username"))
    }

    @Test
    public fun `Data class constructor with default parameters column`() {
        val (result1, result2) = twiceCompile(
            SourceFile.kotlin(
                "source.kt",
                """
                import org.ktorm.ksp.api.PrimaryKey
                import org.ktorm.ksp.api.Table
                
                @Table
                data class User(
                    @PrimaryKey
                    var id: Int,
                    var username: String = "lucy",
                )
                """,
            )
        ) {
            // use reflection create instance
            assertThat(it).contains("object Users", "constructor.callBy(parameterMap)")
        }
        assertThat(result1.exitCode).isEqualTo(ExitCode.OK)
        assertThat(result2.exitCode).isEqualTo(ExitCode.OK)
        result2.getBaseTable("Users")
    }

    @Test
    public fun `Data class constructor with parameters not column`() {
        val result = compile(
            SourceFile.kotlin(
                "source.kt",
                """
                import org.ktorm.ksp.api.Ignore
                import org.ktorm.ksp.api.PrimaryKey
                import org.ktorm.ksp.api.Table
                
                @Table
                data class User(
                    @PrimaryKey
                    var id: Int,
                    @Ignore
                    var username: String,
                    var age: Int
                )
                """,
            )
        )
        assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains("Construct parameter not exists in tableDefinition")
    }

    @Test
    public fun `ColumnDefinition Does not contain isReference parameters`() {
        val (result1, result2) = twiceCompile(
            SourceFile.kotlin(
                "source.kt",
                """
                import org.ktorm.ksp.api.*
                import org.ktorm.schema.varchar
                import org.ktorm.schema.BaseTable
                import kotlin.reflect.KClass
                
                @Table
                data class User(
                    @PrimaryKey
                    var id: Int,
                    @Column(columnName = "c_username", propertyName = "p_username", converter = MyStringConverter::class)
                    var username: String,
                    var age: Int
                )

                object MyStringConverter: SingleTypeConverter<String> {
                    public override fun convert(table: BaseTable<*>, columnName: String, propertyType: KClass<String>): org.ktorm.schema.Column<String> {
                        return with(table) {
                            varchar(columnName).transform({it.uppercase()},{it.lowercase()})
                        }
                    }
                }
                """,
            )
        ) {
            it.contains("""MyStringConverter.convert(this,"c_username",String::class)""")
            it.contains("""val p_username: Column<String>""")
        }
        assertThat(result1.exitCode).isEqualTo(ExitCode.OK)
        assertThat(result2.exitCode).isEqualTo(ExitCode.OK)
        val baseTable = result2.getBaseTable("Users")
        assertThat(baseTable.columns.any { it.name == "c_username" })
    }


    @Test
    public fun `column reference`() {
        val (result1, result2) = twiceCompile(
            SourceFile.kotlin(
                "source.kt",
                """
                import org.ktorm.ksp.api.*
                import org.ktorm.entity.Entity 
                import org.ktorm.schema.varchar
                import org.ktorm.schema.BaseTable
                import kotlin.reflect.KClass
                
                @Table
                interface User: Entity<User> {
                    @PrimaryKey
                    var id: Int
                    var username: String
                    var age: Int
                    @Column(isReferences = true)
                    var school: School
                }

                @Table
                interface School: Entity<School> {
                    @PrimaryKey
                    var id: Int
                    var schoolName: String
                }
                """,
            )
        ) {
            println(it)
        }
        assertThat(result1.exitCode).isEqualTo(ExitCode.OK)
        assertThat(result2.exitCode).isEqualTo(ExitCode.OK)
        val baseTable = result2.getBaseTable("Users")
        val school = baseTable.columns.firstOrNull { it.name == "school" }
        assertThat(school).isNotNull
        assertThat(school!!.referenceTable).isNotNull
        val referenceTable = school.referenceTable
        assertThat(referenceTable!!.tableName).isEqualTo("School")
    }

    @Test
    public fun `column reference is data class`() {
        val (result,_) = twiceCompile(
            SourceFile.kotlin(
                "source.kt",
                """
                import org.ktorm.ksp.api.*
                import org.ktorm.entity.Entity
                import org.ktorm.schema.varchar
                import org.ktorm.schema.BaseTable
                import kotlin.reflect.KClass
                
                @Table
                interface User: Entity<User> {
                    @PrimaryKey
                    var id: Int
                    var username: String
                    var age: Int
                    @Column(isReferences = true)
                    var school: School
                }

                @Table
                data class School(
                    @PrimaryKey
                    var id: Int,
                    var schoolName: String
                )
                """,
            )
        ) {
            println(it)
        }
        assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains("References column must be interface entity type")
    }

    @Test
    public fun `column reference not an entity type`() {
        val result = compile(
            SourceFile.kotlin(
                "source.kt",
                """
                import org.ktorm.ksp.api.*
                import org.ktorm.entity.Entity
                import org.ktorm.schema.varchar
                import org.ktorm.schema.BaseTable
                import kotlin.reflect.KClass
                
                @Table
                interface User: Entity<User> {
                    @PrimaryKey
                    var id: Int
                    var username: String
                    var age: Int
                    @Column(isReferences = true)
                    var school: School
                }

                data class School(
                    var id: Int,
                    var schoolName: String
                )
                """,
            )
        )
        assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains("is not an entity type")
    }

    @Test
    public fun interfaceEntity() {
        val result = compile(
            SourceFile.kotlin(
                "source.kt",
                """
                import org.ktorm.entity.Entity
                import org.ktorm.ksp.api.PrimaryKey
                import org.ktorm.ksp.api.Table
                @Table
                interface User: Entity<User> {
                    @PrimaryKey
                    var id: Int
                    var username: String
                    var age: Int 
                }
                """
            )
        )
        assertThat(result.exitCode).isEqualTo(ExitCode.OK)
    }

    @Test
    public fun `column reference in data class`() {
        val result = compile(
            SourceFile.kotlin(
                "source.kt",
                """
                import org.ktorm.ksp.api.*
                import org.ktorm.schema.varchar
                import org.ktorm.schema.BaseTable
                import kotlin.reflect.KClass
                
                @Table
                data class User(
                    @PrimaryKey
                    var id: Int,
                    var username: String,
                    var age: Int,
                    @Column(isReferences = true)
                    var school: School
                )

                @Table
                interface School: Entity<School> {
                    @PrimaryKey
                    var id: Int
                    var schoolName: String
                }
                """,
            )
        )
        assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains("References Column are only allowed for interface entity type")
    }

    private fun createCompiler(vararg sourceFiles: SourceFile, useKsp: Boolean = true): KotlinCompilation {
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

    private inline fun twiceCompile(
        vararg sourceFiles: SourceFile,
        sourceFileBlock: (String) -> Unit = {}
    ): Pair<KotlinCompilation.Result, KotlinCompilation.Result> {
        val compiler1 = createCompiler(*sourceFiles)
        val result1 = compiler1.compile()
        val result2 =
            createCompiler(*(compiler1.kspGeneratedSourceFiles + sourceFiles).toTypedArray(), useKsp = false).compile()
        compiler1.kspGeneratedFiles.forEach { sourceFileBlock(it.readText()) }
        return result1 to result2
    }

    private fun compile(
        vararg sourceFiles: SourceFile,
        printKspGenerateFile: Boolean = false
    ): KotlinCompilation.Result {
        val compilation = createCompiler(*sourceFiles)
        val result = compilation.compile()
        if (printKspGenerateFile) {
            compilation.kspSourcesDir.walkTopDown()
                .filter { it.extension == "kt" }
                .forEach { println(it.readText()) }
        }
        return result
    }

    private val KotlinCompilation.kspGeneratedSourceFiles: List<SourceFile>
        get() = kspSourcesDir.resolve("kotlin")
            .walk()
            .filter { it.isFile }
            .map { SourceFile.fromPath(it.absoluteFile) }
            .toList()


    private val KotlinCompilation.kspGeneratedFiles: List<File>
        get() = kspSourcesDir.resolve("kotlin")
            .walk()
            .filter { it.isFile }
            .toList()

    private fun KotlinCompilation.Result.getBaseTable(className: String): BaseTable<*> {
        val clazz = classLoader.loadClass(className)
        assertThat(clazz).isNotNull
        val table = clazz.kotlin.objectInstance
        assertThat(table).isInstanceOf(BaseTable::class.java)
        return table as BaseTable<*>
    }
}