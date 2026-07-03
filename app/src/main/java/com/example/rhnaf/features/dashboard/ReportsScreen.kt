package com.example.rhnaf.features.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reportes e Indicadores") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Exportar Información", style = MaterialTheme.typography.titleLarge)
            
            ReportItem("Listado de Empleados (Excel/CSV)")
            ReportItem("Reporte de Asistencia Semanal")
            ReportItem("Incidencias CTPAT / EHS")
            ReportItem("Vencimiento de Contratos")
            ReportItem("Indicadores de Rotación (PDF)")

            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Integraciones Externas", style = MaterialTheme.typography.titleLarge)
            
            OutlinedButton(onClick = { /* Link to PowerBI */ }, modifier = Modifier.fillMaxWidth()) {
                Text("Abrir Dashboard en Power BI")
            }
            
            OutlinedButton(onClick = { /* Link to Looker */ }, modifier = Modifier.fillMaxWidth()) {
                Text("Abrir Google Looker Studio")
            }
        }
    }
}

@Composable
fun ReportItem(title: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text(title) },
            trailingContent = {
                IconButton(onClick = { /* Export Logic */ }) {
                    Icon(Icons.Default.FileDownload, contentDescription = "Descargar")
                }
            }
        )
    }
}
