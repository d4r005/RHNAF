package com.example.rhnaf.features.training

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rhnaf.ui.ViewModelFactory
import com.example.rhnaf.ui.theme.IndustrialBlue
import com.example.rhnaf.ui.theme.IndustrialDark
import com.example.rhnaf.ui.theme.IndustrialLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingScreen(
    onNavigateBack: () -> Unit,
    viewModel: TrainingViewModel = viewModel(factory = ViewModelFactory)
) {
    val courses by viewModel.allTraining.collectAsState()
    val context = LocalContext.current

    val excelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            android.widget.Toast.makeText(context, "Excel importado: ${uri.path}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        containerColor = IndustrialLight,
        topBar = {
            TopAppBar(
                title = { Text("Capacitación y CTPAT", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                actions = {
                    IconButton(onClick = { excelLauncher.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") }) {
                        Icon(Icons.Default.Description, contentDescription = "Importar Excel", tint = IndustrialBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = IndustrialDark
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.addTrainingRecord("Nuevo Curso CTPAT", "Obligatorio") },
                containerColor = IndustrialBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar Curso")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            AlertSection()
            
            // Excel Import Card
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
                        Text("Historial masivo", fontWeight = FontWeight.Bold)
                        Text("Importar capacitaciones desde archivo Excel.", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                    Button(
                        onClick = { excelLauncher.launch("application/vnd.ms-excel") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF166534)), // Green for Excel
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Importar .xlsx", fontSize = 12.sp)
                    }
                }
            }

            Text(
                "Cursos Próximos y Estatus",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(courses) { course ->
                    TrainingCard(course)
                }
            }
        }
    }
}

@Composable
fun TrainingCard(course: TrainingRecord) {
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
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(IndustrialLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.School, contentDescription = null, tint = IndustrialBlue, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(course.courseName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text("Fecha programada: ${course.date}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }

            Surface(
                color = IndustrialLight,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    course.type,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = IndustrialDark
                )
            }
        }
    }
}

@Composable
fun AlertSection() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFEF2F2)
        ),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFEE2E2))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("Capacitaciones Vencidas", style = MaterialTheme.typography.titleSmall, color = Color(0xFF991B1B), fontWeight = FontWeight.Bold)
                Text("12 empleados tienen cursos CTPAT vencidos.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF991B1B))
            }
        }
    }
}
