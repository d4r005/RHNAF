import com.example.rhnaf.shared.model.*
import com.example.rhnaf.domain.model.*
import androidx.compose.runtime.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.attributes.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.launch
import kotlinx.browser.window

@Composable
fun AttendanceModule(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope, t: Translations) {
    var logs by remember { mutableStateOf(emptyList<AttendanceLog>()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var filterDate by remember { mutableStateOf("") }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey) {
        isLoading = true
        errorMsg = ""
        try {
            val resp = client.get("$BACKEND_URL/api/v1/asistencia/logs")
            if (resp.status == HttpStatusCode.OK) {
                logs = resp.body()
            } else {
                errorMsg = "El servidor respondio ${resp.status}"
            }
        } catch (e: Exception) {
            errorMsg = "No se pudo conectar: ${e.message}"
        }
        isLoading = false
    }

    Div({ style { backgroundColor(Color.white); padding(32.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        H3({ style { margin(0.px, 0.px, 4.px, 0.px) } }) { Text("Registro de Asistencia") }
        P({ style { color(Color.gray); margin(0.px, 0.px, 16.px, 0.px); fontSize(14.px) } }) {
            Text("Checadas registradas desde la lectora Hikvision, importacion CSV y app movil")
        }

        Div({ style { display(DisplayStyle.Flex); gap(12.px); marginBottom(16.px); flexWrap(FlexWrap.Wrap); alignItems(AlignItems.Center) } }) {
            Input(InputType.Text) {
                style {
                    padding(8.px, 12.px); borderRadius(6.px); property("border", "1px solid #cbd5e1")
                    width(240.px); property("outline", "none")
                }
                placeholder("Buscar por nombre o ID...")
                value(searchQuery)
                onInput { searchQuery = it.value }
            }
            Input(InputType.Text) {
                style {
                    padding(8.px, 12.px); borderRadius(6.px); property("border", "1px solid #cbd5e1")
                    width(160.px); property("outline", "none")
                }
                placeholder("Filtrar fecha (YYYY-MM-DD)")
                value(filterDate)
                onInput { filterDate = it.value }
            }
            Button({
                style {
                    padding(8.px, 16.px); borderRadius(6.px); property("border", "none")
                    backgroundColor(SidebarActiveColor); color(Color.white); cursor("pointer")
                    fontWeight("bold")
                }
                onClick { refreshKey++ }
            }) { Text("Actualizar") }
        }

        if (isLoading) {
            P({ style { textAlign("center"); padding(40.px, 0.px); color(Color.gray) } }) { Text("Cargando registros...") }
        } else if (errorMsg.isNotEmpty()) {
            Div({ style { padding(16.px); backgroundColor(Color("#fef2f2")); borderRadius(8.px); color(Color("#dc2626")) } }) {
                P({ style { margin(0.px) } }) { Text(errorMsg) }
            }
        } else if (logs.isEmpty()) {
            Div({ style { padding(40.px, 0.px); textAlign("center") } }) {
                P({ style { color(Color.gray); fontSize(16.px); margin(0.px, 0.px, 8.px, 0.px) } }) {
                    Text("No hay registros de asistencia aun")
                }
                P({ style { color(Color("#94a3b8")); fontSize(13.px); margin(0.px) } }) {
                    Text("Los eventos apareceran aqui cuando la lectora Hikvision sincronice via el script de red local")
                }
            }
        } else {
            val filtered = logs.filter { log ->
                (searchQuery.isBlank() ||
                    log.name.contains(searchQuery, ignoreCase = true) ||
                    log.employeeId.contains(searchQuery, ignoreCase = true)) &&
                (filterDate.isBlank() || log.timestamp.startsWith(filterDate))
            }
            val checkIns = filtered.count { it.attendanceStatus.contains("in", ignoreCase = true) }
            val checkOuts = filtered.count { it.attendanceStatus.contains("out", ignoreCase = true) }
            val uniqueEmployees = filtered.map { it.employeeId }.distinct().size

            Div({ style { display(DisplayStyle.Flex); gap(12.px); marginBottom(16.px); flexWrap(FlexWrap.Wrap) } }) {
                AttStatCard("Total Checadas", filtered.size.toString())
                AttStatCard("Empleados Unicos", uniqueEmployees.toString())
                AttStatCard("Check-ins", checkIns.toString())
                AttStatCard("Check-outs", checkOuts.toString())
            }

            Div({ style { overflowX("auto"); property("border", "1px solid #e2e8f0"); borderRadius(8.px) } }) {
                Table({
                    style {
                        width(100.percent)
                        property("border-collapse", "collapse")
                        fontSize(13.px)
                    }
                }) {
                    Thead {
                        Tr({
                            style { backgroundColor(Color("#f8fafc")); property("border-bottom", "2px solid #e2e8f0") }
                        }) {
                            listOf("ID", "Empleado", "Departamento", "Fecha/Hora", "Tipo", "Metodo", "Dispositivo").forEach { h ->
                                Th({
                                    style {
                                        padding(10.px, 12.px); textAlign("left")
                                        fontWeight("bold"); color(Color("#475569"))
                                    }
                                }) { Text(h) }
                            }
                        }
                    }
                    Tbody {
                        filtered.sortedByDescending { it.timestamp }.take(200).forEach { log ->
                            Tr({
                                style {
                                    property("border-bottom", "1px solid #f1f5f9")
                                }
                            }) {
                                Td({ style { padding(8.px, 12.px); color(Color("#64748b")) } }) { Text(log.employeeId) }
                                Td({ style { padding(8.px, 12.px); fontWeight("bold") }) {
                                    Text(log.name.ifBlank { log.employeeId })
                                }
                                Td({ style { padding(8.px, 12.px); color(Color("#64748b")) } }) { Text(log.department) }
                                Td({ style { padding(8.px, 12.px); color(Color("#475569")) } }) {
                                    Text(log.timestamp.replace("T", " ").substringBefore("."))
                                }
                                Td({ style { padding(8.px, 12.px) }) {
                                    val isCheckIn = log.attendanceStatus.contains("in", ignoreCase = true)
                                    Span({
                                        style {
                                            padding(2.px, 10.px); borderRadius(99.px); fontSize(11.px); fontWeight("bold")
                                            backgroundColor(if (isCheckIn) Color("#dcfce7") else Color("#dbeafe"))
                                            color(if (isCheckIn) Color("#166534") else Color("#1e40af"))
                                        }
                                    }) { Text(log.attendanceStatus.ifBlank { "—" }) }
                                }
                                Td({ style { padding(8.px, 12.px); color(Color("#64748b")) } }) { Text(log.verifyMode) }
                                Td({ style { padding(8.px, 12.px); color(Color("#94a3b8")); fontSize(11.px) } }) { Text(log.deviceSerial) }
                            }
                        }
                    }
                }
            }

            if (filtered.size > 200) {
                P({ style { textAlign("center"); color(Color("#94a3b8")); fontSize(12.px); padding(12.px, 0.px) } }) {
                    Text("Mostrando 200 de ${filtered.size} registros. Usa los filtros para acotar.")
                }
            }
        }
    }
}

@Composable
fun AttStatCard(label: String, value: String) {
    Div({
        style {
            flex(1); padding(16.px); borderRadius(8.px)
            backgroundColor(Color("#f8fafc")); property("border", "1px solid #e2e8f0")
            minWidth(140.px)
        }
    }) {
        P({ style { margin(0.px, 0.px, 4.px, 0.px); fontSize(11.px); color(Color("#94a3b8")); fontWeight("bold"); property("text-transform", "uppercase") } }) {
            Text(label)
        }
        P({ style { margin(0.px); fontSize(24.px); fontWeight("bold"); color(Color("#0f172a")) } }) {
            Text(value)
        }
    }
}
