package com.example.rhnaf.routes

import com.example.rhnaf.auth.Roles
import com.example.rhnaf.auth.requireRoleOr403
import com.example.rhnaf.database.DatabaseFactory
import com.example.rhnaf.database.EhsDocumentTable
import com.example.rhnaf.database.EnvironmentalWasteTable
import com.example.rhnaf.database.EmergencyDrillTable
import com.example.rhnaf.database.RiskMatrixTable
import com.example.rhnaf.database.SafetyIncidentTable
import com.example.rhnaf.database.SafetyInspectionTable
import com.example.rhnaf.database.SafetyTrainingTable
import com.example.rhnaf.service.DocumentReaderService
import com.example.rhnaf.service.GoogleDriveService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

private const val DRIVE_POINTER_PREFIX = "gdrive:"

/**
 * Crea registros estructurados a partir de evidencias que YA existen en
 * Google Drive / Supabase (las que la app muestra como "Evidencia sin
 * registro vinculado"). No descarga ni sube archivos nuevos: solo lee el
 * archivo que ya esta en Drive, intenta extraer una fila con el lector de
 * documentos existente y, si faltan datos obligatorios, usa como respaldo
 * los metadatos reales de la propia evidencia (fecha y titulo) para no
 * inventar informacion que el documento no contiene. El registro creado
 * queda vinculado automaticamente a la evidencia (moduleType/moduleRecordId).
 */
private data class CategorySpec(
    val moduleType: String,
    val table: Table,
    val requiresOnlyDate: Boolean,
    val insert: suspend (fecha: String, titulo: String, campos: Map<String, String>) -> Int,
)

private fun norm(s: String): String {
    val flat = buildString {
        s.forEach { c ->
            when (c) {
                'á' -> append('a'); 'é' -> append('e'); 'í' -> append('i'); 'ó' -> append('o'); 'ú' -> append('u'); 'ü' -> append('u'); 'ñ' -> append('n')
                'Á' -> append('a'); 'É' -> append('e'); 'Í' -> append('i'); 'Ó' -> append('o'); 'Ú' -> append('u'); 'Ü' -> append('u'); 'Ñ' -> append('n')
                else -> append(c)
            }
        }
    }
    return flat.lowercase().trim()
}

// Intenta mapear encabezados de una tabla extraida del documento a los campos
// del formulario. Devuelve el mejor valor por campo si el documento SI tiene
// una tabla reconocible; si no, el mapa queda vacio y se usan los respaldos.
private fun extractFieldsFromRows(rows: List<List<String>>, fieldHints: Map<String, List<String>>): Map<String, String> {
    if (rows.size < 2) return emptyMap()
    val header = rows.first().map { norm(it) }
    val dataRow = rows[1]
    val result = mutableMapOf<String, String>()
    fieldHints.forEach { (field, hints) ->
        val idx = header.indexOfFirst { h -> hints.any { hint -> h.contains(hint) } }
        if (idx in dataRow.indices && dataRow[idx].isNotBlank()) result[field] = dataRow[idx].trim()
    }
    return result
}

private val MESES = mapOf(
    "enero" to 1, "febrero" to 2, "marzo" to 3, "abril" to 4, "mayo" to 5, "junio" to 6,
    "julio" to 7, "agosto" to 8, "septiembre" to 9, "setiembre" to 9, "octubre" to 10,
    "noviembre" to 11, "diciembre" to 12
)

// La fecha del documento (dd/MM/yyyy) casi nunca esta escrita en el archivo,
// pero muchas evidencias de NAF se llaman "... <Mes>.pdf" (reportes
// mensuales). Si el titulo contiene un mes en español lo usamos junto con el
// año documental ya guardado; si no hay mes reconocible, caemos al 01/01/año.
// Nunca inventamos un año que la evidencia no tenga.
private fun bestEffortDate(fechaExistente: String, titulo: String, anio: Int): String? {
    if (fechaExistente.isNotBlank()) return fechaExistente
    if (anio <= 0) return null
    val mes = MESES.entries.firstOrNull { norm(titulo).contains(it.key) }?.value ?: 1
    return "%02d/%02d/%04d".format(1, mes, anio)
}

fun Route.ehsAutoRegisterRouting() {
    post("/api/v1/ehs/documentos/generar-registros") {
        requireRoleOr403(call, Roles.EHS_WRITE) ?: return@post
        val soloCategoria = call.request.queryParameters["categoria"]?.trim()

        val specs: Map<String, CategorySpec> = mapOf(
            "Inspeccion" to CategorySpec("inspection", SafetyInspectionTable, true) { fecha, titulo, campos ->
                DatabaseFactory.dbQuery {
                    SafetyInspectionTable.insert {
                        it[SafetyInspectionTable.fecha] = fecha
                        it[SafetyInspectionTable.tipoInspeccion] = campos["tipoInspeccion"] ?: titulo
                        it[SafetyInspectionTable.area] = campos["area"] ?: ""
                        it[SafetyInspectionTable.inspector] = campos["inspector"] ?: ""
                        it[SafetyInspectionTable.hallazgos] = campos["hallazgos"] ?: ""
                        it[SafetyInspectionTable.riesgo] = campos["riesgo"] ?: ""
                        it[SafetyInspectionTable.accionesCorrectivas] = campos["accionesCorrectivas"] ?: ""
                        it[SafetyInspectionTable.estado] = campos["estado"] ?: "Pendiente de revision"
                    } get SafetyInspectionTable.id
                }
            },
            "Incidente" to CategorySpec("incident", SafetyIncidentTable, true) { fecha, titulo, campos ->
                DatabaseFactory.dbQuery {
                    SafetyIncidentTable.insert {
                        it[SafetyIncidentTable.fecha] = fecha
                        it[SafetyIncidentTable.tipo] = campos["tipo"] ?: titulo
                        it[SafetyIncidentTable.severidad] = campos["severidad"] ?: ""
                        it[SafetyIncidentTable.descripcion] = campos["descripcion"] ?: ""
                        it[SafetyIncidentTable.estado] = campos["estado"] ?: "Pendiente de revision"
                    } get SafetyIncidentTable.id
                }
            },
            "Capacitacion" to CategorySpec("training", SafetyTrainingTable, true) { fecha, titulo, campos ->
                DatabaseFactory.dbQuery {
                    SafetyTrainingTable.insert {
                        it[SafetyTrainingTable.fecha] = fecha
                        it[SafetyTrainingTable.tema] = campos["tema"] ?: titulo
                        it[SafetyTrainingTable.instructor] = campos["instructor"] ?: ""
                        it[SafetyTrainingTable.asistentes] = campos["asistentes"] ?: ""
                        it[SafetyTrainingTable.estado] = campos["estado"] ?: "Pendiente de revision"
                    } get SafetyTrainingTable.id
                }
            },
            "Simulacro" to CategorySpec("drill", EmergencyDrillTable, true) { fecha, titulo, campos ->
                DatabaseFactory.dbQuery {
                    EmergencyDrillTable.insert {
                        it[EmergencyDrillTable.fecha] = fecha
                        it[EmergencyDrillTable.tipo] = campos["tipo"] ?: titulo
                        it[EmergencyDrillTable.participantes] = campos["participantes"] ?: ""
                        it[EmergencyDrillTable.resultado] = campos["resultado"] ?: ""
                        it[EmergencyDrillTable.estado] = campos["estado"] ?: "Pendiente de revision"
                    } get EmergencyDrillTable.id
                }
            },
            "Residuos" to CategorySpec("waste", EnvironmentalWasteTable, true) { fecha, titulo, campos ->
                DatabaseFactory.dbQuery {
                    EnvironmentalWasteTable.insert {
                        it[EnvironmentalWasteTable.fecha] = fecha
                        it[EnvironmentalWasteTable.residuo] = campos["residuo"] ?: titulo
                        it[EnvironmentalWasteTable.tipo] = campos["tipo"] ?: ""
                        it[EnvironmentalWasteTable.numeroManifiesto] = campos["numeroManifiesto"] ?: ""
                        it[EnvironmentalWasteTable.estado] = campos["estado"] ?: "Pendiente de revision"
                    } get EnvironmentalWasteTable.id
                }
            },
            "Riesgos" to CategorySpec("risk", RiskMatrixTable, false) { _, titulo, campos ->
                DatabaseFactory.dbQuery {
                    RiskMatrixTable.insert {
                        it[RiskMatrixTable.area] = campos["area"] ?: titulo
                        it[RiskMatrixTable.proceso] = campos["proceso"] ?: ""
                        it[RiskMatrixTable.riesgoIdentificado] = campos["riesgoIdentificado"] ?: ""
                        it[RiskMatrixTable.estado] = campos["estado"] ?: "Pendiente de revision"
                    } get RiskMatrixTable.id
                }
            },
        )

        val categoriasAProcesar = if (soloCategoria.isNullOrBlank()) specs.keys.toList() else listOf(soloCategoria)
        val creados = mutableListOf<Map<String, Any>>()
        val omitidos = mutableListOf<Map<String, Any>>()

        for (categoria in categoriasAProcesar) {
            val spec = specs[categoria]
            if (spec == null) {
                omitidos.add(mapOf("categoria" to categoria, "motivo" to "Categoría sin creación automática de registros"))
                continue
            }
            val pendientes = DatabaseFactory.dbQuery {
                EhsDocumentTable.selectAll().where {
                    (EhsDocumentTable.categoria eq categoria) and (EhsDocumentTable.moduleRecordId eq 0)
                }.toList()
            }
            for (row in pendientes) {
                val docId = row[EhsDocumentTable.id]
                val titulo = row[EhsDocumentTable.titulo]
                val fechaDoc = row[EhsDocumentTable.fecha]
                val anio = row[EhsDocumentTable.anio]
                val pointer = row[EhsDocumentTable.contentBase64]

                var camposExtraidos: Map<String, String> = emptyMap()
                if (pointer.startsWith(DRIVE_POINTER_PREFIX)) {
                    try {
                        val fileId = pointer.removePrefix(DRIVE_POINTER_PREFIX)
                        val bytes = GoogleDriveService.downloadFile(fileId)
                        if (bytes != null) {
                            val rows = DocumentReaderService.extractRows(bytes, row[EhsDocumentTable.fileName])
                            camposExtraidos = extractFieldsFromRows(rows, emptyMap())
                        }
                    } catch (e: Exception) {
                        application.log.warn("No se pudo leer evidencia #$docId para extracción: ${e.message}")
                    }
                }

                val fecha = bestEffortDate(fechaDoc, titulo, anio)
                if (spec.requiresOnlyDate && fecha == null) {
                    omitidos.add(mapOf("id" to docId, "titulo" to titulo, "motivo" to "Sin fecha ni año documental; complétalo manualmente"))
                    continue
                }

                val nuevoId = try {
                    spec.insert(fecha ?: "", titulo, camposExtraidos)
                } catch (e: Exception) {
                    omitidos.add(mapOf("id" to docId, "titulo" to titulo, "motivo" to "Error al crear el registro: ${e.message}"))
                    continue
                }

                DatabaseFactory.dbQuery {
                    EhsDocumentTable.update({ EhsDocumentTable.id eq docId }) {
                        it[EhsDocumentTable.moduleType] = spec.moduleType
                        it[EhsDocumentTable.moduleRecordId] = nuevoId
                    }
                }
                creados.add(mapOf("id" to docId, "titulo" to titulo, "categoria" to categoria, "moduleType" to spec.moduleType, "moduleRecordId" to nuevoId))
            }
        }

        call.respond(mapOf(
            "status" to "ok",
            "creados" to creados.size,
            "omitidos" to omitidos.size,
            "detalleCreados" to creados,
            "detalleOmitidos" to omitidos,
            "message" to "Los campos no encontrados en el propio documento se dejan vacíos para completarlos manualmente; no se inventa información."
        ))
    }
}
