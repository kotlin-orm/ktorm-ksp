@file:OptIn(ExperimentalStdlibApi::class, KotlinPoetKspPreview::class)

package org.ktorm.ksp.compiler

import Id
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import org.ktorm.ksp.annotation.Column
import org.ktorm.ksp.annotation.Table

public class KtormProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        environment.logger.info("create ktorm symbolProcessor")
        return KtormProcessor(environment)
    }
}

public class KtormProcessor(
    private val environment: SymbolProcessorEnvironment
) : SymbolProcessor {

    private val logger = environment.logger

    override fun process(resolver: Resolver): List<KSAnnotated> {
        logger.info("start ktorm ksp processor")
        val configuration = parseOptions()
        logger.info("configuration: $configuration")
        val symbols = resolver.getSymbolsWithAnnotation(Table::class.qualifiedName!!)
        logger.info("symbols:${symbols.toList()}")
        val tableDefinitions = mutableListOf<TableDefinition>()
        val ret = symbols.filter { !it.validate() }.toList()
        symbols
            .filter { it is KSClassDeclaration && it.validate() }
            .forEach { it.accept(EntityVisitor(tableDefinitions), Unit) }
        KtormCodeGenerator().generate(tableDefinitions, environment.codeGenerator, configuration, logger)
        return ret
    }

    private fun parseOptions(): KtormKspConfiguration {
        val options = environment.options
        val allowReflectionCreateEntity = options["allowReflectionCreateEntity"]?.toBoolean() ?: true
        return KtormKspConfiguration(
            allowReflectionCreateEntity
        )
    }


    public inner class EntityVisitor(
        private val tableDefinitions: MutableList<TableDefinition>,
    ) : KSVisitorVoid() {

        @OptIn(KspExperimental::class)
        override fun visitClassDeclaration(
            classDeclaration: KSClassDeclaration,
            data: Unit,
        ) {

            val entityClassName = classDeclaration.toClassName()
            val table = classDeclaration.getAnnotationsByType(Table::class).first()
            val tableClassName = ClassName(entityClassName.packageName, entityClassName.simpleName + "s")

            val tableName = table.tableClassName.ifEmpty { entityClassName.simpleName }

            val columnDefs = classDeclaration
                .getAllProperties()
                .mapNotNull {
                    val propertyKSType = it.type.resolve()
                    val propertyName = it.simpleName.asString()
                    if (it.isAnnotationPresent(Transient::class) || propertyName in table.transientColumns) {
                        return@mapNotNull null
                    }
                    val columnAnnotation = it.getAnnotationsByType(Column::class).firstOrNull()
                    val isId = it.getAnnotationsByType(Id::class).any()
                    val columnName = columnAnnotation?.columnName ?: propertyName
                    ColumnDefinition(
                        columnName,
                        isId,
                        it,
                        propertyKSType.toTypeName(),
                        MemberName(tableClassName, propertyName),
                    )
                }
                .toList()

            val tableDef = TableDefinition(
                tableName,
                tableClassName,
                table.alias,
                table.catalog,
                table.schema,
                entityClassName,
                columnDefs,
                classDeclaration.containingFile!!,
                classDeclaration
            )
            tableDefinitions.add(tableDef)
        }

    }
}