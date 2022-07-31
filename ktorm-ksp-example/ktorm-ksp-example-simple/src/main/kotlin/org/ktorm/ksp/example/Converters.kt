/*
 * Copyright 2018-2021 the original author or authors.
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

import org.ktorm.ksp.api.EnumConverter
import org.ktorm.ksp.api.SingleTypeConverter
import org.ktorm.schema.*
import kotlin.reflect.KClass

public object UIntConverter : SingleTypeConverter<UInt> {
    override fun convert(table: BaseTable<*>, columnName: String, propertyType: KClass<UInt>): Column<UInt> {
        return with(table) {
            int(columnName).transform({ it.toUInt() }, { it.toInt() })
        }
    }
}

public object LocationWrapperConverter : SingleTypeConverter<LocationWrapper> {
    override fun convert(
        table: BaseTable<*>,
        columnName: String,
        propertyType: KClass<LocationWrapper>
    ): Column<LocationWrapper> {
        return with(table) {
            varchar(columnName).transform({ LocationWrapper(it) }, { it.underlying })
        }
    }
}

public object IntEnumConverter : EnumConverter {
    override fun <E : Enum<E>> convert(table: BaseTable<*>, columnName: String, propertyType: KClass<E>): Column<E> {
        val values = propertyType.java.enumConstants
        return with(table) {
            int(columnName).transform({ values[it] }, { it.ordinal })
        }
    }
}
