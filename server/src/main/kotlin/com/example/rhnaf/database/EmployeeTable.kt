package com.example.rhnaf.database

import org.jetbrains.exposed.sql.Table
import com.example.rhnaf.shared.model.EmployeeStatus

object EmployeeTable : Table("employees") {
    val id = varchar("id", 50)
    val firstName = varchar("first_name", 100)
    val lastName = varchar("last_name", 100)
    val position = varchar("position", 100)
    val department = varchar("department", 100)
    val entryDate = varchar("entry_date", 20)
    val status = enumerationByName("status", 20, EmployeeStatus::class)
    val email = varchar("email", 100).nullable()
    val readerId = varchar("reader_id", 50).nullable() 
    val attritionRisk = double("attrition_risk").default(0.15)
    
    override val primaryKey = PrimaryKey(id)
}
