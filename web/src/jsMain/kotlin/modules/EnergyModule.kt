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
 * Energy Module - Energy consumption monitoring
 */
@Composable
fun EnergyModule(t: (String) -> String) {
    // Main container
    Div {
        +"background-color: white"
        +"padding: 2rem"
        +"border-radius: 0.75rem"
        +"box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06)"
    } {
        H3 {
            +t("energy") // "Energy Management"
            +"margin-bottom: 1.5rem"
        }

        // Energy monitoring card
        Div {
            +"height: 18rem"
            +"background-color: #0f172a"
            +"border-radius: 0.75rem"
            +"padding: 2rem"
            +"color: white"
            +"display: flex"
            +"align-items: baseline"
            +"gap: 0.75rem"
        } {
            H4 {
                +t("real_time_electricity_monitor") // "Real-Time Electricity Monitor"
                +"margin-bottom: 1rem"
            }

            // Current consumption
            Div {
                +"display: flex"
                +"align-items: baseline"
                +"gap: 0.75rem"
            } {
                H1 {
                    +"font-size: 4rem"
                    +"color: #fbbf24"
                    +"margin: 0"
                    +"42.5"
                }
                Span {
                    +"kW/h"
                }
            }

            // Peak consumption
            P {
                +"color: #94a3b8"
                +"${t("peak_consumption_today")}: 58.2 kW/h at 11:30 AM" // "Peak consumption today: 58.2 kW/h at 11:30 AM"
                +"margin-top: 1rem"
            }

            // Chart placeholder
            Div {
                +"width: 100%"
                +"height: 6.25rem"
                +"background-color: #1e293b"
                +"margin-top: 1.25rem"
                +"border-radius: 0.5rem"
                +"display: flex"
                +"align-items: center"
                +"justify-content: center"
            } {
                +"${t("consumption_wave_chart")}" // "Consumption Wave Chart"
            }
        }
    }
}