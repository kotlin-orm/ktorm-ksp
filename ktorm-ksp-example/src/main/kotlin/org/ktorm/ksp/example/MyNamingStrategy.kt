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

package org.ktorm.ksp.example

import org.ktorm.ksp.api.CamelCaseToSnakeCaseNamingStrategy
import org.ktorm.ksp.api.NamingStrategy

public object MyNamingStrategy : NamingStrategy {
    override fun toTableName(entityClassName: String): String {
        return "t_" + CamelCaseToSnakeCaseNamingStrategy.toTableName(entityClassName)
    }

    override fun toColumnName(propertyName: String): String {
        return CamelCaseToSnakeCaseNamingStrategy.toColumnName(propertyName)
    }
}
