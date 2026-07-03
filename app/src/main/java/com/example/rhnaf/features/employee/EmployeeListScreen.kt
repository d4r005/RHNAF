package com.example.rhnaf.features.employee

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rhnaf.shared.model.Employee
import com.example.rhnaf.shared.model.EmployeeStatus
import com.example.rhnaf.ui.ViewModelFactory
import com.example.rhnaf.ui.theme.RHNAFTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeListScreen(
    onNavigateBack: () -> Unit,
    onEmployeeClick: (String) -> Unit,
    onAddEmployee: () -> Unit,
    viewModel: EmployeeViewModel = viewModel(factory = ViewModelFactory)
) {
    val employees by viewModel.allEmployees.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val filteredEmployees = remember(employees, searchQuery) {
        if (searchQuery.isEmpty()) employees
        else employees.filter { 
            it.firstName.contains(searchQuery, ignoreCase = true) || 
            it.lastName.contains(searchQuery, ignoreCase = true) ||
            it.id.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Expediente Digital") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Search */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Buscar")
                    }
                }
            )
        },
        floatingActionButton = {
            LargeFloatingActionButton(
                onClick = onAddEmployee,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar Empleado")
            }
        }
    ) { padding ->
        if (employees.isEmpty()) {
            EmptyEmployeesState(onAddEmployee, modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 88.dp) // Space for FAB
            ) {
                items(filteredEmployees, key = { it.id }) { employee ->
                    EmployeeListItem(
                        employee = employee,
                        onClick = { onEmployeeClick(employee.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun EmployeeListItem(
    employee: Employee,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { 
            Text(
                text = "${employee.firstName} ${employee.lastName}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            ) 
        },
        supportingContent = { 
            Text(
                text = "${employee.department} • ${employee.position}",
                style = MaterialTheme.typography.bodyMedium
            ) 
        },
        leadingContent = {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        },
        trailingContent = {
            Badge(
                containerColor = if (employee.status == EmployeeStatus.ACTIVE) 
                    MaterialTheme.colorScheme.primaryContainer 
                else MaterialTheme.colorScheme.errorContainer
            ) {
                Text(
                    text = employee.status.name,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
fun EmptyEmployeesState(onAddEmployee: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No hay empleados registrados",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onAddEmployee) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Agregar el primer empleado")
        }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun EmployeeListPreview() {
    RHNAFTheme {
        EmployeeListScreen(
            onNavigateBack = {},
            onEmployeeClick = {},
            onAddEmployee = {}
        )
    }
}
