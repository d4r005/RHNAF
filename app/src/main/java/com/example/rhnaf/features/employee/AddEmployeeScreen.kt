package com.example.rhnaf.features.employee

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rhnaf.shared.model.Employee
import com.example.rhnaf.shared.model.EmployeeStatus
import com.example.rhnaf.ui.ViewModelFactory
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
    var entryDate by remember { mutableStateOf("2023-11-23") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo Ingreso / Onboarding") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val newEmployee = Employee(
                            id = UUID.randomUUID().toString().take(8),
                            firstName = firstName,
                            lastName = lastName,
                            curp = curp,
                            rfc = rfc,
                            position = position,
                            department = department,
                            entryDate = entryDate,
                            status = EmployeeStatus.ACTIVE
                        )
                        viewModel.addEmployee(newEmployee)
                        onNavigateBack()
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "Guardar")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Datos Generales", style = MaterialTheme.typography.titleMedium)
            
            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text("Nombre(s)") },
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text("Apellidos") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = curp,
                    onValueChange = { curp = it },
                    label = { Text("CURP") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = rfc,
                    onValueChange = { rfc = it },
                    label = { Text("RFC") },
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider()
            Text("Puesto y Área", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = position,
                onValueChange = { position = it },
                label = { Text("Puesto") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = department,
                onValueChange = { department = it },
                label = { Text("Departamento") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = onNavigateToSignature,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Registrar y Continuar a Firma")
            }
        }
    }
}
