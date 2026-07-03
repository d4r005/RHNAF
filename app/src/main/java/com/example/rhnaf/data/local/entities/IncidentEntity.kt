package com.example.rhnaf.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "incidents")
data class IncidentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val description: String,
    val date: String,
    val severity: String,
    val area: String,
    val reportedBy: String
)
