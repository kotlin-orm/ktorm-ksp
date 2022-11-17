/*
 * Copyright 2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ktorm.ksp.compiler.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.ktorm.ksp.compiler.generator.util.NameGenerator
import org.ktorm.ksp.spi.TableGenerateContext
import org.ktorm.ksp.spi.TableTypeGenerator
import org.ktorm.ksp.spi.definition.KtormEntityType
import org.ktorm.schema.BaseTable
import org.ktorm.schema.Table

public open class DefaultTableTypeGenerator : TableTypeGenerator {

    override fun generate(context: TableGenerateContext): TypeSpec.Builder {
        return when (context.table.ktormEntityType) {
            KtormEntityType.ENTITY_INTERFACE -> generateEntityInterfaceEntity(context)
            KtormEntityType.ANY_KIND_CLASS -> generateAnyKindClassEntity(context)
        }
    }

    protected open fun buildTableConstructorParams(context: TableGenerateContext): List<CodeBlock> {
        val table = context.table
        val tableNameParam = NameGenerator.generateSqlTableName(context)

        val params = ArrayList<CodeBlock>()
        params += tableNameParam
        params += CodeBlock.of("alias")

        if (table.catalog.isNotEmpty()) {
            params += CodeBlock.of("catalog·=·%S", table.catalog)
        }

        if (table.schema.isNotEmpty()) {
            params += CodeBlock.of("schema·=·%S", table.schema)
        }

        return params
    }

    public open fun generateEntityInterfaceEntity(context: TableGenerateContext): TypeSpec.Builder {
        val table = context.table
        return TypeSpec.classBuilder(table.tableClassName)
            .superclass(Table::class.asClassName().parameterizedBy(table.entityClassName))
            .apply {
                buildTableConstructorParams(context)
                    .forEach { addSuperclassConstructorParameter(it) }

                buildClassTable(context, this)
            }
    }

    private fun buildClassTable(context: TableGenerateContext, typeSpec: TypeSpec.Builder) {
        val (table, config, _, _) = context
        val localNamingStrategy = config.localNamingStrategy
        val tableName = when {
            table.tableName.isNotEmpty() -> table.tableName
            localNamingStrategy != null -> localNamingStrategy.toTableName(table.entityClassName.simpleName)
            else -> table.entityClassName.simpleName
        }

        typeSpec
            .addKdoc("Table %L. %L", tableName, table.entityClassDeclaration.docString?.trimIndent().orEmpty())
            .addModifiers(KModifier.OPEN)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter(ParameterSpec("alias", typeNameOf<String?>()))
                    .build()
            )
            .addType(
                TypeSpec.companionObjectBuilder(null)
                    .addKdoc("The default table object of %L.", tableName)
                    .superclass(table.tableClassName)
                    .addSuperclassConstructorParameter(
                        CodeBlock.of(
                            "alias·=·%S",
                            table.alias.takeIf { it.isNotBlank() })
                    )
                    .build()
            )
            .addFunction(
                FunSpec.builder("aliased")
                    .addKdoc(
                        "Return a new-created table object with all properties (including the table name and columns " +
                                "and so on) being copied from this table, but applying a new alias given by the parameter."
                    )
                    .returns(table.tableClassName)
                    .addParameter(ParameterSpec.builder("alias", typeNameOf<String>()).build())
                    .addModifiers(KModifier.OVERRIDE)
                    .addCode(
                        "return %T(alias)", table.tableClassName
                    )
                    .build()
            )
    }

    public open fun generateAnyKindClassEntity(context: TableGenerateContext): TypeSpec.Builder {
        val table = context.table
        return TypeSpec.classBuilder(table.tableClassName)
            .superclass(BaseTable::class.asClassName().parameterizedBy(table.entityClassName))
            .apply {
                buildTableConstructorParams(context)
                    .forEach { addSuperclassConstructorParameter(it) }

                buildClassTable(context, this)
            }
    }
}
