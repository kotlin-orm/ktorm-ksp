package org.ktorm.ksp.demo

import org.ktorm.entity.Entity
import org.ktorm.ksp.api.*
import org.ktorm.schema.BaseTable
import org.ktorm.schema.Column
import org.ktorm.schema.SqlType
import org.ktorm.schema.varchar
import java.math.BigDecimal
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

@Table
public data class Money(
    @PrimaryKey
    public val id: Int
)

@Table
public data class Box(
    @PrimaryKey
    public val id: Int
)

@Table(
    tableClassName = "EmployeeTable",
    ignoreColumns = ["updateTime"]
)
public data class Employee(
    @PrimaryKey
    public val pneumonoultramicroscopicsilicovolcanoconiosisPneumonoultramicroscopicsilicovolcanoconiosisPneumonoultramicroscopicsilicovolcanoconiosis: Int,
    public val name: String,
    public val age: Int,
    public val birthday: LocalDate = LocalDate.now(),
    public val gender: Gender,
    @org.ktorm.ksp.api.Column(converter = JsonConverter::class)
    public val salary: Salary,
) {
    @Ignore
    public var createTime: LocalDate = LocalDate.now()
    public var updateTime: LocalDate = LocalDate.now()
}

public data class Salary(
    val money: BigDecimal,
    val currency: String
)

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

public object JsonConverter : MultiTypeConverter {
    override fun <T : Any> convert(table: BaseTable<*>, columnName: String, propertyType: KClass<T>): Column<T> {
        return table.registerColumn(columnName, JsonSqlType(propertyType.java))
    }
}

public class JsonSqlType<T : Any>(private val clazz: Class<T>) : SqlType<T>(Types.VARCHAR, "varchar") {
    override fun doGetResult(rs: ResultSet, index: Int): T? {
        return null
    }

    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: T) {
    }

}

public object CustomStringConverter : SingleTypeConverter<String> {
    override fun convert(table: BaseTable<*>, columnName: String, propertyType: KClass<String>): Column<String> {
        return table.varchar(columnName)
    }
}

public object IntEnumConverter : EnumConverter {
    public override fun <E : Enum<E>> convert(
        table: BaseTable<*>,
        columnName: String,
        propertyType: KClass<E>
    ): Column<E> {
        return table.registerColumn(columnName, IntEnumSqlType(propertyType.java))
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