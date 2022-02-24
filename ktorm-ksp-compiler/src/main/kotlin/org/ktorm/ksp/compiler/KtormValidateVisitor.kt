package org.ktorm.ksp.compiler

import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.visitor.KSValidateVisitor

/**
 * Fixed an issue where [KSValidateVisitor] would sometimes trigger infinite recursion
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