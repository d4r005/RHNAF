package com.example.rhnaf.data.repository

import com.example.rhnaf.data.local.PerformanceDao
import com.example.rhnaf.data.local.entities.PerformanceEntity
import kotlinx.coroutines.flow.Flow

class PerformanceRepository(private val performanceDao: PerformanceDao) {
    fun getEvaluations(employeeId: String): Flow<List<PerformanceEntity>> = 
        performanceDao.getEvaluationsByEmployee(employeeId)

    suspend fun insertEvaluation(evaluation: PerformanceEntity) = 
        performanceDao.insertEvaluation(evaluation)
}
