@file:OptIn(ExperimentalStdlibApi::class, KotlinPoetKspPreview::class)

package org.ktorm.ksp.compiler

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import org.ktorm.ksp.annotation.KtormColumn
import org.ktorm.ksp.annotation.KtormTable

public class KtormProcessorProvider: SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        environment.logger.info("create ktorm symbolProcessor")
        return KtormProcessor(environment.codeGenerator,environment.logger)
    }
}

public class KtormProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        logger.info("start ktorm processor")
        val symbols = resolver.getSymbolsWithAnnotation(KtormTable::class.qualifiedName!!)
        logger.info("symbols:${symbols.toList()}")
        val tableDefinitions = mutableListOf<TableDefinition>()
        val ret = symbols.filter { !it.validate() }.toList()
         symbols
             .filter { it is KSClassDeclaration && it.validate() }
             .forEach { it.accept(TableVisitor(tableDefinitions), Unit) }
         val columnFunctions = KtormColumnFunctions()
         KtormCodeGenerator().generate(tableDefinitions, codeGenerator, columnFunctions,logger)
        return ret
    }


    public inner class TableVisitor(
        private val tableDefinitions: MutableList<TableDefinition>,
    ) : KSVisitorVoid() {

        @OptIn(KspExperimental::class)
        override fun visitClassDeclaration(
            classDeclaration: KSClassDeclaration,
            data: Unit,
        ) {

            val entityClassName = classDeclaration.toClassName()
            val ktormTable = classDeclaration.getAnnotationsByType(KtormTable::class).first()
            val tableClassName = ClassName(entityClassName.packageName, entityClassName.simpleName + "s")

            val tableName = ktormTable.tableClassName.ifEmpty { entityClassName.simpleName }
            val columnDefs = classDeclaration
                .getAllProperties()
                .map {
                    val propertyKSType = it.type.resolve()
                    val columnAnnotation = it.getAnnotationsByType(KtormColumn::class).firstOrNull()
                    val columnName = columnAnnotation?.columnName ?: it.simpleName.asString()
                    val isPrimaryKey = columnAnnotation?.isPrimaryKey ?: false
                    ColumnDefinition(
                        columnName,
                        isPrimaryKey,
                        propertyKSType.toTypeName(),
                        MemberName(tableClassName, it.simpleName.asString()),
                    )
                }
                .toList()

            val tableDef = TableDefinition(
                tableName,
                tableClassName,
                entityClassName,
                columnDefs,
                classDeclaration.containingFile!!,
            )
            tableDefinitions.add(tableDef)
        }

    }
}