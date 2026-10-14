package com.example.rhnaf.database

import org.jetbrains.exposed.sql.Table

/**
 * Listado de Lockers. LockerTable guarda el estado actual de cada casillero;
 * LockerHistoryTable es la bitacora INMUTABLE de todas las asignaciones: aunque
 * un casillero se libere o se reasigne, jamas se borra a quien perteneció.
 */
object LockerTable : Table("lockers") {
    val numero = integer("numero")
    val estado = varchar("estado", 12).default("Libre")           // Libre | Asignado
    val employeeId = varchar("employee_id", 50).default("")
    val nombre = varchar("nombre", 200).default("")
    val fechaCandado = varchar("fecha_candado", 20).default("")
    val llaveCopia = varchar("llave_copia", 4).default("Si")
    val fechaPrestamoLlave = varchar("fecha_prestamo_llave", 20).default("")
    val fechaDevolucionLlave = varchar("fecha_devolucion_llave", 20).default("")
    val notas = varchar("notas", 500).default("")
    override val primaryKey = PrimaryKey(numero)
}

object LockerHistoryTable : Table("locker_history") {
    val id = integer("id").autoIncrement()
    val numero = integer("numero")
    val employeeId = varchar("employee_id", 50).default("")
    val nombre = varchar("nombre", 200).default("")
    val fechaCandado = varchar("fecha_candado", 20).default("")
    val llaveCopia = varchar("llave_copia", 4).default("")
    val fechaPrestamoLlave = varchar("fecha_prestamo_llave", 20).default("")
    val fechaDevolucionLlave = varchar("fecha_devolucion_llave", 20).default("")
    val fechaInicio = varchar("fecha_inicio", 20).default("")
    val fechaFin = varchar("fecha_fin", 20).default("")           // "" = asignacion vigente
    val motivoFin = varchar("motivo_fin", 300).default("")
    val registradoPor = varchar("registrado_por", 200).default("")
    override val primaryKey = PrimaryKey(id)
}
