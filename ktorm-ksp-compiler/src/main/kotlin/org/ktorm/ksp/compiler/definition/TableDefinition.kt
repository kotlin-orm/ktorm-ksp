package org.ktorm.ksp.compiler.definition

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.ClassName

public data class TableDefinition(
    val tableName:String,
    val tableClassName: ClassName,
    val alias: String,
    val catalog: String,
    val schema: String,
    val entityClassName: ClassName,
    val columns: List<ColumnDefinition>,
    val entityFile: KSFile,
    val entityClassDeclaration: KSClassDeclaration,
)