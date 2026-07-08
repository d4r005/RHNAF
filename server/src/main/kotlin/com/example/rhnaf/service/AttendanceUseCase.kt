package com.example.rhnaf.service

import com.example.rhnaf.database.AttendanceLogTable
import com.example.rhnaf.database.DatabaseFactory
import com.example.rhnaf.database.EmployeeTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList

class AttendanceUseCase {

    /**
     * Busca en la ficha del empleado (por readerId o por id) el nombre completo y
     * departamento, para enriquecer automaticamente cada checada aunque la lectora
     * no mande el nombre en el evento (lo normal en la mayoria de eventos de tarjeta/rostro).
     * Debe llamarse DENTRO de una transaccion (dbQuery) ya abierta.
     */
    private fun lookupEmployeeInfo(employeeId: String): Pair<String, String> {
        val row = EmployeeTable
            .selectAll()
            .where { (EmployeeTable.readerId eq employeeId) or (EmployeeTable.id eq employeeId) }
            .limit(1)
            .firstOrNull()

        return if (row != null) {
            "${row[EmployeeTable.firstName]} ${row[EmployeeTable.lastName]}".trim() to row[EmployeeTable.department]
        } else {
            "" to ""
        }
    }

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
                val (resolvedName, resolvedDept) = if (name.isBlank() || department.isBlank()) {
                    val (empName, empDept) = lookupEmployeeInfo(employeeId)
                    (name.ifBlank { empName }) to (department.ifBlank { empDept })
                } else name to department

                AttendanceLogTable.insert {
                    it[AttendanceLogTable.employeeId] = employeeId
                    it[AttendanceLogTable.timestamp] = timestamp
                    it[AttendanceLogTable.deviceSerial] = deviceSerial
                    it[AttendanceLogTable.verifyMode] = verifyMode
                    it[AttendanceLogTable.attendanceStatus] = slot
                    it[AttendanceLogTable.name] = resolvedName
                    it[AttendanceLogTable.department] = resolvedDept
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

    /**
     * Repara registros HISTORICOS que se guardaron antes de que existieran las columnas
     * name/department/attendance_status (o que llegaron sin esos datos):
     *  - attendance_status: se infiere por orden cronologico dentro del mismo dia
     *    (1er registro del dia = Check-in, 2do = Check-out; si por algun motivo ya
     *    hay mas de 2, se dejan alternados en el mismo orden sin borrar nada).
     *  - name / department: se cruzan con la ficha del empleado (EmployeeTable) por
     *    readerId o id.
     * Devuelve cuantos registros se actualizaron. Es seguro correrlo varias veces
     * (idempotente): solo toca filas que tengan estos campos vacios.
     */
    suspend fun backfillMissingMetadata(): Int {
        return DatabaseFactory.dbQuery {
            var updated = 0

            // --- 1) attendance_status faltante, inferido por orden dentro del dia ---
            val missingStatusRows = AttendanceLogTable
                .selectAll()
                .where { AttendanceLogTable.attendanceStatus eq "" }
                .map { row -> LogRecord(row[AttendanceLogTable.id], row[AttendanceLogTable.employeeId], row[AttendanceLogTable.timestamp]) }

            if (missingStatusRows.isNotEmpty()) {
                val grouped = missingStatusRows.groupBy { rec -> rec.employeeId to rec.timestamp.substringBefore("T").substringBefore(" ") }
                for ((_, records) in grouped) {
                    val sorted = records.sortedBy { rec -> rec.timestamp }
                    sorted.forEachIndexed { index, rec ->
                        val slot = if (index % 2 == 0) "Check-in" else "Check-out"
                        AttendanceLogTable.update({ AttendanceLogTable.id eq rec.recordId }) {
                            it[attendanceStatus] = slot
                        }
                        updated++
                    }
                }
            }

            // --- 2) name / department faltantes, cruzando con la ficha del empleado ---
            val missingInfoRows = AttendanceLogTable
                .selectAll()
                .where { (AttendanceLogTable.name eq "") or (AttendanceLogTable.department eq "") }
                .map { row -> row[AttendanceLogTable.id] to row[AttendanceLogTable.employeeId] }

            val employeeIds = missingInfoRows.map { it.second }.distinct()
            if (employeeIds.isNotEmpty()) {
                val employeeInfoById = HashMap<String, Pair<String, String>>()
                EmployeeTable
                    .selectAll()
                    .where { (EmployeeTable.readerId inList employeeIds) or (EmployeeTable.id inList employeeIds) }
                    .forEach { row ->
                        val fullName = "${row[EmployeeTable.firstName]} ${row[EmployeeTable.lastName]}".trim()
                        val dept = row[EmployeeTable.department]
                        row[EmployeeTable.readerId]?.let { employeeInfoById[it] = fullName to dept }
                        employeeInfoById[row[EmployeeTable.id]] = fullName to dept
                    }

                for ((recordId, empId) in missingInfoRows) {
                    val info = employeeInfoById[empId] ?: continue
                    AttendanceLogTable.update({ AttendanceLogTable.id eq recordId }) {
                        it[name] = info.first
                        it[department] = info.second
                    }
                    updated++
                }
            }

            updated
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
