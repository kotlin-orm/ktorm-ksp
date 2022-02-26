package org.ktorm.ksp.codegen.definition

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.ClassName
import org.ktorm.entity.Entity
import org.ktorm.schema.BaseTable
import org.ktorm.schema.Table
import kotlin.reflect.KClass

public data class TableDefinition(
    val tableName: String,
    val tableClassName: ClassName,
    val alias: String,
    val catalog: String,
    val schema: String,
    val entityClassName: ClassName,
    val columns: List<ColumnDefinition>,
    val entityFile: KSFile,
    val entityClassDeclaration: KSClassDeclaration,
    val ktormEntityType: KtormEntityType
)

public enum class KtormEntityType(
    public val defaultTableSuperClass: KClass<out BaseTable<*>>
) {

    /**
     * Interface entity of inherited [Entity], whose table must be a subclass of [Table]
     */
    INTERFACE(Table::class),

    /**
     * Entity of any Class whose table must be a subclass of [BaseTable]
     */
    CLASS(BaseTable::class)
}