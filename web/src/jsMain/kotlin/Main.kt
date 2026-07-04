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
import com.example.rhnaf.shared.model.Employee
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLFormElement

enum class NavItem {
    DASHBOARD, EMPLOYEES, SAFETY_EHS, AI_HUGGINGFACE
}

val PrimaryBlue = Color("#0d47a1")
val BackgroundGray = Color("#f4f7f9")
val TextDark = Color("#2c3e50")

fun main() {
    val client = HttpClient(Js) {
        install(ContentNegotiation) {
            json()
        }
    }

    renderComposable(rootElementId = "root") {
        var isLoggedIn by remember { mutableStateOf(false) }
        var currentNav by remember { mutableStateOf(NavItem.DASHBOARD) }
        
        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var loginError by remember { mutableStateOf("") }
        
        var employees by remember { mutableStateOf(emptyList<Employee>()) }
        val scope = rememberCoroutineScope()

        if (!isLoggedIn) {
            LoginScreen(
                onUsernameChange = { username = it },
                onPasswordChange = { password = it },
                loginError = loginError,
                onLoginClick = {
                    scope.launch {
                        try {
                            val response = client.post("/api/login") {
                                contentType(ContentType.Application.Json)
                                setBody(mapOf("username" to username, "password" to password))
                            }
                            if (response.status == HttpStatusCode.OK) {
                                isLoggedIn = true
                                employees = client.get("/api/employees").body()
                            } else {
                                loginError = "Credenciales incorrectas"
                            }
                        } catch (e: Exception) {
                            loginError = "Error de conexión"
                        }
                    }
                }
            )
        } else {
            // APP LAYOUT
            MainLayout(
                currentNav = currentNav,
                onNavChange = { currentNav = it },
                onLogout = { isLoggedIn = false }
            ) {
                when (currentNav) {
                    NavItem.DASHBOARD -> DashboardContent(employees)
                    NavItem.EMPLOYEES -> EmployeesContent(employees)
                    NavItem.SAFETY_EHS -> SafetyContent(client, scope)
                    NavItem.AI_HUGGINGFACE -> AIHuggingFaceContent(client, scope)
                }
            }
        }
    }
}

@Composable
fun LoginScreen(
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    loginError: String,
    onLoginClick: () -> Unit
) {
    Div({
        style {
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Column)
            alignItems(AlignItems.Center)
            justifyContent(JustifyContent.Center)
            height(100.vh)
            backgroundColor(BackgroundGray)
            fontFamily("Segoe UI", "Tahoma", "Geneva", "Verdana", "sans-serif")
        }
    }) {
        Div({
            style {
                padding(40.px)
                backgroundColor(Color.white)
                borderRadius(16.px)
                property("box-shadow", "0 10px 25px rgba(0,0,0,0.1)")
                display(DisplayStyle.Flex)
                flexDirection(FlexDirection.Column)
                width(350.px)
                textAlign("center")
            }
        }) {
            Div({
                style {
                    color(PrimaryBlue)
                    fontSize(2.em)
                    fontWeight("bold")
                    marginBottom(10.px)
                }
            }) { Text("RH NAF Industrial") }
            
            P({ style { color(Color.gray); marginBottom(30.px) } }) { 
                Text("Sistema de Gestión de Capital Humano") 
            }

            Input(InputType.Text) {
                style {
                    padding(12.px)
                    marginBottom(15.px)
                    borderRadius(8.px)
                    property("border", "1.5px solid #e0e0e0")
                    property("outline", "none")
                }
                placeholder("Usuario de Red")
                onInput { onUsernameChange(it.value) }
            }

            Input(InputType.Password) {
                style {
                    padding(12.px)
                    marginBottom(25.px)
                    borderRadius(8.px)
                    property("border", "1.5px solid #e0e0e0")
                    property("outline", "none")
                }
                placeholder("Contraseña")
                onInput { onPasswordChange(it.value) }
            }

            Button({
                style {
                    padding(14.px)
                    backgroundColor(PrimaryBlue)
                    color(Color.white)
                    property("border", "none")
                    borderRadius(8.px)
                    cursor("pointer")
                    fontWeight("bold")
                    fontSize(1.em)
                    property("transition", "background-color 0.3s")
                }
                onClick { onLoginClick() }
            }) {
                Text("Ingresar al Portal")
            }

            if (loginError.isNotEmpty()) {
                P({ style { color(Color.red); fontSize(14.px); marginTop(15.px) } }) {
                    Text(loginError)
                }
            }
        }
        
        P({ style { marginTop(20.px); color(Color.gray); fontSize(12.px) } }) {
            Text("© 2026 RH NAF Industrial. Todos los derechos reservados.")
        }
    }
}

@Composable
fun MainLayout(
    currentNav: NavItem,
    onNavChange: (NavItem) -> Unit,
    onLogout: () -> Unit,
    content: @Composable () -> Unit
) {
    Div({
        style {
            display(DisplayStyle.Flex)
            height(100.vh)
            fontFamily("Segoe UI", "sans-serif")
            backgroundColor(BackgroundGray)
        }
    }) {
        // SIDEBAR
        Nav({
            style {
                width(260.px)
                backgroundColor(PrimaryBlue)
                color(Color.white)
                display(DisplayStyle.Flex)
                flexDirection(FlexDirection.Column)
                padding(20.px)
            }
        }) {
            H3({ style { marginBottom(40.px); textAlign("center") } }) { Text("RH NAF PANEL") }

            SidebarItem("Dashboard", NavItem.DASHBOARD, currentNav == NavItem.DASHBOARD) { onNavChange(NavItem.DASHBOARD) }
            SidebarItem("Colaboradores", NavItem.EMPLOYEES, currentNav == NavItem.EMPLOYEES) { onNavChange(NavItem.EMPLOYEES) }
            SidebarItem("Seguridad EHS", NavItem.SAFETY_EHS, currentNav == NavItem.SAFETY_EHS) { onNavChange(NavItem.SAFETY_EHS) }
            SidebarItem("Inteligencia Artificial", NavItem.AI_HUGGINGFACE, currentNav == NavItem.AI_HUGGINGFACE) { onNavChange(NavItem.AI_HUGGINGFACE) }

            Div({ style { marginTop(DisplayStyle.Auto.toString()) } }) {
                Button({
                    style {
                        width(100.percent)
                        padding(10.px)
                        backgroundColor(Color("#ffffff33"))
                        color(Color.white)
                        property("border", "none")
                        borderRadius(6.px)
                        cursor("pointer")
                    }
                    onClick { onLogout() }
                }) { Text("Cerrar Sesión") }
            }
        }

        // MAIN CONTENT AREA
        Div({
            style {
                flex(1)
                display(DisplayStyle.Flex)
                flexDirection(FlexDirection.Column)
                overflowY("auto")
            }
        }) {
            Header({
                style {
                    backgroundColor(Color.white)
                    padding(15.px, 30.px)
                    display(DisplayStyle.Flex)
                    justifyContent(JustifyContent.SpaceBetween)
                    alignItems(AlignItems.Center)
                    property("box-shadow", "0 2px 4px rgba(0,0,0,0.05)")
                }
            }) {
                H2({ style { margin(0.px); color(TextDark) } }) {
                    Text(when(currentNav) {
                        NavItem.DASHBOARD -> "Panel Principal"
                        NavItem.EMPLOYEES -> "Gestión de Personal"
                        NavItem.SAFETY_EHS -> "Incidentes y Seguridad"
                        NavItem.AI_HUGGINGFACE -> "Servicios de IA"
                    })
                }
                Span({ style { color(Color.gray) } }) { Text("Admin: RH_NAF_01") }
            }

            Div({ style { padding(30.px) } }) {
                content()
            }
        }
    }
}

@Composable
fun SidebarItem(label: String, item: NavItem, isSelected: Boolean, onClick: () -> Unit) {
    Div({
        style {
            padding(12.px, 20.px)
            marginBottom(10.px)
            borderRadius(8.px)
            cursor("pointer")
            if (isSelected) backgroundColor(Color("#ffffff22"))
            property("transition", "0.2s")
        }
        onClick { onClick() }
    }) {
        Text(label)
    }
}

@Composable
fun DashboardContent(employees: List<Employee>) {
    Div {
        Div({
            style {
                display(DisplayStyle.Grid)
                property("grid-template-columns", "repeat(auto-fit, minmax(240.px, 1fr))")
                gap(20.px)
                marginBottom(30.px)
            }
        }) {
            StatCard("Total Personal", employees.size.toString(), PrimaryBlue)
            StatCard("Activos", employees.count { it.status.name == "ACTIVE" }.toString(), Color.green)
            StatCard("En Vacaciones", employees.count { it.status.name == "VACATION" }.toString(), Color.orange)
            StatCard("Incidentes Mes", "0", Color.red)
        }

        Div({
            style {
                backgroundColor(Color.white)
                padding(25.px)
                borderRadius(12.px)
                property("box-shadow", "0 4px 10px rgba(0,0,0,0.03)")
            }
        }) {
            H3 { Text("Resumen Operativo") }
            P { Text("Bienvenido al sistema RH NAF. Aquí podrá gestionar los expedientes de los colaboradores, analizar reportes de seguridad mediante IA y automatizar el registro de datos.") }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, accent: CSSColorValue) {
    Div({
        style {
            backgroundColor(Color.white)
            padding(20.px)
            borderRadius(12.px)
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Column)
            property("box-shadow", "0 4px 6px rgba(0,0,0,0.02)")
            property("border-left", "5.px solid $accent")
        }
    }) {
        Span({ style { color(Color.gray); fontSize(14.px) } }) { Text(title) }
        Span({ style { fontSize(28.px); fontWeight("bold"); color(TextDark) } }) { Text(value) }
    }
}

@Composable
fun EmployeesContent(employees: List<Employee>) {
    Div({
        style {
            backgroundColor(Color.white)
            padding(20.px)
            borderRadius(12.px)
        }
    }) {
        Table({
            style {
                width(100.percent)
                property("border-collapse", "collapse")
            }
        }) {
            Thead {
                Tr {
                    Th({ style { textAlign("left"); padding(12.px); borderBottom("2px solid #eee") } }) { Text("ID") }
                    Th({ style { textAlign("left"); padding(12.px); borderBottom("2px solid #eee") } }) { Text("Nombre Completo") }
                    Th({ style { textAlign("left"); padding(12.px); borderBottom("2px solid #eee") } }) { Text("Departamento") }
                    Th({ style { textAlign("left"); padding(12.px); borderBottom("2px solid #eee") } }) { Text("Puesto") }
                    Th({ style { textAlign("left"); padding(12.px); borderBottom("2px solid #eee") } }) { Text("Estado") }
                }
            }
            Tbody {
                employees.forEach { emp ->
                    Tr({
                        style { property("border-bottom", "1px solid #f9f9f9") }
                    }) {
                        Td({ style { padding(12.px) } }) { Text(emp.id) }
                        Td({ style { padding(12.px); fontWeight("500") } }) { Text("${emp.firstName} ${emp.lastName}") }
                        Td({ style { padding(12.px) } }) { Text(emp.department) }
                        Td({ style { padding(12.px) } }) { Text(emp.position) }
                        Td({ style { padding(12.px) } }) {
                            Span({
                                style {
                                    padding(4.px, 10.px)
                                    borderRadius(20.px)
                                    fontSize(12.px)
                                    fontWeight("bold")
                                    color(Color.white)
                                    backgroundColor(when(emp.status.name) {
                                        "ACTIVE" -> Color.green
                                        "VACATION" -> Color.orange
                                        else -> Color.red
                                    })
                                }
                            }) { Text(emp.status.name) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SafetyContent(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope) {
    var description by remember { mutableStateOf("") }
    var analysisResult by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Div({
        style {
            backgroundColor(Color.white)
            padding(30.px)
            borderRadius(12.px)
            maxWidth(800.px)
        }
    }) {
        H3 { Text("Reporte de Incidente (Análisis IA)") }
        P({ style { color(Color.gray); marginBottom(20.px) } }) { 
            Text("Describa el incidente ocurrido en planta. La IA analizará la gravedad y categoría automáticamente.") 
        }

        TextArea({
            style {
                width(100.percent)
                height(150.px)
                padding(15.px)
                borderRadius(8.px)
                property("border", "1.5px solid #eee")
                property("resize", "none")
                marginBottom(20.px)
            }
            placeholder("Ej: Se detectó derrame de aceite en línea 4, operador con resbalón leve...")
            onInput { description = it.value }
        })

        Button({
            style {
                padding(12.px, 25.px)
                backgroundColor(if (isLoading) Color.gray else PrimaryBlue)
                color(Color.white)
                property("border", "none")
                borderRadius(8.px)
                cursor("pointer")
                fontWeight("bold")
            }
            if (isLoading) disabled()
            onClick {
                if (description.isBlank()) return@onClick
                isLoading = true
                scope.launch {
                    try {
                        val response = client.post("/api/safety/analyze") {
                            contentType(ContentType.Application.Json)
                            setBody(mapOf("description" to description))
                        }
                        val body: Map<String, String> = response.body()
                        analysisResult = body["analysis"] ?: "No se pudo obtener análisis"
                    } catch (e: Exception) {
                        analysisResult = "Error al conectar con el servicio de IA"
                    } finally {
                        isLoading = false
                    }
                }
            }
        }) {
            Text(if (isLoading) "Analizando..." else "Enviar para Análisis")
        }

        if (analysisResult.isNotEmpty()) {
            Div({
                style {
                    marginTop(30.px)
                    padding(20.px)
                    backgroundColor(Color("#e3f2fd"))
                    borderRadius(8.px)
                    property("border-left", "4px solid $PrimaryBlue")
                }
            }) {
                H4({ style { margin(0.px, 0.px, 10.px, 0.px) } }) { Text("Resultado de la IA:") }
                P { Text(analysisResult) }
            }
        }
    }
}

@Composable
fun AIHuggingFaceContent(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope) {
    var ocrResult by remember { mutableStateOf("") }
    
    Div({
        style {
            backgroundColor(Color.white)
            padding(30.px)
            borderRadius(12.px)
        }
    }) {
        H3 { Text("Digitalización de Documentos (OCR)") }
        P { Text("En esta sección podrá cargar fotos de credenciales o contratos para extraer texto automáticamente mediante modelos de visión artificial.") }
        
        Div({
            style {
                padding(40.px)
                border("2px dashed lightgray")
                borderRadius(12.px)
                textAlign("center")
                marginTop(20.px)
            }
        }) {
            Text("Módulo de carga en desarrollo. Use la función de incidentes para probar la integración con Hugging Face.")
        }
    }
}
