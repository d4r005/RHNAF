package com.example.rhnaf.features.employee

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rhnaf.shared.model.Employee
import com.example.rhnaf.data.repository.EmployeeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EmployeeViewModel(private val repository: EmployeeRepository) : ViewModel() {

    val allEmployees: StateFlow<List<Employee>> = repository.getAllEmployees()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addEmployee(employee: Employee) {
        viewModelScope.launch {
            repository.insertEmployee(employee)
        }
    }

    suspend fun getEmployeeById(id: String): Employee? {
        return repository.getEmployeeById(id)
    }
}
