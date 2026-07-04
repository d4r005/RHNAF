package com.example.rhnaf.features.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rhnaf.ui.theme.IndustrialBlue
import com.example.rhnaf.ui.theme.IndustrialDark

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
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "NAF",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-1).sp
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "CONNECT",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Light,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToNotifications) {
                        BadgedBox(
                            badge = { Badge { Text("3") } }
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notificaciones")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = IndustrialDark
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Panel de Control Industrial",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Gray
                )
                Text(
                    text = "Bienvenido, Dario Robles",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = IndustrialDark
                )

                Spacer(modifier = Modifier.height(20.dp))

                // KPI Section (Like StatCard in Web)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KPICard("Total Empleados", "154", "Base de datos", IndustrialBlue, Modifier.weight(1f))
                    KPICard("Asistencia", "98%", "Hoy", Color(0xFF22C55E), Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Módulos de Gestión",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 12.dp),
                    fontWeight = FontWeight.Bold
                )

                val menuItems = listOf(
                    DashboardMenuItem("Plantilla", Icons.Default.People, onNavigateToEmployeeList),
                    DashboardMenuItem("Asistencia", Icons.Default.Face, onNavigateToAttendance),
                    DashboardMenuItem("Capacitación", Icons.Default.ModelTraining, onNavigateToTraining),
                    DashboardMenuItem("Seguridad EHS", Icons.Default.HealthAndSafety, onNavigateToSafety),
                    DashboardMenuItem("Nómina", Icons.Default.AccountBalanceWallet, onNavigateToPayroll),
                    DashboardMenuItem("Reportes", Icons.Default.BarChart, onNavigateToReports),
                    DashboardMenuItem("Activos", Icons.Default.Inventory2, {}),
                    DashboardMenuItem("Ajustes", Icons.Default.Settings, {}),
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(menuItems) { item ->
                        DashboardCard(item)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // AI Insight Widget
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = IndustrialDark),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(IndustrialBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("NAF AI Insight", color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                "Se detectó riesgo de rotación en Producción. Sugiero revisar bonos de asistencia.",
                                color = Color.LightGray,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KPICard(label: String, value: String, sub: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = IndustrialDark
            )
            Text(text = sub, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
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
            .height(110.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(IndustrialLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    modifier = Modifier.size(20.dp),
                    tint = IndustrialBlue
                )
            }
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = IndustrialDark
            )
        }
    }
}
