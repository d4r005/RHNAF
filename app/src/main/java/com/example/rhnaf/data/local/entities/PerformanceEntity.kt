package com.example.rhnaf.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "performance_evaluations")
data class PerformanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val employeeId: String,
    val evaluatorId: String,
    val date: String,
    val score: Float, // 0 to 5
    val competencies: String, // Comma separated: Quality:4,Leadership:5...
    val feedback: String
)
