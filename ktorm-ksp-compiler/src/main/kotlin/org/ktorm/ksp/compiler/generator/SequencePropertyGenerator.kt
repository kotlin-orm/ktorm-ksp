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

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.asClassName
import org.ktorm.database.Database
import org.ktorm.entity.EntitySequence
import org.ktorm.ksp.codegen.TableGenerateContext
import org.ktorm.ksp.codegen.TopLevelPropertyGenerator

/**
 * Generate entity sequence extend property to [Database].
 * e.g:
 * ```kotlin
 * public val Database.customers: EntitySequence<Customer, Customers>
 *      get() = this.sequenceOf(Customers)
 * ```
 */
public class SequencePropertyGenerator : TopLevelPropertyGenerator {
    override fun generate(context: TableGenerateContext): List<PropertySpec> {
        val table = context.table
        val sequenceOf = MemberName("org.ktorm.entity", "sequenceOf", true)
        val tableClassName = table.tableClassName.simpleName
        val sequenceName = table.sequenceName.ifEmpty {
            tableClassName.substring(0, 1).lowercase() + tableClassName.substring(1)
        }
        val entitySequence = EntitySequence::class.asClassName()
        // EntitySequence<E, T>
        val sequenceType = entitySequence.parameterizedBy(table.entityClassName, table.tableClassName)

        val propertySpec = PropertySpec
            .builder(sequenceName, sequenceType)
            .addKdoc("Return the default entity sequence of [%L].", table.tableClassName.simpleName)
            .receiver(Database::class.asClassName())
            .getter(
                FunSpec.getterBuilder()
                    .addStatement("return·this.%M(%T)", sequenceOf, table.tableClassName)
                    .build()
            )
            .build()
        return listOf(propertySpec)
    }
}
