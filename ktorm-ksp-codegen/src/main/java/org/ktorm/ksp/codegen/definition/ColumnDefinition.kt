package org.ktorm.ksp.codegen.definition

import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName

// todo reference support
public data class ColumnDefinition(
    val columnName: String,
    val isPrimaryKey: Boolean,
    val propertyClassName: ClassName,
    val entityPropertyName: MemberName,
    val tablePropertyName: MemberName,
    val converterDefinition: ConverterDefinition?,
    val propertyDeclaration: KSPropertyDeclaration,
    val propertyType: KSType,
    val tableDefinition: TableDefinition,
    val isReferences: Boolean,
    var referencesColumn: ColumnDefinition?
) {

    val isMutable: Boolean = propertyDeclaration.isMutable
    val isNullable: Boolean = propertyType.nullability != Nullability.NOT_NULL
    val isEnum: Boolean = (propertyType.declaration as KSClassDeclaration).classKind == ClassKind.ENUM_CLASS

    override fun toString(): String {
        return "ColumnDefinition(columnName='$columnName', isPrimaryKey=$isPrimaryKey, propertyClassName=$propertyClassName, entityPropertyName=$entityPropertyName, tablePropertyName=$tablePropertyName, converterDefinition=$converterDefinition, referencesColumn=$referencesColumn)"
    }

}