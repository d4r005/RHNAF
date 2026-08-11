package com.example.rhnaf.web.modules

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.put
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Weight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.VSpacer
import androidx.compose.foundation.layout.HSpacer
import androidx.compose.foundation.shape.CornerRadius
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.Default
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.px
import androidx.compose.ui.text.TextAlign
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.browser.window
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
import io.ktor.client.statement.*
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
import org.jetbrains.compose.web.html.H5
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
import androidx.compose.ui.Modifier.Companion.size

/**
 * Employee Portal Module - Self-service portal for employees
 */
@Composable
fun EmployeePortalModule(t: (String) -> String) {
    // State for the employee portal
    var selectedOption by remember { mutableStateOf("Mis Recibos") }
    var isLoading by remember { mutableStateOf(false) }
    var payslipData by remember { mutableStateOf(listOf<Map<String, String>>()) }
    var vacationRequest by remember { mutableStateOf(mapOf<String, String>()) }
    var clockInTime by remember { mutableStateOf("") }

    // Simulate data loading
    init {
        // In a real app, this would load from API
        payslipData = listOf(
            mapOf("month" to "Enero 2026", "amount" to "$1,250.00", "status" to "Pagado"),
            mapOf("month" to "Febrero 2026", "amount" to "$1,320.50", "status" to "Pagado"),
            mapOf("month" to "Marzo 2026", "amount" to "$1,280.75", "status" to "Procesando")
        )
    }

    // Main container
    Div {
        +"background-color: white"
        +"padding: 2rem"
        +"border-radius: 0.75rem"
        +"box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06)"

        +"margin-bottom: 2rem"

        H3 {
            +t("employee_portal")
            +"margin-bottom: 1.5rem"
        }

        // Navigation tabs
        Div {
            +"display: flex"
            +"gap: 0.5rem"
            +"margin-bottom: 1.5rem"
            +"border-bottom: 1px solid #e2e8f0"
            +"padding-bottom: 0.75rem"

            // Generate tabs dynamically
            +"// Tabs will be generated here"
        } {
            val tabs = listOf("Mis Recibos", "Solicitar Vacaciones", "Mi Reloj Checador", "Beneficios", "Capacitación", "Mensajes")
            tabs.forEach { tab ->
                Button {
                    +"padding: 0.5rem 1rem"
                    +"border-radius: 0.5rem"
                    +"cursor: pointer"
                    +"font-weight: ${if (selectedOption == tab) "bold" else "normal"}"
                    +"border: none"
                    +"background-color: ${if (selectedOption == tab) "#2563eb" else "#f1f5f9"}"
                    +"color: ${if (selectedOption == tab) "white" else "#334155"}"
                    +"transition: all 0.2s ease"

                    onClick { selectedOption = tab }
                } {
                    +tab
                }
            }
        }

        // Content based on selected option
        when (selectedOption) {
            "Mis Recibos" -> {
                // Pay stubs section
                Div {
                    +"margin-bottom: 1.5rem"
                } {
                    H4 {
                        +t("pay_stubs") // Need to add this to translations
                        +"margin-bottom: 1rem"
                    }

                    if (isLoading) {
                        P {
                            +"Loading pay stubs..."
                        }
                    } else if (payslipData.isEmpty()) {
                        P {
                            +"No pay stubs available"
                        }
                    } else {
                        Div {
                            +"display: grid"
                            +"grid-template-columns: repeat(auto-fill, minmax(250px, 1fr))"
                            +"gap: 1rem"
                        } {
                            payslipData.forEach { payslip ->
                                Div {
                                    +"padding: 1rem"
                                    +"border: 1px solid #e2e8f0"
                                    +"border-radius: 0.5rem"
                                    +"background-color: white"
                                } {
                                    H5 {
                                        +payslip["month"] ?: ""
                                        +"margin-bottom: 0.5rem"
                                    }
                                    P {
                                        +"Amount: ${payslip["amount"] ?: ""}"
                                    }
                                    P {
                                        +"Status: ${payslip["status"] ?: ""}"
                                    }
                                    Button {
                                        +"margin-top: 0.5rem"
                                        +"padding: 0.5rem 1rem"
                                        +"background-color: #2563eb"
                                        +"color: white"
                                        +"border-radius: 0.25rem"
                                        +"border: none"
                                        +"cursor: pointer"
                                        +"font-size: 0.75rem"
                                    } {
                                        +"Download PDF"
                                    }
                                }
                            }
                        }
                    }
                }
            }
            "Solicitar Vacaciones" -> {
                // Vacation request section
                Div {
                    +"margin-bottom: 1.5rem"
                } {
                    H4 {
                        +t("vacation_request") // Need to add this to translations
                        +"margin-bottom: 1rem"
                    }

                    Div {
                        +"display: flex"
                        +"flex-direction: column"
                        +"gap: 1rem"
                    } {
                        // Start date
                        Div {
                            +"display: flex"
                            +"flex-direction: column"
                        } {
                            Label {
                                              +"for: vacation-start-date"
                                              +"font-size: 0.875rem"
                                              +"color: gray"
                                              +"display: block"
                            } {
                                              +"Start Date"
                            }
                            Input(InputType.Date) {
                                              +"id: vacation-start-date"
                                              +"placeholder: Start date"
                                              +"value: ${vacationRequest["startDate"] ?: ""}"
                                              +"oninput: this.value"
                                              +"style: padding: 0.75rem; border-radius: 0.375rem; border: 1px solid #ddd; flex: 1"
                            }
                        }

                        // End date
                        Div {
                            +"display: flex"
                            +"flex-direction: column"
                        } {
                            Label {
                                              +"for: vacation-end-date"
                                              +"font-size: 0.875rem"
                                              +"color: gray"
                                              +"display: block"
                            } {
                                              +"End Date"
                            }
                            Input(InputType.Date) {
                                              +"id: vacation-end-date"
                                              +"placeholder: End date"
                                              +"value: ${vacationRequest["endDate"] ?: ""}"
                                              +"oninput: this.value"
                                              +"style: padding: 0.75rem; border-radius: 0.375rem; border: 1px solid #ddd; flex: 1"
                            }
                        }

                        // Days requested
                        Div {
                            +"display: flex"
                            +"flex-direction: column"
                        } {
                            Label {
                                              +"for: vacation-days"
                                              +"font-size: 0.875rem"
                                              +"color: gray"
                                              +"display: block"
                            } {
                                              +"Days Requested"
                            }
                            Input(InputType.Number) {
                                              +"id: vacation-days"
                                              +"placeholder: Days"
                                              +"value: ${(vacationRequest["days"] ?: "").toString()}"
                                              +"oninput: this.value"
                                              +"style: padding: 0.75rem; border-radius: 0.375rem; border: 1px solid #ddd; flex: 1"
                            }
                        }

                        // Submit button
                        Button {
                            +"padding: 0.75rem 1.5rem"
                            +"background-color: #22c55e"
                            +"color: white"
                            +"border: none"
                            +"border-radius: 0.375rem"
                            +"cursor: pointer"
                            +"font-weight: bold"

                            // Simulate API call
                            onclick {
                                // In real app, this would make API call
                                window.alert("Vacation request submitted successfully!")
                            }
                        } {
                            +"Submit Request"
                        }
                    }
                }
            }
            "Mi Reloj Checador" -> {
                // Time clock section
                Div {
                    +"text-align: center"
                    +"padding: 1.5rem"
                } {
                    H4 {
                        +t("time_clock") // Need to add this to translations
                        +"margin-bottom: 0.5rem"
                    }
                    P {
                        +"Status: Online"
                        +"margin-bottom: 1.5rem"
                    }

                    Div {
                        +"margin: 1.5rem 0"
                        +"padding: 1.25rem"
                        +"background-color: #f0f9ff"
                        +"border-radius: 0.75rem"
                    } {
                        P {
                            +"font-size: 1rem"
                            +"color: #1e293b"
                            +"margin-bottom: 1rem"
                            +"Last punch: ${if (clockInTime.isNotEmpty()) clockInTime else "No punches today"}"
                        }
                        Button {
                            +"padding: 0.75rem 1.5rem"
                            +"background-color: ${if (clockInTime.isNotEmpty()) "#ef4444" else "#22c55e"}"
                            +"color: white"
                            +"border: none"
                            +"border-radius: 0.5rem"
                            +"cursor: pointer"
                            +"font-weight: bold"
                            +"font-size: 1rem"

                            // Simulate punch in/out
                            onclick {
                                // In real app, this would make API call
                                if (clockInTime.isEmpty()) {
                                    // Clock in
                                    clockInTime = "${java.time.LocalDateTime.now().getHour()}:${java.time.LocalDateTime.now().getMinute()}"
                                } else {
                                    // Clock out
                                    clockInTime = ""
                                }
                            }
                        } {
                            +"${if (clockInTime.isNotEmpty()) "Clock Out" else "Clock In"}"
                        }
                    }
                }
            }
            "Beneficios" -> {
                // Benefits section
                Div {
                    H4 {
                        +t("benefits") // Need to add this to translations
                        +"margin-bottom: 1rem"
                    }

                    Div {
                        +"display: grid"
                        +"grid-template-columns: repeat(auto-fill, minmax(200px, 1fr))"
                        +"gap: 1rem"
                    } {
                        // Medical Insurance
                        Div {
                            +"padding: 1rem"
                            +"border: 1px solid #e2e8f0"
                            +"border-radius: 0.5rem"
                            +"text-align: center"
                        } {
                            // Icon placeholder
                            Div {
                                +"width: 2.5rem"
                                +"height: 2.5rem"
                                +"background-color: #dbeafe"
                                +"border-radius: 50%"
                                +"margin: 0 auto 0.5rem"
                                +"display: flex"
                                +"align-items: center"
                                +"justify-content: center"
                            } {
                                // Medical icon would go here
                                +"🏥"
                            }
                            P {
                                              +"font-weight: bold"
                                              +"margin-bottom: 0.25rem"
                                              +"Health Insurance"
                            }
                            P {
                                              +"font-size: 0.875rem"
                                              +"color: #64748b"
                                              +"Active coverage"
                            }
                        }

                        // Meal Vouchers
                        Div {
                            +"padding: 1rem"
                            +"border: 1px solid #e2e8f0"
                            +"border-radius: 0.5rem"
                            +"text-align: center"
                        } {
                            Div {
                                +"width: 2.5rem"
                                +"height: 2.5rem"
                                +"background-color: #dcfce7"
                                +"border-radius: 50%"
                                +"margin: 0 auto 0.5rem"
                                +"display: flex"
                                +"align-items: center"
                                +"justify-content: center"
                            } {
                                              🍽️
                            }
                            P {
                                              +"font-weight: bold"
                                              +"margin-bottom: 0.25rem"
                                              +"Meal Vouchers"
                            }
                            P {
                                              +"font-size: 0.875rem"
                                              +"color: #64748b"
                                              +"$800.00 monthly"
                            }
                        }

                        // Free Courses
                        Div {
                            +"padding: 1rem"
                            +"border: 1px solid #e2e8f0"
                            +"border-radius: 0.5rem"
                            +"text-align: center"
                        } {
                            Div {
                                +"width: 2.5rem"
                                +"height: 2.5rem"
                                +"background-color: #dbeafe"
                                +"border-radius: 50%"
                                +"margin: 0 auto 0.5rem"
                                +"display: flex"
                                +"align-items: center"
                                +"justify-content: center"
                            } {
                                              📚
                            }
                            P {
                                              +"font-weight: bold"
                                              +"margin-bottom: 0.25rem"
                                              +"Free Courses"
                            }
                            P {
                                              +"font-size: 0.875rem"
                                              +"color: #64748b"
                                              +"5 available this month"
                            }
                        }
                    }
                }
            }
            "Capacitación" -> {
                // Training section
                Div {
                    H4 {
                        +t("training") // Need to add this to translations
                        +"margin-bottom: 1rem"
                    }

                    Div {
                        +"display: grid"
                        +"grid-template-columns: repeat(auto-fill, minmax(280px, 1fr))"
                        +"gap: 1rem"
                    } {
                        // Sample courses
                        val courses = listOf(
                            mapOf("name" to "Industrial Safety", "progress" to "75%", "status" to "In Progress"),
                            mapOf("name" to "First Aid", "progress" to "100%", "status" to "Completed"),
                            mapOf("name" to "Hazardous Materials Handling", "progress" to "30%", "status" to "Pending")
                        )

                        courses.forEach { course ->
                            Div {
                                +"padding: 1rem"
                                +"border: 1px solid #e2e8f0"
                                +"border-radius: 0.5rem"
                                +"background-color: white"
                            } {
                                H5 {
                                    +course["name"] ?: ""
                                    +"margin-bottom: 0.5rem"
                                }

                                // Progress bar
                                Div {
                                    +"height: 0.5rem"
                                    +"background-color: #e2e8f0"
                                    +"border-radius: 0.25rem"
                                    +"margin-bottom: 0.5rem"
                                    +"overflow: hidden"
                                } {
                                    Div {
                                        +"height: 100%"
                                        +"width: ${when (course["progress"] ?: "0%") {
                                            "75%" -> "75%"
                                            "100%" -> "100%"
                                            "30%" -> "30%"
                                            else -> "0%"
                                        }}"
                                        +"background-color: #2563eb"
                                    }
                                }

                                Div {
                                    +"display: flex"
                                    +"justify-content: space-between"
                                    +"font-size: 0.875rem"
                                } {
                                    Spacer {}
                                    +"${course["progress"] ?: "0%"} Completed"
                                    Spacer {
                                        +"width: 1rem"
                                    }
                                    +(when (course["status"] ?: "") {
                                        "Completed" -> "<span style='color: #22c55e; font-weight: bold'>Completed</span>"
                                        "In Progress" -> "<span style='color: #f59e0b; font-weight: bold'>In Progress</span>"
                                        else -> "<span style='color: #ef4444; font-weight: bold'>Pending</span>"
                                    })
                                }
                            }
                        }
                    }
                }
            }
            "Mensajes" -> {
                // Messages section
                Div {
                    H4 {
                        +t("messages") // Need to add this to translations
                        +"margin-bottom: 1rem"
                    }

                    Div {
                        +"height: 12rem"
                        +"overflow-y: auto"
                        +"border: 1px solid #e2e8f0"
                        +"border-radius: 0.5rem"
                        +"margin-bottom: 1rem"
                    } {
                        val messages = listOf(
                            mapOf("title" to "Vacation Policy Updated", "date" to "15/03/2026", "content" to "The annual vacation policy has been updated..."),
                            mapOf("title" to "Mandatory Safety Training", "date" to "10/03/2026", "content" => "Reminder: Complete safety training by 30/03"),
                            mapOf("title" to "Temporary Schedule Change", "date" to "05/03/2026", "content" => "Due to plant maintenance, schedule adjusted this week")
                        )

                        messages.forEach { msg ->
                            Div {
                                              +"padding: 0.75rem"
                                              +"border-bottom: 1px solid #f1f5f9"
                                              +"background-color: ${if (msg["title"] == "Vacation Policy Updated") "#eff6ff" else "white"}"
                            } {
                                H5 {
                                              +msg["title"] ?: ""
                                              +"margin-bottom: 0.25rem"
                                }
                                P {
                                              +"font-size: 0.875rem"
                                              +"color: #64748b"
                                              +(msg["date"] ?: "")
                                }
                                P {
                                              +msg["content"] ?: ""
                                }
                            }
                        }
                    }

                    Button {
                                              +"padding: 0.5rem 1rem"
                                              +"background-color: #6366f1"
                                              +"color: white"
                                              +"border: none"
                                              +"border-radius: 0.25rem"
                                              +"cursor: pointer"
                    } {
                                              +"Mark all as read"
                    }
                }
            }
        }
    }
}