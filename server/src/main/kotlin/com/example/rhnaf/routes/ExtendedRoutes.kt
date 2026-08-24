package com.example.rhnaf.routes

import com.example.rhnaf.database.*
import com.example.rhnaf.shared.model.*
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
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.update

fun Route.extendedRouting() {

    // ============ FERRETERIA ============
    route("/api/v1/ferreteria") {
        get {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.ALL) ?: return@safeApiCall
                val baseQuery = FerreteriaTable.selectAll()
                val result = pagedQuery(call, baseQuery) {
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
                call.respond(result)
            }
        }
        post {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.WAREHOUSE_WRITE) ?: return@safeApiCall
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
        }
        put("/{id}") {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.WAREHOUSE_WRITE) ?: return@safeApiCall
                val id = call.parameters["id"]?.toIntOrNull() ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                val item = call.receive<FerreteriaItem>()
                DatabaseFactory.dbQuery {
                    FerreteriaTable.update({ FerreteriaTable.id eq id }) {
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
        }
        delete("/{id}") {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.WAREHOUSE_WRITE) ?: return@safeApiCall
                val id = call.parameters["id"]?.toIntOrNull() ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                DatabaseFactory.dbQuery { FerreteriaTable.deleteWhere { FerreteriaTable.id eq id } }
                call.respond(mapOf("status" to "ok"))
            }
        }
    }

    // ============ RECEPCION DE MATERIAS PRIMAS ============
    route("/api/v1/recepcion-mp") {
        get {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.ALL) ?: return@safeApiCall
                val tipo = call.request.queryParameters["tipo"]
                val baseQuery = if (tipo != null)
                    RecepcionMPTable.selectAll().where { RecepcionMPTable.tipo eq tipo }
                else
                    RecepcionMPTable.selectAll()
                val result = pagedQuery(call, baseQuery) {
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
                call.respond(result)
            }
        }
        post {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.PROCUREMENT_WRITE) ?: return@safeApiCall
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
        }
        delete("/{id}") {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.PROCUREMENT_WRITE) ?: return@safeApiCall
                val id = call.parameters["id"]?.toIntOrNull() ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                DatabaseFactory.dbQuery { RecepcionMPTable.deleteWhere { RecepcionMPTable.id eq id } }
                call.respond(mapOf("status" to "ok"))
            }
        }
    }

    // ============ TARIMAS ============
    route("/api/v1/almacen/tarimas") {
        get {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.ALL) ?: return@safeApiCall
                val result = pagedQuery(call, TarimaTable.selectAll()) {
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
                call.respond(result)
            }
        }
        post {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.WAREHOUSE_WRITE) ?: return@safeApiCall
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
        }
        delete("/{id}") {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.WAREHOUSE_WRITE) ?: return@safeApiCall
                val id = call.parameters["id"]?.toIntOrNull() ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                DatabaseFactory.dbQuery { TarimaTable.deleteWhere { TarimaTable.id eq id } }
                call.respond(mapOf("status" to "ok"))
            }
        }
    }

    // ============ CONTENEDORES DE CHINA ============
    route("/api/v1/embarques/contenedores-china") {
        get {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.ALL) ?: return@safeApiCall
                val result = pagedQuery(call, ContenedorChinaTable.selectAll()) {
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
                call.respond(result)
            }
        }
        post {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.SHIPPING_WRITE) ?: return@safeApiCall
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
        }
        delete("/{id}") {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.SHIPPING_WRITE) ?: return@safeApiCall
                val id = call.parameters["id"]?.toIntOrNull() ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                DatabaseFactory.dbQuery { ContenedorChinaTable.deleteWhere { ContenedorChinaTable.id eq id } }
                call.respond(mapOf("status" to "ok"))
            }
        }
    }

    // ============ SELLOS EN STOCK ============
    route("/api/v1/almacen/sellos") {
        get {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.ALL) ?: return@safeApiCall
                val result = pagedQuery(call, SelloStockTable.selectAll()) {
                    SelloStock(
                        id = it[SelloStockTable.id],
                        fecha = it[SelloStockTable.fecha],
                        numeroInicial = it[SelloStockTable.numeroInicial],
                        numeroFinal = it[SelloStockTable.numeroFinal],
                        cantidad = it[SelloStockTable.cantidad]
                    )
                }
                call.respond(result)
            }
        }
        post {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.WAREHOUSE_WRITE) ?: return@safeApiCall
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
        }
        delete("/{id}") {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.WAREHOUSE_WRITE) ?: return@safeApiCall
                val id = call.parameters["id"]?.toIntOrNull() ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                DatabaseFactory.dbQuery { SelloStockTable.deleteWhere { SelloStockTable.id eq id } }
                call.respond(mapOf("status" to "ok"))
            }
        }
    }

    // ============ BITACORA DE GAS ============
    route("/api/v1/ehs/gas") {
        get {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.ALL) ?: return@safeApiCall
                val anio = call.request.queryParameters["anio"]
                val baseQuery = if (anio != null)
                    GasConsumoTable.selectAll().where { GasConsumoTable.fecha like "%$anio%" }
                else
                    GasConsumoTable.selectAll()
                val result = pagedQuery(call, baseQuery.orderBy(GasConsumoTable.id, SortOrder.DESC)) {
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
                call.respond(result)
            }
        }
        post {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.EHS_WRITE) ?: return@safeApiCall
                val item = call.receive<GasConsumo>()
                val tanques = item.numeroTanques
                val litrosTanque = item.litrosPorTanque
                val litrosTotalesCalc = tanques * litrosTanque
                DatabaseFactory.dbQuery {
                    GasConsumoTable.insert {
                        it[fecha] = item.fecha
                        it[cantidadTexto] = item.cantidadTexto
                        it[numeroTanques] = tanques
                        it[litrosPorTanque] = litrosTanque
                        it[litrosTotales] = litrosTotalesCalc
                        it[diaCarga] = item.diaCarga
                    }
                }
                call.respond(mapOf("status" to "ok"))
            }
        }
        delete("/{id}") {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.EHS_WRITE) ?: return@safeApiCall
                val id = call.parameters["id"]?.toIntOrNull() ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                DatabaseFactory.dbQuery { GasConsumoTable.deleteWhere { GasConsumoTable.id eq id } }
                call.respond(mapOf("status" to "ok"))
            }
        }
    }

    // ============ HUELLA DE CARBONO ============
    get("/api/v1/ehs/gas/huella-carbono") {
        safeApiCall(call) {
            requireRoleOr403(call, Roles.ALL) ?: return@safeApiCall
            val litrosPorTanqueParam = call.request.queryParameters["litrosPorTanque"]?.toDoubleOrNull() ?: 1000.0
            val factorEmisionParam = call.request.queryParameters["factorEmision"]?.toDoubleOrNull() ?: 1.51
            val anio = call.request.queryParameters["anio"]

            val registros = DatabaseFactory.dbQuery {
                val query = if (anio != null)
                    GasConsumoTable.selectAll().where { GasConsumoTable.fecha like "%$anio%" }
                else
                    GasConsumoTable.selectAll()
                query.toList()
            }

            val totalTanques = registros.sumOf { it[GasConsumoTable.numeroTanques] ?: 0 }
            val totalLitros = (totalTanques * litrosPorTanqueParam).toInt()
            val co2Kg = totalLitros * factorEmisionParam
            val co2Ton = co2Kg / 1000.0

            // Agregación mensual
            val mesesMap = mutableMapOf<String, MonthlyGasData>()
            registros.forEach { row ->
                val fecha = row[GasConsumoTable.fecha] ?: ""
                val mes = if (fecha.length >= 7) fecha.substring(0, 7) else "desconocido"
                val tanques = row[GasConsumoTable.numeroTanques] ?: 0
                val litros = (tanques * litrosPorTanqueParam).toInt()
                val co2 = litros * factorEmisionParam
                val existing = mesesMap.getOrPut(mes) { MonthlyGasData(mes, 0, 0, 0.0) }
                mesesMap[mes] = existing.copy(
                    tanques = existing.tanques + tanques,
                    litros = existing.litros + litros,
                    co2Kg = existing.co2Kg + co2
                )
            }
            val meses = mesesMap.values.sortedBy { it.mes }

            val resumen = CarbonFootprintSummary(
                totalTanquesAnual = totalTanques,
                totalLitrosAnual = totalLitros,
                co2AnualKg = co2Kg,
                co2AnualTon = co2Ton,
                factorEmision = factorEmisionParam,
                litrosPorTanque = litrosPorTanqueParam.toInt(),
                meses = meses
            )
            call.respond(resumen)
        }
    }

    // ============ PERSONAL TALLAS ============
    route("/api/v1/rh/tallas") {
        get {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.ALL) ?: return@safeApiCall
                val result = pagedQuery(call, PersonalTallaTable.selectAll()) {
                    PersonalTalla(
                        id = it[PersonalTallaTable.id],
                        numero = it[PersonalTallaTable.numero],
                        nombre = it[PersonalTallaTable.nombre],
                        tallaPlayera = it[PersonalTallaTable.tallaPlayera],
                        tallaZapatos = it[PersonalTallaTable.tallaZapatos],
                        notas = it[PersonalTallaTable.notas]
                    )
                }
                call.respond(result)
            }
        }
        post {
            safeApiCall(call) {
                requireRoleOr403(call, setOf(Roles.ADMIN, Roles.RH)) ?: return@safeApiCall
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
        }
        delete("/{id}") {
            safeApiCall(call) {
                requireRoleOr403(call, setOf(Roles.ADMIN, Roles.RH)) ?: return@safeApiCall
                val id = call.parameters["id"]?.toIntOrNull() ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                DatabaseFactory.dbQuery { PersonalTallaTable.deleteWhere { PersonalTallaTable.id eq id } }
                call.respond(mapOf("status" to "ok"))
            }
        }
    }
}
