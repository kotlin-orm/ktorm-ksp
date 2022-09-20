/*
 * Copyright 2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ktorm.ksp.codegen

import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.ClassName
import org.ktorm.ksp.api.KtormKspConfig
import org.ktorm.ksp.api.NamingStrategy

/**
 * @see [KtormKspConfig]
 */
public data class CodeGenerateConfig(
    public val allowReflectionCreateEntity: Boolean,
    public val configDependencyFile: KSFile?,
    public val namingStrategy: ClassName?,
    public val localNamingStrategy: NamingStrategy?,
    public val extensionGenerator: ExtensionGeneratorConfig
) {

    /**
     * The [CodeGenerateConfig] builder.
     */
    public class Builder {
        public var allowReflectionCreateEntity: Boolean = true
        public var configDependencyFile: KSFile? = null
        public var namingStrategy: ClassName? = null
        public var localNamingStrategy: NamingStrategy? = null
        public var extension: ExtensionGeneratorConfig = ExtensionGeneratorConfig()

        /**
         * Build the [CodeGenerateConfig] instance.
         */
        public fun build(): CodeGenerateConfig {
            return CodeGenerateConfig(
                allowReflectionCreateEntity,
                configDependencyFile,
                namingStrategy,
                localNamingStrategy,
                extension
            )
        }
    }
}

/**
 * Entity extension functions and properties generation configuration.
 */
public data class ExtensionGeneratorConfig(
    val enableSequenceOf: Boolean = true,
    val enableClassEntitySequenceAddFun: Boolean = true,
    val enableClassEntitySequenceUpdateFun: Boolean = true,
    val enableInterfaceEntitySimulationDataClass: Boolean = true
)
