package com.example.rhnaf.features.training.dashboard

import com.example.rhnaf.data.local.entities.TrainingEntity

data class TrainingDashboardState(
    val isLoading: Boolean = true,
    val totalCourses: Int = 0,
    val completedCourses: Int = 0,
    val averageProgress: Float = 0f,
    val recentActivities: List<TrainingEntity> = emptyList(),
    val errorMessage: String? = null
)
