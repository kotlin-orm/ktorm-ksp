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
public object Students : BaseTable<Student>(tableName="Student",entityClass=Student::class,) {
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

public fun EntitySequence<Student, Students>.add(entity: Student): Int { /*此处省略具体实现*/ }

public fun EntitySequence<Student, Students>.update(entity: Student):Int { /*此处省略具体实现*/ }

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
public object Students : BaseTable<Student>(tableName="Student",entityClass=Student::class) {
    // 此处省略具体实现
}
public fun EntitySequence<Student, Students>.add(entity: Student): Int { /*此处省略具体实现*/ }
public fun EntitySequence<Student, Students>.update(entity: Student):Int { /*此处省略具体实现*/ }
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
public object Students : Table<Student>(tableName="Student",entityClass=Student::class) {
    // 此处省略具体实现
}
public val Database.students: EntitySequence<Student, Students> get() = this.sequenceOf(Students)
```
与entity实体生成的表不同的地方在于，表继承了Table类而不是BaseTable类，因此无需实现doCreateEntity方法. 也因此无需生成EntitySequence的add update扩展方法（因为已经有了）
```kotlin
val users = database.users.toList()
```

#### 命名风格

在默认情况下，生成表类中的表名，取实体类类名。列名取对应实体类中的属性名称。
有两种方式可以修改生成的名称，第一种方式是单独配置名称:

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
public object Students : Table<Student>(tableName="t_student",entityClass=Student::class) {
  public val id: Column<Int> = int("id").bindTo { it.id }.primaryKey()
  public val name: Column<String> = varchar("student_name").bindTo { it.name }
  public val age: Column<Int> = int("age").bindTo { it.age }
}
```

通过这种方式配置的表名/列名拥有最高优先级，不受下面的第二种方式配置影响。

第二种方式是全局配置命名风格策略：

在任意类上添加@KtormKspConfig注解配置（注意项目中只能声明一次此注解）并赋值namingStrategy属性，此属性需要一个实现NamingStrategy接口的**单例对象**, 在ktorm-ksp中自带了驼峰转小写下划线风格的命名风格策略： CamelCaseToLowerCaseUnderscoresNamingStrategy

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
    Table<StudentProfile>(tableName="student_profile",entityClass=StudentProfile::class) {
  public val id: Column<Int> = int("id").bindTo { it.id }.primaryKey()
  public val firstName: Column<String> = varchar("first_name").bindTo { it.firstName }
  public val lastName: Column<String> = varchar("last_name").bindTo { it.lastName }
  public val telephoneNumber: Column<String> =
      varchar("telephone_number").bindTo { it.telephoneNumber }
}
```

### 类型转换器

...

### 方法/属性生成器

...

#### 默认方法/属性生成器

...

#### 自定义方法/属性生成器

...