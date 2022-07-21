/*
 * Copyright 2018-2021 the original author or authors.
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
import org.ktorm.ksp.codegen.generator.util.SuppressAnnotations
import org.ktorm.schema.BaseTable
import org.ktorm.schema.Table

public open class DefaultTableTypeGenerator : TableTypeGenerator {

    override fun generate(context: TableGenerateContext, emitter: (TypeSpec.Builder) -> Unit) {
        when (context.table.ktormEntityType) {
            KtormEntityType.ENTITY_INTERFACE -> generateEntityInterfaceEntity(context, emitter)
            KtormEntityType.ANY_KIND_CLASS -> generateAnyKindClassEntity(context, emitter)
        }
    }

    protected open fun buildTableNameParameter(table: TableDefinition, config: CodeGenerateConfig): List<CodeBlock> {
        val tableName = when {
            table.tableName.isNotEmpty() -> table.tableName
            config.namingStrategy != null && config.localNamingStrategy != null -> {
                config.localNamingStrategy.toTableName(table.entityClassName.simpleName)
            }
            config.namingStrategy == null -> {
                table.entityClassName.simpleName
            }
            else -> {
                return listOf(
                    CodeBlock.of(
                        "tableName·=·%T.toTableName(%S)",
                        config.namingStrategy,
                        table.entityClassName.simpleName
                    )
                )
            }
        }
        val result = mutableListOf<CodeBlock>()
        result.add(CodeBlock.of("tableName·=·%S", tableName))
        result.add(CodeBlock.of("alias·=·alias"))
        if (table.catalog.isNotEmpty()) {
            result.add(CodeBlock.of("catalog·=·%S", table.catalog))
        }
        if (table.schema.isNotEmpty()) {
            result.add(CodeBlock.of("schema·=·%S", table.schema))
        }
        result.add(CodeBlock.of("entityClass·=·%T::class", table.entityClassName))
        return result
    }

    public open fun generateEntityInterfaceEntity(context: TableGenerateContext, emitter: (TypeSpec.Builder) -> Unit) {
        val table = context.table
        TypeSpec.classBuilder(table.tableClassName)
            .superclass(Table::class.asClassName().parameterizedBy(table.entityClassName))
            .apply {
                buildTableNameParameter(table, context.config)
                    .forEach { addSuperclassConstructorParameter(it) }

                buildClassTable(table, this)
            }
            .run(emitter)
    }

    private fun buildClassTable(table: TableDefinition, typeSpec: TypeSpec.Builder) {
        typeSpec.addModifiers(KModifier.OPEN)
            .addAnnotation(
                SuppressAnnotations.buildSuppress(
                    SuppressAnnotations.leakingThis,
                    SuppressAnnotations.uncheckedCast
                )
            )
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter(
                        ParameterSpec.builder("alias", typeNameOf<String?>())
                            .defaultValue(if (table.alias.isNotEmpty()) "\"${table.alias}\"" else "null")
                            .build()
                    )
                    .build()
            )
            .addType(
                TypeSpec.companionObjectBuilder(null)
                    .superclass(table.tableClassName)
                    .build()
            )
            .addFunction(
                FunSpec.builder("aliased")
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
                buildTableNameParameter(table, context.config)
                    .forEach { addSuperclassConstructorParameter(it) }

                buildClassTable(table, this)
            }
            .run(emitter)
    }
}
