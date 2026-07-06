package com.example.rhnaf.features.training.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rhnaf.data.repository.TrainingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TrainingDashboardViewModel(
    private val repository: TrainingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrainingDashboardState())
    val uiState: StateFlow<TrainingDashboardState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            try {
                combine(
                    repository.getTotalCourses(),
                    repository.getCompletedCourses(),
                    repository.getAverageProgress(),
                    repository.getRecentActivities()
                ) { total, completed, avgProgress, activities ->
                    TrainingDashboardState(
                        isLoading = false,
                        totalCourses = total,
                        completedCourses = completed,
                        averageProgress = (avgProgress ?: 0.0).toFloat(),
                        recentActivities = activities
                    )
                }.collect { updatedState ->
                    _uiState.value = updatedState
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
            }
        }
    }
}
