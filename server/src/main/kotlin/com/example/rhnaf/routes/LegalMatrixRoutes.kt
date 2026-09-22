package com.example.rhnaf.routes

import com.example.rhnaf.database.DatabaseFactory
import com.example.rhnaf.database.LegalMatrixTable
import com.example.rhnaf.shared.model.LegalMatrixItem
import com.example.rhnaf.shared.model.LegalMatrixSummary
import com.example.rhnaf.shared.model.CategoryCompliance
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * EHS - Matriz Legal Dinámica
 * Catálogo de obligaciones normativas (NOMs STPS, SEMARNAT, PROFEPA, Protección Civil)
 * con cálculo automático de estado de vigencia y % de cumplimiento, inspirado en
 * EHSoft (matriz legal precargada + alertas) y ACM Suite (cuestionario de aplicabilidad + dashboards).
 */

private fun parseFecha(s: String): LocalDate? {
    if (s.isBlank()) return null
    return try { LocalDate.parse(s.trim().substring(0, 10)) } catch (e: Exception) { null }
}

/** Calcula el estado dinámico de una obligación: Vigente, PorVencer, Vencido, NoAplica, Pendiente */
private fun calcularEstado(item: LegalMatrixItem, hoy: LocalDate): String {
    if (item.aplica == "No") return "NoAplica"
    if (item.aplica == "Pendiente") return "Pendiente"
    val vigencia = parseFecha(item.fechaVigencia) ?: return "Pendiente"
    return when {
        vigencia.isBefore(hoy) -> "Vencido"
        !vigencia.isAfter(hoy.plusDays(item.diasAlertaPrevia.toLong())) -> "PorVencer"
        else -> "Vigente"
    }
}

/** Lista de NOMs y obligaciones precargadas para el diagnóstico inicial de aplicabilidad */
private val NOM_SEED: List<Triple<String, String, String>> = listOf(
    Triple("NOM-001-STPS-2008", "Edificios, locales, instalaciones y áreas en los centros de trabajo", "STPS"),
    Triple("NOM-002-STPS-2010", "Condiciones de seguridad - Prevención y protección contra incendios", "STPS"),
    Triple("NOM-004-STPS-1999", "Sistemas de protección y dispositivos de seguridad en maquinaria y equipo", "STPS"),
    Triple("NOM-005-STPS-1998", "Manejo, transporte y almacenamiento de sustancias químicas peligrosas", "STPS"),
    Triple("NOM-006-STPS-2014", "Manejo y almacenamiento de materiales", "STPS"),
    Triple("NOM-009-STPS-2011", "Condiciones de seguridad para realizar trabajos en altura", "STPS"),
    Triple("NOM-010-STPS-2014", "Agentes químicos contaminantes del ambiente laboral", "STPS"),
    Triple("NOM-011-STPS-2001", "Condiciones de seguridad e higiene - ruido", "STPS"),
    Triple("NOM-015-STPS-2001", "Condiciones térmicas elevadas o abatidas en los centros de trabajo", "STPS"),
    Triple("NOM-017-STPS-2008", "Equipo de protección personal (EPP) para los trabajadores", "STPS"),
    Triple("NOM-019-STPS-2011", "Constitución y funcionamiento de las comisiones de seguridad e higiene", "STPS"),
    Triple("NOM-020-STPS-2011", "Recipientes sujetos a presión, recipientes criogénicos y generadores de vapor", "STPS"),
    Triple("NOM-022-STPS-2015", "Electricidad estática en los centros de trabajo", "STPS"),
    Triple("NOM-025-STPS-2008", "Condiciones de iluminación en los centros de trabajo", "STPS"),
    Triple("NOM-026-STPS-2008", "Colores y señales de seguridad e higiene", "STPS"),
    Triple("NOM-028-STPS-2012", "Sistema para la administración del trabajo - Seguridad en procesos con sustancias químicas", "STPS"),
    Triple("NOM-029-STPS-2011", "Mantenimiento de las instalaciones eléctricas en los centros de trabajo", "STPS"),
    Triple("NOM-030-STPS-2009", "Servicios preventivos de seguridad y salud en el trabajo", "STPS"),
    Triple("NOM-035-STPS-2018", "Factores de riesgo psicosocial en el trabajo", "STPS"),
    Triple("PIPC", "Programa Interno de Protección Civil", "ProteccionCivil"),
    Triple("Dictamen de Riesgo", "Dictamen de riesgo de Protección Civil", "ProteccionCivil"),
    Triple("LAU", "Licencia Ambiental Única", "SEMARNAT"),
    Triple("Registro Generador RP", "Registro de Generador de Residuos Peligrosos", "SEMARNAT"),
    Triple("Manifiesto RP", "Manifiesto de entrega, transporte y recepción de residuos peligrosos", "PROFEPA")
)

fun Route.legalMatrixRouting() {
    route("/api/v1/ehs/matriz-legal") {

        // Listar (con estado calculado dinámicamente), filtros opcionales por categoria/estado
        get {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.ALL) ?: return@safeApiCall
                val hoy = LocalDate.now()
                val categoriaFiltro = call.request.queryParameters["categoria"]
                val estadoFiltro = call.request.queryParameters["estado"]

                val rows = DatabaseFactory.dbQuery {
                    val q = if (categoriaFiltro != null)
                        LegalMatrixTable.selectAll().where { LegalMatrixTable.categoria eq categoriaFiltro }
                    else
                        LegalMatrixTable.selectAll()
                    q.toList()
                }

                var items = rows.map {
                    val base = LegalMatrixItem(
                        id = it[LegalMatrixTable.id],
                        clave = it[LegalMatrixTable.clave],
                        titulo = it[LegalMatrixTable.titulo],
                        categoria = it[LegalMatrixTable.categoria],
                        aplica = it[LegalMatrixTable.aplica],
                        justificacion = it[LegalMatrixTable.justificacion],
                        frecuenciaRevision = it[LegalMatrixTable.frecuenciaRevision],
                        fechaEmision = it[LegalMatrixTable.fechaEmision],
                        fechaVigencia = it[LegalMatrixTable.fechaVigencia],
                        diasAlertaPrevia = it[LegalMatrixTable.diasAlertaPrevia],
                        documentoUrl = it[LegalMatrixTable.documentoUrl],
                        responsable = it[LegalMatrixTable.responsable],
                        notas = it[LegalMatrixTable.notas]
                    )
                    base.copy(estado = calcularEstado(base, hoy))
                }

                if (estadoFiltro != null) items = items.filter { it.estado == estadoFiltro }

                call.respond(pagedList(call, items))
            }
        }

        // Dashboard: % cumplimiento global, por categoría, y próximos a vencer
        get("/dashboard") {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.ALL) ?: return@safeApiCall
                val hoy = LocalDate.now()
                val rows = DatabaseFactory.dbQuery { LegalMatrixTable.selectAll().toList() }
                val items = rows.map {
                    val base = LegalMatrixItem(
                        id = it[LegalMatrixTable.id],
                        clave = it[LegalMatrixTable.clave],
                        titulo = it[LegalMatrixTable.titulo],
                        categoria = it[LegalMatrixTable.categoria],
                        aplica = it[LegalMatrixTable.aplica],
                        justificacion = it[LegalMatrixTable.justificacion],
                        frecuenciaRevision = it[LegalMatrixTable.frecuenciaRevision],
                        fechaEmision = it[LegalMatrixTable.fechaEmision],
                        fechaVigencia = it[LegalMatrixTable.fechaVigencia],
                        diasAlertaPrevia = it[LegalMatrixTable.diasAlertaPrevia],
                        documentoUrl = it[LegalMatrixTable.documentoUrl],
                        responsable = it[LegalMatrixTable.responsable],
                        notas = it[LegalMatrixTable.notas]
                    )
                    base.copy(estado = calcularEstado(base, hoy))
                }

                val aplicables = items.filter { it.aplica == "Si" }
                val vigentes = aplicables.count { it.estado == "Vigente" }
                val porVencer = aplicables.count { it.estado == "PorVencer" }
                val vencidos = aplicables.count { it.estado == "Vencido" }
                val pendientes = aplicables.count { it.estado == "Pendiente" }
                val noAplica = items.count { it.aplica == "No" }
                val cumplimiento = if (aplicables.isNotEmpty())
                    ((vigentes + porVencer).toDouble() / aplicables.size.toDouble()) * 100.0
                else 0.0

                val porCategoria = items.groupBy { it.categoria }.map { (cat, list) ->
                    val apCat = list.filter { it.aplica == "Si" }
                    val vigCat = apCat.count { it.estado == "Vigente" || it.estado == "PorVencer" }
                    CategoryCompliance(
                        categoria = cat,
                        aplicables = apCat.size,
                        vigentes = vigCat,
                        porcentaje = if (apCat.isNotEmpty()) (vigCat.toDouble() / apCat.size.toDouble()) * 100.0 else 0.0
                    )
                }.sortedBy { it.categoria }

                val proximos = aplicables.filter { it.estado == "PorVencer" || it.estado == "Vencido" }
                    .sortedBy { it.fechaVigencia }
                    .take(10)

                call.respond(
                    LegalMatrixSummary(
                        totalObligaciones = items.size,
                        aplicables = aplicables.size,
                        vigentes = vigentes,
                        porVencer = porVencer,
                        vencidos = vencidos,
                        noAplica = noAplica,
                        pendientes = pendientes,
                        porcentajeCumplimiento = cumplimiento,
                        porCategoria = porCategoria,
                        proximosAVencer = proximos
                    )
                )
            }
        }

        // Precargar catálogo de NOMs (solo si la tabla está vacía) - estilo EHSoft "matriz precargada"
        post("/seed") {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.EHS_WRITE) ?: return@safeApiCall
                val count = DatabaseFactory.dbQuery { LegalMatrixTable.selectAll().count() }
                if (count > 0) {
                    call.respond(mapOf("status" to "skipped", "message" to "La matriz legal ya tiene registros ($count)"))
                    return@safeApiCall
                }
                DatabaseFactory.dbQuery {
                    NOM_SEED.forEach { (clave, titulo, categoria) ->
                        LegalMatrixTable.insert {
                            it[LegalMatrixTable.clave] = clave
                            it[LegalMatrixTable.titulo] = titulo
                            it[LegalMatrixTable.categoria] = categoria
                            it[LegalMatrixTable.aplica] = "Pendiente"
                            it[LegalMatrixTable.frecuenciaRevision] = "Anual"
                            it[LegalMatrixTable.diasAlertaPrevia] = 30
                        }
                    }
                }
                call.respond(mapOf("status" to "ok", "message" to "Se precargaron ${NOM_SEED.size} obligaciones normativas"))
            }
        }

        // Crear obligación (manual, adicional al catálogo precargado)
        post {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.EHS_WRITE) ?: return@safeApiCall
                val item = call.receive<LegalMatrixItem>()
                DatabaseFactory.dbQuery {
                    LegalMatrixTable.insert {
                        it[clave] = item.clave
                        it[titulo] = item.titulo
                        it[categoria] = item.categoria
                        it[aplica] = item.aplica
                        it[justificacion] = item.justificacion
                        it[frecuenciaRevision] = item.frecuenciaRevision
                        it[fechaEmision] = item.fechaEmision
                        it[fechaVigencia] = item.fechaVigencia
                        it[diasAlertaPrevia] = item.diasAlertaPrevia
                        it[documentoUrl] = item.documentoUrl
                        it[responsable] = item.responsable
                        it[notas] = item.notas
                    }
                }
                call.respond(mapOf("status" to "ok"))
            }
        }

        // Actualizar (cuestionario de aplicabilidad, vigencia, evidencia, etc.)
        put("/{id}") {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.EHS_WRITE) ?: return@safeApiCall
                val id = call.parameters["id"]?.toIntOrNull() ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                val item = call.receive<LegalMatrixItem>()
                DatabaseFactory.dbQuery {
                    LegalMatrixTable.update({ LegalMatrixTable.id eq id }) {
                        it[clave] = item.clave
                        it[titulo] = item.titulo
                        it[categoria] = item.categoria
                        it[aplica] = item.aplica
                        it[justificacion] = item.justificacion
                        it[frecuenciaRevision] = item.frecuenciaRevision
                        it[fechaEmision] = item.fechaEmision
                        it[fechaVigencia] = item.fechaVigencia
                        it[diasAlertaPrevia] = item.diasAlertaPrevia
                        it[documentoUrl] = item.documentoUrl
                        it[responsable] = item.responsable
                        it[notas] = item.notas
                    }
                }
                call.respond(mapOf("status" to "ok"))
            }
        }

        delete("/{id}") {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.EHS_WRITE) ?: return@safeApiCall
                val id = call.parameters["id"]?.toIntOrNull() ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                DatabaseFactory.dbQuery { LegalMatrixTable.deleteWhere { LegalMatrixTable.id eq id } }
                call.respond(mapOf("status" to "ok"))
            }
        }
    }
}
