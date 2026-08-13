package com.example.rhnaf.features.employee

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.rhnaf.shared.logic.VacationCalculator
import com.example.rhnaf.shared.model.Employee
import com.example.rhnaf.shared.model.EmployeeStatus
import com.example.rhnaf.ui.ViewModelFactory
import com.example.rhnaf.ui.theme.IndustrialBlue
import com.example.rhnaf.ui.theme.IndustrialDark
import com.example.rhnaf.ui.theme.IndustrialLight

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
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showBajaDialog by remember { mutableStateOf(false) }

    LaunchedEffect(employeeId) {
        employee = employeeViewModel.getEmployeeById(employeeId)
    }

    // Cuándo se da de baja o se elimina, volver a la lista.
    LaunchedEffect(employee?.status) {
        // Si cambió a INACTIVE por acción del dialog, recargar.
    }

    Scaffold(
        containerColor = IndustrialLight,
        topBar = {
            TopAppBar(
                title = { Text("Expediente Digital", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                actions = {
                    IconButton(onClick = { showBajaDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Dar de Baja / Editar")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = IndustrialDark
                )
            )
        }
    ) { padding ->
        val emp = employee
        if (emp == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = IndustrialBlue)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Card con foto real
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Foto del empleado: usa AsyncImage de Coil con el photoUrl
                        // (data URI en base64 que viene de la lectora Hikvision).
                        // Fallback a ícono de persona si no hay foto.
                        Surface(
                            modifier = Modifier.size(100.dp),
                            shape = CircleShape,
                            color = IndustrialLight
                        ) {
                            val photo = emp.photoUrl
                            if (!photo.isNullOrBlank()) {
                                AsyncImage(
                                    model = photo,
                                    contentDescription = "Foto de ${emp.firstName}",
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.padding(20.dp),
                                    tint = IndustrialBlue
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "${emp.firstName} ${emp.lastName}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(text = emp.position, style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        StatusChip(emp.status)
                    }
                }

                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    DetailSection("Datos Laborales") {
                        DetailItem("Departamento", emp.department)
                        DetailItem("Fecha de Ingreso", emp.entryDate)
                        DetailItem("Supervisor", emp.supervisor ?: "N/A")
                        DetailItem("Estatus", emp.status.name)
                    }

                    DetailSection("Identidad y Fiscal") {
                        DetailItem("CURP", emp.curp ?: "N/A")
                        DetailItem("RFC", emp.rfc ?: "N/A")
                        DetailItem("NSS", emp.nss ?: "N/A")
                    }

                    DetailSection("Equipo (EPP)") {
                        if (equipment.isEmpty()) {
                            Text("No hay equipo asignado", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        } else {
                            equipment.forEach { item ->
                                DetailItem(item.itemType, item.description)
                            }
                        }
                        Button(
                            onClick = { equipmentViewModel.assignEquipment(emp.id, "Uniforme", "Polo NAF L") },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = IndustrialLight, contentColor = IndustrialBlue),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("+ Registrar Entrega EPP", fontWeight = FontWeight.Bold)
                        }
                    }

                    DetailSection("Control de Vacaciones") {
                        val daysEarned = VacationCalculator.calculateVacationDays(emp.entryDate)
                        DetailItem("Días Ganados", "$daysEarned")
                        DetailItem("Días Disponibles", "${daysEarned - 2}")
                        LinearProgressIndicator(
                            progress = { (daysEarned - 2) / daysEarned.toFloat() },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clip(RoundedCornerShape(4.dp)),
                            color = IndustrialBlue,
                            trackColor = IndustrialLight
                        )
                    }

                    DetailSection("Historial de Desempeño") {
                        if (evaluations.isEmpty()) {
                            Text("Sin evaluaciones registradas", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        } else {
                            evaluations.forEach { eval ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Puntuación: ${eval.score}", fontWeight = FontWeight.Bold, color = IndustrialBlue)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(eval.date, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                }
                                Text(eval.feedback, style = MaterialTheme.typography.bodySmall)
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = IndustrialLight)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Dialog para dar de baja (cambiar status a INACTIVE)
    if (showBajaDialog && employee != null) {
        AlertDialog(
            onDismissRequest = { showBajaDialog = false },
            title = { Text("Dar de Baja") },
            text = { Text("¿Cambiar el estatus de ${employee!!.firstName} ${employee!!.lastName} a INACTIVO? Esta acción la puede realizar RH o Admin.") },
            confirmButton = {
                TextButton(onClick = {
                    val updated = employee!!.copy(status = EmployeeStatus.INACTIVE)
                    employeeViewModel.updateEmployee(updated)
                    employee = updated
                    showBajaDialog = false
                }) { Text("Confirmar Baja", color = Color(0xFF991B1B)) }
            },
            dismissButton = {
                TextButton(onClick = { showBajaDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // Dialog para eliminar permanentemente
    if (showDeleteDialog && employee != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar Empleado") },
            text = { Text("¿Eliminar definitivamente a ${employee!!.firstName} ${employee!!.lastName}? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    employeeViewModel.deleteEmployee(employee!!)
                    showDeleteDialog = false
                    onNavigateBack()
                }) { Text("Eliminar", color = Color(0xFF991B1B)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun DetailSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = IndustrialDark,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = IndustrialDark)
    }
}
