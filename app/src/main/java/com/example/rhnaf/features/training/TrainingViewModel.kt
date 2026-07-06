package com.example.rhnaf.features.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rhnaf.data.local.entities.TrainingEntity
import com.example.rhnaf.data.repository.TrainingRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TrainingViewModel(private val repository: TrainingRepository) : ViewModel() {

    val allTraining: StateFlow<List<TrainingEntity>> = repository.getAllTraining()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addTrainingRecord(courseName: String, type: String) {
        viewModelScope.launch {
            repository.insertTraining(
                TrainingEntity(
                    employeeId = "ALL",
                    courseName = courseName,
                    date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()),
                    dueDate = "2026-12-31",
                    type = type,
                    progress = 0
                )
            )
        }
    }

    fun importExcelData(fileName: String) {
        viewModelScope.launch {
            val names = arrayOf(
                "Seguridad en equipos industriales motorizados",
                "Reconocimiento de peligros y Riesgos",
                "Preparación y respuesta de emergencias",
                "Comunicación de peligros (SGA) / Sustancias Químicas",
                "Trabajos en caliente / Estrés por calor",
                "Equipo de Protección Personal (EPP)",
                "Seguridad Eléctrica"
            )
            val types = arrayOf(
                "Presencial", "Presencial", "Presencial", "Presencial", "Presencial", "Presencial", "Pendiente"
            )
            
            for (i in names.indices) {
                repository.insertTraining(
                    TrainingEntity(
                        employeeId = "ALL",
                        courseName = names[i],
                        date = "Plan 2026",
                        dueDate = "2026-12-31",
                        type = types[i],
                        progress = if (i < 3) 100 else 0,
                        isCompleted = i < 3
                    )
                )
            }
        }
    }
}
