package com.example.rhnaf.routes

import com.example.rhnaf.auth.Roles
import com.example.rhnaf.auth.requireRoleOr403
import com.example.rhnaf.database.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import com.example.rhnaf.shared.model.EhsCustomEvent
import io.ktor.http.*
import io.ktor.server.request.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * EHS - Calendario de vencimientos y actividades.
 * Concentra en una sola vista mensual las fechas límite de:
 * obligaciones de la matriz legal (permisos/NOMs), capacitaciones,
 * simulacros, planes de acción, inspecciones y documentos de
 * contratistas. Los permisos críticos se destacan para que el
 * responsable vea primero lo que puede costar una clausura.
 */

@Serializable
data class EhsCalendarEvent(
    val fecha: String,          // yyyy-MM-dd
    val tipo: String,           // obligacion, capacitacion, simulacro, accion, inspeccion, contratista
    val id: Int,
    val titulo: String,
    val detalle: String = "",
    val categoria: String = "",
    val esCritico: Boolean = false,
    val estado: String = "",    // Vigente/PorVencer/Vencido para obligaciones
    val tareasAbiertas: Int = 0 // acciones correctivas abiertas vinculadas
)

@Serializable
data class EhsCalendarResponse(
    val mes: String,            // yyyy-MM
    val diasEnMes: Int,
    val primerDiaSemana: Int,   // 0 = domingo ... 6 = sábado
    val hoy: String,            // yyyy-MM-dd
    val eventos: List<EhsCalendarEvent>
)

private val FORMATOS = listOf(
    DateTimeFormatter.ISO_LOCAL_DATE,
    DateTimeFormatter.ofPattern("d/M/uuuu")
)

private fun fechaCal(value: String): LocalDate? = FORMATOS.firstNotNullOfOrNull { f ->
    runCatching { LocalDate.parse(value.trim(), f) }.getOrNull()
}

/** Etiqueta legible del tipo de evento para el ICS. */
private fun etiquetaTipo(tipo: String): String = when (tipo) {
    "obligacion" -> "Obligación legal"
    "documento" -> "Documento de cumplimiento"
    "capacitacion" -> "Capacitación"
    "simulacro" -> "Simulacro"
    "accion" -> "Acción correctiva"
    "inspeccion" -> "Inspección"
    "contratista" -> "Contratista"
    "evento" -> "Evento"
    else -> tipo
}

fun Route.ehsCalendarRouting() {
    val calendarWrite = setOf(Roles.ADMIN, Roles.SEGURIDAD, Roles.RH)

    // Eventos propios capturados desde el calendario (clic en un día).
    route("/api/v1/ehs/calendario/eventos") {
        post {
            safeApiCall(call) {
                requireRoleOr403(call, calendarWrite) ?: return@safeApiCall
                val ev = call.receive<EhsCustomEvent>()
                if (ev.fecha.isBlank() || ev.titulo.isBlank()) {
                    return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Fecha y título son obligatorios"))
                }
                DatabaseFactory.dbQuery {
                    EhsCustomEventTable.insert {
                        it[fecha] = ev.fecha
                        it[tipo] = "evento"
                        it[titulo] = ev.titulo
                        it[detalle] = ev.detalle
                        it[responsable] = ev.responsable
                        it[estado] = ev.estado
                    }
                }
                call.respond(HttpStatusCode.Created, mapOf("status" to "ok"))
            }
        }
        delete("/{id}") {
            safeApiCall(call) {
                requireRoleOr403(call, calendarWrite) ?: return@safeApiCall
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                DatabaseFactory.dbQuery { EhsCustomEventTable.deleteWhere { EhsCustomEventTable.id eq id } }
                call.respond(mapOf("status" to "ok"))
            }
        }
    }

    get("/api/v1/ehs/calendario") {
        safeApiCall(call) {
            requireRoleOr403(call, Roles.ALL) ?: return@safeApiCall
            val hoy = LocalDate.now()
            val mesParam = call.request.queryParameters["mes"]?.trim()
            val mes = try {
                if (mesParam.isNullOrBlank()) YearMonth.from(hoy) else YearMonth.parse(mesParam)
            } catch (e: Exception) { YearMonth.from(hoy) }

            val ini = mes.atDay(1)
            val fin = mes.atEndOfMonth()

            val eventos = DatabaseFactory.dbQuery {
                val all = mutableListOf<EhsCalendarEvent>()

                fun add(fecha: LocalDate?, tipo: String, id: Int, titulo: String, detalle: String, categoria: String, esCritico: Boolean, estado: String, tareasAbiertas: Int = 0) {
                    if (fecha == null || fecha.isBefore(ini) || fecha.isAfter(fin)) return
                    all.add(EhsCalendarEvent(fecha.toString(), tipo, id, titulo, detalle, categoria, esCritico, estado, tareasAbiertas))
                }

                // Tareas abiertas por obligación (acciones correctivas vinculadas)
                val tareasPorObligacion = EhsActionTable.selectAll()
                    .filter { it[EhsActionTable.origenTipo] == "matriz_legal" && it[EhsActionTable.estado] != "Cerrada" }
                    .groupingBy { it[EhsActionTable.origenId] }.eachCount()
                // Documentos de cumplimiento con su propia vigencia y recordatorio
                val docsPorObligacion = LegalMatrixDocTable.selectAll()
                    .filter { fechaCal(it[LegalMatrixDocTable.fechaVigencia]) != null }
                    .groupBy { it[LegalMatrixDocTable.matrizId] }
                // Obligaciones de la matriz legal aplicables
                LegalMatrixTable.selectAll().forEach { row ->
                    val aplica = row[LegalMatrixTable.aplica]
                    if (aplica == "Si" || aplica == "Pendiente") {
                        val v = fechaCal(row[LegalMatrixTable.fechaVigencia])
                        val estado = when {
                            aplica == "Pendiente" -> "Pendiente"
                            v == null -> "Pendiente"
                            v.isBefore(hoy) -> "Vencido"
                            !v.isAfter(hoy.plusDays(row[LegalMatrixTable.diasAlertaPrevia].toLong())) -> "PorVencer"
                            else -> "Vigente"
                        }
                        add(v, "obligacion", row[LegalMatrixTable.id], row[LegalMatrixTable.clave],
                            row[LegalMatrixTable.titulo], row[LegalMatrixTable.categoria],
                            row[LegalMatrixTable.esCritico], estado, tareasPorObligacion[row[LegalMatrixTable.id]] ?: 0)
                        // Cada documento de cumplimiento aparece como evento propio
                        docsPorObligacion[row[LegalMatrixTable.id]]?.forEach { drow ->
                            val dv = fechaCal(drow[LegalMatrixDocTable.fechaVigencia])!!
                            val dEstado = when {
                                dv.isBefore(hoy) -> "Vencido"
                                !dv.isAfter(hoy.plusDays(drow[LegalMatrixDocTable.recordatorioDias].toLong())) -> "PorVencer"
                                else -> "Vigente"
                            }
                            add(dv, "documento", drow[LegalMatrixDocTable.id],
                                drow[LegalMatrixDocTable.nombre].ifBlank { "Documento" },
                                drow[LegalMatrixDocTable.tipoDocumento],
                                row[LegalMatrixTable.categoria], row[LegalMatrixTable.esCritico], dEstado)
                        }
                    }
                }
                SafetyTrainingTable.selectAll().forEach { row ->
                    add(fechaCal(row[SafetyTrainingTable.proximaFecha]), "capacitacion", row[SafetyTrainingTable.id],
                        row[SafetyTrainingTable.tema], row[SafetyTrainingTable.instructor], "Capacitacion", false, "")
                }
                EmergencyDrillTable.selectAll().forEach { row ->
                    add(fechaCal(row[EmergencyDrillTable.fecha]), "simulacro", row[EmergencyDrillTable.id],
                        row[EmergencyDrillTable.tipo], row[EmergencyDrillTable.resultado], "Simulacro", false, "")
                }
                EhsActionTable.selectAll().forEach { row ->
                    if (row[EhsActionTable.estado] != "Cerrada")
                        add(fechaCal(row[EhsActionTable.fechaLimite]), "accion", row[EhsActionTable.id],
                            row[EhsActionTable.titulo], row[EhsActionTable.estado], "Accion", false, "")
                }
                SafetyInspectionTable.selectAll().forEach { row ->
                    add(fechaCal(row[SafetyInspectionTable.fecha]), "inspeccion", row[SafetyInspectionTable.id],
                        row[SafetyInspectionTable.area], row[SafetyInspectionTable.estado], "Inspeccion", false, "")
                }
                EhsContractorTable.selectAll().forEach { row ->
                    if (row[EhsContractorTable.estado] != "Suspendido")
                        add(fechaCal(row[EhsContractorTable.vigenciaDocumento]), "contratista", row[EhsContractorTable.id],
                            row[EhsContractorTable.empresa], "Vigencia documental", "Contratista", false, "")
                }
                // Eventos propios capturados en el calendario
                EhsCustomEventTable.selectAll().forEach { row ->
                    val detalle = listOf(row[EhsCustomEventTable.responsable], row[EhsCustomEventTable.detalle])
                        .filter { it.isNotBlank() }.joinToString(" · ")
                    add(fechaCal(row[EhsCustomEventTable.fecha]), "evento", row[EhsCustomEventTable.id],
                        row[EhsCustomEventTable.titulo], detalle, "Evento", false, row[EhsCustomEventTable.estado])
                }
                all.sortedWith(compareBy<EhsCalendarEvent> { it.fecha }.thenBy { it.esCritico }.thenBy { it.tipo })
            }
            call.respond(EhsCalendarResponse(mes.toString(), mes.lengthOfMonth(), mes.atDay(1).dayOfWeek.value % 7, hoy.toString(), eventos))
        }

        // EXPORTAR a Outlook/Apple Calendar (.ics) y Google Calendar (estilo EHSoft).
        // Devuelve todos los eventos de los proximos 12 meses con alarma segun el
        // plazo de recordatorio de cada obligacion/documento.
        get("/ics") {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.ALL) ?: return@safeApiCall
                val hoy = LocalDate.now()
                val fin = hoy.plusDays(365)
                val eventos = DatabaseFactory.dbQuery {
                    val all = mutableListOf<EhsCalendarEvent>()
                    fun add(fecha: LocalDate?, tipo: String, id: Int, titulo: String, detalle: String, categoria: String, esCritico: Boolean, estado: String, alertaDias: Int) {
                        if (fecha == null || fecha.isBefore(hoy) || fecha.isAfter(fin)) return
                        // Reutilizamos tareasAbiertas como canal del plazo de alarma del evento.
                        all.add(EhsCalendarEvent(fecha.toString(), tipo, id, titulo, detalle, categoria, esCritico, estado, alertaDias))
                    }
                    LegalMatrixTable.selectAll().forEach { row ->
                        if (row[LegalMatrixTable.aplica] == "Si") {
                            add(fechaCal(row[LegalMatrixTable.fechaVigencia]), "obligacion", row[LegalMatrixTable.id],
                                row[LegalMatrixTable.clave], row[LegalMatrixTable.titulo], row[LegalMatrixTable.categoria],
                                row[LegalMatrixTable.esCritico], "", row[LegalMatrixTable.diasAlertaPrevia])
                        }
                    }
                    LegalMatrixDocTable.selectAll().forEach { row ->
                        add(fechaCal(row[LegalMatrixDocTable.fechaVigencia]), "documento", row[LegalMatrixDocTable.id],
                            row[LegalMatrixDocTable.nombre].ifBlank { "Documento" },
                            row[LegalMatrixDocTable.tipoDocumento], "Cumplimiento", false, "",
                            row[LegalMatrixDocTable.recordatorioDias])
                    }
                    SafetyTrainingTable.selectAll().forEach { row ->
                        add(fechaCal(row[SafetyTrainingTable.proximaFecha]), "capacitacion", row[SafetyTrainingTable.id],
                            row[SafetyTrainingTable.tema], row[SafetyTrainingTable.instructor], "Capacitacion", false, "", 7)
                    }
                    EmergencyDrillTable.selectAll().forEach { row ->
                        add(fechaCal(row[EmergencyDrillTable.fecha]), "simulacro", row[EmergencyDrillTable.id],
                            row[EmergencyDrillTable.tipo], row[EmergencyDrillTable.resultado], "Simulacro", false, "", 7)
                    }
                    EhsActionTable.selectAll().forEach { row ->
                        if (row[EhsActionTable.estado] != "Cerrada")
                            add(fechaCal(row[EhsActionTable.fechaLimite]), "accion", row[EhsActionTable.id],
                                row[EhsActionTable.titulo], row[EhsActionTable.estado], "Accion", false, "", 3)
                    }
                    EhsContractorTable.selectAll().forEach { row ->
                        if (row[EhsContractorTable.estado] != "Suspendido")
                            add(fechaCal(row[EhsContractorTable.vigenciaDocumento]), "contratista", row[EhsContractorTable.id],
                                row[EhsContractorTable.empresa], "Vigencia documental", "Contratista", false, "", 15)
                    }
                    EhsCustomEventTable.selectAll().forEach { row ->
                        add(fechaCal(row[EhsCustomEventTable.fecha]), "evento", row[EhsCustomEventTable.id],
                            row[EhsCustomEventTable.titulo], row[EhsCustomEventTable.detalle], "Evento", false, "", 7)
                    }
                    all.sortedWith(compareBy<EhsCalendarEvent> { it.fecha }.thenBy { it.tipo }.thenBy { it.id })
                }

                fun esc(t: String) = t.replace("\\", "\\\\").replace(",", "\\,").replace(";", "\\;").replace("\n", "\\n").replace("\r", "")
                val stamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"))

                val ics = StringBuilder()
                ics.append("BEGIN:VCALENDAR\r\n")
                ics.append("VERSION:2.0\r\n")
                ics.append("PRODID:-//RHNAF//Matriz Legal EHS//ES\r\n")
                ics.append("CALSCALE:GREGORIAN\r\n")
                ics.append("METHOD:PUBLISH\r\n")
                ics.append("X-WR-CALNAME:RH-NAF - Vencimientos EHS\r\n")
                eventos.forEach { ev ->
                    val dIni = ev.fecha.replace("-", "")
                    val dFin = LocalDate.parse(ev.fecha).plusDays(1).toString().replace("-", "")
                    val titulo = if (ev.esCritico) "⚠ " + ev.titulo else ev.titulo
                    ics.append("BEGIN:VEVENT\r\n")
                    ics.append("UID:rhnaf-${ev.tipo}-${ev.id}-$dIni@rhnaf\r\n")
                    ics.append("DTSTAMP:$stamp\r\n")
                    ics.append("DTSTART;VALUE=DATE:$dIni\r\n")
                    ics.append("DTEND;VALUE=DATE:$dFin\r\n")
                    ics.append("SUMMARY:${esc(titulo)}\r\n")
                    val desc = buildString { append("Tipo: ").append(etiquetaTipo(ev.tipo)); if (ev.detalle.isNotBlank()) append(" | ").append(ev.detalle); append(" | RH-NAF Matriz Legal EHS") }
                    ics.append("DESCRIPTION:${esc(desc)}\r\n")
                    ics.append("BEGIN:VALARM\r\n")
                    ics.append("TRIGGER:-P${ev.tareasAbiertas.coerceAtLeast(1)}D\r\n")
                    ics.append("ACTION:DISPLAY\r\n")
                    ics.append("DESCRIPTION:${esc(titulo)}\r\n")
                    ics.append("END:VALARM\r\n")
                    ics.append("END:VEVENT\r\n")
                }
                ics.append("END:VCALENDAR\r\n")

                call.response.header("Content-Disposition", "attachment; filename=\"rhnaf-ehs-vencimientos.ics\"")
                call.respondText(ics.toString(), contentType = ContentType("text", "calendar"))
            }
        }
    }
}
