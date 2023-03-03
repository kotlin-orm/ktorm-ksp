package org.ktorm.ksp.compiler.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.toClassName
import org.ktorm.database.Database
import org.ktorm.entity.EntitySequence
import org.ktorm.ksp.spi.TableMetadata

@OptIn(KotlinPoetKspPreview::class)
object EntitySequencePropertyGenerator {

    fun generate(table: TableMetadata): PropertySpec {
        val entityClass = table.entityClass.toClassName()
        val tableClass = ClassName(table.entityClass.packageName.asString(), table.tableClassName)
        val entitySequenceType = EntitySequence::class.asClassName().parameterizedBy(entityClass, tableClass)

        return PropertySpec.builder(table.entitySequenceName, entitySequenceType)
            .addKdoc("Return the default entity sequence of [%L].", table.tableClassName)
            .receiver(Database::class.asClassName())
            .getter(
                FunSpec.getterBuilder()
                    .addStatement(
                        "return·this.%M(%T)",
                        MemberName("org.ktorm.entity", "sequenceOf", true),
                        table.tableClassName)
                    .build())
            .build()
    }
}