package org.ktorm.ksp.codegen

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

public interface TableCodeGenerator<T : Any> {
    public fun generate(context: TableGenerateContext, emitter: (T) -> Unit)
}

public interface TableTypeGenerator: TableCodeGenerator<TypeSpec.Builder>

public interface TablePropertyGenerator : TableCodeGenerator<PropertySpec>

public interface TableFunctionGenerator : TableCodeGenerator<FunSpec>

public interface TopLevelPropertyGenerator: TableCodeGenerator<PropertySpec>

public interface TopLevelFunctionGenerator : TableCodeGenerator<FunSpec>