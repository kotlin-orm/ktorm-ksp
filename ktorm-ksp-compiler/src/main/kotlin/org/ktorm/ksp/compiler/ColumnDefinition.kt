package org.ktorm.ksp.compiler

import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeName

public data class ColumnDefinition(
    val columnName: String,
    val isPrimaryKey: Boolean,
    val propertyDeclaration : KSPropertyDeclaration,
    val propertyTypeName: TypeName,
    val property: MemberName,
)