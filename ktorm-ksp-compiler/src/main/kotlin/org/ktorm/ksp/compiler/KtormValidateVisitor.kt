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

package org.ktorm.ksp.compiler

import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.visitor.KSValidateVisitor

/**
 * Fixed an issue where [KSValidateVisitor] would sometimes trigger infinite recursion.
 */
public class KtormValidateVisitor(
    predicate: (KSNode?, KSNode) -> Boolean
) : KSValidateVisitor(predicate) {

    private val typeSet = mutableSetOf<KSType>()

    private fun validateType(type: KSType): Boolean {
        if (type in typeSet) return true
        typeSet.add(type)
        return !type.isError && !type.arguments.any { it.type?.accept(this, null) == false }
    }

    override fun visitTypeReference(typeReference: KSTypeReference, data: KSNode?): Boolean {
        return validateType(typeReference.resolve())
    }
}
