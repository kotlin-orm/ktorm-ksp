package org.ktorm.ksp.codegen

public interface TableCodeGenerator<T : Any> {
    public fun generate(context: TableGenerateContext, emitter: (T) -> Unit)
}