package com.example.rhnaf.database

import org.jetbrains.exposed.sql.Table

// Ajustes manuales de pre-nomina por empleado y periodo: permiten corregir
// los valores AUTOMATICOS de ISR/IMSS (o capturar anticipo / otros
// descuentos) cuando el calculo automatico necesita una correccion.
object PayrollOverrideTable : Table("payroll_overrides") {
    val id = integer("id").autoIncrement()
    val employeeId = varchar("employee_id", 50)
    val periodoInicio = varchar("periodo_inicio", 10)
    val periodoFin = varchar("periodo_fin", 10)
    val isr = double("isr").nullable()          // si es null: usar el calculo automatico
    val imss = double("imss").nullable()        // si es null: usar el calculo automatico
    val anticipo = double("anticipo").default(0.0)
    val otros = double("otros").default(0.0)
    val updatedBy = varchar("updated_by", 150).nullable()
    override val primaryKey = PrimaryKey(id)
}
