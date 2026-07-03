package com.example.rhnaf.features.employee

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rhnaf.shared.logic.VacationCalculator
import com.example.rhnaf.shared.model.Employee
import com.example.rhnaf.shared.model.EmployeeStatus
import com.example.rhnaf.ui.ViewModelFactory
import com.example.rhnaf.ui.theme.RHNAFTheme

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
                title = { Text("Expediente Digital") },
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
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                
                // Profile Header
                Surface(
                    modifier = Modifier.size(100.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(60.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "${emp.firstName} ${emp.lastName}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = emp.position,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
                
                Badge(
                    containerColor = if (emp.status == EmployeeStatus.ACTIVE) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(emp.status.name, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                DetailSection("Información Personal") {
                    DetailItem("CURP", emp.curp ?: "No registrado")
                    DetailItem("RFC", emp.rfc ?: "No registrado")
                    DetailItem("NSS", emp.nss ?: "No registrado")
                    DetailItem("Email", emp.email ?: "No registrado")
                    DetailItem("Teléfono", emp.phone ?: "No registrado")
                }
                
                DetailSection("Detalles Laborales") {
                    DetailItem("Departamento", emp.department)
                    DetailItem("Fecha de Ingreso", emp.entryDate)
                    DetailItem("Supervisor", emp.supervisor ?: "N/A")
                    DetailItem("Tipo de Contrato", emp.contractType ?: "N/A")
                    
                    val daysEarned = VacationCalculator.calculateVacationDays(emp.entryDate)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Vacaciones Ganadas: $daysEarned días", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    LinearProgressIndicator(
                        progress = { 0.7f }, // Mock
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    )
                }

                DetailSection("Equipo Asignado") {
                    if (equipment.isEmpty()) {
                        Text("Sin equipo asignado", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    } else {
                        equipment.forEach { item ->
                            DetailItem(item.itemType, item.description)
                        }
                    }
                }

                DetailSection("Últimas Evaluaciones") {
                    if (evaluations.isEmpty()) {
                        Text("Sin evaluaciones", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    } else {
                        evaluations.forEach { eval ->
                            DetailItem("Score: ${eval.score}", eval.date)
                            Text(eval.feedback, style = MaterialTheme.typography.bodySmall)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun DetailSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                content()
            }
        }
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Preview(showBackground = true)
@Composable
fun EmployeeDetailPreview() {
    RHNAFTheme {
        EmployeeDetailScreen("123", {})
    }
}
