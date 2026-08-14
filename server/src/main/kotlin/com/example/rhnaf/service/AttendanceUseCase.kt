package com.example.rhnaf.service

import com.example.rhnaf.database.AttendanceLogTable
import com.example.rhnaf.database.DatabaseFactory
import com.example.rhnaf.database.EmployeeTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like

class AttendanceUseCase {

    /**
     * Busca el nombre y departamento del empleado en la ficha.
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
     * Registra una checada. Guarda EXACTAMENTE lo que manda la lectora.
     *
     * - Si el employeeNo es invalido (none, null, 0, vacio): se descarta.
     * - Si ya existe un registro con el mismo employeeId + timestamp: no duplica (para cuando el sync re-corre).
     * - El attendanceStatus se guarda tal cual viene de la lectora (inferido del checkpoint en AttendanceRoutes).
     *   Si viene vacio, se guarda vacio. No se inventa nada.
     */
    suspend fun registerCheckIn(
        employeeId: String,
        timestamp: String,
        deviceSerial: String = "UNKNOWN",
        verifyMode: String = "UNKNOWN",
        name: String = "",
        department: String = "",
        customName: String = "",
        attendanceStatus: String = ""
    ): Boolean {
        val idClean = employeeId.trim().lowercase()
        if (idClean.isEmpty() || idClean == "none" || idClean == "null" || idClean == "0") {
            return true
        }

        return DatabaseFactory.dbQuery {
            // Deduplicacion exacta: mismo employeeId + mismo timestamp = no insertar otro
            val existing = AttendanceLogTable
                .selectAll()
                .where {
                    (AttendanceLogTable.employeeId eq employeeId) and
                        (AttendanceLogTable.timestamp eq timestamp)
                }
                .count()
            if (existing > 0) return@dbQuery true

            // Buscar nombre/departamento si no vienen en el evento
            val (resolvedName, resolvedDept) = if (name.isBlank() || department.isBlank()) {
                val (empName, empDept) = lookupEmployeeInfo(employeeId)
                (name.ifBlank { empName }) to (department.ifBlank { empDept })
            } else name to department

            AttendanceLogTable.insert {
                it[AttendanceLogTable.employeeId] = employeeId.take(100)
                it[AttendanceLogTable.timestamp] = timestamp
                it[AttendanceLogTable.deviceSerial] = deviceSerial.take(150)
                it[AttendanceLogTable.verifyMode] = verifyMode.take(100)
                it[AttendanceLogTable.attendanceStatus] = attendanceStatus
                it[AttendanceLogTable.name] = resolvedName.take(200)
                it[AttendanceLogTable.department] = resolvedDept.take(150)
                it[AttendanceLogTable.customName] = customName.take(200)
            }
            true
        }
    }

    /**
     * Exportacion para nomina: agrupa por (empleado, dia) y devuelve
     * el check-in mas temprano y el check-out mas tardio.
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

    suspend fun deleteLocalSyncForDay(day: String): Int {
        return DatabaseFactory.dbQuery {
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

    suspend fun deleteAllForDay(day: String): Int {
        return DatabaseFactory.dbQuery {
            AttendanceLogTable.deleteWhere { AttendanceLogTable.timestamp like "$day%" }
        }
    }

    suspend fun deleteAllAttendance(): Int {
        return DatabaseFactory.dbQuery {
            AttendanceLogTable.deleteAll()
        }
    }

    suspend fun normalizeDailyLimits(): Int {
        // No inventar nada. Solo eliminar duplicados exactos.
        return DatabaseFactory.dbQuery {
            val allRows = AttendanceLogTable
                .selectAll()
                .orderBy(AttendanceLogTable.timestamp, SortOrder.ASC)
                .map { row ->
                    Triple(row[AttendanceLogTable.id], row[AttendanceLogTable.employeeId], row[AttendanceLogTable.timestamp])
                }

            val grouped = allRows.groupBy { (id, empId, ts) -> empId to ts }
            var deleted = 0
            for ((_, records) in grouped) {
                if (records.size > 1) {
                    val idsToDelete = records.drop(1).map { it.first }
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

            // Llenar nombre/departamento faltantes
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
