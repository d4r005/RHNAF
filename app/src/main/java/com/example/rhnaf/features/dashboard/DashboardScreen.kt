package com.example.rhnaf.features.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToEmployeeList: () -> Unit,
    onNavigateToAttendance: () -> Unit,
    onNavigateToTraining: () -> Unit,
    onNavigateToSafety: () -> Unit,
    onNavigateToPortal: () -> Unit,
    onNavigateToPayroll: () -> Unit,
    onNavigateToSupervisor: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToReports: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RH NAF Dashboard") },
                actions = {
                    IconButton(onClick = onNavigateToNotifications) {
                        BadgedBox(
                            badge = { Badge { Text("3") } }
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notificaciones")
                        }
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
        ) {
            Text(
                text = "Bienvenido, Administrador",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // KPI Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KPICard("Activos", "154", Modifier.weight(1f))
                KPICard("Ausentismo", "2.5%", Modifier.weight(1f))
                KPICard("Rotación", "1.2%", Modifier.weight(1f))
            }

            val menuItems = listOf(
                DashboardMenuItem("Empleados", Icons.Default.People, onNavigateToEmployeeList),
                DashboardMenuItem("Asistencia", Icons.Default.CheckCircle, onNavigateToAttendance),
                DashboardMenuItem("Capacitación", Icons.Default.School, onNavigateToTraining),
                DashboardMenuItem("Seguridad", Icons.Default.Security, onNavigateToSafety),
                DashboardMenuItem("Mi Portal", Icons.Default.AccountCircle, onNavigateToPortal),
                DashboardMenuItem("Supervisor", Icons.Default.SupervisorAccount, onNavigateToSupervisor),
                DashboardMenuItem("Nómina", Icons.Default.Payments, onNavigateToPayroll),
                DashboardMenuItem("Reportes", Icons.Default.BarChart, onNavigateToReports),
                DashboardMenuItem("Vacaciones", Icons.Default.BeachAccess, {}),
                DashboardMenuItem("Evaluación", Icons.Default.Assessment, {}),
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(menuItems) { item ->
                    DashboardCard(item)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // AI Assistant Mock
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Asistente IA", style = MaterialTheme.typography.titleSmall)
                        Text("Detecto un riesgo de rotación del 15% en el área de soldadura. ¿Deseas ver el análisis?", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun KPICard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall)
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

data class DashboardMenuItem(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
fun DashboardCard(item: DashboardMenuItem) {
    Card(
        onClick = item.onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = item.title, style = MaterialTheme.typography.titleMedium)
        }
    }
}
