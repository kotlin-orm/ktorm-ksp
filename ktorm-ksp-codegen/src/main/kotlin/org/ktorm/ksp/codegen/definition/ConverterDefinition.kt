package org.ktorm.ksp.codegen.definition

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import org.ktorm.ksp.api.Converter

/**
 * The definition of the converter, the converter here refers specifically to the [Converter] type
 */
public data class ConverterDefinition(

    /**
     * The type name of the converter
     */
    public val converterName: ClassName,

    /**
     * Type declaration for converters
     */
    public val converterClassDeclaration: KSClassDeclaration
)