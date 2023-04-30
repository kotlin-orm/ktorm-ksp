/*
 * Copyright 2022-2023 the original author or authors.
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

package org.ktorm.ksp.compiler.util

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.visitor.KSValidateVisitor
import kotlin.reflect.jvm.jvmName

/**
 * Check if the given symbol is valid.
 */
fun KSNode.isValid(): Boolean {
    // Custom visitor to avoid stack overflow, see https://github.com/google/ksp/issues/1114
    val visitor = object : KSValidateVisitor({ _, _ -> true }) {
        private val stack = LinkedHashSet<KSType>()

        private fun validateType(type: KSType): Boolean {
            if (!stack.add(type)) {
                // Skip if the type already in the stack, avoid infinite recursion.
                return true
            }

            try {
                return !type.isError && !type.arguments.any { it.type?.accept(this, null) == false }
            } finally {
                stack.remove(type)
            }
        }

        override fun visitTypeReference(typeReference: KSTypeReference, data: KSNode?): Boolean {
            return validateType(typeReference.resolve())
        }

        override fun visitValueArgument(valueArgument: KSValueArgument, data: KSNode?): Boolean {
            fun visitValue(value: Any?): Boolean = when (value) {
                is KSType -> this.validateType(value)
                is KSAnnotation -> this.visitAnnotation(value, data)
                is List<*> -> value.all { visitValue(it) }
                else -> true
            }

            return visitValue(valueArgument.value)
        }
    }

    return this.accept(visitor, null)
}

/**
 * Check if this class is a subclass of [T].
 */
inline fun <reified T : Any> KSClassDeclaration.isSubclassOf(): Boolean {
    return findSuperTypeReference(T::class.jvmName) != null
}

/**
 * Find the specific super type reference for this class.
 */
fun KSClassDeclaration.findSuperTypeReference(name: String): KSTypeReference? {
    for (superType in this.superTypes) {
        val ksType = superType.resolve()
        val declaration = ksType.declaration

        if (declaration is KSClassDeclaration && declaration.qualifiedName?.asString() == name) {
            return superType
        }

        if (declaration is KSClassDeclaration) {
            val result = declaration.findSuperTypeReference(name)
            if (result != null) {
                return result
            }
        }
    }

    return null
}

/**
 * Check if this type is an inline class.
 */
@OptIn(KspExperimental::class)
fun KSType.isInline(): Boolean {
    val declaration = declaration as KSClassDeclaration
    return declaration.isAnnotationPresent(JvmInline::class) && declaration.modifiers.contains(Modifier.VALUE)
}
