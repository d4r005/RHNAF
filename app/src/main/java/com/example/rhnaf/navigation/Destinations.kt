package com.example.rhnaf.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface Destination : NavKey {
    @Serializable
    data object Dashboard : Destination

    @Serializable
    data object EmployeeList : Destination

    @Serializable
    data object AddEmployee : Destination

    @Serializable
    data class EmployeeDetail(val employeeId: String) : Destination

    @Serializable
    data object Attendance : Destination

    @Serializable
    data object Scanner : Destination

    @Serializable
    data object Training : Destination

    @Serializable
    data object TrainingDashboard : Destination

    @Serializable
    data object Signature : Destination

    @Serializable
    data object Notifications : Destination

    @Serializable
    data object Reports : Destination

    @Serializable
    data object Safety : Destination

    @Serializable
    data object Recruitment : Destination

    @Serializable
    data object Payroll : Destination

    @Serializable
    data object Vacations : Destination

    @Serializable
    data object Performance : Destination

    @Serializable
    data object Settings : Destination

    @Serializable
    data object EmployeePortal : Destination

    @Serializable
    data object SupervisorPortal : Destination
}

object Destinations {
    const val TRAINING_DASHBOARD = "training_dashboard"
    const val TRAINING_SCREEN = "training_screen"
    const val DASHBOARD = "dashboard"
    const val EMPLOYEE_LIST = "employee_list"
    const val ADD_EMPLOYEE = "add_employee"
    const val ATTENDANCE = "attendance"
    const val SCANNER = "scanner"
    const val SIGNATURE = "signature"
    const val NOTIFICATIONS = "notifications"
    const val REPORTS = "reports"
    const val SAFETY = "safety"
    const val RECRUITMENT = "recruitment"
    const val PAYROLL = "payroll"
    const val VACATIONS = "vacations"
    const val PERFORMANCE = "performance"
    const val SETTINGS = "settings"
    const val EMPLOYEE_PORTAL = "employee_portal"
    const val SUPERVISOR_PORTAL = "supervisor_portal"
}
