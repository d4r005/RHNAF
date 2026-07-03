package com.example.rhnaf.data.local

import androidx.room.*
import com.example.rhnaf.data.local.entities.PerformanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PerformanceDao {
    @Query("SELECT * FROM performance_evaluations WHERE employeeId = :employeeId ORDER BY date DESC")
    fun getEvaluationsByEmployee(employeeId: String): Flow<List<PerformanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvaluation(evaluation: PerformanceEntity)
}
