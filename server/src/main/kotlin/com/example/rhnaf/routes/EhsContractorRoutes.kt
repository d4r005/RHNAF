package com.example.rhnaf.routes

import com.example.rhnaf.auth.Roles
import com.example.rhnaf.auth.requireRoleOr403
import com.example.rhnaf.database.DatabaseFactory
import com.example.rhnaf.database.EhsContractorTable
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDate

@Serializable
data class EhsContractor(
    val id: Int = 0,
    val empresa: String,
    val actividad: String,
    val centroTrabajo: String,
    val responsableInterno: String,
    val documentoUrl: String = "",
    val vigenciaDocumento: String = "",
    val estado: String = "Pendiente",
    val notas: String = "",
    val estadoDocumental: String = "SinDocumento"
)

private fun contractorRow(row: ResultRow): EhsContractor {
    val expiry = runCatching { LocalDate.parse(row[EhsContractorTable.vigenciaDocumento]) }.getOrNull()
    val documentStatus = when {
        row[EhsContractorTable.documentoUrl].isBlank() || expiry == null -> "SinDocumento"
        expiry.isBefore(LocalDate.now()) -> "Vencido"
        !expiry.isAfter(LocalDate.now().plusDays(30)) -> "PorVencer"
        else -> "Vigente"
    }
    return EhsContractor(
        id = row[EhsContractorTable.id],
        empresa = row[EhsContractorTable.empresa],
        actividad = row[EhsContractorTable.actividad],
        centroTrabajo = row[EhsContractorTable.centroTrabajo],
        responsableInterno = row[EhsContractorTable.responsableInterno],
        documentoUrl = row[EhsContractorTable.documentoUrl],
        vigenciaDocumento = row[EhsContractorTable.vigenciaDocumento],
        estado = row[EhsContractorTable.estado],
        notas = row[EhsContractorTable.notas],
        estadoDocumental = documentStatus
    )
}

private fun validateContractor(item: EhsContractor): String? = when {
    item.empresa.isBlank() || item.empresa.length > 250 -> "Empresa requerida (máximo 250 caracteres)"
    item.actividad.isBlank() || item.actividad.length > 250 -> "Actividad requerida (máximo 250 caracteres)"
    item.centroTrabajo.isBlank() || item.centroTrabajo.length > 200 -> "Centro de trabajo requerido"
    item.responsableInterno.isBlank() || item.responsableInterno.length > 200 -> "Responsable interno requerido"
    item.documentoUrl.length > 500 || (item.documentoUrl.isNotBlank() && !item.documentoUrl.startsWith("https://")) -> "Documento debe ser una URL HTTPS de máximo 500 caracteres"
    item.vigenciaDocumento.isNotBlank() && runCatching { LocalDate.parse(item.vigenciaDocumento) }.isFailure -> "Fecha inválida (AAAA-MM-DD)"
    item.estado !in setOf("Pendiente", "Aprobado", "Suspendido") -> "Estado inválido"
    item.notas.length > 500 -> "Notas demasiado largas"
    item.estado == "Aprobado" && (item.documentoUrl.isBlank() || runCatching { LocalDate.parse(item.vigenciaDocumento) }.getOrNull()?.isBefore(LocalDate.now()) != false) -> "Aprobar requiere documento y vigencia actual"
    else -> null
}

fun Route.ehsContractorRouting() {
    route("/api/v1/ehs/contratistas") {
        get {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.EHS_WRITE) ?: return@safeApiCall
                call.respond(DatabaseFactory.dbQuery { EhsContractorTable.selectAll().map(::contractorRow).sortedBy { it.empresa } })
            }
        }
        post {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.EHS_WRITE) ?: return@safeApiCall
                val item = call.receive<EhsContractor>()
                val error = validateContractor(item)
                if (error != null || item.id != 0 || item.estado != "Pendiente") {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to (error ?: "Crear en estado Pendiente, sin ID")))
                    return@safeApiCall
                }
                val result = DatabaseFactory.dbQuery {
                    val insert = EhsContractorTable.insert {
                        it[empresa] = item.empresa.trim()
                        it[actividad] = item.actividad.trim()
                        it[centroTrabajo] = item.centroTrabajo.trim()
                        it[responsableInterno] = item.responsableInterno.trim()
                        it[documentoUrl] = item.documentoUrl.trim()
                        it[vigenciaDocumento] = item.vigenciaDocumento
                        it[estado] = "Pendiente"
                        it[notas] = item.notas.trim()
                    }
                    contractorRow(insert.resultedValues!!.first())
                }
                call.respond(HttpStatusCode.Created, result)
            }
        }
        put("/{id}") {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.EHS_WRITE) ?: return@safeApiCall
                val id = call.parameters["id"]?.toIntOrNull()
                val item = call.receive<EhsContractor>()
                val error = validateContractor(item)
                if (id == null || id <= 0 || item.id != id || error != null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to (error ?: "ID inválido")))
                    return@safeApiCall
                }
                val changed = DatabaseFactory.dbQuery {
                    val count = EhsContractorTable.update({ EhsContractorTable.id eq id }) {
                        it[empresa] = item.empresa.trim()
                        it[actividad] = item.actividad.trim()
                        it[centroTrabajo] = item.centroTrabajo.trim()
                        it[responsableInterno] = item.responsableInterno.trim()
                        it[documentoUrl] = item.documentoUrl.trim()
                        it[vigenciaDocumento] = item.vigenciaDocumento
                        it[estado] = item.estado
                        it[notas] = item.notas.trim()
                    }
                    if (count == 0) null else contractorRow(EhsContractorTable.selectAll().where { EhsContractorTable.id eq id }.single())
                }
                if (changed == null) call.respond(HttpStatusCode.NotFound, mapOf("error" to "Contratista no encontrado")) else call.respond(changed)
            }
        }
    }
}
