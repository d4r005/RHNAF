package com.example.rhnaf.features.employee

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rhnaf.ui.ViewModelFactory
import com.example.rhnaf.shared.model.Employee
import com.example.rhnaf.shared.logic.VacationCalculator

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EmployeeDetailScreen(
    employeeId: String,
    onNavigateBack: () -> Unit,
    employeeViewModel: EmployeeViewModel = viewModel(factory = ViewModelFactory),
    equipmentViewModel: EquipmentViewModel = viewModel(factory = ViewModelFactory),
    performanceViewModel: PerformanceViewModel = viewModel(factory = ViewModelFactory)
) {
    var employee by remember { mutableStateOf<Employee?>(null) }
    val equipment by equipmentViewModel.getEquipment(employeeId).collectAsState(initial = emptyList())
    val evaluations by performanceViewModel.getEvaluations(employeeId).collectAsState(initial = emptyList())

    LaunchedEffect(employeeId) {
        employee = employeeViewModel.getEmployeeById(employeeId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expediente: ${employee?.firstName ?: ""} ${employee?.lastName ?: ""}") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Edit */ }) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar")
                    }
                }
            )
        }
    ) { padding ->
        val emp = employee
        if (emp == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Image Placeholder
                Surface(
                    modifier = Modifier.size(120.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.padding(24.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(text = "${emp.firstName} ${emp.lastName}", style = MaterialTheme.typography.headlineMedium)
                Text(text = emp.position, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.secondary)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                DetailSection("Información Personal") {
                    DetailItem("CURP", emp.curp ?: "N/A")
                    DetailItem("RFC", emp.rfc ?: "N/A")
                    DetailItem("NSS", emp.nss ?: "N/A")
                    DetailItem("Estado Civil", emp.maritalStatus ?: "N/A")
                }
                
                DetailSection("Laboral") {
                    DetailItem("Fecha de Ingreso", emp.entryDate)
                    DetailItem("Departamento", emp.department)
                    DetailItem("Supervisor", emp.supervisor ?: "N/A")
                    DetailItem("Tipo de Contrato", emp.contractType ?: "N/A")
                }

                DetailSection("Equipo y Uniformes") {
                    if (equipment.isEmpty()) {
                        Text("No hay equipo asignado", style = MaterialTheme.typography.bodySmall)
                    } else {
                        equipment.forEach { item ->
                            DetailItem(item.itemType, item.description)
                        }
                    }
                    TextButton(onClick = { 
                        equipmentViewModel.assignEquipment(emp.id, "Uniforme", "Playera Polo XL")
                    }) {
                        Text("Asignar Equipo")
                    }
                }

                DetailSection("Evaluación de Desempeño") {
                    if (evaluations.isEmpty()) {
                        Text("Sin evaluaciones registradas", style = MaterialTheme.typography.bodySmall)
                    } else {
                        evaluations.forEach { eval ->
                            DetailItem("Puntuación: ${eval.score}", eval.date)
                            Text(eval.feedback, style = MaterialTheme.typography.bodySmall)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                    TextButton(onClick = { 
                        performanceViewModel.addEvaluation(emp.id, 4.5f, "Excelente desempeño en producción.")
                    }) {
                        Text("Nueva Evaluación")
                    }
                }

                DetailSection("Competencias y Certificaciones") {
                    val comps = listOf("CTPAT", "ISO 9001", "Primeros Auxilios", "Montacargas", "Excel Avanzado")
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        comps.forEach { comp ->
                            SuggestionChip(
                                onClick = { },
                                label = { Text(comp) }
                            )
                        }
                    }
                }
                
                DetailSection("Salud Ocupacional") {
                    DetailItem("Último Examen", "2023-08-10")
                    DetailItem("Restricciones", "Ninguna")
                    TextButton(onClick = { /* TODO: View Medical Records */ }) {
                        Text("Ver Historial Médico")
                    }
                }

                DetailSection("Vacaciones") {
                    // Simple logic for Mexican law (LEY FEDERAL DEL TRABAJO)
                    // Assuming entryDate is yyyy-MM-dd
                    val daysEarned = VacationCalculator.calculateVacationDays(emp.entryDate)
                    DetailItem("Días Ganados (Anual)", "$daysEarned")
                    DetailItem("Días Disponibles", "${daysEarned - 2}") // Mock used days
                    
                    LinearProgressIndicator(
                        progress = { (daysEarned - 2) / daysEarned.toFloat() },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    )
                }
                
                DetailSection("Documentación") {
                    TextButton(onClick = { /* TODO: View PDF */ }) {
                        Text("Ver Contrato Firmado (PDF)")
                    }
                    TextButton(onClick = { /* TODO: View PDF */ }) {
                        Text("Certificados de Capacitación")
                    }
                }
            }
        }
    }
}

@Composable
fun DetailSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                content()
            }
        }
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

