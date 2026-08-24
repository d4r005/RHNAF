package com.example.rhnaf.routes

import com.example.rhnaf.database.DatabaseFactory
import com.example.rhnaf.database.DeliveryRouteTable
import com.example.rhnaf.database.OrderTable
import com.example.rhnaf.database.ShipmentTable
import com.example.rhnaf.database.TraceabilityEventTable
import com.example.rhnaf.shared.model.DeliveryRoute
import com.example.rhnaf.shared.model.Order
import com.example.rhnaf.shared.model.TraceabilityEvent
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

fun Route.shippingRouting() {
    route("/api/v1/embarques") {

        // ---------- LOGISTICA (resumen operativo) ----------
        get("/logistica/resumen") {
            val resumen = DatabaseFactory.dbQuery {
                val pedidos = OrderTable.selectAll().toList()
                val rutas = DeliveryRouteTable.selectAll().toList()
                val envios = ShipmentTable.selectAll().count()

                mapOf(
                    "pedidosTotal" to pedidos.size.toString(),
                    "pedidosPendientes" to pedidos.count { it[OrderTable.estado] == "Pendiente" }.toString(),
                    "pedidosEnPreparacion" to pedidos.count { it[OrderTable.estado] == "En preparacion" }.toString(),
                    "pedidosListos" to pedidos.count { it[OrderTable.estado] == "Listo para embarque" }.toString(),
                    "pedidosEntregados" to pedidos.count { it[OrderTable.estado] == "Entregado" }.toString(),
                    "rutasTotal" to rutas.size.toString(),
                    "rutasEnCurso" to rutas.count { it[DeliveryRouteTable.estado] == "En ruta" }.toString(),
                    "rutasRetrasadas" to rutas.count { it[DeliveryRouteTable.estado] == "Retrasada" }.toString(),
                    "enviosTotal" to envios.toString()
                )
            }
            call.respond(resumen)
        }

        // ---------- PEDIDOS ----------
        route("/pedidos") {
            get {
                val items = DatabaseFactory.dbQuery {
                    OrderTable.selectAll().map {
                        Order(
                            id = it[OrderTable.id],
                            numeroPedido = it[OrderTable.numeroPedido],
                            cliente = it[OrderTable.cliente],
                            fechaPedido = it[OrderTable.fechaPedido],
                            fechaEntregaSolicitada = it[OrderTable.fechaEntregaSolicitada],
                            modelo = it[OrderTable.modelo],
                            cantidad = it[OrderTable.cantidad],
                            prioridad = it[OrderTable.prioridad],
                            estado = it[OrderTable.estado],
                            notas = it[OrderTable.notas]
                        )
                    }
                }
                call.respond(items)
            }
            post {
                val item = call.receive<Order>()
                DatabaseFactory.dbQuery {
                    OrderTable.insert {
                        it[numeroPedido] = item.numeroPedido
                        it[cliente] = item.cliente
                        it[fechaPedido] = item.fechaPedido
                        it[fechaEntregaSolicitada] = item.fechaEntregaSolicitada
                        it[modelo] = item.modelo
                        it[cantidad] = item.cantidad
                        it[prioridad] = item.prioridad
                        it[estado] = item.estado
                        it[notas] = item.notas
                    }
                }
                call.respond(mapOf("status" to "ok"))
            }
            put("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull() ?: return@put call.respond(HttpStatusCode.BadRequest)
                val item = call.receive<Order>()
                DatabaseFactory.dbQuery {
                    OrderTable.update({ OrderTable.id eq id }) {
                        it[numeroPedido] = item.numeroPedido
                        it[cliente] = item.cliente
                        it[fechaPedido] = item.fechaPedido
                        it[fechaEntregaSolicitada] = item.fechaEntregaSolicitada
                        it[modelo] = item.modelo
                        it[cantidad] = item.cantidad
                        it[prioridad] = item.prioridad
                        it[estado] = item.estado
                        it[notas] = item.notas
                    }
                }
                call.respond(mapOf("status" to "ok"))
            }
            delete("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
                DatabaseFactory.dbQuery {
                    OrderTable.deleteWhere { OrderTable.id eq id }
                }
                call.respond(mapOf("status" to "ok"))
            }
        }

        // ---------- RUTAS DE ENTREGA ----------
        route("/rutas") {
            get {
                val items = DatabaseFactory.dbQuery {
                    DeliveryRouteTable.selectAll().map {
                        DeliveryRoute(
                            id = it[DeliveryRouteTable.id],
                            nombreRuta = it[DeliveryRouteTable.nombreRuta],
                            fechaSalida = it[DeliveryRouteTable.fechaSalida],
                            fechaEntregaEstimada = it[DeliveryRouteTable.fechaEntregaEstimada],
                            conductor = it[DeliveryRouteTable.conductor],
                            vehiculo = it[DeliveryRouteTable.vehiculo],
                            placa = it[DeliveryRouteTable.placa],
                            paradas = it[DeliveryRouteTable.paradas],
                            pedidosAsociados = it[DeliveryRouteTable.pedidosAsociados],
                            estado = it[DeliveryRouteTable.estado],
                            notas = it[DeliveryRouteTable.notas]
                        )
                    }
                }
                call.respond(items)
            }
            post {
                val item = call.receive<DeliveryRoute>()
                DatabaseFactory.dbQuery {
                    DeliveryRouteTable.insert {
                        it[nombreRuta] = item.nombreRuta
                        it[fechaSalida] = item.fechaSalida
                        it[fechaEntregaEstimada] = item.fechaEntregaEstimada
                        it[conductor] = item.conductor
                        it[vehiculo] = item.vehiculo
                        it[placa] = item.placa
                        it[paradas] = item.paradas
                        it[pedidosAsociados] = item.pedidosAsociados
                        it[estado] = item.estado
                        it[notas] = item.notas
                    }
                }
                call.respond(mapOf("status" to "ok"))
            }
            put("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull() ?: return@put call.respond(HttpStatusCode.BadRequest)
                val item = call.receive<DeliveryRoute>()
                DatabaseFactory.dbQuery {
                    DeliveryRouteTable.update({ DeliveryRouteTable.id eq id }) {
                        it[nombreRuta] = item.nombreRuta
                        it[fechaSalida] = item.fechaSalida
                        it[fechaEntregaEstimada] = item.fechaEntregaEstimada
                        it[conductor] = item.conductor
                        it[vehiculo] = item.vehiculo
                        it[placa] = item.placa
                        it[paradas] = item.paradas
                        it[pedidosAsociados] = item.pedidosAsociados
                        it[estado] = item.estado
                        it[notas] = item.notas
                    }
                }
                call.respond(mapOf("status" to "ok"))
            }
            delete("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
                DatabaseFactory.dbQuery {
                    DeliveryRouteTable.deleteWhere { DeliveryRouteTable.id eq id }
                }
                call.respond(mapOf("status" to "ok"))
            }
        }

        // ---------- TRAZABILIDAD (bitacora cronologica por referencia) ----------
        route("/trazabilidad") {
            get {
                val referenciaFiltro = call.request.queryParameters["referencia"]
                val items = DatabaseFactory.dbQuery {
                    val query = if (referenciaFiltro != null) {
                        TraceabilityEventTable.selectAll().where { TraceabilityEventTable.referencia eq referenciaFiltro }
                    } else {
                        TraceabilityEventTable.selectAll()
                    }
                    query.map {
                        TraceabilityEvent(
                            id = it[TraceabilityEventTable.id],
                            referencia = it[TraceabilityEventTable.referencia],
                            tipoReferencia = it[TraceabilityEventTable.tipoReferencia],
                            evento = it[TraceabilityEventTable.evento],
                            fecha = it[TraceabilityEventTable.fecha],
                            ubicacion = it[TraceabilityEventTable.ubicacion],
                            responsable = it[TraceabilityEventTable.responsable],
                            notas = it[TraceabilityEventTable.notas]
                        )
                    }
                }
                call.respond(items)
            }
            post {
                val item = call.receive<TraceabilityEvent>()
                DatabaseFactory.dbQuery {
                    TraceabilityEventTable.insert {
                        it[referencia] = item.referencia
                        it[tipoReferencia] = item.tipoReferencia
                        it[evento] = item.evento
                        it[fecha] = item.fecha
                        it[ubicacion] = item.ubicacion
                        it[responsable] = item.responsable
                        it[notas] = item.notas
                    }
                }
                call.respond(mapOf("status" to "ok"))
            }
            delete("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
                DatabaseFactory.dbQuery {
                    TraceabilityEventTable.deleteWhere { TraceabilityEventTable.id eq id }
                }
                call.respond(mapOf("status" to "ok"))
            }
        }
    }
}
