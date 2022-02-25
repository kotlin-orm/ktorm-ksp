package org.ktorm.ksp.compiler.definition

import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.ClassName
import org.ktorm.ksp.api.KtormKspConfig
import org.ktorm.ksp.api.NamingStrategy

/**
 * @see [KtormKspConfig]
 */
public data class CodeGenerateConfig(
    public val allowReflectionCreateEntity: Boolean,
    public val enumConverter: ConverterDefinition?,
    public val singleTypeConverters: Map<ClassName, ConverterDefinition>,
    public val configDependencyFile: KSFile?,
    public val namingStrategy: ClassName?,
    public val localNamingStrategy: NamingStrategy?
) {
    public class Builder {
        public var allowReflectionCreateEntity: Boolean = true
        public var enumConverter: ConverterDefinition? = null
        public var singleTypeConverters: Map<ClassName, ConverterDefinition> = emptyMap()
        public var configDependencyFile: KSFile? = null
        public var namingStrategy: ClassName? = null
        public var localNamingStrategy: NamingStrategy? = null

        public fun build(): CodeGenerateConfig {
            return CodeGenerateConfig(
                allowReflectionCreateEntity,
                enumConverter,
                singleTypeConverters,
                configDependencyFile,
                namingStrategy,
                localNamingStrategy
            )
        }
    }
}