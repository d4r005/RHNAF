package com.example.rhnaf.shared.logic

import kotlinx.datetime.*

object VacationCalculator {
    fun calculateVacationDays(entryDate: String): Int {
        return try {
            val entry = LocalDate.parse(entryDate)
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            
            var years = today.year - entry.year
            if (today.monthNumber < entry.monthNumber || 
                (today.monthNumber == entry.monthNumber && today.dayOfMonth < entry.dayOfMonth)) {
                years--
            }
            
            when {
                years < 1 -> 0
                years == 1 -> 12
                years == 2 -> 14
                years == 3 -> 16
                years == 4 -> 18
                years == 5 -> 20
                years in 6..10 -> 22
                years in 11..15 -> 24
                years in 16..20 -> 26
                years in 21..25 -> 28
                years in 26..30 -> 30
                else -> 32
            }
        } catch (e: Exception) {
            0
        }
    }
}
