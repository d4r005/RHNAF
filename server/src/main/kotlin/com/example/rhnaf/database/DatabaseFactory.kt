package com.example.rhnaf.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.*
import com.example.rhnaf.shared.model.EmployeeStatus
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

object DatabaseFactory {
    fun init() {
        val driverClassName = "org.h2.Driver"
        val jdbcURL = "jdbc:h2:file:./build/db"
        val database = Database.connect(createHikariDataSource(jdbcURL, driverClassName))
        
        transaction(database) {
            SchemaUtils.create(EmployeeTable)
            
            // Añadir datos iniciales si está vacía
            if (EmployeeTable.selectAll().empty()) {
                EmployeeTable.insert {
                    it[id] = "OP-101"
                    it[firstName] = "Pedro"
                    it[lastName] = "García"
                    it[position] = "Operador Montacargas"
                    it[department] = "Logística"
                    it[entryDate] = "2023-01-10"
                    it[status] = EmployeeStatus.ACTIVE
                }
                EmployeeTable.insert {
                    it[id] = "OP-102"
                    it[firstName] = "María"
                    it[lastName] = "López"
                    it[position] = "Supervisor de Línea"
                    it[department] = "Producción"
                    it[entryDate] = "2022-05-20"
                    it[status] = EmployeeStatus.ACTIVE
                }
            }
        }
    }

    private fun createHikariDataSource(
        url: String,
        driver: String
    ) = HikariDataSource(HikariConfig().apply {
        driverClassName = driver
        jdbcUrl = url
        maximumPoolSize = 3
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        validate()
    })

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
