package org.ktorm.ksp.codegen.definition

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName

public data class ConverterDefinition(
    public val converterName: ClassName,
    public val converterClassDeclaration: KSClassDeclaration
)