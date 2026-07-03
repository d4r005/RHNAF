package com.example.rhnaf.data.repository

import com.example.rhnaf.data.local.TrainingDao
import com.example.rhnaf.data.local.entities.TrainingEntity
import kotlinx.coroutines.flow.Flow

class TrainingRepository(private val trainingDao: TrainingDao) {
    fun getAllTraining(): Flow<List<TrainingEntity>> = trainingDao.getAllTraining()

    suspend fun insertTraining(training: TrainingEntity) {
        trainingDao.insertTraining(training)
    }
}
