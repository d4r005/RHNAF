package com.example.rhnaf.data.local

import androidx.room.TypeConverter
import com.example.rhnaf.shared.model.EmployeeStatus

class Converters {
    @TypeConverter
    fun fromStatus(status: EmployeeStatus): String {
        return status.name
    }

    @TypeConverter
    fun toStatus(value: String): EmployeeStatus {
        return EmployeeStatus.valueOf(value)
    }

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return value.joinToString(",")
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return if (value.isEmpty()) emptyList() else value.split(",")
    }
}
