:us: English | :cn: [简体中文](README_cn.md)

# ktorm-ksp是什么？
[Ktorm](https://github.com/kotlin-orm/ktorm) KSP extension to help generate boilerplate code. It can automatically generate Table objects through entity classes, while making entities defined by data classes easier to use, and supports custom extension code generation logic.

- PS： The project is still in development

# Feature

- Just write the entity class and automatically generate the corresponding Table object. Support classes defined based on the Entity interface, as well as entities defined by ordinary class or data class
  
- Better support for class entity classes, the default implementation of the doCreateEntity method, and the add and update method of the entity sequence 

- Extensible code generation logic. Through the SPI mechanism, you only need to implement the specified interface, and you can write your own automatically generated logic.

```kotlin
//custom entity
@Table
public data class Student(
    @PrimaryKey
    public var id: Int?,
    public var name: String,
    public var age: Int
)
//auto generate code
public object Students : BaseTable<Student>(tableName="Student",alias="", catalog="",
    schema="", entityClass=Student::class) {
        
    public val id: Column<Int> = int("id").primaryKey()
    
    public val name: Column<String> = varchar("name")
    
    public val age: Column<Int> = int("age")

    public override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean): Student {
        return Student(id = row[id],name = row[name]!!,age = row[age]!!,)
    }
}

public fun EntitySequence<Student, Students>.add(entity: Student): Int { /* omit code */ }

public fun EntitySequence<Student, Students>.update(entity: Student):Int { /* omit code */ }

public val Database.students: EntitySequence<Student, Students> get() = this.sequenceOf(Students)
```

### Start

...

### Table Definition

...

#### interface entity

...

#### class/data class entity

...

#### NamingStyle

...

### TypeConverter

...

### property or function generator

...

#### default property or function generator

...

#### custom property or function generator

...