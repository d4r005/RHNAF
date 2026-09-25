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
import kotlinx.browser.document
import org.w3c.dom.HTMLInputElement
import kotlin.coroutines.suspendCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Devuelve true si el rol puede editar/eliminar empleados (RH o ADMIN).
 */
fun canManageEmployees(role: UserRole): Boolean =
    role == UserRole.ADMIN || role == UserRole.RH

// Convierte una imagen elegida por el usuario a un data URI (base64) para
// guardarla directo en Employee.photoUrl, igual que ya lo hace employee_sync.py
// cuando trae la foto de la lectora. Asi no se necesita un endpoint nuevo.
private suspend fun readImageAsDataUrl(file: org.w3c.files.File): String = suspendCoroutine { continuation ->
    val reader = js("new FileReader()")
    reader.onload = { _: dynamic ->
        val result = reader.result
        if (result != null) continuation.resume(result.toString())
        else continuation.resumeWithException(IllegalStateException("No se pudo leer la imagen"))
    }
    reader.onerror = { _: dynamic ->
        continuation.resumeWithException(IllegalStateException("No se pudo leer la imagen"))
    }
    reader.readAsDataURL(file)
}

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
    var currentPage by remember { mutableStateOf(1) }
    val pageSize = 50
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
    }.sortedBy { it.id }

    val canManage = canManageEmployees(userRole)
    val totalPages = (filtered.size / pageSize) + (if (filtered.size % pageSize > 0) 1 else 0)
    val pagedEmployees = filtered.drop((currentPage - 1) * pageSize).take(pageSize)

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
                onInput { searchQuery = it.value; currentPage = 1 }
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
                    Text("$withoutPhoto empleados aun no tienen foto. Corre employee_sync.py desde una PC en la red de la planta para jalar la foto y datos completos directo de la lectora, o pulsa \"Editar\" en cada uno para subirla a mano.")
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
            // Tabla compacta: una fila por empleado en vez de tarjetas grandes,
            // asi se ven muchos mas empleados por pantalla sin desperdiciar espacio.
            Div({ style { overflowX("auto"); property("border", "1px solid #e2e8f0"); borderRadius(6.px) } }) {
                Table({ style { width(100.percent); property("border-collapse", "collapse"); fontSize(12.px) } }) {
                    Thead {
                        Tr({ style { backgroundColor(Color("#f1f5f9")); property("border-bottom", "2px solid #cbd5e1") } }) {
                            listOf("Foto", "ID", "Nombre", "Departamento", "Puesto", "Estatus", "Acciones").forEach { h ->
                                Th({ style { padding(8.px, 10.px); textAlign("left"); fontWeight("bold"); color(Color("#475569")); fontSize(11.px); property("white-space", "nowrap") } }) { Text(h) }
                            }
                        }
                    }
                    Tbody {
                        pagedEmployees.forEach { emp ->
                            EmployeeRow(emp, canManage,
                                onEdit = { selectedEmployee = emp; showEditDialog = true },
                                onBaja = { selectedEmployee = emp; showBajaDialog = true },
                                onDelete = { selectedEmployee = emp; showDeleteDialog = true }
                            )
                        }
                    }
                }
            }

            if (filtered.isEmpty()) {
                P({ style { textAlign("center"); color(Color("#94a3b8")); padding(24.px, 0.px) } }) {
                    Text("Ningun empleado coincide con la busqueda.")
                }
            } else if (totalPages > 1) {
                Div({ style { display(DisplayStyle.Flex); justifyContent(JustifyContent.Center); gap(12.px); marginTop(12.px); alignItems(AlignItems.Center); fontSize(12.px) } }) {
                    Button({
                        style {
                            padding(6.px, 12.px); borderRadius(6.px); property("border", "none"); cursor("pointer"); color(Color.white)
                            val bg = if (currentPage > 1) Color("#3d566e") else Color("#cbd5e1")
                            backgroundColor(bg)
                        }
                        onClick { if (currentPage > 1) currentPage-- }
                    }) { Text("Anterior") }
                    Text("Pagina $currentPage de $totalPages")
                    Button({
                        style {
                            padding(6.px, 12.px); borderRadius(6.px); property("border", "none"); cursor("pointer"); color(Color.white)
                            val bg = if (currentPage < totalPages) Color("#3d566e") else Color("#cbd5e1")
                            backgroundColor(bg)
                        }
                        onClick { if (currentPage < totalPages) currentPage++ }
                    }) { Text("Siguiente") }
                }
            }
        }
    }

    // --- DIALOG: Editar empleado (incluye cambiar foto) ---
    if (showEditDialog && selectedEmployee != null) {
        val emp = selectedEmployee!!
        var firstName by remember { mutableStateOf(emp.firstName) }
        var lastName by remember { mutableStateOf(emp.lastName) }
        var position by remember { mutableStateOf(emp.position) }
        var department by remember { mutableStateOf(emp.department) }
        var photoDataUrl by remember { mutableStateOf(emp.photoUrl) }
        var photoError by remember { mutableStateOf("") }
        var photoBusy by remember { mutableStateOf(false) }
        var rfc by remember { mutableStateOf(emp.rfc ?: "") }
        var curp by remember { mutableStateOf(emp.curp ?: "") }
        var nss by remember { mutableStateOf(emp.nss ?: "") }
        var entryDate by remember { mutableStateOf(emp.entryDate) }
        var exitDate by remember { mutableStateOf(emp.exitDate ?: "") }
        var salary by remember { mutableStateOf(emp.salary?.let { it.toString() } ?: "") }
        var sbc by remember { mutableStateOf(emp.sbc?.let { it.toString() } ?: "") }
        var phone by remember { mutableStateOf(emp.phone ?: "") }
        var email by remember { mutableStateOf(emp.email ?: "") }
        var supervisor by remember { mutableStateOf(emp.supervisor ?: "") }
        var contractType by remember { mutableStateOf(emp.contractType ?: "") }
        var maritalStatus by remember { mutableStateOf(emp.maritalStatus ?: "") }
        var emergencyContact by remember { mutableStateOf(emp.emergencyContact ?: "") }
        var status by remember { mutableStateOf(emp.status.name) }

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
                    width(720.px); maxWidth("95vw"); maxHeight("92vh"); overflowY("auto")
                    property("box-shadow", "0 20px 25px -5px rgba(0,0,0,0.3)")
                }
            }) {
                H3({ style { margin(0.px, 0.px, 20.px, 0.px) } }) { Text("Ficha de Empleado - ${emp.id}") }
                P({ style { fontSize(12.px); color(Color("#64748b")); marginBottom(16.px) } }) {
                    Text(if (status == "ACTIVE") "Empleado activo" else "Estado: $status")
                }

                // --- Foto ---
                Div({ style { display(DisplayStyle.Flex); alignItems(AlignItems.Center); gap(12.px); marginBottom(16.px) } }) {
                    val photo = photoDataUrl
                    if (!photo.isNullOrBlank()) {
                        Img(src = photo) {
                            style {
                                width(56.px); height(56.px); borderRadius(50.percent)
                                property("object-fit", "cover"); property("border", "2px solid #e2e8f0")
                            }
                        }
                    } else {
                        Div({
                            style {
                                width(56.px); height(56.px); borderRadius(50.percent)
                                backgroundColor(SidebarActiveColor); color(Color.white)
                                display(DisplayStyle.Flex); alignItems(AlignItems.Center); justifyContent(JustifyContent.Center)
                                fontSize(18.px); fontWeight("bold")
                            }
                        }) { Text("${firstName.take(1)}${lastName.take(1)}".uppercase().ifBlank { "?" }) }
                    }
                    Div {
                        P({ style { fontSize(12.px); color(Color("#475569")); margin(0.px) } }) { Text("Foto") }
                        Input(InputType.File) {
                            id("employee-photo-input")
                            attr("accept", "image/*")
                            style { display(DisplayStyle.Block); fontSize(11.px); marginTop(4.px) }
                            onInput {
                                val file = (document.getElementById("employee-photo-input") as? HTMLInputElement)?.files?.item(0)
                                if (file != null) {
                                    if (file.size.toLong() > 3L * 1024 * 1024) {
                                        photoError = "Maximo 3 MiB por foto."
                                    } else {
                                        photoBusy = true; photoError = ""
                                        scope.launch {
                                            try {
                                                photoDataUrl = readImageAsDataUrl(file)
                                            } catch (e: Exception) {
                                                photoError = e.message ?: "No se pudo leer la imagen"
                                            } finally {
                                                photoBusy = false
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (photoBusy) P({ style { fontSize(11.px); color(Color("#64748b")); margin(4.px, 0.px, 0.px, 0.px) } }) { Text("Leyendo imagen...") }
                        if (photoError.isNotBlank()) P({ style { fontSize(11.px); color(Color("#dc2626")); margin(4.px, 0.px, 0.px, 0.px) } }) { Text(photoError) }
                        if (!photoDataUrl.isNullOrBlank()) {
                            Button({
                                style {
                                    marginTop(6.px); padding(3.px, 8.px); fontSize(11.px); borderRadius(4.px)
                                    property("border", "1px solid #fca5a5"); backgroundColor(Color("#fef2f2")); color(Color("#dc2626")); cursor("pointer")
                                }
                                onClick {
                                    photoDataUrl = null
                                    photoError = ""
                                    // Limpiar tambien el <input type=file> para poder re-seleccionar la misma imagen despues
                                    (document.getElementById("employee-photo-input") as? HTMLInputElement)?.value = ""
                                }
                            }) { Text("Eliminar foto") }
                        }
                    }
                }

                EditField("Nombre(s)", firstName) { firstName = it }
                EditField("Apellidos", lastName) { lastName = it }
                EditField("Puesto", position) { position = it }
                EditField("Departamento", department) { department = it }
                Div({ style { display(DisplayStyle.Flex); gap(12.px) } }) {
                    Div({ style { flex(1) } }) {
                        EditField("Fecha de ingreso (dd/MM/aaaa)", entryDate) { entryDate = it }
                        EditField("Fecha de baja (dd/MM/aaaa)", exitDate) { exitDate = it }
                        EditField("Tipo de contrato", contractType) { contractType = it }
                        EditField("Estado civil", maritalStatus) { maritalStatus = it }
                    }
                    Div({ style { flex(1) } }) {
                        EditField("Sueldo diario ($)", salary) { salary = it }
                        EditField("SBC / IMSS ($)", sbc) { sbc = it }
                        EditField("Jefe directo", supervisor) { supervisor = it }
                        EditField("Estado (ACTIVE / INACTIVE)", status) { status = it }
                    }
                }
                P({ style { fontSize(12.px); fontWeight("bold"); color(Color("#334155")); margin(16.px, 0.px, 4.px, 0.px) } }) { Text("Datos oficiales (RFC, CURP, NSS)") }
                Div({ style { display(DisplayStyle.Flex); gap(12.px) } }) {
                    Div({ style { flex(1) } }) {
                        EditField("RFC", rfc) { rfc = it }
                        EditField("CURP", curp) { curp = it }
                        EditField("NSS (No. Seguro Social)", nss) { nss = it }
                    }
                    Div({ style { flex(1) } }) {
                        EditField("Telefono", phone) { phone = it }
                        EditField("Correo electronico", email) { email = it }
                        EditField("Contacto de emergencia", emergencyContact) { emergencyContact = it }
                    }
                }

                Div({ style { display(DisplayStyle.Flex); gap(12.px); marginTop(24.px) } }) {
                    Button({
                        style {
                            flex(1); padding(10.px); borderRadius(8.px); property("border", "none")
                            backgroundColor(SidebarActiveColor); color(Color.white); cursor("pointer"); fontWeight("bold")
                        }
                        onClick {
                            val updated = emp.copy(
                                firstName = firstName, lastName = lastName,
                                position = position, department = department,
                                photoUrl = photoDataUrl,
                                rfc = rfc.ifBlank { null }, curp = curp.ifBlank { null },
                                nss = nss.ifBlank { null },
                                entryDate = entryDate,
                                exitDate = exitDate.ifBlank { null },
                                salary = salary.replace(",", "").toDoubleOrNull(),
                                sbc = sbc.replace(",", "").toDoubleOrNull(),
                                phone = phone.ifBlank { null },
                                email = email.ifBlank { null },
                                supervisor = supervisor.ifBlank { null },
                                contractType = contractType.ifBlank { null },
                                maritalStatus = maritalStatus.ifBlank { null },
                                emergencyContact = emergencyContact.ifBlank { null },
                                status = try { EmployeeStatus.valueOf(status) } catch (e: Exception) { emp.status }
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
                alignItems(AlignItems.Center); justifyContent(JustifyContent.Center); property("z-index", "999")
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
                alignItems(AlignItems.Center); justifyContent(JustifyContent.Center); property("z-index", "999")
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
fun EmployeeRow(emp: Employee, canManage: Boolean, onEdit: () -> Unit, onBaja: () -> Unit, onDelete: () -> Unit) {
    Tr({
        style {
            property("border-bottom", "1px solid #f1f5f9")
            property("cursor", "pointer")
            property("background-color", "#ffffff")
        }
        onClick { onEdit() }
    }) {
        Td({ style { padding(6.px, 10.px) } }) {
            val photo = emp.photoUrl
            if (!photo.isNullOrBlank()) {
                Img(src = photo) {
                    style {
                        width(32.px); height(32.px); borderRadius(50.percent)
                        property("object-fit", "cover"); property("border", "1px solid #e2e8f0")
                    }
                }
            } else {
                val initials = "${emp.firstName.take(1)}${emp.lastName.take(1)}".uppercase().ifBlank { "?" }
                Div({
                    style {
                        width(32.px); height(32.px); borderRadius(50.percent)
                        backgroundColor(SidebarActiveColor); color(Color.white)
                        display(DisplayStyle.Flex); alignItems(AlignItems.Center); justifyContent(JustifyContent.Center)
                        fontSize(11.px); fontWeight("bold")
                    }
                }) { Text(initials) }
            }
        }
        Td({ style { padding(6.px, 10.px); color(Color("#64748b")); fontSize(12.px); property("white-space", "nowrap") } }) { Text(emp.id) }
        Td({ style { padding(6.px, 10.px); fontWeight("bold"); fontSize(12.px); property("white-space", "nowrap") } }) {
            Text("${emp.firstName} ${emp.lastName}".trim().ifBlank { emp.id })
        }
        Td({ style { padding(6.px, 10.px); fontSize(12.px) } }) {
            Span({
                style {
                    padding(2.px, 8.px); borderRadius(99.px); fontSize(11.px)
                    backgroundColor(Color("#dbeafe")); color(Color("#1e40af"))
                }
            }) { Text(emp.department.ifBlank { "Sin depto" }) }
        }
        Td({ style { padding(6.px, 10.px); fontSize(12.px); color(Color("#64748b")) } }) { Text(emp.position) }
        Td({ style { padding(6.px, 10.px) } }) {
            val (statusColor, statusBg) = when (emp.status) {
                EmployeeStatus.ACTIVE -> Color("#166534") to Color("#dcfce7")
                EmployeeStatus.VACATION -> Color("#854d0e") to Color("#fef9c3")
                EmployeeStatus.INACTIVE -> Color("#991b1b") to Color("#fee2e2")
                else -> Color("#475569") to Color("#f1f5f9")
            }
            Span({
                style {
                    padding(2.px, 8.px); borderRadius(99.px); fontSize(10.px); fontWeight("bold")
                    backgroundColor(statusBg); color(statusColor)
                }
            }) { Text(emp.status.name) }
        }
        Td({ style { padding(6.px, 10.px) } }) {
            if (canManage) {
                Div({ style { display(DisplayStyle.Flex); gap(6.px); flexWrap(FlexWrap.Wrap) } }) {
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
            } else {
                Text("-")
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
            padding(16.px, 20.px); borderRadius(8.px); backgroundColor(Color("#f8fafc"))
            property("border", "1px solid #e2e8f0"); minWidth(140.px)
        }
    }) {
        P({ style { margin(0.px); fontSize(11.px); color(Color("#64748b")); property("text-transform", "uppercase"); letterSpacing(0.5.px) } }) { Text(label) }
        P({ style { margin(4.px, 0.px, 0.px, 0.px); fontSize(24.px); fontWeight("bold"); color(Color("#1e293b")) } }) { Text(value) }
    }
}
