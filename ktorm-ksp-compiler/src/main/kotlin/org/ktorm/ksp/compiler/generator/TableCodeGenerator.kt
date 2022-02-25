package org.ktorm.ksp.compiler.generator

public interface TableCodeGenerator<T : Any> {
    public fun generate(context: TableGenerateContext, emitter: (T) -> Unit)
}