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
     * no mande el nombre en el evento.
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
     * NUEVA REGLA: Acepta TODAS las checadas. No rechaza ninguna.
     * Cada evento se guarda con su timestamp real. Al exportar (CSV/PDF),
     * se toma el check-in mas temprano y el check-out mas tardio del dia.
     *
     * El campo attendanceStatus se asigna asi:
     * - Si es el primer evento del dia para ese empleado: "Check-in"
     * - Si ya hay eventos previos: "Check-out" (y se actualiza el anterior a "Check-in" si era el unico)
     *
     * Devuelve true siempre (el evento se guardo).
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
                        (AttendanceLogTable.timestamp greaterEq day) and (AttendanceLogTable.timestamp lessEq day + "T23:59:59.999999999")
                }
                .count()

            // Si es el primer evento del dia: Check-in. Si ya hay eventos: Check-out.
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

    /**
     * Exportacion para nomina/auditoria: agrupa por (empleado, dia) y devuelve
     * solo el check-in mas temprano y el check-out mas tardio de cada dia.
     * Si un empleado solo tiene una checada en el dia, se marca como Check-in.
     */
    data class DailySummary(
        val employeeId: String,
        val name: String,
        val department: String,
        val date: String,
        val checkIn: String?,
        val checkOut: String?,
        val totalChecks: Int
    )

    suspend fun exportDailySummary(startDate: String, endDate: String): List<DailySummary> {
        return DatabaseFactory.dbQuery {
            val rows = AttendanceLogTable
                .selectAll()
                .where {
                    (AttendanceLogTable.timestamp greaterEq startDate) and
                        (AttendanceLogTable.timestamp lessEq endDate + "\uFFFF")
                }
                .orderBy(AttendanceLogTable.timestamp, SortOrder.ASC)
                .map { row ->
                    Triple(
                        row[AttendanceLogTable.employeeId],
                        row[AttendanceLogTable.name],
                        row[AttendanceLogTable.timestamp]
                    )
                }

            // Agrupar por (empleado, dia)
            val grouped = rows.groupBy { (empId, _, ts) ->
                empId to ts.substringBefore("T").substringBefore(" ")
            }

            val results = mutableListOf<DailySummary>()
            for ((key, records) in grouped) {
                val (empId, day) = key
                val sorted = records.sortedBy { it.third }
                val earliest = sorted.first().third
                val latest = sorted.last().third
                val name = sorted.first().second

                // Buscar departamento del empleado
                val dept = EmployeeTable
                    .selectAll()
                    .where { (EmployeeTable.readerId eq empId) or (EmployeeTable.id eq empId) }
                    .firstOrNull()?.let { it[EmployeeTable.department] } ?: ""

                results.add(
                    DailySummary(
                        employeeId = empId,
                        name = name,
                        department = dept,
                        date = day,
                        checkIn = if (sorted.size >= 1) earliest else null,
                        checkOut = if (sorted.size >= 2) latest else null,
                        totalChecks = sorted.size
                    )
                )
            }

            results.sortedWith(compareBy({ it.date }, { it.employeeId }))
        }
    }

    /**
     * Elimina todos los registros de un dia especifico que vinieron de LOCAL-SYNC
     * (sincronizacion historica) para que no ensucien el dia con checadas falsas.
     */
    suspend fun deleteLocalSyncForDay(day: String): Int {
        return DatabaseFactory.dbQuery {
            // Seleccionamos los IDs de los registros LOCAL-SYNC del dia y borramos por ID
            val ids = AttendanceLogTable
                .select(AttendanceLogTable.id)
                .where {
                    (AttendanceLogTable.deviceSerial eq "LOCAL-SYNC") and
                        (AttendanceLogTable.timestamp like "$day%")
                }
                .map { it[AttendanceLogTable.id] }
            if (ids.isNotEmpty()) {
                AttendanceLogTable.deleteWhere { AttendanceLogTable.id inList ids }
            } else {
                0
            }
        }
    }

    /**
     * Borra TODOS los registros de un dia especifico, sin importar el origen.
     */
    suspend fun deleteAllForDay(day: String): Int {
        return DatabaseFactory.dbQuery {
            val ids = AttendanceLogTable
                .select(AttendanceLogTable.id)
                .where {
                    AttendanceLogTable.timestamp like "$day%"
                }
                .map { it[AttendanceLogTable.id] }
            if (ids.isNotEmpty()) {
                AttendanceLogTable.deleteWhere { AttendanceLogTable.id inList ids }
            } else {
                0
            }
        }
    }

    /**
     * Limpieza retroactiva: para cada (empleado, dia) que tenga MAS de 2 checadas,
     * conserva solo la mas temprana (Check-in) y la mas tardia (Check-out).
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

    suspend fun backfillMissingMetadata(): Int {
        return DatabaseFactory.dbQuery {
            var updated = 0

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
        return 0
    }
}
