package com.example.rhnaf.data.repository

import com.example.rhnaf.data.local.AttendanceDao
import com.example.rhnaf.data.local.entities.AttendanceLogEntity
import com.example.rhnaf.data.local.entities.AttendanceType
import com.example.rhnaf.domain.model.AttendanceLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import java.util.Date

class AttendanceRepository(
    private val attendanceDao: AttendanceDao,
    private val httpClient: HttpClient
) {

    // Trae las checadas procesadas desde TU servidor Ktor central, NO de la lectora
    fun observeAttendanceLogs(): Flow<List<AttendanceLog>> = flow {
        try {
            // Replace with your actual server URL
            val logs: List<AttendanceLog> = httpClient.get("http://10.0.2.2:8080/api/v1/asistencia/logs").body()
            emit(logs)
        } catch (e: Exception) {
            emit(emptyList()) // Manejo de error de red local
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
