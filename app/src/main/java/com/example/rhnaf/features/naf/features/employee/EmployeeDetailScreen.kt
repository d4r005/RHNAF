package com.example.rhnaf.features.employee

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rhnaf.shared.logic.VacationCalculator
import com.example.rhnaf.shared.model.Employee
import com.example.rhnaf.shared.model.EmployeeStatus
import com.example.rhnaf.shared.logic.VacationCalculator
import com.example.rhnaf.shared.model.Employee
import com.example.rhnaf.shared.model.EmployeeStatus
import com.example.rhnaf.ui.ViewModelFactory

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

    // Fetch employee when the employeeId changes
    LaunchedEffect(employeeId) {
        val emp = employeeViewModel.getEmployeeById(employeeId)
        employee = emp
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.employee_detail_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back))
                    }
                },
                actions = {
                    IconButton(
                        onClick = { /* TODO: Implement edit */ },
                        enabled = employee != null
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(id = R.string.edit))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding: PaddingValues ->
        if (employee == null) {
            // Show a placeholder while loading
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = androidx.ui.Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            // Employee data is available
            Column(modifier = Modifier.padding(innerPadding)) {
                // Employee header with avatar and name
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                        ) {
                            Text(
                                text = "${employee.firstName.take(1)}${employee.lastName.take(1)}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                textAlign = androidx.ui.text.TextAlign.Center,
                                modifier = Modifier.align(alignment = Alignment.Center)
                            )
                        }

                        // Name and details
                        Column {
                            Text(
                                text = "${employee.firstName} ${employee.lastName}",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = employee.position,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${getString(R.string.department_label)}: ${employee.department}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Employment duration badge
                    val monthsEmployed = java.time.Duration.between(
                        java.time.LocalDate.parse(employee.entryDate).atStartOfDay(),
                        java.time.LocalDate.now().atStartOfDay()
                    ).toMonths()
                    val years = monthsEmployed / 12
                    val months = monthsEmployed % 12
                    val durationText = if (years > 0) "$years año${if (years > 1) \"s\" else \"\"} $meses mes${if (meses > 1) \"s\" else \"\"}" else "$meses mes${if (meses > 1) \"s\" else \"\"}"
                    Chip(
                        label = { Text("$durationText antigüedad", color = MaterialTheme.colorScheme.onSurface) },
                        colors = ChipDefaults.chipColors(
                            backgroundColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }

                Divider(
                    color = MaterialTheme.colorScheme.divider,
                    thickness = 1.dp
                )

                // Details section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Personal Info
                    Text(
                        text = getString(R.string.personal_info),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Photo placeholder (could be replaced with actual image)
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            InfoRow(
                                label = getString(R.string.first_name),
                                value = employee.firstName
                            )
                            InfoRow(
                                label = getString(R.string.last_name),
                                value = employee.lastName
                            )
                            InfoRow(
                                label = getString(R.string.curp_label),
                                value = employee.curp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Employment Info
                    Text(
                        text = getString(R.string.job_info),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column {
                        InfoRow(
                            label = getString(R.string.position),
                            value = employee.position
                        )
                        InfoRow(
                            label = getString(R.string.department_label),
                            value = employee.department
                        )
                        InfoRow(
                            label = getString(R.string.hire_date_label),
                            value = employee.entryDate
                        )
                        InfoRow(
                            label = getString(R.string.supervisor_label),
                            value = employee.supervisorName ?: "No asignado"
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Identity & Fiscal
                    Text(
                        text = getString(R.string.identity_fiscal_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column {
                        InfoRow(
                            label = getString(R.string.curp_label),
                            value = employee.curp
                        )
                        InfoRow(
                            label = getString(R.string.rfc_label),
                            value = employee.rfc
                        )
                        InfoRow(
                            label = getString(R.string.nss_label),
                            value = employee.nss ?: "No registrado"
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Equipment
                    Text(
                        text = getString(R.string.equipment_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // We would normally collect equipment from the equipmentViewModel, but for simplicity, we show a placeholder.
                    // TODO: Replace with actual equipment list from equipmentViewModel
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = getString(R.string.no_equipment_assigned),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { /* TODO: Register equipment */ },
                            enabled = true
                        ) {
                            Text(text = getString(R.string.register_equipment))
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Vacation Control
                    Text(
                        text = getString(R.string.vacation_control_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = getString(R.string.days_earned),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${VacationCalculator.calculateVacationDays(employee.entryDate)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Column {
                            Text(
                                text = getString(R.string.days_available),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${VacationCalculator.calculateAvailableVacationDays(employee.entryDate, employee.vacationDaysTaken)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Performance History
                    Text(
                        text = getString(R.string.performance_history_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // In a real app, we would collect performance data from the performanceViewModel
                    // For now, we show a placeholder.
                    if (true) { // Replace with actual condition when we have data
                        // Placeholder for performance items
                        Text(
                            text = getString(R.string.no_evaluations),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        // List of evaluations would go here
                    }
                }
            }
        }
    }
}

// Helper composable for displaying a label-value pair
@Composable
private fun InfoLabel(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp, 0.dp)
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall mistake. Let's fix the typo: MaterialTheme.typography.bodySmall

We'll rewrite the file with a corrected version. However, given the length, we might want to break it into multiple writes, but let's try to write the whole thing correctly.

We'll also note that we need to import androidx.compose.ui.text.TextAlign.

Let's write the file again, ensuring we fix any typos.

But first, let's check if there's a way to get the current content to avoid rewriting from scratch? We can read it, but it's likely incomplete.

Given the time, we'll write a corrected version.

We'll also note that we used `getString` which is not available in a composable; we need to use `stringResource`. We'll fix that.

Let's rewrite the file with proper stringResource calls and correct imports.

We'll do it in one<tool_call>
<function=Write>
<parameter=content>
package com.example.rhnaf.features.employee

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.ui.text.TextAlign
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rhnaf.shared.logic.VacationCalculator
import com.example.rhnaf.shared.model.Employee
import com.example.rhnaf.shared.model.EmployeeStatus
import com.example.rhnaf.ui.ViewModelFactory

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

    // Fetch employee when the employeeId changes
    LaunchedEffect(employeeId) {
        val emp = employeeViewModel.getEmployeeById(employeeId)
        employee = emp
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.employee_detail_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back))
                    }
                },
                actions = {
                    IconButton(
                        onClick = { /* TODO: Implement edit */ },
                        enabled = employee != null
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(id = R.string.edit))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding: PaddingValues ->
        if (employee == null) {
            // Show a placeholder while loading
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = androidx.ui.Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            // Employee data is available
            Column(modifier = Modifier.padding(innerPadding)) {
                // Employee header with avatar and name
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                        ) {
                            Text(
                                text = "${employee.firstName.take(1)}${employee.lastName.take(1)}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.align(alignment = Alignment.Center)
                            )
                        }

                        // Name and details
                        Column {
                            Text(
                                text = "${employee.firstName} ${employee.lastName}",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = employee.position,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${getString(R.string.department_label)}: ${employee.department}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Employment duration badge
                    val monthsEmployed = java.time.Duration.between(
                        java.time.LocalDate.parse(employee.entryDate).atStartOfDay(),
                        java.time.LocalDate.now().atStartOfDay()
                    ).toMonths()
                    val years = monthsEmployed / 12
                    val months = monthsEmployed % 12
                    val durationText = if (years > 0) "$years año${if (years > 1) \"s\" else \"\"} $meses mes${if (meses > 1) \"s\" else \"\"}" else "$meses mes${if (meses > 1) \"s\" else \"\"}"
                    Chip(
                        label = { Text("$durationText antigüedad") { /* We'll use stringResource for the text */ } },
                        colors = ChipDefaults.chipColors(
                            backgroundColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }

                Divider(
                    color = MaterialTheme.colorScheme.divider,
                    thickness = 1.dp
                )

                // Details section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Personal Info
                    Text(
                        text = stringResource(id = R.string.personal_info),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Photo placeholder (could be replaced with actual image)
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            InfoRow(
                                label = stringResource(id = R.string.first_name),
                                value = employee.firstName
                            )
                            InfoRow(
                                label = stringResource(id = R.string.last_name),
                                value = employee.lastName
                            )
                            InfoRow(
                                label = stringResource(id = R.string.curp_label),
                                value = employee.curp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Employment Info
                    Text(
                        text = stringResource(id = R.string.job_info),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column {
                        InfoRow(
                            label = stringResource(id = R.string.position),
                            value = employee.position
                        )
                        InfoRow(
                            label = stringResource(id = R.string.department_label),
                            value = employee.department
                        )
                        InfoRow(
                            label = stringResource(id = R.string.hire_date_label),
                            value = employee.entryDate
                        )
                        InfoRow(
                            label = stringResource(id = R.string.supervisor_label),
                            value = employee.supervisorName ?: getString(R.string.not_assigned) // We'll need to add this string
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Identity & Fiscal
                    Text(
                        text = stringResource(id = R.string.identity_fiscal_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column {
                        InfoRow(
                            label = stringResource(id = R.string.curp_label),
                            value = employee.curp
                        )
                        InfoRow(
                            label = stringResource(id = R.string.rfc_label),
                            value = employee.rfc
                        )
                        InfoRow(
                            label = stringResource(id = R.string.nss_label),
                            value = employee.nss = Resource: R.string.nss") // We'll need a string for notnull:employee.nss ?: getString(R.string.not_registered) // We'll need to add this string
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Equipment
                    Text(
                        text = stringResource(id = R.string.equipment_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // We would normally collect equipment from the equipmentViewModel, but for simplicity, we show a placeholder.
                    // TODO: Replace with actual equipment list from equipmentViewModel
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.no_equipment_assigned),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { /* TODO: Register equipment */ },
                            enabled = true
                        ) {
                            Text(text = stringResource(id = R.string.register_equipment))
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Vacation Control
                    Text(
                        text = stringResource(id = R.string.vacation_control_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(id = R.string.days_earned),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${VacationCalculator.calculateVacationDays(employee.entryDate)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(id = R.string.days_available),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${VacationCalculator.calculateAvailableVacationDays(employee.entryDate, employee.vacationDaysTaken)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Performance History
                    Text(
                        text = stringResource(id = R.string.performance_history_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // In a real app, we would collect performance data from the performanceViewModel
                    // For now, we show a placeholder.
                    // We'll check if there are any performance records; for now, we assume none.
                    if (true) { // Replace with actual condition when we have data
                        // Placeholder for no evaluations
                        Text(
                            text = stringResource(id = R.string.no_evaluations),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        // List of evaluations would go here
                    }
                }
            }
        }
    }
}

// Helper composable for displaying a label-value pair
@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp, 0.dp)
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            weight = 0.4f
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            weight = 0.6f
        )
    }
}