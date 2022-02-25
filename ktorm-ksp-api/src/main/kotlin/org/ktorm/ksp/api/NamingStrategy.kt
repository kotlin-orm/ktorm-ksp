package org.ktorm.ksp.api


public interface NamingStrategy {
    public fun toTableName(entityClassName: String): String
    public fun toColumnName(propertyName: String): String
}

public object LowerCaseCamelCaseToUnderscoresNamingStrategy : NamingStrategy {

    override fun toTableName(entityClassName: String): String {
        return entityClassName.camelCase().lowercase()
    }

    override fun toColumnName(propertyName: String): String {
        return propertyName.camelCase().lowercase()
    }

    private fun String.camelCase(): String {
        val builder = StringBuilder(this)
        var i = 1
        while (i < builder.length - 1) {
            if (isUnderscoreRequired(builder[i - 1], builder[i], builder[i + 1])) {
                builder.insert(i++, '_')
            }
            i++
        }
        return builder.toString()
    }

    private fun isUnderscoreRequired(before: Char, current: Char, after: Char): Boolean {
        return Character.isLowerCase(before) && Character.isUpperCase(current) && Character.isLowerCase(after)
    }

}
