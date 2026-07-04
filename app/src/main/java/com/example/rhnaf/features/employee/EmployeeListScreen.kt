package com.example.rhnaf.features.employee

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rhnaf.shared.model.Employee
import com.example.rhnaf.shared.model.EmployeeStatus
import com.example.rhnaf.ui.ViewModelFactory
import com.example.rhnaf.ui.theme.IndustrialBlue
import com.example.rhnaf.ui.theme.IndustrialDark
import com.example.rhnaf.ui.theme.IndustrialLight

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
        containerColor = IndustrialLight,
        topBar = {
            TopAppBar(
                title = { Text("Plantilla Personal", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Search */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Buscar")
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
                onClick = onAddEmployee,
                containerColor = IndustrialBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar Empleado")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Stats summary at top
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Mostrando ${employees.size} colaboradores",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(employees) { employee ->
                    EmployeeCard(employee, onClick = { onEmployeeClick(employee.id) })
                }
            }
        }
    }
}

@Composable
fun EmployeeCard(employee: Employee, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar Placeholder
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(IndustrialLight),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${employee.firstName.take(1)}${employee.lastName.take(1)}",
                    fontWeight = FontWeight.Bold,
                    color = IndustrialBlue
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${employee.firstName} ${employee.lastName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = IndustrialDark
                )
                Text(
                    text = employee.position,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            // Status Badge
            StatusChip(employee.status)
        }
    }
}

@Composable
fun StatusChip(status: EmployeeStatus) {
    val (color, bgColor) = when (status) {
        EmployeeStatus.ACTIVE -> Color(0xFF166534) to Color(0xFFDCFCE7)
        EmployeeStatus.VACATION -> Color(0xFF854D0E) to Color(0xFFFEF9C3)
        else -> Color(0xFF991B1B) to Color(0xFFFEE2E2)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = status.name,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = color
        )
    }
}
