import androidx.compose.runtime.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.attributes.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.browser.window
import kotlinx.browser.document
import kotlinx.coroutines.launch
import kotlin.coroutines.suspendCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import org.w3c.dom.HTMLInputElement
import com.example.rhnaf.shared.model.*

/**
 * QIS - Quality Information System: control de documentos.
 *
 * Alta de documentos controlados (código único + versión), cambio de estado
 * (Vigente/EnRevision/Obsoleto) y registro de cambios con historial por
 * documento. El alta siempre genera la primera entrada del historial y cada
 * nueva versión se registra con autor y aprobador (trazabilidad ISO 9001).
 * La escritura es de ADMIN; el resto de los roles solo consulta.
 */

/** Sube un archivo por multipart (FormData) a un endpoint del backend QIS. */
private suspend fun subirArchivoQis(file: org.w3c.files.File, urlDestino: String): dynamic =
    suspendCoroutine { cont ->
        val form = js("new FormData()")
        form.append("file", file, file.name)
        val options = js("({})")
        options.method = "POST"
        options.headers = js("({})")
        options.headers.Authorization = "Bearer $apiAuthToken"
        options.body = form
        window.asDynamic().fetch(urlDestino, options).then(
            { response: dynamic ->
                response.json().then(
                    { result: dynamic ->
                        if (response.ok) cont.resume(result)
                        else cont.resumeWithException(IllegalStateException(result.message?.toString() ?: "HTTP ${response.status}"))
                    },
                    { _: dynamic -> cont.resumeWithException(IllegalStateException("Respuesta ilegible")) }
                )
            },
            { _: dynamic -> cont.resumeWithException(IllegalStateException("Error de red al subir")) }
        )
    }

/** Abre en otra pestana el archivo de un documento QIS (puntero de Drive).
 *  PDF e imagenes se muestran en el navegador; Word/Excel se descargan con su
 *  nombre real (antes el blob sin nombre bajaba con un GUID inutil). */
private fun abrirArchivoQis(url: String) {
    val options = js("({})")
    options.headers = js("({})")
    options.headers.Authorization = "Bearer $apiAuthToken"
    window.asDynamic().fetch(url, options).then(
        { response: dynamic ->
            response.blob().then({ blob: dynamic ->
                // nombre real desde Content-Disposition (filename* UTF-8 o filename)
                val cd: String = (response.headers.get("content-disposition") as? String) ?: ""
                val nombreUtf8 = cd.substringAfter("filename*=UTF-8''", "").substringBefore(';').trim()
                val nombrePlano = cd.substringAfter("filename=\"", "").substringBefore("\"").trim()
                val nombreCod = if (nombreUtf8.isNotBlank()) nombreUtf8 else if (nombrePlano.isNotBlank()) nombrePlano else "documento"
                val nombre = runCatching { window.asDynamic().decodeURIComponent(nombreCod) as String }.getOrDefault(nombreCod)
                val tipo: String = (blob.type as? String) ?: ""
                val blobUrl = window.asDynamic().URL.createObjectURL(blob) as String
                if (tipo.contains("pdf") || tipo.contains("image/") || tipo.startsWith("text/")) {
                    // visualizable en el navegador
                    window.open(blobUrl, "_blank")
                } else {
                    // Word/Excel/etc: descarga con su nombre real
                    val a = js("document.createElement('a')")
                    a.href = blobUrl
                    a.download = nombre
                    js("document.body.appendChild(a)")
                    a.click()
                    js("document.body.removeChild(a)")
                }
                window.setTimeout({ window.asDynamic().URL.revokeObjectURL(blobUrl) }, 60000)
            })
        },
        { err: dynamic -> window.alert("No se pudo abrir el archivo: " + (err.message ?: "error")) }
            )
}

/** Etiqueta visible del estado segun el idioma activo. */
private fun etiquetaEstado(estado: String) = when (estado) {
    "Vigente" -> I18n.get("qis_estado_vigente")
    "EnRevision" -> I18n.get("qis_estado_revision")
    else -> I18n.get("qis_estado_obsoleto")
}

@Composable
fun QisModule(client: HttpClient, scope: CoroutineScope, isAdmin: Boolean) {
    var docs by remember { mutableStateOf(emptyList<QisDocument>()) }
    var error by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("") }
    var filterEstado by remember { mutableStateOf("") }
    var expandedDocId by remember { mutableStateOf(0) }
    var refreshKey by remember { mutableStateOf(0) }

    // Estados de formulario del alta
    var f_codigo by remember { mutableStateOf("") }
    var f_titulo by remember { mutableStateOf("") }
    var f_tipo by remember { mutableStateOf("Procedimiento") }
    var f_proceso by remember { mutableStateOf("") }
    var f_emision by remember { mutableStateOf("") }
    var f_revision by remember { mutableStateOf("") }
    var f_elab by remember { mutableStateOf("") }
    var f_aprob by remember { mutableStateOf("") }
    var f_url by remember { mutableStateOf("") }
    var f_notas by remember { mutableStateOf("") }

    fun refresh() { refreshKey++ }
    LaunchedEffect(refreshKey) {
        try {
            val url = if (filterEstado.isBlank()) "$BACKEND_URL/api/v1/qis/documentos"
                      else "$BACKEND_URL/api/v1/qis/documentos?estado=$filterEstado"
            docs = client.get(url).body()
            error = ""
        } catch (e: Exception) { error = "No se pudieron cargar los documentos: ${e.message ?: "error"}" }
    }


    fun hoyOrdenable(fecha: String): Boolean {
        // dd/MM/yyyy -> aaaaMMdd y comparar como texto con el dia de hoy
        val m = Regex("""^(\d{2})/(\d{2})/(\d{4})$""").find(fecha) ?: return false
        val (d, mes, a) = m.destructured
        val fechaNum = a + mes + d
        val hoyNum = js("new Date().toISOString().slice(0,10).replace(/-/g, '')").toString()
        return fechaNum < hoyNum
    }

    Div {
        H1({ style { color(Color("#0f172a")) } }) { Text(I18n.get("qis_docs")) }
        P({ style { color(Color("#475569")) } }) {
            Text(I18n.get("qis_intro"))
        }
        if (error.isNotBlank()) P({ style { color(Color("#b91c1c")) } }) { Text(error) }
        if (notice.isNotBlank()) P({ style { color(Color("#047857")) } }) { Text(notice) }

        // ===================== ALTA DE DOCUMENTO =====================
        if (isAdmin) {
            Div({ style { padding(18.px); backgroundColor(Color.white); borderRadius(10.px); property("border", "1px solid #e2e8f0"); marginBottom(16.px) } }) {
                H3({ style { margin(0.px, 0.px, 10.px, 0.px); color(Color("#0f172a")) } }) { Text(I18n.get("qis_alta")) }
                P({ style { fontSize(13.px); color(Color.gray); marginTop(0.px) } }) {
                    Text(I18n.get("qis_alta_hint"))
                }
                Div({ style { display(DisplayStyle.Flex); flexWrap(FlexWrap.Wrap); gap(8.px) } }) {
                    Input(InputType.Text) { placeholder(I18n.get("qis_ph_codigo")); value(f_codigo); onInput { f_codigo = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(120.px) } }
                    Input(InputType.Text) { placeholder(I18n.get("qis_ph_titulo")); value(f_titulo); onInput { f_titulo = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(260.px) } }
                    Select({
                        style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(140.px) }
                        onChange { f_tipo = it.value ?: "Procedimiento" }
                    }) {
                        listOf("Procedimiento", "Instructivo", "Formato", "Politica", "Manual", "Plan", "Registro", "Otro").forEach {
                            Option(it) { Text(it) }
                        }
                    }
                    Input(InputType.Text) { placeholder(I18n.get("qis_ph_proceso")); value(f_proceso); onInput { f_proceso = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(150.px) } }
                    Input(InputType.Text) { placeholder(I18n.get("qis_ph_emision")); value(f_emision); onInput { f_emision = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(160.px) } }
                    Input(InputType.Text) { placeholder(I18n.get("qis_ph_revision")); value(f_revision); onInput { f_revision = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(160.px) } }
                    Input(InputType.Text) { placeholder(I18n.get("qis_ph_elaboro")); value(f_elab); onInput { f_elab = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(150.px) } }
                    Input(InputType.Text) { placeholder(I18n.get("qis_ph_aprobo")); value(f_aprob); onInput { f_aprob = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(150.px) } }
                    Input(InputType.Text) { placeholder(I18n.get("qis_ph_url")); value(f_url); onInput { f_url = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(260.px) } }
                    Input(InputType.Text) { placeholder(I18n.get("qis_ph_notas")); value(f_notas); onInput { f_notas = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(200.px) } }
                }
                Div({ style { display(DisplayStyle.Flex); gap(8.px); marginTop(8.px); alignItems(AlignItems.Center) } }) {
                    Text(I18n.get("qis_archivo"))
                    Input(InputType.File) { id("qis-alta-file"); attr("accept", ".pdf,.docx,.xlsx,.xls,.png,.jpg,.jpeg") }
                    Span({ style { fontSize(11.px); color(Color.gray) } }) { Text(I18n.get("qis_archivo_hint")) }
                }
                Button({
                    style { marginTop(10.px); padding(9.px, 18.px); backgroundColor(Color("#0f766e")); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer"); fontWeight("600") }
                    onClick {
                        error = ""; notice = ""
                        if (f_codigo.isBlank() || f_titulo.isBlank() || f_emision.isBlank() || f_elab.isBlank()) {
                            error = I18n.get("qis_alta_falta")
                        } else {
                            scope.launch {
                                try {
                                    val resp = client.post("$BACKEND_URL/api/v1/qis/documentos") {
                                        contentType(ContentType.Application.Json)
                                        setBody(QisDocumentRequest(
                                            codigo = f_codigo.trim(), titulo = f_titulo.trim(), tipo = f_tipo,
                                            proceso = f_proceso.trim(), fechaEmision = f_emision.trim(),
                                            proximaRevision = f_revision.trim(), elaboradoPor = f_elab.trim(),
                                            aprobadoPor = f_aprob.trim(), urlArchivo = f_url.trim(), notas = f_notas.trim()
                                        ))
                                    }
                                    if (resp.status.value in 200..299) {
                                        val creado = resp.body<Map<String, String>>()
                                        val nuevoId = creado["id"]
                                        val archivo = (document.getElementById("qis-alta-file") as? HTMLInputElement)?.files?.item(0)
                                        var subidaDrive = true
                                        if (archivo != null && nuevoId != null) {
                                            try {
                                                val resDrive = subirArchivoQis(archivo, "$BACKEND_URL/api/v1/qis/documentos/$nuevoId/archivo")
                                                if (resDrive.status != "ok") {
                                                    subidaDrive = false
                                                    error = (resDrive.message ?: I18n.get("qis_archivo_fallo"))
                                                }
                                            } catch (e: Exception) { subidaDrive = false; error = I18n.get("qis_archivo_fallo") + ": " + (e.message ?: "") }
                                        }
                                        f_codigo = ""; f_titulo = ""; f_proceso = ""; f_emision = ""; f_revision = ""
                                        f_elab = ""; f_aprob = ""; f_url = ""; f_notas = ""
                                        notice = I18n.get("qis_alta_ok") + (if (subidaDrive && archivo != null) " " + I18n.get("qis_archivo_ok") else "")
                                        refresh()
                                    } else {
                                        val msg = resp.body<Map<String, String>>()
                                        error = msg["message"] ?: "No se pudo dar de alta (${resp.status.value})"
                                    }
                                } catch (e: Exception) { error = "Error: ${e.message}" }
                            }
                        }
                    }
                }) { Text(I18n.get("qis_btn_alta")) }
            }
        }

        // ===================== IMPORTAR LISTADO MAESTRO =====================
        if (isAdmin) {
            Div({ style { padding(18.px); backgroundColor(Color.white); borderRadius(10.px); property("border", "1px solid #e2e8f0"); marginBottom(16.px) } }) {
                H3({ style { margin(0.px, 0.px, 10.px, 0.px); color(Color("#0f172a")) } }) { Text(I18n.get("qis_import_listado")) }
                P({ style { fontSize(13.px); color(Color.gray); marginTop(0.px) } }) { Text(I18n.get("qis_import_hint")) }
                Div({ style { display(DisplayStyle.Flex); gap(8.px); alignItems(AlignItems.Center) } }) {
                    Input(InputType.File) { id("qis-import-listado"); attr("accept", ".xlsx,.xls") }
                    Button({
                        style { padding(8.px, 16.px); backgroundColor(Color("#0f766e")); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer"); fontWeight("600") }
                        onClick {
                            error = ""; notice = ""
                            val file = (document.getElementById("qis-import-listado") as? HTMLInputElement)?.files?.item(0)
                            if (file == null) {
                                error = I18n.get("qis_import_sin_archivo")
                            } else {
                                scope.launch {
                                    try {
                                        val res = subirArchivoQis(file, "$BACKEND_URL/api/v1/qis/documentos/importar-listado")
                                        if (res.status == "ok") {
                                            notice = I18n.get("qis_import_ok")
                                                .replace("{n}", res.creados?.toString() ?: "0")
                                                .replace("{d}", res.omitidos?.toString() ?: "0")
                                            if (res.drive == "error") notice += " " + I18n.get("qis_import_drive_error")
                                            refresh()
                                        } else {
                                            error = res.message ?: I18n.get("qis_import_error")
                                        }
                                    } catch (e: Exception) { error = I18n.get("qis_import_error") + ": " + (e.message ?: "") }
                                }
                            }
                        }
                    }) { Text(I18n.get("qis_import_btn")) }
                }
            }
        }

        // ===================== LISTADO =====================
        Div({ style { display(DisplayStyle.Flex); gap(8.px); marginBottom(10.px); alignItems(AlignItems.Center) } }) {
            Span({ style { color(Color("#475569")) } }) { Text(I18n.get("qis_filtro")) }
            Select({
                style { padding(6.px); borderRadius(6.px); property("border", "1px solid #cbd5e1") }
                onChange { filterEstado = it.value ?: ""; refresh() }
            }) {
                Option("") { Text(I18n.get("qis_todos")) }
                Option("Vigente") { Text(I18n.get("qis_vigentes")) }
                Option("EnRevision") { Text(I18n.get("qis_en_revision")) }
                Option("Obsoleto") { Text(I18n.get("qis_obsoletos")) }
            }
            Span({ style { marginLeft(12.px); color(Color.gray); fontSize(13.px) } }) { Text(I18n.get("qis_doc_count").replace("{n}", "${docs.size}")) }
        }

        Table({ style { width(100.percent); property("border-collapse", "collapse"); backgroundColor(Color.white); borderRadius(8.px); property("border", "1px solid #e2e8f0") } }) {
            Thead {
                Tr({ style { property("border-bottom", "2px solid #e2e8f0"); textAlign("left") } }) {
                    listOf(I18n.get("qis_h_codigo"), I18n.get("qis_h_titulo"), I18n.get("qis_h_tipo"), I18n.get("qis_h_proceso"), I18n.get("qis_h_version"), I18n.get("qis_h_estado"), I18n.get("qis_h_emision"), I18n.get("qis_h_prox"), I18n.get("qis_h_pdf"), I18n.get("qis_h_cambios"), "").forEach {
                        Th({ style { padding(8.px, 10.px); fontSize(12.px); color(Color("#64748b")) } }) { Text(it) }
                    }
                }
            }
            Tbody {
                docs.forEach { doc ->
                    val (estadoColor, estadoTexto) = when (doc.estado) {
                        "Vigente" -> Color("#047857") to etiquetaEstado("Vigente")
                        "EnRevision" -> Color("#b45309") to etiquetaEstado("EnRevision")
                        else -> Color("#6b7280") to etiquetaEstado("Obsoleto")
                    }
                    Tr({ style { property("border-bottom", "1px solid #f1f5f9") } }) {
                        Td({ style { padding(8.px, 10.px); fontWeight("600") } }) { Text(doc.codigo) }
                        Td({ style { padding(8.px, 10.px) } }) { Text(doc.titulo) }
                        Td({ style { padding(8.px, 10.px) } }) { Text(doc.tipo) }
                        Td({ style { padding(8.px, 10.px) } }) { Text(doc.proceso) }
                        Td({ style { padding(8.px, 10.px); fontWeight("600") } }) { Text(doc.versionActual) }
                        Td({ style { padding(8.px, 10.px) } }) {
                            Span({ style { backgroundColor(estadoColor); color(Color.white); padding(2.px, 8.px); borderRadius(10.px); fontSize(11.px) } }) { Text(estadoTexto) }
                        }
                        Td({ style { padding(8.px, 10.px); fontSize(13.px) } }) { Text(doc.fechaEmision) }
                        Td({ style { padding(8.px, 10.px); fontSize(13.px); color(if (hoyOrdenable(doc.proximaRevision)) Color("#dc2626") else Color("#334155")) } }) {
                            Text(if (doc.proximaRevision.isBlank()) "-" else doc.proximaRevision)
                        }
                        Td({ style { padding(8.px, 10.px) } }) {
                            if (doc.urlArchivo.isNotBlank()) {
                                if (doc.urlArchivo.startsWith("gdrive:")) {
                                    A(href = "#", {
                                        style { color(Color("#2563eb")); fontSize(12.px) }
                                        onClick { ev -> ev.preventDefault(); abrirArchivoQis("$BACKEND_URL/api/v1/qis/documentos/${doc.id}/archivo") }
                                    }) { Text(I18n.get("qis_ver")) }
                                } else A(href = doc.urlArchivo, { style { color(Color("#2563eb")); fontSize(12.px) } }) { Text(I18n.get("qis_ver")) }
                            } else Text("-")
                        }
                        Td({ style { padding(8.px, 10.px) } }) {
                            Button({
                                style { padding(3.px, 8.px); backgroundColor(if (expandedDocId == doc.id) Color("#334155") else Color("#2563eb")); color(Color.white); property("border", "none"); borderRadius(4.px); cursor("pointer"); fontSize(11.px) }
                                onClick { expandedDocId = if (expandedDocId == doc.id) 0 else doc.id }
                            }) { Text(I18n.get("qis_historial")) }
                        }
                        Td({ style { padding(8.px, 10.px) } }) {
                            if (isAdmin) Button({
                                style { padding(4.px, 10.px); backgroundColor(Color("#ef4444")); color(Color.white); property("border", "none"); borderRadius(4.px); cursor("pointer"); fontSize(11.px) }
                                onClick {
                                    if (window.confirm(I18n.get("qis_confirm_borrar").replace("{c}", doc.codigo))) {
                                        scope.launch {
                                            try {
                                                client.delete("$BACKEND_URL/api/v1/qis/documentos/${doc.id}")
                                                refresh()
                                            } catch (e: Exception) { error = "Error al borrar: ${e.message}" }
                                        }
                                    }
                                }
                            }) { Text("X") } else Text("")
                        }
                    }
                    // Fila expandible: estado, historial y (ADMIN) nuevo cambio
                    if (expandedDocId == doc.id) {
                        Tr {
                            Td({ attr("colspan", "11"); style { padding(14.px, 16.px); backgroundColor(Color("#f8fafc")) } }) {
                                QisDocDetail(client, scope, doc, isAdmin) { expandedDocId = 0; refresh() }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Detalle expandido: control de estado, historial de cambios y nuevo cambio de versión.
@Composable
private fun QisDocDetail(client: HttpClient, scope: CoroutineScope, doc: QisDocument, isAdmin: Boolean, onDone: () -> Unit) {
    var cambios by remember(doc.id) { mutableStateOf(emptyList<QisChangeRecord>()) }
    LaunchedEffect(doc.id, doc.versionActual) {
        try {
            cambios = client.get("$BACKEND_URL/api/v1/qis/documentos/${doc.id}/cambios").body()
        } catch (e: Exception) { println("QIS cambios: ${e.message}") }
    }

    Div {
        if (isAdmin) {
            Div({ style { display(DisplayStyle.Flex); gap(6.px); alignItems(AlignItems.Center); marginBottom(12.px) } }) {
                Text(I18n.get("qis_estado"))
                listOf("Vigente", "EnRevision", "Obsoleto").forEach { estado ->
                    Button({
                        style { padding(4.px, 10.px); backgroundColor(if (doc.estado == estado) Color("#0f766e") else Color("#e2e8f0")); color(if (doc.estado == estado) Color.white else Color("#334155")); property("border", "none"); borderRadius(4.px); cursor("pointer"); fontSize(11.px) }
                        onClick {
                            scope.launch {
                                try {
                                    client.patch("$BACKEND_URL/api/v1/qis/documentos/${doc.id}/estado?estado=$estado")
                                    onDone()
                                } catch (e: Exception) { println("QIS estado: ${e.message}") }
                            }
                        }
                    }) { Text(etiquetaEstado(estado)) }
                }
            }
            // Subir o actualizar el archivo controlado en Drive
            Div({ style { display(DisplayStyle.Flex); gap(8.px); alignItems(AlignItems.Center); marginBottom(12.px) } }) {
                Text(I18n.get("qis_subir_archivo"))
                Input(InputType.File) { id("qis-doc-file-${doc.id}"); attr("accept", ".pdf,.docx,.xlsx,.xls,.png,.jpg,.jpeg") }
                Button({
                    style { padding(5.px, 12.px); backgroundColor(Color("#0f766e")); color(Color.white); property("border", "none"); borderRadius(4.px); cursor("pointer"); fontSize(11.px) }
                    onClick {
                        val file = (document.getElementById("qis-doc-file-${doc.id}") as? HTMLInputElement)?.files?.item(0)
                        if (file != null) scope.launch {
                            try {
                                val res = subirArchivoQis(file, "$BACKEND_URL/api/v1/qis/documentos/${doc.id}/archivo")
                                if (res.status == "ok") onDone()
                            } catch (e: Exception) { println("QIS archivo: ${e.message}") }
                        }
                    }
                }) { Text("OK") }
            }
            QisNewChangeForm(client, scope, doc, onDone)
        }

        H4({ style { margin(14.px, 0.px, 6.px, 0.px) } }) { Text(I18n.get("qis_cambios").replace("{n}", "${cambios.size}")) }
        if (cambios.isEmpty()) P({ style { fontSize(13.px); color(Color.gray) } }) { Text(I18n.get("qis_sin_cambios")) }
        else Table({ style { width(100.percent); property("border-collapse", "collapse") } }) {
            Thead {
                Tr({ style { textAlign("left") } }) {
                    listOf(I18n.get("qis_h_version"), I18n.get("qis_h_fecha"), I18n.get("qis_h_desc"), I18n.get("qis_h_cambio_por"), I18n.get("qis_h_aprobo")).forEach {
                        Th({ style { padding(6.px, 8.px); fontSize(11.px); color(Color("#64748b")); property("border-bottom", "1px solid #e2e8f0") } }) { Text(it) }
                    }
                }
            }
            Tbody {
                cambios.forEach { c ->
                    Tr {
                        Td({ style { padding(6.px, 8.px); fontSize(13.px); property("white-space", "nowrap") } }) { Text("${c.versionAnterior} → ${c.versionNueva}") }
                        Td({ style { padding(6.px, 8.px); fontSize(13.px); property("white-space", "nowrap") } }) { Text(c.fechaCambio) }
                        Td({ style { padding(6.px, 8.px); fontSize(13.px) } }) { Text(c.descripcion) }
                        Td({ style { padding(6.px, 8.px); fontSize(13.px) } }) { Text(c.cambiadoPor) }
                        Td({ style { padding(6.px, 8.px); fontSize(13.px) } }) { Text(c.aprobadoPor) }
                    }
                }
            }
        }
    }
}

// Registro de nueva versión: actualiza la vigente y guarda la fila del historial.
@Composable
private fun QisNewChangeForm(client: HttpClient, scope: CoroutineScope, doc: QisDocument, onDone: () -> Unit) {
    var c_version by remember(doc.id) { mutableStateOf("") }
    var c_fecha by remember(doc.id) { mutableStateOf("") }
    var c_desc by remember(doc.id) { mutableStateOf("") }
    var c_quien by remember(doc.id) { mutableStateOf("") }
    var c_aprob by remember(doc.id) { mutableStateOf("") }
    var c_url by remember(doc.id) { mutableStateOf("") }
    var msg by remember(doc.id) { mutableStateOf("") }

    Div({ style { padding(12.px); backgroundColor(Color.white); borderRadius(8.px); property("border", "1px solid #e2e8f0") } }) {
        H4({ style { margin(0.px, 0.px, 8.px, 0.px) } }) { Text(I18n.get("qis_nueva_version").replace("{v}", doc.versionActual)) }
        Div({ style { display(DisplayStyle.Flex); flexWrap(FlexWrap.Wrap); gap(8.px) } }) {
            Input(InputType.Text) { placeholder(I18n.get("qis_ph_nueva_version")); value(c_version); onInput { c_version = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(130.px) } }
            Input(InputType.Text) { placeholder(I18n.get("qis_ph_fecha")); value(c_fecha); onInput { c_fecha = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(140.px) } }
            Input(InputType.Text) { placeholder(I18n.get("qis_ph_desc")); value(c_desc); onInput { c_desc = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(260.px) } }
            Input(InputType.Text) { placeholder(I18n.get("qis_ph_realizo")); value(c_quien); onInput { c_quien = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(150.px) } }
            Input(InputType.Text) { placeholder(I18n.get("qis_ph_aprobo")); value(c_aprob); onInput { c_aprob = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(150.px) } }
            Input(InputType.Text) { placeholder(I18n.get("qis_ph_nueva_url")); value(c_url); onInput { c_url = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(220.px) } }
        }
        Button({
            style { marginTop(10.px); padding(8.px, 16.px); backgroundColor(Color("#2563eb")); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer"); fontWeight("600") }
            onClick {
                msg = ""
                if (c_version.isBlank() || c_desc.isBlank() || c_quien.isBlank()) {
                    msg = I18n.get("qis_cambio_falta")
                } else {
                    scope.launch {
                        try {
                            val resp = client.post("$BACKEND_URL/api/v1/qis/documentos/${doc.id}/cambios") {
                                contentType(ContentType.Application.Json)
                                setBody(QisChangeRequest(
                                    versionNueva = c_version.trim(), fechaCambio = c_fecha.trim(),
                                    descripcion = c_desc.trim(), cambiadoPor = c_quien.trim(),
                                    aprobadoPor = c_aprob.trim(), nuevaUrlArchivo = c_url.trim()
                                ))
                            }
                            if (resp.status.value in 200..299) onDone()
                            else {
                                val m = resp.body<Map<String, String>>()
                                msg = m["message"] ?: "No se pudo registrar el cambio (${resp.status.value})"
                            }
                        } catch (e: Exception) { msg = "Error: ${e.message}" }
                    }
                }
            }
        }) { Text(I18n.get("qis_btn_cambio")) }
        if (msg.isNotBlank()) Span({ style { marginLeft(10.px); color(Color("#b91c1c")); fontSize(12.px) } }) { Text(msg) }
    }
}
