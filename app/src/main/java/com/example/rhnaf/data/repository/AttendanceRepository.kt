package com.example.rhnaf.data.repository

import com.example.rhnaf.data.NetworkConfig
import com.example.rhnaf.data.local.AttendanceDao
import com.example.rhnaf.data.local.entities.AttendanceLogEntity
import com.example.rhnaf.data.local.entities.AttendanceType
import com.example.rhnaf.domain.model.AttendanceLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import java.util.Date

class AttendanceRepository(
    private val attendanceDao: AttendanceDao,
    private val httpClient: HttpClient
) {

    // Trae las checadas procesadas desde TU servidor Ktor central (Hugging Face),
    // NO de la lectora directamente. Esto es lo que refleja la asistencia real
    // de todo el personal, venga de la lectora facial o de captura manual.
    fun observeAttendanceLogs(): Flow<List<AttendanceLog>> = flow {
        try {
            val logs: List<AttendanceLog> =
                httpClient.get("${NetworkConfig.BASE_URL}/api/v1/asistencia/logs").body()
            emit(logs)
        } catch (e: Exception) {
            emit(emptyList()) // Manejo de error de red (sin conexión, servidor caído, etc.)
        }
    }

    fun getLocalAttendanceLogs(): Flow<List<AttendanceLogEntity>> = attendanceDao.getAllAttendanceLogs()

    suspend fun registerCheck(employeeId: String, type: String, latitude: Double = 0.0, longitude: Double = 0.0) {
        val attendanceType = if (type == "ENTRADA") AttendanceType.CLOCK_IN else AttendanceType.CLOCK_OUT
        val newLog = AttendanceLogEntity(
            employeeId = employeeId,
            type = attendanceType,
            timestamp = Date().time,
            latitude = latitude,
            longitude = longitude
        )
        attendanceDao.insertAttendanceLog(newLog)

        // Reflejamos la checada manual también en el servidor central, para que
        // se vea en el dashboard/reportes igual que las checadas de la lectora.
        try {
            httpClient.post("${NetworkConfig.BASE_URL}/hikvision") {
                contentType(ContentType.Application.Json)
                setBody(
                    """
                    {
                      "dateTime": "${Date().time}",
                      "deviceID": "APP-MANUAL",
                      "AccessControllerEvent": {
                        "employeeNoString": "$employeeId",
                        "currentVerifyMode": "manual-app"
                      }
                    }
                    """.trimIndent()
                )
            }
        } catch (e: Exception) {
            // Si no hay internet, el registro local ya quedó guardado; no bloqueamos al usuario.
        }
    }

    suspend fun determineNextCheckType(employeeId: String): String {
        val lastLog = attendanceDao.getLastLogForEmployee(employeeId)
        return if (lastLog == null || lastLog.type == AttendanceType.CLOCK_OUT) {
            "ENTRADA"
        } else {
            "SALIDA"
        }
    }

    suspend fun insertAttendanceLog(log: AttendanceLogEntity) {
        attendanceDao.insertAttendanceLog(log)
    }

    suspend fun getLastLogForEmployee(employeeId: String): AttendanceLogEntity? {
        return attendanceDao.getLastLogForEmployee(employeeId)
    }
}
