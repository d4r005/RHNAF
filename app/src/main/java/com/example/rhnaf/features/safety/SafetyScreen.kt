package com.example.rhnaf.features.safety

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rhnaf.ui.ViewModelFactory
import com.example.rhnaf.ui.theme.IndustrialBlue
import com.example.rhnaf.ui.theme.IndustrialDark
import com.example.rhnaf.ui.theme.IndustrialLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafetyScreen(
    onNavigateBack: () -> Unit,
    viewModel: SafetyViewModel = viewModel(factory = ViewModelFactory)
) {
    val incidents by viewModel.allIncidents.collectAsState()
    val context = LocalContext.current

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            // Simulación de carga
            android.widget.Toast.makeText(context, "Auditoría cargada: ${uri.path}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        containerColor = IndustrialLight,
        topBar = {
            TopAppBar(
                title = { Text("Seguridad e Higiene (EHS)", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                actions = {
                    IconButton(onClick = { pdfLauncher.launch("application/pdf") }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Subir Auditoría PDF", tint = IndustrialBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = IndustrialDark
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.reportIncident("Acto inseguro detectado", "Medio") },
                containerColor = IndustrialDark,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Reportar Riesgo") },
                shape = RoundedCornerShape(16.dp)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Safety Stats
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SafetyStatCard("Días sin Accidentes", "342", Color(0xFF166534), Modifier.weight(1f))
                SafetyStatCard("Inspecciones Pend.", "2", Color(0xFFF59E0B), Modifier.weight(1f))
            }

            // Nueva sección de Auditorías
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auditorías EHS", fontWeight = FontWeight.Bold)
                        Text("Cargar resultados de inspección externa.", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                    Button(
                        onClick = { pdfLauncher.launch("application/pdf") },
                        colors = ButtonDefaults.buttonColors(containerColor = IndustrialBlue),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Subir PDF", fontSize = 12.sp)
                    }
                }
            }

            Text(
                "Bitácora de Incidentes",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(incidents) { incident ->
                    IncidentCard(incident)
                }
            }
        }
    }
}

@Composable
fun SafetyStatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = color)
        }
    }
}

@Composable
fun IncidentCard(incident: com.example.rhnaf.features.safety.Incident) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val severityColor = when (incident.severity) {
                "Alto" -> Color(0xFFEF4444)
                "Medio" -> Color(0xFFF59E0B)
                else -> Color(0xFF3B82F6)
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(severityColor.copy(alpha = 0.1f), androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = severityColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(incident.description, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text("Área: ${incident.area} | ${incident.date}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }

            Surface(
                color = severityColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    incident.severity,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = severityColor
                )
            }
        }
    }
}

import androidx.compose.ui.unit.sp
