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
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font

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
            // Grupo de frecuencia de pago: "Semanal", "Quincenal" o vacio = todos
            val grupo = call.request.queryParameters["grupo"]?.trim()?.takeIf { it.isNotBlank() && it != "Todos" }
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

            // Los timestamps en la BD llegan en formato ISO con offset, tal cual
            // los manda la lectora: "2026-09-11T21:00:27-06:00". El parser viejo
            // asumia un formato con espacio ("YYYY-MM-DD HH:MM:SS") que nunca
            // coincidia con nada real -> descartaba TODOS los registros en
            // silencio y por eso el calculo marcaba falta a todo el mundo.
            fun parseTs(raw: String): java.time.OffsetDateTime? = try {
                java.time.OffsetDateTime.parse(raw)
            } catch (e: Exception) {
                try {
                    // Compatibilidad con datos viejos sin offset ("YYYY-MM-DD HH:MM:SS")
                    val norm = raw.substringBefore(".").replace(" ", "T")
                    java.time.LocalDateTime.parse(norm).atOffset(java.time.ZoneOffset.of("-06:00"))
                } catch (e2: Exception) { null }
            }

            // Cargar asistencias del periodo
            val logs = DatabaseFactory.dbQuery {
                AttendanceLogTable.selectAll()
                    .filter { row ->
                        val dt = parseTs(row[AttendanceLogTable.timestamp]) ?: return@filter false
                        dt.toLocalDate() in inicio..fin
                    }
            }

            // Agrupar por empleado y día
            data class CheckPair(var checkIn: String? = null, var checkOut: String? = null)
            val byEmployeeDay = mutableMapOf<Pair<String, LocalDate>, CheckPair>()
            for (log in logs) {
                val empId = log[AttendanceLogTable.employeeId]
                val ts = log[AttendanceLogTable.timestamp]
                val dt = parseTs(ts) ?: continue
                val day = dt.toLocalDate()
                val timeStr = dt.toLocalTime().toString()
                val status = log[AttendanceLogTable.attendanceStatus].lowercase()
                val key = empId to day
                val pair = byEmployeeDay.getOrPut(key) { CheckPair() }
                if (status.contains("in") || status.contains("entrada")) pair.checkIn = timeStr
                else if (status.contains("out") || status.contains("salida")) pair.checkOut = timeStr
            }

            // ---------- PATRON DE DIAS LABORALES POR EMPLEADO ----------
            // No todo mundo trabaja los mismos dias (ej. alguien de Lunes a Jueves
            // no debe tener falta el Viernes porque no le corresponde asistencia).
            // Como no existe un catalogo de "dias laborales" por empleado, lo
            // inferimos de su propio historial de checadas: miramos las 8 semanas
            // previas al periodo y, por cada dia de la semana, en que fraccion de
            // sus semanas activas (semanas donde SI vino algun dia) tiene checada.
            // Si aparece esa fraccion >= 50%, ese dia de la semana es "suyo".
            // Si no hay suficiente historial (menos de 2 semanas activas), no se
            // aplica el filtro y se usa el comportamiento anterior (todos los dias
            // no-descanso cuentan) para no ocultar faltas reales de gente nueva.
            val lookbackDesde = inicio.minusDays(56)
            val lookbackHasta = inicio.minusDays(1)
            val logsHistoricos = DatabaseFactory.dbQuery {
                AttendanceLogTable.selectAll()
                    .filter { row ->
                        val dt = parseTs(row[AttendanceLogTable.timestamp]) ?: return@filter false
                        dt.toLocalDate() in lookbackDesde..lookbackHasta
                    }
                    .map { it[AttendanceLogTable.employeeId] to parseTs(it[AttendanceLogTable.timestamp])!!.toLocalDate() }
            }
            val isoWeekOf = { d: LocalDate -> d.get(java.time.temporal.WeekFields.ISO.weekBasedYear()) * 100 + d.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear()) }
            val semanasActivasPorEmpleado = mutableMapOf<String, MutableSet<Int>>()
            val semanasPorDiaSemana = mutableMapOf<String, MutableMap<Int, MutableSet<Int>>>() // empId -> dow(0..6) -> semanas
            for ((empId, day) in logsHistoricos) {
                val week = isoWeekOf(day)
                val dow = day.dayOfWeek.value % 7
                semanasActivasPorEmpleado.getOrPut(empId) { mutableSetOf() }.add(week)
                semanasPorDiaSemana.getOrPut(empId) { mutableMapOf() }.getOrPut(dow) { mutableSetOf() }.add(week)
            }
            fun diasLaboralesDeEmpleado(empId: String): Set<Int>? {
                val semanasActivas = semanasActivasPorEmpleado[empId] ?: return null
                if (semanasActivas.size < 2) return null // sin suficiente historial, no filtrar
                val porDia = semanasPorDiaSemana[empId] ?: return null
                val minimo = maxOf(1, (semanasActivas.size * 0.5).let { Math.ceil(it).toInt() })
                return (0..6).filter { dow -> (porDia[dow]?.size ?: 0) >= minimo }.toSet()
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

            // Cargar empleados (id -> nombre) y su frecuencia de pago
            val employees = DatabaseFactory.dbQuery {
                EmployeeTable.selectAll().map { it[EmployeeTable.id] to it[EmployeeTable.firstName] + " " + it[EmployeeTable.lastName] }
            }.toMap()
            val frecuenciaPorEmpleado = DatabaseFactory.dbQuery {
                EmployeeTable.selectAll().map { it[EmployeeTable.id] to (it[EmployeeTable.paymentFrequency] ?: "Semanal") }
            }.toMap()

            // Calcular por empleado
            val resultados = mutableListOf<PrePayrollRecord>()
            var allEmpIds = (byEmployeeDay.keys.map { it.first } + assignments.keys + employees.keys).distinct()
            // Filtrar por grupo semanal/quincenal cuando se pide
            if (grupo != null) {
                allEmpIds = allEmpIds.filter { frecuenciaPorEmpleado[it] == grupo }
            }

            for (empId in allEmpIds) {
                val empName = employees[empId] ?: byEmployeeDay.values.firstOrNull()?.let { "" } ?: ""
                val shiftId = assignments[empId]
                val shiftRow = shiftId?.let { shifts[it] }
                val diasLaborales = diasLaboralesDeEmpleado(empId) // null = sin historial suficiente, no filtrar

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
                    // Dia que no le corresponde a ESTE empleado segun su patron real
                    // de asistencia (ej. trabaja Lunes-Jueves, Viernes no es suyo).
                    val noLeCorresponde = diasLaborales != null && dayOfWeek !in diasLaborales
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
                    } else if (!esDescanso && !justificada && !noLeCorresponde) {
                        // Falta solo si es dia laborable, no hay justificacion Y
                        // le corresponde asistir ese dia segun su patron real.
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

        // ---------- AJUSTES MANUALES (correcciones de ISR/IMSS, anticipos) ----------
        // Devuelve los ajustes manuales capturados para un periodo.
        get("/ajustes") {
            val inicioStr = call.request.queryParameters["inicio"] ?: ""
            val finStr = call.request.queryParameters["fin"] ?: ""
            val items = DatabaseFactory.dbQuery {
                PayrollOverrideTable.selectAll()
                    .filter { it[PayrollOverrideTable.periodoInicio] == inicioStr && it[PayrollOverrideTable.periodoFin] == finStr }
                    .map {
                        mapOf(
                            "employeeId" to it[PayrollOverrideTable.employeeId],
                            "isr" to it[PayrollOverrideTable.isr],
                            "imss" to it[PayrollOverrideTable.imss],
                            "anticipo" to it[PayrollOverrideTable.anticipo],
                            "otros" to it[PayrollOverrideTable.otros],
                            "infonavit" to it[PayrollOverrideTable.infonavit],
                            "fondoAhorro" to it[PayrollOverrideTable.fondoAhorro],
                            "fonacot" to it[PayrollOverrideTable.fonacot],
                            "diasProyectados" to it[PayrollOverrideTable.diasProyectados]
                        )
                    }
            }
            call.respond(items)
        }

        // Captura o corrige un ajuste manual (si isr/imss llegan null se vuelve
        // al calculo automatico). Borra el registro previo del mismo empleado
        // y periodo, y guarda el nuevo.
        post("/ajuste") {
            val body = call.receive<Map<String, String>>()
            val empId = body["employeeId"] ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "employeeId requerido"))
            val inicioStr = body["periodoInicio"] ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "periodoInicio requerido"))
            val finStr = body["periodoFin"] ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "periodoFin requerido"))
            fun d(k: String): Double? = body[k]?.takeIf { it.isNotBlank() }?.replace(",", "")?.toDoubleOrNull()
            val diasProy = body["diasProyectados"]?.takeIf { it.isNotBlank() }?.toIntOrNull() ?: 0
            DatabaseFactory.dbQuery {
                PayrollOverrideTable.deleteWhere {
                    (PayrollOverrideTable.employeeId eq empId).and(PayrollOverrideTable.periodoInicio eq inicioStr).and(PayrollOverrideTable.periodoFin eq finStr)
                }
                PayrollOverrideTable.insert {
                    it[employeeId] = empId
                    it[periodoInicio] = inicioStr
                    it[periodoFin] = finStr
                    it[isr] = d("isr")
                    it[imss] = d("imss")
                    it[anticipo] = d("anticipo") ?: 0.0
                    it[otros] = d("otros") ?: 0.0
                    it[infonavit] = d("infonavit")
                    it[fondoAhorro] = d("fondoAhorro")
                    it[fonacot] = d("fonacot")
                    it[diasProyectados] = diasProy
                    it[updatedBy] = body["updatedBy"]
                }
            }
            call.respond(mapOf("status" to "success"))
        }

        // ---------- DATOS FISCALES / CALCULO DE DINERO ----------
        // Datos de la empresa (de la guia de configuracion del cliente).
        val EMPRESA_NOMBRE = "SHELSER"
        val EMPRESA_RFC = "CGU101126SA2"
        val EMPRESA_DOMICILIO = "CALZADA DEL VALLE #400, COLONIA DEL VALLE, SAN PEDRO GARZA GARCIA"

        // Tabla semanal ISR (Art. 96 LISR): limite inferior -> cuota fija + % excedente.
        val TABLA_ISR_SEMANAL = listOf(
            Triple(0.01, 0.0, 0.0),
            Triple(176.67, 5.30, 0.0688),
            Triple(1487.63, 100.40, 0.1077),
            Triple(2597.29, 219.83, 0.1604),
            Triple(3706.91, 397.81, 0.1792),
            Triple(4486.15, 537.75, 0.2136),
            Triple(5265.39, 704.35, 0.2352),
            Triple(6247.94, 935.53, 0.30),
            Triple(7230.49, 1230.57, 0.32),
            Triple(10401.79, 2316.95, 0.34),
            Triple(20803.82, 5516.55, 0.35)
        )

        fun isrSemanal(base: Double): Double {
            val li = TABLA_ISR_SEMANAL.last { base >= it.first }
            return maxOf(0.0, li.second + (base - li.first) * li.third)
        }

        // ISR prorrateado para periodos distintos de 7 dias.
        fun isrPeriodo(baseGravable: Double, dias: Int): Double {
            if (dias <= 0 || baseGravable <= 0.0) return 0.0
            val semanal = isrSemanal(baseGravable * 7.0 / dias)
            return maxOf(0.0, semanal * dias / 7.0)
        }

        // IMSS obrero (aproximacion de las cuotas del trabajador), separado en dos
        // renglones para el recibo: IMSS (enfermedad/maternidad + invalidez y vida)
        // y Cesantia y Vejez, tal como se desglosa en el listado maestro de nomina.
        val UMA_DIARIA = 113.14
        fun imssObrero(sbc: Double, dias: Int): Double {
            if (sbc <= 0.0 || dias <= 0) return 0.0
            val excedenteBase = maxOf(0.0, sbc - 3 * UMA_DIARIA)
            val diario = sbc * 0.00625 + excedenteBase * 0.01025
            return diario * dias
        }
        fun cesantiaVejezObrero(sbc: Double, dias: Int): Double {
            if (sbc <= 0.0 || dias <= 0) return 0.0
            return sbc * 0.01125 * dias
        }

        fun dinero(v: Double): String = String.format("$%,.2f", v)
        fun limpio(v: Double): String = String.format("%,.2f", v)

        // ---------- PDF DE PRE-NOMINA ----------
        // GET /pdf?inicio=2026-09-21&fin=2026-09-25[&pid=10010]
        // Genera un recibo estilo PreAsyst por empleado: una pagina con
        // percepciones y deducciones, totales y espacio de firma. Si pid
        // viene, solo ese empleado; si no, todos los del periodo.
        get("/pdf") {
            val inicioStr = call.request.queryParameters["inicio"] ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "inicio requerido"))
            val finStr = call.request.queryParameters["fin"] ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "fin requerido"))
            val pid = call.request.queryParameters["pid"]
            // Grupo de frecuencia de pago: "Semanal", "Quincenal" o vacio = todos
            val grupo = call.request.queryParameters["grupo"]?.trim()?.takeIf { it.isNotBlank() && it != "Todos" }

            val records = DatabaseFactory.dbQuery {
                PrePayrollTable.selectAll()
                    .filter { it[PrePayrollTable.periodoInicio] == inicioStr && it[PrePayrollTable.periodoFin] == finStr }
                    .map { PrePayrollRecord(
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
                        estado = it[PrePayrollTable.estado]
                    ) }
                    .filter { pid == null || it.employeeId == pid }
                    .sortedBy { it.employeeId }
            }
            if (records.isEmpty()) return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "No hay pre-nomina calculada para ese periodo"))

            val employees = DatabaseFactory.dbQuery {
                EmployeeTable.selectAll().associate { it[EmployeeTable.id] to it }
            }
            // Filtrar por grupo semanal/quincenal cuando se pide
            val recordsFiltrados = if (grupo != null) {
                records.filter { employees[it.employeeId]?.get(EmployeeTable.paymentFrequency) == grupo }
            } else records
            val overrides = DatabaseFactory.dbQuery {
                PayrollOverrideTable.selectAll()
                    .filter { it[PayrollOverrideTable.periodoInicio] == inicioStr && it[PayrollOverrideTable.periodoFin] == finStr }
                    .associate { it[PayrollOverrideTable.employeeId] to it }
            }
            val policyRow = DatabaseFactory.dbQuery { AttendancePolicyTable.selectAll().firstOrNull { it[AttendancePolicyTable.activo] } }
            val primaDomPct = policyRow?.get(AttendancePolicyTable.primaDominical) ?: 0.25

            val diasPeriodo = try { java.time.temporal.ChronoUnit.DAYS.between(LocalDate.parse(inicioStr), LocalDate.parse(finStr)).toInt() + 1 } catch (e: Exception) { 7 }

            if (recordsFiltrados.isEmpty()) return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "No hay pre-nomina del grupo '$grupo' para ese periodo"))

            PDDocument().use { doc ->
                    // ---- Paginas: 2 recibos por hoja (mitad superior e inferior) ----
                    data class Recibo(val r: PrePayrollRecord, val emp: org.jetbrains.exposed.sql.ResultRow?, val ov: org.jetbrains.exposed.sql.ResultRow?,
                                      val percepciones: List<Pair<String, Double>>, val deducciones: List<Pair<String, Double>>,
                                      val neto: Double, val sueldoDiario: Double?, val sbc: Double?,
                                      val fondoAhorroEmpresa: Double = 0.0, val diasProyectados: Int = 0)

                    // Primero calcular todos los recibos del periodo...
                    val recibos = recordsFiltrados.map { r ->
                        val emp = employees[r.employeeId]
                        val ov = overrides[r.employeeId]
                        val sueldoDiario = emp?.get(EmployeeTable.salary)
                            ?: (if ((emp?.get(EmployeeTable.position) ?: "").contains("Operador", ignoreCase = true)) 337.31 else null)
                        val sbc = emp?.get(EmployeeTable.sbc) ?: (sueldoDiario?.times(1.05))

                        // Dias proyectados: cuando la nomina se envia ANTES de que termine el
                        // periodo real (p.ej. se calcula el 21 pero se paga hasta el 30), se
                        // pueden capturar dias adicionales estimados (sin faltas) desde el
                        // dialogo de "Ajustar". No modifica la asistencia real, solo el pago.
                        val diasProyectados = ov?.get(PayrollOverrideTable.diasProyectados) ?: 0
                        val diasTrabEfectivos = r.diasTrabajados + diasProyectados
                        val diasPagados = diasTrabEfectivos + r.diasDescansoTrabajados
                        // Si se agregan dias proyectados, el periodo tambien se "alarga" para
                        // efectos de prorrateo de ISR/Infonavit (de lo contrario se sobre-
                        // tasaria el ISR al anualizar como si fueran solo los dias calendario
                        // originales cuando en realidad se esta pagando mas dias).
                        val diasPeriodoEfectivo = diasPeriodo + diasProyectados

                        val sueldoBase = (sueldoDiario ?: 0.0) * diasTrabEfectivos
                        val horasExtraPesos = r.horasExtra * ((sueldoDiario ?: 0.0) / 8.0) * 2.0
                        val primaDomPesos = r.primaDominical * (sueldoDiario ?: 0.0) * primaDomPct
                        val descansoTrabPesos = r.diasDescansoTrabajados.toDouble() * (sueldoDiario ?: 0.0) * 2.0
                        val etiquetaSueldo = if (diasProyectados > 0)
                            "SUELDO BASE (${r.diasTrabajados}+$diasProyectados proy. dias)" else "SUELDO BASE (${r.diasTrabajados} dias)"
                        val percepciones = listOf(
                            Pair(etiquetaSueldo, sueldoBase),
                            Pair("HORAS EXTRAS (${limpio(r.horasExtra)} h)", horasExtraPesos),
                            Pair("PRIMA DOMINICAL (${limpio(r.primaDominical)} dias)", primaDomPesos),
                            Pair("DESCANSOS TRABAJADOS (${r.diasDescansoTrabajados})", descansoTrabPesos)
                        ).filter { it.second > 0.0 }
                        val totalPercepciones = percepciones.sumOf { it.second }

                        val isrAuto = isrPeriodo(totalPercepciones, diasPeriodoEfectivo)
                        val imssAuto = imssObrero(sbc ?: 0.0, diasPagados)
                        val cesantiaAuto = cesantiaVejezObrero(sbc ?: 0.0, diasPagados)
                        val isr = ov?.get(PayrollOverrideTable.isr) ?: isrAuto
                        val imss = ov?.get(PayrollOverrideTable.imss) ?: imssAuto
                        val cesantiaVejez = cesantiaAuto
                        val anticipo = ov?.get(PayrollOverrideTable.anticipo) ?: 0.0
                        val otros = ov?.get(PayrollOverrideTable.otros) ?: 0.0

                        // Infonavit: monto fijo por periodo capturado en la ficha del empleado
                        // (como aparece en el listado maestro de nomina) o corregido manualmente.
                        val infonavit = ov?.get(PayrollOverrideTable.infonavit) ?: emp?.get(EmployeeTable.infonavitDescuento) ?: 0.0

                        // Fonacot: monto fijo por periodo (credito Fonacot), igual que Infonavit.
                        val fonacot = ov?.get(PayrollOverrideTable.fonacot) ?: emp?.get(EmployeeTable.fonacotDescuento) ?: 0.0

                        // Fondo de ahorro: % configurado en la ficha del empleado sobre el
                        // sueldo base del periodo. El trabajador se descuenta aqui; la
                        // aportacion de la empresa se muestra por separado (informativa,
                        // no se suma al neto porque no se paga en este recibo).
                        val fondoAhorroPct = emp?.get(EmployeeTable.fondoAhorroPct) ?: 0.0
                        val fondoAhorroTrabajador = ov?.get(PayrollOverrideTable.fondoAhorro) ?: (sueldoBase * fondoAhorroPct)
                        val fondoAhorroEmpresa = sueldoBase * fondoAhorroPct

                        val deducciones = listOf(
                            Pair("ISR (RETENCION)", isr),
                            Pair("IMSS (CUOTA OBRERA)", imss),
                            Pair("CESANTIA Y VEJEZ", cesantiaVejez),
                            Pair("INFONAVIT", infonavit),
                            Pair("FONACOT", fonacot),
                            Pair("FONDO DE AHORRO (TRABAJADOR)", fondoAhorroTrabajador),
                            Pair("ANTICIPO DE NOMINA", anticipo),
                            Pair("OTROS DESCUENTOS", otros)
                        ).filter { it.second > 0.0 }
                        val totalDeducciones = deducciones.sumOf { it.second }
                        val neto = totalPercepciones - totalDeducciones
                        Recibo(r, emp, ov, percepciones, deducciones, neto, sueldoDiario, sbc, fondoAhorroEmpresa, diasProyectados)
                    }

                    // ...y luego dibujar dos por hoja, compactando el espacio
                    var cs: PDPageContentStream? = null
                    recibos.forEachIndexed { idx, rec ->
                        val r = rec.r; val emp = rec.emp
                        if (idx % 2 == 0) {
                            cs?.close()
                            val page = PDPage(PDRectangle(612f, 792f))
                            doc.addPage(page)
                            cs = PDPageContentStream(doc, page)
                            cs?.let { c ->
                                // pie de pagina, una sola vez por hoja
                                c.beginText()
                                c.setFont(PDType1Font.HELVETICA, 7f)
                                c.newLineAtOffset(306f - PDType1Font.HELVETICA.getStringWidth("Documento informativo de pre-nomina generado por RHNAF") / 1000f * 7f / 2f, 30f)
                                c.showText("Documento informativo de pre-nomina generado por RHNAF")
                                c.endText()
                            }
                        }
                        val stream = cs!!
                        val yTop = if (idx % 2 == 0) 762f else 386f
                        val folioNum = idx + 1
                        fun texto(x: Float, y: Float, txt: String, size: Float = 8f, bold: Boolean = false, center: Boolean = false, right: Boolean = false) {
                            stream.beginText()
                            val font = if (bold) PDType1Font.HELVETICA_BOLD else PDType1Font.HELVETICA
                            stream.setFont(font, size)
                            val ancho = font.getStringWidth(txt) / 1000f * size
                            val startX = when {
                                right -> x - ancho
                                center -> x - ancho / 2f
                                else -> x
                            }
                            stream.newLineAtOffset(startX, y)
                            stream.showText(txt)
                            stream.endText()
                        }
                        val margenIzq = 40f
                        // Encabezado compacto
                        texto(margenIzq, yTop, EMPRESA_NOMBRE, 11f, bold = true)
                        texto(572f, yTop, "RECIBO DE PRE-NOMINA", 10f, bold = true, center = true)
                        texto(margenIzq, yTop - 12f, "RFC: $EMPRESA_RFC  |  $EMPRESA_DOMICILIO", 6.5f)
                        texto(572f, yTop - 11f, "FOLIO: PN-${inicioStr.replace("-", "")}-${"%03d".format(folioNum)}", 7.5f, center = true)
                        texto(margenIzq, yTop - 22f, "PERIODO: $inicioStr AL $finStr", 8f, bold = true)
                        texto(572f, yTop - 21f, "FECHA DE IMPRESION: ${LocalDate.now()}", 6.5f, center = true)

                        // Linea divisoria
                        stream.moveTo(margenIzq, yTop - 30f); stream.lineTo(572f, yTop - 30f); stream.stroke()

                        // Datos del empleado (dos columnas, compacto)
                        var y = yTop - 42f
                        fun par(etiqueta: String, valor: String?, x: Float) {
                            if (valor.isNullOrBlank()) return
                            texto(x, y, "$etiqueta $valor", 7.5f)
                            y -= 9.5f
                        }
                        par("EMPLEADO:", (emp?.get(EmployeeTable.firstName) ?: "") + " " + (emp?.get(EmployeeTable.lastName) ?: r.employeeName), margenIzq)
                        par("NO. EMPLEADO:", r.employeeId, margenIzq)
                        par("RFC:", emp?.get(EmployeeTable.rfc), margenIzq)
                        par("CURP:", emp?.get(EmployeeTable.curp), margenIzq)
                        par("NSS:", emp?.get(EmployeeTable.nss), margenIzq)
                        par("PUESTO:", emp?.get(EmployeeTable.position), margenIzq)
                        val yIzq = y
                        y = yTop - 42f
                        par("DEPARTAMENTO:", emp?.get(EmployeeTable.department), 320f)
                        par("FECHA INGRESO:", emp?.get(EmployeeTable.entryDate), 320f)
                        par("SUELDO DIARIO:", rec.sueldoDiario?.let { dinero(it) }, 320f)
                        par("SBC (IMSS):", rec.sbc?.let { dinero(it) }, 320f)
                        par("DIAS TRABAJADOS:", r.diasTrabajados.toString(), 320f)
                        par("FALTAS:", r.faltas.toString(), 320f)
                        y = minOf(yIzq, y) - 6f

                        if (rec.sueldoDiario == null) {
                            texto(306f, y, "SIN SUELDO CAPTURADO: capture el sueldo diario en la ficha del empleado", 8f, bold = true, center = true)
                            y -= 16f
                        }

                        // Encabezados de columnas.
                        // Columna de percepciones: concepto en margenIzq..perAmountX (importe
                        // alineado a la derecha en perAmountX). Columna de deducciones:
                        // concepto en dedConceptoX..dedAmountX (importe a la derecha en
                        // dedAmountX). El hueco entre perAmountX y dedConceptoX evita el
                        // traslape que se veia cuando los importes eran anchos (montos con
                        // miles) y quedaban encima del texto de la columna de deducciones.
                        val perAmountX = 300f
                        val dedConceptoX = 330f
                        val dedAmountX = 572f
                        stream.moveTo(margenIzq, y + 4f); stream.lineTo(dedAmountX, y + 4f); stream.stroke()
                        y -= 12f
                        texto(margenIzq, y, "PERCEPCIONES", 9f, bold = true)
                        texto(dedConceptoX, y, "DEDUCCIONES", 9f, bold = true)
                        y -= 11f
                        stream.moveTo(margenIzq, y + 4f); stream.lineTo(dedAmountX, y + 4f); stream.stroke()
                        y -= 12f

                        var yPer = y
                        var yDed = y
                        for ((concepto, importe) in rec.percepciones) {
                            texto(margenIzq, yPer, concepto, 7.5f)
                            texto(perAmountX, yPer, dinero(importe), 7.5f, right = true)
                            yPer -= 11f
                        }
                        texto(margenIzq, yPer, "TOTAL PERCEPCIONES", 7.5f, bold = true)
                        texto(perAmountX, yPer, dinero(rec.percepciones.sumOf { it.second }), 7.5f, bold = true, right = true)

                        for ((concepto, importe) in rec.deducciones) {
                            texto(dedConceptoX, yDed, concepto, 7.5f)
                            texto(dedAmountX, yDed, dinero(importe), 7.5f, right = true)
                            yDed -= 11f
                        }
                        texto(dedConceptoX, yDed, "TOTAL DEDUCCIONES", 7.5f, bold = true)
                        texto(dedAmountX, yDed, dinero(rec.deducciones.sumOf { it.second }), 7.5f, bold = true, right = true)

                        y = minOf(yPer, yDed) - 16f
                        texto(dedConceptoX, y, "NETO A PAGAR: ${dinero(rec.neto)}", 10f, bold = true)

                        // Notas informativas (no afectan el neto): dias proyectados y
                        // aportacion patronal al fondo de ahorro, cuando aplican.
                        if (rec.diasProyectados > 0 || rec.fondoAhorroEmpresa > 0.0) {
                            y -= 12f
                            if (rec.diasProyectados > 0) {
                                texto(margenIzq, y, "* Incluye ${rec.diasProyectados} dia(s) proyectado(s) (nomina enviada antes de fin de periodo).", 6.5f)
                                y -= 9f
                            }
                            if (rec.fondoAhorroEmpresa > 0.0) {
                                texto(margenIzq, y, "* Aportacion patronal fondo de ahorro: ${dinero(rec.fondoAhorroEmpresa)} (informativa, no incluida en el neto).", 6.5f)
                                y -= 9f
                            }
                        }

                        // Firmas
                        val yFirma = y - 18f
                        texto(140f, yFirma, "_______________________", 8f, center = true)
                        texto(140f, yFirma - 10f, "ELABORO", 7f, center = true)
                        texto(430f, yFirma, "_______________________", 8f, center = true)
                        texto(430f, yFirma - 10f, "RECIBI DE CONFORMIDAD: ${emp?.get(EmployeeTable.firstName) ?: ""} ${emp?.get(EmployeeTable.lastName) ?: ""}", 7f, center = true)
                    }
                    cs?.close()
                val out = java.io.ByteArrayOutputStream()
                doc.save(out)
                val bytes = out.toByteArray()
                call.response.headers.append(HttpHeaders.ContentDisposition, "attachment; filename=\"pre_nomina_${inicioStr}_a_${finStr}.pdf\"")
                call.respondBytes(bytes, ContentType.Application.Pdf)
            }
        }
    }
}
