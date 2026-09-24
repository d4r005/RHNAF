package com.example.rhnaf.routes

import com.example.rhnaf.database.DatabaseFactory
import com.example.rhnaf.database.EhsActionTable
import com.example.rhnaf.database.LegalMatrixTable
import com.example.rhnaf.database.LegalMatrixRefTable
import com.example.rhnaf.database.LegalMatrixDocTable
import com.example.rhnaf.shared.model.LegalMatrixItem
import com.example.rhnaf.shared.model.LegalMatrixSummary
import com.example.rhnaf.shared.model.CategoryCompliance
import com.example.rhnaf.shared.model.SubCategoryCompliance
import com.example.rhnaf.shared.model.LegalMatrixRef
import com.example.rhnaf.shared.model.LegalMatrixDoc
import com.example.rhnaf.shared.model.LegalMatrixDetalle
import com.example.rhnaf.shared.model.LegalMatrixTarea
import com.example.rhnaf.auth.Roles
import com.example.rhnaf.auth.requireRoleOr403
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import com.example.rhnaf.database.EhsDocumentTable
import org.jetbrains.exposed.sql.and
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
    Triple("PIPC", "Programa Interno de Protección Civil", "ProteccionCivil"),
    Triple("Dictamen de Riesgo", "Dictamen de riesgo de Protección Civil", "ProteccionCivil"),
    Triple("LAU", "Licencia Ambiental Única", "SEMARNAT"),
    Triple("Registro Generador RP", "Registro de Generador de Residuos Peligrosos", "SEMARNAT"),
    Triple("Manifiesto RP", "Manifiesto de entrega, transporte y recepción de residuos peligrosos", "PROFEPA")
)

@Serializable
private data class LegalMatrixPageResponse(
    val items: List<LegalMatrixItem> = emptyList(),
    val page: Int = 1,
    val pageSize: Int = 50,
    val total: Long = 0,
    val totalPages: Int = 0
)

private fun rowToLegalMatrixItem(row: org.jetbrains.exposed.sql.ResultRow): LegalMatrixItem = LegalMatrixItem(
    id = row[LegalMatrixTable.id],
    clave = row[LegalMatrixTable.clave],
    titulo = row[LegalMatrixTable.titulo],
    categoria = row[LegalMatrixTable.categoria],
    aplica = row[LegalMatrixTable.aplica],
    justificacion = row[LegalMatrixTable.justificacion],
    frecuenciaRevision = row[LegalMatrixTable.frecuenciaRevision],
    fechaEmision = row[LegalMatrixTable.fechaEmision],
    fechaVigencia = row[LegalMatrixTable.fechaVigencia],
    diasAlertaPrevia = row[LegalMatrixTable.diasAlertaPrevia],
    documentoUrl = row[LegalMatrixTable.documentoUrl],
    responsable = row[LegalMatrixTable.responsable],
    notas = row[LegalMatrixTable.notas],
    esCritico = row[LegalMatrixTable.esCritico],
    urlNorma = row[LegalMatrixTable.urlNorma],
    subCategoria = row[LegalMatrixTable.subCategoria],
    tipoObligacion = row[LegalMatrixTable.tipoObligacion],
    autoridad = row[LegalMatrixTable.autoridad],
    responsableEmail = row[LegalMatrixTable.responsableEmail]
)

/** Conteo de documentos por obligación; llamar dentro de una transacción (dbQuery). */
private fun conteoDocs(): Map<Int, Int> =
    LegalMatrixDocTable.selectAll().map { it[LegalMatrixDocTable.matrizId] }
        .groupingBy { it }.eachCount()

private fun refFrom(row: org.jetbrains.exposed.sql.ResultRow) = LegalMatrixRef(
    id = row[LegalMatrixRefTable.id],
    matrizId = row[LegalMatrixRefTable.matrizId],
    nivel = row[LegalMatrixRefTable.nivel],
    referencia = row[LegalMatrixRefTable.referencia],
    nombreLey = row[LegalMatrixRefTable.nombreLey],
    url = row[LegalMatrixRefTable.url],
    creadoPor = row[LegalMatrixRefTable.creadoPor]
)

private fun docFrom(row: org.jetbrains.exposed.sql.ResultRow) = LegalMatrixDoc(
    id = row[LegalMatrixDocTable.id],
    matrizId = row[LegalMatrixDocTable.matrizId],
    documentId = row[LegalMatrixDocTable.documentId],
    nombre = row[LegalMatrixDocTable.nombre],
    tipoDocumento = row[LegalMatrixDocTable.tipoDocumento],
    fechaExpedicion = row[LegalMatrixDocTable.fechaExpedicion],
    fechaVigencia = row[LegalMatrixDocTable.fechaVigencia],
    recordatorioDias = row[LegalMatrixDocTable.recordatorioDias],
    comentario = row[LegalMatrixDocTable.comentario],
    subidoPor = row[LegalMatrixDocTable.subidoPor],
    subidoFecha = row[LegalMatrixDocTable.subidoFecha]
)

private fun tareaFrom(row: org.jetbrains.exposed.sql.ResultRow) = LegalMatrixTarea(
    id = row[EhsActionTable.id],
    titulo = row[EhsActionTable.titulo],
    responsable = row[EhsActionTable.responsable],
    fechaLimite = row[EhsActionTable.fechaLimite],
    prioridad = row[EhsActionTable.prioridad],
    estado = row[EhsActionTable.estado]
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

                var items = DatabaseFactory.dbQuery {
                    val q = if (categoriaFiltro != null)
                        LegalMatrixTable.selectAll().where { LegalMatrixTable.categoria eq categoriaFiltro }
                    else
                        LegalMatrixTable.selectAll()
                    val raw = q.toList()
                    val conteos = conteoDocs()
                    raw.map { row ->
                        val base = rowToLegalMatrixItem(row)
                        base.copy(estado = calcularEstado(base, hoy), nDocumentos = conteos[base.id] ?: 0)
                    }
                }

                if (estadoFiltro != null) items = items.filter { it.estado == estadoFiltro }

                val page = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
                val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull()?.coerceIn(1, 500) ?: 50
                val total = items.size.toLong()
                val totalPages = if (items.isEmpty()) 0 else ((items.size + pageSize - 1) / pageSize)
                val paged = items.drop((page - 1) * pageSize).take(pageSize)

                call.respond(
                    LegalMatrixPageResponse(
                        items = paged,
                        page = page,
                        pageSize = pageSize,
                        total = total,
                        totalPages = totalPages
                    )
                )
            }
        }

        // Dashboard: % cumplimiento global, por categoría, y próximos a vencer
        get("/dashboard") {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.ALL) ?: return@safeApiCall
                val hoy = LocalDate.now()
                val items = DatabaseFactory.dbQuery {
                    LegalMatrixTable.selectAll().toList().map { row ->
                        val base = rowToLegalMatrixItem(row)
                        base.copy(estado = calcularEstado(base, hoy))
                    }
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
                    // Desglose por subcategoria (estilo EHSoft): % de cumplimiento
                    // por grupo interno (incendios, residuos, EPP, etc.).
                    val subCats = apCat.groupBy { it.subCategoria.ifBlank { "Sin clasificar" } }
                        .map { (sub, subList) ->
                            val vigSub = subList.count { it.estado == "Vigente" || it.estado == "PorVencer" }
                            SubCategoryCompliance(
                                subCategoria = sub,
                                aplicables = subList.size,
                                vigentes = vigSub,
                                porcentaje = if (subList.isNotEmpty()) (vigSub.toDouble() / subList.size.toDouble()) * 100.0 else 0.0
                            )
                        }.sortedByDescending { it.aplicables }
                    CategoryCompliance(
                        categoria = cat,
                        aplicables = apCat.size,
                        vigentes = vigCat,
                        porcentaje = if (apCat.isNotEmpty()) (vigCat.toDouble() / apCat.size.toDouble()) * 100.0 else 0.0,
                        subCategorias = subCats
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

        // CREAR obligación nueva (legislación estatal/municipal, permisos locales,
        // requisitos propios del centro de trabajo) - estilo EHSoft "agregar requisito"
        post {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.EHS_WRITE) ?: return@safeApiCall
                val item = call.receive<LegalMatrixItem>()
                val problem = when {
                    item.clave.isBlank() || item.clave.length > 100 -> "La clave de la obligación es obligatoria (máx. 100)"
                    item.titulo.isBlank() || item.titulo.length > 400 -> "El título es obligatorio (máx. 400)"
                    item.categoria !in setOf("STPS", "SEMARNAT", "PROFEPA", "ProteccionCivil", "Estatal", "Municipal") -> "Categoría inválida"
                    item.aplica !in setOf("Si", "No", "Pendiente") -> "Aplicabilidad inválida"
                    item.aplica != "Pendiente" && (item.justificacion.isBlank() || item.responsable.isBlank()) -> "Se requiere justificación y responsable"
                    item.fechaVigencia.isNotBlank() && runCatching { LocalDate.parse(item.fechaVigencia) }.isFailure -> "Fecha de vigencia inválida"
                    item.urlNorma.isNotBlank() && !item.urlNorma.startsWith("https://") -> "URL de norma debe ser HTTPS"
                    item.responsableEmail.isNotBlank() && !item.responsableEmail.contains("@") -> "Correo de responsable inválido"
                    else -> null
                }
                if (problem != null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to problem))
                    return@safeApiCall
                }
                val duplicada = DatabaseFactory.dbQuery {
                    LegalMatrixTable.selectAll().where { LegalMatrixTable.clave eq item.clave.trim() }.any()
                }
                if (duplicada) {
                    call.respond(HttpStatusCode.Conflict, mapOf("error" to "Ya existe una obligación con la clave ${item.clave.trim()}"))
                    return@safeApiCall
                }
                DatabaseFactory.dbQuery {
                    LegalMatrixTable.insert {
                        it[clave] = item.clave.trim()
                        it[titulo] = item.titulo.trim()
                        it[categoria] = item.categoria
                        it[aplica] = item.aplica
                        it[justificacion] = item.justificacion.trim()
                        it[frecuenciaRevision] = item.frecuenciaRevision
                        it[fechaEmision] = item.fechaEmision
                        it[fechaVigencia] = item.fechaVigencia
                        it[diasAlertaPrevia] = item.diasAlertaPrevia
                        it[documentoUrl] = item.documentoUrl
                        it[responsable] = item.responsable.trim()
                        it[notas] = item.notas
                        it[esCritico] = item.esCritico
                        it[urlNorma] = item.urlNorma
                        it[subCategoria] = item.subCategoria.trim()
                        it[tipoObligacion] = item.tipoObligacion.trim()
                        it[autoridad] = item.autoridad.trim()
                        it[responsableEmail] = item.responsableEmail.trim()
                    }
                }
                call.respond(HttpStatusCode.Created, mapOf("status" to "ok"))
            }
        }

        // FICHA COMPLETA de una obligación: marco legal, documentos de
        // cumplimiento y tareas/acciones vinculadas (estilo EHSoft).
        get("/{id}/detalle") {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.ALL) ?: return@safeApiCall
                val id = call.parameters["id"]?.toIntOrNull() ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                val hoy = LocalDate.now()
                val detalle = DatabaseFactory.dbQuery {
                    val row = LegalMatrixTable.selectAll().where { LegalMatrixTable.id eq id }.singleOrNull()
                        ?: return@dbQuery null
                    val base = rowToLegalMatrixItem(row)
                    val obligacion = base.copy(estado = calcularEstado(base, hoy))
                    val refs = LegalMatrixRefTable.selectAll().where { LegalMatrixRefTable.matrizId eq id }.map(::refFrom)
                    val docs = LegalMatrixDocTable.selectAll().where { LegalMatrixDocTable.matrizId eq id }.map(::docFrom)
                    val tareas = EhsActionTable.selectAll()
                        .where { (EhsActionTable.origenTipo eq "matriz_legal") and (EhsActionTable.origenId eq id) }
                        .map(::tareaFrom)
                    LegalMatrixDetalle(obligacion, refs, docs, tareas)
                }
                if (detalle == null) call.respond(HttpStatusCode.NotFound, mapOf("error" to "Obligación no encontrada"))
                else call.respond(detalle)
            }
        }

        // ---- MARCO LEGAL (referencias por artículo: federal/estatal/municipal) ----
        post("/{id}/referencias") {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.EHS_WRITE) ?: return@safeApiCall
                val id = call.parameters["id"]?.toIntOrNull() ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                val ref = call.receive<LegalMatrixRef>()
                val problem = when {
                    ref.nivel !in setOf("Federal", "Estatal", "Municipal") -> "Nivel inválido (Federal, Estatal o Municipal)"
                    ref.nombreLey.isBlank() || ref.nombreLey.length > 300 -> "Nombre de la ley obligatorio (máx. 300)"
                    ref.referencia.length > 200 || ref.url.length > 500 -> "Campo demasiado largo"
                    ref.url.isNotBlank() && !ref.url.startsWith("http") -> "La URL debe iniciar con http"
                    else -> null
                }
                if (problem != null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to problem))
                    return@safeApiCall
                }
                val existe = DatabaseFactory.dbQuery { LegalMatrixTable.selectAll().where { LegalMatrixTable.id eq id }.any() }
                if (!existe) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Obligación no encontrada"))
                    return@safeApiCall
                }
                val creadoPor = call.request.header(HttpHeaders.Authorization)?.removePrefix("Bearer ")?.trim().orEmpty()
                DatabaseFactory.dbQuery {
                    LegalMatrixRefTable.insert {
                        it[matrizId] = id
                        it[nivel] = ref.nivel
                        it[referencia] = ref.referencia.trim()
                        it[nombreLey] = ref.nombreLey.trim()
                        it[url] = ref.url.trim()
                        it[LegalMatrixRefTable.creadoPor] = creadoPor
                    }
                }
                call.respond(HttpStatusCode.Created, mapOf("status" to "ok"))
            }
        }

        delete("/{id}/referencias/{refId}") {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.EHS_WRITE) ?: return@safeApiCall
                val refId = call.parameters["refId"]?.toIntOrNull() ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                DatabaseFactory.dbQuery {
                    LegalMatrixRefTable.deleteWhere { LegalMatrixRefTable.id eq refId }
                }
                call.respond(mapOf("status" to "ok"))
            }
        }

        // ---- DOCUMENTOS DE CUMPLIMIENTO (varios por obligación con histórico) ----
        // Vincula una evidencia ya subida (ehs_documents/Drive) con sus metadatos
        // normativos: expedición, vigencia y recordatorio configurables por archivo.
        post("/{id}/documentos") {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.EHS_WRITE) ?: return@safeApiCall
                val id = call.parameters["id"]?.toIntOrNull() ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                val doc = call.receive<LegalMatrixDoc>()
                val problem = when {
                    doc.documentId <= 0 -> "Primero sube el archivo con el botón de evidencia y usa el ID resultante"
                    doc.nombre.isBlank() -> "Nombre del documento obligatorio"
                    doc.nombre.length > 300 || doc.comentario.length > 500 -> "Campo demasiado largo"
                    doc.fechaExpedicion.isNotBlank() && runCatching { LocalDate.parse(doc.fechaExpedicion) }.isFailure -> "Fecha de expedición inválida (AAAA-MM-DD)"
                    doc.fechaVigencia.isNotBlank() && runCatching { LocalDate.parse(doc.fechaVigencia) }.isFailure -> "Fecha de vigencia inválida (AAAA-MM-DD)"
                    doc.recordatorioDias !in 0..365 -> "Recordatorio debe estar entre 0 y 365 días"
                    else -> null
                }
                if (problem != null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to problem))
                    return@safeApiCall
                }
                val ok = DatabaseFactory.dbQuery {
                    val existeMatriz = LegalMatrixTable.selectAll().where { LegalMatrixTable.id eq id }.any()
                    val existeDoc = EhsDocumentTable.selectAll().where { EhsDocumentTable.id eq doc.documentId }.any()
                    existeMatriz && existeDoc
                }
                if (!ok) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Obligación o documento no encontrado"))
                    return@safeApiCall
                }
                val subidoPor = call.request.header(HttpHeaders.Authorization)?.removePrefix("Bearer ")?.trim().orEmpty()
                DatabaseFactory.dbQuery {
                    LegalMatrixDocTable.insert {
                        it[matrizId] = id
                        it[documentId] = doc.documentId
                        it[nombre] = doc.nombre.trim()
                        it[tipoDocumento] = doc.tipoDocumento.trim()
                        it[fechaExpedicion] = doc.fechaExpedicion
                        it[fechaVigencia] = doc.fechaVigencia
                        it[recordatorioDias] = doc.recordatorioDias
                        it[comentario] = doc.comentario.trim()
                        it[LegalMatrixDocTable.subidoPor] = subidoPor
                        it[subidoFecha] = LocalDate.now().toString()
                    }
                    // Actualizar la vigencia de la obligación con la del documento
                    // más reciente si la obligación no tiene fecha asignada.
                    val vigenciaActual = LegalMatrixTable.selectAll().where { LegalMatrixTable.id eq id }.first()[LegalMatrixTable.fechaVigencia]
                    if (vigenciaActual.isBlank() && doc.fechaVigencia.isNotBlank()) {
                        LegalMatrixTable.update({ LegalMatrixTable.id eq id }) { it[fechaVigencia] = doc.fechaVigencia }
                    }
                }
                call.respond(HttpStatusCode.Created, mapOf("status" to "ok"))
            }
        }

        delete("/{id}/documentos/{docId}") {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.EHS_WRITE) ?: return@safeApiCall
                val docId = call.parameters["docId"]?.toIntOrNull() ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                // Solo desvincula el documento de la obligación; la evidencia
                // física (ehs_documents/Drive) se conserva intacta.
                DatabaseFactory.dbQuery {
                    LegalMatrixDocTable.deleteWhere { LegalMatrixDocTable.id eq docId }
                }
                call.respond(mapOf("status" to "ok"))
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


        // Actualizar (cuestionario de aplicabilidad, vigencia, evidencia, etc.)
        put("/{id}") {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.EHS_WRITE) ?: return@safeApiCall
                val id = call.parameters["id"]?.toIntOrNull() ?: return@safeApiCall call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID inválido"))
                val item = call.receive<LegalMatrixItem>()
                val problem = when {
                    item.id != id -> "ID de obligación no coincide"
                    item.aplica !in setOf("Si", "No", "Pendiente") -> "Aplicabilidad inválida"
                    item.aplica != "Pendiente" && (item.justificacion.isBlank() || item.responsable.isBlank()) -> "Se requiere justificación y responsable"
                    item.fechaVigencia.isNotBlank() && runCatching { LocalDate.parse(item.fechaVigencia) }.isFailure -> "Fecha de evidencia inválida"
                    item.documentoUrl.isNotBlank() && !item.documentoUrl.startsWith("https://") -> "Evidencia debe ser URL HTTPS"
                    item.urlNorma.isNotBlank() && !item.urlNorma.startsWith("https://") -> "URL de norma debe ser HTTPS"
                    item.responsableEmail.isNotBlank() && !item.responsableEmail.contains("@") -> "Correo de responsable inválido"
                    item.justificacion.length > 500 || item.responsable.length > 200 || item.documentoUrl.length > 500 || item.urlNorma.length > 500
                        || item.subCategoria.length > 100 || item.tipoObligacion.length > 60 || item.autoridad.length > 200 || item.responsableEmail.length > 200 -> "Campo demasiado largo"
                    else -> null
                }
                if (problem != null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to problem))
                    return@safeApiCall
                }
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
                        it[esCritico] = item.esCritico
                        it[urlNorma] = item.urlNorma
                        it[subCategoria] = item.subCategoria.trim()
                        it[tipoObligacion] = item.tipoObligacion.trim()
                        it[autoridad] = item.autoridad.trim()
                        it[responsableEmail] = item.responsableEmail.trim()
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
