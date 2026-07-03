package com.example.rhnaf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.rhnaf.features.attendance.AttendanceScreen
import com.example.rhnaf.features.attendance.ScannerScreen
import com.example.rhnaf.features.dashboard.DashboardScreen
import com.example.rhnaf.features.dashboard.NotificationsScreen
import com.example.rhnaf.features.dashboard.ReportsScreen
import com.example.rhnaf.features.dashboard.SupervisorPortalScreen
import com.example.rhnaf.features.employee.*
import com.example.rhnaf.features.safety.SafetyScreen
import com.example.rhnaf.features.training.TrainingScreen
import com.example.rhnaf.navigation.Destination
import com.example.rhnaf.ui.theme.RHNAFTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RHNAFTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RHNAFApp()
                }
            }
        }
    }
}

@Composable
fun RHNAFApp() {
    val backStack = rememberNavBackStack(Destination.Dashboard as NavKey)
    
    val myEntryProvider = entryProvider<NavKey> {
        entry<Destination.Dashboard> {
            DashboardScreen(
                onNavigateToEmployeeList = { backStack.add(Destination.EmployeeList) },
                onNavigateToAttendance = { backStack.add(Destination.Attendance) },
                onNavigateToTraining = { backStack.add(Destination.Training) },
                onNavigateToSafety = { backStack.add(Destination.Safety) },
                onNavigateToPortal = { backStack.add(Destination.EmployeePortal) },
                onNavigateToPayroll = { backStack.add(Destination.Payroll) },
                onNavigateToSupervisor = { backStack.add(Destination.SupervisorPortal) },
                onNavigateToNotifications = { backStack.add(Destination.Notifications) },
                onNavigateToReports = { backStack.add(Destination.Reports) }
            )
        }
        entry<Destination.EmployeeList> {
            EmployeeListScreen(
                onNavigateBack = { backStack.removeLastOrNull() },
                onEmployeeClick = { id -> backStack.add(Destination.EmployeeDetail(id)) },
                onAddEmployee = { backStack.add(Destination.AddEmployee) }
            )
        }
        entry<Destination.AddEmployee> {
            AddEmployeeScreen(
                onNavigateBack = { backStack.removeLastOrNull() },
                onNavigateToSignature = { backStack.add(Destination.Signature) }
            )
        }
        entry<Destination.EmployeeDetail> { key ->
            EmployeeDetailScreen(
                employeeId = key.employeeId,
                onNavigateBack = { backStack.removeLastOrNull() }
            )
        }
        entry<Destination.Attendance> {
            AttendanceScreen(
                onNavigateBack = { backStack.removeLastOrNull() },
                onNavigateToScanner = { backStack.add(Destination.Scanner) }
            )
        }
        entry<Destination.Scanner> {
            ScannerScreen(
                onNavigateBack = { backStack.removeLastOrNull() }
            )
        }
        entry<Destination.Training> {
            TrainingScreen(
                onNavigateBack = { backStack.removeLastOrNull() }
            )
        }
        entry<Destination.Safety> {
            SafetyScreen(
                onNavigateBack = { backStack.removeLastOrNull() }
            )
        }
        entry<Destination.EmployeePortal> {
            EmployeePortalScreen(
                onNavigateBack = { backStack.removeLastOrNull() }
            )
        }
        entry<Destination.SupervisorPortal> {
            SupervisorPortalScreen(
                onNavigateBack = { backStack.removeLastOrNull() }
            )
        }
        entry<Destination.Notifications> {
            NotificationsScreen(
                onNavigateBack = { backStack.removeLastOrNull() }
            )
        }
        entry<Destination.Reports> {
            ReportsScreen(
                onNavigateBack = { backStack.removeLastOrNull() }
            )
        }
        entry<Destination.Payroll> {
            PayrollScreen(
                onNavigateBack = { backStack.removeLastOrNull() }
            )
        }
        entry<Destination.Signature> {
            SignatureScreen(
                onNavigateBack = { backStack.removeLastOrNull() }
            )
        }
    }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = myEntryProvider,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        )
    )
}
