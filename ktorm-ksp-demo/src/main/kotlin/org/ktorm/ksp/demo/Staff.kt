package org.ktorm.ksp.demo

import PrimaryKey
import org.ktorm.entity.Entity
import org.ktorm.ksp.api.*
import org.ktorm.schema.BaseTable
import org.ktorm.schema.Column
import org.ktorm.schema.SqlType
import org.ktorm.schema.varchar
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
import java.time.LocalDate
import kotlin.reflect.KClass

@Table
public interface Staff : Entity<Staff> {
    @PrimaryKey
    public val id: Int
    public val name: String
    public val age: Int
    public val birthday: LocalDate

}

@Table(
    tableClassName = "EmployeeTable",
    ignoreColumns = ["updateTime"]
)
public data class Employee(
    @PrimaryKey
    public val id: Int,
    public val name: String,
    public val age: Int,
    public val birthday: LocalDate = LocalDate.now(),
    public val gender: Gender,
) {
    @Ignore
    public var createTime: LocalDate = LocalDate.now()
    public var updateTime: LocalDate = LocalDate.now()
}

@Table
public class Job {
    @PrimaryKey
    public var id: Int? = null
    public var name: String? = null
    @Ignore
    public var createTime: LocalDate = LocalDate.now()
}

public enum class Gender {
    MALE,
    FEMALE
}


@KtormKspConfig(
    enumConverter = IntEnumConverter::class,
    singleTypeConverters = [StringConverter::class],
    allowReflectionCreateEntity = false
)
public class KtormConfig

public interface CustomSingleTypeConverter: SingleTypeConverter<String> {
}

public object StringConverter:CustomSingleTypeConverter {
    override fun convert(table: BaseTable<*>, columnName: String, propertyType: KClass<String>): Column<String> {
        return table.varchar(columnName)
    }
}

public object IntEnumConverter: EnumConverter {
    public override fun <E : Enum<E>> convert(
        table: BaseTable<*>,
        columnName: String,
        propertyType: KClass<E>
    ): Column<E> {
        return table.registerColumn(columnName,IntEnumSqlType(propertyType.java))
    }
}

public class IntEnumSqlType<C : Enum<C>>(private val enumClass: Class<C>) : SqlType<C>(Types.INTEGER, "int") {
    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: C) {
        ps.setInt(index, parameter.ordinal)
    }

    override fun doGetResult(rs: ResultSet, index: Int): C? {
        return rs.getString(index)
            ?.toIntOrNull()
            ?.let { enumClass.enumConstants.getOrNull(it) }
    }
}