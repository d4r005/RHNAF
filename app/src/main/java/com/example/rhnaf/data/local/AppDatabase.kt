package com.example.rhnaf.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.rhnaf.data.local.entities.EmployeeEntity
import com.example.rhnaf.data.local.entities.IncidentEntity
import com.example.rhnaf.data.local.entities.EquipmentEntity
import com.example.rhnaf.data.local.entities.TrainingEntity
import com.example.rhnaf.data.local.entities.PerformanceEntity

@Database(entities = [EmployeeEntity::class, IncidentEntity::class, EquipmentEntity::class, TrainingEntity::class, PerformanceEntity::class], version = 5, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun employeeDao(): EmployeeDao
    abstract fun incidentDao(): IncidentDao
    abstract fun equipmentDao(): EquipmentDao
    abstract fun trainingDao(): TrainingDao
    abstract fun performanceDao(): PerformanceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rhnaf_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
