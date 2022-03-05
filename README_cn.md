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
public object Students : BaseTable<Student>(tableName="Student",alias="", catalog="",
    schema="", entityClass=Student::class) {
        
    public val id: Column<Int> = int("id").primaryKey()
    
    public val name: Column<String> = varchar("name")
    
    public val age: Column<Int> = int("age")

    public override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean): Student {
        return Student(id = row[id],name = row[name]!!,age = row[age]!!,)
    }
}

public fun EntitySequence<Student, Students>.add(entity: Student): Int { /*此处省略具体实现*/ }

public fun EntitySequence<Student, Students>.update(entity: Student):Int { /*此处省略具体实现*/ }

public val Database.students: EntitySequence<Student, Students> get() = this.sequenceOf(Students)
```
```


### 快速入门

...

### 实体定义

...

#### interface 实体

...

#### class/data class 实体

...

#### 命名风格

...

### 类型转换器

...

### 方法/属性生成器

...

#### 默认方法/属性生成器

...

#### 自定义方法/属性生成器

...