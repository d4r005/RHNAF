package com.example.rhnaf.data.repository

import com.example.rhnaf.data.local.EquipmentDao
import com.example.rhnaf.data.local.entities.EquipmentEntity
import kotlinx.coroutines.flow.Flow

class EquipmentRepository(private val equipmentDao: EquipmentDao) {
    fun getEquipmentByEmployee(employeeId: String): Flow<List<EquipmentEntity>> = 
        equipmentDao.getEquipmentByEmployee(employeeId)

    suspend fun insertEquipment(equipment: EquipmentEntity) {
        equipmentDao.insertEquipment(equipment)
    }
}
