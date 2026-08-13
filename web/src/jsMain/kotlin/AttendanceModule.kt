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
import kotlin.js.Date

@Composable
fun AttendanceModule(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope, t: Translations) {
    var logs by remember { mutableStateOf(emptyList<AttendanceLog>()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf("") }
    
    // Filtros estilo iVMS-4200
    val today = Date().toISOString().substringBefore("T")
    var startTime by remember { mutableStateOf("$today 00:00:00") }
    var endTime by remember { mutableStateOf("$today 23:59:59") }
    var deptFilter by remember { mutableStateOf("") }
    var nameFilter by remember { mutableStateOf("") }
    var pidFilter by remember { mutableStateOf("") }
    var sourceFilter by remember { mutableStateOf("") }
    
    var currentPage by remember { mutableStateOf(1) }
    val pageSize = 50
    var refreshKey by remember { mutableStateOf(0) }
    
    // Estado de la sincronizacion remota
    var activeTaskId by remember { mutableStateOf<String?>(null) }
    var taskStatus by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(refreshKey) {
        isLoading = true
        errorMsg = ""
        try {
            // Construir URL con filtros
            val url = "$BACKEND_URL/api/v1/asistencia/logs?" +
                "from=${startTime.replace(" ", "T")}&" +
                "to=${endTime.replace(" ", "T")}&" +
                "pid=$pidFilter&" +
                "name=$nameFilter&" +
                "dept=$deptFilter&" +
                "source=$sourceFilter"
            
            val resp = client.get(url)
            if (resp.status == HttpStatusCode.OK) {
                logs = resp.body()
                currentPage = 1 // Reset a primera pagina al buscar
            } else {
                errorMsg = "El servidor respondió ${resp.status}"
            }
        } catch (e: Exception) {
            errorMsg = "Error: ${e.message}"
        }
        isLoading = false
    }

    // Polling del estado de la tarea si hay una activa
    LaunchedEffect(activeTaskId) {
        if (activeTaskId != null) {
            while (taskStatus != "DONE" && taskStatus != "ERROR") {
                try {
                    val resp = client.get("$BACKEND_URL/api/v1/asistencia/task-status/$activeTaskId")
                    val body: Map<String, String> = resp.body()
                    taskStatus = body["status"]
                    if (taskStatus == "DONE") {
                        refreshKey++
                        window.alert("Sincronización terminada exitosamente.")
                        activeTaskId = null
                    } else if (taskStatus == "ERROR") {
                        window.alert("Error en la sincronización: ${body["result"]}")
                        activeTaskId = null
                    }
                } catch (e: Exception) {}
                kotlinx.coroutines.delay(2000)
            }
        }
    }

    Div({ style { backgroundColor(Color.white); padding(32.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        H3({ style { margin(0.px, 0.px, 16.px, 0.px) } }) { Text("Search Events - Attendance Control") }

        // PANEL DE BUSQUEDA ESTILO iVMS-4200
        Div({
            style {
                backgroundColor(Color("#2c3e50"))
                padding(24.px)
                borderRadius(8.px)
                marginBottom(24.px)
                display(DisplayStyle.Grid)
                property("grid-template-columns", "repeat(auto-fit, minmax(280.px, 1fr))")
                gap(16.px)
                color(Color.white)
            }
        }) {
            // Columna 1
            Div {
                SearchLabel("Start Time")
                Input(InputType.Text) {
                    value(startTime); onInput { startTime = it.value }
                    style { 
                        width(100.percent)
                        padding(8.px)
                        borderRadius(4.px)
                        property("border", "1px solid #455a64")
                        backgroundColor(Color("#34495e"))
                        color(Color.white)
                        property("margin-bottom", "12.px")
                        property("outline", "none")
                    }
                }
                SearchLabel("Department")
                Select({
                    style { 
                        width(100.percent)
                        padding(8.px)
                        borderRadius(4.px)
                        property("border", "1px solid #455a64")
                        backgroundColor(Color("#34495e"))
                        color(Color.white)
                        property("margin-bottom", "12.px")
                        property("outline", "none")
                    }
                    onChange { deptFilter = it.target?.asDynamic().value as String }
                }) {
                    Option("") { Text("North America Flooring (All)") }
                    listOf("Producción", "Almacén", "RH", "Mantenimiento", "Seguridad").forEach {
                        Option(it) { Text(it) }
                    }
                }
                SearchLabel("Person ID")
                Input(InputType.Text) {
                    value(pidFilter); onInput { pidFilter = it.value }
                    style { 
                        width(100.percent)
                        padding(8.px)
                        borderRadius(4.px)
                        property("border", "1px solid #455a64")
                        backgroundColor(Color("#34495e"))
                        color(Color.white)
                        property("margin-bottom", "12.px")
                        property("outline", "none")
                    }
                }
            }

            // Columna 2
            Div {
                SearchLabel("End Time")
                Input(InputType.Text) {
                    value(endTime); onInput { endTime = it.value }
                    style { 
                        width(100.percent)
                        padding(8.px)
                        borderRadius(4.px)
                        property("border", "1px solid #455a64")
                        backgroundColor(Color("#34495e"))
                        color(Color.white)
                        property("margin-bottom", "12.px")
                        property("outline", "none")
                    }
                }
                SearchLabel("Name")
                Input(InputType.Text) {
                    value(nameFilter); onInput { nameFilter = it.value }
                    style { 
                        width(100.percent)
                        padding(8.px)
                        borderRadius(4.px)
                        property("border", "1px solid #455a64")
                        backgroundColor(Color("#34495e"))
                        color(Color.white)
                        property("margin-bottom", "12.px")
                        property("outline", "none")
                    }
                }
                SearchLabel("Data Source")
                Select({
                    style { 
                        width(100.percent)
                        padding(8.px)
                        borderRadius(4.px)
                        property("border", "1px solid #455a64")
                        backgroundColor(Color("#34495e"))
                        color(Color.white)
                        property("margin-bottom", "12.px")
                        property("outline", "none")
                    }
                    onChange { sourceFilter = it.target?.asDynamic().value as String }
                }) {
                    Option("") { Text("All Sources") }
                    Option("HIKVISIONWEB") { Text("HIKVISIONWEB") }
                    Option("CSV-IMPORT") { Text("CSV-IMPORT") }
                }
            }

            // Botones de Accion
            Div({ style { display(DisplayStyle.Flex); flexDirection(FlexDirection.Column); justifyContent(JustifyContent.FlexEnd); gap(8.px) } }) {
                Button({
                    style { 
                        width(100.percent); padding(10.px); borderRadius(4.px); property("border", "none")
                        backgroundColor(Color("#e74c3c")); color(Color.white); fontWeight("bold"); cursor("pointer")
                    }
                    onClick { refreshKey++ }
                }) { Text("Search") }
                
                Button({
                    style { 
                        width(100.percent); padding(10.px); borderRadius(4.px); property("border", "none")
                        backgroundColor(Color("#34495e")); color(Color.white); fontWeight("bold"); cursor("pointer")
                    }
                    onClick { 
                        startTime = ""; endTime = ""; deptFilter = ""; nameFilter = ""; pidFilter = ""; sourceFilter = ""
                        refreshKey++ 
                    }
                }) { Text("Reset") }

                Button({
                    style { 
                        width(100.percent); padding(10.px); borderRadius(4.px); property("border", "none")
                        backgroundColor(Color("#2980b9")); color(Color.white); fontWeight("bold"); cursor("pointer")
                    }
                    onClick { 
                        scope.launch {
                            try {
                                val resp = client.post("$BACKEND_URL/api/v1/asistencia/request-sync")
                                val body: Map<String, String> = resp.body()
                                activeTaskId = body["taskId"]
                                taskStatus = "PENDING"
                            } catch (e: Exception) {
                                window.alert("No se pudo solicitar la sincronización.")
                            }
                        }
                    }
                }) { 
                    Text(if (activeTaskId != null) "Sync in Progress ($taskStatus)..." else "Get Events from Device") 
                }
            }
        }

        // ACCIONES DE EXPORTACION Y LIMPIEZA
        Div({ style { display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); alignItems(AlignItems.Center); marginBottom(16.px) } }) {
            Div({ style { display(DisplayStyle.Flex); gap(8.px) } }) {
                Button({
                    style { 
                        padding(8.px, 16.px); borderRadius(6.px); property("border", "none")
                        backgroundColor(Color("#27ae60")); color(Color.white); cursor("pointer"); fontSize(13.px)
                    }
                    onClick { 
                        val url = "$BACKEND_URL/api/v1/asistencia/export/raw/csv?" +
                            "from=${startTime.replace(" ", "T")}&to=${endTime.replace(" ", "T")}&pid=$pidFilter&name=$nameFilter&dept=$deptFilter"
                        window.open(url, "_blank")
                    }
                }) { Text("Download CSV") }
                Button({
                    style { 
                        padding(8.px, 16.px); borderRadius(6.px); property("border", "none")
                        backgroundColor(Color("#c0392b")); color(Color.white); cursor("pointer"); fontSize(13.px)
                    }
                    onClick { 
                        val url = "$BACKEND_URL/api/v1/asistencia/export/pdf?" +
                            "from=${startTime.split(" ")[0]}&to=${endTime.split(" ")[0]}"
                        window.open(url, "_blank")
                    }
                }) { Text("Generate PDF Report") }
            }

            Button({
                style { 
                    padding(8.px, 16.px); borderRadius(6.px); property("border", "none")
                    backgroundColor(Color("#7f8c8d")); color(Color.white); cursor("pointer"); fontSize(13.px)
                }
                onClick {
                    if (window.confirm("¿ESTÁS SEGURO? Se borrarán TODOS los registros de asistencia de la base de datos.")) {
                        scope.launch {
                            client.delete("$BACKEND_URL/api/v1/asistencia/all")
                            refreshKey++
                            window.alert("Base de datos vaciada.")
                        }
                    }
                }
            }) { Text("Clear Database (Danger)") }
        }

        if (isLoading) {
            P({ style { textAlign("center"); padding(40.px, 0.px); color(Color.gray) } }) { Text("Loading events...") }
        } else if (errorMsg.isNotEmpty()) {
            Div({ style { padding(16.px); backgroundColor(Color("#fef2f2")); borderRadius(8.px); color(Color("#dc2626")) } }) { Text(errorMsg) }
        } else {
            val totalPages = (logs.size / pageSize) + (if (logs.size % pageSize > 0) 1 else 0)
            val pagedLogs = logs.drop((currentPage - 1) * pageSize).take(pageSize)

            Div({ style { display(DisplayStyle.Flex); gap(12.px); marginBottom(16.px) } }) {
                AttStatCard("Events Found", logs.size.toString())
                AttStatCard("Page", "$currentPage / $totalPages")
            }

            Div({ style { overflowX("auto"); property("border", "1px solid #e2e8f0"); borderRadius(8.px) } }) {
                Table({ style { width(100.percent); property("border-collapse", "collapse"); fontSize(13.px) } }) {
                    Thead {
                        Tr({ style { backgroundColor(Color("#f8fafc")); property("border-bottom", "2px solid #e2e8f0") } }) {
                            listOf("ID", "Empleado", "Departamento", "Fecha/Hora", "Tipo", "Metodo", "Dispositivo").forEach { h ->
                                Th({ style { padding(12.px); textAlign("left"); fontWeight("bold"); color(Color("#475569")) } }) { Text(h) }
                            }
                        }
                    }
                    Tbody {
                        pagedLogs.forEach { log ->
                            Tr({ style { property("border-bottom", "1px solid #f1f5f9") } }) {
                                Td({ style { padding(10.px, 12.px); color(Color("#64748b")) } }) { Text(log.employeeId) }
                                Td({ style { padding(10.px, 12.px); fontWeight("bold") } }) { Text(log.name.ifBlank { "Unknown" }) }
                                Td({ style { padding(10.px, 12.px); color(Color("#64748b")) } }) { Text(log.department) }
                                Td({ style { padding(10.px, 12.px) } }) { 
                                    Text(log.timestamp.replace("T", " ").substringBefore("-").trim()) 
                                }
                                Td({ style { padding(10.px, 12.px) } }) {
                                    val isCheckIn = log.attendanceStatus.contains("in", ignoreCase = true)
                                    Span({
                                        style {
                                            padding(2.px, 10.px); borderRadius(4.px); fontSize(11.px); fontWeight("bold")
                                            backgroundColor(if (isCheckIn) Color("#dcfce7") else Color("#dbeafe"))
                                            color(if (isCheckIn) Color("#166534") else Color("#1e40af"))
                                        }
                                    }) { Text(log.attendanceStatus.uppercase()) }
                                }
                                Td({ style { padding(10.px, 12.px) } }) { 
                                    val mode = if (log.verifyMode.lowercase().contains("face")) "Face" else log.verifyMode
                                    Text(mode) 
                                }
                                Td({ style { padding(10.px, 12.px); color(Color("#94a3b8")) } }) { Text(log.deviceSerial) }
                            }
                        }
                    }
                }
            }

            // PAGINACION
            Div({ style { display(DisplayStyle.Flex); justifyContent(JustifyContent.Center); gap(16.px); marginTop(24.px); alignItems(AlignItems.Center) } }) {
                Button({
                    style { 
                        val bg = if(currentPage > 1) Color("#34495e") else Color("#bdc3c7")
                        padding(8.px, 16.px); borderRadius(6.px); property("border", "none")
                        backgroundColor(bg); color(Color.white); cursor("pointer"); fontSize(13.px)
                    }
                    onClick { if(currentPage > 1) currentPage-- }
                }) { Text("Previous") }
                
                Text("Page $currentPage of $totalPages")
                
                Button({
                    style { 
                        val bg = if(currentPage < totalPages) Color("#34495e") else Color("#bdc3c7")
                        padding(8.px, 16.px); borderRadius(6.px); property("border", "none")
                        backgroundColor(bg); color(Color.white); cursor("pointer"); fontSize(13.px)
                    }
                    onClick { if(currentPage < totalPages) currentPage++ }
                }) { Text("Next") }
            }
        }
    }
}

@Composable
fun SearchLabel(text: String) {
    Label(attrs = { style { display(DisplayStyle.Block); fontSize(12.px); marginBottom(4.px); color(Color("#bdc3c7")) } }) { Text(text) }
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
