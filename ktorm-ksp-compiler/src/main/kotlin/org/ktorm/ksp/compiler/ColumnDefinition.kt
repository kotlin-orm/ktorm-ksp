package org.ktorm.ksp.compiler

import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeName

public data class ColumnDefinition (
    val columnName:String,
    val isPrimaryKey: Boolean,
    val columnType: TypeName,
    val property: MemberName,
)