package com.example.rhnaf.features.employee

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rhnaf.data.local.entities.EquipmentEntity
import com.example.rhnaf.data.repository.EquipmentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class EquipmentViewModel(private val repository: EquipmentRepository) : ViewModel() {
    fun getEquipment(employeeId: String): Flow<List<EquipmentEntity>> = 
        repository.getEquipmentByEmployee(employeeId)

    fun assignEquipment(employeeId: String, type: String, description: String) {
        viewModelScope.launch {
            repository.insertEquipment(
                EquipmentEntity(
                    employeeId = employeeId,
                    itemType = type,
                    description = description,
                    deliveryDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()),
                    status = "Nuevo"
                )
            )
        }
    }
}
