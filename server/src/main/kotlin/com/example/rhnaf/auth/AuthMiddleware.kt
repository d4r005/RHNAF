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
 * Roles del sistema RHNAF — alineados con módulos estilo SAP:
 *  - ADMIN: acceso total
 *  - RH: gestión de empleados y asistencia
 *  - COMPRAS: órdenes de compra, recepción de MP
 *  - MANTENIMIENTO: órdenes de mantenimiento
 *  - SEGURIDAD: EHS, inspecciones, incidentes
 *  - ALMACEN: inventario, tarimas, sellos, ferretería
 *  - IMPORT_EXPORT: embarques, contenedores, pedidos
 *  - FINANZAS: contabilidad, costos, nómina
 *  - EMPLEADO: solo lectura de su propia info
 */
object Roles {
    const val ADMIN = "ADMIN"
    const val RH = "RH"
    const val COMPRAS = "COMPRAS"
    const val MANTENIMIENTO = "MANTENIMIENTO"
    const val SEGURIDAD = "SEGURIDAD"
    const val ALMACEN = "ALMACEN"
    const val IMPORT_EXPORT = "IMPORT_EXPORT"
    const val FINANZAS = "FINANZAS"
    const val EMPLEADO = "EMPLEADO"

    /** Todos los roles válidos */
    val ALL = setOf(ADMIN, RH, COMPRAS, MANTENIMIENTO, SEGURIDAD, ALMACEN, IMPORT_EXPORT, FINANZAS, EMPLEADO)

    /** Roles que pueden editar/modificar/eliminar empleados. */
    val CAN_MANAGE_EMPLOYEES = setOf(ADMIN, RH)

    /** Roles que pueden gestionar usuarios (alta/baja/edición). */
    val CAN_MANAGE_USERS = setOf(ADMIN)

    /** Roles con acceso de escritura al almacén */
    val WAREHOUSE_WRITE = setOf(ADMIN, ALMACEN)

    /** Roles con acceso de escritura a embarques */
    val SHIPPING_WRITE = setOf(ADMIN, IMPORT_EXPORT, ALMACEN)

    /** Roles con acceso de escritura a EHS */
    val EHS_WRITE = setOf(ADMIN, SEGURIDAD)

    /** Roles con acceso de escritura a compras/recepción */
    val PROCUREMENT_WRITE = setOf(ADMIN, COMPRAS)

    /** Roles con acceso de escritura a nómina/finanzas */
    val FINANCE_WRITE = setOf(ADMIN, FINANZAS, RH)

    /** Roles con acceso de escritura a producción */
    val PRODUCTION_WRITE = setOf(ADMIN)

    /** Roles con acceso de escritura a asistencia */
    val ATTENDANCE_WRITE = setOf(ADMIN, RH)

    /** Roles que pueden leer (cualquier rol autenticado) */
    val READ_ANY = ALL
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
 * Responde 401 si no hay token, 403 si no tiene el rol requerido.
 * Si tiene permiso, devuelve el rol.
 */
suspend fun requireRoleOr403(call: ApplicationCall, allowedRoles: Set<String>): String? {
    val authHeader = call.request.header(HttpHeaders.Authorization)
    if (authHeader == null) {
        call.respond(HttpStatusCode.Unauthorized, mapOf(
            "status" to "error",
            "message" to "Token de autenticación requerido. Envía Authorization: Bearer <email>"
        ))
        return null
    }
    val role = requireRole(call, allowedRoles)
    if (role == null) {
        call.respond(HttpStatusCode.Forbidden, mapOf(
            "status" to "error",
            "message" to "No tienes permisos para esta acción. Se requiere rol: ${allowedRoles.joinToString(" o ")}"
        ))
    }
    return role
}

/**
 * Helper para endpoints de solo lectura (cualquier usuario autenticado).
 */
suspend fun requireAuthOr401(call: ApplicationCall): String? {
    return requireRoleOr403(call, Roles.ALL)
}
