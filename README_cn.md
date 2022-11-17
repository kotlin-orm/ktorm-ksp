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

:us: [English](README.md) | :cn: 简体中文

# ktorm-ksp 是什么？

帮助[Ktorm](https://github.com/kotlin-orm/ktorm) 生成样板代码的KSP扩展. 它可以通过实体类自动生成Table对象，同时让data class定义的实体更加好用，支持自定义扩展代码生成逻辑。

# 特性

- 只需编写实体类，自动生成相应的Table对象。支持基于Entity接口/[任意类](https://www.ktorm.org/zh-cn/define-entities-as-any-kind-of-classes.html)
  定义的实体类

- 让[任意类](https://www.ktorm.org/zh-cn/define-entities-as-any-kind-of-classes.html)
  实体类更好用。默认自动实现doCreateEntity方法，以及实体序列的新增/更新方法

- 对基于Entity接口的实体生成```伪构造函数```、```copy```、```componentN```方法, 使其像data class一样好用

- 可扩展的代码生成逻辑。通过SPI机制，只需实现指定的接口，即可编写自己所需的自动生成逻辑。

定义实体 ▼

```kotlin
@Table
public data class Student(
    @PrimaryKey
    public var id: Int?,
    public var name: String,
    public var age: Int
)
```

自动生成代码 ▼

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

public fun EntitySequence<Student, Students>.add(entity: Student, isDynamic: Boolean = false): Int { /*此处省略具体实现*/
}

public fun EntitySequence<Student, Students>.update(entity: Student, isDynamic: Boolean = false): Int { /*此处省略具体实现*/
}

public val Database.students: EntitySequence<Student, Students> get() = this.sequenceOf(Students)
```

> 为什么使用class作为表类型，而不是object单例？
> 请查阅文档说明:  [自连接查询与表别名](https://www.ktorm.org/zh-cn/joining.html#%E8%87%AA%E8%BF%9E%E6%8E%A5%E6%9F%A5%E8%AF%A2%E4%B8%8E%E8%A1%A8%E5%88%AB%E5%90%8D)


默认生成的代码:

|                                 | 基于Entity接口的实体类 | 任意类的实体类           |
|---------------------------------|----------------|-------------------|
| ```Table```类型                   | ✅              | ✅                 |
| ```column```属性                  | ✅              | ✅ (不支持references) |
| ```doCreateEntity```方法          |                | ✅                 |
| ```sequenceOf```扩展属性            | ✅              | ✅                 |
| ```EntitySequence.add``` 扩展方法   |                | ✅                 |
| ```EntitySequence.update```扩展方法 |                | ✅                 |
| 伪构造函数                           | ✅              |                   |
| ```entity.componentN```扩展方法     | ✅              |                   |
| ```entity.copy```扩展方法           | ✅              |                   |

- [快速入门](#快速入门)
- [实体定义](#实体定义)
    - [任意类的实体类定义](#任意类的实体类定义)
    - [基于Entity接口的实体类定义](#基于Entity接口的实体类定义)
    - [表定义](#表定义)
    - [主键定义](#主键定义)
    - [列定义](#列定义)
    - [忽略指定属性](#忽略指定属性)
- [全局配置](#全局配置)
- [命名风格](#命名风格)
    - [命名单独配置](#命名单独配置)
    - [全局命名风格配置](#全局命名风格配置)
- [SqlType](#SqlType)
- [方法/属性生成器](#方法/属性生成器)
    - [自定义生成器的步骤](#自定义生成器的步骤)
    - [可用的生成器扩展](#可用的生成器扩展)

### 快速入门

在```build.gradle```或```pom.xml```中添加依赖

```groovy
// Groovy DSL
plugins {
    id 'com.google.devtools.ksp' version '1.6.21-1.0.5'
}

dependencies {
    implementation 'org.ktorm:ktorm-ksp-api:${ktorm_ksp.version}'
    ksp 'org.ktorm:ktorm-ksp-compiler:${ktorm_ksp.version}'
}
```

```kotlin
// Kotlin DSL
plugins {
    id("com.google.devtools.ksp").version("1.6.21-1.0.5")
}

dependencies {
    implementation("org.ktorm:ktorm-ksp-api:${ktorm_ksp.version}")
    ksp("org.ktorm:ktorm-ksp-compiler:${ktorm_ksp.version}")
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
                        <version>${ktorm_ksp.version}</version>
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
            <version>${ktorm_ksp.version}</version>
        </dependency>
    </dependencies>
</project>
```

为了让idea识别生成的代码，还需要在build.gradle中添加以下配置。（否则你将会看到一堆红线警告）如果你使用的是maven则可以跳过这一步，因为上一步
已经包含相关的配置了。

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

如何让KSP生成代码?

- Gradle: 构建项目、运行应用、执行 ```gradle build``` 命令均可。将会在```build/generated/ksp/main/kotlin```目录中生成代码。
- Maven: 执行```mvn kotlin:compile``` 命令。将会在```target/generated-sources/ksp``` 目录中生成代码。

### 实体定义

#### 任意类的实体类定义

```kotlin
@Table
public data class Student(
    @PrimaryKey
    public var id: Int?,
    public var name: String,
    public var age: Int
)
```

生成代码：

```kotlin
public open class Students(
    alias: String? = null,
) : BaseTable<Student>(tableName = "Student", alias = alias, entityClass = Student::class) {
    // Ignore code
}

public fun EntitySequence<Student, Students>.add(entity: Student, isDynamic: Boolean = false): Int { /*此处省略具体实现*/
}
public fun EntitySequence<Student, Students>.update(entity: Student, isDynamic: Boolean = false): Int { /*此处省略具体实现*/
}
public val Database.students: EntitySequence<Student, Students> get() = this.sequenceOf(Students)
```

默认生成了EntitySequence的扩展属性，省去了手写样板代码的步骤，直接调用即可查询

```kotlin
val users = database.users.toList()
```

#### 基于Entity接口的实体类定义

```kotlin
@Table
public interface Student : Entity<Student> {
    @PrimaryKey
    public var id: Int?
    public var name: String
    public var age: Int
}
```

生成代码：

```kotlin
public open class Students(
    alias: String? = null,
) : Table<Student>(tableName = "Student", alias = alias, entityClass = Student::class) {
    // Ignore code
}

public val Database.students: EntitySequence<Student, Students> get() = this.sequenceOf(Students)

public fun Student(id: Int? = Undefined.of(), name: String? = Undefined.of(), age: Int? = Undefined.of()): Student {
  // Ignore code
}

public fun Student.copy(
  id: Int? = Undefined.of(),
  name: String? = Undefined.of(),
  age: Int? = Undefined.of()
): Student {
  // Ignore code
}

public operator fun Student.component1(): Int? = this.id
public operator fun Student.component2(): String = this.name
public operator fun Student.component3(): Int = this.age

```

默认生成了EntitySequence的扩展属性，省去了手写样板代码的步骤，直接调用即可查询

```kotlin
val users = database.users.toList()
```


#### 表定义

将@Table注解添加到实体类上，将会自动生成相应的Table类。

@Table的参数如下：

| 参数                 |                           说明                            |
|--------------------|:-------------------------------------------------------:|
| name               |                指定BaseTable.tableName的参数值                |
| className          |               指定生成表类型的类型名称，默认取实体类的名词复数形式                |
| alias              |                  指定BaseTable.alias的参数值                  |
| catalog            |                 指定BaseTable.catalog的参数值                 |
| schema             |                 指定BaseTable.schema的参数值                  |
| ignoreColumns      |       指定要忽略的属性名称列表，被忽略的属性将不会在生成的Table类中，生成对应的列定义        |
| entitySequenceName | 指定生成EntitySequence的扩展属性名称，默认取tableClassName首字母小写转换后的名称。 |

#### 主键定义

在实体类属性添加@PrimaryKey注解，指定属性为主键。

#### 列定义

在实体类属性添加@Column注解，可配置列定义的生成选项。

@Column的参数如下：

| 参数           | 说明                    |
|--------------|:----------------------|
| name         | 指定SQL中的列名             |
| sqlType      | 指定[SqlType](#SqlType) |
| propertyName | 指定在生成表类中，对应列定义的属性名称。  |

#### 引用列定义

如果需要引用另一个表, 在引用的属性添加@References注解即可. 添加该注解的实体类及引用的实体类, 必须都是基于Entity的接口实体类.

@References的参数如下

| 参数           | 说明                   |
|--------------|:---------------------|
| name         | 指定SQL中的列名            |
| propertyName | 指定在生成表类中，对应列定义的属性名称。 |

当```name```为空, 则默认生成```列名```为```字段名```+```引用表主键字段名```, 如果配置了[命名风格策略](#命名风格) 则会进一步进行转换.

代码示例:

```kotlin
@Table
public interface School : Entity<School> {
    @PrimaryKey
    public var id: Int
    public var name: String
}

@Table
public interface Student : Entity<Student> {
    @PrimaryKey
    public var id: Int

    @References
    public var school: School
}
```

生成代码:

```kotlin
public open class Students(alias: String?) : Table<Student>("Student", alias) {
    public val id: Column<Int> = int("id").primaryKey().bindTo { it.id }
    public val school: Column<Int> = int("schoolId").references(Schools) { it.school }

    // ...
}
```

#### 忽略指定属性

在实体类属性添加@Ignore注解，生成的表类中不会包含此属性的列定义。也可以在@Table中的ignoreColumns参数指定要忽略的属性。

### 全局配置

在任意类上添加@KtormKspConfig注解，可以进行全局配置（只能添加一次此注解），注解参数如下

| 参数                               | 说明                                                                                                                                                                    |
|----------------------------------|:----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| allowReflectionCreateClassEntity | 是否允许在doCreateEntity方法中通过反射创建```任意类的实体类```的实例对象。如果为true，那么当实体类构造参数存在默认值参数时，会使用反射进行创建实例 （反射意味着带来了轻微的性能损耗，尽管大部分情况下这个损耗可以忽略不计）。如果为false，那么会直接构造方法创建实例，构造中的默认值参数的默认值，将无法生效 |
| namingStrategy                   | 全局命名风格配置。关于命名风格请参考下文命名风格的说明                                                                                                                                           |
| extension                        | 扩展方法/属性的生成选项（具体的扩展说明请参考下文方法/属性生成器的相关说明）                                                                                                                               |

```extension```参数说明

- enableSequenceOf
  是否生成```EntitySequence```属性扩展. 生成代码示例:
  ```kotlin 
  val Database.employees: EntitySequence<Employee,Employees>
  ```


- enableClassEntitySequenceAddFun
  是否生成```EntitySequence.add```方法扩展. 该方法用于将实体插入到数据库中, 生成代码示例:
  ```kotlin
  fun EntitySequence<Employee,Employees>.add(employee: Employee, isDynamic: Boolean = false)
  ```
  ```isDynamic```: 如果值为true，则生成的 SQL 将仅包含非空列。


- enableClassEntitySequenceUpdateFun
  是否生成```EntitySequence.update```方法扩展. 改方法用于根据主键更新实体字段, 生成代码示例:
  ```kotlin
  fun EntitySequence<Employee,Employees>.update(employee: Employee, isDynamic: Boolean = false)
  ```
  ```isDynamic```: 如果值为true，则生成的 SQL 将仅包含非空列。


- enableInterfaceEntitySimulationDataClass
  是否生成```伪构造函数``` ```componentN``` ```copy```方法. 只会对基于Entity接口的实体类生成, 目的是让实体类变的像```data class```一样好用, 生成代码示例:
  ```kotlin
  public fun Employee(
    id: Int? = Undefined.of(),
    name: String? = Undefined.of(),
    job: String? = Undefined.of(),
  ): Employee
  
  public fun Employee.copy(
    id: Int? = Undefined.of(),
    name: String? = Undefined.of(),
    job: String? = Undefined.of(),
  ): Employee 
  
  public operator fun Employee.component1(): Int = this.id
  public operator fun Employee.component2(): String = this.name
  public operator fun Employee.component3(): String = this.job
  ```
  **深入了解参数默认值:**```Undefined.of()```

  在ktorm中创建实体实例后，对实例属性赋值null和未赋值是两种实质上不同的行为。例如:
  ```kotlin
  val employee1 = Entity.create<Employee>()
  employee1.id = null
  database.employees.add(employee1)
  // SQL: insert into employee (id) values (null)
  val employee2 = Entity.create<Employee>()
  employee2.id = null
  employee2.name = null
  database.employees.add(employee2)
  // SQL: insert into employee (id, name) values (null, null)
  ```
  生成的SQL语句中不会包含未赋值的属性。
  ksp生成的```构造函数```和```copy函数```有类似的效果。
  ```kotlin
  val employee = Employee(id = null)
  // 实际效果相当于下面的写法
  val employee = Entity.create<Employee>()
  employee.id = null
  // 没有赋值name属性: employee.name = null
  ```
  调用函数时，创建的实体实例不会赋值没有传参的相应属性。
  为了实现这一点，```构造函数```和```copy函数```中的参数默认值可能是JDK动态代理对象、动态生成字节码的对象、由Unsafe创建的对象
  (这取决于具体类型是什么) 这个生成的实例是唯一的，不会与调用时传递的参数冲突 (除非你也调用```Undefined.of()```来获取实例) 因此，
  它可以帮助我们确定哪些参数传递了值，哪些参数在调用方法时没有传递值。  
  此实现的限制是参数类型不能是非空基本类型。这是因为kotlin中的非空基本类型会被自动拆箱，这将导致我们上述实现失败，并且无法判断调用时传递了
  哪些参数值。所以在生成的```构造函数```和```copy函数```中，如果属性是非空基本类型，则会自动转换为可空基本类型。并且在实际创建实例的过程中，
  会判断参数值是否为null，如果为null，则会引发异常

### 命名风格

在默认情况下，生成表类中的表名，取实体类类名。列名取对应实体类中的属性名称。

可以通过全局配置、单独配置修改生成的名称。

#### 命名单独配置

表名配置：在实体类上的@Table注解中赋值name参数

列名配置：在属性上添加@Column注解并赋值name参数

```kotlin
@Table(name = "t_student")
public interface Student : Entity<Student> {
    @PrimaryKey
    public var id: Int?

    @Column(name = "student_name")
    public var name: String
    public var age: Int
}
```

生成代码：

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

通过这种方式配置的表名/列名拥有最高优先级，不受全局命名风格配置影响。

#### 全局命名风格配置

在任意类上添加@KtormKspConfig注解配置（注意项目中只能声明一次此注解）并赋值namingStrategy参数，此属性需要一个实现NamingStrategy接口的**单例对象**,
在ktorm-ksp中自带了驼峰转蛇形的命名策略： CamelCaseToSnakeCaseNamingStrategy

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

生成代码：

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

### SqlType

在ktorm-ksp中默认支持的数据类型如下

| kotlin类型                |      函数名      |   底层SQL类型 | JDBC 类型码 (java.sql.Types) |
|-------------------------|:-------------:|----------:|--------------------------:|
| kotlin.Boolean          |    boolean    |   boolean |             Types.BOOLEAN |
| kotlin.Int              |      int      |       int |             Types.INTEGER |
| kotlin.Short            |     short     |  smallint |            Types.SMALLINT |
| kotlin.Long             |     long      |    bigint |              Types.BIGINT |
| kotlin.Float            |     float     |     float |               Types.FLOAT |
| kotlin.Double           |    double     |    double |              Types.DOUBLE |
| kotlin.BigDecimal       |    decimal    |   decimal |             Types.DECIMAL |
| kotlin.String           |    varchar    |   varchar |             Types.VARCHAR |
| java.sql.Date           |   jdbcDate    |      date |                Types.DATE |
| java.sql.Time           |   jdbcTime    |      time |                Types.TIME |
| java.sql.Timestamp      | jdbcTimestamp | timestamp |           Types.TIMESTAMP |
| java.time.LocalDateTime |   datetime    |  datetime |           Types.TIMESTAMP |
| java.time.LocalDate     |     date      |      date |                Types.DATE |
| java.time.LocalTime     |     time      |      time |                Types.TIME |
| java.time.MonthDay      |   monthDay    |   varchar |             Types.VARCHAR |
| java.time.YearMonth     |   yearMonth   |   varchar |             Types.VARCHAR |
| java.time.Year          |     year      |       int |             Types.INTEGER |
| java.time.Instant       |   timestamp   | timestamp |           Types.TIMESTAMP |
| java.util.UUID          |     uuid      |      uuid |               Types.OTHER |
| kotlin.ByteArray        |     bytes     |     bytes |              Types.BINARY |
| kotlin.Enum             |     enum      |      enum |             Types.VARCHAR |

如果需要使用不在上述的类型，或者想覆盖默认的类型行为，需要在```@Column```注解中传入```sqlType```参数, 传入```SqlType```或```SqlTypeFactory```
的类型, 且此类型必须是```单例```的.

- SqlType
  适用于明确某一个kotlin类型, 如Int、String. 更多信息请参考[文档](https://www.ktorm.org/zh-cn/schema-definition.html#SqlType)
- SqlTypeFactory
  接收字段的信息返回```SqlType```实例, 适用于不明确kotlin类型, 如json类型

代码参考

```kotlin
@Table
public data class Student(
    @PrimaryKey
    public var id: Int?,
    @Column(sqlType = UIntSqlType::class)
    public var age: UInt,
    @Column(sqlType = IntEnumSqlTypeFactory::class)
    public var gender: Gender
)

public object UIntSqlType : SqlType<UInt>(Types.INTEGER, "int") {
    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: UInt) {
        ps.setLong(index, parameter.toLong())
    }
    override fun doGetResult(rs: ResultSet, index: Int): UInt {
        return rs.getLong(index).toUInt()
    }
}

public object IntEnumSqlTypeFactory : SqlTypeFactory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> createSqlType(property: KProperty1<*, T?>): SqlType<T> {
        val returnType = property.returnType.jvmErasure.java
        if (returnType.isEnum) {
            return IntEnumSqlType(returnType as Class<out Enum<*>>) as SqlType<T>
        } else {
            throw IllegalArgumentException("The property is required to be typed of enum but actually: $returnType")
        }
    }

    private class IntEnumSqlType<E : Enum<E>>(val enumClass: Class<E>) : SqlType<E>(Types.INTEGER, "int") {
        override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: E) {
            ps.setInt(index, parameter.ordinal)
        }
        override fun doGetResult(rs: ResultSet, index: Int): E? {
            return enumClass.enumConstants[rs.getInt(index)]
        }
    }
}
```

生成代码

```kotlin
public open class Students(alias: String?) : BaseTable<Student>("student", alias) {
    public val id: Column<Int> = int("id").primaryKey()
    public val age: Column<UInt> = registerColumn("age", UIntSqlType)
    public val gender: Column<Gender> = registerColumn("gender", IntEnumSqlTypeFactory.createSqlType(Student::gender))
    // ...
}
```

### 方法/属性生成器

ktorm-ksp生成的表类代码由多个代码生成器进行生成，这些生成器都是可自定义扩展的。

- 表类型生成器 TableTypeGenerator

  表类型声明生成，只允许存在一个，自定义实现会覆盖默认实现。

- 表属性生成器 TablePropertyGenerator

  表类属性声明生成，只允许存在一个，自定义实现会覆盖默认实现。

- 表方法生成器 TableFunctionGenerator

  表类方法生成，只允许存在一个，自定义实现会覆盖默认实现。

- 顶级属性生成器 TopLevelPropertyGenerator

  顶级属性生成，一般用于生成扩展属性，允许存在多个。

- 顶级方法生成器 TopLevelFunctionGenerator

  顶级方法生成，一般用于生成扩展方法，允许存在多个。

#### 自定义生成器原理

ktorm-ksp通过[SPI](https://docs.oracle.com/javase/tutorial/sound/SPI-intro.html) 机制实现生成器的自定义扩展，模块依赖关系如下（经过简化）：

![ext_dependency graph](docs/img/ext_dependency_graph.png)

ktorm-ksp-compiler模块通过SPI自动加载```my-ktorm-ksp-ext```中定义的生成器，并使用它参与生成代码，从而达到自定义生成器的目的。

#### 自定义生成器的步骤

（请参考项目[ktorm-ksp-ext-batch](https://github.com/kotlin-orm/ktorm-ksp-ext-batch)的代码实现）

新建实现生成器的module（对应上图中的```my-ktorm-ksp-ext```），在```build.gradle```或```pom.xml```中添加依赖

```groovy
// groovy dsl gradle 
dependencies {
    implementation 'org.ktorm:ktorm-ksp-spi:${ktorm_ksp.version}'
}
```

```kotlin
// kotlin dsl gradle
dependencies {
    implementation("org.ktorm:ktorm-ksp-spi:${ktorm_ksp.version}")
}
```

```xml
<!-- maven -->
<dependencies>
    <dependency>
        <groupId>org.ktorm</groupId>
        <artifactId>ktorm-ksp-spi</artifactId>
        <version>${ktorm_ksp.version}</version>
    </dependency>
</dependencies>
```

新建生成器类，实现任意一个生成器接口。

```kotlin
public class SequenceAddAllFunctionGenerator : TopLevelFunctionGenerator {
    // 忽略具体实现
}
public class SequenceUpdateAllFunctionGenerator : TopLevelFunctionGenerator {
    // 忽略具体实现
}
```

在resources/META-INF/services目录下新建文件，文件名为生成器接口的全限定类名（org.ktorm.ksp.spi.TopLevelFunctionGenerator）
并中文件中新增自定义的生成器的全限定类名，多个类以换行分割。

```
org.ktorm.ksp.ext.SequenceAddAllFunctionGenerator
org.ktorm.ksp.ext.SequenceUpdateAllFunctionGenerator
```

将上面的```my-ktorm-ksp-ext```模块添加到需要用它来生成代码的模块（对应上图中的```app```模块）

```groovy
// groovy dsl gradle 
dependencies {
    implementation 'org.ktorm:ktorm-ksp-api:${ktorm_ksp.version}'
    ksp 'org.ktorm:ktorm-ksp-compiler:${ktorm_ksp.version}'
    ksp project(':my-ktorm-ksp-ext')
}
```

```kotlin
// kotlin dsl gradle
dependencies {
    implementation("org.ktorm:ktorm-ksp-api:${ktorm_ksp.version}")
    ksp("org.ktorm:ktorm-ksp-compiler:${ktorm_ksp.version}")
    ksp(project(":my-ktorm-ksp-ext"))
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
            <version>${ktorm_ksp.version}</version>
        </dependency>
        <dependency>
            <groupId><!-- my-ktorm-ksp-ext groupId --></groupId>
            <artifactId><!-- my-ktorm-ksp-ext artifactId --></artifactId>
            <version><!-- my-ktorm-ksp-ext version --></version>
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

构建项目，你将看到通过自定义生成器生成的代码。

#### 可用的生成器扩展

- [ktorm-ksp-ext-batch](https://github.com/kotlin-orm/ktorm-ksp-ext-batch)
