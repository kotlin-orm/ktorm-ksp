<p align="center">
    <img src="https://raw.githubusercontent.com/kotlin-orm/ktorm-docs/master/source/images/logo-full.png" alt="Ktorm" width="300" />
</p>
<p align="center">
    <a href="https://github.com/kotlin-orm/ktorm-ksp/actions/workflows/build.yml">
        <img src="https://github.com/kotlin-orm/ktorm-ksp/actions/workflows/build.yml/badge.svg" alt="Build Status" />
    </a>
    <a href="https://search.maven.org/search?q=g:%22org.ktorm%22">
        <img src="https://img.shields.io/maven-central/v/org.ktorm/ktorm-ksp-api.svg?label=Maven%20Central" alt="Maven Central" />
    </a>
    <a href="LICENSE">
        <img src="https://img.shields.io/badge/license-Apache%202-blue.svg?maxAge=2592000" alt="Apache License 2" />
    </a>
</p>

:us: English | :cn: [简体中文](README_cn.md)

# What's ktorm-ksp?

[Ktorm](https://github.com/kotlin-orm/ktorm) KSP extension to help generate boilerplate code. It can automatically
generate Table objects through entity classes, while making entities defined by data classes easier to use, and supports
custom extension code generation logic.

- PS： The project is still in development

# Feature

- Just write the entity class and automatically generate the corresponding Table object. Support classes defined based
  on the Entity interface, as well as entities defined by ordinary class or data class

- Better support for class entity classes, the default implementation of the doCreateEntity method, and the add and
  update method of the entity sequence

- Extensible code generation logic. Through the SPI mechanism, you only need to implement the specified interface, and
  you can write your own automatically generated logic.

custom entity ▼

```kotlin
@Table
public data class Student(
    @PrimaryKey
    public var id: Int?,
    public var name: String,
    public var age: Int
)
```

Auto generate code ▼

```kotlin
public open class Students(
    alias: String? = null,
) : BaseTable<Student>(tableName = "Student", alias = alias, entityClass = Student::class) {
    public val id: Column<Int> = int("id").primaryKey()

    public val name: Column<String> = varchar("name")

    public val age: Column<Int> = int("age")

    public override fun aliased(alias: String): Students = Students(alias)

    public override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean): Student {
        return Student(
            id = row[this.id],
            name = row[this.name]!!,
            age = row[this.age]!!,
        )
    }

    public companion object : Students()
}

public fun EntitySequence<Student, Students>.add(entity: Student): Int { /*Ignore code*/
}

public fun EntitySequence<Student, Students>.update(entity: Student): Int { /*Ignore code*/
}

public val Database.students: EntitySequence<Student, Students> get() = this.sequenceOf(Students)
```

> Why use class as a table type instead of an object singleton? Please refer to the documentation:  [Self-Joining-amp-Table-Aliases](https://www.ktorm.org/en/joining.html#Self-Joining-amp-Table-Aliases)


- [Quick Start](#quick-start)
- [Define Entities](#define-entities)
    - [Define Entities Of Any Kind Of Class](#define-entities-of-any-kind-of-class)
    - [Define Entities Based On The Entity Interface](#define-entities-based-on-the-entity-interface)
    - [Define Table Schema](#define-table-schema)
    - [Define Primary Key](#define-primary-key)
    - [Define Table Column](#define-table-column)
    - [Ignore The Specified Properties](#ignore-the-specified-properties)
- [Global Configuration](#global-configuration)
- [Naming Style](#naming-style)
    - [Single Naming Configuration](#type-converter)
    - [Global Naming Configuration](#global-naming-configuration)
- [Type Converter](#type-converter)
    - [Use Type Converter in Column](#use-type-converter-in-column)
    - [Use Type Converter in Global Configuration](#use-type-converter-in-global-configuration)
- [Function And Property Generator](#function-and-property-generator)
    - [Steps To Customize The Generator](#steps-to-customize-the-generator)
    - [Available Generator Extensions](#available-generator-extensions)

### Quick Start

Add a dependency to ```build.gradle``` or ```pom.xml``` file:

```groovy
// groovy dsl gradle 
plugins {
    id 'com.google.devtools.ksp' version '1.6.21-1.0.5'
}

dependencies {
    implementation 'org.ktorm:ktorm-ksp-api:${ktorm-ksp.version}'
    ksp 'org.ktorm:ktorm-ksp-compiler:${ktorm-ksp.version}'
}
```

```kotlin
// kotlin dsl gradle
plugins {
    id("com.google.devtools.ksp").version("1.6.21-1.0.5")
}

dependencies {
    implementation("org.ktorm:ktorm-ksp-api:${ktorm-ksp.version}")
    ksp("org.ktorm:ktorm-ksp-compiler:${ktorm-ksp.version}")
}
```

```xml
<!-- maven -->
<project>
    <build>
        <plugins>
            <plugin>
                <groupId>org.jetbrains.kotlin</groupId>
                <artifactId>kotlin-maven-plugin</artifactId>
                <version>${kotlin.version}</version>
                <configuration>
                    <compilerPlugins>
                        <compilerPlugin>ksp</compilerPlugin>
                    </compilerPlugins>
                    <sourceDirs>
                        <sourceDir>src/main/kotlin</sourceDir>
                        <sourceDir>target/generated-sources/ksp</sourceDir>
                    </sourceDirs>
                </configuration>
                <dependencies>
                    <dependency>
                        <groupId>com.dyescape</groupId>
                        <artifactId>kotlin-maven-symbol-processing</artifactId>
                        <version>1.3</version>
                    </dependency>
                    <dependency>
                        <groupId>org.ktorm</groupId>
                        <artifactId>ktorm-ksp-compiler</artifactId>
                        <version>${ktorm-ksp.version}</version>
                    </dependency>
                </dependencies>
                <executions>
                    <execution>
                        <id>compile</id>
                        <phase>compile</phase>
                        <goals>
                            <goal>compile</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>

    <dependencies>
        <dependency>
            <groupId>org.ktorm</groupId>
            <artifactId>ktorm-ksp-api</artifactId>
            <version>${ktorm-ksp.version}</version>
        </dependency>
    </dependencies>
</project>
```

In order for idea to aware the generated code, you also need to add the following configuration to ```build.gradle```
(otherwise you will see some red line warnings). If you use maven, please ignore this step. Because the relevant
configuration has been added in the previous step.

```groovy
// Groovy DSL
kotlin {
    sourceSets {
        main.kotlin.srcDirs += 'build/generated/ksp/main/kotlin'
        test.kotlin.srcDirs += 'build/generated/ksp/test/kotlin'
    }
}
```

```kotlin
// Kotlin DSL
kotlin {
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
    sourceSets.test {
        kotlin.srcDir("build/generated/ksp/test/kotlin")
    }
}
```

How to get ksp to generate code?

- Gradle: build project, running application, execute ```gradle build``` command. will generate code in
  ```build/generated/ksp/main/kotlin``` directory
- Maven: execute ```mvn kotlin:compile``` will generate code in ```target/generated-sources/ksp``` directory

### Define Entities

#### Define Entities Of Any Kind Of Class

```kotlin
@Table
public data class Student(
    @PrimaryKey
    public var id: Int?,
    public var name: String,
    public var age: Int
)
```

Generate code：

```kotlin
public open class Students(
    alias: String? = null,
) : BaseTable<Student>(tableName = "Student", alias = alias, entityClass = Student::class) {
    // Ignore code
}

public fun EntitySequence<Student, Students>.add(entity: Student): Int { /*Ignore code*/
}
public fun EntitySequence<Student, Students>.update(entity: Student): Int { /*Ignore code*/
}
public val Database.students: EntitySequence<Student, Students> get() = this.sequenceOf(Students)
```

The extended properties of Entity Sequence are generated by default, eliminating the need to manually write boilerplate
code for creating entity sequences.

```kotlin
val users = database.users.toList()
```

#### Define Entities Based On The Entity Interface

```kotlin
@Table
public interface Student : Entity<Student> {
    @PrimaryKey
    public var id: Int?
    public var name: String
    public var age: Int
}
```

Generate code：

```kotlin
public open class Students(
    alias: String? = null,
) : Table<Student>(tableName = "Student", alias = alias, entityClass = Student::class) {
    // Ignore code
}

public val Database.students: EntitySequence<Student, Students> get() = this.sequenceOf(Students)
```

The difference from the table generated by the ```entity of any kind class``` is that the table inherits
the ```Table class``` instead of the
```BaseTable class```, so there is no need to implement the ```doCreateEntity``` method. Therefore, there is no need to
generate the
```add``` ```update``` extension method of ```EntitySequence``` (because it already exists)

```kotlin
val users = database.users.toList()
```

#### Define Table Schema

Adding the @Table annotation to the entity class will automatically generate the corresponding Table class.

The parameters of @Table are as follows:


| Parameter      | Description                                                                                                                                          |
|----------------|:-----------------------------------------------------------------------------------------------------------------------------------------------------|
| tableName      | Specify the parameter value of  ```BaseTable.tableName```                                                                                            |
| tableClassName | Specifies the type name of the generated table type, which defaults to the plural form of the noun of the entity class                               |
| alias          | Specify the parameter value of  ```BaseTable.alias```                                                                                                |
| catalog        | Specify the parameter value of  ```BaseTable.catalog```                                                                                              |
| schema         | Specify the parameter value of  ```BaseTable.schema```                                                                                               |
| ignoreColumns  | Specifies a list of property names to ignore. The ignored properties will not generate corresponding column definitions in the generated Table class |
| sequenceName   | The sequence name，By default, the first character lowercase of the tableClassName                                                                   |


#### Define Primary Key

Add the @Primary Key annotation to the entity class property to specify the property as the primary key.

#### Define Table Column

Add the @Column annotation to the entity class property to configure the generation options of the column definition.

The parameters of @Column are as follows:

| Parameter    | Description                                                                                                                                                                                                                                          |
|--------------|:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| columnName   | Specify column names in SQL                                                                                                                                                                                                                          |
| converter    | Specify the column converter. For the converter, please refer to the type converter description at the bottom of the document                                                                                                                        |
| propertyName | Specifies the property name of the corresponding column definition in the generated table class.                                                                                                                                                     |
| isReferences | Specifies whether this property is a reference column. Only ```entity class based on the Entity interface``` can be assigned a value of true. When this value is true, the generated column definition will automatically call the references method |

#### Ignore The Specified Properties

Add the @Ignore annotation to the entity class property, and the generated table class will not contain the column
definition of this property. Properties to ignore can also be specified in the ignore Columns parameter in @Table.

### Global Configuration

Add the @KtormKspConfig annotation to any class for global configuration (this annotation can only be added once), and
the annotation parameters are as follows:

| Parameter                        | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
|----------------------------------|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| allowReflectionCreateClassEntity | Whether to allow the creation of instance objects of ```entity of any kind class``` through reflection in the ```doCreateEntity``` method. If true, the instance will be created using reflection when the entity class constructor parameter has a default value parameter (reflection means a slight performance penalty, although in most cases this penalty is negligible). If it is false, the method will be directly constructed to create an instance, and the default value of the default value parameter in the construction will not take effect. |
| enumConverter                    | Global enum converter, which is automatically used by enum type properties in entity classes. For converters, please refer to the description of type converters below                                                                                                                                                                                                                                                                                                                                                                                        |
| singleTypeConverters             | Global single-type converter, which is automatically used by properties of the corresponding type in the entity class. For converters, please refer to the description of type converters below                                                                                                                                                                                                                                                                                                                                                               |
| namingStrategy                   | Global naming style configuration. For the naming style, please refer to the description of the naming style below                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| extension                        | Generation options for extension methods/properties. For specific extension descriptions, please refer to the related descriptions of method/property generators below                                                                                                                                                                                                                                                                                                                                                                                        |

extension parameter description

| Parameter                          | Description                                                             |
|------------------------------------|:------------------------------------------------------------------------|
| enableSequenceOf                   | whether to generate ```entity sequence``` extensions                    |
| enableClassEntitySequenceAddFun    | Whether to generate ```entity sequence``` ```add``` method extension    |
| enableClassEntitySequenceUpdateFun | Whether to generate ```entity sequence``` ```update``` method extension |

### Naming Style

By default, the table name in the table class is generated, taking the entity class name. The column name takes the
property name in the corresponding entity class.

The generated name can be modified through ```Global Naming Configuration``` and ```Single Naming Configuration```.

#### Single Naming Configuration

Table name: assign the ```tableName``` parameter to the @Table annotation on the entity class

Column name: add the @Column annotation to the property and assign the ```columnName``` parameter

```kotlin
@Table(tableName = "t_student")
public interface Student : Entity<Student> {
    @PrimaryKey
    public var id: Int?

    @Column(columnName = "student_name")
    public var name: String
    public var age: Int
}
```

Generate code：

```kotlin
public open class Students(
    alias: String? = null,
) : Table<Student>(tableName = "t_student", alias = alias, entityClass = Student::class) {
    public val id: Column<Int> = int("id").bindTo { it.id }.primaryKey()
    public val name: Column<String> = varchar("student_name").bindTo { it.name }
    public val age: Column<Int> = int("age").bindTo { it.age }
    public override fun aliased(alias: String): Students = Students(alias)
    public companion object : Students()
}
```

Table and column names configured in this way have the highest priority and are not affected by
the ```Global Naming Configuration```.

#### Global Naming Configuration

Add @KtormKspConfig annotation configuration on any class (this annotation can only be added once) and assign
the ```namingStrategy``` parameter, this property requires a **singleton object** that implements the NamingStrategy
interface, In ktorm-ksp comes a camel case to snake case naming style strategy: ```CamelCaseToSnakeCaseNamingStrategy```

```kotlin
@KtormKspConfig(
    namingStrategy = CamelCaseToSnakeCaseNamingStrategy::class
)
public class KtormConfig

@Table
public interface Student : Entity<Student> {
    @PrimaryKey
    public var id: Int?
    public var firstName: String
    public var age: Int
}
```

Generate code：

```kotlin
public open class Students(
    alias: String? = null,
) : Table<Student>(tableName = "student", alias = alias, entityClass = Student::class) {
    public val id: Column<Int> = int("id").bindTo { it.id }.primaryKey()

    public val firstName: Column<String> = varchar("first_name").bindTo { it.firstName }

    public val age: Column<Int> = int("age").bindTo { it.age }

    public override fun aliased(alias: String): Students = Students(alias)

    public companion object : Students()
}
```

### Type Converter

The data types supported by default in ktorm-ksp are as follows:

| kotlin Type             | Function Name | Underlying SQL Type | JDBC Type Code (java.sql.Types) |
|-------------------------|:-------------:|--------------------:|--------------------------------:|
| kotlin.Boolean          |    boolean    |             boolean |                   Types.BOOLEAN |
| kotlin.Int              |      int      |                 int |                   Types.INTEGER |
| kotlin.Short            |     short     |            smallint |                  Types.SMALLINT |
| kotlin.Long             |     long      |              bigint |                    Types.BIGINT |
| kotlin.Float            |     float     |               float |                     Types.FLOAT |
| kotlin.Double           |    double     |              double |                    Types.DOUBLE |
| kotlin.BigDecimal       |    decimal    |             decimal |                   Types.DECIMAL |
| kotlin.String           |    varchar    |             varchar |                   Types.VARCHAR |
| java.sql.Date           |   jdbcDate    |                date |                      Types.DATE |
| java.sql.Time           |   jdbcTime    |                time |                      Types.TIME |
| java.sql.Timestamp      | jdbcTimestamp |           timestamp |                 Types.TIMESTAMP |
| java.time.LocalDateTime |   datetime    |            datetime |                 Types.TIMESTAMP |
| java.time.LocalDate     |     date      |                date |                      Types.DATE |
| java.time.LocalTime     |     time      |                time |                      Types.TIME |
| java.time.MonthDay      |   monthDay    |             varchar |                   Types.VARCHAR |
| java.time.YearMonth     |   yearMonth   |             varchar |                   Types.VARCHAR |
| java.time.Year          |     year      |                 int |                   Types.INTEGER |
| java.time.Instant       |   timestamp   |           timestamp |                 Types.TIMESTAMP |
| java.util.UUID          |     uuid      |                uuid |                     Types.OTHER |
| kotlin.ByteArray        |     bytes     |               bytes |                    Types.BINARY |
| kotlin.Enum             |     enum      |                enum |                   Types.VARCHAR |

If you need to use a type that is not listed above, or if you want to override the default type behavior, you need to
use a type converter

There are three types of type converters (corresponding to three interfaces)

- SingleTypeConverter

  Only supports a certain type of converter, which can be used for global configuration or specified column
  configuration

- MultiTypeConverter

  Supports any type of converter, suitable for use in scenarios where objects are converted into json and stored in the
  database, and can only be used to specify column configuration

- EnumConverter

  A converter that supports any enumeration type, which can be used for global configuration or specific column
  configuration

#### How To Use Type Converter

You need to define a singleton and implement any of the above converter type interfaces. Then type converters can be
used via ```global configuration``` or ```column configuration```, and the priority of the converters is as follows:

Column Configuration > Global Configuration > Default Type Conversion Behavior

##### Use Type Converter in Column

Any type of converter can be used via the converter property in @Column.

```kotlin
//Define Entities
@Table
data class User(
    @PrimaryKey
    var id: Int,
    @Column(converter = UsernameConverter::class)
    var username: Username,
    var age: Int,
    @Column(converter = IntEnumConverter::class)
    var gender: Gender
)

enum class Gender {
    MALE,
    FEMALE
}

data class Username(
    val firstName: String,
    val lastName: String
)

//Type Converter
object UsernameConverter : SingleTypeConverter<Username> {
    public override fun convert(
        table: BaseTable<*>,
        columnName: String,
        propertyType: KClass<Username>
    ): Column<Username> {
        return with(table) {
            varchar(columnName).transform({
                val spilt = it.split("#")
                Username(spilt[0], spilt[1])
            }, {
                it.firstName + "#" + it.lastName
            })
        }
    }
}

object IntEnumConverter : EnumConverter {
    override fun <E : Enum<E>> convert(table: BaseTable<*>, columnName: String, propertyType: KClass<E>): Column<E> {
        val values = propertyType.java.enumConstants
        return with(table) {
            int(columnName).transform({ values[it] }, { it.ordinal })
        }
    }
}
```

Generate Code:

```kotlin
public open class Users(
    alias: String? = null,
) : BaseTable<User>(tableName = "User", alias = alias, entityClass = User::class) {
    public val id: Column<Int> = int("id").primaryKey()

    public val username: Column<Username> =
        UsernameConverter.convert(this,"username",Username::class)

    public val age: Column<Int> = int("age")

    public val gender: Column<Gender> = IntEnumConverter.convert(this,"gender",Gender::class)
    // ...
}
```

##### Use Type Converter in Global Configuration

Type converters can be added to the singleTypeConverters and enumConverter parameters in the global configuration
@KtormKspConfig

- singleTypeConverters: Receive the type array of SingleTypeConverter, when there is a property of the type supported by
  SingleTypeConverter, the corresponding converter will be used automatically

- enumConverter: Receives a type of EnumConverter, all enumeration types will automatically use the converter.

```kotlin
enum class Gender {
    MALE,
    FEMALE
}

@Table
data class User(
    @PrimaryKey
    var id: Int,
    var username: Username,
    var age: Int,
    var gender: Gender
)

data class Username(
    val firstName: String,
    val lastName: String
)

@KtormKspConfig(
    singleTypeConverters = [UsernameConverter::class],
    enumConverter = IntEnumConverter::class
)
class KtormConfig

object UsernameConverter : SingleTypeConverter<Username> {
    public override fun convert(
        table: BaseTable<*>,
        columnName: String,
        propertyType: KClass<Username>
    ): Column<Username> {
        return with(table) {
            varchar(columnName).transform({
                val spilt = it.split("#")
                Username(spilt[0], spilt[1])
            }, {
                it.firstName + "#" + it.lastName
            })
        }
    }
}

object IntEnumConverter : EnumConverter {
    override fun <E : Enum<E>> convert(table: BaseTable<*>, columnName: String, propertyType: KClass<E>): Column<E> {
        val values = propertyType.java.enumConstants
        return with(table) {
            int(columnName).transform({ values[it] }, { it.ordinal })
        }
    }
}
```

Generate code：

```kotlin
public open class Users(
    alias: String? = null,
) : BaseTable<User>(tableName = "User", alias = alias, entityClass = User::class) {
    public val id: Column<Int> = int("id").primaryKey()

    public val username: Column<Username> =
        UsernameConverter.convert(this,"username",Username::class)

    public val age: Column<Int> = int("age")

    public val gender: Column<Gender> = IntEnumConverter.convert(this,"gender",Gender::class)
    // ...
}
```

### Function And Property Generator

The table class code generated by ktorm-ksp is generated by multiple code generators, and these generators are all
customizable and extensible.

- TableTypeGenerator

  The table type declaration generator, only one is allowed, and the custom implementation will override the default
  implementation.

- TablePropertyGenerator

  The table class property declaration generator, only one is allowed, and the custom implementation will override the
  default implementation.

- TableFunctionGenerator

  The table class function generator, only one is allowed, and the custom implementation will override the default
  implementation.

- TopLevelPropertyGenerator

  Top-level property generator, generally used to generate extended property, multiple are allowed.

- TopLevelFunctionGenerator

  Top-level function generator, generally used to generate extension function, multiple are allowed.

#### Principles of custom generators

ktorm-ksp implements the custom extension of the generator through
the [SPI](https://docs.oracle.com/javase/tutorial/sound/SPI-intro.html) mechanism. The module dependencies are as
follows (simplified):

![ext_dependency graph](image/ext_dependency_graph.png)

The ```ktorm-ksp-compiler``` module automatically loads the generator defined in ```your-ext-module``` through SPI, and
uses it to participate in ```code generator```, to achieve the purpose of custom generator.

#### Steps To Customize The Generator

Please refer to the code implementation of this [module](ktorm-ksp-ext/ktorm-ksp-sequence-batch)

Create a new module that implements the generator (corresponding to ```your-ext-module``` in the above figure), and add
dependencies in ```build.gradle``` or ```pom.xml```

```groovy
// groovy dsl gradle 
dependencies {
    implementation 'org.ktorm:ktorm-ksp-codegen:${ktorm-ksp.version}'
}
```

```kotlin
// kotlin dsl gradle
dependencies {
    implementation("org.ktorm:ktorm-ksp-codegen:${ktorm-ksp.version}")
}
```

```xml
<!-- maven -->
<dependencies>
    <dependency>
        <groupId>org.ktorm</groupId>
        <artifactId>ktorm-ksp-codegen</artifactId>
        <version>${ktorm-ksp.version}</version>
    </dependency>
</dependencies>
```

Create a new generator class that implements any generator interface.

```kotlin
public class SequenceAddAllFunctionGenerator : TopLevelFunctionGenerator {
    // Ignore code
}
public class SequenceUpdateAllFunctionGenerator : TopLevelFunctionGenerator {
    // Ignore code
}
```

Create a new file in the ```resources/META-INF/services``` directory, the file name is the fully qualified class name of
the generator interface (org.ktorm.ksp.codegen.TopLevelFunctionGenerator), and add the fully qualified class name of the
custom generator in the file. name, and multiple classes are separated by newlines.

```
org.ktorm.ksp.ext.SequenceAddAllFunctionGenerator
org.ktorm.ksp.ext.SequenceUpdateAllFunctionGenerator
```

Add the ```your-ext-module``` to the modules that need to generate code with it (corresponding to ```your-app-module```
in the above figure)

```groovy
// groovy dsl gradle 
dependencies {
    implementation 'org.ktorm:ktorm-ksp-api:${ktorm-ksp.version}'
    ksp 'org.ktorm:ktorm-ksp-compile:${ktorm-ksp.version}'
    ksp project(':your-ext-module')
}
```

```kotlin
// kotlin dsl gradle
dependencies {
    implementation("org.ktorm:ktorm-ksp-api:${ktorm-ksp.version}")
    ksp("org.ktorm:ktorm-ksp-compile:${ktorm-ksp.version}")
    ksp(project(":your-ext-module"))
}
```

```xml
<!-- maven -->
<plugin>
    <groupId>org.jetbrains.kotlin</groupId>
    <artifactId>kotlin-maven-plugin</artifactId>
    <version>${kotlin.version}</version>
    <configuration>
        <compilerPlugins>
            <compilerPlugin>ksp</compilerPlugin>
        </compilerPlugins>
        <sourceDirs>
            <sourceDir>src/main/kotlin</sourceDir>
            <sourceDir>target/generated-sources/ksp</sourceDir>
        </sourceDirs>
    </configuration>
    <dependencies>
        <dependency>
            <groupId>com.dyescape</groupId>
            <artifactId>kotlin-maven-symbol-processing</artifactId>
            <version>1.3</version>
        </dependency>
        <dependency>
            <groupId>org.ktorm</groupId>
            <artifactId>ktorm-ksp-compiler</artifactId>
            <version>${ktorm-ksp.version}</version>
        </dependency>
        <dependency>
            <groupId><!-- your-ext-module groupId --></groupId>
            <artifactId><!-- your-ext-module artifactId --></artifactId>
            <version><!-- your-ext-module version --></version>
        </dependency>
    </dependencies>
    <executions>
        <execution>
            <id>compile</id>
            <phase>compile</phase>
            <goals>
                <goal>compile</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

Let ksp generate the code again. You will see the code generated by the custom generator.

#### Available Generator Extensions

- [ktorm-ksp-sequence-batch](ktorm-ksp-ext/ktorm-ksp-sequence-batch)

A function to generate batch addition and batch update for ```any kind of class``` entity sequence. Dependencies:

```groovy
ksp 'org.ktorm:ktorm-ksp-sequence-batch:${ktorm-ksp.version}'
```

Generate the following extension function:

```kotlin
/**
 * Batch insert entities into the database, this method will not get the auto-incrementing primary key
 * @param entities List of entities to insert
 * @return the effected row counts for each sub-operation.
 */
public fun EntitySequence<Customer, Customers>.addAll(entities: Iterable<Customer>): IntArray =
    this.database.batchInsert(Customers) {
        for (entity in entities) {
            item {
                set(Customers.id, entity.id)
                set(Customers.name, entity.name)
                set(Customers.email, entity.email)
                set(Customers.phoneNumber, entity.phoneNumber)
            }
        }
    }

/**
 * Batch update based on entity primary key
 * @param entities List of entities to update
 * @return the effected row counts for each sub-operation.
 */
public fun EntitySequence<Customer, Customers>.updateAll(entities: Iterable<Customer>): IntArray =
    this.database.batchUpdate(Customers) {
        for (entity in entities) {
            item {
                set(Customers.name, entity.name)
                set(Customers.email, entity.email)
                set(Customers.phoneNumber, entity.phoneNumber)
                where {
                    it.id eq entity.id!!
                }
            }
        }
    }
```
