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
    val rfc = varchar("rfc", 20).nullable()
    val curp = varchar("curp", 30).nullable()
    val nss = varchar("nss", 20).nullable()
    val email = varchar("email", 100).nullable()
    val readerId = varchar("reader_id", 50).nullable() 
    val photoUrl = text("photo_url").nullable()
    val attritionRisk = double("attrition_risk").default(0.15)
    val salary = double("salary").nullable()            // sueldo diario en pesos
    val sbc = double("sbc").nullable()                  // salario base de cotizacion IMSS
    val exitDate = varchar("exit_date", 20).nullable()  // fecha de baja (dd/MM/yyyy)
    val phone = varchar("phone", 30).nullable()
    val supervisor = varchar("supervisor", 150).nullable()
    val contractType = varchar("contract_type", 60).nullable()
    val maritalStatus = varchar("marital_status", 40).nullable()
    val emergencyContact = varchar("emergency_contact", 150).nullable()
    val paymentFrequency = varchar("payment_frequency", 20).default("Semanal") // Semanal | Quincenal
    val infonavitDescuento = double("infonavit_descuento").nullable() // monto fijo por periodo de credito Infonavit
    val fondoAhorroPct = double("fondo_ahorro_pct").nullable()        // % del sueldo base para fondo de ahorro (trabajador y empresa)
    
    override val primaryKey = PrimaryKey(id)
}
