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
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addTrainingRecord(courseName: String, type: String) {
        viewModelScope.launch {
            repository.insertTraining(
                TrainingEntity(
                    employeeId = "ALL",
                    courseName = courseName,
                    date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()),
                    type = type
                )
            )
        }
    }

    fun importExcelData(fileName: String) {
        addTrainingRecord("Importación masiva: $fileName", "Historial")
    }
}
