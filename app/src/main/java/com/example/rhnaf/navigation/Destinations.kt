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
