package org.ktorm.ksp.codegen

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.withIndent

public fun CodeBlock.Builder.withIndent(count: Int, builderAction: CodeBlock.Builder.() -> Unit) {
    assert(count > 0)
    if (count == 1) {
        withIndent(builderAction)
    } else {
        withIndent {
            withIndent(count - 1, builderAction)
        }
    }
}
