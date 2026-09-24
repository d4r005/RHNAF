package com.example.rhnaf.routes

import com.example.rhnaf.auth.Roles
import com.example.rhnaf.auth.requireRoleOr403
import com.example.rhnaf.database.*
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
data class EhsAction(
    val id: Int = 0,
    val titulo: String,
    val descripcion: String = "",
    val origenTipo: String = "manual",
    val origenId: Int = 0,
    val responsable: String,
    val fechaLimite: String,
    val prioridad: String = "Media",
    val estado: String = "Abierta",
    val evidenciaUrl: String = "",
    val fechaCierre: String = ""
)

private fun actionFrom(row: ResultRow) = EhsAction(
    id = row[EhsActionTable.id],
    titulo = row[EhsActionTable.titulo],
    descripcion = row[EhsActionTable.descripcion],
    origenTipo = row[EhsActionTable.origenTipo],
    origenId = row[EhsActionTable.origenId],
    responsable = row[EhsActionTable.responsable],
    fechaLimite = row[EhsActionTable.fechaLimite],
    prioridad = row[EhsActionTable.prioridad],
    estado = row[EhsActionTable.estado],
    evidenciaUrl = row[EhsActionTable.evidenciaUrl],
    fechaCierre = row[EhsActionTable.fechaCierre]
)

private fun validateAction(action: EhsAction): String? = when {
    action.titulo.isBlank() || action.titulo.length > 300 -> "Título requerido (máximo 300 caracteres)"
    action.descripcion.length > 1000 -> "Descripción demasiado larga"
    action.responsable.isBlank() || action.responsable.length > 200 -> "Responsable requerido (máximo 200 caracteres)"
    runCatching { LocalDate.parse(action.fechaLimite) }.isFailure -> "Fecha límite inválida (AAAA-MM-DD)"
    action.prioridad !in setOf("Alta", "Media", "Baja") -> "Prioridad inválida"
    action.estado !in setOf("Abierta", "EnProgreso", "Cerrada") -> "Estado inválido"
    action.origenTipo !in setOf("manual", "matriz_legal", "inspeccion", "incidente", "checklist") -> "Origen inválido"
    action.origenTipo == "manual" && action.origenId != 0 -> "Origen manual no admite identificador"
    action.origenTipo != "manual" && action.origenId <= 0 -> "Se requiere ID del registro de origen"
    action.evidenciaUrl.length > 500 -> "URL de evidencia demasiado larga"
    action.evidenciaUrl.isNotBlank() && !action.evidenciaUrl.startsWith("https://") -> "La evidencia debe ser una URL HTTPS"
    action.estado == "Cerrada" && action.evidenciaUrl.isBlank() -> "El cierre requiere URL de evidencia"
    else -> null
}

private fun sourceExists(action: EhsAction): Boolean = when (action.origenTipo) {
    "manual" -> true
    "matriz_legal" -> LegalMatrixTable.selectAll().where { LegalMatrixTable.id eq action.origenId }.limit(1).any()
    "inspeccion" -> SafetyInspectionTable.selectAll().where { SafetyInspectionTable.id eq action.origenId }.limit(1).any()
    "incidente" -> SafetyIncidentTable.selectAll().where { SafetyIncidentTable.id eq action.origenId }.limit(1).any()
    "checklist" -> EhsChecklistItemTable.selectAll().where { EhsChecklistItemTable.id eq action.origenId }.limit(1).any()
    else -> false
}

fun Route.ehsActionRouting() {
    route("/api/v1/ehs/acciones") {
        get {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.EHS_WRITE) ?: return@safeApiCall
                val status = call.request.queryParameters["estado"]
                if (status != null && status !in setOf("Abierta", "EnProgreso", "Cerrada")) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Estado inválido"))
                    return@safeApiCall
                }
                val actions = DatabaseFactory.dbQuery {
                    EhsActionTable.selectAll().map(::actionFrom).filter { status == null || it.estado == status }.sortedWith(
                        compareBy<EhsAction> { it.estado == "Cerrada" }.thenBy { it.fechaLimite }.thenBy { it.id }
                    )
                }
                call.respond(actions)
            }
        }
        post {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.EHS_WRITE) ?: return@safeApiCall
                val item = call.receive<EhsAction>()
                val error = validateAction(item)
                if (error != null || item.id != 0 || item.estado != "Abierta" || item.fechaCierre.isNotBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to (error ?: "La acción debe crearse abierta y sin ID ni fecha de cierre")))
                    return@safeApiCall
                }
                val created = DatabaseFactory.dbQuery {
                    if (!sourceExists(item)) null else {
                        val row = EhsActionTable.insert {
                            it[titulo] = item.titulo.trim()
                            it[descripcion] = item.descripcion.trim()
                            it[origenTipo] = item.origenTipo
                            it[origenId] = item.origenId
                            it[responsable] = item.responsable.trim()
                            it[fechaLimite] = item.fechaLimite
                            it[prioridad] = item.prioridad
                            it[estado] = "Abierta"
                        }
                        actionFrom(row.resultedValues!!.first())
                    }
                }
                if (created == null) call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Registro de origen inexistente"))
                else call.respond(HttpStatusCode.Created, created)
            }
        }
        put("/{id}") {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.EHS_WRITE) ?: return@safeApiCall
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null || id <= 0) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                    return@safeApiCall
                }
                val item = call.receive<EhsAction>()
                val error = validateAction(item)
                if (error != null || item.id != id) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to (error ?: "ID no coincide")))
                    return@safeApiCall
                }
                val updated = DatabaseFactory.dbQuery {
                    val previous = EhsActionTable.selectAll().where { EhsActionTable.id eq id }.singleOrNull()?.let(::actionFrom)
                    if (previous == null || !sourceExists(item) || previous.estado == "Cerrada" || previous.origenTipo != item.origenTipo || previous.origenId != item.origenId) null else {
                        EhsActionTable.update({ EhsActionTable.id eq id }) {
                            it[titulo] = item.titulo.trim()
                            it[descripcion] = item.descripcion.trim()
                            it[responsable] = item.responsable.trim()
                            it[fechaLimite] = item.fechaLimite
                            it[prioridad] = item.prioridad
                            it[estado] = item.estado
                            it[evidenciaUrl] = item.evidenciaUrl.trim()
                            it[fechaCierre] = if (item.estado == "Cerrada") LocalDate.now().toString() else ""
                        }
                        EhsActionTable.selectAll().where { EhsActionTable.id eq id }.single().let(::actionFrom)
                    }
                }
                if (updated == null) call.respond(HttpStatusCode.Conflict, mapOf("error" to "Acción no encontrada, origen cambiado o ya cerrada"))
                else call.respond(updated)
            }
        }
    }
}
