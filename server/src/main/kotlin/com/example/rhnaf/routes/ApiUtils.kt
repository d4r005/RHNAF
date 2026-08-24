package com.example.rhnaf.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.http.*
import org.jetbrains.exposed.sql.*

/**
 * Utilidades de paginación y manejo de errores para todos los endpoints.
 */

data class PagedResult<T>(
    val data: List<T>,
    val total: Int,
    val limit: Int,
    val offset: Int,
    val hasMore: Boolean
)

object PaginationUtil {
    private const val DEFAULT_LIMIT = 50
    private const val MAX_LIMIT = 500

    fun getLimit(call: ApplicationCall): Int {
        val raw = call.request.queryParameters["limit"]?.toIntOrNull() ?: DEFAULT_LIMIT
        return raw.coerceIn(1, MAX_LIMIT)
    }

    fun getOffset(call: ApplicationCall): Int {
        return call.request.queryParameters["offset"]?.toIntOrNull() ?: 0
    }
}

/**
 * Wraps a database query with pagination.
 * Usage: val result = pagedQuery(call, SomeTable.selectAll()) { row -> SomeModel(...) }
 */
fun <T> pagedQuery(call: ApplicationCall, baseQuery: Query, mapper: (ResultRow) -> T): PagedResult<T> {
    val limit = PaginationUtil.getLimit(call)
    val offset = PaginationUtil.getOffset(call)
    val total = baseQuery.count().toInt()
    val rows = baseQuery.limit(limit, offset.toLong()).toList()
    val data = rows.map(mapper)
    return PagedResult(
        data = data,
        total = total,
        limit = limit,
        offset = offset,
        hasMore = (offset + limit) < total
    )
}

/**
 * Wraps a list result with pagination metadata.
 */
fun <T> pagedList(call: ApplicationCall, allItems: List<T>): PagedResult<T> {
    val limit = PaginationUtil.getLimit(call)
    val offset = PaginationUtil.getOffset(call)
    val total = allItems.size
    val data = allItems.drop(offset).take(limit)
    return PagedResult(
        data = data,
        total = total,
        limit = limit,
        offset = offset,
        hasMore = (offset + limit) < total
    )
}

/**
 * Safe API call wrapper — catches exceptions and returns proper HTTP error.
 */
suspend fun safeApiCall(call: ApplicationCall, block: suspend () -> Unit) {
    try {
        block()
    } catch (e: IllegalArgumentException) {
        call.respond(HttpStatusCode.BadRequest, mapOf(
            "status" to "error",
            "message" to (e.message ?: "Datos inválidos")
        ))
    } catch (e: NoSuchElementException) {
        call.respond(HttpStatusCode.NotFound, mapOf(
            "status" to "error",
            "message" to (e.message ?: "Recurso no encontrado")
        ))
    } catch (e: Exception) {
        call.respond(HttpStatusCode.InternalServerError, mapOf(
            "status" to "error",
            "message" to (e.message ?: "Error interno del servidor")
        ))
    }
}
