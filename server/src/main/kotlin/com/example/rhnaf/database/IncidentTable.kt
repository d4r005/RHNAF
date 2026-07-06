package com.example.rhnaf.database

import org.jetbrains.exposed.sql.Table

object IncidentTable : Table("incidents") {
    val id = integer("id").autoIncrement()
    val employeeId = varchar("employee_id", 50) references EmployeeTable.id
    val weekNumber = integer("week_number")
    val year = integer("year")
    val attendance = varchar("attendance", 100) // Serialized list "1,1,1,0,V,D,D"
    val punctualityBonus = double("punctuality_bonus").default(0.0)
    val attendanceBonus = double("attendance_bonus").default(0.0)
    val sundayPremium = double("sunday_premium").default(0.0)
    val extraHours = double("extra_hours").default(0.0)
    val foodAllowance = double("food_allowance").default(0.0)
    val weekendBonus = double("weekend_bonus").default(0.0)
    val perfectAttendance = bool("perfect_attendance").default(false)
    val absences = integer("absences").default(0)
    val observations = text("observations").default("")
    val deductions = double("deductions").default(0.0)
    val pending = double("pending").default(0.0)
    val infonavit = double("infonavit").default(0.0)
    val otherDiscounts = double("other_discounts").default(0.0)
    
    override val primaryKey = PrimaryKey(id)
}
