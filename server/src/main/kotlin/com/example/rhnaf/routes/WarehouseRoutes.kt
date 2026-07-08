package com.example.rhnaf.routes

import com.example.rhnaf.database.DatabaseFactory
import com.example.rhnaf.database.ShipmentSummaryTable
import com.example.rhnaf.database.ShipmentTable
import com.example.rhnaf.database.WarehouseIncomingLogTable
import com.example.rhnaf.database.WarehouseInventoryTable
import com.example.rhnaf.shared.model.Shipment
import com.example.rhnaf.shared.model.ShipmentSummary
import com.example.rhnaf.shared.model.WarehouseIncomingLog
import com.example.rhnaf.shared.model.WarehouseInventoryItem
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

fun Route.warehouseRouting() {
    route("/api/v1/almacen") {

        // ---------- INVENTARIO (producto terminado por ubicación) ----------
        route("/inventario") {
            get {
                val items = DatabaseFactory.dbQuery {
                    WarehouseInventoryTable.selectAll().map {
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
                }
                call.respond(items)
            }
            post {
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
            put("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull() ?: return@put call.respond(HttpStatusCode.BadRequest)
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
            delete("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
                DatabaseFactory.dbQuery {
                    WarehouseInventoryTable.deleteWhere { WarehouseInventoryTable.id eq id }
                }
                call.respond(mapOf("status" to "ok"))
            }
            post("/bulk") {
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
            delete("/bulk/all") {
                DatabaseFactory.dbQuery { WarehouseInventoryTable.deleteWhere { org.jetbrains.exposed.sql.Op.TRUE } }
                call.respond(mapOf("status" to "ok"))
            }
        }

        // ---------- ENTRADAS (bitácora con fecha) ----------
        route("/entradas") {
            get {
                val items = DatabaseFactory.dbQuery {
                    WarehouseIncomingLogTable.selectAll().map {
                        WarehouseIncomingLog(
                            id = it[WarehouseIncomingLogTable.id],
                            fecha = it[WarehouseIncomingLogTable.fecha],
                            po = it[WarehouseIncomingLogTable.po],
                            modelo = it[WarehouseIncomingLogTable.modelo],
                            cantidad = it[WarehouseIncomingLogTable.cantidad],
                            ubicacion = it[WarehouseIncomingLogTable.ubicacion]
                        )
                    }
                }
                call.respond(items)
            }
            post {
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
            delete("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
                DatabaseFactory.dbQuery {
                    WarehouseIncomingLogTable.deleteWhere { WarehouseIncomingLogTable.id eq id }
                }
                call.respond(mapOf("status" to "ok"))
            }
            post("/bulk") {
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
            delete("/bulk/all") {
                DatabaseFactory.dbQuery { WarehouseIncomingLogTable.deleteWhere { org.jetbrains.exposed.sql.Op.TRUE } }
                call.respond(mapOf("status" to "ok"))
            }
        }

        // ---------- ENVIOS DETALLADOS (fd, sf, aj, evf, rbt) ----------
        route("/envios") {
            get {
                val items = DatabaseFactory.dbQuery {
                    ShipmentTable.selectAll().map {
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
                }
                call.respond(items)
            }
            post {
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
            delete("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
                DatabaseFactory.dbQuery {
                    ShipmentTable.deleteWhere { ShipmentTable.id eq id }
                }
                call.respond(mapOf("status" to "ok"))
            }
            post("/bulk") {
                val items = call.receive<List<Shipment>>()
                DatabaseFactory.dbQuery {
                    items.forEach { item ->
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
                }
                call.respond(mapOf("insertados" to items.size.toString()))
            }
            delete("/bulk/all") {
                DatabaseFactory.dbQuery { ShipmentTable.deleteWhere { org.jetbrains.exposed.sql.Op.TRUE } }
                call.respond(mapOf("status" to "ok"))
            }
        }

        // ---------- RESUMEN DE ENVIOS (hoja "producto que salio de planta") ----------
        route("/envios-resumen") {
            get {
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
            post {
                val item = call.receive<ShipmentSummary>()
                DatabaseFactory.dbQuery {
                    ShipmentSummaryTable.insert {
                        it[cliente] = item.cliente
                        it[po] = item.po
                        it[modelo] = item.modelo
                        it[cantidad] = item.cantidad
                        it[fecha] = item.fecha
                    }
                }
                call.respond(mapOf("status" to "ok"))
            }
            delete("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
                DatabaseFactory.dbQuery {
                    ShipmentSummaryTable.deleteWhere { ShipmentSummaryTable.id eq id }
                }
                call.respond(mapOf("status" to "ok"))
            }
            post("/bulk") {
                val items = call.receive<List<ShipmentSummary>>()
                DatabaseFactory.dbQuery {
                    items.forEach { item ->
                        ShipmentSummaryTable.insert {
                            it[cliente] = item.cliente
                            it[po] = item.po
                            it[modelo] = item.modelo
                            it[cantidad] = item.cantidad
                            it[fecha] = item.fecha
                        }
                    }
                }
                call.respond(mapOf("insertados" to items.size.toString()))
            }
            delete("/bulk/all") {
                DatabaseFactory.dbQuery { ShipmentSummaryTable.deleteWhere { org.jetbrains.exposed.sql.Op.TRUE } }
                call.respond(mapOf("status" to "ok"))
            }
        }
    }
}
