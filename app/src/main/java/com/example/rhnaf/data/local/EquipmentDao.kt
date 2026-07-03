package com.example.rhnaf.data.local

import androidx.room.*
import com.example.rhnaf.data.local.entities.EquipmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EquipmentDao {
    @Query("SELECT * FROM equipment_delivery WHERE employeeId = :employeeId")
    fun getEquipmentByEmployee(employeeId: String): Flow<List<EquipmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEquipment(equipment: EquipmentEntity)
}
