package com.example.rhnaf.data.local

import androidx.room.*
import com.example.rhnaf.data.local.entities.TrainingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrainingDao {
    @Query("SELECT * FROM training_records WHERE employeeId = :employeeId")
    fun getTrainingByEmployee(employeeId: String): Flow<List<TrainingEntity>>

    @Query("SELECT * FROM training_records")
    fun getAllTraining(): Flow<List<TrainingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTraining(training: TrainingEntity)
}
