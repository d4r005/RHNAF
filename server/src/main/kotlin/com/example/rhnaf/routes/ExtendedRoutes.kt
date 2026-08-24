package com.example.rhnaf.routes

import com.example.rhnaf.database.*
import com.example.rhnaf.shared.model.*
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
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.SortOrder

fun Route.extendedRouting() {

    // ============ FERRETERIA ============
    route("/api/v1/ferreteria") {
        get {
            val items = DatabaseFactory.dbQuery {
                FerreteriaTable.selectAll().map {
                    FerreteriaItem(
                        id = it[FerreteriaTable.id],
                        descripcion = it[FerreteriaTable.descripcion],
                        categoria = it[FerreteriaTable.categoria],
                        stock = it[FerreteriaTable.stock],
                        unidad = it[FerreteriaTable.unidad],
                        ubicacion = it[FerreteriaTable.ubicacion],
                        notas = it[FerreteriaTable.notas]
                    )
                }
            }
            call.respond(items)
        }
        post {
            val item = call.receive<FerreteriaItem>()
            DatabaseFactory.dbQuery {
                FerreteriaTable.insert {
                    it[descripcion] = item.descripcion
                    it[categoria] = item.categoria
                    it[stock] = item.stock
                    it[unidad] = item.unidad
                    it[ubicacion] = item.ubicacion
                    it[notas] = item.notas
                }
            }
            call.respond(mapOf("status" to "ok"))
        }
        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
            DatabaseFactory.dbQuery { FerreteriaTable.deleteWhere { FerreteriaTable.id eq id } }
            call.respond(mapOf("status" to "ok"))
        }
    }

    // ============ RECEPCION DE MATERIAS PRIMAS ============
    route("/api/v1/recepcion-mp") {
        get {
            val tipo = call.request.queryParameters["tipo"]
            val items = DatabaseFactory.dbQuery {
                val query = if (tipo != null) RecepcionMPTable.selectAll().where { RecepcionMPTable.tipo eq tipo }
                            else RecepcionMPTable.selectAll()
                query.map {
                    RecepcionMP(
                        id = it[RecepcionMPTable.id],
                        fecha = it[RecepcionMPTable.fecha],
                        tipo = it[RecepcionMPTable.tipo],
                        descripcion = it[RecepcionMPTable.descripcion],
                        proveedor = it[RecepcionMPTable.proveedor],
                        cantidad = it[RecepcionMPTable.cantidad],
                        unidad = it[RecepcionMPTable.unidad],
                        folio = it[RecepcionMPTable.folio],
                        notas = it[RecepcionMPTable.notas]
                    )
                }
            }
            call.respond(items)
        }
        post {
            val item = call.receive<RecepcionMP>()
            DatabaseFactory.dbQuery {
                RecepcionMPTable.insert {
                    it[fecha] = item.fecha
                    it[tipo] = item.tipo
                    it[descripcion] = item.descripcion
                    it[proveedor] = item.proveedor
                    it[cantidad] = item.cantidad
                    it[unidad] = item.unidad
                    it[folio] = item.folio
                    it[notas] = item.notas
                }
            }
            call.respond(mapOf("status" to "ok"))
        }
        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
            DatabaseFactory.dbQuery { RecepcionMPTable.deleteWhere { RecepcionMPTable.id eq id } }
            call.respond(mapOf("status" to "ok"))
        }
    }

    // ============ TARIMAS ============
    route("/api/v1/almacen/tarimas") {
        get {
            val items = DatabaseFactory.dbQuery {
                TarimaTable.selectAll().map {
                    Tarima(
                        id = it[TarimaTable.id],
                        compania = it[TarimaTable.compania],
                        fechaLlegada = it[TarimaTable.fechaLlegada],
                        folio = it[TarimaTable.folio],
                        cantidad = it[TarimaTable.cantidad],
                        medidas = it[TarimaTable.medidas],
                        rechazadas = it[TarimaTable.rechazadas],
                        cantidadAceptable = it[TarimaTable.cantidadAceptable],
                        fechaRegreso = it[TarimaTable.fechaRegreso]
                    )
                }
            }
            call.respond(items)
        }
        post {
            val item = call.receive<Tarima>()
            DatabaseFactory.dbQuery {
                TarimaTable.insert {
                    it[compania] = item.compania
                    it[fechaLlegada] = item.fechaLlegada
                    it[folio] = item.folio
                    it[cantidad] = item.cantidad
                    it[medidas] = item.medidas
                    it[rechazadas] = item.rechazadas
                    it[cantidadAceptable] = item.cantidadAceptable
                    it[fechaRegreso] = item.fechaRegreso
                }
            }
            call.respond(mapOf("status" to "ok"))
        }
        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
            DatabaseFactory.dbQuery { TarimaTable.deleteWhere { TarimaTable.id eq id } }
            call.respond(mapOf("status" to "ok"))
        }
    }

    // ============ CONTENEDORES DE CHINA ============
    route("/api/v1/embarques/contenedores-china") {
        get {
            val items = DatabaseFactory.dbQuery {
                ContenedorChinaTable.selectAll().map {
                    ContenedorChina(
                        id = it[ContenedorChinaTable.id],
                        fecha = it[ContenedorChinaTable.fecha],
                        codigoProducto = it[ContenedorChinaTable.codigoProducto],
                        nombre = it[ContenedorChinaTable.nombre],
                        especificacion = it[ContenedorChinaTable.especificacion],
                        modelo = it[ContenedorChinaTable.modelo],
                        cantidad = it[ContenedorChinaTable.cantidad],
                        unidad = it[ContenedorChinaTable.unidad]
                    )
                }
            }
            call.respond(items)
        }
        post {
            val item = call.receive<ContenedorChina>()
            DatabaseFactory.dbQuery {
                ContenedorChinaTable.insert {
                    it[fecha] = item.fecha
                    it[codigoProducto] = item.codigoProducto
                    it[nombre] = item.nombre
                    it[especificacion] = item.especificacion
                    it[modelo] = item.modelo
                    it[cantidad] = item.cantidad
                    it[unidad] = item.unidad
                }
            }
            call.respond(mapOf("status" to "ok"))
        }
        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
            DatabaseFactory.dbQuery { ContenedorChinaTable.deleteWhere { ContenedorChinaTable.id eq id } }
            call.respond(mapOf("status" to "ok"))
        }
    }

    // ============ SELLOS EN STOCK ============
    route("/api/v1/almacen/sellos") {
        get {
            val items = DatabaseFactory.dbQuery {
                SelloStockTable.selectAll().map {
                    SelloStock(
                        id = it[SelloStockTable.id],
                        fecha = it[SelloStockTable.fecha],
                        numeroInicial = it[SelloStockTable.numeroInicial],
                        numeroFinal = it[SelloStockTable.numeroFinal],
                        cantidad = it[SelloStockTable.cantidad]
                    )
                }
            }
            call.respond(items)
        }
        post {
            val item = call.receive<SelloStock>()
            DatabaseFactory.dbQuery {
                SelloStockTable.insert {
                    it[fecha] = item.fecha
                    it[numeroInicial] = item.numeroInicial
                    it[numeroFinal] = item.numeroFinal
                    it[cantidad] = item.cantidad
                }
            }
            call.respond(mapOf("status" to "ok"))
        }
        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
            DatabaseFactory.dbQuery { SelloStockTable.deleteWhere { SelloStockTable.id eq id } }
            call.respond(mapOf("status" to "ok"))
        }
    }

    // ============ BITACORA DE GAS ============
    route("/api/v1/ehs/gas") {
        get {
            val items = DatabaseFactory.dbQuery {
                GasConsumoTable.selectAll().orderBy(GasConsumoTable.id, SortOrder.DESC).map {
                    GasConsumo(
                        id = it[GasConsumoTable.id],
                        fecha = it[GasConsumoTable.fecha],
                        cantidadTexto = it[GasConsumoTable.cantidadTexto],
                        numeroTanques = it[GasConsumoTable.numeroTanques],
                        litrosPorTanque = it[GasConsumoTable.litrosPorTanque],
                        litrosTotales = it[GasConsumoTable.litrosTotales],
                        diaCarga = it[GasConsumoTable.diaCarga]
                    )
                }
            }
            call.respond(items)
        }
        post {
            val item = call.receive<GasConsumo>()
            // Parse numero de tanques del texto si no viene
            val numTanques = if (item.numeroTanques > 0) item.numeroTanques
                            else item.cantidadTexto.split(" ").firstOrNull()?.toIntOrNull() ?: 0
            val lpt = item.litrosPorTanque
            val litrosTotal = numTanques * lpt
            DatabaseFactory.dbQuery {
                GasConsumoTable.insert {
                    it[GasConsumoTable.fecha] = item.fecha
                    it[GasConsumoTable.cantidadTexto] = item.cantidadTexto.ifBlank { "$numTanques TANQUES" }
                    it[GasConsumoTable.numeroTanques] = numTanques
                    it[GasConsumoTable.litrosPorTanque] = lpt
                    it[GasConsumoTable.litrosTotales] = litrosTotal
                    it[GasConsumoTable.diaCarga] = item.diaCarga
                }
            }
            call.respond(mapOf("status" to "ok"))
        }
        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
            DatabaseFactory.dbQuery { GasConsumoTable.deleteWhere { GasConsumoTable.id eq id } }
            call.respond(mapOf("status" to "ok"))
        }

        // ============ HUELLA DE CARBONO (para COA) ============
        get("/huella-carbono") {
            val litrosPorTanque = call.request.queryParameters["litrosPorTanque"]?.toIntOrNull() ?: 1000
            val factorEmision = call.request.queryParameters["factorEmision"]?.toDoubleOrNull() ?: 1.51
            val anio = call.request.queryParameters["anio"]?.toIntOrNull()
            // kg CO2 por litro de LPG = 1.51 (IPCC)
            // Toneladas CO2 = kg / 1000

            val allRecords = DatabaseFactory.dbQuery {
                GasConsumoTable.selectAll().map {
                    Triple(
                        it[GasConsumoTable.fecha],
                        it[GasConsumoTable.numeroTanques],
                        it[GasConsumoTable.litrosTotales]
                    )
                }
            }

            // Filtrar por anio si se especifica, de lo contrario usar todos
            val records = if (anio != null) {
                allRecords.filter { it.first.startsWith(anio.toString()) }
            } else {
                allRecords
            }

            // Agrupar por mes (formato fecha: "2024-05-10 00:00:00" o "2024-05-10")
            val monthlyMap = mutableMapOf<String, MutableList<Int>>()
            for ((fecha, tanques, litros) in records) {
                val mesKey = if (fecha.length >= 7) fecha.substring(0, 7) else "unknown"
                monthlyMap.getOrPut(mesKey) { mutableListOf() }.add(tanques)
            }

            val meses = monthlyMap.toSortedMap().map { (mes, tanquesList) ->
                val totalTanques = tanquesList.sum()
                val litros = if (litrosPorTanque == 1000) {
                    // Recalcular con el factor solicitado
                    tanquesList.map { it * litrosPorTanque }.sum()
                } else {
                    totalTanques * litrosPorTanque
                }
                MonthlyGasData(
                    mes = mes,
                    tanques = totalTanques,
                    litros = litros,
                    co2Kg = litros * factorEmision
                )
            }

            val totalTanquesAnual = meses.sumOf { it.tanques }
            val totalLitrosAnual = meses.sumOf { it.litros }
            val co2AnualKg = totalLitrosAnual * factorEmision
            val co2AnualTon = co2AnualKg / 1000.0

            call.respond(CarbonFootprintSummary(
                totalRegistros = records.size,
                totalTanquesAnual = totalTanquesAnual,
                totalLitrosAnual = totalLitrosAnual,
                co2AnualKg = co2AnualKg,
                co2AnualTon = co2AnualTon,
                litrosPorTanque = litrosPorTanque,
                factorEmision = factorEmision,
                meses = meses
            ))
        }
    }

    // ============ PERSONAL - TALLAS ============
    route("/api/v1/rh/tallas") {
        get {
            val items = DatabaseFactory.dbQuery {
                PersonalTallaTable.selectAll().map {
                    PersonalTalla(
                        id = it[PersonalTallaTable.id],
                        numero = it[PersonalTallaTable.numero],
                        nombre = it[PersonalTallaTable.nombre],
                        tallaPlayera = it[PersonalTallaTable.tallaPlayera],
                        tallaZapatos = it[PersonalTallaTable.tallaZapatos],
                        notas = it[PersonalTallaTable.notas]
                    )
                }
            }
            call.respond(items)
        }
        post {
            val item = call.receive<PersonalTalla>()
            DatabaseFactory.dbQuery {
                PersonalTallaTable.insert {
                    it[numero] = item.numero
                    it[nombre] = item.nombre
                    it[tallaPlayera] = item.tallaPlayera
                    it[tallaZapatos] = item.tallaZapatos
                    it[notas] = item.notas
                }
            }
            call.respond(mapOf("status" to "ok"))
        }
        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
            DatabaseFactory.dbQuery { PersonalTallaTable.deleteWhere { PersonalTallaTable.id eq id } }
            call.respond(mapOf("status" to "ok"))
        }
    }
}
