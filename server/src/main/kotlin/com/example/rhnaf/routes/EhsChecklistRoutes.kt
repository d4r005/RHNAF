package com.example.rhnaf.routes

import com.example.rhnaf.auth.Roles
import com.example.rhnaf.auth.requireRoleOr403
import com.example.rhnaf.database.DatabaseFactory
import com.example.rhnaf.database.EhsActionTable
import com.example.rhnaf.database.EhsChecklistItemTable
import com.example.rhnaf.database.EhsChecklistTable
import com.example.rhnaf.shared.model.EhsChecklist
import com.example.rhnaf.shared.model.EhsChecklistItem
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

/**
 * EHS - Auditorías con checklist (estilo ACM/Prysmex).
 * Cada auditoría tiene puntos de verificación; los hallazgos de no
 * conformidad se convierten en planes de acción trazables (origen
 * "checklist"). No se genera cumplimiento: se registra evidencia y
 * seguimiento humano.
 */

private const val RESULTADOS = "Pendiente,Conforme,NoConforme,NoAplica"

private fun checklistFrom(row: org.jetbrains.exposed.sql.ResultRow) = EhsChecklist(
    id = row[EhsChecklistTable.id],
    titulo = row[EhsChecklistTable.titulo],
    area = row[EhsChecklistTable.area],
    fecha = row[EhsChecklistTable.fecha],
    auditor = row[EhsChecklistTable.auditor],
    estado = row[EhsChecklistTable.estado],
    observaciones = row[EhsChecklistTable.observaciones]
)

private fun itemFrom(row: org.jetbrains.exposed.sql.ResultRow) = EhsChecklistItem(
    id = row[EhsChecklistItemTable.id],
    checklistId = row[EhsChecklistItemTable.checklistId],
    punto = row[EhsChecklistItemTable.punto],
    resultado = row[EhsChecklistItemTable.resultado],
    hallazgo = row[EhsChecklistItemTable.hallazgo],
    responsable = row[EhsChecklistItemTable.responsable],
    fechaCompromiso = row[EhsChecklistItemTable.fechaCompromiso],
    accionId = row[EhsChecklistItemTable.accionId]
)

fun Route.ehsChecklistRouting() {
    route("/api/v1/ehs/checklists") {
        // Listado de auditorías
        get {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.EHS_WRITE) ?: return@safeApiCall
                val items = DatabaseFactory.dbQuery {
                    EhsChecklistTable.selectAll().map(::checklistFrom).sortedWith(
                        compareBy<EhsChecklist> { it.estado == "Cerrada" }.thenByDescending { it.fecha }.thenBy { it.id }
                    )
                }
                call.respond(items)
            }
        }
        post {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.EHS_WRITE) ?: return@safeApiCall
                val item = call.receive<EhsChecklist>()
                if (item.titulo.isBlank() || item.fecha.isBlank()) {
                    return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Título y fecha son obligatorios"))
                }
                if (item.estado != "Abierta" || item.id != 0) {
                    return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("error" to "La auditoría se crea abierta y sin ID"))
                }
                val id = DatabaseFactory.dbQuery {
                    EhsChecklistTable.insert {
                        it[titulo] = item.titulo.trim()
                        it[area] = item.area.trim()
                        it[fecha] = item.fecha.trim()
                        it[auditor] = item.auditor.trim()
                        it[estado] = "Abierta"
                        it[observaciones] = item.observaciones.trim()
                    } get EhsChecklistTable.id
                }
                call.respond(HttpStatusCode.Created, mapOf("id" to id))
            }
        }
        delete("/{id}") {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.EHS_WRITE) ?: return@safeApiCall
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                val blocked = DatabaseFactory.dbQuery {
                    EhsChecklistItemTable.selectAll().where { EhsChecklistItemTable.checklistId eq id }
                        .any { it[EhsChecklistItemTable.accionId] > 0 }
                }
                if (blocked) {
                    return@safeApiCall call.respond(HttpStatusCode.Conflict,
                        mapOf("error" to "La auditoría tiene hallazgos con acciones creadas; no se puede borrar"))
                }
                DatabaseFactory.dbQuery {
                    EhsChecklistItemTable.deleteWhere { EhsChecklistItemTable.checklistId eq id }
                    EhsChecklistTable.deleteWhere { EhsChecklistTable.id eq id }
                }
                call.respond(mapOf("status" to "ok"))
            }
        }
        // Cerrar auditoría: exige que ningún punto quede Pendiente.
        post("/{id}/cerrar") {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.EHS_WRITE) ?: return@safeApiCall
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                val pendientes = DatabaseFactory.dbQuery {
                    EhsChecklistItemTable.selectAll()
                        .where { (EhsChecklistItemTable.checklistId eq id) and (EhsChecklistItemTable.resultado eq "Pendiente") }
                        .count()
                }
                if (pendientes > 0) {
                    return@safeApiCall call.respond(HttpStatusCode.Conflict,
                        mapOf("error" to "No se puede cerrar: $pendientes punto(s) sin evaluar"))
                }
                DatabaseFactory.dbQuery {
                    EhsChecklistTable.update({ EhsChecklistTable.id eq id }) { it[estado] = "Cerrada" }
                }
                call.respond(mapOf("status" to "ok"))
            }
        }
        // Puntos del checklist
        get("/{id}/items") {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.EHS_WRITE) ?: return@safeApiCall
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                val items = DatabaseFactory.dbQuery {
                    EhsChecklistItemTable.selectAll().where { EhsChecklistItemTable.checklistId eq id }.map(::itemFrom)
                }
                call.respond(items)
            }
        }
        post("/{id}/items") {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.EHS_WRITE) ?: return@safeApiCall
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                val item = call.receive<EhsChecklistItem>()
                if (item.punto.isBlank()) {
                    return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("error" to "El punto de verificación es obligatorio"))
                }
                if (item.resultado !in RESULTADOS.split(",")) {
                    return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Resultado inválido"))
                }
                if (item.resultado == "NoConforme" && item.hallazgo.isBlank()) {
                    return@safeApiCall call.respond(HttpStatusCode.BadRequest,
                        mapOf("error" to "Un punto No conforme requiere descripción del hallazgo"))
                }
                val exists = DatabaseFactory.dbQuery {
                    EhsChecklistTable.selectAll().where { EhsChecklistTable.id eq id }.limit(1).any()
                }
                if (!exists) return@safeApiCall call.respond(HttpStatusCode.NotFound, mapOf("error" to "Auditoría inexistente"))
                val itemId = DatabaseFactory.dbQuery {
                    EhsChecklistItemTable.insert {
                        it[checklistId] = id
                        it[punto] = item.punto.trim()
                        it[resultado] = item.resultado
                        it[hallazgo] = item.hallazgo.trim()
                        it[responsable] = item.responsable.trim()
                        it[fechaCompromiso] = item.fechaCompromiso.trim()
                    } get EhsChecklistItemTable.id
                }
                call.respond(HttpStatusCode.Created, mapOf("id" to itemId))
            }
        }
        // Actualizar resultado/hallazgo de un punto
        put("/items/{itemId}") {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.EHS_WRITE) ?: return@safeApiCall
                val itemId = call.parameters["itemId"]?.toIntOrNull()
                    ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                val item = call.receive<EhsChecklistItem>()
                if (item.resultado !in RESULTADOS.split(",")) {
                    return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Resultado inválido"))
                }
                if (item.resultado == "NoConforme" && item.hallazgo.isBlank()) {
                    return@safeApiCall call.respond(HttpStatusCode.BadRequest,
                        mapOf("error" to "Un punto No conforme requiere descripción del hallazgo"))
                }
                val updated = DatabaseFactory.dbQuery {
                    EhsChecklistItemTable.update({ EhsChecklistItemTable.id eq itemId }) {
                        it[resultado] = item.resultado
                        it[hallazgo] = item.hallazgo.trim()
                        it[responsable] = item.responsable.trim()
                        it[fechaCompromiso] = item.fechaCompromiso.trim()
                    }
                }
                if (updated == 0) call.respond(HttpStatusCode.NotFound, mapOf("error" to "Punto inexistente"))
                else call.respond(mapOf("status" to "ok"))
            }
        }
        // Borrar un punto (solo si aún no generó acción)
        delete("/items/{itemId}") {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.EHS_WRITE) ?: return@safeApiCall
                val itemId = call.parameters["itemId"]?.toIntOrNull()
                    ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                val linked = DatabaseFactory.dbQuery {
                    EhsChecklistItemTable.selectAll().where { EhsChecklistItemTable.id eq itemId }
                        .limit(1).any { it[EhsChecklistItemTable.accionId] > 0 }
                }
                if (linked) {
                    return@safeApiCall call.respond(HttpStatusCode.Conflict,
                        mapOf("error" to "El punto ya tiene un plan de acción; no se puede borrar"))
                }
                DatabaseFactory.dbQuery { EhsChecklistItemTable.deleteWhere { EhsChecklistItemTable.id eq itemId } }
                call.respond(mapOf("status" to "ok"))
            }
        }
        // Convertir un hallazgo en plan de acción trazable.
        post("/items/{itemId}/accion") {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.EHS_WRITE) ?: return@safeApiCall
                val itemId = call.parameters["itemId"]?.toIntOrNull()
                    ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                val body = call.receive<EhsChecklistItem>()
                val existing = DatabaseFactory.dbQuery {
                    EhsChecklistItemTable.selectAll().where { EhsChecklistItemTable.id eq itemId }.limit(1).firstOrNull()
                }
                if (existing == null) {
                    return@safeApiCall call.respond(HttpStatusCode.NotFound, mapOf("error" to "Punto inexistente"))
                }
                if (existing[EhsChecklistItemTable.accionId] > 0) {
                    return@safeApiCall call.respond(HttpStatusCode.Conflict, mapOf("error" to "El hallazgo ya tiene una acción"))
                }
                if (existing[EhsChecklistItemTable.resultado] != "NoConforme") {
                    return@safeApiCall call.respond(HttpStatusCode.BadRequest,
                        mapOf("error" to "Solo los puntos No conforme generan acciones"))
                }
                if (body.responsable.isBlank() || body.fechaCompromiso.isBlank()) {
                    return@safeApiCall call.respond(HttpStatusCode.BadRequest,
                        mapOf("error" to "Responsable y fecha límite son obligatorios"))
                }
                val accionId = DatabaseFactory.dbQuery {
                    val stmt = EhsActionTable.insert {
                        it[titulo] = "Hallazgo de auditoría: " + existing[EhsChecklistItemTable.punto].take(200)
                        it[descripcion] = existing[EhsChecklistItemTable.hallazgo]
                        it[origenTipo] = "checklist"
                        it[origenId] = itemId
                        it[responsable] = body.responsable.trim()
                        it[fechaLimite] = body.fechaCompromiso.trim()
                        it[prioridad] = "Alta"
                        it[estado] = "Abierta"
                    }
                    stmt get EhsActionTable.id
                }
                DatabaseFactory.dbQuery {
                    EhsChecklistItemTable.update({ EhsChecklistItemTable.id eq itemId }) {
                        it[EhsChecklistItemTable.accionId] = accionId
                    }
                }
                call.respond(HttpStatusCode.Created, mapOf("accionId" to accionId))
            }
        }
    }
}
