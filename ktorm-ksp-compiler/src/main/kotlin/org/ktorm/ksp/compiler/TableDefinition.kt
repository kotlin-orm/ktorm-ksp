package org.ktorm.ksp.compiler

import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.ClassName

public data class TableDefinition(
    val tableName:String,
    val tableClassName: ClassName,
    val entityClassName: ClassName,
    val columns: List<ColumnDefinition>,
    val entityFile: KSFile
)