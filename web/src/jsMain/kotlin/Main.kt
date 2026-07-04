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
                    TopBar("Admin NAF", "Gestión Industrial")

                    Div({ style { padding(32.px) } }) {
                        when (activeModule) {
                            Module.DASHBOARD -> DashboardView(employees)
                            Module.EMPLOYEES -> {
                                if (selectedEmployee == null) {
                                    EmployeeListView(employees) { selectedEmployee = it }
                                } else {
                                    EmployeeDigitalFile(selectedEmployee!!) { selectedEmployee = null }
                                }
                            }
                            Module.INCIDENTS -> SafetyModule(client, scope)
                            Module.ATTENDANCE -> AttendanceModule()
                            else -> PlaceholderModule(activeModule.name)
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
            StatCard("Total Empleados", "1,248", "+12 este mes", Color("#6366f1"))
            StatCard("Empleados Activos", "1,180", "94.5% del total", Color("#22c55e"))
            StatCard("Vacantes Abiertas", "24", "-3 vs mes anterior", Color("#eab308"))
            StatCard("Capacitación Pendiente", "15", "Cursos por vencer", Color("#a855f7"))
            StatCard("Incidencias Hoy", "7", "+2 vs ayer", Color("#ef4444"))
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
fun TopBar(user: String, role: String) {
    Header({
        style {
            backgroundColor(Color.white); padding(12.px, 24.px); display(DisplayStyle.Flex)
            justifyContent(JustifyContent.SpaceBetween); alignItems(AlignItems.Center)
            property("border-bottom", "1px solid #e2e8f0")
        }
    }) {
        Div {
            H2({ style { margin(0.px); fontSize(16.px); fontWeight("bold") } }) { Text("Dashboard") }
            P({ style { margin(0.px); fontSize(12.px); color(Color("#64728b")) } }) { Text("Resumen general del sistema") }
        }
        Div({ style { display(DisplayStyle.Flex); alignItems(AlignItems.Center); gap(20.px) } }) {
            Div({ style { width(20.px); height(20.px); backgroundColor(Color("#64728b")); borderRadius(50.percent) } })
            Div({ style { display(DisplayStyle.Flex); alignItems(AlignItems.Center); gap(12.px) } }) {
                Div({ style { textAlign("right") } }) {
                    P({ style { margin(0.px); fontSize(14.px); fontWeight("600") } }) { Text(user) }
                    P({ style { margin(0.px); fontSize(12.px); color(Color("#64728b")) } }) { Text(role) }
                }
                Div({ style { width(36.px); height(36.px); backgroundColor(Color("#cbd5e1")); borderRadius(50.percent) } })
            }
        }
    }
}

@Composable
fun EmployeeListView(employees: List<Employee>, onSelect: (Employee) -> Unit) {
    Div({ style { backgroundColor(Color.white); padding(24.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        H3 { Text("Gestión de Personal") }
        Table({ style { width(100.percent); property("border-collapse", "collapse") } }) {
            Thead {
                Tr {
                    Th({ style { textAlign("left"); padding(12.px); property("border-bottom", "2px solid #f1f5f9") } }) { Text("ID") }
                    Th({ style { textAlign("left"); padding(12.px); property("border-bottom", "2px solid #f1f5f9") } }) { Text("Colaborador") }
                    Th({ style { textAlign("left"); padding(12.px); property("border-bottom", "2px solid #f1f5f9") } }) { Text("Departamento") }
                    Th({ style { textAlign("left"); padding(12.px); property("border-bottom", "2px solid #f1f5f9") } }) { Text("Estado") }
                    Th({ style { textAlign("left"); padding(12.px); property("border-bottom", "2px solid #f1f5f9") } }) { Text("Acción") }
                }
            }
            Tbody {
                employees.forEach { emp ->
                    Tr {
                        Td({ style { padding(12.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(emp.id) }
                        Td({ style { padding(12.px); property("border-bottom", "1px solid #f1f5f9") } }) {
                            Div({ style { display(DisplayStyle.Flex); alignItems(AlignItems.Center); gap(12.px) } }) {
                                Div({ style { width(32.px); height(32.px); backgroundColor(Color("#e2e8f0")); borderRadius(50.percent) } })
                                Div {
                                    P({ style { margin(0.px); fontWeight("600") } }) { Text("${emp.firstName} ${emp.lastName}") }
                                    P({ style { margin(0.px); fontSize(12.px); color(Color.gray) } }) { Text(emp.position) }
                                }
                            }
                        }
                        Td({ style { padding(12.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(emp.department) }
                        Td({ style { padding(12.px); property("border-bottom", "1px solid #f1f5f9") } }) { StatusBadge(emp.status) }
                        Td({ style { padding(12.px); property("border-bottom", "1px solid #f1f5f9") } }) {
                            Button({
                                style { padding(6.px, 12.px); backgroundColor(SidebarActiveColor); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") }
                                onClick { onSelect(emp) }
                            }) { Text("Ver Expediente") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmployeeDigitalFile(emp: Employee, onBack: () -> Unit) {
    Div {
        Button({ onClick { onBack() }; style { marginBottom(16.px); cursor("pointer"); backgroundColor(Color.white); property("border", "1px solid #ccc"); padding(8.px, 16.px); borderRadius(6.px) } }) { Text("← Regresar al listado") }
        
        Div({
            style {
                backgroundColor(Color.white); padding(32.px); borderRadius(16.px); property("box-shadow", CardShadow)
                display(DisplayStyle.Flex); gap(40.px)
            }
        }) {
            // Perfil
            Div({ style { width(220.px); textAlign("center") } }) {
                Div({ style { width(120.px); height(120.px); backgroundColor(Color("#e2e8f0")); borderRadius(50.percent); property("margin", "0 auto 20px") } })
                H3({ style { margin(0.px) } }) { Text("${emp.firstName} ${emp.lastName}") }
                P({ style { color(Color.gray); property("margin", "8px 0") } }) { Text(emp.position) }
                StatusBadge(emp.status)
                
                Div({ style { property("margin-top", "32px"); display(DisplayStyle.Flex); flexDirection(FlexDirection.Column); gap(10.px) } }) {
                    Button({ style { width(100.percent); padding(10.px); borderRadius(6.px); property("border", "1px solid #e2e8f0"); backgroundColor(Color.white); cursor("pointer") } }) { Text("Editar Perfil") }
                    Button({ style { width(100.percent); padding(10.px); borderRadius(6.px); property("border", "1px solid #e2e8f0"); backgroundColor(Color.white); cursor("pointer") } }) { Text("Descargar PDF") }
                }
            }

            // Datos
            Div({ style { flex(1) } }) {
                H2({ style { property("border-bottom", "2px solid #3b82f6"); display(DisplayStyle.InlineBlock); paddingBottom(8.px); marginBottom(24.px) } }) { Text("Expediente Digital del Empleado") }
                
                Div({ style { display(DisplayStyle.Grid); property("grid-template-columns", "1fr 1fr"); gap(32.px) } }) {
                    InfoSection("Identidad y Legal", listOf(
                        "CURP" to (emp.curp ?: "No registrado"),
                        "RFC" to (emp.rfc ?: "No registrado"),
                        "NSS" to (emp.nss ?: "No registrado"),
                        "INE" to (emp.ine ?: "Vigente")
                    ))
                    InfoSection("Información Laboral", listOf(
                        "Departamento" to emp.department,
                        "Fecha Ingreso" to emp.entryDate,
                        "Supervisor" to (emp.supervisor ?: "N/A"),
                        "Tipo Contrato" to (emp.contractType ?: "Indeterminado")
                    ))
                    InfoSection("Salud Ocupacional", listOf(
                        "Examen Médico" to "Realizado (Vence 2025)",
                        "Historial Médico" to (emp.medicalHistory ?: "Sin observaciones"),
                        "Restricciones" to "Ninguna"
                    ))
                    InfoSection("Documentos Digitales", listOf(
                        "Contrato" to "✓ Cargado",
                        "Identificación" to "✓ Cargada",
                        "Certificados" to "✓ 2 archivos"
                    ))
                }
            }
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

@Composable fun PlaceholderModule(name: String) {
    Div({ style { textAlign("center"); padding(100.px); backgroundColor(Color.white); borderRadius(12.px) } }) {
        H2 { Text("Módulo $name") }
        P({ style { color(Color.gray) } }) { Text("Esta sección está siendo integrada con los flujos de trabajo industriales.") }
    }
}
