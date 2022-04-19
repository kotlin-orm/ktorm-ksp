package org.ktorm.ksp.codegen

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

/**
 * Standard interface for generating table code
 */
public sealed interface TableCodeGenerator<T : Any> {

    /**
     * Generate table component code
     * @param context the table generate context, including the definition of table and its column,
     * global configuration, etc.
     * @param emitter After generating the specified component code, call this function to submit the code
     */
    public fun generate(context: TableGenerateContext, emitter: (T) -> Unit)
}

/**
 * Generate table type code
 */
public interface TableTypeGenerator: TableCodeGenerator<TypeSpec.Builder>

/**
 * Generate table properties code
 */
public interface TablePropertyGenerator : TableCodeGenerator<PropertySpec>

/**
 * Generate table functions code
 */
public interface TableFunctionGenerator : TableCodeGenerator<FunSpec>

/**
 * Generate top-level properties code in table file
 */
public interface TopLevelPropertyGenerator: TableCodeGenerator<PropertySpec>

/**
 * Generate top-level functions code in table file
 */
public interface TopLevelFunctionGenerator : TableCodeGenerator<FunSpec>