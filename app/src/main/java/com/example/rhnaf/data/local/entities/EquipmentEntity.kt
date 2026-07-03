package com.example.rhnaf.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "equipment_delivery")
data class EquipmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val employeeId: String,
    val itemType: String, // Uniform, Boots, Helmet, Laptop, Tool
    val description: String,
    val deliveryDate: String,
    val status: String // New, Used, Returned
)
