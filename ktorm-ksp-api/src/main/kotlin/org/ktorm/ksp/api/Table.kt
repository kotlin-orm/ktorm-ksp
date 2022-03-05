package org.ktorm.ksp.api
import org.ktorm.entity.Entity
import org.ktorm.schema.BaseTable

/**
 * The specified class is an entity, and ksp will generate the corresponding [org.ktorm.schema.Table] or [BaseTable]
 * object This class can be an interface based on [Entity] and will generate [org.ktorm.schema.Table] objects.
 * It can also be a normal class/data class that will generate a [BaseTable] object
 *
 * For the above two different entity classes, the generated Table will also be different.
 * Among them, the [BaseTable] object generated based on the class will generate the [BaseTable.createEntity] method
 * implementation by default. And based on the [Entity] interface implementation [org.ktorm.schema.Table] does not
 * generate an implementation of this method. The generated column property definitions are also different.
 * For details, please refer to the description in [Column].
 *
 * By default, the class name of [BaseTable] or [org.ktorm.schema.Table] is generated, and the plural of nouns
 * is converted based on the class name. The generated class name can be modified by assigning the [tableClassName]
 * property
 *
 * @see BaseTable
 * @see org.ktorm.schema.Table
 *
 * @param tableName Specify the table tableName, corresponding to the [BaseTable.tableName] property. By default,
 * the class name of the entity class will be used as the table name, or you can use [KtormKspConfig] The
 * [NamingStrategy] configured in automatically maps table names. But [tableName] in this annotation has the
 * highest priority
 * @param tableClassName Specifies the class name of the generated table class. By default, the noun plural is
 * converted from the class name of the entity class.
 * @param alias Specify the table alias, corresponding to the [BaseTable.alias] property
 * @param catalog Specify the table catalog, corresponding to the [BaseTable.catalog] property
 * @param schema Specify the table schema, corresponding to the [BaseTable.schema] property
 * @param ignoreColumns Specifies to ignore properties that do not generate column definitions.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
public annotation class Table(
    val tableName: String = "",
    val tableClassName: String = "",
    val alias: String = "",
    val catalog: String = "",
    val schema: String = "",
    val ignoreColumns: Array<String> = []
)