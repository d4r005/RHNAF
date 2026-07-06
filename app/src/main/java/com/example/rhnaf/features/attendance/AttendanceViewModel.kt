package com.example.rhnaf.features.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rhnaf.data.local.entities.AttendanceLogEntity
import com.example.rhnaf.data.repository.AttendanceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AttendanceViewModel(private val repository: AttendanceRepository) : ViewModel() {

    val allLogs: StateFlow<List<AttendanceLogEntity>> = repository.getAllAttendanceLogs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun logAttendance(log: AttendanceLogEntity) {
        viewModelScope.launch {
            repository.insertAttendanceLog(log)
        }
    }
}
