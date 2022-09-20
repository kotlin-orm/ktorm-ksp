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

package org.ktorm.ksp.codegen.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.ktorm.ksp.codegen.CodeGenerateConfig
import org.ktorm.ksp.codegen.TableGenerateContext
import org.ktorm.ksp.codegen.TableTypeGenerator
import org.ktorm.ksp.codegen.definition.KtormEntityType
import org.ktorm.ksp.codegen.definition.TableDefinition
import org.ktorm.schema.BaseTable
import org.ktorm.schema.Table

public open class DefaultTableTypeGenerator : TableTypeGenerator {

    override fun generate(context: TableGenerateContext, emitter: (TypeSpec.Builder) -> Unit) {
        when (context.table.ktormEntityType) {
            KtormEntityType.ENTITY_INTERFACE -> generateEntityInterfaceEntity(context, emitter)
            KtormEntityType.ANY_KIND_CLASS -> generateAnyKindClassEntity(context, emitter)
        }
    }

    protected open fun buildTableConstructorParams(table: TableDefinition, config: CodeGenerateConfig): List<CodeBlock> {
        val tableNameParam = when {
            table.tableName.isNotEmpty() -> {
                CodeBlock.of("%S", table.tableName)
            }
            config.namingStrategy == null -> {
                CodeBlock.of("%S", table.entityClassName.simpleName)
            }
            config.localNamingStrategy != null -> {
                CodeBlock.of("%S", config.localNamingStrategy.toTableName(table.entityClassName.simpleName))
            }
            else -> {
                CodeBlock.of("%T.toTableName(%S)", config.namingStrategy, table.entityClassName.simpleName)
            }
        }

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

    public open fun generateEntityInterfaceEntity(context: TableGenerateContext, emitter: (TypeSpec.Builder) -> Unit) {
        val table = context.table
        TypeSpec.classBuilder(table.tableClassName)
            .superclass(Table::class.asClassName().parameterizedBy(table.entityClassName))
            .apply {
                buildTableConstructorParams(table, context.config)
                    .forEach { addSuperclassConstructorParameter(it) }

                buildClassTable(table, context.config, this)
            }
            .run(emitter)
    }

    private fun buildClassTable(table: TableDefinition, config: CodeGenerateConfig, typeSpec: TypeSpec.Builder) {
        val tableName = when {
            table.tableName.isNotEmpty() -> table.tableName
            config.localNamingStrategy != null -> config.localNamingStrategy.toTableName(table.entityClassName.simpleName)
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
                    .addSuperclassConstructorParameter(CodeBlock.of("alias·=·%S", table.alias.takeIf { it.isNotBlank() }))
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

    public open fun generateAnyKindClassEntity(context: TableGenerateContext, emitter: (TypeSpec.Builder) -> Unit) {
        val table = context.table
        TypeSpec.classBuilder(table.tableClassName)
            .superclass(BaseTable::class.asClassName().parameterizedBy(table.entityClassName))
            .apply {
                buildTableConstructorParams(table, context.config)
                    .forEach { addSuperclassConstructorParameter(it) }

                buildClassTable(table, context.config, this)
            }
            .run(emitter)
    }
}
