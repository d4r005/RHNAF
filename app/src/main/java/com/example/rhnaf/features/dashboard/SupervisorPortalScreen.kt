package com.example.rhnaf.features.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupervisorPortalScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Portal del Supervisor") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text("Aprobaciones Pendientes", style = MaterialTheme.typography.titleLarge)
            
            Spacer(modifier = Modifier.height(16.dp))

            val pendingRequests = listOf(
                Request("Juan Pérez", "Vacaciones", "2023-12-01 al 2023-12-05"),
                Request("María García", "Permiso Personal", "2023-11-25"),
                Request("Carlos López", "Horas Extras", "4 horas - 2023-11-20"),
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(pendingRequests) { request ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        ListItem(
                            headlineContent = { Text(request.employeeName) },
                            supportingContent = { Text("${request.type}: ${request.detail}") },
                            trailingContent = {
                                Row {
                                    IconButton(onClick = { /* Approve */ }) {
                                        Icon(Icons.Default.Check, contentDescription = "Aprobar", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = { /* Deny */ }) {
                                        Icon(Icons.Default.Close, contentDescription = "Rechazar", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

data class Request(val employeeName: String, val type: String, val detail: String)
