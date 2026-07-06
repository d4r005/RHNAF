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

    @Query("SELECT COUNT(*) FROM training_records")
    fun getTotalCoursesCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM training_records WHERE progress = 100")
    fun getCompletedCoursesCount(): Flow<Int>

    @Query("SELECT AVG(progress) FROM training_records")
    fun getAverageProgress(): Flow<Double?>
    
    @Query("SELECT * FROM training_records ORDER BY dueDate DESC LIMIT 5")
    fun getRecentTrainingActivities(): Flow<List<TrainingEntity>>
}
