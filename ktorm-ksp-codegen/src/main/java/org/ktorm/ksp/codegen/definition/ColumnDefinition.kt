package org.ktorm.ksp.codegen.definition

import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Nullability
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName

public data class ColumnDefinition(
    val sqlColumnName: String,
    val isPrimaryKey: Boolean,
    val propertyDeclaration: KSPropertyDeclaration,
    val propertyTypeDeclaration: KSType,
    val propertyClassName: ClassName,
    val propertyMemberName: MemberName,
    val converterDefinition: ConverterDefinition?,
    val tableDefinition: TableDefinition
) {
    val propertyIsNullable: Boolean = propertyTypeDeclaration.nullability != Nullability.NOT_NULL
    val columnMemberName: MemberName = MemberName(tableDefinition.tableClassName, propertyMemberName.simpleName)

    override fun toString(): String {
        return "ColumnDefinition(sqlColumnName='$sqlColumnName', isPrimaryKey=$isPrimaryKey, propertyDeclaration=$propertyDeclaration, propertyTypeDeclaration=$propertyTypeDeclaration, propertyClassName=$propertyClassName, propertyMemberName=$propertyMemberName, converterDefinition=$converterDefinition, propertyIsNullable=$propertyIsNullable, columnMemberName=$columnMemberName)"
    }

}