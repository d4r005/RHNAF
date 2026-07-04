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

// TEMAS Y COLORES INDUSTRIALES
val Blue900 = Color("#0d47a1")
val Blue700 = Color("#1976d2")
val Blue50 = Color("#e3f2fd")
val Gray100 = Color("#f5f5f5")
val Gray800 = Color("#424242")
val SuccessGreen = Color("#2e7d32")
val WarningOrange = Color("#ed6c02")
val ErrorRed = Color("#d32f2f")

enum class Module {
    DASHBOARD, EMPLOYEES, RECRUITMENT, ATTENDANCE, SAFETY, TRAINING, DOCUMENTS, SETTINGS
}

fun main() {
    val client = HttpClient(Js) {
        install(ContentNegotiation) { json() }
    }

    renderComposable(rootElementId = "root") {
        var isLoggedIn by remember { mutableStateOf(false) }
        var activeModule by remember { mutableStateOf(Module.DASHBOARD) }
        var selectedEmployee by remember { mutableStateOf<Employee?>(null) }
        
        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var employees by remember { mutableStateOf(emptyList<Employee>()) }
        val scope = rememberCoroutineScope()

        if (!isLoggedIn) {
            LoginScreen(
                onLogin = { u, p ->
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
            )
        } else {
            Div({
                style {
                    display(DisplayStyle.Flex)
                    height(100.vh)
                    fontFamily("Roboto", "Segoe UI", "sans-serif")
                    backgroundColor(Gray100)
                }
            }) {
                // SIDEBAR
                Sidebar(activeModule) { 
                    activeModule = it 
                    selectedEmployee = null
                }

                // CONTENIDO PRINCIPAL
                Div({ style { flex(1); display(DisplayStyle.Flex); flexDirection(FlexDirection.Column); overflowY("auto") } }) {
                    TopBar("Administrador RH")

                    Div({ style { padding(24.px) } }) {
                        when (activeModule) {
                            Module.DASHBOARD -> DashboardView(employees)
                            Module.EMPLOYEES -> {
                                if (selectedEmployee == null) {
                                    EmployeeListView(employees) { selectedEmployee = it }
                                } else {
                                    EmployeeDigitalFile(selectedEmployee!!) { selectedEmployee = null }
                                }
                            }
                            Module.SAFETY -> SafetyModule(client, scope)
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
            backgroundColor(Blue900)
            color(Color.white)
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Column)
        }
    }) {
        Div({ style { padding(32.px); textAlign("center"); borderBottom("1px solid #ffffff22") } }) {
            H2({ style { margin(0.px); fontSize(20.px) } }) { Text("RH NAF INDUSTRIAL") }
        }

        Div({ style { padding(16.px); flex(1) } }) {
            SidebarLink("Dashboard", Module.DASHBOARD, active == Module.DASHBOARD, onSelect)
            SidebarLink("Expediente Digital", Module.EMPLOYEES, active == Module.EMPLOYEES, onSelect)
            SidebarLink("Asistencia / QR", Module.ATTENDANCE, active == Module.ATTENDANCE, onSelect)
            SidebarLink("Seguridad EHS (IA)", Module.SAFETY, active == Module.SAFETY, onSelect)
            SidebarLink("Capacitación / CTPAT", Module.TRAINING, active == Module.TRAINING, onSelect)
            SidebarLink("Reclutamiento", Module.RECRUITMENT, active == Module.RECRUITMENT, onSelect)
            SidebarLink("Gestión Documental", Module.DOCUMENTS, active == Module.DOCUMENTS, onSelect)
        }
    }
}

@Composable
fun SidebarLink(label: String, mod: Module, isSelected: Boolean, onSelect: (Module) -> Unit) {
    Div({
        style {
            padding(12.px, 20.px)
            marginBottom(4.px)
            borderRadius(8.px)
            cursor("pointer")
            if (isSelected) backgroundColor(Color("#ffffff15"))
            property("transition", "0.2s")
        }
        onClick { onSelect(mod) }
    }) {
        Text(label)
    }
}

@Composable
fun DashboardView(employees: List<Employee>) {
    Div {
        H2 { Text("Panel de Control Estratégico") }
        
        // KPIs
        Div({
            style {
                display(DisplayStyle.Grid)
                property("grid-template-columns", "repeat(auto-fit, minmax(200.px, 1fr))")
                gap(20.px); marginBottom(32.px)
            }
        }) {
            KPICard("Total Personal", employees.size.toString(), Blue700)
            KPICard("Activos", employees.count { it.status == EmployeeStatus.ACTIVE }.toString(), SuccessGreen)
            KPICard("En Vacaciones", employees.count { it.status == EmployeeStatus.VACATION }.toString(), WarningOrange)
            KPICard("Ausentismo %", "2.4%", ErrorRed)
        }

        Div({
            style {
                display(DisplayStyle.Flex); gap(20.px)
            }
        }) {
            // Alertas
            DashboardPanel("Alertas de Hoy", 1) {
                AlertItem("Vencimiento de Contrato: 3 empleados", ErrorRed)
                AlertItem("Examen Médico Pendiente: OP-105", WarningOrange)
                AlertItem("Capacitación CTPAT Vence: Mañana", Blue700)
            }
            // Cumpleaños
            DashboardPanel("Próximos Cumpleaños", 1) {
                P { Text("Pedro García - 05 de Julio") }
                P { Text("María López - 12 de Julio") }
            }
        }
    }
}

@Composable
fun EmployeeListView(employees: List<Employee>, onSelect: (Employee) -> Unit) {
    Div({ style { backgroundColor(Color.white); padding(24.px); borderRadius(12.px) } }) {
        H3 { Text("Listado de Personal") }
        Table({ style { width(100.percent); property("border-collapse", "collapse") } }) {
            Thead {
                Tr {
                    Th({ style { textAlign("left"); padding(12.px); borderBottom("2px solid #eee") } }) { Text("ID / Foto") }
                    Th({ style { textAlign("left"); padding(12.px); borderBottom("2px solid #eee") } }) { Text("Nombre Completo") }
                    Th({ style { textAlign("left"); padding(12.px); borderBottom("2px solid #eee") } }) { Text("Puesto") }
                    Th({ style { textAlign("left"); padding(12.px); borderBottom("2px solid #eee") } }) { Text("Antigüedad") }
                    Th({ style { textAlign("left"); padding(12.px); borderBottom("2px solid #eee") } }) { Text("Acción") }
                }
            }
            Tbody {
                employees.forEach { emp ->
                    Tr {
                        Td({ style { padding(12.px); borderBottom("1px solid #eee") } }) { Text(emp.id) }
                        Td({ style { padding(12.px); borderBottom("1px solid #eee"); fontWeight("bold") } }) { Text("${emp.firstName} ${emp.lastName}") }
                        Td({ style { padding(12.px); borderBottom("1px solid #eee") } }) { Text(emp.position) }
                        Td({ style { padding(12.px); borderBottom("1px solid #eee") } }) { Text(emp.entryDate) }
                        Td({ style { padding(12.px); borderBottom("1px solid #eee") } }) {
                            Button({
                                style { padding(6.px, 12.px); backgroundColor(Blue700); color(Color.white); border("none"); borderRadius(4.px); cursor("pointer") }
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
        Button({ onClick { onBack() }; style { marginBottom(16.px); cursor("pointer") } }) { Text("← Volver a lista") }
        
        Div({
            style {
                backgroundColor(Color.white); padding(32.px); borderRadius(16.px); display(DisplayStyle.Flex); gap(32.px)
            }
        }) {
            // Columna Izquierda: Foto y Datos Base
            Div({ style { width(200.px); textAlign("center") } }) {
                Div({ style { width(150.px); height(150.px); backgroundColor(Color.lightgray); borderRadius(50.percent); margin("0 auto 16.px") } })
                H3 { Text("${emp.firstName} ${emp.lastName}") }
                P({ style { color(Color.gray) } }) { Text(emp.position) }
                StatusBadge(emp.status)
            }

            // Columna Derecha: Pestañas de información
            Div({ style { flex(1) } }) {
                H2 { Text("Expediente Digital Electrónico") }
                
                Div({ style { display(DisplayStyle.Grid); property("grid-template-columns", "1fr 1fr"); gap(24.px) } }) {
                    InfoGroup("Datos Legales", listOf("CURP: ${emp.curp ?: "N/A"}", "RFC: ${emp.rfc ?: "N/A"}", "NSS: ${emp.nss ?: "N/A"}"))
                    InfoGroup("Organizacional", listOf("Departamento: ${emp.department}", "Supervisor: ${emp.supervisor ?: "N/A"}", "Ingreso: ${emp.entryDate}"))
                    InfoGroup("Salud y Seguridad", listOf("Historial Médico: ${emp.medicalHistory ?: "Sin registros"}", "Equipo EPP: Casco, Botas, Lentes"))
                    InfoGroup("Documentos Digitales", listOf("INE.pdf", "Contrato_Firmado.pdf", "Certificado_Medico.jpg"))
                }
            }
        }
    }
}

// COMPONENTES AUXILIARES
@Composable fun KPICard(t: String, v: String, c: CSSColorValue) {
    Div({ style { backgroundColor(Color.white); padding(20.px); borderRadius(12.px); borderLeft("5px solid $c"); property("box-shadow", "0 2px 4px rgba(0,0,0,0.05)") } }) {
        Span({ style { color(Color.gray); fontSize(14.px) } }) { Text(t) }
        Div({ style { fontSize(28.px); fontWeight("bold"); color(Gray800) } }) { Text(v) }
    }
}

@Composable fun DashboardPanel(t: String, f: Int, content: @Composable () -> Unit) {
    Div({ style { flex(f.toString()); backgroundColor(Color.white); padding(20.px); borderRadius(12.px) } }) {
        H3({ style { borderBottom("1px solid #eee"); paddingBottom(10.px) } }) { Text(t) }
        content()
    }
}

@Composable fun AlertItem(t: String, c: CSSColorValue) {
    Div({ style { padding(8.px); marginBottom(8.px); backgroundColor(Blue50); borderLeft("3px solid $c"); fontSize(14.px) } }) { Text(t) }
}

@Composable fun InfoGroup(title: String, items: List<String>) {
    Div({ style { marginBottom(20.px) } }) {
        H4({ style { color(Blue700); margin("0 0 8px 0") } }) { Text(title) }
        Ul({ style { paddingLeft(20.px); margin(0.px) } }) {
            items.forEach { Li { Text(it) } }
        }
    }
}

@Composable fun StatusBadge(s: EmployeeStatus) {
    Span({
        style {
            padding(4.px, 12.px); borderRadius(20.px); color(Color.white); fontSize(12.px); fontWeight("bold")
            backgroundColor(when(s) {
                EmployeeStatus.ACTIVE -> SuccessGreen
                EmployeeStatus.VACATION -> WarningOrange
                else -> ErrorRed
            })
        }
    }) { Text(s.name) }
}

@Composable fun TopBar(user: String) {
    Header({ style { backgroundColor(Color.white); padding(16.px, 32.px); display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); alignItems(AlignItems.Center); borderBottom("1px solid #eee") } }) {
        Div { Text("Bienvenido, $user") }
        Button({ style { cursor("pointer") } }) { Text("Soporte Técnico") }
    }
}

@Composable fun LoginScreen(onLogin: (String, String) -> Unit) {
    var u by remember { mutableStateOf("") }
    var p by remember { mutableStateOf("") }
    Div({ style { display(DisplayStyle.Flex); alignItems(AlignItems.Center); justifyContent(JustifyContent.Center); height(100.vh); backgroundColor(Blue900) } }) {
        Div({ style { backgroundColor(Color.white); padding(48.px); borderRadius(16.px); width(320.px); textAlign("center") } }) {
            H1({ style { color(Blue900) } }) { Text("RH NAF") }
            P { Text("Sistema Industrial") }
            Input(InputType.Text) { placeholder("Usuario"); style { width(100.percent); padding(10.px); margin("10px 0"); borderRadius(4.px); border("1px solid #ccc") }; onInput { u = it.value } }
            Input(InputType.Password) { placeholder("Contraseña"); style { width(100.percent); padding(10.px); margin("10px 0"); borderRadius(4.px); border("1px solid #ccc") }; onInput { p = it.value } }
            Button({ style { width(100.percent); padding(12.px); backgroundColor(Blue900); color(Color.white); border("none"); borderRadius(4.px); cursor("pointer"); marginTop(16.px) }; onClick { onLogin(u, p) } }) { Text("Entrar") }
        }
    }
}

@Composable fun PlaceholderModule(name: String) {
    Div({ style { textAlign("center"); padding(100.px) } }) {
        H2 { Text("Módulo $name en Desarrollo") }
        P { Text("Estamos trabajando para integrar esta funcionalidad.") }
    }
}

@Composable fun SafetyModule(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope) {
    var desc by remember { mutableStateOf("") }
    var res by remember { mutableStateOf("") }
    Div({ style { backgroundColor(Color.white); padding(32.px); borderRadius(12.px) } }) {
        H3 { Text("Investigación de Incidentes EHS (IA)") }
        P { Text("Describa el evento para análisis de riesgo inmediato.") }
        TextArea({ style { width(100.percent); height(120.px); margin("16px 0") }; onInput { desc = it.value } })
        Button({ 
            style { padding(12.px, 24.px); backgroundColor(Blue900); color(Color.white); border("none"); borderRadius(6.px); cursor("pointer") }
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
        }) { Text("Analizar Incidente") }
        if (res.isNotEmpty()) {
            Div({ style { marginTop(24.px); padding(16.px); backgroundColor(Blue50); borderLeft("4px solid $Blue900") } }) { Text(res) }
        }
    }
}
