import androidx.compose.runtime.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.attributes.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.browser.window
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import com.example.rhnaf.shared.model.*

/**
 * Listado de Lockers (EHS y RH).
 *
 * Tabla de casilleros con asignar / liberar / llave copia. La BITACORA de cada
 * casillero conserva a TODAS las personas a quienes les ha pertenecido, aunque
 * ya este libre. Se puede importar el listado maestro (Excel) o subir cualquier
 * documento: se guarda en Google Drive (Lockers/AAAA).
 */

// Sube un archivo (multipart) a una ruta de lockers y devuelve el JSON de respuesta.
private suspend fun subirArchivoLockers(path: String, file: org.w3c.files.File, extra: Map<String, String>): dynamic =
    suspendCoroutine { cont ->
        val form = js("new FormData()")
        extra.forEach { (k, v) -> form.append(k, v) }
        form.append("file", file, file.name)
        val options = js("({})")
        options.method = "POST"
        options.headers = js("({})")
        options.headers.Authorization = "Bearer $apiAuthToken"
        options.body = form
        window.asDynamic().fetch("$BACKEND_URL$path", options).then(
            { response: dynamic ->
                response.json().then(
                    { result: dynamic ->
                        if (response.ok) cont.resume(result)
                        else cont.resumeWithException(IllegalStateException(result.message?.toString() ?: "HTTP ${response.status}"))
                    },
                    { _: dynamic -> cont.resumeWithException(IllegalStateException("Respuesta ilegible del servidor")) }
                )
            },
            { _: dynamic -> cont.resumeWithException(IllegalStateException("Error de red al subir")) }
        )
    }

private fun fechaOk(s: String) = s.isBlank() || Regex("""^\d{2}/\d{2}/\d{4}$""").matches(s)

@Composable
fun LockerModule(client: HttpClient, scope: CoroutineScope, canEdit: Boolean) {
    var lockers by remember { mutableStateOf(emptyList<Locker>()) }
    var docs by remember { mutableStateOf(emptyList<Map<String, String>>()) }
    var historial by remember { mutableStateOf(emptyList<LockerHistoryRecord>()) }
    var historialDe by remember { mutableStateOf(0) }          // numero de casillero abierto (0 = ninguno)
    var historialEmpleado by remember { mutableStateOf("") }   // busqueda global por empleado
    var historialGlobal by remember { mutableStateOf(false) }
    var filtro by remember { mutableStateOf("") }
    var busqueda by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableStateOf(0) }
    var r_inicio by remember { mutableStateOf(js("new Date().toISOString().slice(0,10)").toString()) }
    var r_fin by remember { mutableStateOf(js("new Date().toISOString().slice(0,10)").toString()) }
    var retiradosCount by remember { mutableStateOf(-1) }

    // Edicion en linea de un casillero
    var asignandoA by remember { mutableStateOf(0) }
    var f_empId by remember { mutableStateOf("") }
    var f_nombre by remember { mutableStateOf("") }
    var f_candado by remember { mutableStateOf("") }
    var f_llave by remember { mutableStateOf("Si") }
    var llaveDe by remember { mutableStateOf(0) }
    var f_prestamo by remember { mutableStateOf("") }
    var f_devolucion by remember { mutableStateOf("") }
    var liberando by remember { mutableStateOf(0) }
    var f_motivo by remember { mutableStateOf("") }
    var nuevoNumero by remember { mutableStateOf("") }

    fun refresh() { refreshKey++ }

    LaunchedEffect(refreshKey) {
        try {
            lockers = client.get("$BACKEND_URL/api/v1/lockers").body()
            docs = client.get("$BACKEND_URL/api/v1/lockers/documentos").body()
            if (historialDe > 0) historial = client.get("$BACKEND_URL/api/v1/lockers/$historialDe/historial").body()
        } catch (e: Exception) { error = "No se pudo cargar: ${e.message}" }
    }

    LaunchedEffect(r_inicio, r_fin, refreshKey) {
        retiradosCount = -1
        if (r_inicio.isNotBlank() && r_fin.isNotBlank()) {
            try {
                val lista: List<LockerHistoryRecord> = client.get("$BACKEND_URL/api/v1/lockers/retirados?inicio=$r_inicio&fin=$r_fin").body()
                retiradosCount = lista.size
            } catch (e: Exception) { retiradosCount = -1 }
        }
    }

    val total = lockers.size
    val asignados = lockers.count { it.estado == "Asignado" }
    val libres = lockers.count { it.estado == "Libre" }
    val visibles = lockers.filter { l ->
        (filtro.isBlank() || l.estado == filtro) &&
        (busqueda.isBlank() || l.nombre.contains(busqueda, ignoreCase = true) ||
            l.employeeId.contains(busqueda, ignoreCase = true) || l.numero.toString() == busqueda.trim())
    }

    fun mostrarError(r: io.ktor.client.statement.HttpResponse, defecto: String) {
        scope.launch {
            error = try { r.body<Map<String, String>>()["message"] ?: defecto } catch (e: Exception) { defecto }
        }
    }

    Div {
        H1({ style { color(Color("#0f172a")) } }) { Text("Listado de Lockers") }
        P({ style { color(Color("#475569")) } }) {
            Text("Control de casilleros de empleados para EHS y RH. Aunque un casillero se libere, la bitácora conserva a todas las personas a quienes les ha pertenecido.")
        }
        if (error.isNotBlank()) P({ style { color(Color("#b91c1c")) } }) { Text(error) }
        if (notice.isNotBlank()) P({ style { color(Color("#047857")) } }) { Text(notice) }

        // ===================== RESUMEN =====================
        Div({ style { display(DisplayStyle.Flex); gap(12.px); marginBottom(16.px); flexWrap(FlexWrap.Wrap) } }) {
            listOf(Triple("Total", total, "#0f172a"), Triple("Asignados", asignados, "#b45309"), Triple("Lockers disponibles", libres, "#047857")).forEach { (t, n, c) ->
                Div({ style { padding(14.px, 22.px); backgroundColor(Color.white); borderRadius(10.px); property("border", "1px solid #e2e8f0"); minWidth(150.px) } }) {
                    Div({ style { fontSize(12.px); color(Color("#64748b")) } }) { Text(t) }
                    Div({ style { fontSize(28.px); fontWeight("700"); color(Color(c)) } }) { Text(n.toString()) }
                }
            }
        }

        // ===================== LOCKERS RETIRADOS POR PERIODO =====================
        Div({ style { padding(16.px); backgroundColor(Color.white); borderRadius(10.px); property("border", "1px solid #e2e8f0"); marginBottom(16.px) } }) {
            H3({ style { margin(0.px, 0.px, 6.px, 0.px); color(Color("#0f172a")) } }) { Text("Lockers retirados por periodo") }
            P({ style { fontSize(13.px); color(Color("#64748b")); margin(0.px, 0.px, 10.px, 0.px) } }) {
                Text("Elige el rango con el calendario y descarga el listado de casilleros liberados en ese periodo, con el nombre de quien lo tenia.")
            }
            Div({ style { display(DisplayStyle.Flex); gap(8.px); alignItems(AlignItems.Center); flexWrap(FlexWrap.Wrap) } }) {
                Text("Del:")
                Input(InputType.Date) { value(r_inicio); onInput { r_inicio = it.value }; style { padding(6.px); borderRadius(6.px); property("border", "1px solid #cbd5e1") } }
                Text("al:")
                Input(InputType.Date) { value(r_fin); onInput { r_fin = it.value }; style { padding(6.px); borderRadius(6.px); property("border", "1px solid #cbd5e1") } }
                if (retiradosCount >= 0) Span({ style { fontSize(13.px); color(Color("#64748b")) } }) { Text("$retiradosCount liberados en el rango") }
                Button({
                    style { padding(6.px, 14.px); backgroundColor(Color("#2563eb")); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer"); fontSize(13.px) }
                    onClick {
                        if (r_inicio.isNotBlank() && r_fin.isNotBlank()) {
                        val opts = js("({})"); opts.headers = js("({})"); opts.headers.Authorization = "Bearer $apiAuthToken"
                        window.asDynamic().fetch("$BACKEND_URL/api/v1/lockers/retirados/csv?inicio=$r_inicio&fin=$r_fin", opts)
                            .then { r: dynamic -> r.blob() }
                            .then { b: dynamic ->
                                val url = window.asDynamic().URL.createObjectURL(b) as String
                                val a = js("document.createElement('a')")
                                a.href = url
                                a.download = "lockers_retirados_" + r_inicio + "_a_" + r_fin + ".csv"
                                js("document.body.appendChild(a)")
                                a.click()
                                js("document.body.removeChild(a)")
                                window.setTimeout({ window.asDynamic().URL.revokeObjectURL(url) }, 3000)
                            }
                            .`catch` { e: dynamic -> scope.launch { error = "No se pudo descargar el listado: " + (e.message ?: "error") } }
                        }
                    }
                }) { Text("Descargar CSV") }
            }
        }

        // ===================== IMPORTAR / DRIVE =====================
        if (canEdit) {
            Div({ style { padding(16.px); backgroundColor(Color.white); borderRadius(10.px); property("border", "1px solid #e2e8f0"); marginBottom(16.px) } }) {
                H3({ style { margin(0.px, 0.px, 8.px, 0.px); color(Color("#0f172a")) } }) { Text("Importar listado maestro y guardar en Drive") }
                P({ style { fontSize(13.px); color(Color.gray); marginTop(0.px) } }) {
                    Text("Sube tu Excel (.xlsx/.xls). Se actualizan los casilleros y la bitácora (sin duplicar) y el archivo queda respaldado en Google Drive, carpeta Lockers/año.")
                }
                Input(InputType.File) {
                    attr("accept", ".xlsx,.xls")
                    onChange { ev ->
                        val file = ev.target.files?.item(0) ?: return@onChange
                        error = ""; notice = ""; cargando = true
                        scope.launch {
                            try {
                                val r = subirArchivoLockers("/api/v1/lockers/importar-archivo", file, emptyMap())
                                val driveTxt = when (r.drive.toString()) {
                                    "ok" -> "Respaldado en Drive (Lockers)."
                                    "no-configurado" -> "Google Drive no está conectado: no se guardó el respaldo."
                                    else -> "Importado, pero Drive no confirmó el respaldo."
                                }
                                notice = "Importación lista: ${r.asignados} asignados, ${r.liberados} liberados, ${r.creados} casilleros nuevos, ${r.sinCambio} sin cambios. $driveTxt"
                                refresh()
                            } catch (e: Exception) { error = "No se pudo importar: ${e.message}" }
                            finally { cargando = false }
                        }
                    }
                }
                if (cargando) Span({ style { marginLeft(10.px); color(Color("#2563eb")) } }) { Text("Procesando...") }

                H4({ style { margin(16.px, 0.px, 6.px, 0.px); color(Color("#0f172a")) } }) { Text("Subir otro documento a Drive") }
                Div({ style { display(DisplayStyle.Flex); alignItems(AlignItems.Center); gap(8.px) } }) {
                    Input(InputType.File) {
                        onChange { ev ->
                            val file = ev.target.files?.item(0) ?: return@onChange
                            error = ""; notice = ""; cargando = true
                            scope.launch {
                                try {
                                    val r = subirArchivoLockers("/api/v1/lockers/documentos", file, mapOf("titulo" to file.name))
                                    notice = "Documento guardado en Drive: ${r.folder}"
                                    refresh()
                                } catch (e: Exception) { error = "No se pudo subir: ${e.message}" }
                                finally { cargando = false }
                            }
                        }
                    }
                }
                if (docs.isNotEmpty()) {
                    Div({ style { marginTop(10.px); fontSize(13.px) } }) {
                        Div({ style { color(Color("#64748b")); marginBottom(4.px) } }) { Text("Documentos en Drive (${docs.size}):") }
                        docs.take(8).forEach { d ->
                            Div({ style { padding(2.px, 0.px) } }) {
                                A(href = "$BACKEND_URL/api/v1/ehs/documentos/${d["id"]}/descargar", {
                                    style { color(Color("#2563eb")) }
                                    onClick { ev ->
                                        ev.preventDefault()
                                        val opts = js("({})"); opts.headers = js("({})"); opts.headers.Authorization = "Bearer $apiAuthToken"
                                        window.asDynamic().fetch("$BACKEND_URL/api/v1/ehs/documentos/${d["id"]}/descargar", opts)
                                            .then { r: dynamic -> r.blob() }
                                            .then { b: dynamic -> window.open(window.asDynamic().URL.createObjectURL(b) as String, "_blank") }
                                    }
                                }) { Text(d["fileName"] ?: d["titulo"] ?: "documento") }
                                Span({ style { color(Color.gray) } }) { Text("  ·  ${d["fecha"] ?: ""}") }
                            }
                        }
                    }
                }
            }

            // Alta de casillero nuevo
            Div({ style { display(DisplayStyle.Flex); gap(8.px); alignItems(AlignItems.Center); marginBottom(12.px) } }) {
                Input(InputType.Number) { placeholder("N° casillero nuevo"); value(nuevoNumero); onInput { nuevoNumero = it.value.toString() }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(150.px) } }
                Button({
                    style { padding(8.px, 14.px); backgroundColor(Color("#0f766e")); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") }
                    onClick {
                        val n = nuevoNumero.toIntOrNull()
                        if (n == null) { error = "Escribe un número de casillero válido."; return@onClick }
                        error = ""; notice = ""
                        scope.launch {
                            try {
                                val r = client.post("$BACKEND_URL/api/v1/lockers/$n")
                                if (r.status.value in 200..299) { notice = "Casillero $n agregado."; nuevoNumero = ""; refresh() }
                                else mostrarError(r, "No se pudo agregar el casillero")
                            } catch (e: Exception) { error = "Error: ${e.message}" }
                        }
                    }
                }) { Text("+ Agregar casillero") }
            }
        }

        // ===================== FILTROS =====================
        Div({ style { display(DisplayStyle.Flex); gap(8.px); marginBottom(10.px); alignItems(AlignItems.Center); flexWrap(FlexWrap.Wrap) } }) {
            Input(InputType.Text) { placeholder("Buscar por nombre, N° empleado o casillero"); value(busqueda); onInput { busqueda = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(300.px) } }
            Select({
                style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1") }
                onChange { filtro = it.value ?: "" }
            }) {
                Option("") { Text("Todos") }
                Option("Asignado") { Text("Asignados") }
                Option("Libre") { Text("Libres") }
            }
            Span({ style { color(Color.gray); fontSize(13.px) } }) { Text("${visibles.size} de $total") }
            Button({
                style { padding(7.px, 12.px); backgroundColor(Color("#475569")); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") }
                onClick { historialGlobal = !historialGlobal; historialDe = 0
                    if (historialGlobal) scope.launch {
                        try { historial = client.get("$BACKEND_URL/api/v1/lockers/historial").body() } catch (e: Exception) { error = "Error: ${e.message}" }
                    }
                }
            }) { Text(if (historialGlobal) "Ocultar bitácora global" else "Ver bitácora global") }
        }

        // ===================== TABLA =====================
        Table({ style { width(100.percent); property("border-collapse", "collapse"); backgroundColor(Color.white); borderRadius(8.px); property("border", "1px solid #e2e8f0") } }) {
            Thead {
                Tr({ style { property("border-bottom", "2px solid #e2e8f0"); textAlign("left") } }) {
                    listOf("N°", "N° Empleado", "Nombre completo", "Entrega de candado", "Llave copia", "Préstamo llave", "Devolución llave", "Estado", "").forEach {
                        Th({ style { padding(8.px, 10.px); fontSize(12.px); color(Color("#64748b")) } }) { Text(it) }
                    }
                }
            }
            Tbody {
                visibles.forEach { l ->
                    val ocupado = l.estado == "Asignado"
                    // Igual que el Excel original: sin llave copia se resalta en rojo.
                    val resaltar = ocupado && l.llaveCopia == "No"
                    Tr({ style { property("border-bottom", "1px solid #f1f5f9"); if (resaltar) backgroundColor(Color("#fee2e2")) } }) {
                        Td({ style { padding(8.px, 10.px); fontWeight("700") } }) { Text(l.numero.toString()) }
                        Td({ style { padding(8.px, 10.px) } }) { Text(l.employeeId.ifBlank { "-" }) }
                        Td({ style { padding(8.px, 10.px) } }) { Text(if (ocupado) l.nombre else "—") }
                        Td({ style { padding(8.px, 10.px); fontSize(13.px) } }) { Text(l.fechaCandado.ifBlank { "-" }) }
                        Td({ style { padding(8.px, 10.px) } }) { Text(if (ocupado) l.llaveCopia else "-") }
                        Td({ style { padding(8.px, 10.px); fontSize(13.px) } }) { Text(l.fechaPrestamoLlave.ifBlank { "-" }) }
                        Td({ style { padding(8.px, 10.px); fontSize(13.px) } }) { Text(l.fechaDevolucionLlave.ifBlank { "-" }) }
                        Td({ style { padding(8.px, 10.px) } }) {
                            Span({ style { backgroundColor(Color(if (ocupado) "#b45309" else "#047857")); color(Color.white); padding(2.px, 8.px); borderRadius(10.px); fontSize(11.px) } }) {
                                Text(if (ocupado) "Asignado" else "Libre")
                            }
                        }
                        Td({ style { padding(8.px, 10.px); property("white-space", "nowrap") } }) {
                            Button({
                                style { padding(4.px, 10.px); backgroundColor(Color("#e2e8f0")); color(Color("#0f172a")); property("border", "none"); borderRadius(6.px); cursor("pointer"); marginRight(4.px) }
                                onClick {
                                    historialGlobal = false
                                    if (historialDe == l.numero) { historialDe = 0 } else {
                                        historialDe = l.numero
                                        scope.launch {
                                            try { historial = client.get("$BACKEND_URL/api/v1/lockers/${l.numero}/historial").body() }
                                            catch (e: Exception) { error = "Error: ${e.message}" }
                                        }
                                    }
                                }
                            }) { Text("Historial") }
                            if (canEdit) {
                                Button({
                                    style { padding(4.px, 10.px); backgroundColor(Color("#2563eb")); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer"); marginRight(4.px) }
                                    onClick { asignandoA = if (asignandoA == l.numero) 0 else l.numero; liberando = 0; llaveDe = 0
                                        f_empId = ""; f_nombre = ""; f_candado = ""; f_llave = "Si" }
                                }) { Text(if (ocupado) "Reasignar" else "Asignar") }
                                if (ocupado) {
                                    Button({
                                        style { padding(4.px, 10.px); backgroundColor(Color("#7c3aed")); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer"); marginRight(4.px) }
                                        onClick { llaveDe = if (llaveDe == l.numero) 0 else l.numero; asignandoA = 0; liberando = 0
                                            f_prestamo = l.fechaPrestamoLlave; f_devolucion = l.fechaDevolucionLlave }
                                    }) { Text("Llave") }
                                    Button({
                                        style { padding(4.px, 10.px); backgroundColor(Color("#dc2626")); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") }
                                        onClick { liberando = if (liberando == l.numero) 0 else l.numero; asignandoA = 0; llaveDe = 0; f_motivo = "" }
                                    }) { Text("Liberar") }
                                }
                            }
                        }
                    }

                    // ---------- formularios en linea ----------
                    if (asignandoA == l.numero) {
                        Tr { Td({ attr("colspan", "9"); style { padding(12.px); backgroundColor(Color("#eff6ff")) } }) {
                            if (ocupado) P({ style { margin(0.px, 0.px, 8.px, 0.px); fontSize(13.px); color(Color("#b45309")) } }) {
                                Text("Al reasignar, ${l.nombre} queda registrado en la bitácora como anterior titular del casillero ${l.numero}.")
                            }
                            Div({ style { display(DisplayStyle.Flex); gap(8.px); flexWrap(FlexWrap.Wrap) } }) {
                                Input(InputType.Text) { placeholder("N° empleado"); value(f_empId); onInput { f_empId = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(120.px) } }
                                Input(InputType.Text) { placeholder("Nombre completo *"); value(f_nombre); onInput { f_nombre = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(240.px) } }
                                Input(InputType.Text) { placeholder("Entrega candado (dd/MM/aaaa)"); value(f_candado); onInput { f_candado = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(190.px) } }
                                Select({ style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1") }; onChange { f_llave = it.value ?: "Si" } }) {
                                    Option("Si") { Text("Llave copia: Sí") }
                                    Option("No") { Text("Llave copia: No") }
                                }
                                Button({
                                    style { padding(8.px, 16.px); backgroundColor(Color("#2563eb")); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer"); fontWeight("600") }
                                    onClick {
                                        error = ""; notice = ""
                                        if (f_nombre.isBlank()) { error = "El nombre es obligatorio."; return@onClick }
                                        if (!fechaOk(f_candado.trim())) { error = "La fecha de candado debe ser dd/MM/aaaa."; return@onClick }
                                        scope.launch {
                                            try {
                                                val r = client.put("$BACKEND_URL/api/v1/lockers/${l.numero}/asignar") {
                                                    contentType(ContentType.Application.Json)
                                                    setBody(LockerAssignRequest(employeeId = f_empId.trim(), nombre = f_nombre.trim(), fechaCandado = f_candado.trim(), llaveCopia = f_llave))
                                                }
                                                if (r.status.value in 200..299) { notice = "Casillero ${l.numero} asignado a ${f_nombre.trim()}."; asignandoA = 0; refresh() }
                                                else mostrarError(r, "No se pudo asignar")
                                            } catch (e: Exception) { error = "Error: ${e.message}" }
                                        }
                                    }
                                }) { Text("Guardar") }
                            }
                        } }
                    }
                    if (llaveDe == l.numero) {
                        Tr { Td({ attr("colspan", "9"); style { padding(12.px); backgroundColor(Color("#f5f3ff")) } }) {
                            Div({ style { display(DisplayStyle.Flex); gap(8.px); flexWrap(FlexWrap.Wrap) } }) {
                                Input(InputType.Text) { placeholder("Préstamo de llave (dd/MM/aaaa)"); value(f_prestamo); onInput { f_prestamo = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(210.px) } }
                                Input(InputType.Text) { placeholder("Devolución de llave (dd/MM/aaaa)"); value(f_devolucion); onInput { f_devolucion = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(220.px) } }
                                Button({
                                    style { padding(8.px, 16.px); backgroundColor(Color("#7c3aed")); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer"); fontWeight("600") }
                                    onClick {
                                        error = ""; notice = ""
                                        if (!fechaOk(f_prestamo.trim()) || !fechaOk(f_devolucion.trim())) { error = "Las fechas deben ser dd/MM/aaaa."; return@onClick }
                                        scope.launch {
                                            try {
                                                val r = client.put("$BACKEND_URL/api/v1/lockers/${l.numero}/llave") {
                                                    contentType(ContentType.Application.Json)
                                                    setBody(LockerKeyRequest(fechaPrestamoLlave = f_prestamo.trim(), fechaDevolucionLlave = f_devolucion.trim()))
                                                }
                                                if (r.status.value in 200..299) { notice = "Llave del casillero ${l.numero} actualizada."; llaveDe = 0; refresh() }
                                                else mostrarError(r, "No se pudo actualizar la llave")
                                            } catch (e: Exception) { error = "Error: ${e.message}" }
                                        }
                                    }
                                }) { Text("Guardar llave") }
                            }
                        } }
                    }
                    if (liberando == l.numero) {
                        Tr { Td({ attr("colspan", "9"); style { padding(12.px); backgroundColor(Color("#fef2f2")) } }) {
                            P({ style { margin(0.px, 0.px, 8.px, 0.px); fontSize(13.px); color(Color("#991b1b")) } }) {
                                Text("${l.nombre} quedará en la bitácora como anterior titular del casillero ${l.numero}. El registro NO se borra.")
                            }
                            Div({ style { display(DisplayStyle.Flex); gap(8.px) } }) {
                                Input(InputType.Text) { placeholder("Motivo (baja, cambio, devolución...)"); value(f_motivo); onInput { f_motivo = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(320.px) } }
                                Button({
                                    style { padding(8.px, 16.px); backgroundColor(Color("#dc2626")); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer"); fontWeight("600") }
                                    onClick {
                                        error = ""; notice = ""
                                        scope.launch {
                                            try {
                                                val r = client.put("$BACKEND_URL/api/v1/lockers/${l.numero}/liberar") {
                                                    contentType(ContentType.Application.Json)
                                                    setBody(LockerReleaseRequest(motivo = f_motivo.trim()))
                                                }
                                                if (r.status.value in 200..299) { notice = "Casillero ${l.numero} liberado."; liberando = 0; refresh() }
                                                else mostrarError(r, "No se pudo liberar")
                                            } catch (e: Exception) { error = "Error: ${e.message}" }
                                        }
                                    }
                                }) { Text("Confirmar liberación") }
                            }
                        } }
                    }
                }
            }
        }

        // ===================== BITACORA =====================
        if (historialDe > 0 || historialGlobal) {
            Div({ style { marginTop(20.px); padding(16.px); backgroundColor(Color.white); borderRadius(10.px); property("border", "1px solid #e2e8f0") } }) {
                H3({ style { margin(0.px, 0.px, 6.px, 0.px); color(Color("#0f172a")) } }) {
                    Text(if (historialGlobal) "Bitácora global de lockers" else "Bitácora del casillero $historialDe")
                }
                P({ style { fontSize(13.px); color(Color.gray); marginTop(0.px) } }) {
                    Text("Todas las personas a quienes les ha pertenecido. Las filas sin fecha de fin son la asignación vigente.")
                }
                if (historialGlobal) {
                    Div({ style { display(DisplayStyle.Flex); gap(8.px); marginBottom(8.px) } }) {
                        Input(InputType.Text) { placeholder("Filtrar por N° de empleado"); value(historialEmpleado); onInput { historialEmpleado = it.value }; style { padding(7.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(200.px) } }
                        Button({
                            style { padding(7.px, 12.px); backgroundColor(Color("#2563eb")); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") }
                            onClick { scope.launch {
                                try {
                                    historial = client.get("$BACKEND_URL/api/v1/lockers/historial") {
                                        if (historialEmpleado.isNotBlank()) parameter("empleado", historialEmpleado.trim())
                                    }.body()
                                } catch (e: Exception) { error = "Error: ${e.message}" }
                            } }
                        }) { Text("Buscar") }
                    }
                }
                if (historial.isEmpty()) {
                    P({ style { color(Color.gray) } }) { Text("Sin registros en la bitácora.") }
                } else {
                    Table({ style { width(100.percent); property("border-collapse", "collapse") } }) {
                        Thead {
                            Tr({ style { property("border-bottom", "2px solid #e2e8f0"); textAlign("left") } }) {
                                (if (historialGlobal) listOf("Casillero") else emptyList<String>()).plus(listOf("N° Empleado", "Nombre", "Candado", "Llave copia", "Inicio", "Fin", "Motivo", "Registró")).forEach {
                                    Th({ style { padding(6.px, 8.px); fontSize(12.px); color(Color("#64748b")) } }) { Text(it) }
                                }
                            }
                        }
                        Tbody {
                            historial.forEach { h ->
                                Tr({ style { property("border-bottom", "1px solid #f1f5f9"); if (h.fechaFin.isBlank()) backgroundColor(Color("#f0fdf4")) } }) {
                                    if (historialGlobal) Td({ style { padding(6.px, 8.px); fontWeight("700") } }) { Text(h.numero.toString()) }
                                    Td({ style { padding(6.px, 8.px) } }) { Text(h.employeeId.ifBlank { "-" }) }
                                    Td({ style { padding(6.px, 8.px) } }) { Text(h.nombre) }
                                    Td({ style { padding(6.px, 8.px); fontSize(13.px) } }) { Text(h.fechaCandado.ifBlank { "-" }) }
                                    Td({ style { padding(6.px, 8.px) } }) { Text(h.llaveCopia.ifBlank { "-" }) }
                                    Td({ style { padding(6.px, 8.px); fontSize(13.px) } }) { Text(h.fechaInicio.ifBlank { "-" }) }
                                    Td({ style { padding(6.px, 8.px); fontSize(13.px) } }) { Text(if (h.fechaFin.isBlank()) "Vigente" else h.fechaFin) }
                                    Td({ style { padding(6.px, 8.px); fontSize(13.px) } }) { Text(h.motivoFin.ifBlank { "-" }) }
                                    Td({ style { padding(6.px, 8.px); fontSize(12.px); color(Color.gray) } }) { Text(h.registradoPor.ifBlank { "-" }) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
