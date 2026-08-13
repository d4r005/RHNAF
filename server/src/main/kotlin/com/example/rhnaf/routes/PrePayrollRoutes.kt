package com.example.rhnaf.routes

import com.example.rhnaf.database.*
import com.example.rhnaf.shared.model.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.and
import java.time.LocalDate
import java.time.LocalTime
import java.time.Duration

fun Route.prePayrollRouting() {
    route("/api/v1/pre-nomina") {

        // ---------- TURNOS ----------
        route("/turnos") {
            get {
                val items = DatabaseFactory.dbQuery {
                    ShiftTable.selectAll().map {
                        Shift(
                            id = it[ShiftTable.id],
                            nombre = it[ShiftTable.nombre],
                            horaEntrada = it[ShiftTable.horaEntrada],
                            horaSalida = it[ShiftTable.horaSalida],
                            minutoTolerancia = it[ShiftTable.minutoTolerancia],
                            minutosComida = it[ShiftTable.minutosComida],
                            tipoTurno = it[ShiftTable.tipoTurno],
                            horaEntrada2 = it[ShiftTable.horaEntrada2],
                            horaSalida2 = it[ShiftTable.horaSalida2],
                            activo = it[ShiftTable.activo]
                        )
                    }
                }
                call.respond(items)
            }
            post {
                val item = call.receive<Shift>()
                DatabaseFactory.dbQuery {
                    ShiftTable.insert {
                        it[nombre] = item.nombre
                        it[horaEntrada] = item.horaEntrada
                        it[horaSalida] = item.horaSalida
                        it[minutoTolerancia] = item.minutoTolerancia
                        it[minutosComida] = item.minutosComida
                        it[tipoTurno] = item.tipoTurno
                        it[horaEntrada2] = item.horaEntrada2
                        it[horaSalida2] = item.horaSalida2
                        it[activo] = item.activo
                    }
                }
                call.respond(mapOf("status" to "ok"))
            }
            delete("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
                DatabaseFactory.dbQuery { ShiftTable.deleteWhere { ShiftTable.id eq id } }
                call.respond(mapOf("status" to "ok"))
            }
        }

        // ---------- POLÍTICAS ----------
        route("/politicas") {
            get {
                val items = DatabaseFactory.dbQuery {
                    AttendancePolicyTable.selectAll().map {
                        AttendancePolicy(
                            id = it[AttendancePolicyTable.id],
                            nombre = it[AttendancePolicyTable.nombre],
                            toleranciaRetardoMin = it[AttendancePolicyTable.toleranciaRetardoMin],
                            retardoMayorMin = it[AttendancePolicyTable.retardoMayorMin],
                            salidaAnticipadaMin = it[AttendancePolicyTable.salidaAnticipadaMin],
                            horasExtraInicio = it[AttendancePolicyTable.horasExtraInicio],
                            primaDominical = it[AttendancePolicyTable.primaDominical],
                            diasDescanso = it[AttendancePolicyTable.diasDescanso],
                            activo = it[AttendancePolicyTable.activo]
                        )
                    }
                }
                call.respond(items)
            }
            post {
                val item = call.receive<AttendancePolicy>()
                DatabaseFactory.dbQuery {
                    AttendancePolicyTable.insert {
                        it[nombre] = item.nombre
                        it[toleranciaRetardoMin] = item.toleranciaRetardoMin
                        it[retardoMayorMin] = item.retardoMayorMin
                        it[salidaAnticipadaMin] = item.salidaAnticipadaMin
                        it[horasExtraInicio] = item.horasExtraInicio
                        it[primaDominical] = item.primaDominical
                        it[diasDescanso] = item.diasDescanso
                        it[activo] = item.activo
                    }
                }
                call.respond(mapOf("status" to "ok"))
            }
            delete("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
                DatabaseFactory.dbQuery { AttendancePolicyTable.deleteWhere { AttendancePolicyTable.id eq id } }
                call.respond(mapOf("status" to "ok"))
            }
        }

        // ---------- ASIGNACIÓN DE TURNOS ----------
        route("/asignaciones") {
            get {
                val items = DatabaseFactory.dbQuery {
                    EmployeeShiftTable.selectAll().map {
                        EmployeeShiftAssignment(
                            id = it[EmployeeShiftTable.id],
                            employeeId = it[EmployeeShiftTable.employeeId],
                            employeeName = it[EmployeeShiftTable.employeeName],
                            shiftId = it[EmployeeShiftTable.shiftId],
                            shiftName = it[EmployeeShiftTable.shiftName],
                            fechaInicio = it[EmployeeShiftTable.fechaInicio],
                            activo = it[EmployeeShiftTable.activo]
                        )
                    }
                }
                call.respond(items)
            }
            post {
                val item = call.receive<EmployeeShiftAssignment>()
                DatabaseFactory.dbQuery {
                    EmployeeShiftTable.insert {
                        it[employeeId] = item.employeeId
                        it[employeeName] = item.employeeName
                        it[shiftId] = item.shiftId
                        it[shiftName] = item.shiftName
                        it[fechaInicio] = item.fechaInicio
                        it[activo] = item.activo
                    }
                }
                call.respond(mapOf("status" to "ok"))
            }
            delete("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
                DatabaseFactory.dbQuery { EmployeeShiftTable.deleteWhere { EmployeeShiftTable.id eq id } }
                call.respond(mapOf("status" to "ok"))
            }
        }

        // ---------- JUSTIFICACIONES ----------
        route("/justificaciones") {
            get {
                val items = DatabaseFactory.dbQuery {
                    JustificationTable.selectAll().map {
                        Justification(
                            id = it[JustificationTable.id],
                            employeeId = it[JustificationTable.employeeId],
                            employeeName = it[JustificationTable.employeeName],
                            fecha = it[JustificationTable.fecha],
                            tipo = it[JustificationTable.tipo],
                            motivo = it[JustificationTable.motivo],
                            evidencia = it[JustificationTable.evidencia],
                            estado = it[JustificationTable.estado],
                            autorizadoPor = it[JustificationTable.autorizadoPor],
                            fechaSolicitud = it[JustificationTable.fechaSolicitud],
                            observaciones = it[JustificationTable.observaciones]
                        )
                    }
                }
                call.respond(items)
            }
            post {
                val item = call.receive<Justification>()
                DatabaseFactory.dbQuery {
                    JustificationTable.insert {
                        it[employeeId] = item.employeeId
                        it[employeeName] = item.employeeName
                        it[fecha] = item.fecha
                        it[tipo] = item.tipo
                        it[motivo] = item.motivo
                        it[evidencia] = item.evidencia
                        it[estado] = item.estado
                        it[autorizadoPor] = item.autorizadoPor
                        it[fechaSolicitud] = item.fechaSolicitud
                        it[observaciones] = item.observaciones
                    }
                }
                call.respond(mapOf("status" to "ok"))
            }
            put("/{id}/aprobar") {
                val id = call.parameters["id"]?.toIntOrNull() ?: return@put call.respond(HttpStatusCode.BadRequest)
                val body = call.receive<Map<String, String>>()
                val autorizadoPor = body["autorizadoPor"] ?: ""
                DatabaseFactory.dbQuery {
                    JustificationTable.update({ JustificationTable.id eq id }) {
                        it[estado] = "Aprobado"
                        it[JustificationTable.autorizadoPor] = autorizadoPor
                    }
                }
                call.respond(mapOf("status" to "ok"))
            }
            put("/{id}/rechazar") {
                val id = call.parameters["id"]?.toIntOrNull() ?: return@put call.respond(HttpStatusCode.BadRequest)
                val body = call.receive<Map<String, String>>()
                val observaciones = body["observaciones"] ?: ""
                DatabaseFactory.dbQuery {
                    JustificationTable.update({ JustificationTable.id eq id }) {
                        it[estado] = "Rechazado"
                        it[JustificationTable.observaciones] = observaciones
                    }
                }
                call.respond(mapOf("status" to "ok"))
            }
            delete("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
                DatabaseFactory.dbQuery { JustificationTable.deleteWhere { JustificationTable.id eq id } }
                call.respond(mapOf("status" to "ok"))
            }
        }

        // ---------- PRE-NÓMINA (resultados calculados) ----------
        route("/resultados") {
            get {
                val items = DatabaseFactory.dbQuery {
                    PrePayrollTable.selectAll().map {
                        PrePayrollRecord(
                            id = it[PrePayrollTable.id],
                            employeeId = it[PrePayrollTable.employeeId],
                            employeeName = it[PrePayrollTable.employeeName],
                            periodoInicio = it[PrePayrollTable.periodoInicio],
                            periodoFin = it[PrePayrollTable.periodoFin],
                            diasTrabajados = it[PrePayrollTable.diasTrabajados],
                            faltas = it[PrePayrollTable.faltas],
                            retardosMenores = it[PrePayrollTable.retardosMenores],
                            retardosMayores = it[PrePayrollTable.retardosMayores],
                            salidasAnticipadas = it[PrePayrollTable.salidasAnticipadas],
                            horasTrabajadas = it[PrePayrollTable.horasTrabajadas],
                            horasExtra = it[PrePayrollTable.horasExtra],
                            primaDominical = it[PrePayrollTable.primaDominical],
                            diasDescansoTrabajados = it[PrePayrollTable.diasDescansoTrabajados],
                            observaciones = it[PrePayrollTable.observaciones],
                            estado = it[PrePayrollTable.estado]
                        )
                    }
                }
                call.respond(items)
            }
            delete("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
                DatabaseFactory.dbQuery { PrePayrollTable.deleteWhere { PrePayrollTable.id eq id } }
                call.respond(mapOf("status" to "ok"))
            }
            delete("/bulk/all") {
                DatabaseFactory.dbQuery { PrePayrollTable.deleteWhere { Op.TRUE } }
                call.respond(mapOf("status" to "ok"))
            }
        }

        // ---------- CÁLCULO DE PRE-NÓMINA ----------
        // POST /api/v1/pre-nomina/calcular?inicio=2026-08-01&fin=2026-08-15
        post("/calcular") {
            val inicioStr = call.request.queryParameters["inicio"] ?: LocalDate.now().withDayOfMonth(1).toString()
            val finStr = call.request.queryParameters["fin"] ?: LocalDate.now().toString()
            val inicio = LocalDate.parse(inicioStr)
            val fin = LocalDate.parse(finStr)

            // Cargar turnos y asignaciones
            val shifts = DatabaseFactory.dbQuery {
                ShiftTable.selectAll().associate { it[ShiftTable.id] to it }
            }
            val assignments = DatabaseFactory.dbQuery {
                EmployeeShiftTable.selectAll().filter { it[EmployeeShiftTable.activo] }.associate { it[EmployeeShiftTable.employeeId] to it[EmployeeShiftTable.shiftId] }
            }
            // Cargar política activa
            val policyRow = DatabaseFactory.dbQuery {
                AttendancePolicyTable.selectAll().firstOrNull { it[AttendancePolicyTable.activo] }
            }
            val tolRetardo = policyRow?.get(AttendancePolicyTable.toleranciaRetardoMin) ?: 5
            val retardoMayor = policyRow?.get(AttendancePolicyTable.retardoMayorMin) ?: 15
            val salidaAnt = policyRow?.get(AttendancePolicyTable.salidaAnticipadaMin) ?: 5
            val primaDom = policyRow?.get(AttendancePolicyTable.primaDominical) ?: 0.25
            val diasDescanso = (policyRow?.get(AttendancePolicyTable.diasDescanso) ?: "6").split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()

            // Cargar asistencias del periodo
            val logs = DatabaseFactory.dbQuery {
                AttendanceLogTable.selectAll()
                    .filter { row ->
                        val ts = row[AttendanceLogTable.timestamp].substringBefore(" ")
                        try { LocalDate.parse(ts) in inicio..fin } catch (e: Exception) { false }
                    }
            }

            // Agrupar por empleado y día
            data class CheckPair(var checkIn: String? = null, var checkOut: String? = null)
            val byEmployeeDay = mutableMapOf<Pair<String, LocalDate>, CheckPair>()
            for (log in logs) {
                val empId = log[AttendanceLogTable.employeeId]
                val ts = log[AttendanceLogTable.timestamp]
                val day = try { LocalDate.parse(ts.substringBefore(" ")) } catch (e: Exception) { continue }
                val timeStr = ts.substringAfter(" ").substringBefore(".")
                val status = log[AttendanceLogTable.attendanceStatus].lowercase()
                val key = empId to day
                val pair = byEmployeeDay.getOrPut(key) { CheckPair() }
                if (status.contains("in") || status.contains("entrada")) pair.checkIn = timeStr
                else if (status.contains("out") || status.contains("salida")) pair.checkOut = timeStr
            }

            // Cargar justificaciones aprobadas
            val justificadas = DatabaseFactory.dbQuery {
                JustificationTable.selectAll()
                    .filter { it[JustificationTable.estado] == "Aprobado" }
                    .map { it[JustificationTable.employeeId] to it[JustificationTable.fecha] }
            }.toSet()

            // Limpiar resultados anteriores del periodo
            DatabaseFactory.dbQuery {
                PrePayrollTable.deleteWhere {
                    (PrePayrollTable.periodoInicio eq inicioStr).and(PrePayrollTable.periodoFin eq finStr)
                }
            }

            // Cargar empleados
            val employees = DatabaseFactory.dbQuery {
                EmployeeTable.selectAll().map { it[EmployeeTable.id] to it[EmployeeTable.firstName] + " " + it[EmployeeTable.lastName] }
            }.toMap()

            // Calcular por empleado
            val resultados = mutableListOf<PrePayrollRecord>()
            val allEmpIds = (byEmployeeDay.keys.map { it.first } + assignments.keys + employees.keys).distinct()

            for (empId in allEmpIds) {
                val empName = employees[empId] ?: byEmployeeDay.values.firstOrNull()?.let { "" } ?: ""
                val shiftId = assignments[empId]
                val shiftRow = shiftId?.let { shifts[it] }

                var diasTrabajados = 0
                var faltas = 0
                var retardosMenores = 0
                var retardosMayores = 0
                var salidasAnticipadas = 0
                var horasTrabajadas = 0.0
                var horasExtra = 0.0
                var primaDominical = 0.0
                var diasDescansoTrabajados = 0

                var day = inicio
                while (!day.isAfter(fin)) {
                    val dayOfWeek = day.dayOfWeek.value % 7 // 0=domingo, 6=sabado
                    val esDescanso = dayOfWeek in diasDescanso
                    val pair = byEmployeeDay[empId to day]
                    val justificada = (empId to day.toString()) in justificadas

                    if (pair?.checkIn != null) {
                        // Registró asistencia
                        val checkInTime = LocalTime.parse(pair.checkIn)
                        val checkOutTime = pair.checkOut?.let { try { LocalTime.parse(it) } catch (e: Exception) { null } }

                        if (esDescanso) {
                            diasDescansoTrabajados++
                            if (dayOfWeek == 0) primaDominical += 1.0 // domingo
                        } else {
                            diasTrabajados++
                        }

                        // Calcular horas trabajadas
                        if (checkOutTime != null) {
                            val durMin = Duration.between(checkInTime, checkOutTime).toMinutes()
                            val comidaDescuento = shiftRow?.get(ShiftTable.minutosComida)?.toDouble() ?: 60.0
                            val horasNetas = maxOf(0.0, (durMin - comidaDescuento) / 60.0)
                            horasTrabajadas += horasNetas

                            // Horas extra: si hay hora de inicio de extra en la política
                            val horasExtraInicioStr = policyRow?.get(AttendancePolicyTable.horasExtraInicio) ?: ""
                            if (horasExtraInicioStr.isNotBlank()) {
                                val extraInicio = try { LocalTime.parse(horasExtraInicioStr) } catch (e: Exception) { null }
                                if (extraInicio != null && checkOutTime.isAfter(extraInicio)) {
                                    val extraMin = Duration.between(extraInicio, checkOutTime).toMinutes()
                                    horasExtra += maxOf(0.0, extraMin / 60.0)
                                }
                            }

                            // Salida anticipada
                            val horaSalidaStr = shiftRow?.get(ShiftTable.horaSalida) ?: ""
                            if (horaSalidaStr.isNotBlank()) {
                                val horaSalida = try { LocalTime.parse(horaSalidaStr) } catch (e: Exception) { null }
                                if (horaSalida != null && checkOutTime.isBefore(horaSalida.minusMinutes(salidaAnt.toLong())) && !esDescanso) {
                                    salidasAnticipadas++
                                }
                            }
                        }

                        // Retardos (solo si no es día de descanso y hay turno asignado)
                        if (!esDescanso && shiftRow != null) {
                            val horaEntrada = try { LocalTime.parse(shiftRow[ShiftTable.horaEntrada]) } catch (e: Exception) { null }
                            val tolMin = shiftRow[ShiftTable.minutoTolerancia]
                            if (horaEntrada != null) {
                                val diffMin = Duration.between(horaEntrada, checkInTime).toMinutes()
                                when {
                                    diffMin <= tolMin -> { /* a tiempo */ }
                                    diffMin <= retardoMayor -> retardosMenores++
                                    else -> retardosMayores++
                                }
                            }
                        }
                    } else if (!esDescanso && !justificada) {
                        // Falta si es día laborable y no hay justificación
                        faltas++
                    }

                    day = day.plusDays(1)
                }

                val record = PrePayrollRecord(
                    employeeId = empId,
                    employeeName = employees[empId] ?: empId,
                    periodoInicio = inicioStr,
                    periodoFin = finStr,
                    diasTrabajados = diasTrabajados,
                    faltas = faltas,
                    retardosMenores = retardosMenores,
                    retardosMayores = retardosMayores,
                    salidasAnticipadas = salidasAnticipadas,
                    horasTrabajadas = horasTrabajadas,
                    horasExtra = horasExtra,
                    primaDominical = primaDominical * primaDom,
                    diasDescansoTrabajados = diasDescansoTrabajados,
                    observaciones = "",
                    estado = "Calculado"
                )

                DatabaseFactory.dbQuery {
                    PrePayrollTable.insert {
                        it[PrePayrollTable.employeeId] = record.employeeId
                        it[PrePayrollTable.employeeName] = record.employeeName
                        it[PrePayrollTable.periodoInicio] = record.periodoInicio
                        it[PrePayrollTable.periodoFin] = record.periodoFin
                        it[PrePayrollTable.diasTrabajados] = record.diasTrabajados
                        it[PrePayrollTable.faltas] = record.faltas
                        it[PrePayrollTable.retardosMenores] = record.retardosMenores
                        it[PrePayrollTable.retardosMayores] = record.retardosMayores
                        it[PrePayrollTable.salidasAnticipadas] = record.salidasAnticipadas
                        it[PrePayrollTable.horasTrabajadas] = record.horasTrabajadas
                        it[PrePayrollTable.horasExtra] = record.horasExtra
                        it[PrePayrollTable.primaDominical] = record.primaDominical
                        it[PrePayrollTable.diasDescansoTrabajados] = record.diasDescansoTrabajados
                        it[PrePayrollTable.observaciones] = record.observaciones
                        it[PrePayrollTable.estado] = record.estado
                    }
                }
                resultados.add(record)
            }

            call.respond(mapOf(
                "status" to "ok",
                "registros" to resultados.size.toString(),
                "periodo" to "$inicioStr a $finStr"
            ))
        }
    }
}
