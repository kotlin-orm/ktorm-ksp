package org.ktorm.ksp.compiler.generator

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.asClassName
import org.ktorm.database.Database
import org.ktorm.entity.EntitySequence

public class SequencePropertyGenerator : TableCodeGenerator<PropertySpec> {
    override fun generate(context: TableGenerateContext, emitter: (PropertySpec) -> Unit) {
        val table = context.table
        val sequenceOf = MemberName("org.ktorm.entity", "sequenceOf", true)
        val tableClassName = table.tableClassName.simpleName
        val sequenceName = tableClassName.substring(0, 1).lowercase() + tableClassName.substring(1)
        val entitySequence = EntitySequence::class.asClassName()
        //EntitySequence<E, T>
        val sequenceType = entitySequence.parameterizedBy(table.entityClassName, table.tableClassName)
        PropertySpec.builder(sequenceName, sequenceType)
            .receiver(Database::class.asClassName())
            .getter(
                FunSpec.getterBuilder()
                    .addStatement("return this.%M(%T)", sequenceOf, table.tableClassName)
                    .build()
            )
            .build()
            .run(emitter)
    }
}