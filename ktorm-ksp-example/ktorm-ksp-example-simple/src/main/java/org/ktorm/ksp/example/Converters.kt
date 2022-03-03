package org.ktorm.ksp.example

import org.ktorm.ksp.api.EnumConverter
import org.ktorm.ksp.api.SingleTypeConverter
import org.ktorm.schema.BaseTable
import org.ktorm.schema.Column
import org.ktorm.schema.int
import org.ktorm.schema.varchar
import kotlin.reflect.KClass


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