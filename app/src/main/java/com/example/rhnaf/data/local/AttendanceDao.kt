package com.example.rhnaf.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.rhnaf.data.local.entities.AttendanceLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceLog(log: AttendanceLogEntity)

    // Obtener los registros de asistencia ordenados por fecha/hora descendente
    @Query("SELECT * FROM attendance_logs ORDER BY timestamp DESC")
    fun getAllAttendanceLogs(): Flow<List<AttendanceLogEntity>>

    // Obtener la última checada de un empleado específico para determinar el estado actual (Entrada o Salida)
    @Query("SELECT * FROM attendance_logs WHERE employeeId = :employeeId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastLogForEmployee(employeeId: String): AttendanceLogEntity?
}
