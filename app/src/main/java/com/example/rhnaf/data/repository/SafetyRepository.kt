package com.example.rhnaf.data.repository

import com.example.rhnaf.data.local.IncidentDao
import com.example.rhnaf.data.local.entities.IncidentEntity
import kotlinx.coroutines.flow.Flow

class SafetyRepository(private val incidentDao: IncidentDao) {
    fun getAllIncidents(): Flow<List<IncidentEntity>> = incidentDao.getAllIncidents()

    suspend fun reportIncident(incident: IncidentEntity) {
        incidentDao.insertIncident(incident)
    }
}
