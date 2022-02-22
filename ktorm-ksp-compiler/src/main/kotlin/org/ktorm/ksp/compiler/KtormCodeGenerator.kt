@file:OptIn(KotlinPoetKspPreview::class)

package org.ktorm.ksp.compiler

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.writeTo
import org.ktorm.schema.Column
import org.ktorm.schema.Table

public class KtormCodeGenerator {

    private val defaultTableSuperType = Table::class.asTypeName()
    private val ignoreDefinitionProperties = setOf("entityClass", "properties", "createTime", "updateTime", "deleted")

    public fun generate(
        tables: List<TableDefinition>,
        codeGenerator: CodeGenerator,
        columnFunctions: KtormColumnFunctions,
        logger: KSPLogger,
    ) {
        logger.info("generate tables:${tables.map { it.entityClassName.simpleName }}}")
        for (table in tables) {
            val (tableName, tableClassName, entityClassName, properties, entityFile) = table
            val superType = defaultTableSuperType

            FileSpec.builder(tableClassName.packageName, tableClassName.simpleName)
                .addType(
                    TypeSpec.objectBuilder(tableClassName)
                        .superclass(superType.parameterizedBy(entityClassName))
                        .addSuperclassConstructorParameter("\"$tableName\"")
                        .also {
                            val ktormColumn = Column::class.asClassName()
                            val bindTo = MemberName("", "bindTo")
                            val primaryKey = MemberName("", "primaryKey")
                            //property
                            for (column in properties) {
                                val (columnName, isPrimaryKey, columnType, property) = column
                                val tableColumnType = columnType.copy(nullable = false)
                                if (columnName in ignoreDefinitionProperties) continue
                                val columnFunction =
                                    columnFunctions.getColumnCode(property, tableColumnType)

                                val params = mapOf(
                                    "columnName" to columnName,
                                    "bindTo" to bindTo,
                                    "primaryKey" to primaryKey
                                )
                                val initializer = CodeBlock.builder()
                                    .add(columnFunction)
                                    .addNamed(".%bindTo:M { it.%columnName:L }", params)
                                    .apply { if (isPrimaryKey) addNamed(".%primaryKey:M()", params) }
                                    .build()
                                it.addProperty(
                                    PropertySpec.builder(columnName, ktormColumn.parameterizedBy(tableColumnType))
                                        .initializer(initializer)
                                        .build()
                                )
                            }
                        }
                        .build()
                )
                .build()
                .writeTo(codeGenerator, Dependencies(true, entityFile))
        }
    }

}