package org.ktorm.ksp.api

import org.ktorm.entity.Entity
import org.ktorm.schema.Table
import kotlin.reflect.KClass

/**
 * Specifies the mapped [org.ktorm.schema.Column] for a table property . If no Column annotation is specified, the
 * default values apply.
 *
 * In the entity class based on the [Entity] interface, the generated column property will add the bindTo method
 * to bind it to the entity property. The ordinary class entity class will not generate the bindTo method.
 *
 * @see [org.ktorm.schema.Column]
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
public annotation class Column(

    /**
     * Column names in SQL, Corresponds to [org.ktorm.schema.Column.name] property. If the value is an empty string,
     * the default value will be used. The [KtormKspConfig.namingStrategy] property can affect the default column
     * name generation rules, but the [columnName] property has the highest priority
     * @see [KtormKspConfig.namingStrategy]
     */
    val columnName: String = "",

    /**
     * Column converter，Used to declare the column property in the generated table, value must be a singleton type
     * or nothing. If the value is a Nothing::class, will try to find the appropriate column type automatically
     *
     * This property has the highest priority and overrides the default type conversion rules
     * and converter properties in [KtormKspConfig]
     *
     * @see [Converter]
     */
    val converter: KClass<out Converter> = Nothing::class,

    /**
     * property names in generate [Table]. If the value is an empty string, will use the name of the property
     * to which this annotation is added
     */
    val propertyName: String = "",


    /**
     * Generate the [Table.references] method for the table property, bind the column to the reference table,
     * and the reference table is the generated table of the property type.
     * Only entity class property based on the [Entity] interface can use references, and the referenced
     * entity class must also be an entity class based on the [Entity] interface.
     *
     * entity code
     * ```kotlin
     * @Table
     * public interface Employee : Entity<Employee> {
     *     @PrimaryKey
     *     public var id: Int
     *     public var name: String
     *     @Column(isReferences = true, columnName = "department_id")
     *     public var department: Department
     * }
     * ```
     * auto generate code
     * ```kotlin
     *  public object Employees: Table<Employee>(tableName="Employee",alias="",catalog="",schema="",
     *                                           entityClass=Employee::class) {
     *      public val id: Column<Int> = int("id").bindTo { it.id }.primaryKey()
     *      public val name: Column<String> = varchar("name").bindTo { it.name }
     *      public val department: Column<Int> = int("department_id").references(Departments) { it.department }
     *  }
     * ```
     * @see [org.ktorm.schema.Table.references]
     */
    val isReferences: Boolean = false
)
