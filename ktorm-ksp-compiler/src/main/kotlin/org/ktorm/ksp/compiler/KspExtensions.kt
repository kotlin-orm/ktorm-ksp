package org.ktorm.ksp.compiler

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSTypeReference

public fun KSNode.ktormValidate(predicate: (KSNode?, KSNode) -> Boolean = { _, _ -> true }): Boolean {
    return this.accept(KtormValidateVisitor(predicate), null)
}

public fun KSClassDeclaration.findSuperTypeReference(name: String): KSTypeReference? {
    for (superType in this.superTypes) {
        val ksType = superType.resolve()
        val declaration = ksType.declaration
        if (declaration is KSClassDeclaration && declaration.qualifiedName!!.asString() == name) {
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
