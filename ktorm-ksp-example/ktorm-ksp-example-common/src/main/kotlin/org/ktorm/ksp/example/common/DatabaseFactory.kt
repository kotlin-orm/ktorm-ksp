package org.ktorm.ksp.example.common

import org.ktorm.database.Database
import org.ktorm.database.use
import org.ktorm.logging.ConsoleLogger
import org.ktorm.logging.LogLevel

public object DatabaseFactory {

    public val database: Database

    init {
        database = Database.connect(
            url = "jdbc:h2:mem:ktorm;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
            logger = ConsoleLogger(threshold = LogLevel.TRACE),
            alwaysQuoteIdentifiers = true
        )

        execInitSqlScript()
    }

    private fun execInitSqlScript() {
        val filename = "init-data.sql"
        database.useConnection { conn ->
            conn.createStatement().use { statement ->
                javaClass.classLoader
                    ?.getResourceAsStream(filename)
                    ?.bufferedReader()
                    ?.use { reader ->
                        for (sql in reader.readText().split(';')) {
                            if (sql.any { it.isLetterOrDigit() }) {
                                statement.executeUpdate(sql)
                            }
                        }
                    }
            }
        }
    }

}