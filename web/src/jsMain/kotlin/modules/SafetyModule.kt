package com.example.rhnaf.web.modules

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.attributes.*
import io.ktor.client.*
import io.ktor.client.engine.js.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.client.request.*
import io.ktor.client.call.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.jetbrains.compose.web.html.*
import org.jetbrains.compose.web.css.BackgroundColor
import org.jetbrains.compose.web.css.BorderRadius
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.FontWeight
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.Padding
import org.jetbrains.compose.web.css.Property
import org.jetbrains.compose.web.css.Cursor
import org.jetbrains.compose.web.css.Width
import org.jetbrains.compose.web.css.Height
import org.jetbrains.compose.web.css.Margin
import org.jetbrains.compose.web.css.TextAlign as CssTextAlign
import kotlinx.coroutines.launch

/**
 * Safety Module - EHS Incident Management and Safety Analytics
 */
@Composable
fun SafetyModule(
    client: HttpClient,
    scope: kotlinx.coroutines.CoroutineScope,
    t: (String) -> String
) {
    var desc by remember { mutableStateOf("") }
    var res by remember { mutableStateOf("") }
    var isAnalyzing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val audits = remember { mutableStateListOf("2024-06-30" to "Producción B", "2024-06-28" to "Aduanas") }

    // Main container
    Div {
        +"background-color: white"
        +"padding: 2rem"
        +"border-radius: 0.75rem"
        +"box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06)"
    } {
        H3 {
            +t("incidents") // "Incidents"
            +"margin-bottom: 1rem"
        }
        P {
            +"color: #64748b"
            +"Investigation of EHS Incidents with Artificial Intelligence"
            +"margin-bottom: 1.5rem"
        }

        Div {
            +"display: grid"
            +"grid-template-columns: 1fr 1fr"
            +"gap: 1.5rem"
            +"margin-bottom: 2rem"
        } {
            // Days without accidents
            Div {
                +"padding: 1.5rem"
                +"background-color: #fef2f2"
                +"border-radius: 0.75rem"
                +"border-left: 4px solid #ef4444"
            } {
                H4 {
                    +t("days_without_accidents") // "Days Without Accidents"
                    +"margin-bottom: 0.5rem"
                }
                H1 {
                    +"font-size: 2.5rem"
                    +"color: #991b1b"
                    +"margin: 0"
                    +"342"
                }
                P {
                    +"color: #64748b"
                    +"font-size: 0.875rem"
                    +"Historical record: 500 days"
                }
            }

            // Safety inspections
            Div {
                +"padding: 1.5rem"
                +"background-color: #f0fdf4"
                +"border-radius: 0.75rem"
                +"border-left: 4px solid #22c55e"
            } {
                H4 {
                    +t("safety_inspections") // "Safety Inspections"
                    +"margin-bottom: 0.5rem"
                }
                P {
                    +"margin: 0.25rem 0"
                    +"● Plant: 100% Completed"
                }
                P {
                    +"margin: 0.25rem 0"
                    +"● Warehouse: Pending today"
                }
            }
        }

        // Report Incident / Predictive Analysis
        H4 {
            +t("report_incident_analysis") // "Report Incident / Predictive Analysis"
            +"margin-bottom: 1rem"
        }
        TextArea(
            value = desc,
            onValueChange = { desc = it },
            label = { +"Describe the incident" },
            placeholder = { +"Describe what happened..." }
        ) {
            +"width: 100%"
            +"height: 8rem"
            +"margin-bottom: 1rem"
            +"padding: 1rem"
            +"border-radius: 0.5rem"
            +"border: 1px solid #e2e8f0"
            +"box-sizing: border-box"
        }

        // Error message
        error?.let {
            Div {
                +"margin-top: 0.5rem"
                +"padding: 1rem"
                +"background-color: #fee2e2"
                +"border-radius: 0.5rem"
                +"border-left: 4px solid #ef4444"
            } {
                +"color: #991b1b"
                +"font-size: 0.875rem"
            } {
                +it
            }
        }

        Button(
            onClick = {
                if (desc.isNotBlank()) {
                    error = null
                    isAnalyzing = true
                    scope.launch {
                        try {
                            val response = client.post("$BACKEND_URL/api/safety/analyze") {
                                contentType(ContentType.Application.Json)
                                setBody(mapOf("description" to desc))
                            }
                            val body = response.body()
                            res = body["analysis"] ?: ""
                        } catch (e: Exception) {
                            println("Error analyzing incident: ${e.message}")
                            error = "Error analyzing incident: ${e.message}"
                        } finally {
                            isAnalyzing = false
                        }
                    }
                }
            }
        ) {
            +"padding: 0.75rem 1.5rem"
            +"background-color: #0f172a"
            +"color: white"
            +"border: none"
            +"border-radius: 0.5rem"
            +"font-weight: bold"
            +"cursor: pointer"
            +"opacity: ${if (isAnalyzing) 0.7 else 1.0}"
        } {
            if (isAnalyzing) {
                +"Analyzing..."
            } else {
                +t("ai_analysis") // "AI Analysis"
            }
        }

        if (isAnalyzing && !res.isNotBlank()) {
            Div {
                +"margin-top: 1rem"
                +"text-align: center"
                +"color: #64748b"
            } {
                +"Analyzing incident..."
            }
        }

        if (res.isNotBlank()) {
            Div {
                +"margin-top: 1.5rem"
                +"padding: 1.5rem"
                +"background-color: #f0f9ff"
                +"border-left: 4px solid #2563eb"
                +"border-radius: 0.375rem"
            } {
                H4 {
                    +t("risk_analysis") // "Risk Analysis:"
                    +"margin-bottom: 0.75rem"
                }
                +"white-space: pre-line"
                +"font-size: 0.875rem"
                +"color: #1e293b"
            } {
                +res
            }
        }

        Div {
            +"margin-top: 2.5rem"
        } {
            H4 {
                +t("safety_audits") // "Safety Audits"
                +"margin-bottom: 1rem"
            }
            Div {
                +"display: flex"
                +"align-items: center"
                +"gap: 0.75rem"
                +"margin-bottom: 1rem"
            } {
                Button {
                    +"padding: 0.5rem 1rem"
                    +"font-size: 0.875rem"
                    +"border-radius: 0.375rem"
                    +"cursor: pointer"
                    +"background-color: #2563eb"
                    +"color: white"
                    +"border: none"
                } {
                    +"📤"
                    +" "+t("upload_audit") // "Upload Audit (PDF)"
                }
                onClick { /* File upload logic would go here */ }
            }
            Div {
                +"width: 100%"
                +"border-collapse: collapse"
            } {
                Thead {
                    Tr {
                        Th {
                            +"padding: 0.75rem"
                            +"text-align: left"
                            +"border-bottom: 2px solid #f1f5f9"
                        } {
                            +t("date") // "Date"
                        }
                        Th {
                            +"padding: 0.75rem"
                            +"text-align: left"
                            +"border-bottom: 2px solid #f1f5f9"
                        } {
                            +t("area") // "Area"
                        }
                        Th {
                            +"padding: 0.75rem"
                            +"text-align: left"
                            +"border-bottom: 2px solid #f1f5f9"
                        } {
                            +t("inspector") // "Inspector"
                        }
                        Th {
                            +"padding: 0.75rem"
                            +"text-align: left"
                            +"border-bottom: 2px solid #f1f5f9"
                        } {
                            +t("result") // "Result"
                        }
                    }
                }
                Tbody {
                    // Sample data - in a real app this would come from an API
                    +"2024-06-30" to "Producción B" to "Ing. Martínez" to "APPROVED"
                    +"2024-06-28" to "Aduanas" to "Ing. Martínez" to "PENDING"
                } {
                    Tr {
                        Td {
                            +"padding: 0.75rem"
                            +"border-bottom": "1px solid #f1f5f9"
                        } {
                            +it.first
                        }
                        Td {
                            +"padding: 0.75rem"
                            +"border-bottom": "1px solid #f1f5f9"
                        } {
                            +it.second.first
                        }
                        Td {
                            +"padding: 0.75rem"
                            +"border-bottom": "1px solid #f1f5f9"
                        } {
                            +it.second.second
                        }
                        Td {
                            +"padding: 0.75rem"
                            +"border-bottom": "1px solid #f1f5f9"
                            +"display: flex"
                            +"align-items: center"
                        } {
                            Span {
                                +"padding: 0.25rem 0.5rem"
                                +"border-radius: 0.25rem"
                                +"font-size: 0.75rem"
                                +"font-weight: bold"
                            } {
                                +(when (it.third) {
                                    "APPROVED" -> #{"background-color: #dcfce7"; "color: #166534"}
                                    "PENDING" -> #{"background-color: #fef3c7"; "color: #92400e"}
                                    "REJECTED" -> #{"background-color: #fee2e2"; "color: #991b1b"}
                                    else -> #{"background-color: #e2e8f0"; "color: #64748b"}
                                }) {
                                    +it.third
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}