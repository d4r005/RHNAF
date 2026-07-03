package com.example.rhnaf.features.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notificaciones y Alertas") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        }
    ) { padding ->
        val alerts = listOf(
            Alert("Tu contrato vence en 30 días.", "RH", "2023-11-20"),
            Alert("Tienes capacitación CTPAT mañana a las 10:00 AM.", "Entrenamiento", "2023-11-22"),
            Alert("Nueva evaluación de desempeño disponible.", "Desempeño", "2023-11-21"),
            Alert("Solicitud de vacaciones aprobada.", "Supervisor", "2023-11-19"),
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(alerts) { alert ->
                ListItem(
                    headlineContent = { Text(alert.message) },
                    supportingContent = { Text("${alert.source} - ${alert.date}") },
                    leadingContent = { Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) }
                )
                HorizontalDivider()
            }
        }
    }
}

data class Alert(val message: String, val source: String, val date: String)
