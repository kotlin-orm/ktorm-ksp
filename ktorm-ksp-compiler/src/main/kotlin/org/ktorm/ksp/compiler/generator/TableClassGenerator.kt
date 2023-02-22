package org.ktorm.ksp.compiler.generator

import com.google.devtools.ksp.symbol.ClassKind
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.toClassName
import org.ktorm.ksp.spi.TableMetadata
import org.ktorm.schema.BaseTable
import org.ktorm.schema.Table

@OptIn(KotlinPoetKspPreview::class)
object TableClassGenerator {

    fun generate(table: TableMetadata): TypeSpec {
        return TypeSpec.classBuilder(table.tableClassName)
            .addKdoc("Table %L. %L", table.name, table.entityClass.docString?.trimIndent().orEmpty())
            .addModifiers(KModifier.OPEN)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter("alias", typeNameOf<String?>())
                    .build()
            )
            .superclass(
                if (table.entityClass.classKind == ClassKind.INTERFACE) {
                    Table::class.asClassName().parameterizedBy(table.entityClass.toClassName())
                } else {
                    BaseTable::class.asClassName().parameterizedBy(table.entityClass.toClassName())
                }
            )
            .also { typeSpec ->
                typeSpec.addSuperclassConstructorParameter("%S", table.name)
                typeSpec.addSuperclassConstructorParameter("alias")

                if (table.catalog != null) {
                    typeSpec.addSuperclassConstructorParameter("catalog·=·%S", table.catalog!!)
                }

                if (table.schema != null) {
                    typeSpec.addSuperclassConstructorParameter("schema·=·%S", table.schema!!)
                }
            }
            .addFunction(
                FunSpec.builder("aliased")
                    .addKdoc(
                        "Return a new-created table object with all properties (including the table name and columns " +
                        "and so on) being copied from this table, but applying a new alias given by the parameter."
                    )
                    .addModifiers(KModifier.OVERRIDE)
                    .addParameter("alias", typeNameOf<String>())
                    .returns(ClassName(table.entityClass.packageName.asString(), table.tableClassName))
                    .addCode("return %T(alias)", table.tableClassName)
                    .build()
            )
            .addType(
                TypeSpec.companionObjectBuilder(null)
                    .addKdoc("The default table object of %L.", table.name)
                    .superclass(ClassName(table.entityClass.packageName.asString(), table.tableClassName))
                    .addSuperclassConstructorParameter(CodeBlock.of("alias·=·%S", table.alias))
                    .build()
            )
            .build()
    }
}
