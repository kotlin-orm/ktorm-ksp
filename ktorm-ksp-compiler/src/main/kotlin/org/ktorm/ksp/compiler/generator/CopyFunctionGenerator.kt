package org.ktorm.ksp.compiler.generator

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.toClassName
import org.ktorm.ksp.spi.TableMetadata

@OptIn(KotlinPoetKspPreview::class)
object CopyFunctionGenerator {

    fun generate(table: TableMetadata): FunSpec {
        return FunSpec.builder("copy")
            .addKdoc(
                "Return a deep copy of this entity (which has the same property values and tracked statuses), " +
                "and alter the specified property values. "
            )
            .receiver(table.entityClass.toClassName())
            .addParameters(PseudoConstructorFunctionGenerator.buildParameters(table))
            .returns(table.entityClass.toClassName())
            .addCode(PseudoConstructorFunctionGenerator.buildFunctionBody(table, isCopy = true))
            .build()
    }
}
