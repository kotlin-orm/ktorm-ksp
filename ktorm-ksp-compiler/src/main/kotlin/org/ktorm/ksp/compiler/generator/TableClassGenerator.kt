package org.ktorm.ksp.compiler.generator

import com.google.devtools.ksp.symbol.ClassKind
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import org.ktorm.ksp.compiler.util.toRegisterCodeBlock
import org.ktorm.ksp.spi.ColumnMetadata
import org.ktorm.ksp.spi.TableMetadata
import org.ktorm.schema.BaseTable
import org.ktorm.schema.Column
import org.ktorm.schema.Table

@OptIn(KotlinPoetKspPreview::class)
object TableClassGenerator {

    fun generate(table: TableMetadata): TypeSpec {
        return TypeSpec.classBuilder(table.tableClassName)
            .addKdoc("Table %L. %L", table.name, table.entityClass.docString?.trimIndent().orEmpty())
            .addModifiers(KModifier.OPEN)
            .primaryConstructor(FunSpec.constructorBuilder().addParameter("alias", typeNameOf<String?>()).build())
            .configureSuperClass(table)
            .configureColumnProperties(table)
            .configureAliasedFunction(table)
            .configureCompanionObject(table)
            .build()
    }

    private fun TypeSpec.Builder.configureSuperClass(table: TableMetadata): TypeSpec.Builder {
        if (table.entityClass.classKind == ClassKind.INTERFACE) {
            superclass(Table::class.asClassName().parameterizedBy(table.entityClass.toClassName()))
        } else {
            superclass(BaseTable::class.asClassName().parameterizedBy(table.entityClass.toClassName()))
        }

        addSuperclassConstructorParameter("%S", table.name)
        addSuperclassConstructorParameter("alias")

        if (table.catalog != null) {
            addSuperclassConstructorParameter("catalog·=·%S", table.catalog!!)
        }

        if (table.schema != null) {
            addSuperclassConstructorParameter("schema·=·%S", table.schema!!)
        }

        return this
    }

    private fun TypeSpec.Builder.configureColumnProperties(table: TableMetadata): TypeSpec.Builder {
        for (column in table.columns) {
            val propertySpec = PropertySpec.builder(column.columnPropertyName, column.getColumnType())
                .addKdoc("Column %L. %L", column.name, column.entityProperty.docString?.trimIndent().orEmpty())
                .initializer(buildCodeBlock {
                    add(column.toRegisterCodeBlock())

                    if (column.isPrimaryKey) {
                        add(".primaryKey()")
                    }

                    if (table.entityClass.classKind == ClassKind.INTERFACE) {
                        if (column.isReference) {
                            val pkg = column.referenceTable!!.entityClass.packageName.asString()
                            val name = column.referenceTable!!.tableClassName
                            val propName = column.entityProperty.simpleName.asString()
                            add(".references(%T)·{·it.%N·}", ClassName(pkg, name), propName)
                        } else {
                            add(".bindTo·{·it.%N·}", column.entityProperty.simpleName.asString())
                        }
                    }
                })
                .build()

            addProperty(propertySpec)
        }

        return this
    }

    private fun ColumnMetadata.getColumnType(): TypeName {
        if (isReference) {
            return referenceTable!!.columns.single { it.isPrimaryKey }.getColumnType()
        } else {
            val propType = entityProperty.type.resolve().makeNotNullable().toTypeName()
            return Column::class.asClassName().parameterizedBy(propType)
        }
    }

    private fun TypeSpec.Builder.configureAliasedFunction(table: TableMetadata): TypeSpec.Builder {
        val func = FunSpec.builder("aliased")
            .addKdoc(
                "Return a new-created table object with all properties (including the table name and columns " +
                "and so on) being copied from this table, but applying a new alias given by the parameter."
            )
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("alias", typeNameOf<String>())
            .returns(ClassName(table.entityClass.packageName.asString(), table.tableClassName))
            .addCode("return %L(alias)", table.tableClassName)
            .build()

        addFunction(func)
        return this
    }

    private fun TypeSpec.Builder.configureCompanionObject(table: TableMetadata): TypeSpec.Builder {
        val companion = TypeSpec.companionObjectBuilder(null)
            .addKdoc("The default table object of %L.", table.name)
            .superclass(ClassName(table.entityClass.packageName.asString(), table.tableClassName))
            .addSuperclassConstructorParameter(CodeBlock.of("alias·=·%S", table.alias))
            .build()

        addType(companion)
        return this
    }
}
