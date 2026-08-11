package com.example.rhnaf.web.modules

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.browser.window
import io.ktor.client.*
import io.ktor.client.engine.js.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.client.request.*
import io.ktor.client.call.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import com.example.rhnaf.domain.model.UserRole
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.html.InputType
import org.jetbrains.compose.web.html.Button
import org.jetbrains.compose.web.html.Div
import org.jetbrains.compose.web.html.H3
import org.jetbrains.compose.web.html.H4
import org.jetbrains.compose.web.html.P
import org.jetbrains.compose.web.html.Input
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
import org.jetbrains.compose.web.html.Span

/**
 * User Management Module - User administration and role management
 */
@Composable
fun UserManagementModule(
    client: HttpClient,
    scope: kotlinx.coroutines.CoroutineScope,
    t: (String) -> String
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("Production") }

    var users by remember { mutableStateOf(emptyList<Map<String, String>>()) }
    var isLoading by remember { mutableStateOf(true) }

    // Load users on init
    init {
        loadUsers()
    }

    private fun loadUsers() {
        // Reset loading state
        isLoading = true

        // Launch coroutine to fetch users
        scope.launch {
            try {
                val response = client.get("$BACKEND_URL/api/users")
                users = response.body()
            } catch (e: Exception) {
                println("Error loading users: ${e.message}")
                window.alert("Error loading users: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    // Main container
    Div {
        +"background-color: white"
        +"padding: 2rem"
        +"border-radius: 0.75rem"
        +"box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06)"
        +"margin-bottom: 2rem"
    } {
        H3 {
            +t("user_mgmt") // "User Management"
            +"margin-bottom: 1rem"
        }
        P {
            +"color: #64748b"
            +"Administration of access and system roles."
            +"margin-bottom: 1.5rem"
        }

        // Create User Form
        Div {
            +"padding: 1.5rem"
            +"background-color: #f8fafc"
            +"border-radius: 0.75rem"
            +"border: 1px solid #e2e8f0"
            +"margin-bottom: 2rem"
        } {
            H4 {
                +"Create New Account"
                +"margin-bottom: 1rem"
            }

            Div {
                +"display: flex"
                +"gap: 1rem"
                +"flex-wrap: wrap"
                +"align-items: center"
            } {
                // Name Input
                Div {
                    +"display: flex"
                    +"flex-direction: column"
                    +"flex: 1"
                    +"min-width: 200px"
                } {
                    Label {
                                              +"font-size: 0.875rem"
                                              +"color: #64748b"
                                              +"display: block"
                    } {
                                              +"Full Name"
                    }
                    Input(InputType.Text) {
                                              +"placeholder: Full Name"
                                              +"value: ${name}"
                                              +"oninput: this.value"
                                              +"style: padding: 0.75rem; border-radius: 0.375rem; border: 1px solid #ddd; flex: 1"
                    }
                }

                // Email Input
                Div {
                    +"display: flex"
                    +"flex-direction: column"
                    +"flex: 1"
                    +"min-width: 200px"
                } {
                    Label {
                                              +"font-size: 0.875rem"
                                              +"color: #64748b"
                                              +"display: block"
                    } {
                                              +"Email"
                    }
                    Input(InputType.Email) {
                                              +"placeholder: Email"
                                              +"value: ${email}"
                                              +"oninput: this.value"
                                              +"style: padding: 0.75rem; border-radius: 0.375rem; border: 1px solid #ddd; flex: 1"
                    }
                }

                // Password Input
                Div {
                    +"display: flex"
                    +"flex-direction: column"
                    +"flex: 1"
                    +"min-width: 200px"
                } {
                    Label {
                                              +"font-size: 0.875rem"
                                              +"color: #64748b"
                                              +"display: block"
                    } {
                                              +"Password"
                    }
                    Input(InputType.Password) {
                                              +"placeholder: Password"
                                              +"value: ${password}"
                                              +"oninput: this.value"
                                              +"style: padding: 0.75rem; border-radius: 0.375rem; border: 1px solid #ddd; flex: 1"
                    }
                }

                // Department/Role Selector
                Div {
                    +"display: flex"
                    +"flex-direction: column"
                } {
                    Label {
                                              +"font-size: 0.875rem"
                                              +"color: #64748b"
                                              +"display: block"
                    } {
                                              +"Department / Role"
                    }
                    Div {
                        +"display: flex"
                        +"gap: 0.5rem"
                    } {
                        val deps = listOf("Direction", "Human Resources", "Purchasing", "Maintenance", "Security", "Warehouse", "Import/Export", "Production")
                        deps.forEach { dep ->
                            Button {
                                              +"padding: 0.5rem 1rem"
                                              +"font-size: 0.875rem"
                                              +"border-radius: 0.25rem"
                                              +"cursor: pointer"
                                              +"background-color: ${if (department == dep) "#2563eb" else "white"}"
                                              +"color: ${if (department == dep) "white" else "black"}"
                                              +"border: ${if (department == dep) "none" else "1px solid #ccc"}"
                                              +"transition: all 0.2s ease"

                                              onClick { department = dep }
                            } {
                                              +dep
                            }
                        }
                    }
                }

                // Create Button
                Button {
                                              +"padding: 0.75rem 1.5rem"
                                              +"background-color: #22c55e"
                                              +"color: white"
                                              +"border: none"
                                              +"border-radius: 0.375rem"
                                              +"cursor: pointer"
                                              +"font-weight: bold"

                                              onclick {
                                                  if (email.isNotBlank() && password.isNotBlank()) {
                                                      // Map department to role
                                                      val role = when (department) {
                                                          "Direction" -> "ADMIN"
                                                          "Human Resources" -> "RH"
                                                          "Purchasing" -> "COMPRAS"
                                                          "Maintenance" -> "MANTENIMIENTO"
                                                          "Security" -> "SEGURIDAD"
                                                          "Warehouse" -> "ALMACEN"
                                                          "Import/Export" -> "IMPORT_EXPORT"
                                                          else -> "EMPLEADO"
                                                      }

                                                      scope.launch {
                                                          try {
                                                              client.post("$BACKEND_URL/api/user/add") {
                                                                  contentType(ContentType.Application.Json)
                                                                  setBody(mapOf(
                                                                      "email" to email,
                                                                      "password" to password,
                                                                      "role" to role,
                                                                      "name" to name
                                                                  ))
                                                              }
                                                              // Reload users
                                                              loadUsers()
                                                              window.alert("User $email created successfully.")
                                                              // Clear form
                                                              email = ""
                                                              password = ""
                                                              name = ""
                                                          } catch (e: Exception) {
                                                              window.alert("Error: ${e.message}")
                                                          }
                                                  }
                                              }
                } {
                                              +"Create User"
                }
            }
        }

        // Users List
        if (isLoading) {
            P {
                                              +"text-align: center"
                                              +"padding: 2rem"
                                              +"color: #64748b"
                                              +"Loading users..."
            }
        } else {
            Table {
                                              +"width: 100%"
                                              +"border-collapse: collapse"
            } {
                Thead {
                    Tr {
                                                      +"text-align: left"
                                                      +"padding: 0.75rem"
                                                      +"border-bottom: 2px solid #f1f5f9"
                    } {
                                              +"Email"
                    }
                                                      +"text-align: left"
                                                      +"padding: 0.75rem"
                                                      +"border-bottom: 2px solid #f1f5f9"
                    } {
                                              +"Name"
                    }
                                                      +"text-align: left"
                                                      +"padding: 0.75rem"
                                                      +"border-bottom: 2px solid #f1f5f9"
                    } {
                                              +"Role"
                    }
                                                      +"text-align: left"
                                                      +"padding: 0.75rem"
                                                      +"border-bottom: 2px solid #f1f5f9"
                    } {
                                              +"Status"
                    }
                }
                Tbody {
                    users.forEach { user ->
                        Tr {
                                                      +"padding: 0.75rem"
                                                      +"border-bottom: 1px solid #f1f5f9"
                        } {
                                              +(user["email"] ?: "")
                        }
                                                      +"padding: 0.75rem"
                                                      +"border-bottom: 1px solid #f1f5f9"
                        } {
                                              +(user["name"] ?: "")
                        }
                                                      +"padding: 0.75rem"
                                                      +"border-bottom: 1px solid #f1f5f9"
                        } {
                                              Span {
                                                      +"background-color: #e2e8f0"
                                                      +"padding: 0.25rem 0.5rem"
                                                      +"border-radius: 0.25rem"
                                                      +"font-size: 0.875rem"
                                              } {
                                              +(user["role"] ?: "")
                                              }
                        }
                                                      +"padding: 0.75rem"
                                                      +"border-bottom: 1px solid #f1f5f9"
                        } {
                                              Span {
                                                      +"color: #166534"
                                                      +"font-weight: bold"
                                              } {
                                              +"ACTIVE"
                                              }
                        }
                    }
                }
            }
        }
    }
}