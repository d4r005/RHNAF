package com.example.rhnaf.web.modules

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.px
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import com.example.rhnaf.domain.model.UserRole
import io.ktor.client.*
import io.ktor.client.engine.js.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.client.request.*
import io.ktor.client.call.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.html.InputType
import org.jetbrains.compose.web.html.Button
import org.jetbrains.compose.web.html.Div
import org.jetbrains.compose.web.html.H1
import org.jetbrains.compose.web.html.H2
import org.jetbrains.compose.web.html.H3
import org.jetbrains.compose.web.html.H4
import org.jetbrains.compose.web.html.P
import org.jetbrains.compose.web.html.Span
import org.jetbrains.compose.web.html.Input
import org.jetbrains.compose.web.html.TextArea
import org.jetbrains.compose.web.html.Select
import org.jetbrains.compose.web.html.Option
import org.jetbrains.compose.web.html.Label
import org.jetbrains.compose.web.css.BackgroundColor
import org.jetbrains.compose.web.css.BorderRadius
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.FlexWrap
import org.jetbrains.compose.web.css.FontWeight
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.Padding
import org.jetbrains.compose.web.css.Property
import org.jetbrains.compose.web.css.TextAlign as CssTextAlign
import org.jetbrains.compose.web.css.Cursor
import org.jetbrains.compose.web.css.Width
import org.jetbrains.compose.web.css.Height
import org.jetbrains.compose.web.css.Margin

/**
 * Finance Module - Financial dashboard and reporting
 */
@Composable
fun FinanceModule(t: (String) -> String) {
    // Main container
    Div {
        +"background-color: white"
        +"padding: 2rem"
        +"border-radius: 0.75rem"
        +"box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06)"
    } {
        // Financial section
        Div {
            +"display: grid"
            +"grid-template-columns: 1fr 1fr"
            +"gap: 1.5rem"
            +"margin-bottom: 2rem"
        } {
            // Income Statement
            Div {
                +"padding: 1.5rem"
                +"background-color: #f8fafc"
                +"border-radius: 0.75rem"
            } {
                H4 {
                    +t("financial_statement") // "Financial Statement (Monthly)"
                    +"margin-bottom: 1rem"
                }
                P {
                    +"${t("revenues")}: $2,450,000" // "Revenues: $2,450,000"
                    +"margin-bottom: 0.5rem"
                }
                P {
                    +"${t("operating_expenses")}: $1,800,000" // "Operating Expenses: $1,800,000"
                    +"margin-bottom: 0.5rem"
                }
                H3 {
                    +"color: #10b981"
                    +"${t("profit")}: $650,000" // "Profit: $650,000"
                }
            }

            // Payroll Cost
            Div {
                +"padding: 1.5rem"
                +"background-color: #f8fafc"
                +"border-radius: 0.75rem"
            } {
                H4 {
                    +t("current_payroll_cost") // "Current Payroll Cost"
                    +"margin-bottom: 1rem"
                }
                H3 {
                    +"$458,200.00"
                }
                P {
                    +"${t("includes_taxes_benefits")}" // "Includes taxes and benefits."
                }
            }
        }
    }
}