/*
 * Copyright 2022 the original author or authors.
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

import org.ktorm.ksp.api.SqlTypeFactory
import org.ktorm.schema.SqlType
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.jvmErasure

public object LocationWrapperSqlType : SqlType<LocationWrapper>(Types.VARCHAR, "varchar") {

    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: LocationWrapper) {
        ps.setString(index, parameter.underlying)
    }

    override fun doGetResult(rs: ResultSet, index: Int): LocationWrapper? {
        return rs.getString(index)?.let { LocationWrapper(it) }
    }
}

public object IntEnumSqlTypeFactory : SqlTypeFactory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> createSqlType(property: KProperty1<*, T?>): SqlType<T> {
        val returnType = property.returnType.jvmErasure.java
        if (returnType.isEnum) {
            return IntEnumSqlType(returnType as Class<out Enum<*>>) as SqlType<T>
        } else {
            throw IllegalArgumentException("The property is required to be typed of enum but actually: $returnType")
        }
    }

    private class IntEnumSqlType<E : Enum<E>>(val enumClass: Class<E>) : SqlType<E>(Types.INTEGER, "int") {

        override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: E) {
            ps.setInt(index, parameter.ordinal)
        }

        override fun doGetResult(rs: ResultSet, index: Int): E? {
            return enumClass.enumConstants[rs.getInt(index)]
        }
    }
}
