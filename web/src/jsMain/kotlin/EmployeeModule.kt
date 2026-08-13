import com.example.rhnaf.shared.model.*
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

/**
 * Devuelve true si el rol puede editar/eliminar empleados (RH o ADMIN).
 */
fun canManageEmployees(role: UserRole): Boolean =
    role == UserRole.ADMIN || role == UserRole.RH

@Composable
fun EmployeeModule(
    employees: List<Employee>,
    client: HttpClient,
    scope: kotlinx.coroutines.CoroutineScope,
    t: Translations,
    userRole: UserRole,
    authToken: String,
    onEmployeesUpdated: (List<Employee>) -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedEmployee by remember { mutableStateOf<Employee?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showBajaDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    fun refresh() {
        scope.launch {
            isLoading = true
            errorMsg = ""
            try {
                val fresh: List<Employee> = client.get("$BACKEND_URL/api/employees").body()
                onEmployeesUpdated(fresh)
            } catch (e: Exception) {
                errorMsg = "No se pudo conectar: ${e.message}"
            }
            isLoading = false
        }
    }

    fun updateEmployee(emp: Employee) {
        scope.launch {
            try {
                val resp = client.post("$BACKEND_URL/api/employee/update") {
                    contentType(ContentType.Application.Json)
                    header(HttpHeaders.Authorization, "Bearer $authToken")
                    setBody(emp)
                }
                if (resp.status == HttpStatusCode.OK) {
                    refresh()
                } else {
                    val body: String = resp.bodyAsText()
                    errorMsg = "Error al actualizar: $body"
                }
            } catch (e: Exception) {
                errorMsg = "Error de conexión: ${e.message}"
            }
        }
    }

    fun deleteEmployee(id: String) {
        scope.launch {
            try {
                val resp = client.delete("$BACKEND_URL/api/employee/$id") {
                    header(HttpHeaders.Authorization, "Bearer $authToken")
                }
                if (resp.status == HttpStatusCode.OK) {
                    refresh()
                } else {
                    val body: String = resp.bodyAsText()
                    errorMsg = "Error al eliminar: $body"
                }
            } catch (e: Exception) {
                errorMsg = "Error de conexión: ${e.message}"
            }
        }
    }

    val withPhoto = employees.count { !it.photoUrl.isNullOrBlank() }
    val withoutPhoto = employees.size - withPhoto

    val filtered = employees.filter { e ->
        searchQuery.isBlank() ||
            e.id.contains(searchQuery, ignoreCase = true) ||
            "${e.firstName} ${e.lastName}".contains(searchQuery, ignoreCase = true) ||
            e.department.contains(searchQuery, ignoreCase = true)
    }

    val canManage = canManageEmployees(userRole)

    Div({ style { backgroundColor(Color.white); padding(32.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        H3({ style { margin(0.px, 0.px, 4.px, 0.px) } }) { Text("Plantilla de Empleados") }
        P({ style { color(Color.gray); margin(0.px, 0.px, 16.px, 0.px); fontSize(14.px) } }) {
            Text("Ficha de cada empleado con su foto, tomadas directamente de la lectora Hikvision (via employee_sync.py)")
        }

        Div({ style { display(DisplayStyle.Flex); gap(12.px); marginBottom(20.px); flexWrap(FlexWrap.Wrap) } }) {
            EmpStatCard("Total Empleados", employees.size.toString())
            EmpStatCard("Con Foto", withPhoto.toString())
            EmpStatCard("Sin Foto", withoutPhoto.toString())
        }

        Div({ style { display(DisplayStyle.Flex); gap(12.px); marginBottom(20.px); flexWrap(FlexWrap.Wrap); alignItems(AlignItems.Center) } }) {
            Input(InputType.Text) {
                style {
                    padding(8.px, 12.px); borderRadius(6.px); property("border", "1px solid #cbd5e1")
                    width(280.px); property("outline", "none")
                }
                placeholder("Buscar por nombre, ID o departamento...")
                value(searchQuery)
                onInput { searchQuery = it.value }
            }
            Button({
                style {
                    padding(8.px, 16.px); borderRadius(6.px); property("border", "none")
                    backgroundColor(SidebarActiveColor); color(Color.white); cursor("pointer")
                    fontWeight("bold")
                }
                onClick { refresh() }
            }) { Text(if (isLoading) "Actualizando..." else "Actualizar") }
        }

        if (withoutPhoto > 0) {
            Div({ style { padding(12.px, 16.px); backgroundColor(Color("#fffbeb")); borderRadius(8.px); marginBottom(20.px); property("border", "1px solid #fde68a") } }) {
                P({ style { margin(0.px); color(Color("#92400e")); fontSize(13.px) } }) {
                    Text("$withoutPhoto empleados aun no tienen foto. Corre employee_sync.py desde una PC en la red de la planta para jalar la foto y datos completos directo de la lectora.")
                }
            }
        }

        if (errorMsg.isNotEmpty()) {
            Div({ style { padding(16.px); backgroundColor(Color("#fef2f2")); borderRadius(8.px); color(Color("#dc2626")); marginBottom(20.px) } }) {
                P({ style { margin(0.px) } }) { Text(errorMsg) }
            }
        }

        if (employees.isEmpty()) {
            Div({ style { padding(40.px, 0.px); textAlign("center") } }) {
                P({ style { color(Color.gray); fontSize(16.px) } }) { Text("No hay empleados cargados aun") }
            }
        } else {
            Div({
                style {
                    display(DisplayStyle.Grid)
                    property("grid-template-columns", "repeat(auto-fill, minmax(220.px, 1fr))")
                    gap(16.px)
                }
            }) {
                filtered.sortedBy { it.id }.forEach { emp ->
                    EmployeeCard(emp, canManage,
                        onEdit = { selectedEmployee = emp; showEditDialog = true },
                        onBaja = { selectedEmployee = emp; showBajaDialog = true },
                        onDelete = { selectedEmployee = emp; showDeleteDialog = true }
                    )
                }
            }

            if (filtered.isEmpty()) {
                P({ style { textAlign("center"); color(Color("#94a3b8")); padding(24.px, 0.px) } }) {
                    Text("Ningun empleado coincide con la busqueda.")
                }
            }
        }
    }

    // --- DIALOG: Editar empleado ---
    if (showEditDialog && selectedEmployee != null) {
        val emp = selectedEmployee!!
        var firstName by remember { mutableStateOf(emp.firstName) }
        var lastName by remember { mutableStateOf(emp.lastName) }
        var position by remember { mutableStateOf(emp.position) }
        var department by remember { mutableStateOf(emp.department) }

        Div({
            style {
                position(Position.Fixed); top(0.px); left(0.px); width(100.vw); height(100.vh)
                backgroundColor(Color("rgba(0,0,0,0.5)")); display(DisplayStyle.Flex)
                alignItems(AlignItems.Center); justifyContent(JustifyContent.Center); zIndex(999)
            }
        }) {
            Div({
                style {
                    backgroundColor(Color.white); borderRadius(12.px); padding(32.px)
                    width(420.px); maxWidth("90vw"); maxHeight("90vh"); overflowY("auto")
                    property("box-shadow", "0 20px 25px -5px rgba(0,0,0,0.3)")
                }
            }) {
                H3({ style { margin(0.px, 0.px, 20.px, 0.px) } }) { Text("Editar Empleado") }
                P({ style { fontSize(12.px); color(Color("#64748b")); marginBottom(16.px) } }) { Text("ID: ${emp.id}") }
                
                EditField("Nombre(s)", firstName) { firstName = it }
                EditField("Apellidos", lastName) { lastName = it }
                EditField("Puesto", position) { position = it }
                EditField("Departamento", department) { department = it }

                Div({ style { display(DisplayStyle.Flex); gap(12.px); marginTop(24.px) } }) {
                    Button({
                        style {
                            flex(1); padding(10.px); borderRadius(8.px); property("border", "none")
                            backgroundColor(SidebarActiveColor); color(Color.white); cursor("pointer"); fontWeight("bold")
                        }
                        onClick {
                            val updated = emp.copy(
                                firstName = firstName, lastName = lastName,
                                position = position, department = department
                            )
                            updateEmployee(updated)
                            showEditDialog = false
                        }
                    }) { Text("Guardar") }
                    Button({
                        style {
                            flex(1); padding(10.px); borderRadius(8.px)
                            property("border", "1px solid #cbd5e1"); backgroundColor(Color.white); cursor("pointer")
                        }
                        onClick { showEditDialog = false }
                    }) { Text("Cancelar") }
                }
            }
        }
    }

    // --- DIALOG: Dar de baja ---
    if (showBajaDialog && selectedEmployee != null) {
        val emp = selectedEmployee!!
        Div({
            style {
                position(Position.Fixed); top(0.px); left(0.px); width(100.vw); height(100.vh)
                backgroundColor(Color("rgba(0,0,0,0.5)")); display(DisplayStyle.Flex)
                alignItems(AlignItems.Center); justifyContent(JustifyContent.Center); zIndex(999)
            }
        }) {
            Div({
                style {
                    backgroundColor(Color.white); borderRadius(12.px); padding(32.px)
                    width(400.px); maxWidth("90vw")
                    property("box-shadow", "0 20px 25px -5px rgba(0,0,0,0.3)")
                }
            }) {
                H3({ style { margin(0.px, 0.px, 16.px, 0.px); color(Color("#991b1b")) } }) { Text("Dar de Baja") }
                P({ style { fontSize(14.px); color(Color("#475569")); marginBottom(24.px) } }) {
                    Text("¿Confirmas el cambio de estatus de ${emp.firstName} ${emp.lastName} a INACTIVO?")
                }
                Div({ style { display(DisplayStyle.Flex); gap(12.px) } }) {
                    Button({
                        style {
                            flex(1); padding(10.px); borderRadius(8.px); property("border", "none")
                            backgroundColor(Color("#991b1b")); color(Color.white); cursor("pointer"); fontWeight("bold")
                        }
                        onClick {
                            val updated = emp.copy(status = EmployeeStatus.INACTIVE)
                            updateEmployee(updated)
                            showBajaDialog = false
                        }
                    }) { Text("Confirmar Baja") }
                    Button({
                        style {
                            flex(1); padding(10.px); borderRadius(8.px)
                            property("border", "1px solid #cbd5e1"); backgroundColor(Color.white); cursor("pointer")
                        }
                        onClick { showBajaDialog = false }
                    }) { Text("Cancelar") }
                }
            }
        }
    }

    // --- DIALOG: Eliminar ---
    if (showDeleteDialog && selectedEmployee != null) {
        val emp = selectedEmployee!!
        Div({
            style {
                position(Position.Fixed); top(0.px); left(0.px); width(100.vw); height(100.vh)
                backgroundColor(Color("rgba(0,0,0,0.5)")); display(DisplayStyle.Flex)
                alignItems(AlignItems.Center); justifyContent(JustifyContent.Center); zIndex(999)
            }
        }) {
            Div({
                style {
                    backgroundColor(Color.white); borderRadius(12.px); padding(32.px)
                    width(400.px); maxWidth("90vw")
                    property("box-shadow", "0 20px 25px -5px rgba(0,0,0,0.3)")
                }
            }) {
                H3({ style { margin(0.px, 0.px, 16.px, 0.px); color(Color("#991b1b")) } }) { Text("Eliminar Empleado") }
                P({ style { fontSize(14.px); color(Color("#475569")); marginBottom(24.px) } }) {
                    Text("¿Eliminar definitivamente a ${emp.firstName} ${emp.lastName}? Esta acción no se puede deshacer.")
                }
                Div({ style { display(DisplayStyle.Flex); gap(12.px) } }) {
                    Button({
                        style {
                            flex(1); padding(10.px); borderRadius(8.px); property("border", "none")
                            backgroundColor(Color("#991b1b")); color(Color.white); cursor("pointer"); fontWeight("bold")
                        }
                        onClick {
                            deleteEmployee(emp.id)
                            showDeleteDialog = false
                        }
                    }) { Text("Eliminar") }
                    Button({
                        style {
                            flex(1); padding(10.px); borderRadius(8.px)
                            property("border", "1px solid #cbd5e1"); backgroundColor(Color.white); cursor("pointer")
                        }
                        onClick { showDeleteDialog = false }
                    }) { Text("Cancelar") }
                }
            }
        }
    }
}

@Composable
fun EmployeeCard(emp: Employee, canManage: Boolean, onEdit: () -> Unit, onBaja: () -> Unit, onDelete: () -> Unit) {
    Div({
        style {
            padding(16.px); borderRadius(10.px)
            property("border", "1px solid #e2e8f0")
            display(DisplayStyle.Flex); flexDirection(FlexDirection.Column); alignItems(AlignItems.Center)
            gap(8.px)
            backgroundColor(Color("#f8fafc"))
        }
    }) {
        val photo = emp.photoUrl
        if (!photo.isNullOrBlank()) {
            Img(src = photo) {
                style {
                    width(72.px); height(72.px); borderRadius(50.percent)
                    property("object-fit", "cover")
                    property("border", "2px solid #e2e8f0")
                }
            }
        } else {
            val initials = "${emp.firstName.take(1)}${emp.lastName.take(1)}".uppercase().ifBlank { "?" }
            Div({
                style {
                    width(72.px); height(72.px); borderRadius(50.percent)
                    backgroundColor(SidebarActiveColor); color(Color.white)
                    display(DisplayStyle.Flex); alignItems(AlignItems.Center); justifyContent(JustifyContent.Center)
                    fontSize(22.px); fontWeight("bold")
                }
            }) { Text(initials) }
        }
        P({ style { margin(0.px); fontWeight("bold"); fontSize(14.px); textAlign("center") } }) {
            Text("${emp.firstName} ${emp.lastName}".trim().ifBlank { emp.id })
        }
        P({ style { margin(0.px); fontSize(12.px); color(Color("#64748b")) } }) { Text("ID: ${emp.id}") }
        Span({
            style {
                padding(2.px, 10.px); borderRadius(99.px); fontSize(11.px)
                backgroundColor(Color("#dbeafe")); color(Color("#1e40af"))
            }
        }) { Text(emp.department.ifBlank { "Sin depto" }) }
        P({ style { margin(0.px); fontSize(11.px); color(Color("#94a3b8")) } }) { Text(emp.position) }

        // Chip de estatus
        val (statusColor, statusBg) = when (emp.status) {
            EmployeeStatus.ACTIVE -> Color("#166534") to Color("#dcfce7")
            EmployeeStatus.VACATION -> Color("#854d0e") to Color("#fef9c3")
            EmployeeStatus.INACTIVE -> Color("#991b1b") to Color("#fee2e2")
            else -> Color("#475569") to Color("#f1f5f9")
        }
        Span({
            style {
                padding(2.px, 10.px); borderRadius(99.px); fontSize(10.px); fontWeight("bold")
                backgroundColor(statusBg); color(statusColor)
            }
        }) { Text(emp.status.name) }

        // Botones de gestión (solo RH y ADMIN)
        if (canManage) {
            Div({ style { display(DisplayStyle.Flex); gap(6.px); marginTop(8.px) } }) {
                Button({
                    style {
                        padding(4.px, 10.px); borderRadius(6.px); fontSize(11.px); cursor("pointer")
                        property("border", "1px solid #2563eb"); backgroundColor(Color.white); color(Color("#2563eb"))
                    }
                    onClick { onEdit() }
                }) { Text("Editar") }
                if (emp.status == EmployeeStatus.ACTIVE) {
                    Button({
                        style {
                            padding(4.px, 10.px); borderRadius(6.px); fontSize(11.px); cursor("pointer")
                            property("border", "1px solid #f59e0b"); backgroundColor(Color.white); color(Color("#f59e0b"))
                        }
                        onClick { onBaja() }
                    }) { Text("Baja") }
                }
                Button({
                    style {
                        padding(4.px, 10.px); borderRadius(6.px); fontSize(11.px); cursor("pointer")
                        property("border", "1px solid #ef4444"); backgroundColor(Color.white); color(Color("#ef4444"))
                    }
                    onClick { onDelete() }
                }) { Text("Eliminar") }
            }
        }
    }
}

@Composable
fun EditField(label: String, value: String, onChange: (String) -> Unit) {
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
fun EmpStatCard(label: String, value: String) {
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
