package org.ktorm.ksp.compiler.definition

import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName

public data class ColumnDefinition(
    val columnName: String,
    val isPrimaryKey: Boolean,
    val propertyDeclaration: KSPropertyDeclaration,
    val propertyTypeName: ClassName,
    val property: MemberName,
    val converterDefinition: ConverterDefinition?
)