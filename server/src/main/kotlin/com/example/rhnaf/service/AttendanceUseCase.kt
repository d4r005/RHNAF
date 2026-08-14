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
        customName: String = "",
        attendanceStatus: String = ""
    ): Boolean {
        // Validacion defensiva contra IDs invalidos (ruido de sistema Hikvision)
        val idClean = employeeId.trim().lowercase()
        if (idClean.isEmpty() || idClean == "none" || idClean == "null" || idClean == "0") {
            return true // Respondemos true para que la lectora no reintente, pero no guardamos nada
        }

        val day = timestamp.substringBefore("T").substringBefore(" ")

        return DatabaseFactory.dbQuery {
            // DEDUPLICACION: si ya existe un registro con el mismo employeeId y timestamp,
            // no insertamos otro (el sync script puede re-subir el mismo evento).
            val existing = AttendanceLogTable
                .selectAll()
                .where {
                    (AttendanceLogTable.employeeId eq employeeId) and
                        (AttendanceLogTable.timestamp eq timestamp)
                }
                .count()
            if (existing > 0) return@dbQuery true // Ya existe, no duplicar

            val todayEvents = AttendanceLogTable
                .selectAll()
                .where {
                    (AttendanceLogTable.employeeId eq employeeId) and
                        (AttendanceLogTable.timestamp greaterEq day) and (AttendanceLogTable.timestamp lessEq day + "T23:59:59.999999999")
                }
                .orderBy(AttendanceLogTable.timestamp, SortOrder.ASC)
                .toList()

            val countToday = todayEvents.size

            // Lógica de asignación de status:
            // PRIORIDAD 1: Si el evento trae attendanceStatus desde el checkpoint
            //   (inferido del nombre del lector: Entrance=Check-in, Exit=Check-out),
            //   lo usamos directamente — igual que IVMS-4200.
            // PRIORIDAD 2: Si no trae status, usamos la lógica de gap de tiempo:
            // - Primer evento del día: "Check-in"
            // - Último evento fue hace menos de 5 minutos: ruido (Duplicate)
            // - Último evento fue hace más de 4 horas: Check-out
            // - Caso intermedio: "Event"
            val slot = if (attendanceStatus.isNotBlank()) {
                attendanceStatus
            } else if (countToday == 0) {
                "Check-in"
            } else {
                val lastTs = todayEvents.last()[AttendanceLogTable.timestamp]
                val gapMinutes = computeGapMinutes(lastTs, timestamp)
                if (gapMinutes >= 240) {
                    "Check-out"
                } else if (gapMinutes < 5) {
                    "Duplicate"
                } else {
                    "Event"
                }
            }

            val (resolvedName, resolvedDept) = if (name.isBlank() || department.isBlank()) {
                val (empName, empDept) = lookupEmployeeInfo(employeeId)
                (name.ifBlank { empName }) to (department.ifBlank { empDept })
            } else name to department

            // Truncamos defensivamente a los limites de columna (ver AttendanceLogTable):
            // la lectora real manda valores de deviceName/currentVerifyMode mas largos
            // que los de prueba, y un insert que excede el varchar tumba la request con
            // un 500 "value too long for type character varying". Mejor recortar que
            // que se pierda el registro completo.
            AttendanceLogTable.insert {
                it[AttendanceLogTable.employeeId] = employeeId.take(100)
                it[AttendanceLogTable.timestamp] = timestamp
                it[AttendanceLogTable.deviceSerial] = deviceSerial.take(150)
                it[AttendanceLogTable.verifyMode] = verifyMode.take(100)
                it[AttendanceLogTable.attendanceStatus] = slot
                it[AttendanceLogTable.name] = resolvedName.take(200)
                it[AttendanceLogTable.department] = resolvedDept.take(150)
                it[AttendanceLogTable.customName] = customName.take(200)
            }
            true
        }
    }

    /**
     * Calcula la diferencia en minutos entre dos timestamps ISO (formato yyyy-MM-ddTHH:mm:ss).
     * Usado para decidir si un evento consecutivo es un Check-out real (> 4h) o ruido (< 5min).
     */
    private fun computeGapMinutes(oldTs: String, newTs: String): Long {
        return runCatching {
            val oldLocal = java.time.LocalDateTime.parse(oldTs.substring(0, 19))
            val newLocal = java.time.LocalDateTime.parse(newTs.substring(0, 19))
            java.time.Duration.between(oldLocal, newLocal).toMinutes()
        }.getOrDefault(9999L) // si no se puede parsear, asumimos gap grande
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
     * Borra TODOS los registros de la tabla de asistencia.
     * Útil para reiniciar pruebas de sincronización.
     */
    suspend fun deleteAllAttendance(): Int {
        return DatabaseFactory.dbQuery {
            AttendanceLogTable.deleteAll()
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
                val sorted = records.sortedBy { rec -> rec.timestamp }
                val checkInId = sorted.first().recordId
                val checkOutId = sorted.last().recordId
                val totalGap = computeGapMinutes(sorted.first().timestamp, sorted.last().timestamp)

                if (records.size == 2 && totalGap < 5) {
                    // Solo 2 eventos y a menos de 5 min: es un duplicado de la lectora
                    // Borramos el segundo y dejamos solo el Check-in
                    AttendanceLogTable.update({ AttendanceLogTable.id eq checkInId }) {
                        it[attendanceStatus] = "Check-in"
                    }
                    AttendanceLogTable.deleteWhere { AttendanceLogTable.id eq checkOutId }
                    deleted += 1
                    continue
                }

                if (records.size <= 2) {
                    // 1 o 2 eventos reales: marcar primero como Check-in
                    AttendanceLogTable.update({ AttendanceLogTable.id eq checkInId }) {
                        it[attendanceStatus] = "Check-in"
                    }
                    if (records.size == 2) {
                        AttendanceLogTable.update({ AttendanceLogTable.id eq checkOutId }) {
                            it[attendanceStatus] = if (totalGap >= 240) "Check-out" else "Event"
                        }
                    }
                    continue
                }

                // 3+ eventos: borrar duplicados < 5min, marcar primero y ultimo
                val idsToDelete = mutableListOf<Int>()
                for (i in 1 until sorted.size) {
                    val gap = computeGapMinutes(sorted[i - 1].timestamp, sorted[i].timestamp)
                    if (gap < 5 && sorted[i].recordId != checkInId && sorted[i].recordId != checkOutId) {
                        idsToDelete.add(sorted[i].recordId)
                    }
                }

                AttendanceLogTable.update({ AttendanceLogTable.id eq checkInId }) {
                    it[attendanceStatus] = "Check-in"
                }
                AttendanceLogTable.update({ AttendanceLogTable.id eq checkOutId }) {
                    it[attendanceStatus] = if (totalGap >= 240) "Check-out" else "Event"
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
