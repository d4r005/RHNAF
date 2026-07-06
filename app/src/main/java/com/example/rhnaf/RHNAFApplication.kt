package com.example.rhnaf

import android.app.Application
import com.example.rhnaf.data.local.AppDatabase
import com.example.rhnaf.data.repository.EmployeeRepository
import com.example.rhnaf.data.repository.SafetyRepository
import com.example.rhnaf.data.repository.EquipmentRepository
import com.example.rhnaf.data.repository.TrainingRepository
import com.example.rhnaf.data.repository.PerformanceRepository
import com.example.rhnaf.data.repository.AttendanceRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class RHNAFApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    
    val httpClient by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                    isLenient = true
                })
            }
        }
    }

    val employeeRepository by lazy { EmployeeRepository(database.employeeDao()) }
    val safetyRepository by lazy { SafetyRepository(database.incidentDao()) }
    val equipmentRepository by lazy { EquipmentRepository(database.equipmentDao()) }
    val trainingRepository by lazy { TrainingRepository(database.trainingDao()) }
    val performanceRepository by lazy { PerformanceRepository(database.performanceDao()) }
    val attendanceRepository by lazy { AttendanceRepository(database.attendanceDao(), httpClient) }
}
