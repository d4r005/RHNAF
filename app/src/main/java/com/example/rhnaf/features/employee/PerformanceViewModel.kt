package com.example.rhnaf.features.employee

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rhnaf.data.local.entities.PerformanceEntity
import com.example.rhnaf.data.repository.PerformanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class PerformanceViewModel(private val repository: PerformanceRepository) : ViewModel() {
    fun getEvaluations(employeeId: String): Flow<List<PerformanceEntity>> = 
        repository.getEvaluations(employeeId)

    fun addEvaluation(employeeId: String, score: Float, feedback: String) {
        viewModelScope.launch {
            repository.insertEvaluation(
                PerformanceEntity(
                    employeeId = employeeId,
                    evaluatorId = "Supervisor_1",
                    date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()),
                    score = score,
                    competencies = "Liderazgo:5,Calidad:4,Seguridad:5",
                    feedback = feedback
                )
            )
        }
    }
}
