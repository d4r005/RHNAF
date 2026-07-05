package com.example.rhnaf.database

import org.jetbrains.exposed.sql.Table

object UserTable : Table("users") {
    val email = varchar("email", 100)
    val password = varchar("password", 100)
    val department = varchar("department", 100)
    val role = varchar("role", 20)
    val name = varchar("name", 100)

    override val primaryKey = PrimaryKey(email)
}
