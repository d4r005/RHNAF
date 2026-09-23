package com.example.rhnaf.routes

import com.example.rhnaf.auth.Roles
import com.example.rhnaf.auth.requireRoleOr403
import com.example.rhnaf.database.DatabaseFactory
import com.example.rhnaf.database.SafetyIncidentTable
import com.example.rhnaf.database.SafetyInspectionTable
import com.example.rhnaf.database.SafetyTrainingTable
import io.ktor.server.routing.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.selectAll
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Serializable
data class EhsMetrics(
    val incidentes: Int,
    val diasPerdidosRegistrados: Int,
    val incidentesSinDiasValidos: Int,
    val inspeccionesAbiertas: Int,
    val capacitacionesVencidas: Int,
    val capacitacionesPorVencer30Dias: Int,
    val capacitacionesSinFechaValida: Int,
    val horasTrabajadasDisponibles: Boolean = false
)

private fun fechaEhs(value: String): LocalDate? = listOf(
    DateTimeFormatter.ISO_LOCAL_DATE,
    DateTimeFormatter.ofPattern("d/M/uuuu")
).firstNotNullOfOrNull { format ->
    runCatching { LocalDate.parse(value.trim(), format) }.getOrNull()
}

fun Route.ehsMetricsRouting() {
    get("/api/v1/ehs/indicadores") {
        safeApiCall(call) {
            requireRoleOr403(call, Roles.EHS_WRITE) ?: return@safeApiCall
            val today = LocalDate.now()
            val result = DatabaseFactory.dbQuery {
                val lostDays = SafetyIncidentTable.selectAll().map { it[SafetyIncidentTable.diasPerdidos].trim().toIntOrNull()?.takeIf { days -> days >= 0 } }
                val trainings = SafetyTrainingTable.selectAll().map { fechaEhs(it[SafetyTrainingTable.proximaFecha]) }
                val openInspections = SafetyInspectionTable.selectAll().count { row ->
                    val status = row[SafetyInspectionTable.estado].trim().lowercase()
                    status !in setOf("cerrada", "cerrado", "resuelta", "resuelto", "completada", "completado")
                }
                EhsMetrics(
                    incidentes = lostDays.size,
                    diasPerdidosRegistrados = lostDays.filterNotNull().sum(),
                    incidentesSinDiasValidos = lostDays.count { it == null },
                    inspeccionesAbiertas = openInspections,
                    capacitacionesVencidas = trainings.count { it != null && it.isBefore(today) },
                    capacitacionesPorVencer30Dias = trainings.count { it != null && !it.isBefore(today) && !it.isAfter(today.plusDays(30)) },
                    capacitacionesSinFechaValida = trainings.count { it == null }
                )
            }
            call.respond(result)
        }
    }
}
