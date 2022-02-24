package org.ktorm.ksp.compiler

import com.google.devtools.ksp.symbol.KSNode

public fun KSNode.ktormValidate(predicate: (KSNode?, KSNode) -> Boolean = { _, _ -> true }): Boolean {
    return this.accept(KtormValidateVisitor(predicate), null)
}
