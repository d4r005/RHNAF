package com.example.rhnaf.routes

import com.example.rhnaf.database.DatabaseFactory
import com.example.rhnaf.database.ShipmentSummaryTable
import com.example.rhnaf.database.ShipmentTable
import com.example.rhnaf.database.WarehouseIncomingLogTable
import com.example.rhnaf.database.WarehouseInventoryTable
import com.example.rhnaf.database.WarehouseLocationTable
import com.example.rhnaf.database.WarehouseOutgoingLogTable
import com.example.rhnaf.database.WarehouseAuditTable
import com.example.rhnaf.shared.model.Shipment
import com.example.rhnaf.shared.model.ShipmentSummary
import com.example.rhnaf.shared.model.WarehouseIncomingLog
import com.example.rhnaf.shared.model.WarehouseInventoryItem
import com.example.rhnaf.shared.model.WarehouseLocation
import com.example.rhnaf.shared.model.WarehouseOutgoingLog
import com.example.rhnaf.shared.model.WarehouseAudit
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
import org.jetbrains.exposed.sql.Op

fun Route.warehouseRouting() {
    route("/api/v1/almacen") {

        // ---------- INVENTARIO (producto terminado por ubicación) ----------
        route("/inventario") {
            get {
                safeApiCall(call) {
                    requireRoleOr403(call, Roles.ALL) ?: return@safeApiCall
                    val result = pagedQuery(call, WarehouseInventoryTable.selectAll()) {
                        WarehouseInventoryItem(
                            id = it[WarehouseInventoryTable.id],
                            lugar = it[WarehouseInventoryTable.lugar],
                            po = it[WarehouseInventoryTable.po],
                            modelo = it[WarehouseInventoryTable.modelo],
                            cantidad = it[WarehouseInventoryTable.cantidad],
                            falta = it[WarehouseInventoryTable.falta],
                            existencia = it[WarehouseInventoryTable.existencia]
                        )
                    }
                    call.respond(result)
                }
            }
            post {
                safeApiCall(call) {
                    requireRoleOr403(call, Roles.WAREHOUSE_WRITE) ?: return@safeApiCall
                    val item = call.receive<WarehouseInventoryItem>()
                    DatabaseFactory.dbQuery {
                        WarehouseInventoryTable.insert {
                            it[lugar] = item.lugar
                            it[po] = item.po
                            it[modelo] = item.modelo
                            it[cantidad] = item.cantidad
                            it[falta] = item.falta
                            it[existencia] = item.existencia
                        }
                    }
                    call.respond(mapOf("status" to "ok"))
                }
            }
            put("/{id}") {
                safeApiCall(call) {
                    requireRoleOr403(call, Roles.WAREHOUSE_WRITE) ?: return@safeApiCall
                    val id = call.parameters["id"]?.toIntOrNull() ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                    val item = call.receive<WarehouseInventoryItem>()
                    DatabaseFactory.dbQuery {
                        WarehouseInventoryTable.update({ WarehouseInventoryTable.id eq id }) {
                            it[lugar] = item.lugar
                            it[po] = item.po
                            it[modelo] = item.modelo
                            it[cantidad] = item.cantidad
                            it[falta] = item.falta
                            it[existencia] = item.existencia
                        }
                    }
                    call.respond(mapOf("status" to "ok"))
                }
            }
            delete("/{id}") {
                safeApiCall(call) {
                    requireRoleOr403(call, Roles.WAREHOUSE_WRITE) ?: return@safeApiCall
                    val id = call.parameters["id"]?.toIntOrNull() ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                    DatabaseFactory.dbQuery { WarehouseInventoryTable.deleteWhere { WarehouseInventoryTable.id eq id } }
                    call.respond(mapOf("status" to "ok"))
                }
            }
            post("/bulk") {
                safeApiCall(call) {
                    requireRoleOr403(call, Roles.WAREHOUSE_WRITE) ?: return@safeApiCall
                    val items = call.receive<List<WarehouseInventoryItem>>()
                    DatabaseFactory.dbQuery {
                        items.forEach { item ->
                            WarehouseInventoryTable.insert {
                                it[lugar] = item.lugar
                                it[po] = item.po
                                it[modelo] = item.modelo
                                it[cantidad] = item.cantidad
                                it[falta] = item.falta
                                it[existencia] = item.existencia
                            }
                        }
                    }
                    call.respond(mapOf("insertados" to items.size.toString()))
                }
            }
            delete("/bulk/all") {
                safeApiCall(call) {
                    requireRoleOr403(call, setOf(Roles.ADMIN)) ?: return@safeApiCall
                    DatabaseFactory.dbQuery { WarehouseInventoryTable.deleteWhere { Op.TRUE } }
                    call.respond(mapOf("status" to "ok"))
                }
            }
        }

        // ---------- ENTRADAS (bitácora con fecha) ----------
        route("/entradas") {
            get {
                safeApiCall(call) {
                    requireRoleOr403(call, Roles.ALL) ?: return@safeApiCall
                    val result = pagedQuery(call, WarehouseIncomingLogTable.selectAll()) {
                        WarehouseIncomingLog(
                            id = it[WarehouseIncomingLogTable.id],
                            fecha = it[WarehouseIncomingLogTable.fecha],
                            po = it[WarehouseIncomingLogTable.po],
                            modelo = it[WarehouseIncomingLogTable.modelo],
                            cantidad = it[WarehouseIncomingLogTable.cantidad],
                            ubicacion = it[WarehouseIncomingLogTable.ubicacion]
                        )
                    }
                    call.respond(result)
                }
            }
            post {
                safeApiCall(call) {
                    requireRoleOr403(call, Roles.WAREHOUSE_WRITE) ?: return@safeApiCall
                    val item = call.receive<WarehouseIncomingLog>()
                    DatabaseFactory.dbQuery {
                        WarehouseIncomingLogTable.insert {
                            it[fecha] = item.fecha
                            it[po] = item.po
                            it[modelo] = item.modelo
                            it[cantidad] = item.cantidad
                            it[ubicacion] = item.ubicacion
                        }
                    }
                    call.respond(mapOf("status" to "ok"))
                }
            }
            delete("/{id}") {
                safeApiCall(call) {
                    requireRoleOr403(call, Roles.WAREHOUSE_WRITE) ?: return@safeApiCall
                    val id = call.parameters["id"]?.toIntOrNull() ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                    DatabaseFactory.dbQuery { WarehouseIncomingLogTable.deleteWhere { WarehouseIncomingLogTable.id eq id } }
                    call.respond(mapOf("status" to "ok"))
                }
            }
            post("/bulk") {
                safeApiCall(call) {
                    requireRoleOr403(call, Roles.WAREHOUSE_WRITE) ?: return@safeApiCall
                    val items = call.receive<List<WarehouseIncomingLog>>()
                    DatabaseFactory.dbQuery {
                        items.forEach { item ->
                            WarehouseIncomingLogTable.insert {
                                it[fecha] = item.fecha
                                it[po] = item.po
                                it[modelo] = item.modelo
                                it[cantidad] = item.cantidad
                                it[ubicacion] = item.ubicacion
                            }
                        }
                    }
                    call.respond(mapOf("insertados" to items.size.toString()))
                }
            }
            delete("/bulk/all") {
                safeApiCall(call) {
                    requireRoleOr403(call, setOf(Roles.ADMIN)) ?: return@safeApiCall
                    DatabaseFactory.dbQuery { WarehouseIncomingLogTable.deleteWhere { Op.TRUE } }
                    call.respond(mapOf("status" to "ok"))
                }
            }
        }

        // ---------- ENVIOS DETALLADOS (fd, sf, aj, evf, rbt) ----------
        route("/envios") {
            get {
                safeApiCall(call) {
                    requireRoleOr403(call, Roles.ALL) ?: return@safeApiCall
                    val cliente = call.request.queryParameters["cliente"]
                    val baseQuery = if (cliente != null)
                        ShipmentTable.selectAll().where { ShipmentTable.cliente eq cliente }
                    else
                        ShipmentTable.selectAll()
                    val result = pagedQuery(call, baseQuery) {
                        Shipment(
                            id = it[ShipmentTable.id],
                            cliente = it[ShipmentTable.cliente],
                            fechaCarga = it[ShipmentTable.fechaCarga],
                            poContenedor = it[ShipmentTable.poContenedor],
                            sku = it[ShipmentTable.sku],
                            nombreProducto = it[ShipmentTable.nombreProducto],
                            numeroSello = it[ShipmentTable.numeroSello],
                            placa = it[ShipmentTable.placa],
                            cantidad = it[ShipmentTable.cantidad],
                            gabinetes = it[ShipmentTable.gabinetes],
                            conductor = it[ShipmentTable.conductor],
                            horaInicio = it[ShipmentTable.horaInicio],
                            horaFin = it[ShipmentTable.horaFin],
                            operador = it[ShipmentTable.operador],
                            inspector = it[ShipmentTable.inspector]
                        )
                    }
                    call.respond(result)
                }
            }
            post {
                safeApiCall(call) {
                    requireRoleOr403(call, Roles.SHIPPING_WRITE) ?: return@safeApiCall
                    val item = call.receive<Shipment>()
                    DatabaseFactory.dbQuery {
                        ShipmentTable.insert {
                            it[cliente] = item.cliente
                            it[fechaCarga] = item.fechaCarga
                            it[poContenedor] = item.poContenedor
                            it[sku] = item.sku
                            it[nombreProducto] = item.nombreProducto
                            it[numeroSello] = item.numeroSello
                            it[placa] = item.placa
                            it[cantidad] = item.cantidad
                            it[gabinetes] = item.gabinetes
                            it[conductor] = item.conductor
                            it[horaInicio] = item.horaInicio
                            it[horaFin] = item.horaFin
                            it[operador] = item.operador
                            it[inspector] = item.inspector
                        }
                    }
                    call.respond(mapOf("status" to "ok"))
                }
            }
            delete("/{id}") {
                safeApiCall(call) {
                    requireRoleOr403(call, Roles.SHIPPING_WRITE) ?: return@safeApiCall
                    val id = call.parameters["id"]?.toIntOrNull() ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                    DatabaseFactory.dbQuery { ShipmentTable.deleteWhere { ShipmentTable.id eq id } }
                    call.respond(mapOf("status" to "ok"))
                }
            }
        }

        // ---------- UBICACIONES ----------
        route("/ubicaciones") {
            get {
                safeApiCall(call) {
                    requireRoleOr403(call, Roles.ALL) ?: return@safeApiCall
                    val result = pagedQuery(call, WarehouseLocationTable.selectAll()) {
                        WarehouseLocation(
                            id = it[WarehouseLocationTable.id],
                            codigo = it[WarehouseLocationTable.codigo],
                            zona = it[WarehouseLocationTable.zona],
                            tipo = it[WarehouseLocationTable.tipo],
                            capacidad = it[WarehouseLocationTable.capacidad],
                            ocupacion = it[WarehouseLocationTable.ocupacion],
                            estado = it[WarehouseLocationTable.estado],
                            notas = it[WarehouseLocationTable.notas]
                        )
                    }
                    call.respond(result)
                }
            }
            post {
                safeApiCall(call) {
                    requireRoleOr403(call, Roles.WAREHOUSE_WRITE) ?: return@safeApiCall
                    val item = call.receive<WarehouseLocation>()
                    DatabaseFactory.dbQuery {
                        WarehouseLocationTable.insert {
                            it[codigo] = item.codigo
                            it[zona] = item.zona
                            it[tipo] = item.tipo
                            it[capacidad] = item.capacidad
                            it[ocupacion] = item.ocupacion
                            it[estado] = item.estado
                            it[notas] = item.notas
                        }
                    }
                    call.respond(mapOf("status" to "ok"))
                }
            }
            put("/{id}") {
                safeApiCall(call) {
                    requireRoleOr403(call, Roles.WAREHOUSE_WRITE) ?: return@safeApiCall
                    val id = call.parameters["id"]?.toIntOrNull() ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                    val item = call.receive<WarehouseLocation>()
                    DatabaseFactory.dbQuery {
                        WarehouseLocationTable.update({ WarehouseLocationTable.id eq id }) {
                            it[codigo] = item.codigo
                            it[zona] = item.zona
                            it[tipo] = item.tipo
                            it[capacidad] = item.capacidad
                            it[ocupacion] = item.ocupacion
                            it[estado] = item.estado
                            it[notas] = item.notas
                        }
                    }
                    call.respond(mapOf("status" to "ok"))
                }
            }
            delete("/{id}") {
                safeApiCall(call) {
                    requireRoleOr403(call, Roles.WAREHOUSE_WRITE) ?: return@safeApiCall
                    val id = call.parameters["id"]?.toIntOrNull() ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                    DatabaseFactory.dbQuery { WarehouseLocationTable.deleteWhere { WarehouseLocationTable.id eq id } }
                    call.respond(mapOf("status" to "ok"))
                }
            }
        }

        // ---------- SALIDAS ----------
        route("/salidas") {
            get {
                safeApiCall(call) {
                    requireRoleOr403(call, Roles.ALL) ?: return@safeApiCall
                    val result = pagedQuery(call, WarehouseOutgoingLogTable.selectAll()) {
                        WarehouseOutgoingLog(
                            id = it[WarehouseOutgoingLogTable.id],
                            fecha = it[WarehouseOutgoingLogTable.fecha],
                            po = it[WarehouseOutgoingLogTable.po],
                            modelo = it[WarehouseOutgoingLogTable.modelo],
                            cantidad = it[WarehouseOutgoingLogTable.cantidad],
                            ubicacion = it[WarehouseOutgoingLogTable.ubicacion],
                            motivo = it[WarehouseOutgoingLogTable.motivo],
                            responsable = it[WarehouseOutgoingLogTable.responsable]
                        )
                    }
                    call.respond(result)
                }
            }
            post {
                safeApiCall(call) {
                    requireRoleOr403(call, Roles.WAREHOUSE_WRITE) ?: return@safeApiCall
                    val item = call.receive<WarehouseOutgoingLog>()
                    DatabaseFactory.dbQuery {
                        WarehouseOutgoingLogTable.insert {
                            it[fecha] = item.fecha
                            it[po] = item.po
                            it[modelo] = item.modelo
                            it[cantidad] = item.cantidad
                            it[ubicacion] = item.ubicacion
                            it[motivo] = item.motivo
                            it[responsable] = item.responsable
                        }
                    }
                    call.respond(mapOf("status" to "ok"))
                }
            }
            delete("/{id}") {
                safeApiCall(call) {
                    requireRoleOr403(call, Roles.WAREHOUSE_WRITE) ?: return@safeApiCall
                    val id = call.parameters["id"]?.toIntOrNull() ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                    DatabaseFactory.dbQuery { WarehouseOutgoingLogTable.deleteWhere { WarehouseOutgoingLogTable.id eq id } }
                    call.respond(mapOf("status" to "ok"))
                }
            }
        }

        // ---------- AUDITORIAS ----------
        route("/auditorias") {
            get {
                safeApiCall(call) {
                    requireRoleOr403(call, Roles.ALL) ?: return@safeApiCall
                    val result = pagedQuery(call, WarehouseAuditTable.selectAll()) {
                        WarehouseAudit(
                            id = it[WarehouseAuditTable.id],
                            fecha = it[WarehouseAuditTable.fecha],
                            ubicacion = it[WarehouseAuditTable.ubicacion],
                            modelo = it[WarehouseAuditTable.modelo],
                            cantidadSistema = it[WarehouseAuditTable.cantidadSistema],
                            cantidadFisica = it[WarehouseAuditTable.cantidadFisica],
                            diferencia = it[WarehouseAuditTable.diferencia],
                            responsable = it[WarehouseAuditTable.responsable],
                            observaciones = it[WarehouseAuditTable.observaciones],
                            estado = it[WarehouseAuditTable.estado]
                        )
                    }
                    call.respond(result)
                }
            }
            post {
                safeApiCall(call) {
                    requireRoleOr403(call, Roles.WAREHOUSE_WRITE) ?: return@safeApiCall
                    val item = call.receive<WarehouseAudit>()
                    DatabaseFactory.dbQuery {
                        WarehouseAuditTable.insert {
                            it[fecha] = item.fecha
                            it[ubicacion] = item.ubicacion
                            it[modelo] = item.modelo
                            it[cantidadSistema] = item.cantidadSistema
                            it[cantidadFisica] = item.cantidadFisica
                            it[diferencia] = item.diferencia
                            it[responsable] = item.responsable
                            it[observaciones] = item.observaciones
                            it[estado] = item.estado
                        }
                    }
                    call.respond(mapOf("status" to "ok"))
                }
            }
            delete("/{id}") {
                safeApiCall(call) {
                    requireRoleOr403(call, Roles.WAREHOUSE_WRITE) ?: return@safeApiCall
                    val id = call.parameters["id"]?.toIntOrNull() ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                    DatabaseFactory.dbQuery { WarehouseAuditTable.deleteWhere { WarehouseAuditTable.id eq id } }
                    call.respond(mapOf("status" to "ok"))
                }
            }
        }

        // ---------- RESUMEN DE EMBARQUES (envíos por cliente) ----------
        get("/envios/resumen") {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.ALL) ?: return@safeApiCall
                val items = DatabaseFactory.dbQuery {
                    ShipmentSummaryTable.selectAll().map {
                        ShipmentSummary(
                            id = it[ShipmentSummaryTable.id],
                            cliente = it[ShipmentSummaryTable.cliente],
                            po = it[ShipmentSummaryTable.po],
                            modelo = it[ShipmentSummaryTable.modelo],
                            cantidad = it[ShipmentSummaryTable.cantidad],
                            fecha = it[ShipmentSummaryTable.fecha]
                        )
                    }
                }
                call.respond(items)
            }
        }
    }
}
