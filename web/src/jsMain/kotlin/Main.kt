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

fun main() {
    val client = HttpClient(Js) {
        install(ContentNegotiation) {
            json()
        }
    }

    renderComposable(rootElementId = "root") {
        var isLoggedIn by remember { mutableStateOf(false) }
        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var loginError by remember { mutableStateOf("") }
        
        var employees by remember { mutableStateOf(emptyList<Employee>()) }
        val scope = rememberCoroutineScope()

        if (!isLoggedIn) {
            // PANTALLA DE LOGIN
            Div({
                style {
                    display(DisplayStyle.Flex)
                    flexDirection(FlexDirection.Column)
                    alignItems(AlignItems.Center)
                    justifyContent(JustifyContent.Center)
                    height(100.vh)
                    backgroundColor(Color.whitesmoke)
                    fontFamily("sans-serif")
                }
            }) {
                Div({
                    style {
                        padding(40.px)
                        backgroundColor(Color.white)
                        borderRadius(12.px)
                        property("box-shadow", "0 4px 6px rgba(0,0,0,0.1)")
                        display(DisplayStyle.Flex)
                        flexDirection(FlexDirection.Column)
                        width(300.px)
                    }
                }) {
                    H2 { Text("RH NAF Industrial") }
                    P { Text("Acceso Administrativo") }

                    Input(InputType.Text) {
                        style {
                            padding(10.px)
                            marginBottom(10.px)
                            borderRadius(4.px)
                            property("border", "1px solid lightgray")
                        }
                        placeholder("Usuario")
                        onInput { username = it.value }
                    }

                    Input(InputType.Password) {
                        style {
                            padding(10.px)
                            marginBottom(20.px)
                            borderRadius(4.px)
                            property("border", "1px solid lightgray")
                        }
                        placeholder("Contraseña")
                        onInput { password = it.value }
                    }

                    Button({
                        style {
                            padding(10.px)
                            backgroundColor(Color.darkblue)
                            color(Color.white)
                            property("border", "none")
                            borderRadius(4.px)
                            cursor("pointer")
                        }
                        onClick {
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
                                        loginError = "Usuario o contraseña incorrectos"
                                    }
                                } catch (e: Exception) {
                                    loginError = "Error de conexión con el servidor"
                                }
                            }
                        }
                    }) {
                        Text("Iniciar Sesión")
                    }

                    if (loginError.isNotEmpty()) {
                        P({ style { color(Color.red); fontSize(12.px); marginTop(10.px) } }) {
                            Text(loginError)
                        }
                    }
                }
            }
        } else {
            // DASHBOARD (YA LOGUEADO)
            Div({
                style {
                    padding(20.px)
                    fontFamily("sans-serif")
                }
            }) {
                Header({
                    style {
                        display(DisplayStyle.Flex)
                        justifyContent(JustifyContent.SpaceBetween)
                        alignItems(AlignItems.Center)
                        property("border-bottom", "1px solid lightgray")
                        marginBottom(20.px)
                    }
                }) {
                    H1 { Text("RH NAF - Panel de Control") }
                    Button({
                        style {
                            padding(8.px, 16.px)
                            backgroundColor(Color.gray)
                            color(Color.white)
                            property("border", "none")
                            borderRadius(4.px)
                            cursor("pointer")
                        }
                        onClick { isLoggedIn = false }
                    }) { Text("Cerrar Sesión") }
                }

                Div({
                    style {
                        display(DisplayStyle.Flex)
                        gap(20.px)
                    }
                }) {
                    // Sidebar
                    Nav({
                        style {
                            width(200.px)
                            property("border-right", "1px solid lightgray")
                        }
                    }) {
                        Ul {
                            Li { A(href = "#") { Text("Dashboard") } }
                            Li { A(href = "#") { Text("Empleados (${employees.size})") } }
                            Li { A(href = "#") { Text("Seguridad EHS") } }
                            Li { A(href = "#") { Text("IA - Hugging Face") } }
                        }
                    }

                    // Main Content
                    Main({ style { flex(1) } }) {
                        Section {
                            H2 { Text("Lista de Operadores") }
                            
                            Table({
                                style {
                                    width(100.percent)
                                    property("border-collapse", "collapse")
                                }
                            }) {
                                Thead {
                                    Tr {
                                        Th { Text("ID") }
                                        Th { Text("Nombre") }
                                        Th { Text("Puesto") }
                                        Th { Text("Departamento") }
                                        Th { Text("Estado") }
                                    }
                                }
                                Tbody {
                                    employees.forEach { emp ->
                                        Tr({
                                            style { property("border-bottom", "1px solid whitesmoke") }
                                        }) {
                                            Td({ style { padding(10.px) } }) { Text(emp.id) }
                                            Td({ style { padding(10.px) } }) { Text("${emp.firstName} ${emp.lastName}") }
                                            Td({ style { padding(10.px) } }) { Text(emp.position) }
                                            Td({ style { padding(10.px) } }) { Text(emp.department) }
                                            Td({ style { padding(10.px) } }) {
                                                Span({
                                                    style {
                                                        color(if (emp.status.name == "ACTIVE") Color.green else Color.red)
                                                        fontWeight("bold")
                                                    }
                                                }) { Text(emp.status.name) }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
