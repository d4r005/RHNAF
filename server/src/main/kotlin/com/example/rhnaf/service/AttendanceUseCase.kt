package com.example.rhnaf.service

import com.example.rhnaf.database.AttendanceLogTable
import com.example.rhnaf.database.DatabaseFactory
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList

class AttendanceUseCase {

    /**
     * Regla de negocio: cada empleado puede tener COMO MAXIMO 1 "Check-in" y 1 "Check-out"
     * por dia calendario. El primer evento del dia se guarda como Check-in, el segundo como
     * Check-out, y cualquier evento adicional ese mismo dia se RECHAZA (no se guarda).
     *
     * Devuelve true si el evento se guardo, false si se rechazo por exceder el limite diario.
     */
    suspend fun registerCheckIn(
        employeeId: String,
        timestamp: String,
        deviceSerial: String = "UNKNOWN",
        verifyMode: String = "UNKNOWN",
        name: String = "",
        department: String = "",
        customName: String = ""
    ): Boolean {
        val day = timestamp.substringBefore("T").substringBefore(" ")

        return DatabaseFactory.dbQuery {
            val countToday = AttendanceLogTable
                .selectAll()
                .where {
                    (AttendanceLogTable.employeeId eq employeeId) and
                        (AttendanceLogTable.timestamp like "$day%")
                }
                .count()

            if (countToday >= 2) {
                false
            } else {
                val slot = if (countToday == 0L) "Check-in" else "Check-out"
                AttendanceLogTable.insert {
                    it[AttendanceLogTable.employeeId] = employeeId
                    it[AttendanceLogTable.timestamp] = timestamp
                    it[AttendanceLogTable.deviceSerial] = deviceSerial
                    it[AttendanceLogTable.verifyMode] = verifyMode
                    it[AttendanceLogTable.attendanceStatus] = slot
                    it[AttendanceLogTable.name] = name
                    it[AttendanceLogTable.department] = department
                    it[AttendanceLogTable.customName] = customName
                }
                true
            }
        }
    }

    /**
     * Limpieza retroactiva: para cada (empleado, dia) que tenga MAS de 2 checadas guardadas
     * (de datos historicos importados antes de existir esta regla), se conserva solo la mas
     * temprana (Check-in) y la mas tardia (Check-out), y se eliminan las de en medio.
     * Devuelve cuantos registros se eliminaron.
     */
    private data class LogRecord(val recordId: Int, val employeeId: String, val timestamp: String)

    suspend fun normalizeDailyLimits(): Int {
        return DatabaseFactory.dbQuery {
            val all = AttendanceLogTable
                .selectAll()
                .orderBy(AttendanceLogTable.employeeId, SortOrder.ASC)
                .map { row ->
                    LogRecord(row[AttendanceLogTable.id], row[AttendanceLogTable.employeeId], row[AttendanceLogTable.timestamp])
                }

            val grouped = all.groupBy { rec -> rec.employeeId to rec.timestamp.substringBefore("T").substringBefore(" ") }

            var deleted = 0
            for ((_, records) in grouped) {
                if (records.size <= 2) continue

                val sorted = records.sortedBy { rec -> rec.timestamp }
                val checkInId = sorted.first().recordId
                val checkOutId = sorted.last().recordId
                val idsToDelete = sorted.map { rec -> rec.recordId }.filter { recId -> recId != checkInId && recId != checkOutId }

                // Aseguramos las etiquetas correctas para los dos que sí se quedan
                AttendanceLogTable.update({ AttendanceLogTable.id eq checkInId }) {
                    it[attendanceStatus] = "Check-in"
                }
                AttendanceLogTable.update({ AttendanceLogTable.id eq checkOutId }) {
                    it[attendanceStatus] = "Check-out"
                }

                if (idsToDelete.isNotEmpty()) {
                    AttendanceLogTable.deleteWhere { AttendanceLogTable.id inList idsToDelete }
                    deleted += idsToDelete.size
                }
            }
            deleted
        }
    }

    suspend fun syncWithDevice(deviceIp: String): Int {
        // IP de la lectora configurada: 10.141.1.230
        // En un entorno real, aquí usaríamos Ktor Client para llamar a la ISAPI de Hikvision
        // Ejemplo: GET http://admin:password@$deviceIp/ISAPI/AccessControl/AcsEvent?format=json

        // Eliminamos los datos de prueba (mock) para mostrar solo información verídica
        // que la lectora envíe vía Push a este servidor.

        return 0 // Retornamos 0 ya que la sincronización manual vía IP local no es posible desde la nube
    }
}
