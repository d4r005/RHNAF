package com.example.rhnaf.api

import kotlinx.serialization.Serializable

@Serializable
data class HikvisionEventRequest(
    val dateTime: String,
    val deviceID: String,
    val AccessControllerEvent: AccessControllerEventData
)

@Serializable
data class AccessControllerEventData(
    val employeeNoString: String,
    val currentVerifyMode: String,
    val name: String? = null,
    val attendanceStatus: String? = null
)

@Serializable
data class HikvisionResponse(
    val statusString: String,
    val statusCode: Int
)
