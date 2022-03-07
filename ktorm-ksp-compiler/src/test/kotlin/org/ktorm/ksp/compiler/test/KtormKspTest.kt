package org.ktorm.ksp.compiler.test

import com.tschuchort.compiletesting.*
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.ktorm.database.Database
import org.ktorm.entity.EntitySequence
import org.ktorm.entity.toList
import org.ktorm.ksp.compiler.KtormProcessorProvider
import org.ktorm.logging.ConsoleLogger
import org.ktorm.logging.LogLevel
import org.ktorm.schema.*
import java.io.File
import kotlin.reflect.full.functions

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
    public fun `data class constructor with default parameters column`() {
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
    public fun `data class constructor with parameters not column`() {
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
    public fun `columnDefinition Does not contain isReference parameters`() {
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
        val (result, _) = twiceCompile(
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

    @Test
    public fun `singleton single type converter in ktormKspConfig`() {
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
                    var username: Username,
                    var age: Int,
                    
                )
                
                data class Username(
                    val firstName:String,
                    val lastName:String
                )

                @KtormKspConfig(
                    singleTypeConverters = [UsernameConverter::class]
                )
                class KtormConfig

                object UsernameConverter: SingleTypeConverter<Username> {
                    public override fun convert(table: BaseTable<*>, columnName: String, propertyType: KClass<Username>): org.ktorm.schema.Column<Username> {
                        return with(table) {
                            varchar(columnName).transform({
                                val spilt = it.split("#")
                                Username(spilt[0],spilt[1])
                            },{
                                it.firstName +"#" + it.lastName
                            })
                        }
                    }
                }
                """,
            )
        ) {
            assertThat(it).contains("UsernameConverter.convert")
        }
        assertThat(result1.exitCode).isEqualTo(ExitCode.OK)
        assertThat(result2.exitCode).isEqualTo(ExitCode.OK)
        val baseTable = result2.getBaseTable("Users")
        val column = baseTable.columns.firstOrNull { it.name == "username" }
        assertThat(column).isNotNull
        assertThat(column!!.sqlType.typeCode).isEqualTo(VarcharSqlType.typeCode)
    }

    @Test
    public fun `non singleton single type converter in ktormKspConfig`() {
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
                    var username: Username,
                    var age: Int,
                    
                )
                
                data class Username(
                    val firstName:String,
                    val lastName:String
                )

                @KtormKspConfig(
                    singleTypeConverters = [UsernameConverter::class]
                )
                class KtormConfig

                class UsernameConverter: SingleTypeConverter<Username> {
                    public override fun convert(table: BaseTable<*>, columnName: String, propertyType: KClass<Username>): org.ktorm.schema.Column<Username> {
                        return with(table) {
                            varchar(columnName).transform({
                                val spilt = it.split("#")
                                Username(spilt[0],spilt[1])
                            },{
                                it.firstName +"#" + it.lastName
                            })
                        }
                    }
                }
                """,
            )
        )
        assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains("converter must be singleton")
    }

    @Test
    public fun `singleton enum converter in ktormKspConfig`() {
        val (result1, result2) = twiceCompile(
            SourceFile.kotlin(
                "source.kt",
                """
                import org.ktorm.ksp.api.*
                import org.ktorm.schema.varchar
                import org.ktorm.schema.BaseTable
                import org.ktorm.schema.Column
                import org.ktorm.schema.int
                import kotlin.reflect.KClass
                
                @Table
                data class User(
                    @PrimaryKey
                    var id: Int,
                    var username: String,
                    var age: Int,
                    var gender: Gender
                )
                
                enum class Gender {
                    MALE,
                    FEMALE
                }               

                @KtormKspConfig(
                    enumConverter = IntEnumConverter::class
                )
                class KtormConfig

                object IntEnumConverter: EnumConverter {
                    override fun <E : Enum<E>> convert(table: BaseTable<*>, columnName: String, propertyType: KClass<E>): Column<E>{
                        val values = propertyType.java.enumConstants
                        return with(table) {
                            int(columnName).transform( {values[it]} , {it.ordinal} )
                        }
                    }
                }
                """,
            )
        ) {
            assertThat(it).contains("IntEnumConverter.convert")
        }
        assertThat(result1.exitCode).isEqualTo(ExitCode.OK)
        assertThat(result2.exitCode).isEqualTo(ExitCode.OK)
        val baseTable = result2.getBaseTable("Users")
        val column = baseTable.columns.firstOrNull { it.name == "gender" }
        assertThat(column).isNotNull
        assertThat(column!!.sqlType.typeCode).isEqualTo(IntSqlType.typeCode)
    }

    @Test
    public fun `non singleton enum converter in ktormKspConfig`() {
        val result = compile(
            SourceFile.kotlin(
                "source.kt",
                """
                import org.ktorm.ksp.api.*
                import org.ktorm.schema.varchar
                import org.ktorm.schema.BaseTable
                import org.ktorm.schema.Column
                import org.ktorm.schema.int
                import kotlin.reflect.KClass
                
                @Table
                data class User(
                    @PrimaryKey
                    var id: Int,
                    var username: String,
                    var age: Int,
                    var gender: Gender
                )
                
                enum class Gender {
                    MALE,
                    FEMALE
                }               

                @KtormKspConfig(
                    enumConverter = IntEnumConverter::class
                )
                class KtormConfig

                class IntEnumConverter: EnumConverter {
                    override fun <E : Enum<E>> convert(table: BaseTable<*>, columnName: String, propertyType: KClass<E>): Column<E>{
                        val values = propertyType.java.enumConstants
                        return with(table) {
                            int(columnName).transform( {values[it]} , {it.ordinal} )
                        }
                    }
                }
                """,
            )
        )
        assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains("converter must be singleton")
    }

    @Test
    public fun lowerCaseCamelCaseToUnderscoresNamingStrategy() {
        val (result1, result2) = twiceCompile(
            SourceFile.kotlin(
                "source.kt",
                """
                import org.ktorm.ksp.api.*
                import org.ktorm.schema.varchar
                import org.ktorm.schema.BaseTable
                import org.ktorm.schema.Column
                import org.ktorm.schema.int
                import kotlin.reflect.KClass
                
                @Table
                data class UserProfile(
                    @PrimaryKey
                    var id: Int,
                    var publicEmail: String,
                    var profilePicture: Int
                )
               
                @KtormKspConfig(
                    namingStrategy = LowerCaseCamelCaseToUnderscoresNamingStrategy::class
                )
                class KtormConfig
                
                """,
            )
        )
        assertThat(result1.exitCode).isEqualTo(ExitCode.OK)
        assertThat(result2.exitCode).isEqualTo(ExitCode.OK)
        val baseTable = result2.getBaseTable("UserProfiles")
        assertThat(baseTable.tableName).isEqualTo("user_profile")
        assertThat(baseTable.columns.map { it.name }.toSet()).isEqualTo(setOf("id", "public_email", "profile_picture"))
    }

    @Test
    public fun `custom singleton NamingStrategy`() {
        val (result1, result2) = twiceCompile(
            SourceFile.kotlin(
                "source.kt",
                """
                import org.ktorm.ksp.api.*
                import org.ktorm.schema.varchar
                import org.ktorm.schema.BaseTable
                import org.ktorm.schema.Column
                import org.ktorm.schema.int
                import kotlin.reflect.KClass
                
                @Table
                data class UserProfile(
                    @PrimaryKey
                    var id: Int,
                    var publicEmail: String,
                    var profilePicture: Int
                )
               
                @KtormKspConfig(
                    namingStrategy = CustomNamingStrategy::class
                )
                class KtormConfig

                object CustomNamingStrategy: NamingStrategy {
                    override fun toTableName(entityClassName: String): String {
                        return "t_${'$'}entityClassName"
                    }
                    override fun toColumnName(propertyName: String): String {
                        return "c_${'$'}propertyName"
                    }
                }
                
                """,
            )
        )
        assertThat(result1.exitCode).isEqualTo(ExitCode.OK)
        assertThat(result2.exitCode).isEqualTo(ExitCode.OK)
        val baseTable = result2.getBaseTable("UserProfiles")
        assertThat(baseTable.tableName).isEqualTo("t_UserProfile")
        assertThat(baseTable.columns.map { it.name }.toSet()).isEqualTo(
            setOf(
                "c_id",
                "c_publicEmail",
                "c_profilePicture"
            )
        )
    }


    @Test
    public fun `custom non singleton NamingStrategy`() {
        val result = compile(
            SourceFile.kotlin(
                "source.kt",
                """
                import org.ktorm.ksp.api.*
                import org.ktorm.schema.varchar
                import org.ktorm.schema.BaseTable
                import org.ktorm.schema.Column
                import org.ktorm.schema.int
                import kotlin.reflect.KClass
                
                @Table
                data class UserProfile(
                    @PrimaryKey
                    var id: Int,
                    var publicEmail: String,
                    var profilePicture: Int
                )
               
                @KtormKspConfig(
                    namingStrategy = CustomNamingStrategy::class
                )
                class KtormConfig

                class CustomNamingStrategy: NamingStrategy {
                    override fun toTableName(entityClassName: String): String {
                        return "t_${'$'}entityClassName"
                    }
                    override fun toColumnName(propertyName: String): String {
                        return "c_${'$'}propertyName"
                    }
                }
                
                """,
            )
        )
        assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains("namingStrategy must be singleton")
    }

    @Test
    public fun `disable allowReflectionCreateClassEntity`() {
        val (result1, result2) = twiceCompile(
            SourceFile.kotlin(
                "source.kt",
                """
                import org.ktorm.ksp.api.*
                
                @Table
                data class User(
                    @PrimaryKey
                    var id: Int,
                    var username: String = "",
                )
                
                @KtormKspConfig(
                    allowReflectionCreateClassEntity = false
                )
                class KtormConfig
                """,
            )
        ) {
            assertThat(it).doesNotContain("constructor.callBy(parameterMap)")
        }
        assertThat(result1.exitCode).isEqualTo(ExitCode.OK)
        assertThat(result2.exitCode).isEqualTo(ExitCode.OK)
    }

    @Test
    public fun `enable sequenceOf for extensionGenerator`() {
        val (result1, result2) = twiceCompile(
            SourceFile.kotlin(
                "source.kt",
                """
                import org.ktorm.ksp.api.*
                    
                @Table
                data class User(
                    @PrimaryKey
                    var id: Int?,
                    var username: String,
                    var age: Int,
                )
                
                @KtormKspConfig(
                    extension = ExtensionGenerator(
                        enableSequenceOf = true, 
                        enableClassEntitySequenceAddFun = false,
                        enableClassEntitySequenceUpdateFun = false
                    )
                )
                class KtormConfig
                """,
            )
        ) {
            val sequenceLine = it.lineSequence().filter { str -> str.contains("EntitySequence<User, Users>") }.toList()
            assertThat(sequenceLine.size).isEqualTo(1)
            assertThat(sequenceLine.first()).contains("val Database.users: EntitySequence<User, Users>")
        }
        assertThat(result1.exitCode).isEqualTo(ExitCode.OK)
        assertThat(result2.exitCode).isEqualTo(ExitCode.OK)
    }

    @Test
    public fun `enable sequence add function for extensionGenerator`() {
        val (result1, result2) = twiceCompile(
            SourceFile.kotlin(
                "source.kt",
                """
                import org.ktorm.ksp.api.*
                    
                @Table
                data class User(
                    @PrimaryKey
                    var id: Int?,
                    var username: String,
                    var age: Int,
                )
                
                @KtormKspConfig(
                    extension = ExtensionGenerator(
                        enableSequenceOf = false, 
                        enableClassEntitySequenceAddFun = true,
                        enableClassEntitySequenceUpdateFun = false
                    )
                )
                class KtormConfig
                """,
            )
        ) {
            val sequenceLine = it.lineSequence().filter { str -> str.contains("EntitySequence<User, Users>") }.toList()
            assertThat(sequenceLine.size).isEqualTo(1)
            assertThat(sequenceLine.first()).contains("EntitySequence<User, Users>.add")
        }
        assertThat(result1.exitCode).isEqualTo(ExitCode.OK)
        assertThat(result2.exitCode).isEqualTo(ExitCode.OK)
    }

    @Test
    public fun `enable sequence update function for extensionGenerator`() {
        val (result1, result2) = twiceCompile(
            SourceFile.kotlin(
                "source.kt",
                """
                import org.ktorm.ksp.api.*
                    
                @Table
                data class User(
                    @PrimaryKey
                    var id: Int?,
                    var username: String,
                    var age: Int,
                )
                
                @KtormKspConfig(
                    extension = ExtensionGenerator(
                        enableSequenceOf = false, 
                        enableClassEntitySequenceAddFun = false,
                        enableClassEntitySequenceUpdateFun = true
                    )
                )
                class KtormConfig
                """,
            )
        ) {
            val sequenceLine = it.lineSequence().filter { str -> str.contains("EntitySequence<User, Users>") }.toList()
            assertThat(sequenceLine.size).isEqualTo(1)
            assertThat(sequenceLine.first()).contains("EntitySequence<User, Users>.update")
        }
        assertThat(result1.exitCode).isEqualTo(ExitCode.OK)
        assertThat(result2.exitCode).isEqualTo(ExitCode.OK)
    }

    @Test
    public fun `sequenceOf function`() {
        val (result1, result2) = twiceCompile(
            SourceFile.kotlin(
                "source.kt",
                """
                import org.ktorm.database.Database
                import org.ktorm.entity.Entity
                import org.ktorm.entity.EntitySequence
                import org.ktorm.ksp.api.*
                import java.time.LocalDate
                    
                @Table
                data class User(
                    @PrimaryKey
                    var id: Int,
                    var username: String,
                    var age: Int
                )

                @Table
                interface Employee: Entity<Employee> {
                    @PrimaryKey
                    var id: Int
                    var name: String    
                    var job: String
                    var hireDate: LocalDate
                }
                
                @KtormKspConfig(namingStrategy = LowerCaseCamelCaseToUnderscoresNamingStrategy::class)
                class KtormConfig

                object TestBridge {
                    fun getUsers(database:Database): EntitySequence<User,Users> {
                        return database.users
                    }
                    fun getEmployees(database: Database): EntitySequence<Employee,Employees> {
                        return database.employees
                    }
                }
                """,
            )
        )
        assertThat(result1.exitCode).isEqualTo(ExitCode.OK)
        assertThat(result2.exitCode).isEqualTo(ExitCode.OK)
        val bridgeClass = result2.classLoader.loadClass("TestBridge")
        val bridge = bridgeClass.kotlin.objectInstance
        useDatabase { database ->
            val users =
                bridgeClass.kotlin.functions.first { it.name == "getUsers" }
                    .call(bridge, database) as EntitySequence<*, *>
            assertThat(users.sourceTable.tableName).isEqualTo("user")
            val toList = users.toList()
            assertThat(toList.toString()).isEqualTo("[User(id=1, username=jack, age=20), User(id=2, username=lucy, age=22), User(id=3, username=mike, age=22)]")
            val employees = bridgeClass.kotlin.functions.first { it.name == "getEmployees" }
                .call(bridge, database) as EntitySequence<*, *>
            assertThat(employees.sourceTable.tableName).isEqualTo("employee")
            assertThat(
                employees.toList().toString()
            ).isEqualTo("[Employee{id=1, name=vince, job=engineer, hireDate=2018-01-01}, Employee{id=2, name=marry, job=trainee, hireDate=2019-01-01}, Employee{id=3, name=tom, job=director, hireDate=2018-01-01}, Employee{id=4, name=penny, job=assistant, hireDate=2019-01-01}]")
        }
    }

    @Test
    public fun `sequence add function`() {
        val (result1, result2) = twiceCompile(
            SourceFile.kotlin(
                "source.kt",
                """
                import org.ktorm.database.Database
                import org.ktorm.entity.Entity
                import org.ktorm.entity.EntitySequence
                import org.ktorm.ksp.api.*
                import java.time.LocalDate
                    
                @Table
                data class User(
                    @PrimaryKey
                    var id: Int?,
                    var username: String,
                    var age: Int,
                )

                @KtormKspConfig(namingStrategy = LowerCaseCamelCaseToUnderscoresNamingStrategy::class)
                class KtormConfig

                object TestBridge {
                    fun getUsers(database:Database): EntitySequence<User,Users> {
                        return database.users
                    }
                    fun addUser(database: Database) {
                        database.users.add(User(null,"test",100))
                    }
                }
                """,
            )
        )
        assertThat(result1.exitCode).isEqualTo(ExitCode.OK)
        assertThat(result2.exitCode).isEqualTo(ExitCode.OK)
        val bridgeClass = result2.classLoader.loadClass("TestBridge")
        val bridge = bridgeClass.kotlin.objectInstance
        useDatabase { database ->
            var users =
                bridgeClass.kotlin.functions.first { it.name == "getUsers" }
                    .call(bridge, database) as EntitySequence<*, *>
            assertThat(users.sourceTable.tableName).isEqualTo("user")
            assertThat(
                users.toList().toString()
            ).isEqualTo("[User(id=1, username=jack, age=20), User(id=2, username=lucy, age=22), User(id=3, username=mike, age=22)]")

            bridgeClass.kotlin.functions.first { it.name == "addUser" }.call(bridge, database)
            users =
                bridgeClass.kotlin.functions.first { it.name == "getUsers" }
                    .call(bridge, database) as EntitySequence<*, *>
            assertThat(
                users.toList().toString()
            ).isEqualTo("[User(id=1, username=jack, age=20), User(id=2, username=lucy, age=22), User(id=3, username=mike, age=22), User(id=4, username=test, age=100)]")
        }
    }

    @Test
    public fun `default column initializer`() {
        val (result1, result2) = twiceCompile(
            SourceFile.kotlin(
                "source.kt",
                """
                import org.ktorm.database.Database
                import org.ktorm.entity.Entity
                import org.ktorm.entity.EntitySequence
                import org.ktorm.ksp.api.*
                import java.math.BigDecimal
                import java.sql.Time
                import java.sql.Date
                import java.sql.Timestamp
                import java.time.*
                import java.util.UUID
                    
                @Table
                @Suppress("ArrayInDataClass")
                data class User(
                    val int: Int,
                    val string: String,
                    val boolean: Boolean,
                    val long: Long,
                    val short: Short,
                    val double: Double,
                    val float: Float,
                    val bigDecimal: BigDecimal,
                    val date: Date,
                    val time: Time,
                    val timestamp: Timestamp,
                    val localDateTime: LocalDateTime,
                    val localDate: LocalDate,
                    val localTime: LocalTime,
                    val monthDay: MonthDay,
                    val yearMonth: YearMonth,
                    val year: Year,
                    val instant: Instant,
                    val uuid: UUID,
                    val byteArray: ByteArray,
                    val gender: Gender
                )

                enum class Gender {
                    MALE,
                    FEMALE
                }
                """,
            )
        )
        assertThat(result1.exitCode).isEqualTo(ExitCode.OK)
        assertThat(result2.exitCode).isEqualTo(ExitCode.OK)
        val table = result2.getBaseTable("Users")
        table.columns.forEach {
            when (it.name) {
                "int" -> assertThat(it.sqlType).isEqualTo(IntSqlType)
                "string" -> assertThat(it.sqlType).isEqualTo(VarcharSqlType)
                "boolean" -> assertThat(it.sqlType).isEqualTo(BooleanSqlType)
                "long" -> assertThat(it.sqlType).isEqualTo(LongSqlType)
                "short" -> assertThat(it.sqlType).isEqualTo(ShortSqlType)
                "double" -> assertThat(it.sqlType).isEqualTo(DoubleSqlType)
                "float" -> assertThat(it.sqlType).isEqualTo(FloatSqlType)
                "bigDecimal" -> assertThat(it.sqlType).isEqualTo(DecimalSqlType)
                "date" -> assertThat(it.sqlType).isEqualTo(DateSqlType)
                "time" -> assertThat(it.sqlType).isEqualTo(TimeSqlType)
                "timestamp" -> assertThat(it.sqlType).isEqualTo(TimestampSqlType)
                "localDateTime" -> assertThat(it.sqlType).isEqualTo(LocalDateTimeSqlType)
                "localDate" -> assertThat(it.sqlType).isEqualTo(LocalDateSqlType)
                "localTime" -> assertThat(it.sqlType).isEqualTo(LocalTimeSqlType)
                "monthDay" -> assertThat(it.sqlType).isEqualTo(MonthDaySqlType)
                "yearMonth" -> assertThat(it.sqlType).isEqualTo(YearMonthSqlType)
                "year" -> assertThat(it.sqlType).isEqualTo(YearSqlType)
                "instant" -> assertThat(it.sqlType).isEqualTo(InstantSqlType)
                "uuid" -> assertThat(it.sqlType).isEqualTo(UuidSqlType)
                "byteArray" -> assertThat(it.sqlType).isEqualTo(BytesSqlType)
                "gender" -> assertThat(it.sqlType).isInstanceOf(EnumSqlType::class.java)
            }
        }
    }


    @Test
    public fun `sequence update function`() {
        val (result1, result2) = twiceCompile(
            SourceFile.kotlin(
                "source.kt",
                """
                import org.ktorm.database.Database
                import org.ktorm.entity.Entity
                import org.ktorm.entity.EntitySequence
                import org.ktorm.entity.toList
                import org.ktorm.ksp.api.*
                import org.ktorm.dsl.eq
                import org.ktorm.entity.filter
                import java.time.LocalDate
                    
                @Table
                data class User(
                    @PrimaryKey
                    var id: Int?,
                    var username: String,
                    var age: Int,
                )

                @KtormKspConfig(namingStrategy = LowerCaseCamelCaseToUnderscoresNamingStrategy::class)
                class KtormConfig

                object TestBridge {
                    @Suppress("MemberVisibilityCanBePrivate")
                    fun getUsers(database:Database): EntitySequence<User,Users> {
                        return database.users
                    }
                    fun updateUser(database: Database) {
                        val user = getUsers(database).filter { it.id eq 1  }.toList().first()
                        user.username = "tom"
                        getUsers(database).update(user)
                    }
                }
                """,
            )
        )
        assertThat(result1.exitCode).isEqualTo(ExitCode.OK)
        assertThat(result2.exitCode).isEqualTo(ExitCode.OK)
        val bridgeClass = result2.classLoader.loadClass("TestBridge")
        val bridge = bridgeClass.kotlin.objectInstance
        useDatabase { database ->
            var users =
                bridgeClass.kotlin.functions.first { it.name == "getUsers" }
                    .call(bridge, database) as EntitySequence<*, *>
            assertThat(users.sourceTable.tableName).isEqualTo("user")
            assertThat(
                users.toList().toString()
            ).isEqualTo("[User(id=1, username=jack, age=20), User(id=2, username=lucy, age=22), User(id=3, username=mike, age=22)]")

            bridgeClass.kotlin.functions.first { it.name == "updateUser" }.call(bridge, database)
            users = bridgeClass.kotlin.functions.first { it.name == "getUsers" }
                .call(bridge, database) as EntitySequence<*, *>
            assertThat(
                users.toList().toString()
            ).isEqualTo("[User(id=1, username=tom, age=20), User(id=2, username=lucy, age=22), User(id=3, username=mike, age=22)]")
        }
    }

    @Test
    public fun `multi KtormKspConfig annotation`() {
        val result = compile(
            SourceFile.kotlin(
                "source.kt",
                """
                import org.ktorm.ksp.api.KtormKspConfig
                    
                @KtormKspConfig
                class KtormConfig1

                @KtormKspConfig
                class KtormConfig2
                """,
            )
        )
        assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains("@KtormKspConfig can only be added to a class")
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

    private fun KotlinCompilation.Result.getTable(className: String): BaseTable<*> {
        val clazz = classLoader.loadClass(className)
        assertThat(clazz).isNotNull
        val table = clazz.kotlin.objectInstance
        assertThat(table).isInstanceOf(Table::class.java)
        return table as Table<*>
    }

    private inline fun useDatabase(action: (Database) -> Unit) {
        Database.connect(
            url = "jdbc:h2:mem:ktorm;",
            driver = "org.h2.Driver",
            logger = ConsoleLogger(threshold = LogLevel.TRACE),
            alwaysQuoteIdentifiers = true
        ).apply {
            this.useConnection {
                it.createStatement().use { statement ->
                    val sql =
                        KtormKspTest::class.java.classLoader.getResourceAsStream("init-data.sql")!!.bufferedReader()
                            .readText()
                    statement.executeUpdate(sql)
                }
                action(this)
            }
        }
    }
}