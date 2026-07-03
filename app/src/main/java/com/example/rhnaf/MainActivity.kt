package com.example.rhnaf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.rhnaf.features.dashboard.DashboardScreen
import com.example.rhnaf.features.employee.AddEmployeeScreen
import com.example.rhnaf.features.employee.EmployeeDetailScreen
import com.example.rhnaf.features.employee.SignatureScreen
import com.example.rhnaf.features.employee.EmployeeListScreen
import com.example.rhnaf.features.employee.EmployeePortalScreen
import com.example.rhnaf.features.dashboard.SupervisorPortalScreen
import com.example.rhnaf.features.dashboard.NotificationsScreen
import com.example.rhnaf.features.dashboard.ReportsScreen
import com.example.rhnaf.features.employee.PayrollScreen
import com.example.rhnaf.features.attendance.AttendanceScreen
import com.example.rhnaf.features.attendance.ScannerScreen
import com.example.rhnaf.features.training.TrainingScreen
import com.example.rhnaf.features.safety.SafetyScreen
import com.example.rhnaf.navigation.Destination
import com.example.rhnaf.ui.theme.RHNAFTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RHNAFTheme {
                var currentDestination by remember { mutableStateOf<Destination>(Destination.Dashboard) }
                val backStack = remember { mutableStateListOf<Destination>() }

                fun navigateTo(destination: Destination) {
                    backStack.add(currentDestination)
                    currentDestination = destination
                }

                fun navigateBack() {
                    if (backStack.isNotEmpty()) {
                        currentDestination = backStack.removeAt(backStack.size - 1)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (val dest = currentDestination) {
                        is Destination.Dashboard -> DashboardScreen(
                            onNavigateToEmployeeList = { navigateTo(Destination.EmployeeList) },
                            onNavigateToAttendance = { navigateTo(Destination.Attendance) },
                            onNavigateToTraining = { navigateTo(Destination.Training) },
                            onNavigateToSafety = { navigateTo(Destination.Safety) },
                            onNavigateToPortal = { navigateTo(Destination.EmployeePortal) },
                            onNavigateToPayroll = { navigateTo(Destination.Payroll) },
                            onNavigateToSupervisor = { navigateTo(Destination.SupervisorPortal) },
                            onNavigateToNotifications = { navigateTo(Destination.Notifications) },
                            onNavigateToReports = { navigateTo(Destination.Reports) }
                        )
                        is Destination.EmployeeList -> EmployeeListScreen(
                            onNavigateBack = { navigateBack() },
                            onEmployeeClick = { id -> navigateTo(Destination.EmployeeDetail(id)) },
                            onAddEmployee = { navigateTo(Destination.AddEmployee) }
                        )
                        is Destination.AddEmployee -> AddEmployeeScreen(
                            onNavigateBack = { navigateBack() },
                            onNavigateToSignature = { navigateTo(Destination.Signature) }
                        )
                        is Destination.Attendance -> AttendanceScreen(
                            onNavigateBack = { navigateBack() },
                            onNavigateToScanner = { navigateTo(Destination.Scanner) }
                        )
                        is Destination.Scanner -> ScannerScreen(
                            onNavigateBack = { navigateBack() }
                        )
                        is Destination.Training -> TrainingScreen(
                            onNavigateBack = { navigateBack() }
                        )
                        is Destination.Safety -> SafetyScreen(
                            onNavigateBack = { navigateBack() }
                        )
                        is Destination.EmployeeDetail -> EmployeeDetailScreen(
                            employeeId = dest.employeeId,
                            onNavigateBack = { navigateBack() }
                        )
                        is Destination.EmployeePortal -> EmployeePortalScreen(
                            onNavigateBack = { navigateBack() }
                        )
                        is Destination.SupervisorPortal -> SupervisorPortalScreen(
                            onNavigateBack = { navigateBack() }
                        )
                        is Destination.Notifications -> NotificationsScreen(
                            onNavigateBack = { navigateBack() }
                        )
                        is Destination.Reports -> ReportsScreen(
                            onNavigateBack = { navigateBack() }
                        )
                        is Destination.Payroll -> PayrollScreen(
                            onNavigateBack = { navigateBack() }
                        )
                        is Destination.Signature -> SignatureScreen(
                            onNavigateBack = { navigateBack() }
                        )
                        else -> {
                            Text("Pantalla en construcción: $dest")
                        }
                    }
                }
            }
        }
    }
}
