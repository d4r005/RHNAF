package com.example.rhnaf.routes

import com.example.rhnaf.auth.Roles
import com.example.rhnaf.auth.requireRoleOr403
import com.example.rhnaf.database.DatabaseFactory
import com.example.rhnaf.database.Dc3ConstanciaTable
import com.example.rhnaf.shared.model.Dc3Constancia
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll

/**
 * EHS - Constancias DC-3 de la capacitación interna.
 * Registro por trabajador con tema, horas, fecha y responsable; la
 * evidencia (constancia firmada) se enlaza por URL HTTPS (Drive).
 * El responsable se registra en cada constancia; no se infiere.
 */
fun Route.dc3Routing() {
    val dc3Write = setOf(Roles.ADMIN, Roles.SEGURIDAD, Roles.RH)
    route("/api/v1/ehs/dc3") {
        get {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.ALL) ?: return@safeApiCall
                val items = DatabaseFactory.dbQuery {
                    Dc3ConstanciaTable.selectAll().map {
                        Dc3Constancia(
                            id = it[Dc3ConstanciaTable.id],
                            trabajador = it[Dc3ConstanciaTable.trabajador],
                            tema = it[Dc3ConstanciaTable.tema],
                            fecha = it[Dc3ConstanciaTable.fecha],
                            horas = it[Dc3ConstanciaTable.horas],
                            responsable = it[Dc3ConstanciaTable.responsable],
                            evidenciaUrl = it[Dc3ConstanciaTable.evidenciaUrl]
                        )
                    }
                }
                call.respond(items)
            }
        }
        post {
            safeApiCall(call) {
                requireRoleOr403(call, dc3Write) ?: return@safeApiCall
                val item = call.receive<Dc3Constancia>()
                if (item.trabajador.isBlank() || item.tema.isBlank() || item.fecha.isBlank()) {
                    return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Trabajador, tema y fecha son obligatorios"))
                }
                DatabaseFactory.dbQuery {
                    Dc3ConstanciaTable.insert {
                        it[trabajador] = item.trabajador
                        it[tema] = item.tema
                        it[fecha] = item.fecha
                        it[horas] = item.horas
                        it[responsable] = item.responsable
                        it[evidenciaUrl] = item.evidenciaUrl
                    }
                }
                call.respond(HttpStatusCode.Created, mapOf("status" to "ok"))
            }
        }
        delete("/{id}") {
            safeApiCall(call) {
                requireRoleOr403(call, dc3Write) ?: return@safeApiCall
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                DatabaseFactory.dbQuery { Dc3ConstanciaTable.deleteWhere { Dc3ConstanciaTable.id eq id } }
                call.respond(mapOf("status" to "ok"))
            }
        }
    }
}
