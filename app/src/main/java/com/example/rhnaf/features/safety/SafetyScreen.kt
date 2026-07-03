package com.example.rhnaf.features.safety

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rhnaf.ui.ViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafetyScreen(
    onNavigateBack: () -> Unit,
    viewModel: SafetyViewModel = viewModel(factory = ViewModelFactory)
) {
    val incidents by viewModel.allIncidents.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Seguridad EHS") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { 
                    viewModel.reportIncident("Acto inseguro detectado", "Medio")
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Reportar Incidente") }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text("Incidentes Recientes", style = MaterialTheme.typography.titleLarge)
            
            LazyColumn {
                items(incidents) { incident ->
                    ListItem(
                        headlineContent = { Text(incident.description) },
                        supportingContent = { Text("Fecha: ${incident.date} | Área: ${incident.area}") },
                        leadingContent = { Icon(Icons.Default.HealthAndSafety, contentDescription = null) },
                        trailingContent = { 
                            Text(incident.severity, color = when(incident.severity) {
                                "Alto" -> MaterialTheme.colorScheme.error
                                "Medio" -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.outline
                            })
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
