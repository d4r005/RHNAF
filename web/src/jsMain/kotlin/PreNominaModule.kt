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

fun fmt1(d: Double): String {
    val s = d.toString()
    val dot = s.indexOf('.')
    return if (dot >= 0 && dot + 2 < s.length) s.substring(0, dot + 2) else s
}
fun fmt2(d: Double): String {
    val s = d.toString()
    val dot = s.indexOf('.')
    return if (dot >= 0 && dot + 3 < s.length) s.substring(0, dot + 3) else s
}

enum class PreNominaTab { TURNOS, POLITICAS, ASIGNACIONES, JUSTIFICACIONES, CALCULO }

@Composable
fun PreNominaModule(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope, t: Translations, userRole: UserRole) {
    val canEdit = userRole == UserRole.ADMIN || userRole == UserRole.RH
    var tab by remember { mutableStateOf(PreNominaTab.TURNOS) }

    Div({ style { backgroundColor(Color.white); padding(32.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        H3({ style { margin(0.px, 0.px, 4.px, 0.px) } }) { Text("Pre-Nómina (estilo PreAsyst)") }
        P({ style { color(Color.gray); margin(0.px, 0.px, 16.px, 0.px); fontSize(14.px) } }) {
            Text("Gestión de turnos, políticas, incidencias y cálculo de pre-nómina")
        }

        Div({ style { display(DisplayStyle.Flex); gap(8.px); marginBottom(16.px); property("border-bottom", "1px solid #e2e8f0"); paddingBottom(8.px); flexWrap(FlexWrap.Wrap) } }) {
            listOf(
                PreNominaTab.TURNOS to "Turnos y Horarios",
                PreNominaTab.POLITICAS to "Políticas",
                PreNominaTab.ASIGNACIONES to "Asignación de Turnos",
                PreNominaTab.JUSTIFICACIONES to "Justificaciones",
                PreNominaTab.CALCULO to "Calcular Pre-Nómina"
            ).forEach { (tb, label) ->
                Button({
                    style {
                        padding(8.px, 16.px); borderRadius(6.px); property("border", "none"); cursor("pointer")
                        backgroundColor(if (tab == tb) SidebarActiveColor else Color("#f1f5f9"))
                        color(if (tab == tb) Color.white else Color("#334155"))
                        fontWeight("bold")
                    }
                    onClick { tab = tb }
                }) { Text(label) }
            }
        }

        when (tab) {
            PreNominaTab.TURNOS -> TurnosTab(client, scope, canEdit)
            PreNominaTab.POLITICAS -> PoliticasTab(client, scope, canEdit)
            PreNominaTab.ASIGNACIONES -> AsignacionesTab(client, scope, canEdit)
            PreNominaTab.JUSTIFICACIONES -> JustificacionesTab(client, scope, canEdit, userRole)
            PreNominaTab.CALCULO -> CalculoTab(client, scope, canEdit)
        }
    }
}

@Composable
fun TurnosTab(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope, canEdit: Boolean) {
    var items by remember { mutableStateOf(emptyList<Shift>()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey) {
        isLoading = true
        try { items = client.get("$BACKEND_URL/api/v1/pre-nomina/turnos").body() } catch (e: Exception) { println("Error: ${e.message}") }
        isLoading = false
    }

    var f_nombre by remember { mutableStateOf("") }
    var f_entrada by remember { mutableStateOf("") }
    var f_salida by remember { mutableStateOf("") }
    var f_tolerancia by remember { mutableStateOf("0") }
    var f_comida by remember { mutableStateOf("60") }
    var f_tipo by remember { mutableStateOf("Fijo") }

    if (canEdit) {
        Div({ style { display(DisplayStyle.Flex); gap(8.px); marginBottom(16.px); flexWrap(FlexWrap.Wrap); alignItems(AlignItems.Center) } }) {
            Input(InputType.Text) { placeholder("Nombre *"); value(f_nombre); onInput { f_nombre = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(180.px) } }
            Input(InputType.Text) { placeholder("Entrada HH:MM"); value(f_entrada); onInput { f_entrada = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(120.px) } }
            Input(InputType.Text) { placeholder("Salida HH:MM"); value(f_salida); onInput { f_salida = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(120.px) } }
            Input(InputType.Text) { placeholder("Tolerancia min"); value(f_tolerancia); onInput { f_tolerancia = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(100.px) } }
            Input(InputType.Text) { placeholder("Comida min"); value(f_comida); onInput { f_comida = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(100.px) } }
            Select({ style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1") }; onChange { f_tipo = (it.target as org.w3c.dom.HTMLSelectElement).value } }) {
                Option("Fijo") { Text("Fijo") }; Option("Quebrado") { Text("Quebrado") }; Option("Rotativo") { Text("Rotativo") }; Option("24h") { Text("24h") }
            }
            Button({
                style { padding(8.px, 16.px); backgroundColor(SidebarActiveColor); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") }
                onClick {
                    if (f_nombre.isNotBlank() && f_entrada.isNotBlank() && f_salida.isNotBlank()) {
                        scope.launch {
                            client.post("$BACKEND_URL/api/v1/pre-nomina/turnos") {
                                contentType(ContentType.Application.Json)
                                setBody(Shift(nombre = f_nombre, horaEntrada = f_entrada, horaSalida = f_salida, minutoTolerancia = f_tolerancia.toIntOrNull() ?: 0, minutosComida = f_comida.toIntOrNull() ?: 60, tipoTurno = f_tipo))
                            }
                            f_nombre = ""; f_entrada = ""; f_salida = ""; f_tolerancia = "0"; f_comida = "60"; f_tipo = "Fijo"
                            refreshKey++
                        }
                    }
                }
            }) { Text("+ Agregar Turno") }
        }
    }

    if (isLoading) { P { Text("Cargando...") } } else {
        Table({ style { width(100.percent) } }) {
            Thead { Tr { Th { Text("Nombre") }; Th { Text("Entrada") }; Th { Text("Salida") }; Th { Text("Tolerancia") }; Th { Text("Comida") }; Th { Text("Tipo") }; if (canEdit) Th { Text("") } } }
            Tbody {
                items.forEach { s ->
                    Tr {
                        Td { Text(s.nombre) }; Td { Text(s.horaEntrada) }; Td { Text(s.horaSalida) }
                        Td { Text("${s.minutoTolerancia} min") }; Td { Text("${s.minutosComida} min") }; Td { Text(s.tipoTurno) }
                        if (canEdit) Td {
                            Button({ style { backgroundColor(Color("#ef4444")); color(Color.white); property("border", "none"); borderRadius(4.px); padding(4.px, 10.px); cursor("pointer") }
                                onClick { scope.launch { client.delete("$BACKEND_URL/api/v1/pre-nomina/turnos/${s.id}"); refreshKey++ } }
                            }) { Text("Eliminar") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PoliticasTab(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope, canEdit: Boolean) {
    var items by remember { mutableStateOf(emptyList<AttendancePolicy>()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey) {
        isLoading = true
        try { items = client.get("$BACKEND_URL/api/v1/pre-nomina/politicas").body() } catch (e: Exception) { println("Error: ${e.message}") }
        isLoading = false
    }

    var f_nombre by remember { mutableStateOf("") }
    var f_tolRet by remember { mutableStateOf("5") }
    var f_retMayor by remember { mutableStateOf("15") }
    var f_salAnt by remember { mutableStateOf("5") }
    var f_extraInicio by remember { mutableStateOf("") }
    var f_primaDom by remember { mutableStateOf("0.25") }
    var f_descanso by remember { mutableStateOf("6") }

    if (canEdit) {
        Div({ style { display(DisplayStyle.Flex); gap(8.px); marginBottom(16.px); flexWrap(FlexWrap.Wrap); alignItems(AlignItems.Center) } }) {
            Input(InputType.Text) { placeholder("Nombre politica"); value(f_nombre); onInput { f_nombre = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(160.px) } }
            Input(InputType.Text) { placeholder("Tol. retardo min"); value(f_tolRet); onInput { f_tolRet = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(100.px) } }
            Input(InputType.Text) { placeholder("Retardo mayor min"); value(f_retMayor); onInput { f_retMayor = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(100.px) } }
            Input(InputType.Text) { placeholder("Salida ant. min"); value(f_salAnt); onInput { f_salAnt = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(100.px) } }
            Input(InputType.Text) { placeholder("Inicio extra HH:MM"); value(f_extraInicio); onInput { f_extraInicio = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(140.px) } }
            Input(InputType.Text) { placeholder("Prima dominical"); value(f_primaDom); onInput { f_primaDom = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(100.px) } }
            Input(InputType.Text) { placeholder("Dias descanso 0=D,6=S"); value(f_descanso); onInput { f_descanso = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(140.px) } }
            Button({
                style { padding(8.px, 16.px); backgroundColor(SidebarActiveColor); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") }
                onClick {
                    if (f_nombre.isNotBlank()) {
                        scope.launch {
                            client.post("$BACKEND_URL/api/v1/pre-nomina/politicas") {
                                contentType(ContentType.Application.Json)
                                setBody(AttendancePolicy(nombre = f_nombre, toleranciaRetardoMin = f_tolRet.toIntOrNull() ?: 5, retardoMayorMin = f_retMayor.toIntOrNull() ?: 15, salidaAnticipadaMin = f_salAnt.toIntOrNull() ?: 5, horasExtraInicio = f_extraInicio, primaDominical = f_primaDom.toDoubleOrNull() ?: 0.25, diasDescanso = f_descanso))
                            }
                            f_nombre = ""; refreshKey++
                        }
                    }
                }
            }) { Text("+ Agregar Politica") }
        }
    }

    if (isLoading) { P { Text("Cargando...") } } else {
        Table({ style { width(100.percent) } }) {
            Thead { Tr { Th { Text("Nombre") }; Th { Text("Tol. Retardo") }; Th { Text("Ret. Mayor") }; Th { Text("Salida Ant.") }; Th { Text("Inicio Extra") }; Th { Text("Prima Dom.") }; Th { Text("Dias Descanso") }; if (canEdit) Th { Text("") } } }
            Tbody {
                items.forEach { p ->
                    Tr {
                        Td { Text(p.nombre) }; Td { Text("${p.toleranciaRetardoMin} min") }; Td { Text("${p.retardoMayorMin} min") }
                        Td { Text("${p.salidaAnticipadaMin} min") }; Td { Text(p.horasExtraInicio.ifBlank { "—" }) }
                        Td { Text("${(p.primaDominical * 100).toInt()}%") }; Td { Text(p.diasDescanso) }
                        if (canEdit) Td {
                            Button({ style { backgroundColor(Color("#ef4444")); color(Color.white); property("border", "none"); borderRadius(4.px); padding(4.px, 10.px); cursor("pointer") }
                                onClick { scope.launch { client.delete("$BACKEND_URL/api/v1/pre-nomina/politicas/${p.id}"); refreshKey++ } }
                            }) { Text("Eliminar") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AsignacionesTab(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope, canEdit: Boolean) {
    var items by remember { mutableStateOf(emptyList<EmployeeShiftAssignment>()) }
    var shifts by remember { mutableStateOf(emptyList<Shift>()) }
    var employees by remember { mutableStateOf(emptyList<Employee>()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey) {
        isLoading = true
        try {
            items = client.get("$BACKEND_URL/api/v1/pre-nomina/asignaciones").body()
            shifts = client.get("$BACKEND_URL/api/v1/pre-nomina/turnos").body()
            employees = client.get("$BACKEND_URL/api/v1/employees").body()
        } catch (e: Exception) { println("Error: ${e.message}") }
        isLoading = false
    }

    var f_empId by remember { mutableStateOf("") }
    var f_shiftId by remember { mutableStateOf("") }

    if (canEdit) {
        Div({ style { display(DisplayStyle.Flex); gap(8.px); marginBottom(16.px); alignItems(AlignItems.Center) } }) {
            Select({ style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(250.px) }; onChange { f_empId = (it.target as org.w3c.dom.HTMLSelectElement).value } }) {
                Option("") { Text("Seleccionar empleado...") }
                employees.forEach { e -> Option(e.id) { Text("${e.firstName} ${e.lastName} (${e.id})") } }
            }
            Select({ style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(200.px) }; onChange { f_shiftId = (it.target as org.w3c.dom.HTMLSelectElement).value } }) {
                Option("") { Text("Seleccionar turno...") }
                shifts.forEach { s -> Option(s.id.toString()) { Text("${s.nombre} (${s.horaEntrada}-${s.horaSalida})") } }
            }
            Button({
                style { padding(8.px, 16.px); backgroundColor(SidebarActiveColor); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") }
                onClick {
                    if (f_empId.isNotBlank() && f_shiftId.isNotBlank()) {
                        val emp = employees.find { it.id == f_empId }
                        val shift = shifts.find { it.id.toString() == f_shiftId }
                        scope.launch {
                            client.post("$BACKEND_URL/api/v1/pre-nomina/asignaciones") {
                                contentType(ContentType.Application.Json)
                                setBody(EmployeeShiftAssignment(employeeId = f_empId, employeeName = "${emp?.firstName} ${emp?.lastName}", shiftId = f_shiftId.toInt(), shiftName = shift?.nombre ?: "", fechaInicio = ""))
                            }
                            f_empId = ""; f_shiftId = ""; refreshKey++
                        }
                    }
                }
            }) { Text("+ Asignar") }
        }
    }

    if (isLoading) { P { Text("Cargando...") } } else {
        Table({ style { width(100.percent) } }) {
            Thead { Tr { Th { Text("Empleado") }; Th { Text("ID") }; Th { Text("Turno") }; Th { Text("Desde") }; if (canEdit) Th { Text("") } } }
            Tbody {
                items.forEach { a ->
                    Tr {
                        Td { Text(a.employeeName) }; Td { Text(a.employeeId) }; Td { Text(a.shiftName) }; Td { Text(a.fechaInicio) }
                        if (canEdit) Td {
                            Button({ style { backgroundColor(Color("#ef4444")); color(Color.white); property("border", "none"); borderRadius(4.px); padding(4.px, 10.px); cursor("pointer") }
                                onClick { scope.launch { client.delete("$BACKEND_URL/api/v1/pre-nomina/asignaciones/${a.id}"); refreshKey++ } }
                            }) { Text("Eliminar") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun JustificacionesTab(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope, canEdit: Boolean, userRole: UserRole) {
    var items by remember { mutableStateOf(emptyList<Justification>()) }
    var employees by remember { mutableStateOf(emptyList<Employee>()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey) {
        isLoading = true
        try {
            items = client.get("$BACKEND_URL/api/v1/pre-nomina/justificaciones").body()
            employees = client.get("$BACKEND_URL/api/v1/employees").body()
        } catch (e: Exception) { println("Error: ${e.message}") }
        isLoading = false
    }

    var f_empId by remember { mutableStateOf("") }
    var f_fecha by remember { mutableStateOf("") }
    var f_tipo by remember { mutableStateOf("Falta") }
    var f_motivo by remember { mutableStateOf("") }

    if (canEdit) {
        Div({ style { display(DisplayStyle.Flex); gap(8.px); marginBottom(16.px); flexWrap(FlexWrap.Wrap); alignItems(AlignItems.Center) } }) {
            Select({ style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(200.px) }; onChange { f_empId = (it.target as org.w3c.dom.HTMLSelectElement).value } }) {
                Option("") { Text("Empleado...") }
                employees.forEach { e -> Option(e.id) { Text("${e.firstName} ${e.lastName}") } }
            }
            Input(InputType.Text) { placeholder("Fecha YYYY-MM-DD"); value(f_fecha); onInput { f_fecha = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(150.px) } }
            Select({ style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1") }; onChange { f_tipo = (it.target as org.w3c.dom.HTMLSelectElement).value } }) {
                Option("Falta") { Text("Falta") }; Option("Retardo") { Text("Retardo") }; Option("Salida anticipada") { Text("Salida anticipada") }
                Option("Permiso") { Text("Permiso") }; Option("Incapacidad") { Text("Incapacidad") }; Option("Vacaciones") { Text("Vacaciones") }
            }
            Input(InputType.Text) { placeholder("Motivo"); value(f_motivo); onInput { f_motivo = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(200.px) } }
            Button({
                style { padding(8.px, 16.px); backgroundColor(SidebarActiveColor); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") }
                onClick {
                    if (f_empId.isNotBlank() && f_fecha.isNotBlank()) {
                        val emp = employees.find { it.id == f_empId }
                        scope.launch {
                            client.post("$BACKEND_URL/api/v1/pre-nomina/justificaciones") {
                                contentType(ContentType.Application.Json)
                                setBody(Justification(employeeId = f_empId, employeeName = "${emp?.firstName} ${emp?.lastName}", fecha = f_fecha, tipo = f_tipo, motivo = f_motivo, fechaSolicitud = ""))
                            }
                            f_empId = ""; f_fecha = ""; f_tipo = "Falta"; f_motivo = ""; refreshKey++
                        }
                    }
                }
            }) { Text("+ Nueva Justificacion") }
        }
    }

    if (isLoading) { P { Text("Cargando...") } } else {
        Table({ style { width(100.percent) } }) {
            Thead { Tr { Th { Text("Empleado") }; Th { Text("Fecha") }; Th { Text("Tipo") }; Th { Text("Motivo") }; Th { Text("Estado") }; Th { Text("Autorizado por") }; if (canEdit) Th { Text("") } } }
            Tbody {
                items.forEach { j ->
                    Tr {
                        Td { Text(j.employeeName) }; Td { Text(j.fecha) }; Td { Text(j.tipo) }; Td { Text(j.motivo) }
                        Td {
                            Span({ style {
                                padding(4.px, 8.px); borderRadius(4.px); fontSize(12.px); fontWeight("bold")
                                backgroundColor(when (j.estado) { "Aprobado" -> Color("#10b981"); "Rechazado" -> Color("#ef4444"); else -> Color("#f59e0b") })
                                color(Color.white)
                            } }) { Text(j.estado) }
                        }
                        Td { Text(j.autorizadoPor) }
                        if (canEdit) Td {
                            Div({ style { display(DisplayStyle.Flex); gap(4.px) } }) {
                                if (j.estado == "Pendiente") {
                                    Button({ style { backgroundColor(Color("#10b981")); color(Color.white); property("border", "none"); borderRadius(4.px); padding(4.px, 8.px); cursor("pointer"); fontSize(12.px) }
                                        onClick { scope.launch { client.put("$BACKEND_URL/api/v1/pre-nomina/justificaciones/${j.id}/aprobar") { contentType(ContentType.Application.Json); setBody(mapOf("autorizadoPor" to "Admin")) }; refreshKey++ } }
                                    }) { Text("Aprobar") }
                                    Button({ style { backgroundColor(Color("#ef4444")); color(Color.white); property("border", "none"); borderRadius(4.px); padding(4.px, 8.px); cursor("pointer"); fontSize(12.px) }
                                        onClick { scope.launch { client.put("$BACKEND_URL/api/v1/pre-nomina/justificaciones/${j.id}/rechazar") { contentType(ContentType.Application.Json); setBody(mapOf("observaciones" to "Rechazado")) }; refreshKey++ } }
                                    }) { Text("Rechazar") }
                                }
                                Button({ style { backgroundColor(Color("#6b7280")); color(Color.white); property("border", "none"); borderRadius(4.px); padding(4.px, 8.px); cursor("pointer"); fontSize(12.px) }
                                    onClick { scope.launch { client.delete("$BACKEND_URL/api/v1/pre-nomina/justificaciones/${j.id}"); refreshKey++ } }
                                }) { Text("Borrar") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalculoTab(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope, canEdit: Boolean) {
    var items by remember { mutableStateOf(emptyList<PrePayrollRecord>()) }
    var isLoading by remember { mutableStateOf(false) }
    var isCalculating by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableStateOf(0) }
    var f_inicio by remember { mutableStateOf("") }
    var f_fin by remember { mutableStateOf("") }

    LaunchedEffect(refreshKey) {
        isLoading = true
        try { items = client.get("$BACKEND_URL/api/v1/pre-nomina/resultados").body() } catch (e: Exception) { println("Error: ${e.message}") }
        isLoading = false
    }

    val totalEmpleados = items.map { it.employeeId }.distinct().size
    val totalFaltas = items.sumOf { it.faltas }
    val totalRetardos = items.sumOf { it.retardosMenores + it.retardosMayores }
    val totalHorasExtra = items.sumOf { it.horasExtra }

    Div({ style { display(DisplayStyle.Grid); property("grid-template-columns", "1fr 1fr 1fr 1fr"); gap(16.px); marginBottom(24.px) } }) {
        StatCard("Empleados en periodo", totalEmpleados.toString(), "Calculo de pre-nomina", SidebarActiveColor)
        StatCard("Total de faltas", totalFaltas.toString(), "Sin justificar", Color("#ef4444"))
        StatCard("Total de retardos", totalRetardos.toString(), "Menores + mayores", Color("#f59e0b"))
        StatCard("Horas extra total", fmt1(totalHorasExtra), "Acumuladas", Color("#10b981"))
    }

    Div({ style { display(DisplayStyle.Flex); gap(8.px); marginBottom(16.px); alignItems(AlignItems.Center) } }) {
        Input(InputType.Text) { placeholder("Inicio YYYY-MM-DD"); value(f_inicio); onInput { f_inicio = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(160.px) } }
        Input(InputType.Text) { placeholder("Fin YYYY-MM-DD"); value(f_fin); onInput { f_fin = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(160.px) } }
        Button({
            style { padding(8.px, 16.px); backgroundColor(if (isCalculating) Color.gray else SidebarActiveColor); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer"); fontWeight("bold") }
            onClick {
                if (f_inicio.isNotBlank() && f_fin.isNotBlank()) {
                    isCalculating = true
                    scope.launch {
                        try {
                            val resp: String = client.post("$BACKEND_URL/api/v1/pre-nomina/calcular?inicio=$f_inicio&fin=$f_fin").body()
                            println("Calcular response: $resp")
                            refreshKey++
                        } catch (e: Exception) { window.alert("Error: ${e.message}") }
                        finally { isCalculating = false }
                    }
                }
            }
        }) { Text(if (isCalculating) "Calculando..." else "Calcular Pre-Nomina") }
        Button({
            style { padding(8.px, 16.px); backgroundColor(Color("#475569")); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") }
            onClick { refreshKey++ }
        }) { Text("Refrescar") }
        if (canEdit && items.isNotEmpty()) {
            Button({
                style { padding(8.px, 16.px); backgroundColor(Color("#ef4444")); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") }
                onClick { scope.launch { client.delete("$BACKEND_URL/api/v1/pre-nomina/resultados/bulk/all"); refreshKey++ } }
            }) { Text("Borrar resultados") }
        }
    }

    if (isLoading) { P { Text("Cargando...") } } else if (items.isEmpty()) {
        P({ style { color(Color.gray); textAlign("center"); padding(32.px, 0.px) } }) { Text("No hay resultados calculados. Selecciona un periodo y presiona Calcular Pre-Nomina.") }
    } else {
        Table({ style { width(100.percent); fontSize(13.px) } }) {
            Thead {
                Tr { Th { Text("Empleado") }; Th { Text("Periodo") }; Th { Text("Dias Trab.") }; Th { Text("Faltas") }; Th { Text("Ret.Men.") }; Th { Text("Ret.May.") }; Th { Text("Sal.Ant.") }; Th { Text("Hrs.Trab.") }; Th { Text("Hrs.Extra") }; Th { Text("Prima Dom.") }; Th { Text("Dias Desc.") }; if (canEdit) Th { Text("") } }
            }
            Tbody {
                items.forEach { r ->
                    Tr {
                        Td { Text(r.employeeName) }; Td { Text("${r.periodoInicio} a ${r.periodoFin}") }
                        Td { Text(r.diasTrabajados.toString()) }
                        Td { if (r.faltas > 0) Span({ style { color(Color("#ef4444")); fontWeight("bold") } }) { Text(r.faltas.toString()) } else Text("0") }
                        Td { Text(r.retardosMenores.toString()) }; Td { Text(r.retardosMayores.toString()) }; Td { Text(r.salidasAnticipadas.toString()) }
                        Td { Text(fmt1(r.horasTrabajadas)) }
                        Td { if (r.horasExtra > 0) Span({ style { color(Color("#10b981")); fontWeight("bold") } }) { Text(fmt1(r.horasExtra)) } else Text("0") }
                        Td { Text(fmt2(r.primaDominical)) }; Td { Text(r.diasDescansoTrabajados.toString()) }
                        if (canEdit) Td {
                            Button({ style { backgroundColor(Color("#ef4444")); color(Color.white); property("border", "none"); borderRadius(4.px); padding(4.px, 10.px); cursor("pointer") }
                                onClick { scope.launch { client.delete("$BACKEND_URL/api/v1/pre-nomina/resultados/${r.id}"); refreshKey++ } }
                            }) { Text("Eliminar") }
                        }
                    }
                }
            }
        }
    }
}
