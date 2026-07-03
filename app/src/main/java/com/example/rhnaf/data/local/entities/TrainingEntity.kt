package com.example.rhnaf.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "training_records")
data class TrainingEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val employeeId: String,
    val courseName: String,
    val date: String,
    val type: String, // CTPAT, Safety, Technical
    val isCompleted: Boolean = false,
    val score: Int? = null
)
