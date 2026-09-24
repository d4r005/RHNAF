package com.example.rhnaf.routes

import com.example.rhnaf.auth.Roles
import com.example.rhnaf.auth.requireRoleOr403
import com.example.rhnaf.database.*
import com.example.rhnaf.service.GoogleDriveService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*

private const val DRIVE_POINTER_PREFIX = "gdrive:"
private const val FOLDER_MIME = "application/vnd.google-apps.folder"

@Serializable
data class EhsPurgeResult(
    val driveFilesDeleted: Int,
    val driveFilesFailed: Int,
    val driveFoldersDeleted: Int,
    val rowsDeleted: Map<String, Int>
)

/**
 * Purga administrativa: elimina TODA la evidencia de Google Drive (archivos
 * creados por la app en la carpeta configurada, con o sin registro en la BD)
 * y todos los registros EHS de la plataforma, porque el dueño recargará la
 * información manualmente. Requiere rol ADMIN y ?confirm=ELIMINAR.
 * No toca usuarios, empleados, nómina, almacén ni logs de auditoría de acceso.
 */
fun Route.ehsPurgeRouting() {
    delete("/api/v1/ehs/purge") {
        safeApiCall(call) {
            requireRoleOr403(call, setOf(Roles.ADMIN)) ?: return@safeApiCall
            if (call.request.queryParameters["confirm"] != "ELIMINAR") {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Confirmación requerida: usa ?confirm=ELIMINAR"))
                return@safeApiCall
            }
            if (!GoogleDriveService.isConfigured()) {
                call.respond(HttpStatusCode.ServiceUnavailable, mapOf("error" to "Google Drive no está configurado; solo se pueden borrar los registros"))
                return@safeApiCall
            }

            // 1) IDs de Drive rastreados en la BD (incluye años y carpetas previas).
            val knownIds = DatabaseFactory.dbQuery {
                EhsDocumentTable.selectAll().mapNotNull { row ->
                    val stored = row[EhsDocumentTable.contentBase64]
                    if (stored.startsWith(DRIVE_POINTER_PREFIX)) stored.removePrefix(DRIVE_POINTER_PREFIX).takeIf { it.isNotBlank() } else null
                }
            }.toSet()

            val deletedIds = HashSet<String>()
            var driveFailed = 0
            var foldersDeleted = 0

            // Un solo archivo atascado en Drive no debe abortar la purga completa:
            // límite de 20 s por llamada y un reintento con espera.
            suspend fun tryDelete(fileId: String): Boolean =
                runCatching { withTimeout(20_000) { GoogleDriveService.deleteFile(fileId) } }.getOrDefault(false)

            suspend fun deleteWithCount(fileId: String) {
                if (tryDelete(fileId)) { deletedIds.add(fileId); return }
                delay(300)
                if (tryDelete(fileId)) deletedIds.add(fileId) else driveFailed++
            }

            // 2) Todo el árbol de la carpeta de evidencia (archivos y subcarpetas).
            suspend fun purgeFolder(folderId: String) {
                val entries = GoogleDriveService.listFilesInFolder(folderId) ?: return
                for ((id, mime) in entries) {
                    if (mime == FOLDER_MIME) {
                        purgeFolder(id)
                        if (tryDelete(id)) foldersDeleted++
                    } else {
                        deleteWithCount(id)
                    }
                }
            }
            GoogleDriveService.folderId?.let { purgeFolder(it) }

            // 3) Apuntadores conocidos que no estaban en el árbol (movidos de carpeta).
            knownIds.forEach { id -> if (id !in deletedIds) deleteWithCount(id) }

            // 4) Registros EHS en la plataforma.
            val rows = DatabaseFactory.dbQuery {
                linkedMapOf(
                    "documentos" to EhsDocumentTable.deleteWhere { Op.TRUE },
                    "inspecciones" to SafetyInspectionTable.deleteWhere { Op.TRUE },
                    "incidentes" to SafetyIncidentTable.deleteWhere { Op.TRUE },
                    "permisos_trabajo" to WorkPermitTable.deleteWhere { Op.TRUE },
                    "entregas_epp" to PpeDeliveryTable.deleteWhere { Op.TRUE },
                    "capacitaciones" to SafetyTrainingTable.deleteWhere { Op.TRUE },
                    "simulacros" to EmergencyDrillTable.deleteWhere { Op.TRUE },
                    "matriz_riesgos" to RiskMatrixTable.deleteWhere { Op.TRUE },
                    "residuos" to EnvironmentalWasteTable.deleteWhere { Op.TRUE },
                    "salud_ocupacional" to OccupationalHealthTable.deleteWhere { Op.TRUE },
                    "quimicos" to ChemicalInventoryTable.deleteWhere { Op.TRUE },
                    "matriz_legal" to LegalMatrixTable.deleteWhere { Op.TRUE },
                    "planes_accion" to EhsActionTable.deleteWhere { Op.TRUE },
                    "contratistas" to EhsContractorTable.deleteWhere { Op.TRUE },
                    "tasas" to EhsRatePeriodTable.deleteWhere { Op.TRUE },
                    "consumo_gas" to GasConsumoTable.deleteWhere { Op.TRUE },
                    "dc3_constancias" to Dc3ConstanciaTable.deleteWhere { Op.TRUE },
                    "eventos_calendario" to EhsCustomEventTable.deleteWhere { Op.TRUE },
                    "checklists" to EhsChecklistTable.deleteWhere { Op.TRUE },
                    "checklist_items" to EhsChecklistItemTable.deleteWhere { Op.TRUE }
                )
            }
            call.respond(EhsPurgeResult(
                driveFilesDeleted = deletedIds.size,
                driveFilesFailed = driveFailed,
                driveFoldersDeleted = foldersDeleted,
                rowsDeleted = rows
            ))
        }
    }
}
