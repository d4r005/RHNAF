package com.example.rhnaf.features.employee

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeePortalScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Portal RH") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Juan Pérez", style = MaterialTheme.typography.titleLarge)
                        Text("N. Empleado: 85042", style = MaterialTheme.typography.bodyMedium)
                        Text("Días de vacaciones disponibles: 12", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Acciones Rápidas", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start))
            
            Spacer(modifier = Modifier.height(8.dp))

            val portalItems = listOf(
                PortalItem("Mis Recibos", Icons.AutoMirrored.Filled.ReceiptLong),
                PortalItem("Solicitar Vacaciones", Icons.Default.BeachAccess),
                PortalItem("Mis Cursos", Icons.Default.HistoryEdu),
                PortalItem("Documentos", Icons.Default.FolderOpen),
                PortalItem("Actualizar Datos", Icons.Default.Badge),
                PortalItem("Ayuda / FAQ", Icons.Default.QuestionAnswer),
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                portalItems.forEach { item ->
                    OutlinedCard(
                        onClick = { /* TODO */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ListItem(
                            headlineContent = { Text(item.title) },
                            leadingContent = { Icon(item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) }
                        )
                    }
                }
            }
        }
    }
}

data class PortalItem(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
