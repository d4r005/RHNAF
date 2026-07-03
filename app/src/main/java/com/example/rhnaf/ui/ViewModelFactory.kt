package com.example.rhnaf.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.rhnaf.RHNAFApplication
import com.example.rhnaf.features.employee.EmployeeViewModel
import com.example.rhnaf.features.employee.EquipmentViewModel
import com.example.rhnaf.features.safety.SafetyViewModel
import com.example.rhnaf.features.employee.PerformanceViewModel
import com.example.rhnaf.features.training.TrainingViewModel

val ViewModelFactory = object : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as RHNAFApplication
        
        return when {
            modelClass.isAssignableFrom(EmployeeViewModel::class.java) -> {
                EmployeeViewModel(application.employeeRepository) as T
            }
            modelClass.isAssignableFrom(SafetyViewModel::class.java) -> {
                SafetyViewModel(application.safetyRepository) as T
            }
            modelClass.isAssignableFrom(EquipmentViewModel::class.java) -> {
                EquipmentViewModel(application.equipmentRepository) as T
            }
            modelClass.isAssignableFrom(TrainingViewModel::class.java) -> {
                TrainingViewModel(application.trainingRepository) as T
            }
            modelClass.isAssignableFrom(PerformanceViewModel::class.java) -> {
                PerformanceViewModel(application.performanceRepository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
