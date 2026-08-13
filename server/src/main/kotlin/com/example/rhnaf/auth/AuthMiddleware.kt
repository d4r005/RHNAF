package com.example.rhnaf.auth

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.http.*
import com.example.rhnaf.database.DatabaseFactory
import com.example.rhnaf.database.UserTable
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

/**
 * Roles del sistema RHNAF:
 *  - ADMIN: acceso total (crear, editar, eliminar empleados y usuarios)
 *  - RH: puede editar/eliminar empleados y gestionar bajas
 *  - EMPLEADO: solo lectura de su propia info
 */
object Roles {
    const val ADMIN = "ADMIN"
    const val RH = "RH"
    const val EMPLEADO = "EMPLEADO"

    /** Roles que pueden editar/modificar/eliminar empleados. */
    val CAN_MANAGE_EMPLOYEES = setOf(ADMIN, RH)

    /** Roles que pueden gestionar usuarios (alta/baja/edición). */
    val CAN_MANAGE_USERS = setOf(ADMIN)
}

/**
 * Extrae el rol del header Authorization "Bearer <token>".
 * Busca el usuario en la DB por email para obtener su rol real.
 */
suspend fun requireRole(call: ApplicationCall, allowedRoles: Set<String>): String? {
    val authHeader = call.request.header(HttpHeaders.Authorization) ?: return null
    val token = authHeader.removePrefix("Bearer ").trim()
    if (token.isBlank()) return null

    val user = DatabaseFactory.dbQuery {
        UserTable.selectAll().where { UserTable.email eq token }.singleOrNull()
    }

    if (user == null) return null
    val role = user[UserTable.role]
    if (role !in allowedRoles) return null
    return role
}

/**
 * Responde 403 si el usuario no tiene el rol requerido.
 * Si tiene permiso, devuelve el rol.
 */
suspend fun requireRoleOr403(call: ApplicationCall, allowedRoles: Set<String>): String? {
    val role = requireRole(call, allowedRoles)
    if (role == null) {
        call.respond(HttpStatusCode.Forbidden, mapOf(
            "status" to "error",
            "message" to "No tienes permisos para esta acción. Se requiere rol: ${allowedRoles.joinToString(" o ")}"
        ))
    }
    return role
}
