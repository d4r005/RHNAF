import androidx.compose.runtime.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.renderComposable
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.attributes.*
import io.ktor.client.*
import io.ktor.client.engine.js.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.client.request.*
import io.ktor.client.call.*
import io.ktor.http.*
import com.example.rhnaf.shared.model.*
import kotlinx.coroutines.launch
import kotlinx.browser.window
import kotlinx.browser.document

// DISEÑO NAF CONNECT - IDENTIDAD INDUSTRIAL MODERNA
val SidebarColor = Color("#0f172a") 
val SidebarActiveColor = Color("#2563eb")
val BackgroundColor = Color("#f1f5f9")
val CardShadow = "0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06)"

enum class Module {
    DASHBOARD, EMPLOYEES, RECRUITMENT, ATTENDANCE, PAYROLL, TRAINING, PERFORMANCE, INCIDENTS, VACATIONS, DOCUMENTS, REPORTS, SETTINGS
}

fun main() {
    val client = HttpClient(Js) {
        install(ContentNegotiation) { json() }
    }

    renderComposable(rootElementId = "root") {
        var isLoggedIn by remember { mutableStateOf(false) }
        var activeModule by remember { mutableStateOf(Module.DASHBOARD) }
        var selectedEmployee by remember { mutableStateOf<Employee?>(null) }
        
        var employees by remember { mutableStateOf(emptyList<Employee>()) }
        var userName by remember { mutableStateOf("Admin NAF") }
        var userAvatar by remember { mutableStateOf("https://api.dicebear.com/7.x/avataaars/svg?seed=Felix") }
        val scope = rememberCoroutineScope()

        if (!isLoggedIn) {
            LoginScreen { u, p ->
                scope.launch {
                    try {
                        val resp = client.post("/api/login") {
                            contentType(ContentType.Application.Json)
                            setBody(mapOf("username" to u, "password" to p))
                        }
                        if (resp.status == HttpStatusCode.OK) {
                            userName = when(u) {
                                "d.trujillo@brancoindustries.com" -> "Dario Robles"
                                "arni.oziel@brancoindustries.com" -> "Arni Oziel"
                                else -> "Administrador"
                            }
                            employees = client.get("/api/employees").body()
                            isLoggedIn = true
                        }
                    } catch (e: Exception) { console.log(e) }
                }
            }
        } else {
            Div({
                style {
                    display(DisplayStyle.Flex)
                    height(100.vh)
                    fontFamily("Inter", "Segoe UI", "sans-serif")
                    backgroundColor(BackgroundColor)
                }
            }) {
                // SIDEBAR
                Sidebar(activeModule) { 
                    activeModule = it 
                    selectedEmployee = null
                }

                // CONTENIDO PRINCIPAL
                Div({ style { flex(1); display(DisplayStyle.Flex); flexDirection(FlexDirection.Column); overflowY("auto") } }) {
                    TopBar(userName, "Gestión Industrial", userAvatar)

                    Div({ style { padding(32.px) } }) {
                        when (activeModule) {
                            Module.DASHBOARD -> DashboardView(employees)
                            Module.EMPLOYEES -> {
                                if (selectedEmployee == null) {
                                    EmployeeListView(
                                        employees = employees, 
                                        onSelect = { selectedEmployee = it },
                                        onDelete = { id ->
                                            scope.launch {
                                                client.delete("/api/employee/$id")
                                                employees = client.get("/api/employees").body()
                                            }
                                        }
                                    )
                                } else {
                                    EmployeeDigitalFile(
                                        emp = selectedEmployee!!,
                                        onBack = { 
                                            selectedEmployee = null
                                            scope.launch { employees = client.get("/api/employees").body() }
                                        },
                                        onSave = { updated ->
                                            scope.launch {
                                                client.post("/api/employee/update") {
                                                    contentType(ContentType.Application.Json)
                                                    setBody(updated)
                                                }
                                                employees = client.get("/api/employees").body()
                                                selectedEmployee = null
                                            }
                                        }
                                    )
                                }
                            }
                            Module.INCIDENTS -> SafetyModule(client, scope)
                            Module.ATTENDANCE -> AttendanceModule()
                            Module.RECRUITMENT -> RecruitmentModule()
                            Module.PAYROLL -> PayrollModule()
                            Module.TRAINING -> TrainingModule()
                            Module.PERFORMANCE -> PerformanceModule()
                            Module.VACATIONS -> VacationsModule()
                            Module.DOCUMENTS -> DocumentsModule()
                            Module.REPORTS -> ReportsModule()
                            Module.SETTINGS -> SettingsView(userName, userAvatar, { userName = it }, { userAvatar = it })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Sidebar(active: Module, onSelect: (Module) -> Unit) {
    Nav({
        style {
            width(260.px)
            backgroundColor(SidebarColor)
            color(Color.white)
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Column)
        }
    }) {
        Div({ style { padding(32.px); display(DisplayStyle.Flex); alignItems(AlignItems.Center); justifyContent(JustifyContent.Center) } }) {
            // LOGO NAF CONNECT (Versión Sidebar)
            H2({ style { margin(0.px); fontSize(22.px); fontFamily("Inter", "sans-serif"); color(Color.white) } }) { 
                Span({ style { fontWeight("900"); property("font-style", "italic") } }) { Text("NAF") }
                Span({ style { fontWeight("300"); color(Color("#94a3b8")); marginLeft(6.px); property("font-style", "normal") } }) { Text("CONNECT") }
            }
        }

        Input(InputType.Text) {
            style {
                property("margin", "0 20px 24px 20px")
                padding(10.px, 14.px)
                backgroundColor(Color("#1e293b"))
                property("border", "1px solid #334155")
                borderRadius(8.px)
                color(Color.white)
                property("outline", "none")
            }
            placeholder("Buscar...")
        }

        Div({ style { flex(1); overflowY("auto"); padding(0.px, 16.px) } }) {
            SidebarLink("Panel de Control", Module.DASHBOARD, active == Module.DASHBOARD, onSelect)
            SidebarLink("Plantilla Personal", Module.EMPLOYEES, active == Module.EMPLOYEES, onSelect)
            SidebarLink("Reclutamiento", Module.RECRUITMENT, active == Module.RECRUITMENT, onSelect)
            SidebarLink("Asistencia Facial", Module.ATTENDANCE, active == Module.ATTENDANCE, onSelect)
            SidebarLink("Nómina y Pagos", Module.PAYROLL, active == Module.PAYROLL, onSelect)
            SidebarLink("Capacitación", Module.TRAINING, active == Module.TRAINING, onSelect)
            SidebarLink("Seguridad (EHS)", Module.INCIDENTS, active == Module.INCIDENTS, onSelect)
            SidebarLink("Vacaciones", Module.VACATIONS, active == Module.VACATIONS, onSelect)
            SidebarLink("Expedientes", Module.DOCUMENTS, active == Module.DOCUMENTS, onSelect)
            SidebarLink("Configuración", Module.SETTINGS, active == Module.SETTINGS, onSelect)
        }

        Div({ style { padding(24.px); property("border-top", "1px solid #1e293b") } }) {
            P({ style { fontSize(11.px); color(Color("#64728b")); textAlign("center") } }) { Text("NAF CONNECT v3.0") }
        }
    }
}

@Composable
fun SidebarLink(label: String, mod: Module, isSelected: Boolean, onSelect: (Module) -> Unit) {
    Div({
        style {
            padding(10.px, 16.px)
            marginBottom(4.px)
            borderRadius(8.px)
            cursor("pointer")
            display(DisplayStyle.Flex)
            alignItems(AlignItems.Center)
            gap(12.px)
            if (isSelected) backgroundColor(SidebarActiveColor) else backgroundColor(Color.transparent)
            property("transition", "all 0.2s")
        }
        onClick { onSelect(mod) }
    }) {
        Div({ style { width(18.px); height(18.px); backgroundColor(if (isSelected) Color.white else Color("#94a3b8")); borderRadius(4.px) } })
        Text(label)
    }
}

@Composable
fun DashboardView(employees: List<Employee>) {
    val totalEmployees = employees.size
    val activeEmployees = employees.count { it.status == EmployeeStatus.ACTIVE }
    val activePercent = if (totalEmployees > 0) (activeEmployees.toDouble() / totalEmployees * 100).toInt() else 0

    Div {
        // TOP CARDS
        Div({
            style {
                display(DisplayStyle.Grid)
                property("grid-template-columns", "repeat(auto-fit, minmax(200.px, 1fr))")
                gap(20.px)
                property("margin-bottom", "24px")
            }
        }) {
            StatCard("Total Empleados", "$totalEmployees", "Base de datos NAF", Color("#6366f1"))
            StatCard("Empleados Activos", "$activeEmployees", "$activePercent% del total", Color("#22c55e"))
            StatCard("Vacantes Abiertas", "4", "Procesos activos", Color("#eab308"))
            StatCard("Capacitación Pendiente", "2", "Próximos cursos", Color("#a855f7"))
            StatCard("Incidencias Hoy", "0", "Sin reportes críticos", Color("#22c55e"))
        }

        // MIDDLE SECTION: CHART + WIDGETS
        Div({
            style {
                display(DisplayStyle.Grid)
                property("grid-template-columns", "2fr 1fr 1fr")
                gap(24.px)
            }
        }) {
            // Main Chart Placeholder
            Div({
                style {
                    backgroundColor(Color.white); padding(24.px); borderRadius(12.px)
                    property("box-shadow", CardShadow)
                }
            }) {
                Div({ style { display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); property("margin-bottom", "20px") } }) {
                    H3({ style { margin(0.px); fontSize(16.px) } }) { Text("Indicadores Clave") }
                    Span({ style { color(Color.gray); fontSize(12.px) } }) { Text("Este mes ▼") }
                }
                Div({ style { height(200.px); backgroundColor(Color("#f1f5f9")); borderRadius(8.px); display(DisplayStyle.Flex); alignItems(AlignItems.Center); justifyContent(JustifyContent.Center) } }) {
                    Text("Visualización de Gráfica de Líneas")
                }
                Div({
                    style { display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); property("margin-top", "20px") }
                }) {
                    MiniStat("Ausentismo", "3.2%", "-0.5% vs anterior", Color("#22c55e"))
                    MiniStat("Rotación", "1.8%", "-0.2% vs anterior", Color("#22c55e"))
                    MiniStat("Horas Extras", "2,456", "+8.2% vs anterior", Color("#ef4444"))
                    MiniStat("Antigüedad Prom.", "2.4 años", "+0.2 años vs anterior", Color("#22c55e"))
                }
            }

            // Birthdays
            Div({
                style {
                    backgroundColor(Color.white); padding(24.px); borderRadius(12.px)
                    property("box-shadow", CardShadow)
                }
            }) {
                H3({ style { property("margin", "0 0 20px 0"); fontSize(16.px) } }) { Text("Cumpleaños del Mes") }
                BirthdayItem("Carlos Rodríguez", "15 de Mayo", true)
                BirthdayItem("María González", "18 de Mayo", false)
                BirthdayItem("Juan Pérez", "22 de Mayo", false)
                BirthdayItem("Ana López", "28 de Mayo", false)
                BirthdayItem("Luis Martínez", "30 de Mayo", false)
            }

            // Important Alerts
            Div({
                style {
                    backgroundColor(Color.white); padding(24.px); borderRadius(12.px)
                    property("box-shadow", CardShadow)
                }
            }) {
                H3({ style { property("margin", "0 0 20px 0"); fontSize(16.px) } }) { Text("Alertas Importantes") }
                AlertItem("5 contratos vencen en los próximos 7 días", Color("#ef4444"))
                AlertItem("12 empleados con documentos vencidos", Color("#f97316"))
                AlertItem("15 capacitaciones por vencer", Color("#3b82f6"))
                AlertItem("3 evaluaciones pendientes", Color("#f59e0b"))
                AlertItem("8 vacaciones pendientes de aprobar", Color("#6366f1"))
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, sub: String, color: CSSColorValue) {
    Div({
        style {
            backgroundColor(Color.white); padding(20.px); borderRadius(12.px)
            property("box-shadow", CardShadow)
            display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); alignItems(AlignItems.Center)
        }
    }) {
        Div {
            P({ style { margin(0.px); color(Color("#64728b")); fontSize(14.px) } }) { Text(label) }
            H2({ style { property("margin", "4px 0"); fontSize(24.px); fontWeight("bold") } }) { Text(value) }
            P({ style { margin(0.px); color(color); fontSize(12.px); fontWeight("500") } }) { Text(sub) }
        }
        Div({ style { width(48.px); height(48.px); backgroundColor(Color("#f1f5f9")); borderRadius(12.px) } })
    }
}

@Composable
fun MiniStat(label: String, value: String, sub: String, color: CSSColorValue) {
    Div {
        P({ style { margin(0.px); color(Color("#64728b")); fontSize(11.px) } }) { Text(label) }
        P({ style { property("margin", "2px 0"); fontSize(16.px); fontWeight("bold") } }) { Text(value) }
        P({ style { margin(0.px); color(color); fontSize(10.px) } }) { Text(sub) }
    }
}

@Composable
fun BirthdayItem(name: String, date: String, isToday: Boolean) {
    Div({
        style {
            display(DisplayStyle.Flex); alignItems(AlignItems.Center); gap(12.px); marginBottom(16.px)
        }
    }) {
        Div({ style { width(36.px); height(36.px); backgroundColor(Color("#e2e8f0")); borderRadius(50.percent) } })
        Div({ style { flex(1) } }) {
            P({ style { margin(0.px); fontSize(14.px); fontWeight("500") } }) { Text(name) }
            P({ style { margin(0.px); fontSize(12.px); color(Color("#64728b")) } }) { Text(date) }
        }
        if (isToday) {
            Span({ style { fontSize(10.px); backgroundColor(Color("#dbeafe")); color(Color("#1e40af")); padding(2.px, 6.px); borderRadius(4.px); fontWeight("bold") } }) { Text("Hoy") }
        }
    }
}

@Composable
fun AlertItem(text: String, color: CSSColorValue) {
    Div({
        style {
            display(DisplayStyle.Flex); gap(12.px); marginBottom(16.px); alignItems(AlignItems.FlexStart)
        }
    }) {
        Div({ style { width(20.px); height(20.px); borderRadius(50.percent); property("border", "2px solid $color"); property("flex-shrink", "0") } })
        Div {
            P({ style { margin(0.px); fontSize(13.px); fontWeight("500") } }) { Text(text) }
            A(href = "#", { style { fontSize(11.px); color(SidebarActiveColor); textDecoration("none") } }) { Text("Ver detalles") }
        }
    }
}

@Composable
fun TopBar(user: String, role: String, avatarUrl: String) {
    Header({
        style {
            backgroundColor(Color.white); padding(12.px, 24.px); display(DisplayStyle.Flex)
            justifyContent(JustifyContent.SpaceBetween); alignItems(AlignItems.Center)
            property("border-bottom", "1px solid #e2e8f0")
        }
    }) {
        Div {
            H2({ style { margin(0.px); fontSize(16.px); fontWeight("bold") } }) { Text("Panel NAF CONNECT") }
            P({ style { margin(0.px); fontSize(12.px); color(Color("#64728b")) } }) { Text("Gestión Industrial de Talento") }
        }
        Div({ style { display(DisplayStyle.Flex); alignItems(AlignItems.Center); gap(20.px) } }) {
            Div({ style { width(20.px); height(20.px); backgroundColor(Color("#64728b")); borderRadius(50.percent) } })
            Div({ style { display(DisplayStyle.Flex); alignItems(AlignItems.Center); gap(12.px) } }) {
                Div({ style { textAlign("right") } }) {
                    P({ style { margin(0.px); fontSize(14.px); fontWeight("600") } }) { Text(user) }
                    P({ style { margin(0.px); fontSize(12.px); color(Color("#64728b")) } }) { Text(role) }
                }
                Img(src = avatarUrl) {
                    style { width(36.px); height(36.px); borderRadius(50.percent); property("object-fit", "cover"); backgroundColor(Color("#cbd5e1")) }
                }
            }
        }
    }
}

@Composable
fun EmployeeListView(employees: List<Employee>, onSelect: (Employee) -> Unit, onDelete: (String) -> Unit) {
    Div({ style { backgroundColor(Color.white); padding(24.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        Div({ style { display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); alignItems(AlignItems.Center); marginBottom(24.px) } }) {
            H3({ style { margin(0.px) } }) { Text("Gestión de Personal NAF CONNECT") }
            Div({ style { display(DisplayStyle.Flex); gap(12.px) } }) {
                Button({
                    style { padding(8.px, 16.px); backgroundColor(Color("#22c55e")); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer"); fontSize(13.px); fontWeight("bold") }
                    onClick { /* Implementar diálogo de nuevo empleado */ }
                }) { Text("+ Nuevo Empleado") }
                Button({
                    style { padding(8.px, 16.px); backgroundColor(Color("#1e293b")); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer"); fontSize(13.px) }
                    onClick { exportToCSV(employees) }
                }) { Text("Exportar CSV") }
            }
        }
        Table({ style { width(100.percent); property("border-collapse", "collapse") } }) {
            Thead {
                Tr {
                    Th({ style { textAlign("left"); padding(12.px); property("border-bottom", "2px solid #f1f5f9") } }) { Text("ID") }
                    Th({ style { textAlign("left"); padding(12.px); property("border-bottom", "2px solid #f1f5f9") } }) { Text("Colaborador") }
                    Th({ style { textAlign("left"); padding(12.px); property("border-bottom", "2px solid #f1f5f9") } }) { Text("Puesto") }
                    Th({ style { textAlign("left"); padding(12.px); property("border-bottom", "2px solid #f1f5f9") } }) { Text("Acciones") }
                }
            }
            Tbody {
                employees.forEach { emp ->
                    Tr {
                        Td({ style { padding(12.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(emp.id) }
                        Td({ style { padding(12.px); property("border-bottom", "1px solid #f1f5f9") } }) {
                            Text("${emp.firstName} ${emp.lastName}")
                        }
                        Td({ style { padding(12.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(emp.position) }
                        Td({ style { padding(12.px); property("border-bottom", "1px solid #f1f5f9"); display(DisplayStyle.Flex); gap(8.px) } }) {
                            Button({
                                style { padding(6.px, 12.px); backgroundColor(SidebarActiveColor); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") }
                                onClick { onSelect(emp) }
                            }) { Text("Editar") }
                            Button({
                                style { padding(6.px, 12.px); backgroundColor(Color("#ef4444")); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") }
                                onClick { if(window.confirm("¿Eliminar a ${emp.firstName}?")) onDelete(emp.id) }
                            }) { Text("Eliminar") }
                        }
                    }
                }
            }
        }
    }
}

fun exportToCSV(employees: List<Employee>) {
    val header = "ID,Nombre,Puesto,Fecha Alta,Estado\n"
    val rows = employees.joinToString("\n") { 
        "${it.id},${it.firstName} ${it.lastName},${it.position},${it.entryDate},${it.status.name}" 
    }
    val csvContent = header + rows
    val blob = org.w3c.dom.url.URL.createObjectURL(org.w3c.files.Blob(arrayOf(csvContent), org.w3c.files.BlobPropertyBag(type = "text/csv")))
    val link = kotlinx.browser.document.createElement("a") as org.w3c.dom.HTMLAnchorElement
    link.href = blob
    link.download = "Lista_Personal_NAF_CONNECT.csv"
    link.click()
}

@Composable
fun EmployeeDigitalFile(emp: Employee, onBack: () -> Unit, onSave: (Employee) -> Unit) {
    var editMode by remember { mutableStateOf(false) }
    var editedEmp by remember { mutableStateOf(emp) }
    
    Div {
        Div({ style { display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); marginBottom(16.px) } }) {
            Button({ onClick { onBack() }; style { cursor("pointer"); backgroundColor(Color.white); property("border", "1px solid #ccc"); padding(8.px, 16.px); borderRadius(6.px) } }) { Text("← Regresar") }
            Div({ style { display(DisplayStyle.Flex); gap(12.px) } }) {
                if (editMode) {
                    Button({ 
                        style { padding(8.px, 20.px); backgroundColor(Color("#22c55e")); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer"); fontWeight("bold") }
                        onClick { onSave(editedEmp) }
                    }) { Text("Guardar Cambios") }
                    Button({ 
                        style { padding(8.px, 20.px); backgroundColor(Color("#ef4444")); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") }
                        onClick { editMode = false }
                    }) { Text("Cancelar") }
                } else {
                    Button({ 
                        style { padding(8.px, 20.px); backgroundColor(SidebarActiveColor); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") }
                        onClick { editMode = true }
                    }) { Text("Editar Información") }
                }
            }
        }
        
        Div({
            style {
                backgroundColor(Color.white); padding(32.px); borderRadius(16.px); property("box-shadow", CardShadow)
                display(DisplayStyle.Flex); gap(40.px)
            }
        }) {
            // Perfil
            Div({ style { width(220.px); textAlign("center") } }) {
                Div({ style { width(120.px); height(120.px); backgroundColor(Color("#e2e8f0")); borderRadius(50.percent); property("margin", "0 auto 20px") } })
                if (editMode) {
                    Input(InputType.Text) { 
                        value(editedEmp.firstName); onInput { editedEmp = editedEmp.copy(firstName = it.value) }
                        style { width(100.percent); marginBottom(8.px); padding(8.px); borderRadius(4.px); property("border", "1px solid #ccc") }
                    }
                    Input(InputType.Text) { 
                        value(editedEmp.lastName); onInput { editedEmp = editedEmp.copy(lastName = it.value) }
                        style { width(100.percent); padding(8.px); borderRadius(4.px); property("border", "1px solid #ccc") }
                    }
                } else {
                    H3({ style { margin(0.px) } }) { Text("${emp.firstName} ${emp.lastName}") }
                    P({ style { color(Color.gray); property("margin", "8px 0") } }) { Text(emp.position) }
                }
            }

            // Datos
            Div({ style { flex(1) } }) {
                H2({ style { property("border-bottom", "2px solid #3b82f6"); display(DisplayStyle.InlineBlock); paddingBottom(8.px); marginBottom(24.px) } }) { Text("Expediente Digital del Empleado") }
                
                Div({ style { display(DisplayStyle.Grid); property("grid-template-columns", "1fr 1fr"); gap(32.px) } }) {
                    Div {
                        H4({ style { color(SidebarActiveColor); marginBottom(12.px) } }) { Text("Datos Laborales") }
                        EditField("Puesto", editedEmp.position, editMode) { editedEmp = editedEmp.copy(position = it) }
                        EditField("Departamento", editedEmp.department, editMode) { editedEmp = editedEmp.copy(department = it) }
                        EditField("Fecha Ingreso", editedEmp.entryDate, editMode) { editedEmp = editedEmp.copy(entryDate = it) }
                        EditField("ID Lectora Facial", editedEmp.readerId ?: "", editMode) { editedEmp = editedEmp.copy(readerId = it) }
                    }
                    Div {
                        H4({ style { color(SidebarActiveColor); marginBottom(12.px) } }) { Text("Identidad") }
                        EditField("CURP", editedEmp.curp ?: "", editMode) { editedEmp = editedEmp.copy(curp = it) }
                        EditField("RFC", editedEmp.rfc ?: "", editMode) { editedEmp = editedEmp.copy(rfc = it) }
                        EditField("NSS", editedEmp.nss ?: "", editMode) { editedEmp = editedEmp.copy(nss = it) }
                    }
                }
            }
        }
    }
}

@Composable
fun EditField(label: String, value: String, editMode: Boolean, onUpdate: (String) -> Unit) {
    Div({ style { display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); padding(8.px, 0.px); property("border-bottom", "1px solid #f1f5f9"); alignItems(AlignItems.Center) } }) {
        Span({ style { color(Color.gray); fontSize(13.px) } }) { Text(label) }
        if (editMode) {
            Input(InputType.Text) { 
                value(value); onInput { onUpdate(it.value) }
                style { padding(4.px); borderRadius(4.px); property("border", "1px solid #ddd"); fontSize(13.px); textAlign("right") }
            }
        } else {
            Span({ style { fontWeight("600"); fontSize(13.px) } }) { Text(value.ifEmpty { "No registrado" }) }
        }
    }
}

@Composable
fun InfoSection(title: String, data: List<Pair<String, String>>) {
    Div {
        H4({ style { color(SidebarActiveColor); marginBottom(12.px) } }) { Text(title) }
        data.forEach { (k, v) ->
            Div({ style { display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); padding(8.px, 0.px); property("border-bottom", "1px solid #f1f5f9") } }) {
                Span({ style { color(Color.gray); fontSize(13.px) } }) { Text(k) }
                Span({ style { fontWeight("600"); fontSize(13.px) } }) { Text(v) }
            }
        }
    }
}

@Composable
fun StatusBadge(s: EmployeeStatus) {
    val (bg, txt) = when(s) {
        EmployeeStatus.ACTIVE -> Color("#dcfce7") to Color("#166534")
        EmployeeStatus.VACATION -> Color("#fef9c3") to Color("#854d0e")
        else -> Color("#fee2e2") to Color("#991b1b")
    }
    Span({
        style {
            padding(4.px, 12.px); borderRadius(20.px); color(txt); backgroundColor(bg); fontSize(11.px); fontWeight("bold")
        }
    }) { Text(s.name) }
}

@Composable
fun LoginScreen(onLogin: (String, String) -> Unit) {
    var u by remember { mutableStateOf("") }
    var p by remember { mutableStateOf("") }
    Div({ style { display(DisplayStyle.Flex); alignItems(AlignItems.Center); justifyContent(JustifyContent.Center); height(100.vh); backgroundColor(SidebarColor) } }) {
        Div({ style { backgroundColor(Color.white); padding(48.px); borderRadius(24.px); width(400.px); property("box-shadow", "0 25px 50px -12px rgba(0, 0, 0, 0.5)") } }) {
            Div({ style { textAlign("center"); marginBottom(40.px) } }) {
                // LOGO NAF CONNECT (Réplica exacta del logo proporcionado)
                H1({ style { margin(0.px); fontSize(42.px); fontFamily("Inter", "sans-serif"); letterSpacing((-1).px) } }) { 
                    Span({ style { fontWeight("900"); property("font-style", "italic"); color(Color("#0f172a")) } }) { Text("NAF") }
                    Span({ style { fontWeight("300"); color(Color("#475569")); marginLeft(8.px); property("font-style", "normal") } }) { Text("CONNECT") }
                }
                P({ style { color(Color("#64728b")); marginTop(4.px); fontSize(14.px); letterSpacing(2.px); fontWeight("500") } }) { Text("PORTAL DE GESTIÓN") }
            }
            
            Label(attrs = { style { fontSize(12.px); fontWeight("600"); color(Color("#475569")); letterSpacing(0.5.px) } }) { Text("CORREO ELECTRÓNICO") }
            Input(InputType.Text) { placeholder("usuario@dominio.com"); style { width(100.percent); padding(12.px); property("margin", "8px 0 20px 0"); borderRadius(8.px); property("border", "1px solid #e2e8f0"); property("box-sizing", "border-box"); property("outline", "none"); backgroundColor(Color("#f8fafc")) }; onInput { u = it.value } }
            
            Label(attrs = { style { fontSize(12.px); fontWeight("600"); color(Color("#475569")); letterSpacing(0.5.px) } }) { Text("CONTRASEÑA") }
            Input(InputType.Password) { placeholder("••••••••"); style { width(100.percent); padding(12.px); property("margin", "8px 0 20px 0"); borderRadius(8.px); property("border", "1px solid #e2e8f0"); property("box-sizing", "border-box"); property("outline", "none"); backgroundColor(Color("#f8fafc")) }; onInput { p = it.value } }
            
            Button({ 
                style { 
                    width(100.percent); padding(16.px); backgroundColor(Color("#0f172a")); color(Color.white); 
                    property("border", "none"); borderRadius(8.px); cursor("pointer"); property("margin-top", "12.px"); 
                    fontWeight("bold"); fontSize(14.px); property("transition", "all 0.2s") 
                }
                onClick { onLogin(u, p) } 
            }) { Text("INICIAR SESIÓN") }
            
            P({ style { textAlign("center"); marginTop(32.px); fontSize(11.px); color(Color("#94a3b8")) } }) { Text("© 2024 NAF CONNECT • SISTEMA INDUSTRIAL") }
        }
    }
}

@Composable
fun SafetyModule(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope) {
    var desc by remember { mutableStateOf("") }
    var res by remember { mutableStateOf("") }
    Div({ style { backgroundColor(Color.white); padding(32.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        H3 { Text("Investigación de Incidentes EHS (Inteligencia Artificial)") }
        P({ style { color(Color.gray) } }) { Text("Describa el incidente para obtener un análisis de riesgo instantáneo mediante modelos de lenguaje.") }
        
        TextArea(value = desc) {
            style { 
                width(100.percent); height(150.px); property("margin", "20px 0"); padding(12.px); borderRadius(8.px); property("border", "1px solid #e2e8f0"); property("box-sizing", "border-box") 
            }
            onInput { desc = it.value }
        }
        
        Button({ 
            style { padding(12.px, 24.px); backgroundColor(SidebarColor); color(Color.white); property("border", "none"); borderRadius(8.px); cursor("pointer"); fontWeight("bold") }
            onClick {
                scope.launch {
                    val resp = client.post("/api/safety/analyze") {
                        contentType(ContentType.Application.Json)
                        setBody(mapOf("description" to desc))
                    }
                    val body: Map<String, String> = resp.body()
                    res = body["analysis"] ?: ""
                }
            }
        }) { Text("Analizar con IA") }
        if (res.isNotEmpty()) {
            Div({ style { property("margin-top", "32px"); padding(20.px); backgroundColor(Color("#f0f9ff")); property("border-left", "4px solid $SidebarActiveColor"); borderRadius(4.px) } }) { 
                H4({ style { property("margin", "0 0 10px 0") } }) { Text("Análisis de Riesgo:") }
                Text(res) 
            }
        }
    }
}

@Composable
fun AttendanceModule() {
    Div({ style { backgroundColor(Color.white); padding(32.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        H3 { Text("Control de Asistencia Biométrica (Rostro)") }
        P({ style { color(Color.gray) } }) { Text("Monitoreo en tiempo real de terminales Hikonect con reconocimiento facial.") }
        
        Div({ style { display(DisplayStyle.Flex); gap(24.px); property("margin-top", "24px") } }) {
            // Estado de la lectora
            Div({ style { flex(1); padding(20.px); backgroundColor(Color("#f8fafc")); borderRadius(8.px); property("border", "1px solid #e2e8f0") } }) {
                H4({ style { margin(0.px) } }) { Text("Terminal Acceso Principal") }
                P({ style { color(Color("#22c55e")); fontWeight("bold"); fontSize(14.px) } }) { Text("● RECONOCIMIENTO ACTIVO") }
                P({ style { fontSize(12.px); color(Color.gray) } }) { Text("Modelo: Hikonect Face-ID v2") }
                P({ style { fontSize(12.px); color(Color.gray) } }) { Text("Usuarios en memoria: 154") }
            }
            
            Div({ style { flex(2) } }) {
                H4 { Text("Últimos Accesos (Rostro Detectado)") }
                Table({ style { width(100.percent); fontSize(13.px) } }) {
                    Tbody {
                        Tr {
                            Td { Text("08:30:12 AM") }
                            Td { B { Text("Daniel Trujillo") } }
                            Td { Span({ style { color(Color("#166534")); backgroundColor(Color("#dcfce7")); padding(2.px, 8.px); borderRadius(4.px) } }) { Text("Rostro Verificado") } }
                        }
                        Tr {
                            Td { Text("08:32:45 AM") }
                            Td { B { Text("Arni Oziel") } }
                            Td { Span({ style { color(Color("#166534")); backgroundColor(Color("#dcfce7")); padding(2.px, 8.px); borderRadius(4.px) } }) { Text("Rostro Verificado") } }
                        }
                    }
                }
            }
        }
        
        Div({ style { property("margin-top", "32px"); padding(16.px); backgroundColor(Color("#f0fdf4")); borderRadius(8.px); property("border", "1px solid #dcfce7") } }) {
            P({ style { margin(0.px); fontSize(13.px); color(Color("#166534")) } }) { 
                Text("Sincronización Automática: La lectora está vinculada mediante el ID de empleado (employeeNo). Los registros se alimentan directamente de la base de datos interna del dispositivo.")
            }
        }
    }
}

@Composable
fun SettingsView(name: String, avatar: String, onNameChange: (String) -> Unit, onAvatarChange: (String) -> Unit) {
    Div({ style { backgroundColor(Color.white); padding(32.px); borderRadius(16.px); property("box-shadow", CardShadow) } }) {
        H2 { Text("Configuración de Perfil") }
        P({ style { color(Color.gray); marginBottom(24.px) } }) { Text("Personalice su identidad en el portal NAF CONNECT.") }
        
        Div({ style { display(DisplayStyle.Flex); gap(40.px); alignItems(AlignItems.Center) } }) {
            // Avatar Actual
            Div({ style { textAlign("center") } }) {
                Img(src = avatar) {
                    style { width(120.px); height(120.px); borderRadius(50.percent); property("border", "4px solid $SidebarActiveColor"); marginBottom(16.px) }
                }
                P({ style { fontWeight("bold"); margin(0.px) } }) { Text(name) }
            }
            
            // Selector de Avatares
            Div({ style { flex(1) } }) {
                H4 { Text("Seleccionar Avatar Profesional") }
                Div({ style { display(DisplayStyle.Flex); gap(16.px); flexWrap(FlexWrap.Wrap) } }) {
                    val avatars = listOf(
                        "https://api.dicebear.com/7.x/avataaars/svg?seed=Felix",
                        "https://api.dicebear.com/7.x/avataaars/svg?seed=Jace",
                        "https://api.dicebear.com/7.x/avataaars/svg?seed=Jack",
                        "https://api.dicebear.com/7.x/avataaars/svg?seed=Aneka",
                        "https://api.dicebear.com/7.x/avataaars/svg?seed=Caleb"
                    )
                    avatars.forEach { url ->
                        Img(src = url) {
                            style { 
                                width(60.px); height(60.px); borderRadius(50.percent); cursor("pointer")
                                property("border", if (url == avatar) "3px solid $SidebarActiveColor" else "1px solid #ddd")
                                property("transition", "transform 0.2s")
                            }
                            onClick { onAvatarChange(url) }
                        }
                    }
                }
                
                H4({ style { marginTop(24.px) } }) { Text("Nombre a mostrar") }
                Input(InputType.Text) {
                    value(name)
                    onInput { onNameChange(it.value) }
                    style { width(100.percent); padding(12.px); borderRadius(8.px); property("border", "1px solid #e2e8f0") }
                }
            }
        }
    }
}

@Composable
fun RecruitmentModule() {
    Div({ style { backgroundColor(Color.white); padding(32.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        H3 { Text("Módulo de Reclutamiento") }
        P({ style { color(Color.gray); marginBottom(24.px) } }) { Text("Gestión de vacantes y candidatos para NAF CONNECT.") }
        
        Div({ style { display(DisplayStyle.Grid); property("grid-template-columns", "1fr 1fr"); gap(24.px) } }) {
            // Vacantes
            Div({ style { padding(20.px); property("border", "1px solid #e2e8f0"); borderRadius(8.px) } }) {
                H4 { Text("Vacantes Activas") }
                listOf("Ingeniero de Procesos", "Operador de Montacargas", "Técnico Eléctrico").forEach { job ->
                    Div({ style { padding(12.px, 0.px); property("border-bottom", "1px solid #f1f5f9"); display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween) } }) {
                        Text(job)
                        Span({ style { color(SidebarActiveColor); fontWeight("bold"); fontSize(12.px) } }) { Text("4 Candidatos") }
                    }
                }
            }
            // Candidatos Recientes
            Div({ style { padding(20.px); property("border", "1px solid #e2e8f0"); borderRadius(8.px) } }) {
                H4 { Text("Últimos Candidatos") }
                listOf("Roberto Sosa", "Elena Peña", "Miguel Rivas").forEach { name ->
                    Div({ style { padding(12.px, 0.px); property("border-bottom", "1px solid #f1f5f9") } }) {
                        Text(name)
                        P({ style { fontSize(11.px); color(Color.gray); margin(0.px) } }) { Text("Entrevista pendiente") }
                    }
                }
            }
        }
    }
}

@Composable
fun PayrollModule() {
    Div({ style { backgroundColor(Color.white); padding(32.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        H3 { Text("Gestión de Nómina") }
        Div({ style { display(DisplayStyle.Flex); gap(24.px); marginBottom(32.px) } }) {
            StatCard("Próxima Dispersión", "15 Jul", "Nómina Quincenal", SidebarActiveColor)
            StatCard("Total a Pagar", "$458,200", "Estimado actual", Color("#10b981"))
        }
        Table({ style { width(100.percent) } }) {
            Thead { Tr { Th { Text("Periodo") }; Th { Text("Monto Total") }; Th { Text("Estado") }; Th { Text("Recibos") } } }
            Tbody {
                listOf("Junio Q2", "Junio Q1", "Mayo Q2").forEach { period ->
                    Tr {
                        Td { Text(period) }
                        Td { Text("$442,150.00") }
                        Td { Span({ style { color(Color("#166534")); backgroundColor(Color("#dcfce7")); padding(2.px, 8.px); borderRadius(4.px); fontSize(11.px) } }) { Text("Pagado") } }
                        Td { Button({ style { property("border", "none"); background("none"); color(SidebarActiveColor); cursor("pointer") } }) { Text("Descargar ZIP") } }
                    }
                }
            }
        }
    }
}

@Composable
fun TrainingModule() {
    Div({ style { backgroundColor(Color.white); padding(32.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        H3 { Text("Centro de Capacitación") }
        Div({ style { display(DisplayStyle.Grid); property("grid-template-columns", "repeat(auto-fill, minmax(280.px, 1fr))"); gap(20.px) } }) {
            listOf(
                "Seguridad Industrial (EHS)" to "85%",
                "Manejo de Sustancias" to "40%",
                "Cultura NAF Connect" to "100%",
                "Primeros Auxilios" to "15%"
            ).forEach { (course, prog) ->
                Div({ style { padding(20.px); property("border", "1px solid #e2e8f0"); borderRadius(12.px) } }) {
                    H4({ style { margin(0.px) } }) { Text(course) }
                    Div({ style { height(8.px); width(100.percent); backgroundColor(Color("#f1f5f9")); borderRadius(4.px); property("margin", "16px 0") } }) {
                        Div({ style { height(100.percent); property("width", prog); backgroundColor(SidebarActiveColor); borderRadius(4.px) } })
                    }
                    P({ style { fontSize(12.px); color(Color.gray); margin(0.px); textAlign("right") } }) { Text("Progreso: $prog") }
                }
            }
        }
    }
}

@Composable
fun PerformanceModule() {
    Div({ style { backgroundColor(Color.white); padding(32.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        H3 { Text("Evaluación de Desempeño") }
        Div({ style { display(DisplayStyle.Flex); gap(24.px); alignItems(AlignItems.Center); marginBottom(32.px) } }) {
            Div({ style { flex(1); padding(24.px); backgroundColor(Color("#f8fafc")); borderRadius(12.px); textAlign("center") } }) {
                H2({ style { margin(0.px); color(SidebarActiveColor) } }) { Text("4.8 / 5.0") }
                P({ style { color(Color.gray) } }) { Text("Calificación Promedio Planta") }
            }
            Div({ style { flex(2) } }) {
                Text("Ciclo de evaluación actual: Junio - Diciembre 2024")
                Div({ style { marginTop(12.px); display(DisplayStyle.Flex); gap(12.px) } }) {
                    Button({ style { padding(8.px, 16.px); backgroundColor(SidebarActiveColor); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") } }) { Text("Iniciar Evaluación 360") }
                }
            }
        }
    }
}

@Composable
fun VacationsModule() {
    Div({ style { backgroundColor(Color.white); padding(32.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        H3 { Text("Control de Vacaciones y Ausencias") }
        Div({ style { display(DisplayStyle.Grid); property("grid-template-columns", "2fr 1fr"); gap(24.px) } }) {
            Div {
                H4 { Text("Solicitudes Pendientes") }
                listOf("Juan Perez (3 días)", "Ana López (5 días)").forEach { req ->
                    Div({ style { padding(16.px); backgroundColor(Color("#fffbeb")); property("border-left", "4px solid #f59e0b"); borderRadius(4.px); marginBottom(12.px); display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); alignItems(AlignItems.Center) } }) {
                        Text(req)
                        Div({ style { display(DisplayStyle.Flex); gap(8.px) } }) {
                            Button({ style { backgroundColor(Color("#22c55e")); color(Color.white); property("border", "none"); borderRadius(4.px); padding(4.px, 8.px); cursor("pointer") } }) { Text("✓") }
                            Button({ style { backgroundColor(Color("#ef4444")); color(Color.white); property("border", "none"); borderRadius(4.px); padding(4.px, 8.px); cursor("pointer") } }) { Text("✕") }
                        }
                    }
                }
            }
            Div({ style { padding(20.px); backgroundColor(Color("#f8fafc")); borderRadius(8.px) } }) {
                H4 { Text("Resumen Global") }
                P { Text("● 5 Empleados ausentes hoy") }
                P { Text("● 12 Vacaciones programadas este mes") }
            }
        }
    }
}

@Composable
fun DocumentsModule() {
    Div({ style { backgroundColor(Color.white); padding(32.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        H3 { Text("Repositorio de Documentos") }
        Div({ style { display(DisplayStyle.Grid); property("grid-template-columns", "repeat(4, 1fr)"); gap(20.px) } }) {
            listOf("Contratos", "Identificaciones", "Políticas", "Certificados").forEach { folder ->
                Div({ style { textAlign("center"); padding(24.px); property("border", "1px solid #e2e8f0"); borderRadius(12.px); cursor("pointer") } }) {
                    Div({ style { width(48.px); height(48.px); backgroundColor(Color("#cbd5e1")); borderRadius(8.px); property("margin", "0 auto 12.px") } })
                    Text(folder)
                }
            }
        }
    }
}

@Composable
fun ReportsModule() {
    Div({ style { backgroundColor(Color.white); padding(32.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        H3 { Text("Reportes y Estadísticas") }
        Div({ style { display(DisplayStyle.Grid); property("grid-template-columns", "1fr 1fr"); gap(24.px) } }) {
            Div({ style { height(250.px); backgroundColor(Color("#f1f5f9")); borderRadius(8.px); display(DisplayStyle.Flex); alignItems(AlignItems.Center); justifyContent(JustifyContent.Center) } }) {
                Text("Gráfica: Distribución por Departamentos")
            }
            Div({ style { height(250.px); backgroundColor(Color("#f1f5f9")); borderRadius(8.px); display(DisplayStyle.Flex); alignItems(AlignItems.Center); justifyContent(JustifyContent.Center) } }) {
                Text("Gráfica: Índice de Rotación Anual")
            }
        }
        Div({ style { marginTop(32.px); textAlign("center") } }) {
            Button({ style { padding(12.px, 24.px); backgroundColor(SidebarColor); color(Color.white); property("border", "none"); borderRadius(8.px); cursor("pointer") } }) { Text("Generar Reporte Anual NAF (PDF)") }
        }
    }
}

@Composable fun PlaceholderModule(name: String) {
    Div({ style { textAlign("center"); padding(100.px); backgroundColor(Color.white); borderRadius(12.px) } }) {
        H2 { Text("Módulo $name") }
        P({ style { color(Color.gray) } }) { Text("Esta sección está siendo integrada con los flujos de trabajo industriales.") }
    }
}
