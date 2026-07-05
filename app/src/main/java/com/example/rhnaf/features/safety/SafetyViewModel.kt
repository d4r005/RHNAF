package com.example.rhnaf.features.safety

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rhnaf.data.local.entities.IncidentEntity
import com.example.rhnaf.data.repository.SafetyRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SafetyViewModel(private val repository: SafetyRepository) : ViewModel() {
    val allIncidents: StateFlow<List<IncidentEntity>> = repository.getAllIncidents()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun reportIncident(description: String, severity: String, area: String = "General") {
        viewModelScope.launch {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            repository.reportIncident(
                IncidentEntity(
                    description = description,
                    date = date,
                    severity = severity,
                    area = area,
                    reportedBy = "Admin"
                )
            )
        }
    }

    fun importAudit(fileName: String) {
        viewModelScope.launch {
            val audits = listOf(
                "Auditoría Interna ISO 45001" to "Cumplimiento 95%",
                "Inspección de EPP - Planta 1" to "Aprobado",
                "Revisión de Seguridad Eléctrica" to "Observaciones Menores"
            )
            audits.forEach { (desc, area) ->
                repository.reportIncident(
                    IncidentEntity(
                        description = desc,
                        date = "2026-07-05",
                        severity = "Bajo",
                        area = area,
                        reportedBy = "Sistema (PDF)"
                    )
                )
            }
        }
    }
}
