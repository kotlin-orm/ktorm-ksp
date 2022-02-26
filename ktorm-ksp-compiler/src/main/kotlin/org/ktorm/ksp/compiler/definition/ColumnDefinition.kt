package org.ktorm.ksp.compiler.definition

import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Nullability
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName

public data class ColumnDefinition(
    val columnName: String,
    val isPrimaryKey: Boolean,
    val propertyDeclaration: KSPropertyDeclaration,
    val propertyTypeDeclaration: KSType,
    val propertyClassName: ClassName,
    val property: MemberName,
    val converterDefinition: ConverterDefinition?
) {
    val propertyIsNullable:Boolean
        get() = propertyTypeDeclaration.nullability != Nullability.NOT_NULL
}