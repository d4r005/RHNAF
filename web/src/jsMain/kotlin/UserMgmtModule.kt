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
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val email: String = "",
    val role: String = "EMPLEADO",
    val name: String = ""
)

@Composable
fun UserMgmtModule(
    client: HttpClient,
    scope: kotlinx.coroutines.CoroutineScope,
    t: Translations,
    authToken: String
) {
    var users by remember { mutableStateOf(emptyList<UserDto>()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var editUser by remember { mutableStateOf<UserDto?>(null) }

    fun load() {
        scope.launch {
            isLoading = true
            errorMsg = ""
            try {
                val resp: List<Map<String, String>> = client.get("$BACKEND_URL/api/users").body()
                users = resp.map { UserDto(email = it["email"] ?: "", role = it["role"] ?: "EMPLEADO", name = it["name"] ?: "") }
            } catch (e: Exception) {
                errorMsg = "Error al cargar usuarios: ${e.message}"
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    fun addUser(email: String, name: String, role: String, password: String) {
        scope.launch {
            try {
                val resp = client.post("$BACKEND_URL/api/user/add") {
                    contentType(ContentType.Application.Json)
                    header(HttpHeaders.Authorization, "Bearer $authToken")
                    setBody(mapOf("email" to email, "name" to name, "role" to role, "password" to password))
                }
                if (resp.status == HttpStatusCode.Created) {
                    showAddDialog = false
                    load()
                } else {
                    errorMsg = "Error: ${resp.bodyAsText()}"
                }
            } catch (e: Exception) {
                errorMsg = "Error de conexión: ${e.message}"
            }
        }
    }

    fun updateUser(email: String, name: String, role: String, password: String?) {
        scope.launch {
            try {
                val body = mutableMapOf("email" to email, "name" to name, "role" to role)
                if (password != null) body["password"] = password
                val resp = client.post("$BACKEND_URL/api/user/update") {
                    contentType(ContentType.Application.Json)
                    header(HttpHeaders.Authorization, "Bearer $authToken")
                    setBody(body)
                }
                if (resp.status == HttpStatusCode.OK) {
                    editUser = null
                    load()
                } else {
                    errorMsg = "Error: ${resp.bodyAsText()}"
                }
            } catch (e: Exception) {
                errorMsg = "Error de conexión: ${e.message}"
            }
        }
    }

    fun deleteUser(email: String) {
        scope.launch {
            try {
                val resp = client.delete("$BACKEND_URL/api/user/$email") {
                    header(HttpHeaders.Authorization, "Bearer $authToken")
                }
                if (resp.status == HttpStatusCode.OK) {
                    load()
                } else {
                    errorMsg = "Error: ${resp.bodyAsText()}"
                }
            } catch (e: Exception) {
                errorMsg = "Error de conexión: ${e.message}"
            }
        }
    }

    Div({ style { backgroundColor(Color.white); padding(32.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        Div({ style { display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); alignItems(AlignItems.Center); marginBottom(20.px) } }) {
            Div {
                H3({ style { margin(0.px, 0.px, 4.px, 0.px) } }) { Text(t.get("user_mgmt")) }
                P({ style { color(Color.gray); margin(0.px); fontSize(14.px) } }) { Text("Crear, editar y dar de baja usuarios del sistema") }
            }
            Button({
                style {
                    padding(8.px, 16.px); borderRadius(6.px); property("border", "none")
                    backgroundColor(SidebarActiveColor); color(Color.white); cursor("pointer"); fontWeight("bold")
                }
                onClick { showAddDialog = true }
            }) { Text("+ Nuevo Usuario") }
        }

        if (errorMsg.isNotEmpty()) {
            Div({ style { padding(16.px); backgroundColor(Color("#fef2f2")); borderRadius(8.px); color(Color("#dc2626")); marginBottom(20.px) } }) {
                P({ style { margin(0.px) } }) { Text(errorMsg) }
            }
        }

        if (isLoading) {
            P({ style { textAlign("center"); color(Color.gray); padding(40.px, 0.px) } }) { Text("Cargando...") }
        } else if (users.isEmpty()) {
            P({ style { textAlign("center"); color(Color.gray); padding(40.px, 0.px) } }) { Text("No hay usuarios registrados") }
        } else {
            Div({ style { display(DisplayStyle.Flex); flexDirection(FlexDirection.Column); gap(8.px) } }) {
                // Header row
                Div({ style { display(DisplayStyle.Grid); property("grid-template-columns", "2fr 2fr 1fr 1fr"); gap(12.px); padding(8.px, 16.px); property("border-bottom", "2px solid #e2e8f0") } }) {
                    Span({ style { fontSize(13.px); color(Color("#64748b")); fontWeight("bold") } }) { Text("Nombre") }
                    Span({ style { fontSize(13.px); color(Color("#64748b")); fontWeight("bold") } }) { Text("Email") }
                    Span({ style { fontSize(13.px); color(Color("#64748b")); fontWeight("bold") } }) { Text("Rol") }
                    Span({ style { fontSize(13.px); color(Color("#64748b")); fontWeight("bold"); textAlign("center") } }) { Text("Acciones") }
                }
                // User rows
                users.forEach { u ->
                    Div({ style { display(DisplayStyle.Grid); property("grid-template-columns", "2fr 2fr 1fr 1fr"); gap(12.px); padding(12.px, 16.px); property("border-bottom", "1px solid #f1f5f9"); alignItems(AlignItems.Center) } }) {
                        Span({ style { fontSize(14.px) } }) { Text(u.name) }
                        Span({ style { fontSize(14.px); color(Color("#64748b")) } }) { Text(u.email) }
                        val (roleColor, roleBg) = when (u.role) {
                            "ADMIN" -> Color("#991b1b") to Color("#fee2e2")
                            "RH" -> Color("#1e40af") to Color("#dbeafe")
                            else -> Color("#475569") to Color("#f1f5f9")
                        }
                        Span({
                            style {
                                padding(2.px, 10.px); borderRadius(99.px); fontSize(11.px); fontWeight("bold")
                                backgroundColor(roleBg); color(roleColor); textAlign("center")
                            }
                        }) { Text(u.role) }
                        Div({ style { display(DisplayStyle.Flex); gap(6.px); justifyContent(JustifyContent.Center) } }) {
                            Button({
                                style {
                                    padding(4.px, 10.px); borderRadius(6.px); fontSize(11.px); cursor("pointer")
                                    property("border", "1px solid #2563eb"); backgroundColor(Color.white); color(Color("#2563eb"))
                                }
                                onClick { editUser = u }
                            }) { Text("Editar") }
                            Button({
                                style {
                                    padding(4.px, 10.px); borderRadius(6.px); fontSize(11.px); cursor("pointer")
                                    property("border", "1px solid #ef4444"); backgroundColor(Color.white); color(Color("#ef4444"))
                                }
                                onClick { deleteUser(u.email) }
                            }) { Text("Eliminar") }
                        }
                    }
                }
            }
        }
    }

    // --- Dialog: Agregar usuario ---
    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var role by remember { mutableStateOf("EMPLEADO") }
        var password by remember { mutableStateOf("") }

        ModalDialog("Nuevo Usuario") {
            UserFormField("Nombre", name) { name = it }
            UserFormField("Email", email) { email = it }
            // Rol: botones en lugar de Select (Compose Web no soporta Select/Option facil)
            Div({ style { marginBottom(12.px) } }) {
                Label { Text("Rol") }
                Div({ style { display(DisplayStyle.Flex); gap(8.px); marginTop(8.px) } }) {
                    listOf("ADMIN", "RH", "EMPLEADO").forEach { r ->
                        Button({
                            style {
                                padding(6.px, 14.px); borderRadius(6.px); fontSize(12.px); cursor("pointer")
                                if (role == r) {
                                    backgroundColor(SidebarActiveColor); color(Color.white); property("border", "none")
                                } else {
                                    backgroundColor(Color.white); color(Color("#475569")); property("border", "1px solid #cbd5e1")
                                }
                            }
                            onClick { role = r }
                        }) { Text(r) }
                    }
                }
            }
            UserFormField("Contraseña", password) { password = it }
            Div({ style { display(DisplayStyle.Flex); gap(12.px); marginTop(24.px) } }) {
                Button({
                    style {
                        flex(1); padding(10.px); borderRadius(8.px); property("border", "none")
                        backgroundColor(SidebarActiveColor); color(Color.white); cursor("pointer"); fontWeight("bold")
                    }
                    onClick { addUser(email, name, role, password) }
                }) { Text("Crear") }
                Button({
                    style {
                        flex(1); padding(10.px); borderRadius(8.px)
                        property("border", "1px solid #cbd5e1"); backgroundColor(Color.white); cursor("pointer")
                    }
                    onClick { showAddDialog = false }
                }) { Text("Cancelar") }
            }
        }
    }

    // --- Dialog: Editar usuario ---
    if (editUser != null) {
        val u = editUser!!
        var name by remember { mutableStateOf(u.name) }
        var role by remember { mutableStateOf(u.role) }
        var password by remember { mutableStateOf("") }

        ModalDialog("Editar Usuario") {
            P({ style { fontSize(12.px); color(Color("#64748b")); marginBottom(16.px) } }) { Text("Email: ${u.email}") }
            UserFormField("Nombre", name) { name = it }
            Div({ style { marginBottom(12.px) } }) {
                Label { Text("Rol") }
                Div({ style { display(DisplayStyle.Flex); gap(8.px); marginTop(8.px) } }) {
                    listOf("ADMIN", "RH", "EMPLEADO").forEach { r ->
                        Button({
                            style {
                                padding(6.px, 14.px); borderRadius(6.px); fontSize(12.px); cursor("pointer")
                                if (role == r) {
                                    backgroundColor(SidebarActiveColor); color(Color.white); property("border", "none")
                                } else {
                                    backgroundColor(Color.white); color(Color("#475569")); property("border", "1px solid #cbd5e1")
                                }
                            }
                            onClick { role = r }
                        }) { Text(r) }
                    }
                }
            }
            UserFormField("Nueva Contraseña (opcional)", password) { password = it }
            Div({ style { display(DisplayStyle.Flex); gap(12.px); marginTop(24.px) } }) {
                Button({
                    style {
                        flex(1); padding(10.px); borderRadius(8.px); property("border", "none")
                        backgroundColor(SidebarActiveColor); color(Color.white); cursor("pointer"); fontWeight("bold")
                    }
                    onClick { updateUser(u.email, name, role, if (password.isBlank()) null else password) }
                }) { Text("Guardar") }
                Button({
                    style {
                        flex(1); padding(10.px); borderRadius(8.px)
                        property("border", "1px solid #cbd5e1"); backgroundColor(Color.white); cursor("pointer")
                    }
                    onClick { editUser = null }
                }) { Text("Cancelar") }
            }
        }
    }
}

@Composable
fun UserFormField(label: String, value: String, onChange: (String) -> Unit) {
    Div({ style { marginBottom(12.px) } }) {
        Label { Text(label) }
        Input(InputType.Text) {
            style {
                width(100.percent); padding(8.px, 12.px); marginTop(4.px)
                borderRadius(6.px); property("border", "1px solid #cbd5e1")
                property("box-sizing", "border-box"); property("outline", "none")
            }
            this.value(value)
            onInput { onChange(it.value) }
        }
    }
}

@Composable
fun ModalDialog(title: String, content: @Composable () -> Unit) {
    Div({
        style {
            position(Position.Fixed); top(0.px); left(0.px); width(100.vw); height(100.vh)
            backgroundColor(Color("rgba(0,0,0,0.5)")); display(DisplayStyle.Flex)
            alignItems(AlignItems.Center); justifyContent(JustifyContent.Center); property("z-index", "999")
        }
    }) {
        Div({
            style {
                backgroundColor(Color.white); borderRadius(12.px); padding(32.px)
                width(420.px); maxWidth("90vw"); maxHeight("90vh"); overflowY("auto")
                property("box-shadow", "0 20px 25px -5px rgba(0,0,0,0.3)")
            }
        }) {
            H3({ style { margin(0.px, 0.px, 20.px, 0.px) } }) { Text(title) }
            content()
        }
    }
}
