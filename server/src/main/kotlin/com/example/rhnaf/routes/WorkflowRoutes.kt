package com.example.rhnaf.routes

import com.example.rhnaf.database.DatabaseFactory
import com.example.rhnaf.database.ApprovalWorkflowTable
import com.example.rhnaf.database.DocumentLogTable
import com.example.rhnaf.shared.model.ApprovalRequest
import com.example.rhnaf.shared.model.DocumentLog
import com.example.rhnaf.auth.Roles
import com.example.rhnaf.auth.requireRoleOr403
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

fun Route.workflowRouting() {

    // ============ WORKFLOW DE APROBACIONES ============
    route("/api/v1/wf/aprobaciones") {
        // Listar todas las aprobaciones (con filtros opcionales)
        get {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.ALL) ?: return@safeApiCall
                val estado = call.request.queryParameters["estado"]
                val entityType = call.request.queryParameters["entityType"]
                val baseQuery = if (estado != null)
                    ApprovalWorkflowTable.selectAll().where { ApprovalWorkflowTable.estado eq estado }
                else if (entityType != null)
                    ApprovalWorkflowTable.selectAll().where { ApprovalWorkflowTable.entityType eq entityType }
                else
                    ApprovalWorkflowTable.selectAll()
                val result = pagedQuery(call, baseQuery) {
                    ApprovalRequest(
                        id = it[ApprovalWorkflowTable.id],
                        entityType = it[ApprovalWorkflowTable.entityType],
                        entityId = it[ApprovalWorkflowTable.entityId],
                        entityTable = it[ApprovalWorkflowTable.entityTable],
                        estado = it[ApprovalWorkflowTable.estado],
                        solicitadoPor = it[ApprovalWorkflowTable.solicitadoPor],
                        fechaSolicitud = it[ApprovalWorkflowTable.fechaSolicitud],
                        aprobadoPor = it[ApprovalWorkflowTable.aprobadoPor],
                        fechaAprobacion = it[ApprovalWorkflowTable.fechaAprobacion],
                        comentarios = it[ApprovalWorkflowTable.comentarios],
                        prioridad = it[ApprovalWorkflowTable.prioridad]
                    )
                }
                call.respond(result)
            }
        }

        // Crear solicitud de aprobación
        post {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.ALL) ?: return@safeApiCall
                val item = call.receive<ApprovalRequest>()
                DatabaseFactory.dbQuery {
                    ApprovalWorkflowTable.insert {
                        it[entityType] = item.entityType
                        it[entityId] = item.entityId
                        it[entityTable] = item.entityTable
                        it[estado] = "PENDIENTE"
                        it[solicitadoPor] = item.solicitadoPor
                        it[comentarios] = item.comentarios
                        it[prioridad] = item.prioridad
                    }
                }
                call.respond(mapOf("status" to "ok", "message" to "Solicitud de aprobación creada"))
            }
        }

        // Aprobar una solicitud
        put("/{id}/aprobar") {
            safeApiCall(call) {
                requireRoleOr403(call, setOf(Roles.ADMIN, Roles.RH, Roles.COMPRAS, Roles.SEGURIDAD, Roles.FINANZAS, Roles.IMPORT_EXPORT)) ?: return@safeApiCall
                val id = call.parameters["id"]?.toIntOrNull() ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                val body = call.receive<Map<String, String>>()
                val aprobadoPor = body["aprobadoPor"] ?: ""
                val comentarios = body["comentarios"] ?: ""
                DatabaseFactory.dbQuery {
                    ApprovalWorkflowTable.update({ ApprovalWorkflowTable.id eq id }) {
                        it[estado] = "APROBADO"
                        it[ApprovalWorkflowTable.aprobadoPor] = aprobadoPor
                        it[fechaAprobacion] = java.time.LocalDateTime.now().toString()
                        it[ApprovalWorkflowTable.comentarios] = comentarios
                    }
                }
                call.respond(mapOf("status" to "ok", "message" to "Solicitud aprobada"))
            }
        }

        // Rechazar una solicitud
        put("/{id}/rechazar") {
            safeApiCall(call) {
                requireRoleOr403(call, setOf(Roles.ADMIN, Roles.RH, Roles.COMPRAS, Roles.SEGURIDAD, Roles.FINANZAS, Roles.IMPORT_EXPORT)) ?: return@safeApiCall
                val id = call.parameters["id"]?.toIntOrNull() ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                val body = call.receive<Map<String, String>>()
                val aprobadoPor = body["aprobadoPor"] ?: ""
                val comentarios = body["comentarios"] ?: ""
                DatabaseFactory.dbQuery {
                    ApprovalWorkflowTable.update({ ApprovalWorkflowTable.id eq id }) {
                        it[estado] = "RECHAZADO"
                        it[ApprovalWorkflowTable.aprobadoPor] = aprobadoPor
                        it[fechaAprobacion] = java.time.LocalDateTime.now().toString()
                        it[ApprovalWorkflowTable.comentarios] = comentarios
                    }
                }
                call.respond(mapOf("status" to "ok", "message" to "Solicitud rechazada"))
            }
        }

        // Eliminar solicitud
        delete("/{id}") {
            safeApiCall(call) {
                requireRoleOr403(call, setOf(Roles.ADMIN)) ?: return@safeApiCall
                val id = call.parameters["id"]?.toIntOrNull() ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                DatabaseFactory.dbQuery { ApprovalWorkflowTable.deleteWhere { ApprovalWorkflowTable.id eq id } }
                call.respond(mapOf("status" to "ok"))
            }
        }
    }

    // ============ LOG DE DOCUMENTOS (Trazabilidad) ============
    route("/api/v1/wf/documentos") {
        get {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.ALL) ?: return@safeApiCall
                val tipo = call.request.queryParameters["tipo"]
                val baseQuery = if (tipo != null)
                    DocumentLogTable.selectAll().where { DocumentLogTable.tipoDocumento eq tipo }
                else
                    DocumentLogTable.selectAll()
                val result = pagedQuery(call, baseQuery) {
                    DocumentLog(
                        id = it[DocumentLogTable.id],
                        tipoDocumento = it[DocumentLogTable.tipoDocumento],
                        numeroDocumento = it[DocumentLogTable.numeroDocumento],
                        tablaOrigen = it[DocumentLogTable.tablaOrigen],
                        registroId = it[DocumentLogTable.registroId],
                        usuario = it[DocumentLogTable.usuario],
                        fecha = it[DocumentLogTable.fecha],
                        descripcion = it[DocumentLogTable.descripcion]
                    )
                }
                call.respond(result)
            }
        }

        post {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.ALL) ?: return@safeApiCall
                val item = call.receive<DocumentLog>()
                DatabaseFactory.dbQuery {
                    DocumentLogTable.insert {
                        it[tipoDocumento] = item.tipoDocumento
                        it[numeroDocumento] = item.numeroDocumento
                        it[tablaOrigen] = item.tablaOrigen
                        it[registroId] = item.registroId
                        it[usuario] = item.usuario
                        it[descripcion] = item.descripcion
                    }
                }
                call.respond(mapOf("status" to "ok"))
            }
        }
    }
}
