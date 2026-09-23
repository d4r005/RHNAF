package com.example.rhnaf.routes

import com.example.rhnaf.auth.Roles
import com.example.rhnaf.auth.requireRoleOr403
import com.example.rhnaf.database.DatabaseFactory
import com.example.rhnaf.database.EhsRatePeriodTable
import com.example.rhnaf.database.LegalMatrixTable
import com.example.rhnaf.database.SafetyIncidentTable
import com.example.rhnaf.database.SafetyInspectionTable
import com.example.rhnaf.database.SafetyTrainingTable
import io.ktor.server.routing.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.selectAll
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Serializable
data class IncidenteMes(
    val mes: String,              // yyyy-MM
    val incidentes: Int,
    val diasPerdidos: Int
)

@Serializable
data class EhsMetrics(
    val incidentes: Int,
    val diasPerdidosRegistrados: Int,
    val incidentesSinDiasValidos: Int,
    val inspeccionesAbiertas: Int,
    val capacitacionesVencidas: Int,
    val capacitacionesPorVencer30Dias: Int,
    val capacitacionesSinFechaValida: Int,
    val horasTrabajadasDisponibles: Boolean = false,
    // KPIs de accidentabilidad (estilo EHSoft): serie mensual de incidentes
    // de los ultimos 12 meses y tasas del periodo validado mas reciente.
    val incidentesPorMes: List<IncidenteMes> = emptyList(),
    val periodoTasas: String = "",           // yyyy-MM del ultimo periodo validado
    val tasaFrecuencia: Double? = null,      // IF = accidentes*200,000/horas trabajadas
    val tasaGravedad: Double? = null,        // IG = dias perdidos*200,000/horas trabajadas
    // Permisos criticos de la matriz legal
    val criticasTotal: Int = 0,
    val criticasVencidas: Int = 0,
    val criticasPorVencer: Int = 0,
    // Cumplimiento legal global (aplicables vigentes o por vencer)
    val aplicables: Int = 0,
    val porcentajeCumplimientoLegal: Double = 0.0
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

                // Serie mensual de incidentes (ultimos 12 meses), un solo escaneo
                val incidentesMes = SafetyIncidentTable.selectAll().map { row ->
                    val fecha = fechaEhs(row[SafetyIncidentTable.fecha])
                    val dias = row[SafetyIncidentTable.diasPerdidos].trim().toIntOrNull() ?: 0
                    Triple(fecha, dias, fecha?.let { YearMonth.from(it) })
                }
                val inicioSerie = YearMonth.from(today).minusMonths(11)
                val serie = (0..11).map { i ->
                    val mes = inicioSerie.plusMonths(i.toLong())
                    IncidenteMes(
                        mes = mes.toString(),
                        incidentes = incidentesMes.count { it.third == mes },
                        diasPerdidos = incidentesMes.filter { it.third == mes }.sumOf { it.second }
                    )
                }

                // Tasas del periodo validado mas reciente (version mayor del mes mas reciente)
                val ultimo = EhsRatePeriodTable.selectAll()
                    .sortedWith(compareByDescending<org.jetbrains.exposed.sql.ResultRow> { it[EhsRatePeriodTable.periodo] }
                        .thenByDescending { it[EhsRatePeriodTable.version] })
                    .firstOrNull()
                val horas = ultimo?.get(EhsRatePeriodTable.horasTrabajadas) ?: 0.0
                val tasas = if (horas > 0.0) {
                    ultimo?.let { u ->
                        Triple(
                            u[EhsRatePeriodTable.periodo],
                            u[EhsRatePeriodTable.accidentesRegistrables] * 200000.0 / horas,
                            u[EhsRatePeriodTable.diasPerdidos] * 200000.0 / horas
                        )
                    }
                } else null

                // Permisos criticos y cumplimiento legal de la matriz
                val hoyEstado = { vigencia: LocalDate?, alerta: Int ->
                    when {
                        vigencia == null -> "Pendiente"
                        vigencia.isBefore(today) -> "Vencido"
                        !vigencia.isAfter(today.plusDays(alerta.toLong())) -> "PorVencer"
                        else -> "Vigente"
                    }
                }
                val obligaciones = LegalMatrixTable.selectAll().map { row ->
                    Triple(row[LegalMatrixTable.aplica], row[LegalMatrixTable.esCritico],
                        hoyEstado(fechaEhs(row[LegalMatrixTable.fechaVigencia]), row[LegalMatrixTable.diasAlertaPrevia]))
                }
                val aplicables = obligaciones.filter { it.first == "Si" }
                val cumpliendo = aplicables.count { it.third == "Vigente" || it.third == "PorVencer" }
                val criticas = aplicables.filter { it.second }

                EhsMetrics(
                    incidentes = lostDays.size,
                    diasPerdidosRegistrados = lostDays.filterNotNull().sum(),
                    incidentesSinDiasValidos = lostDays.count { it == null },
                    inspeccionesAbiertas = openInspections,
                    capacitacionesVencidas = trainings.count { it != null && it.isBefore(today) },
                    capacitacionesPorVencer30Dias = trainings.count { it != null && !it.isBefore(today) && !it.isAfter(today.plusDays(30)) },
                    capacitacionesSinFechaValida = trainings.count { it == null },
                    horasTrabajadasDisponibles = horas > 0.0,
                    incidentesPorMes = serie,
                    periodoTasas = tasas?.first ?: "",
                    tasaFrecuencia = tasas?.second,
                    tasaGravedad = tasas?.third,
                    criticasTotal = criticas.size,
                    criticasVencidas = criticas.count { it.third == "Vencido" },
                    criticasPorVencer = criticas.count { it.third == "PorVencer" },
                    aplicables = aplicables.size,
                    porcentajeCumplimientoLegal = if (aplicables.isNotEmpty()) cumpliendo.toDouble() / aplicables.size * 100.0 else 0.0
                )
            }
            call.respond(result)
        }
    }
}
