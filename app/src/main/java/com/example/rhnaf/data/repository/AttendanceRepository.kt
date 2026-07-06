package com.example.rhnaf.data.repository

import com.example.rhnaf.data.local.AttendanceDao
import com.example.rhnaf.data.local.entities.AttendanceLogEntity
import kotlinx.coroutines.flow.Flow

class AttendanceRepository(private val attendanceDao: AttendanceDao) {
    suspend fun insertAttendanceLog(log: AttendanceLogEntity) {
        attendanceDao.insertAttendanceLog(log)
    }

    fun getAllAttendanceLogs(): Flow<List<AttendanceLogEntity>> {
        return attendanceDao.getAllAttendanceLogs()
    }

    fun getAttendanceLogsForEmployee(employeeId: String): Flow<List<AttendanceLogEntity>> {
        return attendanceDao.getAttendanceLogsForEmployee(employeeId)
    }
}
