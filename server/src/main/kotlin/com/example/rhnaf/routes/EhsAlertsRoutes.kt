package com.example.rhnaf.routes

import com.example.rhnaf.auth.Roles
import com.example.rhnaf.auth.requireRoleOr403
import com.example.rhnaf.database.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.selectAll
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Serializable
data class EhsAlert(
    val tipo: String,
    val origenId: Int,
    val titulo: String,
    val fechaLimite: String,
    val diasRestantes: Long,
    val estado: String,
    val esCritico: Boolean = false
)

private fun dateForAlert(value: String): LocalDate? = listOf(
    DateTimeFormatter.ISO_LOCAL_DATE,
    DateTimeFormatter.ofPattern("d/M/uuuu")
).firstNotNullOfOrNull { format -> runCatching { LocalDate.parse(value.trim(), format) }.getOrNull() }

fun Route.ehsAlertsRouting() {
    get("/api/v1/ehs/avisos") {
        safeApiCall(call) {
            requireRoleOr403(call, Roles.EHS_WRITE) ?: return@safeApiCall
            val today = LocalDate.now(ZoneId.of("America/Mexico_City"))
            val alerts = DatabaseFactory.dbQuery {
                val all = mutableListOf<EhsAlert>()
                fun add(type: String, id: Int, title: String, rawDate: String, critico: Boolean = false) {
                    val date = dateForAlert(rawDate) ?: return
                    val days = ChronoUnit.DAYS.between(today, date)
                    // Los permisos criticos alertan hasta con 90 dias de
                    // anticipacion: gestionarlos lleva semanas (tramites ante
                    // autoridades), 30 dias ya es tarde para reaccionar.
                    val ventana = if (critico) 90L else 30L
                    if (days <= ventana) all.add(EhsAlert(
                        tipo = type,
                        origenId = id,
                        titulo = title,
                        fechaLimite = date.toString(),
                        diasRestantes = days,
                        estado = if (days < 0) "Vencido" else "PorVencer",
                        esCritico = critico
                    ))
                }
                EhsActionTable.selectAll().forEach { row ->
                    if (row[EhsActionTable.estado] != "Cerrada") add("accion", row[EhsActionTable.id], row[EhsActionTable.titulo], row[EhsActionTable.fechaLimite])
                }
                EhsContractorTable.selectAll().forEach { row ->
                    if (row[EhsContractorTable.estado] != "Suspendido" && row[EhsContractorTable.documentoUrl].isNotBlank())
                        add("contratista", row[EhsContractorTable.id], row[EhsContractorTable.empresa], row[EhsContractorTable.vigenciaDocumento])
                }
                SafetyTrainingTable.selectAll().forEach { row ->
                    add("capacitacion", row[SafetyTrainingTable.id], row[SafetyTrainingTable.tema], row[SafetyTrainingTable.proximaFecha])
                }
                LegalMatrixTable.selectAll().forEach { row ->
                    if (row[LegalMatrixTable.aplica] == "Si")
                        add("obligacion", row[LegalMatrixTable.id], row[LegalMatrixTable.clave],
                            row[LegalMatrixTable.fechaVigencia], row[LegalMatrixTable.esCritico])
                }
                all.sortedWith(compareBy<EhsAlert> { it.esCritico.not() }.thenBy { it.diasRestantes }.thenBy { it.tipo }.thenBy { it.origenId }).take(200)
            }
            call.respond(alerts)
        }
    }
}
