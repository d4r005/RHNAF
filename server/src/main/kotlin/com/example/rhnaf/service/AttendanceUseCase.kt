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
     * Registra una checada del personal. SOLO acepta eventos reales de la lectora
     * Hikvision — no datos inventados, no "none", no ruido de sistema.
     *
     * REGLAS:
     * 1) IDs invalidos (none, null, 0, vacio) → se descartan (no se guardan).
     * 2) Anti-ruido: si el mismo empleado tiene un evento hace menos de 2 minutos,
     *    es la misma deteccion de cara disparada varias veces en segundos. Se descarta.
     *    A partir de 2 minutos, se guarda (puede ser una segunda checada real).
     * 3) NUNCA se asigna "Check-out" si el ultimo evento fue hace menos de 4 horas.
     * 4) Status: solo "Check-in" o "Check-out".
     *    - Si el evento trae attendanceStatus explicito (del checkpoint): se usa tal cual.
     *    - Primer evento del dia: "Check-in".
     *    - Si el ultimo evento fue hace 4+ horas: "Check-out".
     *    - Resto: "Check-in" (regreso de lunch, segunda entrada, etc.).
     * 5) Deduplicacion exacta: mismo employeeId + mismo timestamp = no insertar otro.
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
            // DEDUPLICACION EXACTA: mismo employeeId + mismo timestamp = no insertar
            val existing = AttendanceLogTable
                .selectAll()
                .where {
                    (AttendanceLogTable.employeeId eq employeeId) and
                        (AttendanceLogTable.timestamp eq timestamp)
                }
                .count()
            if (existing > 0) return@dbQuery true

            // ANTI-RUIDO: si el ultimo evento fue hace menos de 2 minutos, es la misma
            // deteccion de cara (la lectora dispara varias veces en segundos).
            // Se descarta para no ensuciar. 2+ minutos = segunda checada real.
            val recentEvents = AttendanceLogTable
                .selectAll()
                .where {
                    (AttendanceLogTable.employeeId eq employeeId) and
                        (AttendanceLogTable.timestamp greaterEq day) and
                        (AttendanceLogTable.timestamp lessEq day + "T23:59:59.999999999")
                }
                .orderBy(AttendanceLogTable.timestamp, SortOrder.DESC)
                .limit(1)
                .toList()

            if (recentEvents.isNotEmpty()) {
                val lastTs = recentEvents.first()[AttendanceLogTable.timestamp]
                val gapSeconds = computeGapMinutes(lastTs, timestamp) * 60
                if (gapSeconds < 120) {
                    // Misma deteccion disparada varias veces en < 2 min. Descartar.
                    return@dbQuery true
                }
            }

            // ASIGNACION DE STATUS: solo "Check-in" o "Check-out"
            val countToday = AttendanceLogTable
                .selectAll()
                .where {
                    (AttendanceLogTable.employeeId eq employeeId) and
                        (AttendanceLogTable.timestamp greaterEq day) and
                        (AttendanceLogTable.timestamp lessEq day + "T23:59:59.999999999")
                }
                .count()

            val slot = if (attendanceStatus.isNotBlank()) {
                // PRIORIDAD 1: el evento trae status explicito (del checkpoint de la lectora)
                // PERO: si el status es "Check-out" y el ultimo evento fue hace menos de
                // 4 horas, lo cambiamos a "Check-in" — no puede haber un check-out segundos
                // despues de un check-in.
                if (attendanceStatus.equals("Check-out", ignoreCase = true) && recentEvents.isNotEmpty()) {
                    val lastTs = recentEvents.first()[AttendanceLogTable.timestamp]
                    val gapMin = computeGapMinutes(lastTs, timestamp)
                    if (gapMin < 240) "Check-in" else "Check-out"
                } else {
                    attendanceStatus
                }
            } else if (countToday == 0) {
                // PRIORIDAD 2: primer evento del dia
                "Check-in"
            } else {
                // PRIORIDAD 3: gap de tiempo desde el ultimo evento
                // NUNCA Check-out si el gap es < 4 horas
                val lastTs = recentEvents.first()[AttendanceLogTable.timestamp]
                val gapMinutes = computeGapMinutes(lastTs, timestamp)
                if (gapMinutes >= 240) {
                    "Check-out"
                } else {
                    "Check-in"
                }
            }

            // Buscar nombre/departamento del empleado si no vienen en el evento
            val (resolvedName, resolvedDept) = if (name.isBlank() || department.isBlank()) {
                val (empName, empDept) = lookupEmployeeInfo(employeeId)
                (name.ifBlank { empName }) to (department.ifBlank { empDept })
            } else name to department

            // Truncar defensivamente a los limites de columna
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
     */
    private fun computeGapMinutes(oldTs: String, newTs: String): Long {
        return runCatching {
            val oldLocal = java.time.LocalDateTime.parse(oldTs.substring(0, 19))
            val newLocal = java.time.LocalDateTime.parse(newTs.substring(0, 19))
            java.time.Duration.between(oldLocal, newLocal).toMinutes()
        }.getOrDefault(9999L)
    }

    /**
     * Exportacion para nomina/auditoria: agrupa por (empleado, dia) y devuelve
     * solo el check-in mas temprano y el check-out mas tardio de cada dia.
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

    /**
     * Elimina todos los registros de un dia especifico que vinieron de LOCAL-SYNC.
     */
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

    suspend fun deleteAllAttendance(): Int {
        return DatabaseFactory.dbQuery {
            AttendanceLogTable.deleteAll()
        }
    }

    private data class LogRecord(val recordId: Int, val employeeId: String, val timestamp: String)

    /**
     * Normaliza los eventos del dia: marca el mas temprano como "Check-in" y el
     * mas tardio como "Check-out" (solo si hay 4+ horas de diferencia). Los eventos
     * intermedios se eliminan para dejar el dia limpio. Si todos los eventos estan
     * dentro de 4 horas, solo se deja el primero como "Check-in".
     */
    suspend fun normalizeDailyLimits(): Int {
        return DatabaseFactory.dbQuery {
            // Agrupar por (employeeId, dia)
            val allRows = AttendanceLogTable
                .selectAll()
                .orderBy(AttendanceLogTable.timestamp, SortOrder.ASC)
                .map { row ->
                    LogRecord(
                        row[AttendanceLogTable.id],
                        row[AttendanceLogTable.employeeId],
                        row[AttendanceLogTable.timestamp]
                    )
                }

            val grouped = allRows.groupBy { rec ->
                rec.employeeId to rec.timestamp.substringBefore("T").substringBefore(" ")
            }

            var deleted = 0
            for ((_, records) in grouped) {
                val sorted = records.sortedBy { it.timestamp }
                if (sorted.size <= 2) {
                    // 1 o 2 eventos: marcar primero como Check-in, ultimo como Check-out
                    if (sorted.size == 1) {
                        AttendanceLogTable.update({ AttendanceLogTable.id eq sorted[0].recordId }) {
                            it[attendanceStatus] = "Check-in"
                        }
                    } else {
                        val totalGap = computeGapMinutes(sorted.first().timestamp, sorted.last().timestamp)
                        AttendanceLogTable.update({ AttendanceLogTable.id eq sorted.first().recordId }) {
                            it[attendanceStatus] = "Check-in"
                        }
                        AttendanceLogTable.update({ AttendanceLogTable.id eq sorted.last().recordId }) {
                            it[attendanceStatus] = if (totalGap >= 240) "Check-out" else "Check-in"
                        }
                    }
                    continue
                }

                // 3+ eventos: quedar con el primero (Check-in) y el ultimo (Check-out
                // solo si hay 4+ horas de gap). Borrar el resto.
                val checkInId = sorted.first().recordId
                val checkOutId = sorted.last().recordId
                val totalGap = computeGapMinutes(sorted.first().timestamp, sorted.last().timestamp)

                if (totalGap >= 240) {
                    // Hay jornada completa: Check-in + Check-out
                    val idsToDelete = sorted
                        .filter { it.recordId != checkInId && it.recordId != checkOutId }
                        .map { it.recordId }

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
                } else {
                    // Todos los eventos estan en menos de 4 horas: solo dejar el primero como Check-in
                    val idsToDelete = sorted
                        .filter { it.recordId != checkInId }
                        .map { it.recordId }

                    AttendanceLogTable.update({ AttendanceLogTable.id eq checkInId }) {
                        it[attendanceStatus] = "Check-in"
                    }

                    if (idsToDelete.isNotEmpty()) {
                        AttendanceLogTable.deleteWhere { AttendanceLogTable.id inList idsToDelete }
                        deleted += idsToDelete.size
                    }
                }
            }
            deleted
        }
    }

    suspend fun backfillMissingMetadata(): Int {
        return DatabaseFactory.dbQuery {
            var updated = 0

            // Llenar status vacios
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
