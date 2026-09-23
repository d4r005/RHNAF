package com.example.rhnaf.routes

import com.example.rhnaf.auth.Roles
import com.example.rhnaf.auth.requireRoleOr403
import com.example.rhnaf.database.*
import com.example.rhnaf.service.GoogleDriveService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.selectAll

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

            suspend fun deleteWithCount(fileId: String) {
                if (GoogleDriveService.deleteFile(fileId)) deletedIds.add(fileId) else driveFailed++
            }

            // 2) Todo el árbol de la carpeta de evidencia (archivos y subcarpetas).
            suspend fun purgeFolder(folderId: String) {
                val entries = GoogleDriveService.listFilesInFolder(folderId) ?: return
                for ((id, mime) in entries) {
                    if (mime == FOLDER_MIME) {
                        purgeFolder(id)
                        if (GoogleDriveService.deleteFile(id)) foldersDeleted++
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
                    "documentos" to EhsDocumentTable.deleteAll(),
                    "inspecciones" to SafetyInspectionTable.deleteAll(),
                    "incidentes" to SafetyIncidentTable.deleteAll(),
                    "permisos_trabajo" to WorkPermitTable.deleteAll(),
                    "entregas_epp" to PpeDeliveryTable.deleteAll(),
                    "capacitaciones" to SafetyTrainingTable.deleteAll(),
                    "simulacros" to EmergencyDrillTable.deleteAll(),
                    "matriz_riesgos" to RiskMatrixTable.deleteAll(),
                    "residuos" to EnvironmentalWasteTable.deleteAll(),
                    "salud_ocupacional" to OccupationalHealthTable.deleteAll(),
                    "quimicos" to ChemicalInventoryTable.deleteAll(),
                    "matriz_legal" to LegalMatrixTable.deleteAll(),
                    "planes_accion" to EhsActionTable.deleteAll(),
                    "contratistas" to EhsContractorTable.deleteAll(),
                    "tasas" to EhsRatePeriodTable.deleteAll(),
                    "consumo_gas" to GasConsumoTable.deleteAll()
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
