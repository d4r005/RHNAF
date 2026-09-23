package com.example.rhnaf.routes

import com.example.rhnaf.auth.Roles
import com.example.rhnaf.auth.requireRoleOr403
import com.example.rhnaf.database.DatabaseFactory
import com.example.rhnaf.database.EhsRatePeriodTable
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

@Serializable
data class EhsRatePeriod(
    val id: Int = 0,
    val periodo: String,
    val horasTrabajadas: Double,
    val accidentesRegistrables: Int,
    val diasPerdidos: Int,
    val fuenteHoras: String,
    val validadoPor: String,
    val motivoRevision: String = "",
    val version: Int = 0,
    val frecuenciaPorMillon: Double = 0.0,
    val gravedadPorMillon: Double = 0.0
)

private fun rateFrom(row: ResultRow): EhsRatePeriod {
    val hours = row[EhsRatePeriodTable.horasTrabajadas]
    return EhsRatePeriod(
        id = row[EhsRatePeriodTable.id],
        periodo = row[EhsRatePeriodTable.periodo],
        horasTrabajadas = hours,
        accidentesRegistrables = row[EhsRatePeriodTable.accidentesRegistrables],
        diasPerdidos = row[EhsRatePeriodTable.diasPerdidos],
        fuenteHoras = row[EhsRatePeriodTable.fuenteHoras],
        validadoPor = row[EhsRatePeriodTable.validadoPor],
        motivoRevision = row[EhsRatePeriodTable.motivoRevision],
        version = row[EhsRatePeriodTable.version],
        frecuenciaPorMillon = row[EhsRatePeriodTable.accidentesRegistrables] * 1_000_000.0 / hours,
        gravedadPorMillon = row[EhsRatePeriodTable.diasPerdidos] * 1_000_000.0 / hours
    )
}

fun Route.ehsRateRouting() {
    route("/api/v1/ehs/tasas") {
        get {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.EHS_WRITE) ?: return@safeApiCall
                val history = call.request.queryParameters["historial"] == "true"
                val periods = DatabaseFactory.dbQuery {
                    val all = EhsRatePeriodTable.selectAll().map(::rateFrom)
                    val selected = if (history) all else all.groupBy { it.periodo }.values.map { it.maxBy { entry -> entry.version } }
                    selected.sortedWith(compareByDescending<EhsRatePeriod> { it.periodo }.thenByDescending { it.version })
                }
                call.respond(periods)
            }
        }
        post {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.EHS_WRITE) ?: return@safeApiCall
                val item = call.receive<EhsRatePeriod>()
                val error = when {
                    !item.periodo.matches(Regex("\\d{4}-(0[1-9]|1[0-2])")) -> "Periodo inválido (AAAA-MM)"
                    !item.horasTrabajadas.isFinite() || item.horasTrabajadas <= 0 || item.horasTrabajadas > 1_000_000_000 -> "Horas trabajadas deben ser positivas"
                    item.accidentesRegistrables !in 0..100_000 || item.diasPerdidos !in 0..10_000_000 -> "Conteos inválidos"
                    item.fuenteHoras.isBlank() || item.fuenteHoras.length > 300 -> "Fuente de horas obligatoria"
                    item.validadoPor.isBlank() || item.validadoPor.length > 200 -> "Responsable validador obligatorio"
                    item.motivoRevision.length > 500 -> "Motivo de revisión demasiado largo"
                    item.id != 0 || item.version != 0 -> "ID y versión son asignados por el servidor"
                    else -> null
                }
                if (error != null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to error))
                    return@safeApiCall
                }
                val result = DatabaseFactory.dbQuery {
                    val existing = EhsRatePeriodTable.selectAll().where { EhsRatePeriodTable.periodo eq item.periodo }.map(::rateFrom)
                    if (existing.isNotEmpty() && item.motivoRevision.isBlank()) null else {
                        val record = EhsRatePeriodTable.insert {
                            it[periodo] = item.periodo
                            it[version] = (existing.maxOfOrNull { rate -> rate.version } ?: 0) + 1
                            it[horasTrabajadas] = item.horasTrabajadas
                            it[accidentesRegistrables] = item.accidentesRegistrables
                            it[diasPerdidos] = item.diasPerdidos
                            it[fuenteHoras] = item.fuenteHoras.trim()
                            it[validadoPor] = item.validadoPor.trim()
                            it[motivoRevision] = item.motivoRevision.trim()
                        }
                        rateFrom(record.resultedValues!!.first())
                    }
                }
                if (result == null) call.respond(HttpStatusCode.Conflict, mapOf("error" to "Periodo existente: indica motivo de revisión para conservar ambas versiones"))
                else call.respond(HttpStatusCode.Created, result)
            }
        }
    }
}
