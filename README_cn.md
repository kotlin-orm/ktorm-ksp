<p align="center">
    <img src="https://raw.githubusercontent.com/kotlin-orm/ktorm-docs/master/source/images/logo-full.png" alt="Ktorm" width="300" />
</p>

:us: [English](README.md) | :cn: 简体中文

# ktorm-ksp是什么？

帮助[Ktorm](https://github.com/kotlin-orm/ktorm) 生成样板代码的KSP扩展. 它可以通过实体类自动生成Table对象，同时让data class定义的实体更加好用，支持自定义扩展代码生成逻辑。

- 注意： 该项目还在进行开发中

# 特性

- 只需编写实体类，自动生成相应的Table对象。支持基于Entity接口定义的类，也支持普通的class/data class定义的实体

- 对class实体类更好的支持，默认实现doCreateEntity方法，以及实体序列的新增/更新方法

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
public object Students : BaseTable<Student>(tableName = "Student", entityClass = Student::class,) {
    public val id: Column<Int> = int("id").primaryKey()

    public val name: Column<String> = varchar("name")

    public val age: Column<Int> = int("age")

    public override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean): Student {
        return Student(
            id = row[id],
            name = row[name]!!,
            age = row[age]!!,
        )
    }
}

public fun EntitySequence<Student, Students>.add(entity: Student): Int { /*此处省略具体实现*/
}

public fun EntitySequence<Student, Students>.update(entity: Student): Int { /*此处省略具体实现*/
}

public val Database.students: EntitySequence<Student, Students> get() = this.sequenceOf(Students)
```

### 快速入门

在build.gradle中添加依赖

```groovy
plugins {
    id 'com.google.devtools.ksp' version '1.6.10-1.0.2'
}

dependencies {
    implementation 'org.ktorm:ktorm-ksp-api:${ktorm-ksp.version}'
    ksp 'org.ktorm:ktorm-ksp-compile:${ktorm-ksp.version}'
}
```

为了让idea识别生成的代码 还需要在build.gradle中添加以下配置

```groovy
kotlin {
    sourceSets {
        main.kotlin.srcDirs += 'build/generated/ksp/main/kotlin'
        test.kotlin.srcDirs += 'build/generated/ksp/test/kotlin'
    }
}
```

### 实体定义

#### class 实体类定义

声明一个data class的实体类

```kotlin
@Table
public data class Student(
    @PrimaryKey
    public var id: Int?,
    public var name: String,
    public var age: Int
)
```

运行项目或者gradle build命令, 将会在项目下的 build/generated/ksp/main/kotlin 生成表定义以及相关的扩展，下面是生成的代码

```kotlin
public object Students : BaseTable<Student>(tableName = "Student", entityClass = Student::class) {
    // 此处省略具体实现
}

public fun EntitySequence<Student, Students>.add(entity: Student): Int { /*此处省略具体实现*/
}
public fun EntitySequence<Student, Students>.update(entity: Student): Int { /*此处省略具体实现*/
}
public val Database.students: EntitySequence<Student, Students> get() = this.sequenceOf(Students)
```

默认生成了EntitySequence的扩展属性，省去了手动编写样板代码的步骤，直接调用即可查询

```kotlin
val users = database.users.toList()
```

#### interface 实体类定义

基于Entity接口定义的实体类，在使用上和class实体没有太大区别

```kotlin
@Table
public interface Student : Entity<Student> {
    @PrimaryKey
    public var id: Int?
    public var name: String
    public var age: Int
}
```

运行项目或者gradle build命令, 将会在项目下的 build/generated/ksp/main/kotlin 生成表定义以及相关的扩展，下面是生成的代码

```kotlin
public object Students : Table<Student>(tableName = "Student", entityClass = Student::class) {
    // 此处省略具体实现
}

public val Database.students: EntitySequence<Student, Students> get() = this.sequenceOf(Students)
```

与entity实体生成的表不同的地方在于，表继承了Table类而不是BaseTable类，因此无需实现doCreateEntity方法. 也因此无需生成EntitySequence的add update扩展方法（因为已经有了）

```kotlin
val users = database.users.toList()
```

#### 命名风格

在默认情况下，生成表类中的表名，取实体类类名。列名取对应实体类中的属性名称。 有两种方式可以修改生成的名称，第一种方式是单独配置名称:

表名配置：在实体类上的@Table注解中赋值tableName属性

列名配置：在属性上添加@Column注解并赋值columnName属性

例如：

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

生成的表定义代码如下

```kotlin
public object Students : Table<Student>(tableName = "t_student", entityClass = Student::class) {
    public val id: Column<Int> = int("id").bindTo { it.id }.primaryKey()
    public val name: Column<String> = varchar("student_name").bindTo { it.name }
    public val age: Column<Int> = int("age").bindTo { it.age }
}
```

通过这种方式配置的表名/列名拥有最高优先级，不受下面的第二种方式配置影响。

第二种方式是全局配置命名风格策略：

在任意类上添加@KtormKspConfig注解配置（注意项目中只能声明一次此注解）并赋值namingStrategy属性，此属性需要一个实现NamingStrategy接口的**单例对象**,
在ktorm-ksp中自带了驼峰转小写下划线风格的命名风格策略： CamelCaseToLowerCaseUnderscoresNamingStrategy

```kotlin
@KtormKspConfig(
    namingStrategy = CamelCaseToLowerCaseUnderscoresNamingStrategy::class
)
public class KtormConfig

@Table
public interface Student : Entity<Student> {
    @PrimaryKey
    public var id: Int?
    public var name: String
    public var age: Int
}
```

生成代码如下

```kotlin
public object StudentProfiles :
    Table<StudentProfile>(tableName = "student_profile", entityClass = StudentProfile::class) {
    public val id: Column<Int> = int("id").bindTo { it.id }.primaryKey()
    public val firstName: Column<String> = varchar("first_name").bindTo { it.firstName }
    public val lastName: Column<String> = varchar("last_name").bindTo { it.lastName }
    public val telephoneNumber: Column<String> =
        varchar("telephone_number").bindTo { it.telephoneNumber }
}
```

### 类型转换器

在ktorm-ksp中默认支持的数据类型如下

| kotlin类型 | 函数名 | 底层SQL类型 | JDBC 类型码 (java.sql.Types)
|---------|:-------------:|------:|------:|
kotlin.Boolean  | boolean | boolean | Types.BOOLEAN
kotlin.Int  | int | int | Types.INTEGER
kotlin.Short  | short | smallint | Types.SMALLINT
kotlin.Long  | long | bigint | Types.BIGINT
kotlin.Float  | float | float | Types.FLOAT
kotlin.Double  | double | double | Types.DOUBLE
kotlin.BigDecimal  | decimal | decimal | Types.DECIMAL
kotlin.String  | varchar | varchar | Types.VARCHAR
java.sql.Date  | jdbcDate | date | Types.DATE
java.sql.Time  | jdbcTime | time | Types.TIME
java.sql.Timestamp  | jdbcTimestamp | timestamp | Types.TIMESTAMP
java.time.LocalDateTime  | datetime | datetime | Types.TIMESTAMP
java.time.LocalDate  | date | date | Types.DATE
java.time.LocalTime  | time | time | Types.TIME
java.time.MonthDay  | monthDay | varchar | Types.VARCHAR
java.time.YearMonth  | yearMonth | varchar | Types.VARCHAR
java.time.Year  | year | int | Types.INTEGER
java.time.Instant  | timestamp | timestamp | Types.TIMESTAMP
java.util.UUID  | uuid | uuid | Types.OTHER
kotlin.ByteArray  | bytes | bytes | Types.BINARY
kotlin.Enum  | enum | enum | Types.VARCHAR

如果需要使用不包含上述的类型，或者想覆盖默认的类型行为，则需要使用到类型转换器

类型转换器有以下三种（对应三个接口)

- SingleTypeConverter

  仅支持某一个类型的转换器，可用于全局配置或者指定列配置

- MultiTypeConverter

  支持任意类型的转换器，适合使用将对象转换成json存储到数据库中的使用场景, 只能用于指定列配置

- EnumConverter

  支持任意枚举类型的转换器，可用于全局配置或者指定列配置

#### 如何使用类型转换器

首先需要定义一个单例，并且实现上述任意一个转换器类型接口。 然后可以通过全局配置或者列配置使用类型转换器。

##### 全局配置使用类型转换器

类型转换器可以添加到全局配置@KtormKspConfig中的singleTypeConverters和enumConverter属性

- singleTypeConverters: 接收SingleTypeConverter的类型列表，当有任意类型符合SingleTypeConverter支持的类型时，就会自动使用该转换器

- enumConverter: 接收一个EnumConverter的类型，所有的枚举类型会自动使用该转换器。

通过此方式可以覆盖上诉表格中的默认类型转换行为

代码示例:

```kotlin
@Table
data class User(
    @PrimaryKey
    var id: Int,
    var username: Username,
    var age: Int
)

data class Username(
    val firstName: String,
    val lastName: String
)

@KtormKspConfig(singleTypeConverters = [UsernameConverter::class])
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
```

生成代码如下

```kotlin
public object Users : BaseTable<User>(tableName = "User", entityClass = User::class) {
    public val id: Column<Int> = int("id").primaryKey()
    public val username: Column<Username> = UsernameConverter.convert(this, "username", Username::class)
    public val age: Column<Int> = int("age")
    public val gender: Column<Gender> = IntEnumConverter.convert(this, "gender", Gender::class)
    // ...
}
```

##### 指定列使用类型转换器

通过列配置@Column中的converter属性，可以使用任意类型的转换器，通过此方式配置拥有最高的优先级，会覆盖全局配置的类型转换器。

代码示例:

```kotlin
@Table
data class User(
    @PrimaryKey
    var id: Int,
    var username: String,
    var age: Int,
    @org.ktorm.ksp.api.Column(converter = IntEnumConverter::class)
    var gender: Gender
)

enum class Gender {
    MALE,
    FEMALE
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

生成代码

```kotlin
public object Users : BaseTable<User>(tableName="User",entityClass=User::class) {
  public val id: Column<Int> = int("id").primaryKey()
  public val username: Column<String> = varchar("username")
  public val age: Column<Int> = int("age")
  public val gender: Column<Gender> = IntEnumConverter.convert(this,"gender",Gender::class)
  // ...
}
```

### 方法/属性生成器

...

#### 默认方法/属性生成器

...

#### 自定义方法/属性生成器

...