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
    val estado: String = ""     // Vigente/PorVencer/Vencido para obligaciones
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

                fun add(fecha: LocalDate?, tipo: String, id: Int, titulo: String, detalle: String, categoria: String, esCritico: Boolean, estado: String) {
                    if (fecha == null || fecha.isBefore(ini) || fecha.isAfter(fin)) return
                    all.add(EhsCalendarEvent(fecha.toString(), tipo, id, titulo, detalle, categoria, esCritico, estado))
                }

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
                            row[LegalMatrixTable.esCritico], estado)
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
    }
}
