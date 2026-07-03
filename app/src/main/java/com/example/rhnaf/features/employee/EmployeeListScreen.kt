package com.example.rhnaf.features.employee

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rhnaf.ui.ViewModelFactory

import com.example.rhnaf.shared.model.Employee
import com.example.rhnaf.shared.model.EmployeeStatus
import androidx.compose.material.icons.automirrored.filled.ArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeListScreen(
    onNavigateBack: () -> Unit,
    onEmployeeClick: (String) -> Unit,
    onAddEmployee: () -> Unit,
    viewModel: EmployeeViewModel = viewModel(factory = ViewModelFactory)
) {
    val employees by viewModel.allEmployees.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expediente de Empleados") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddEmployee) {
                Icon(Icons.Default.Add, contentDescription = "Agregar Empleado")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(employees) { employee ->
                ListItem(
                    headlineContent = { Text("${employee.firstName} ${employee.lastName}") },
                    supportingContent = { Text("${employee.department} - ${employee.position}") },
                    trailingContent = { Text("#${employee.id}") },
                    modifier = Modifier.clickable { onEmployeeClick(employee.id) }
                )
                HorizontalDivider()
            }
        }
    }
}

// I need a clickable modifier on ListItem, but it's not directly there in M3 ListItem.
// Using Modifier.clickable on the ListItem itself.
import androidx.compose.foundation.clickable
