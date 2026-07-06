package com.example.rhnaf.features.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rhnaf.data.repository.AttendanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AttendanceViewModel(
    private val repository: AttendanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AttendanceState())
    val uiState: StateFlow<AttendanceState> = _uiState.asStateFlow()

    init {
        observeAttendanceLogs()
    }

    private fun observeAttendanceLogs() {
        viewModelScope.launch {
            // Combinamos logs locales y remotos si es necesario, o solo mostramos los locales por ahora
            repository.getLocalAttendanceLogs().collect { logsList ->
                _uiState.update { it.copy(logs = logsList) }
            }
        }
    }

    fun onEmployeeIdChanged(newId: String) {
        _uiState.update { it.copy(targetEmployeeId = newId) }
        
        // Buscamos dinámicamente qué tipo de checada le corresponde
        if (newId.isNotBlank()) {
            viewModelScope.launch {
                val nextType = repository.determineNextCheckType(newId)
                _uiState.update { it.copy(nextCheckType = nextType) }
            }
        }
    }

    fun registerCheck() {
        val employeeId = _uiState.value.targetEmployeeId
        val checkType = _uiState.value.nextCheckType

        if (employeeId.isBlank()) {
            _uiState.update { it.copy(message = "Por favor, ingresa un ID válido") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                repository.registerCheck(employeeId, checkType)
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        targetEmployeeId = "", 
                        message = "¡Checada de $checkType registrada con éxito!"
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, message = "Error: ${e.localizedMessage}") }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
