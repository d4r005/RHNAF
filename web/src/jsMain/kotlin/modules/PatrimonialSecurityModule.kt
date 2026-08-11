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
import org.jetbrains.compose.web.html.H3
import org.jetbrains.compose.web.html.P
import org.jetbrains.compose.web.html.Div
import org.jetbrains.compose.web.html.Span
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

/**
 * Patrimonial Security Module - Asset Protection and Security Overview
 */
@Composable
fun PatrimonialSecurityModule(t: (String) -> String) {
    // Main container
    Div {
        +"background-color: white"
        +"padding: 2rem"
        +"border-radius: 0.75rem"
        +"box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06)"
    } {
        H3 {
            +t("patrimonial") // "Asset Protection"
            +"margin-bottom: 1.5rem"
        }

        // Security overview cards
        Div {
            +"display: grid"
            +"grid-template-columns: repeat(auto-fit, minmax(200px, 1fr))"
            +"gap: 1.5rem"
            +"margin-bottom: 2rem"
        } {
            // Security Status Card
            Div {
                +"padding: 1.5rem"
                +"background-color: #f8fafc"
                +"border-radius: 0.75rem"
                +"border-left: 4px solid #10b981"
            } {
                H4 {
                    +t("security_status") // "Security Status"
                    +"margin-bottom: 1rem"
                }
                Div {
                    +"display: flex"
                    +"align-items: center"
                    +"gap: 1rem"
                } {
                    Div {
                        +"width: 2.5rem"
                        +"height: 2.5rem"
                        +"background-color: #dcfce7"
                        +"border-radius: 0.5rem"
                        +"display: flex"
                        +"align-items: center"
                        +"justify-content: center"
                    } {
                        +"🛡️"
                    }
                    Div {
                        P {
                            +"font-size: 1.5rem"
                            +"font-weight: bold"
                            +"color: #10b981"
                            +"margin: 0"
                        }
                        P {
                            +"color: #64748b"
                            +"font-size: 0.875rem"
                            +"All systems secure"
                        }
                    }
                }
            }

            // Active Alerts Card
            Div {
                +"padding: 1.5rem"
                +"background-color: #fef2f2"
                +"border-radius: 0.75rem"
                +"border-left: 4px solid #ef4444"
            } {
                H4 {
                    +t("active_alerts") // "Active Alerts"
                    +"margin-bottom: 1rem"
                }
                P {
                    +"color: #991b1b"
                    +"font-weight: 500"
                    +"2 active alerts"
                }
                P {
                    +"color: #64748b"
                    +"font-size: 0.875rem"
                    +"margin-top: 0.5rem"
                    +"Requires attention"
                }
            }

            // Compliance Status Card
            Div {
                +"padding: 1.5rem"
                +"background-color: #f0f9ff"
                +"border-radius: 0.75rem"
                +"border-left: 4px solid #3b82f6"
            } {
                H4 {
                    +t("compliance") // "Compliance"
                    +"margin-bottom: 1rem"
                }
                Div {
                    +"display: flex"
                    +"align-items: center"
                    +"gap: 1rem"
                } {
                    Div {
                        +"width: 2.5rem"
                        +"height: 2.5rem"
                        +"background-color: #dbeafe"
                        +"border-radius: 0.5rem"
                        +"display: flex"
                        +"align-items: center"
                        +"justify-content: center"
                    } {
                        +"📋"
                    }
                    Div {
                        P {
                            +"font-size: 1.5rem"
                            +"font-weight: bold"
                            +"color: #3b82f6"
                            +"margin: 0"
                        }
                        P {
                            +"color: #64748b"
                            +"font-size: 0.875rem"
                            +"98% compliant"
                        }
                    }
                }
            }

            // Incidents This Month Card
            Div {
                +"padding: 1.5rem"
                +"background-color: #fffbeb"
                +"border-radius: 0.75rem"
                +"border-left: 4px solid #f59e0b"
            } {
                H4 {
                    +t("incidents_this_month") // "Incidents This Month"
                    +"margin-bottom: 1rem"
                }
                Div {
                    +"display: flex"
                    +"align-items: center"
                    +"gap: 1rem"
                } {
                    Div {
                        +"width: 2.5rem"
                        +"height: 2.5rem"
                        +"background-color: #ffedd5"
                        +"border-radius: 0.5rem"
                        +"display: flex"
                        +"align-items: center"
                        +"justify-content: center"
                    } {
                        +"⚠️"
                    }
                    Div {
                        P {
                            +"font-size: 1.5rem"
                            +"font-weight: bold"
                            +"color: #f59e0b"
                            +"margin: 0"
                        }
                        P {
                            +"color: #64748b"
                            +"font-size: 0.875rem"
                            +"3 incidents"
                        }
                    }
                }
            }
        }

        // Recent Security Events
        Div {
            +"padding: 1.5rem"
            +"background-color: #f8fafc"
            +"border-radius: 0.75rem"
        } {
            H3 {
                +t("recent_events") // "Recent Security Events"
                +"margin-bottom: 1rem"
            }
            Div {
                +"border: 1px solid #e2e8f0"
                +"border-radius: 0.5rem"
                +"overflow: hidden"
            } {
                // Event Item 1
                Div {
                    +"padding: 1.25rem"
                    +"border-bottom: 1px solid #e2e8f0"
                    +"display: flex"
                    +"align-items: center"
                    +"gap: 1rem"
                } {
                    Div {
                        +"width: 2.5rem"
                        +"height: 2.5rem"
                        +"background-color: #dcfce7"
                        +"border-radius: 0.5rem"
                        +"display: flex"
                        +"align-items: center"
                        +"justify-content: center"
                    } {
                        +"✅"
                    }
                    Div {
                        +"flex: 1"
                        P {
                            +"font-weight: 500"
                            +"margin-bottom: 0.25rem"
                            +"Access control system updated"
                        }
                        P {
                            +"color: #64748b"
                            +"font-size: 0.875rem"
                            +"2 hours ago"
                        }
                    }
                    Div {
                        +"padding: 0.25rem 0.75rem"
                        +"background-color: #bbf7d0"
                        +"border-radius: 0.25rem"
                        +"font-size: 0.75rem"
                        +"color: #166534"
                    } {
                        +"Resolved"
                    }
                }

                // Event Item 2
                Div {
                    +"padding: 1.25rem"
                    +"border-bottom: 1px solid #e2e8f0"
                    +"display: flex"
                    +"align-items: center"
                    +"gap: 1rem"
                } {
                    Div {
                        +"width: 2.5rem"
                        +"height: 2.5rem"
                        +"background-color: #fed7d7"
                        +"border-radius: 0.5rem"
                        +"display: flex"
                        +"align-items: center"
                        +"justify-content: center"
                    } {
                        +"🔍"
                    }
                    Div {
                        +"flex: 1"
                        P {
                            +"font-weight: 500"
                            +"margin-bottom: 0.25rem"
                            +"Unauthorized access attempt detected"
                        }
                        P {
                            +"color: #64748b"
                            +"font-size: 0.875rem"
                            +"4 hours ago"
                        }
                    }
                    Div {
                        +"padding: 0.25rem 0.75rem"
                        +"background-color: #fed7d7"
                        +"border-radius: 0.25rem"
                        +"font-size: 0.75rem"
                        +"color: #991b1b"
                    } {
                        +"Blocked"
                    }
                }

                // Event Item 3
                Div {
                    +"padding: 1.25rem"
                    +"display: flex"
                    +"align-items: center"
                    +"gap: 1rem"
                } {
                    Div {
                        +"width: 2.5rem"
                        +"height: 2.5rem"
                        +"background-color: #ffedd5"
                        +"border-radius: 0.5rem"
                        +"display: flex"
                        +"align-items: center"
                        +"justify-content: center"
                    } {
                        +"⚠️"
                    }
                    Div {
                        +"flex: 1"
                        P {
                            +"font-weight: 500"
                            +"margin-bottom: 0.25rem"
                            +"Security camera maintenance required"
                        }
                        P {
                            +"color: #64748b"
                            +"font-size: 0.875rem"
                            +"1 day ago"
                        }
                    }
                    Div {
                        +"padding: 0.25rem 0.75rem"
                        +"background-color: #fef3c7"
                        +"border-radius: 0.25rem"
                        +"font-size: 0.75rem"
                        +"color: #92400e"
                    } {
                        +"Pending"
                    }
                }
            }
        }
    }
}