package com.example.rhnaf.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ImportResult(
    val totalRows: Int,
    val imported: Int,
    val skippedDuplicates: Int,
    val skippedInvalid: Int,
    val skippedDailyLimit: Int = 0,
    val message: String
)
