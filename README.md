# ktorm-ksp是什么？
Ktorm KSP用于帮助 [Ktorm](https://github.com/kotlin-orm/ktorm) 减少样板代码（基于 [ksp](https://github.com/google/ksp) 实现）。 只需要编写Entity类，自动生成相应的Table类。并可定义自动生成有用的扩展方法/属性

- 注意： 该项目还在进行开发中

# 特性

- 支持继承Entity接口定义的"实体"

- 支持 class/data class定义的"实体"，在ktorm中如果使用data class作为实体，会存在一些限制（可以通过[链接](https://www.ktorm.org/zh-cn/define-entities-as-any-kind-of-classes.html) 参考） 通过ktorm-ksp可以改善此问题，让ktorm更好的支持data class 

- 可自定义的代码生成配置，通过Java SPI机制可以扩展代码生成逻辑。

```kotlin
//custom entity
@Table
public data class Student(
    @PrimaryKey
    public var id: Int?,
    public var name: String,
    public var age: Int
)
//generate code
public object Students : BaseTable<Student>(tableName="Student",alias="", catalog="",
    schema="", entityClass=Student::class) {
        
    public val id: Column<Int> = int("id").primaryKey()
    
    public val name: Column<String> = varchar("name")
    
    public val age: Column<Int> = int("age")

    public override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean): Student {
        val instance = Student(id = row[id],name = row[name]!!,age = row[age]!!,)
        return instance
    }
}

public fun EntitySequence<Student, Students>.add(entity: Student): Int { ... }

public fun EntitySequence<Student, Students>.update(entity: Student):Int { ... }

public val Database.students: EntitySequence<Student, Students>
  get() = this.sequenceOf(Students)
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

### 方法/属性扩展

...

#### 默认方法/属性扩展

...

#### 自定义方法/属性扩展

...