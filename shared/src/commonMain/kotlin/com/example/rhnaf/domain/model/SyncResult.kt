package com.example.rhnaf.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SyncResult(
    val synced: Int,
    val message: String
)
