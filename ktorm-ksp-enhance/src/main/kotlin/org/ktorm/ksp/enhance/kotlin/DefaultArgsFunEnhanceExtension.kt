/*
 * Copyright 2018-2021 the original author or authors.
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

package org.ktorm.ksp.enhance.kotlin

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.parents
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.putArgument
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.getArgumentsWithIr
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.ktorm.ksp.api.DefaultArgsImplementationFunction
import org.ktorm.ksp.api.DefaultArgsVirtualFunction
import org.ktorm.ksp.codegen.generator.util.CodeFactory
import kotlin.math.ceil

public class DefaultArgsFunEnhanceExtension : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        moduleFragment.accept(DefaultArgsFunEnhanceInjector(pluginContext), null)
    }
}

/**
 * Replace expressions that call functions annotated with [DefaultArgsVirtualFunction] with calls to
 * corresponding expressions annotated with [DefaultArgsImplementationFunction].
 *
 * e.g:
 * ```kotlin
 * @KtormKspDefaultArgsVirtualFunction
 * public fun User(username: String = "", age: Int = 0)
 *
 * @KtormKspDefaultArgsImplementationFunction
 * public fun $User$implementation(username: String? = null, age:Int? = null, flag: Int)
 * ```
 *
 * When the "User" function is called, it will be replaced by the "$User$implementation" function after compilation.
 *
 * ```kotlin
 * // before compilation
 * User(username = "jack")
 *
 * // after compilation
 * $User$implementation(username = "jack", age = null, flag = 1)
 * ```
 * When calling the "User" function, for parameters without parameters, after calling "$User$implementation",
 * the parameters will be replaced by null. Therefore, other parameters of "$User$implementation" except flag
 * must be nullable
 *
 *
 * The flag parameter of the $User$implementation function is a bit flag, which indicates which parameters
 * are passed in when the User function is called. The bit value of the passed parameter is 1, and the bit
 * value of no parameter is 0.
 *
 * Since the flag is an int type with only 32 bits, when the number of parameters of the "User" function exceeds 32,
 * the "$User$implementation" function needs to have multiple flags to jointly indicate which parameters are passed.
 * ```kotlin
 * @KtormKspDefaultArgsVirtualFunction
 * public fun User(arg1: String = "", //....//  arg33: String = "") // 拥有33个参数
 *
 * @KtormKspDefaultArgsImplementationFunction
 * public fun $User$implementation(arg1: String?, //....// arg33: String?, flag1: Int, flag2: Int)
 * ```
 *
 * This compiler plugin borrows kotlin compiler default parameter implementation.
 * @see org.jetbrains.kotlin.backend.common.lower.DefaultParameterInjector
 */
public class DefaultArgsFunEnhanceInjector(
    private val pluginContext: IrPluginContext
) : IrElementTransformerVoid() {

    private val virtualFunctionAnnotation = FqName(DefaultArgsVirtualFunction::class.java.canonicalName)
    private val implementationFunctionAnnotation =
        FqName(DefaultArgsImplementationFunction::class.java.canonicalName)

    override fun visitCall(expression: IrCall): IrExpression {
        expression.transformChildrenVoid()
        return visitIrFunctionAccessExpression(expression) { implementationFunction, valueArgumentsCount ->
            IrCallImpl(
                expression.startOffset,
                expression.endOffset,
                expression.type,
                implementationFunction,
                expression.typeArgumentsCount,
                valueArgumentsCount,
                expression.origin,
                expression.superQualifierSymbol
            )
        }
    }

    private fun <T : IrFunctionAccessExpression> visitIrFunctionAccessExpression(
        expression: T,
        newExpressionBuilder: (implementationFunction: IrSimpleFunctionSymbol, valueArgumentsCount: Int) -> T
    ): T {
        val expressionFunction = expression.symbol.owner
        if (!expressionFunction.hasAnnotation(virtualFunctionAnnotation)) {
            return expression
        }
        val container = expressionFunction.parents.first { it is IrDeclarationContainer } as IrDeclarationContainer
        // Find implementation method declarations
        val implementationFunName =
            Name.identifier(CodeFactory.convertDefaultImplementationFunName(expressionFunction.name.asString()))
        val implementationFunction =
            container.declarations.firstOrNull {
                it is IrFunction
                        && it.name == implementationFunName
                        && it.hasAnnotation(implementationFunctionAnnotation)
            } as? IrFunction ?: return expression // implementationFunction not exists
        val virtualArgsCount = expression.valueArgumentsCount
        val flagCount = ceil(virtualArgsCount / 32.0).toInt()
        val implementationFunctionSymbol = implementationFunction.symbol as IrSimpleFunctionSymbol
        return newExpressionBuilder(implementationFunctionSymbol, virtualArgsCount + flagCount)
            .apply {
                this.extensionReceiver = expression.extensionReceiver
                this.dispatchReceiver = expression.dispatchReceiver
                val virtualArgs = expression.getArgumentsWithIr()
                val implementationArgs = implementationFunction.valueParameters
                val flags = IntArray(flagCount) { 0 }
                // actual parameter
                for (argument in virtualArgs) {
                    if (argument.first.index < 0) {
                        // This is a extensionReceiver or dispatchReceiver
                        continue
                    }
                    val flagIndex = argument.first.index / 32
                    flags[flagIndex] = flags[flagIndex] or (1 shl (argument.first.index % 32))
                    this.putArgument(argument.first, argument.second)
                }

                // flag parameters
                flags.forEachIndexed { index, flag ->
                    this.putValueArgument(
                        virtualArgsCount + index,
                        IrConstImpl.int(startOffset, endOffset, pluginContext.irBuiltIns.intType, flag)
                    )
                }

                // null parameters
                for (index in 0 until virtualArgsCount) {
                    val flag = flags[index / 32]
                    if (flag and (1 shl index) == 0) {
                        this.putValueArgument(
                            index,
                            IrConstImpl.constNull(startOffset, endOffset, implementationArgs[index].type)
                        )
                    }
                }
            }
    }
}
