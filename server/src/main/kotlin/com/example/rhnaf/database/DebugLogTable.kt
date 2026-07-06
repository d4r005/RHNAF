package com.example.rhnaf.database

import org.jetbrains.exposed.sql.Table

object DebugLogTable : Table("debug_logs") {
    val id = integer("id").autoIncrement()
    val timestamp = varchar("timestamp", 50)
    val rawContent = text("raw_content")
    val sourceIp = varchar("source_ip", 50)
    
    override val primaryKey = PrimaryKey(id)
}
