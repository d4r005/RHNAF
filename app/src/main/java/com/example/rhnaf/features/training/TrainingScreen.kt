package com.example.rhnaf.features.training

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rhnaf.ui.ViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingScreen(
    onNavigateBack: () -> Unit,
    viewModel: TrainingViewModel = viewModel(factory = ViewModelFactory)
) {
    val courses by viewModel.allTraining.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Capacitación y CTPAT") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.addTrainingRecord("Nuevo Curso CTPAT", "Obligatorio")
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Agregar Curso")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            AlertSection()
            
            Text(
                "Cursos Próximos",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )
            
            LazyColumn {
                items(courses) { course ->
                    ListItem(
                        headlineContent = { Text(course.courseName) },
                        supportingContent = { Text("Fecha: ${course.date}") },
                        trailingContent = { 
                            Badge(containerColor = MaterialTheme.colorScheme.secondary) {
                                Text(course.type)
                            }
                        }
                    )
                    HorizontalDivider()
                }
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
            containerColor = Color(0xFFFFEBEE)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("Capacitaciones Vencidas", style = MaterialTheme.typography.titleSmall, color = Color.Red)
                Text("12 empleados tienen cursos CTPAT vencidos.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
