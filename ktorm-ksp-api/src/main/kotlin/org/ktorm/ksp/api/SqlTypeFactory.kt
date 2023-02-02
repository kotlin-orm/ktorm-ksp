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

package org.ktorm.ksp.api

import org.ktorm.schema.SqlType
import kotlin.reflect.KProperty1

/**
 * Factory interface that creates [SqlType] instances from entity properties.
 */
public interface SqlTypeFactory {

    /**
     * Create a [SqlType] instance.
     */
    public fun <T : Any> createSqlType(property: KProperty1<*, T?>): SqlType<T>
}
