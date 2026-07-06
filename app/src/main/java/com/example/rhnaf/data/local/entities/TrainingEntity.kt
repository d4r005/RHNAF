package com.example.rhnaf.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "training_records")
data class TrainingEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val employeeId: String,
    val courseName: String,
    val date: String,
    val dueDate: String? = null,
    val type: String, // CTPAT, Safety, Technical
    val isCompleted: Boolean = false,
    val progress: Int = 0, // 0 to 100
    val score: Int? = null
)
