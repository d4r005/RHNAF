package com.example.rhnaf.routes

import com.example.rhnaf.database.DatabaseFactory
import com.example.rhnaf.database.EmployeeTable
import com.example.rhnaf.shared.model.EmployeeStatus
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val lenientJson = Json { ignoreUnknownKeys = true; isLenient = true }

// Una fila de empleado tal como la manda employee_sync.py despues de leerla
// de la lectora Hikvision via ISAPI (UserInfo/Search + foto de rostro).
@Serializable
data class EmployeeSyncRow(
    val employeeNo: String,
    val name: String? = null,
    val department: String? = null,
    val position: String? = null,
    // JPEG en base64 SIN el prefijo "data:image/jpeg;base64,"
    val photoBase64: String? = null
)

@Serializable
data class EmployeeSyncResult(
    val total: Int,
    val creados: Int,
    val actualizados: Int,
    val fotosGuardadas: Int,
    val mensaje: String
)

/**
 * Separa un nombre completo en (firstName, lastName).
 * La lectora Hikvision manda el nombre como "NOMBRE APELLIDO" (ej. "Dario Robles"
 * o "Miguel Angel Chavez"), asi que la PRIMERA palabra(s) es el nombre de pila
 * y el resto son apellidos. El resultado se muestra como "firstName lastName"
 * (ej. "Dario Robles") en el modulo de Empleados y Asistencia.
 */
private fun splitName(fullName: String): Pair<String, String> {
    val trimmed = fullName.trim()
    if (trimmed.isBlank()) return "" to ""
    val parts = trimmed.split(" ").filter { it.isNotBlank() }
    val firstName = parts.firstOrNull() ?: ""
    val lastName = parts.drop(1).joinToString(" ")
    return firstName to lastName
}

fun Route.employeeSyncRouting() {
    route("/api/v1/empleados") {

        // Devuelve cuantos empleados ya tienen foto vs cuantos faltan, para saber
        // que tan completa esta la sincronizacion con la lectora.
        get("/estado-fotos") {
            val (conFoto, sinFoto) = DatabaseFactory.dbQuery {
                val total = EmployeeTable.selectAll().count()
                val conFotoCount = EmployeeTable.selectAll().where {
                    EmployeeTable.photoUrl.isNotNull()
                }.count()
                conFotoCount to (total - conFotoCount)
            }
            call.respond(mapOf("con_foto" to conFoto.toString(), "sin_foto" to sinFoto.toString()))
        }

        // Endpoint que llama employee_sync.py (corrido en la red local de la planta):
        // recibe TODOS los usuarios/empleados dados de alta en la lectora Hikvision,
        // junto con su foto de rostro si la lectora la expone via ISAPI, y los da de
        // alta o actualiza en la ficha de empleado (EmployeeTable). Esto llena los
        // huecos que se ven en Asistencia cuando un employeeNo no tiene nombre porque
        // nunca se habia dado de alta manualmente en el sistema.
        post("/sync-device") {
            val rawBody = call.receiveText()
            val rows = runCatching {
                lenientJson.decodeFromString<List<EmployeeSyncRow>>(rawBody)
            }.getOrElse {
                call.respond(
                    EmployeeSyncResult(0, 0, 0, 0, "JSON invalido, se esperaba una lista de empleados.")
                )
                return@post
            }

            if (rows.isEmpty()) {
                call.respond(EmployeeSyncResult(0, 0, 0, 0, "No se recibieron empleados."))
                return@post
            }

            val today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            var creados = 0
            var actualizados = 0
            var fotosGuardadas = 0

            DatabaseFactory.dbQuery {
                val distinctRows = rows.filter { it.employeeNo.isNotBlank() }
                    .distinctBy { it.employeeNo }

                val ids = distinctRows.map { it.employeeNo }
                val existingIds = EmployeeTable
                    .select(EmployeeTable.id)
                    .where { EmployeeTable.id inList ids }
                    .map { it[EmployeeTable.id] }
                    .toHashSet()

                for (row in distinctRows) {
                    val photoDataUri = row.photoBase64
                        ?.takeIf { it.isNotBlank() }
                        ?.let { b64 -> if (b64.startsWith("data:")) b64 else "data:image/jpeg;base64,$b64" }

                    if (row.employeeNo in existingIds) {
                        // Ya existe: solo pisamos los campos que la lectora SI trae
                        // (no queremos borrar RFC/CURP/NSS/salario ya capturados a mano).
                        EmployeeTable.update({ EmployeeTable.id eq row.employeeNo }) {
                            if (!row.name.isNullOrBlank()) {
                                val (fn, ln) = splitName(row.name)
                                it[firstName] = fn
                                it[lastName] = ln
                            }
                            if (!row.department.isNullOrBlank()) it[department] = row.department
                            if (!row.position.isNullOrBlank()) it[position] = row.position
                            if (photoDataUri != null) it[photoUrl] = photoDataUri
                            it[readerId] = row.employeeNo
                        }
                        actualizados++
                        if (photoDataUri != null) fotosGuardadas++
                    } else {
                        // No existe todavia: lo damos de alta con lo que tengamos.
                        val (fn, ln) = splitName(row.name ?: "")
                        EmployeeTable.insert {
                            it[id] = row.employeeNo
                            it[firstName] = fn.ifBlank { row.employeeNo }
                            it[lastName] = ln
                            it[position] = row.position?.ifBlank { "Operador General" } ?: "Operador General"
                            it[department] = row.department?.ifBlank { "General" } ?: "General"
                            it[entryDate] = today
                            it[status] = EmployeeStatus.ACTIVE
                            it[readerId] = row.employeeNo
                            it[photoUrl] = photoDataUri
                        }
                        creados++
                        if (photoDataUri != null) fotosGuardadas++
                    }
                }
            }

            call.respond(
                EmployeeSyncResult(
                    total = rows.size,
                    creados = creados,
                    actualizados = actualizados,
                    fotosGuardadas = fotosGuardadas,
                    mensaje = "Sincronizacion completa: $creados nuevos, $actualizados actualizados, $fotosGuardadas con foto."
                )
            )
        }
    }
}
