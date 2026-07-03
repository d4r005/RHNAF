package com.example.rhnaf.features.employee

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rhnaf.shared.model.Employee
import com.example.rhnaf.shared.model.EmployeeStatus
import com.example.rhnaf.ui.ViewModelFactory
import com.example.rhnaf.ui.theme.RHNAFTheme
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEmployeeScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSignature: () -> Unit,
    viewModel: EmployeeViewModel = viewModel(factory = ViewModelFactory)
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var curp by remember { mutableStateOf("") }
    var rfc by remember { mutableStateOf("") }
    var position by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var entryDate by remember { mutableStateOf("2024-01-01") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alta de Empleado") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        if (firstName.isNotBlank() && lastName.isNotBlank()) {
                            val newEmployee = Employee(
                                id = UUID.randomUUID().toString().take(8).uppercase(),
                                firstName = firstName,
                                lastName = lastName,
                                curp = curp.uppercase(),
                                rfc = rfc.uppercase(),
                                position = position,
                                department = department,
                                entryDate = entryDate,
                                status = EmployeeStatus.ACTIVE
                            )
                            viewModel.addEmployee(newEmployee)
                            onNavigateBack()
                        }
                    }) {
                        Text("GUARDAR")
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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            FormSection(title = "Información Personal", icon = Icons.Default.Person) {
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("Nombre(s)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Apellidos") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = curp,
                        onValueChange = { curp = it },
                        label = { Text("CURP") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = rfc,
                        onValueChange = { rfc = it },
                        label = { Text("RFC") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }

            FormSection(title = "Datos Laborales", icon = Icons.Default.Business) {
                OutlinedTextField(
                    value = position,
                    onValueChange = { position = it },
                    label = { Text("Puesto") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = department,
                    onValueChange = { department = it },
                    label = { Text("Departamento") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = entryDate,
                    onValueChange = { entryDate = it },
                    label = { Text("Fecha de Ingreso (AAAA-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            Button(
                onClick = onNavigateToSignature,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Registrar y Proceder a Firma")
            }
        }
    }
}

@Composable
fun FormSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        }
        content()
    }
}

@Preview(showBackground = true)
@Composable
fun AddEmployeePreview() {
    RHNAFTheme {
        AddEmployeeScreen({}, {})
    }
}
