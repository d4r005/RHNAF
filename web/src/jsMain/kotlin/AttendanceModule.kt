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

    val today = Date().toISOString().substringBefore("T")
    var startTime by remember { mutableStateOf("2026-01-01T00:00:00") }
    var endTime by remember { mutableStateOf("${today}T23:59:59") }
    var deptFilter by remember { mutableStateOf("") }
    var nameFilter by remember { mutableStateOf("") }
    var pidFilter by remember { mutableStateOf("") }
    var sourceFilter by remember { mutableStateOf("") }

    var currentPage by remember { mutableStateOf(1) }
    val pageSize = 50
    var refreshKey by remember { mutableStateOf(0) }

    var activeTaskId by remember { mutableStateOf<String?>(null) }
    var taskStatus by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(refreshKey) {
        isLoading = true
        errorMsg = ""
        try {
            val url = "$BACKEND_URL/api/v1/asistencia/logs?" +
                "from=${startTime}&" +
                "to=${endTime}&" +
                "pid=$pidFilter&" +
                "name=$nameFilter&" +
                "dept=$deptFilter&" +
                "source=$sourceFilter"

            val resp = client.get(url)
            if (resp.status == HttpStatusCode.OK) {
                logs = resp.body()
                currentPage = 1
            } else {
                errorMsg = "El servidor respondio ${resp.status}"
            }
        } catch (e: Exception) {
            errorMsg = "Error: ${e.message}"
        }
        isLoading = false
    }

    LaunchedEffect(activeTaskId) {
        if (activeTaskId != null) {
            while (taskStatus != "DONE" && taskStatus != "ERROR") {
                try {
                    val resp = client.get("$BACKEND_URL/api/v1/asistencia/task-status/$activeTaskId")
                    val body: Map<String, String> = resp.body()
                    taskStatus = body["status"]
                    if (taskStatus == "DONE") {
                        refreshKey++
                        window.alert("Sincronizacion completada. Datos actualizados desde la lectora.")
                        activeTaskId = null
                    } else if (taskStatus == "ERROR") {
                        window.alert("Error en la sincronizacion: ${body["result"]}")
                        activeTaskId = null
                    }
                } catch (e: Exception) {}
                kotlinx.coroutines.delay(2000)
            }
        }
    }

    Div({ style { backgroundColor(Color.white); padding(20.px); borderRadius(10.px); property("box-shadow", CardShadow) } }) {

        // === PANEL DE BUSQUEDA COMPACTO iVMS-4200 ===
        // Una sola barra oscura con todos los filtros en fila, inputs pequenos,
        // igual que el panel "Search" del iVMS-4200 Access Control.
        Div({
            style {
                backgroundColor(Color("#1a2733"))
                borderRadius(6.px)
                marginBottom(12.px)
                padding(10.px, 12.px)
                display(DisplayStyle.Flex)
                flexWrap(org.jetbrains.compose.web.css.FlexWrap.Wrap)
                gap(8.px)
                alignItems(AlignItems.FlexEnd)
            }
        }) {
            // Start Time
            Div({ style { display(DisplayStyle.Flex); flexDirection(FlexDirection.Column) } }) {
                Label(null, { style { fontSize(10.px); color(Color("#8b9dad")); marginBottom(2.px); fontWeight("bold") } }) { Text("Start") }
                Input(InputType.Text) {
                    value(startTime)
                    onInput { startTime = it.value }
                    style { IvmsInputStyle() }
                }
            }
            // End Time
            Div({ style { display(DisplayStyle.Flex); flexDirection(FlexDirection.Column) } }) {
                Label(null, { style { fontSize(10.px); color(Color("#8b9dad")); marginBottom(2.px); fontWeight("bold") } }) { Text("End") }
                Input(InputType.Text) {
                    value(endTime)
                    onInput { endTime = it.value }
                    style { IvmsInputStyle() }
                }
            }
            // Person ID
            Div({ style { display(DisplayStyle.Flex); flexDirection(FlexDirection.Column) } }) {
                Label(null, { style { fontSize(10.px); color(Color("#8b9dad")); marginBottom(2.px); fontWeight("bold") } }) { Text("PID") }
                Input(InputType.Text) {
                    value(pidFilter)
                    onInput { pidFilter = it.value }
                    style { IvmsInputStyle(); width(80.px) }
                }
            }
            // Name
            Div({ style { display(DisplayStyle.Flex); flexDirection(FlexDirection.Column) } }) {
                Label(null, { style { fontSize(10.px); color(Color("#8b9dad")); marginBottom(2.px); fontWeight("bold") } }) { Text("Name") }
                Input(InputType.Text) {
                    value(nameFilter)
                    onInput { nameFilter = it.value }
                    style { IvmsInputStyle(); width(120.px) }
                }
            }
            // Department
            Div({ style { display(DisplayStyle.Flex); flexDirection(FlexDirection.Column) } }) {
                Label(null, { style { fontSize(10.px); color(Color("#8b9dad")); marginBottom(2.px); fontWeight("bold") } }) { Text("Dept") }
                Select({
                    style { IvmsInputStyle(); width(100.px) }
                    onChange { deptFilter = it.target.asDynamic().value as String }
                }) {
                    Option("") { Text("All") }
                    listOf("Produccion", "Almacen", "RH", "Mantenimiento", "Seguridad", "General").forEach {
                        Option(it) { Text(it) }
                    }
                }
            }
            // Source
            Div({ style { display(DisplayStyle.Flex); flexDirection(FlexDirection.Column) } }) {
                Label(null, { style { fontSize(10.px); color(Color("#8b9dad")); marginBottom(2.px); fontWeight("bold") } }) { Text("Source") }
                Select({
                    style { IvmsInputStyle(); width(100.px) }
                    onChange { sourceFilter = it.target.asDynamic().value as String }
                }) {
                    Option("") { Text("All") }
                    Option("HIKVISIONWEB") { Text("HIKVISIONWEB") }
                    Option("LOCAL-SYNC") { Text("LOCAL-SYNC") }
                    Option("CSV-IMPORT") { Text("CSV-IMPORT") }
                }
            }
            // Botones
            Div({ style { display(DisplayStyle.Flex); gap(4.px); alignItems(AlignItems.FlexEnd) } }) {
                Button({
                    style { IvmsButtonStyle(Color("#e74c3c")) }
                    onClick { refreshKey++ }
                }) { Text("Search") }
                Button({
                    style { IvmsButtonStyle(Color("#3d566e")) }
                    onClick {
                        startTime = "2026-01-01T00:00:00"
                        endTime = "${today}T23:59:59"
                        deptFilter = ""; nameFilter = ""; pidFilter = ""; sourceFilter = ""
                        refreshKey++
                    }
                }) { Text("Reset") }
                Button({
                    style { IvmsButtonStyle(Color("#2980b9")) }
                    onClick {
                        scope.launch {
                            try {
                                val resp = client.post("$BACKEND_URL/api/v1/asistencia/request-sync")
                                val body: Map<String, String> = resp.body()
                                activeTaskId = body["taskId"]
                                taskStatus = "PENDING"
                                window.alert("Sincronizacion solicitada. La app de escritorio sincronizara desde la lectora directamente.")
                            } catch (e: Exception) {
                                window.alert("No se pudo solicitar la sincronizacion: ${'$'}{e.message}")
                            }
                        }
                    }
                }) {
                    Text(if (activeTaskId != null) "\uD83D\uDD04 Sync..." else "\uD83D\uDD04 Sync")
                }
                Button({
                    style { IvmsButtonStyle(Color("#27ae60")) }
                    onClick {
                        val url = "$BACKEND_URL/api/v1/asistencia/export/raw/csv?" +
                            "from=${startTime}&to=${endTime}&pid=$pidFilter&name=$nameFilter&dept=$deptFilter"
                        window.open(url, "_blank")
                    }
                }) { Text("CSV") }
                Button({
                    style { IvmsButtonStyle(Color("#c0392b")) }
                    onClick {
                        val url = "$BACKEND_URL/api/v1/asistencia/export/pdf?" +
                            "from=${startTime.split("T")[0]}&to=${endTime.split("T")[0]}"
                        window.open(url, "_blank")
                    }
                }) { Text("PDF") }
                Button({
                    style { IvmsButtonStyle(Color("#7f8c8d")) }
                    onClick {
                        if (window.confirm("ESTAS SEGURO? Se borraran TODOS los registros de asistencia.")) {
                            scope.launch {
                                client.delete("$BACKEND_URL/api/v1/asistencia/all")
                                refreshKey++
                                window.alert("Base de datos vaciada.")
                            }
                        }
                    }
                }) { Text("Clear") }
            }
        }

        // Stats bar compacta
        Div({ style { display(DisplayStyle.Flex); gap(8.px); marginBottom(8.px); fontSize(12.px); color(Color("#64748b")) } }) {
            Span { Text("Events: ${logs.size}") }
            Span { Text("|") }
            Span { Text("Page $currentPage of ${(logs.size / pageSize) + (if (logs.size % pageSize > 0) 1 else 0)}") }
        }

        if (isLoading) {
            P({ style { textAlign("center"); padding(40.px, 0.px); color(Color.gray) } }) { Text("Loading events...") }
        } else if (errorMsg.isNotEmpty()) {
            Div({ style { padding(12.px); backgroundColor(Color("#fef2f2")); borderRadius(6.px); color(Color("#dc2626")); fontSize(13.px) } }) { Text(errorMsg) }
        } else {
            val totalPages = (logs.size / pageSize) + (if (logs.size % pageSize > 0) 1 else 0)
            val pagedLogs = logs.drop((currentPage - 1) * pageSize).take(pageSize)

            Div({ style { overflowX("auto"); property("border", "1px solid #e2e8f0"); borderRadius(6.px) } }) {
                Table({ style { width(100.percent); property("border-collapse", "collapse"); fontSize(12.px) } }) {
                    Thead {
                        Tr({ style { backgroundColor(Color("#f1f5f9")); property("border-bottom", "2px solid #cbd5e1") } }) {
                            listOf("ID", "Name", "Dept", "Date/Time", "Type", "Method", "Device").forEach { h ->
                                Th({ style { padding(8.px, 10.px); textAlign("left"); fontWeight("bold"); color(Color("#475569")); fontSize(11.px); property("white-space", "nowrap") } }) { Text(h) }
                            }
                        }
                    }
                    Tbody {
                        pagedLogs.forEach { log ->
                            Tr({ style { property("border-bottom", "1px solid #f1f5f9") } }) {
                                Td({ style { padding(6.px, 10.px); color(Color("#64748b")); fontSize(12.px) } }) { Text(log.employeeId) }
                                Td({ style { padding(6.px, 10.px); fontWeight("bold"); fontSize(12.px) } }) { Text(log.name.ifBlank { "Unknown" }) }
                                Td({ style { padding(6.px, 10.px); color(Color("#64748b")); fontSize(12.px) } }) { Text(log.department) }
                                Td({ style { padding(6.px, 10.px); fontSize(12.px); property("white-space", "nowrap") } }) {
                                    val displayTs = log.timestamp.replace("T", " ")
                                        .let { if (it.length > 19) it.substring(0, 19) else it }
                                    Text(displayTs)
                                }
                                Td({ style { padding(6.px, 10.px) } }) {
                                    val isCheckIn = log.attendanceStatus.contains("in", ignoreCase = true)
                                    val isCheckOut = log.attendanceStatus.contains("out", ignoreCase = true)
                                    val isDuplicate = log.attendanceStatus.equals("Duplicate", ignoreCase = true)
                                    Span({
                                        style {
                                            padding(2.px, 8.px); borderRadius(3.px); fontSize(10.px); fontWeight("bold")
                                            when {
                                                isCheckIn -> { backgroundColor(Color("#dcfce7")); color(Color("#166534")) }
                                                isCheckOut -> { backgroundColor(Color("#dbeafe")); color(Color("#1e40af")) }
                                                isDuplicate -> { backgroundColor(Color("#f1f5f9")); color(Color("#94a3b8")) }
                                                else -> { backgroundColor(Color("#fef3c7")); color(Color("#92400e")) }
                                            }
                                        }
                                    }) { Text(log.attendanceStatus.uppercase()) }
                                }
                                Td({ style { padding(6.px, 10.px); fontSize(12.px) } }) {
                                    val mode = if (log.verifyMode.lowercase().contains("face")) "Face" else log.verifyMode
                                    Text(mode)
                                }
                                Td({ style { padding(6.px, 10.px); color(Color("#94a3b8")); fontSize(11.px) } }) { Text(log.deviceSerial) }
                            }
                        }
                    }
                }
            }

            // Paginacion compacta
            Div({ style { display(DisplayStyle.Flex); justifyContent(JustifyContent.Center); gap(12.px); marginTop(12.px); alignItems(AlignItems.Center); fontSize(12.px) } }) {
                Button({
                    style {
                        val bg = if(currentPage > 1) Color("#3d566e") else Color("#cbd5e1")
                        padding(4.px, 12.px); borderRadius(4.px); property("border", "none")
                        backgroundColor(bg); color(Color.white); cursor("pointer"); fontSize(12.px)
                    }
                    onClick { if(currentPage > 1) currentPage-- }
                }) { Text("< Prev") }

                Text("Page $currentPage of $totalPages")

                Button({
                    style {
                        val bg = if(currentPage < totalPages) Color("#3d566e") else Color("#cbd5e1")
                        padding(4.px, 12.px); borderRadius(4.px); property("border", "none")
                        backgroundColor(bg); color(Color.white); cursor("pointer"); fontSize(12.px)
                    }
                    onClick { if(currentPage < totalPages) currentPage++ }
                }) { Text("Next >") }
            }
        }
    }
}

// === Estilos compactos iVMS-4200 ===
fun StyleScope.IvmsInputStyle() {
    padding(4.px, 6.px)
    borderRadius(3.px)
    property("border", "1px solid #3d566e")
    backgroundColor(Color("#243441"))
    color(Color("#e2e8f0"))
    fontSize(12.px)
    property("outline", "none")
    height(26.px)
}

fun StyleScope.IvmsButtonStyle(bg: CSSColorValue) {
    padding(4.px, 12.px)
    borderRadius(3.px)
    property("border", "none")
    backgroundColor(bg)
    color(Color.white)
    cursor("pointer")
    fontSize(11.px)
    height(26.px)
    fontWeight("bold")
    property("white-space", "nowrap")
}
