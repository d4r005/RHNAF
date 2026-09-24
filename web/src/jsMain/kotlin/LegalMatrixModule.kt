import androidx.compose.runtime.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.attributes.ATarget
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.w3c.dom.HTMLInputElement
import org.w3c.files.File
import kotlinx.browser.document
import kotlinx.browser.window
import com.example.rhnaf.shared.model.*

@Serializable
private data class LegalMatrixPage(
    val items: List<LegalMatrixItem> = emptyList(),
    val page: Int = 1,
    val pageSize: Int = 50,
    val total: Long = 0,
    val totalPages: Int = 0
)

// POST /api/v1/ehs/acciones usa este modelo (definido en el backend).
@Serializable
private data class NuevaTarea(
    val titulo: String,
    val descripcion: String = "",
    val origenTipo: String = "matriz_legal",
    val origenId: Int = 0,
    val responsable: String,
    val fechaLimite: String,
    val prioridad: String = "Media",
    val estado: String = "Abierta",
    val evidenciaUrl: String = "",
    val fechaCierre: String = "",
    val id: Int = 0
)

@Composable
fun LegalMatrixModule(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope, fixedCategory: String = "") {
    var items by remember { mutableStateOf(emptyList<LegalMatrixItem>()) }
    var summary by remember { mutableStateOf(LegalMatrixSummary()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<LegalMatrixItem?>(null) }
    var fichaId by remember { mutableStateOf<Int?>(null) }
    var creating by remember { mutableStateOf(false) }

    fun refresh() {
        scope.launch {
            loading = true
            error = ""
            try {
                val params = buildList {
                    val selectedCategory = fixedCategory.ifBlank { category }
                    if (selectedCategory.isNotBlank()) add("categoria=$selectedCategory")
                    if (status.isNotBlank()) add("estado=$status")
                    add("pageSize=100")
                }.joinToString("&")
                items = client.get("$BACKEND_URL/api/v1/ehs/matriz-legal?$params").body<LegalMatrixPage>().items
                if (fixedCategory.isBlank()) summary = client.get("$BACKEND_URL/api/v1/ehs/matriz-legal/dashboard").body()
            } catch (e: Exception) {
                error = "No se pudo cargar la matriz legal: ${e.message ?: "error desconocido"}"
            } finally { loading = false }
        }
    }

    LaunchedEffect(category, status, fixedCategory) { refresh() }

    Div {
        Div({ style { display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); alignItems(AlignItems.Center); marginBottom(24.px); flexWrap(FlexWrap.Wrap); gap(10.px) } }) {
            Div {
                H1({ style { margin(0.px); fontSize(28.px); color(Color("#0f172a")) } }) { Text(if (fixedCategory == "STPS") "Normas STPS" else "Matriz Legal EHS") }
                P({ style { marginTop(6.px); color(Color("#64748b")) } }) { Text(if (fixedCategory == "STPS") "Seguimiento de obligaciones STPS aplicables. Valida la aplicabilidad y vigencia con el responsable legal." else "Cumplimiento normativo, marco legal, evidencias documentales y recordatorios") }
            }
            Div({ style { display(DisplayStyle.Flex); gap(10.px) } }) {
                Button({
                    style { padding(10.px, 16.px); border(0.px); borderRadius(8.px); backgroundColor(Color("#059669")); color(Color.white); cursor("pointer") }
                    onClick { creating = true }
                }) { Text("+ Agregar obligación") }
                Button({
                    style { padding(10.px, 16.px); border(0.px); borderRadius(8.px); backgroundColor(Color("#2563eb")); color(Color.white); cursor("pointer") }
                    onClick {
                        scope.launch {
                            try {
                                client.post("$BACKEND_URL/api/v1/ehs/matriz-legal/seed")
                                refresh()
                            } catch (e: Exception) { error = "No se pudo inicializar el catálogo: ${e.message}" }
                        }
                    }
                }) { Text("Cargar catálogo de NOMs") }
            }
        }

        if (fixedCategory.isBlank()) Div({ style { display(DisplayStyle.Grid); property("grid-template-columns", "repeat(5, minmax(130px, 1fr))"); gap(14.px); marginBottom(22.px) } }) {
            LegalStat("Cumplimiento", "${summary.porcentajeCumplimiento.toInt()}%", "#2563eb")
            LegalStat("Vigentes", summary.vigentes.toString(), "#16a34a")
            LegalStat("Por vencer", summary.porVencer.toString(), "#d97706")
            LegalStat("Vencidas", summary.vencidos.toString(), "#dc2626")
            LegalStat("Pendientes", summary.pendientes.toString(), "#64748b")
        }

        if (fixedCategory.isBlank() && summary.porCategoria.isNotEmpty()) {
            H2({ style { margin(0.px, 0.px, 10.px, 0.px); fontSize(17.px); color(Color("#0f172a")) } }) { Text("Cumplimiento por categoría y subcategoría") }
            Div({ style { display(DisplayStyle.Grid); property("grid-template-columns", "repeat(auto-fit, minmax(280px, 1fr))"); gap(14.px); marginBottom(22.px) } }) {
                summary.porCategoria.forEach { cat ->
                    Div({ style { padding(14.px); backgroundColor(Color.white); borderRadius(10.px); property("border", "1px solid #e2e8f0") } }) {
                        Div({ style { display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); alignItems(AlignItems.Center) } }) {
                            B { Text(cat.categoria) }
                            Span({ style { fontWeight("700"); color(Color(if (cat.porcentaje >= 80.0) "#16a34a" else if (cat.porcentaje >= 50.0) "#d97706" else "#dc2626")) } }) { Text("${cat.porcentaje.toInt()}%") }
                        }
                        Div({ style { marginTop(6.px); height(6.px); borderRadius(4.px); backgroundColor(Color("#e2e8f0")); overflow("hidden") } }) {
                            Div({ style { height(100.percent); backgroundColor(Color("#2563eb")); width(cat.porcentaje.percent) } })
                        }
                        P({ style { fontSize(11.px); color(Color("#64748b")); margin(4.px, 0.px, 8.px, 0.px) } }) { Text("${cat.vigentes}/${cat.aplicables} obligaciones al día") }
                        cat.subCategorias.forEach { sub ->
                            Div({ style { display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); fontSize(12.px); padding(4.px, 0.px); property("border-top", "1px solid #f1f5f9") } }) {
                                Span({ style { color(Color("#475569")) } }) { Text(sub.subCategoria) }
                                Span({ style { fontWeight("600"); color(Color(if (sub.porcentaje >= 80.0) "#16a34a" else if (sub.porcentaje >= 50.0) "#d97706" else "#dc2626")) } }) {
                                    Text("${sub.porcentaje.toInt()}% (${sub.vigentes}/${sub.aplicables})")
                                }
                            }
                        }
                    }
                }
            }
        }

        Div({ style { display(DisplayStyle.Flex); gap(12.px); marginBottom(18.px) } }) {
            if (fixedCategory.isBlank()) Select({
                style { padding(9.px, 12.px); borderRadius(8.px); property("border", "1px solid #cbd5e1") }
                onChange { category = it.value ?: "" }
            }) {
                Option("") { Text("Todas las categorías") }
                listOf("STPS", "SEMARNAT", "PROFEPA", "ProteccionCivil", "Estatal", "Municipal").forEach { Option(it) { Text(it) } }
            }
            Select({
                style { padding(9.px, 12.px); borderRadius(8.px); property("border", "1px solid #cbd5e1") }
                onChange { status = it.value ?: "" }
            }) {
                Option("") { Text("Todos los estados") }
                listOf("Vigente", "PorVencer", "Vencido", "Pendiente", "NoAplica").forEach { Option(it) { Text(it) } }
            }
        }

        if (error.isNotBlank()) P({ style { color(Color("#dc2626")); padding(12.px); backgroundColor(Color("#fef2f2")); borderRadius(8.px) } }) { Text(error) }

        if (creating) {
            LegalNuevaObligacionForm(
                onCancel = { creating = false },
                onSave = { nueva ->
                    scope.launch {
                        try {
                            val response = client.post("$BACKEND_URL/api/v1/ehs/matriz-legal") {
                                contentType(ContentType.Application.Json); setBody(nueva)
                            }
                            if (!response.status.isSuccess()) {
                                val msg = runCatching { response.body<Map<String, String>>()["error"] }.getOrNull()
                                error = "No se creó la obligación: ${msg ?: "error ${response.status.value}"}"
                            } else { creating = false; refresh() }
                        } catch (e: Exception) { error = "No se pudo crear: ${e.message ?: "error"}" }
                    }
                }
            )
        }

        editing?.let { selected ->
            LegalApplicabilityEditor(selected, onCancel = { editing = null }) { changed ->
                scope.launch {
                    try {
                        val response = client.put("$BACKEND_URL/api/v1/ehs/matriz-legal/${changed.id}") {
                            contentType(ContentType.Application.Json); setBody(changed)
                        }
                        if (!response.status.isSuccess()) error = "No se guardó la evaluación (${response.status.value}). Revisa la justificación, responsable y fecha."
                        else { editing = null; refresh() }
                    } catch (e: Exception) { error = "No se pudo actualizar: ${e.message ?: "error"}" }
                }
            }
        }

        fichaId?.let { id -> LegalObligationDetail(client, scope, id, onClose = { fichaId = null }, onChanged = { refresh() }) }

        if (loading) {
            P { Text("Cargando matriz legal...") }
        } else if (items.isEmpty()) {
            Div({ style { padding(38.px); textAlign("center"); backgroundColor(Color.white); borderRadius(12.px); property("border", "1px solid #e2e8f0") } }) {
                H3 { Text(if (fixedCategory == "STPS") "No hay obligaciones STPS registradas" else "La matriz legal está vacía") }
                P({ style { color(Color("#64748b")) } }) { Text("Usa “Cargar catálogo de NOMs” para agregar las obligaciones iniciales, o “+ Agregar obligación” para legislación estatal, municipal o permisos locales.") }
            }
        } else {
            Div({ style { backgroundColor(Color.white); borderRadius(12.px); property("border", "1px solid #e2e8f0"); overflow("auto") } }) {
                Table({ style { width(100.percent); property("border-collapse", "collapse"); fontSize(13.px) } }) {
                    Thead { Tr { listOf("Clave", "Obligación", "Categoría", "Subcategoría", "Tipo", "Aplica", "Estado", "Vigencia", "Responsable", "Crítica", "Docs", "").forEach { Th({ style { padding(12.px); textAlign("left"); backgroundColor(Color("#f8fafc")); color(Color("#475569")); property("border-bottom", "1px solid #e2e8f0") } }) { Text(it) } } } }
                    Tbody {
                        items.forEach { item ->
                            Tr {
                                Td({ style { padding(12.px); fontWeight("600"); property("border-bottom", "1px solid #f1f5f9") } }) { Text(item.clave) }
                                Td({ style { padding(12.px); property("min-width", "260px"); property("border-bottom", "1px solid #f1f5f9") } }) {
                                    Text(item.titulo)
                                    if (item.urlNorma.isNotBlank()) {
                                        A(href = item.urlNorma, attrs = { target(ATarget.Blank); style { color(Color("#2563eb")); fontSize(11.px); marginLeft(6.px); textDecoration("underline") } }) { Text("Norma") }
                                    }
                                }
                                Td({ style { padding(12.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(item.categoria) }
                                Td({ style { padding(12.px); property("border-bottom", "1px solid #f1f5f9"); fontSize(12.px); color(Color("#475569")) } }) { Text(item.subCategoria.ifBlank { "—" }) }
                                Td({ style { padding(12.px); property("border-bottom", "1px solid #f1f5f9"); fontSize(12.px); color(Color("#475569")) } }) { Text(item.tipoObligacion.ifBlank { "—" }) }
                                Td({ style { padding(12.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(item.aplica) }
                                Td({ style { padding(12.px); property("border-bottom", "1px solid #f1f5f9") } }) { LegalStatus(item.estado) }
                                Td({ style { padding(12.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(item.fechaVigencia.ifBlank { "Sin fecha" }) }
                                Td({ style { padding(12.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(item.responsable.ifBlank { "Sin asignar" }) }
                                Td({ style { padding(12.px); property("border-bottom", "1px solid #f1f5f9") } }) {
                                    if (item.esCritico) Span({ style { color(Color("#dc2626")); fontWeight("700") } }) { Text("⚠ Sí") } else Text("—")
                                }
                                Td({ style { padding(12.px); property("border-bottom", "1px solid #f1f5f9") } }) {
                                    if (item.nDocumentos > 0) Span({ style { backgroundColor(Color("#eff6ff")); color(Color("#1d4ed8")); padding(2.px, 8.px); borderRadius(10.px); fontSize(12.px); fontWeight("600") } }) { Text("${item.nDocumentos}") } else Text("—")
                                }
                                Td({ style { padding(12.px); whiteSpace("nowrap"); property("border-bottom", "1px solid #f1f5f9") } }) {
                                    Button({ onClick { editing = item } }) { Text("Evaluar") }
                                    Button({ onClick { fichaId = item.id }; style { marginLeft(6.px) } }) { Text("Ficha") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ================== ALTA DE OBLIGACIÓN (estatal/municipal/permisos) ==================
@Composable
private fun LegalNuevaObligacionForm(onCancel: () -> Unit, onSave: (LegalMatrixItem) -> Unit) {
    var clave by remember { mutableStateOf("") }
    var titulo by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf("Estatal") }
    var subCategoria by remember { mutableStateOf("") }
    var tipoObligacion by remember { mutableStateOf("Permiso") }
    var autoridad by remember { mutableStateOf("") }
    var responsableEmail by remember { mutableStateOf("") }
    var vigencia by remember { mutableStateOf("") }
    var esCritico by remember { mutableStateOf("No") }
    var urlNorma by remember { mutableStateOf("") }
    var problem by remember { mutableStateOf("") }
    Div({ style { padding(18.px); marginBottom(18.px); backgroundColor(Color.white); borderRadius(10.px); property("border", "1px solid #e2e8f0") } }) {
        H3 { Text("Agregar obligación") }
        P({ style { color(Color("#475569")); fontSize(13.px) } }) { Text("Para legislación estatal o municipal, permisos locales y requisitos propios del centro de trabajo. La clave debe ser única (p. ej. LIC-AMB-NL-2026).") }
        Div({ style { display(DisplayStyle.Flex); flexWrap(FlexWrap.Wrap); gap(8.px) } }) {
            Input(InputType.Text) { placeholder("Clave única *"); value(clave); onInput { clave = it.value }; style { padding(9.px); property("min-width", "180px") } }
            Input(InputType.Text) { placeholder("Título de la obligación *"); value(titulo); onInput { titulo = it.value }; style { padding(9.px); property("min-width", "320px") } }
            Select({ onChange { categoria = it.value ?: "Estatal" }; style { padding(9.px) } }) {
                listOf("Estatal", "Municipal", "STPS", "SEMARNAT", "PROFEPA", "ProteccionCivil").forEach { c -> Option(c, { if (c == categoria) selected() }) { Text(c) } }
            }
            Select({ onChange { tipoObligacion = it.value ?: "Permiso" }; style { padding(9.px) } }) {
                listOf("Permiso", "Registro", "Dictamen", "Manifiesto", "Informe", "Programa", "Cumplimiento continuo").forEach { c -> Option(c, { if (c == tipoObligacion) selected() }) { Text(c) } }
            }
            Input(InputType.Text) { placeholder("Subcategoría (p. ej. Licencias, Residuos)"); value(subCategoria); onInput { subCategoria = it.value }; style { padding(9.px) } }
        }
        Div({ style { display(DisplayStyle.Flex); flexWrap(FlexWrap.Wrap); gap(8.px); marginTop(8.px) } }) {
            Input(InputType.Text) { placeholder("Autoridad emisora (p. ej. Secretaría de Medio Ambiente del Estado)"); value(autoridad); onInput { autoridad = it.value }; style { padding(9.px); property("min-width", "300px") } }
            Input(InputType.Text) { placeholder("Correo del responsable (recordatorios)"); value(responsableEmail); onInput { responsableEmail = it.value }; style { padding(9.px) } }
            Input(InputType.Text) { placeholder("Vencimiento AAAA-MM-DD"); value(vigencia); onInput { vigencia = it.value }; style { padding(9.px) } }
            Select({ onChange { esCritico = it.value ?: "No" }; style { padding(9.px) } }) {
                listOf("No", "Sí").forEach { c -> Option(c, { if (c == esCritico) selected() }) { Text(c) } }
            }
            Input(InputType.Text) { placeholder("URL al texto oficial (https://...)"); value(urlNorma); onInput { urlNorma = it.value }; style { padding(9.px); property("min-width", "260px") } }
        }
        if (problem.isNotBlank()) P({ style { color(Color("#b91c1c")) } }) { Text(problem) }
        Div({ style { marginTop(10.px) } }) {
            Button({
                onClick {
                    problem = when {
                        clave.isBlank() || titulo.isBlank() -> "La clave y el título son obligatorios."
                        vigencia.isNotBlank() && !vigencia.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) -> "La vigencia debe tener formato AAAA-MM-DD."
                        else -> ""
                    }
                    if (problem.isBlank()) onSave(
                        LegalMatrixItem(
                            clave = clave.trim(), titulo = titulo.trim(), categoria = categoria,
                            aplica = "Pendiente", fechaVigencia = vigencia.trim(), esCritico = esCritico == "Sí",
                            urlNorma = urlNorma.trim(), subCategoria = subCategoria.trim(),
                            tipoObligacion = tipoObligacion, autoridad = autoridad.trim(),
                            responsableEmail = responsableEmail.trim()
                        )
                    )
                }; style { padding(9.px) } }) { Text("Guardar obligación") }
            Button({ onClick { onCancel() }; style { padding(9.px); marginLeft(8.px) } }) { Text("Cancelar") }
        }
    }
}

@Composable
private fun LegalStat(label: String, value: String, accent: String) {
    Div({ style { padding(18.px); backgroundColor(Color.white); borderRadius(12.px); property("border", "1px solid #e2e8f0"); property("border-top", "3px solid $accent") } }) {
        P({ style { margin(0.px); color(Color("#64748b")); fontSize(12.px) } }) { Text(label.uppercase()) }
        H2({ style { marginTop(8.px); marginBottom(0.px); color(Color("#0f172a")) } }) { Text(value) }
    }
}

@Composable
private fun LegalStatus(status: String) {
    val color = when (status) { "Vigente" -> "#16a34a"; "PorVencer" -> "#d97706"; "Vencido" -> "#dc2626"; else -> "#64748b" }
    Span({ style { color(Color(color)); fontWeight("600") } }) { Text(status.ifBlank { "Pendiente" }) }
}

/** Revisión humana asistida: guarda el criterio, nunca declara cumplimiento legal. */
@Composable
private fun LegalApplicabilityEditor(item: LegalMatrixItem, onCancel: () -> Unit, onSave: (LegalMatrixItem) -> Unit) {
    var applies by remember(item.id) { mutableStateOf(item.aplica) }
    var reason by remember(item.id) { mutableStateOf(item.justificacion) }
    var owner by remember(item.id) { mutableStateOf(item.responsable) }
    var expiry by remember(item.id) { mutableStateOf(item.fechaVigencia) }
    var url by remember(item.id) { mutableStateOf(item.documentoUrl) }
    var critico by remember(item.id) { mutableStateOf(if (item.esCritico) "Sí" else "No") }
    var urlNorma by remember(item.id) { mutableStateOf(item.urlNorma) }
    var subCat by remember(item.id) { mutableStateOf(item.subCategoria) }
    var tipoOb by remember(item.id) { mutableStateOf(item.tipoObligacion.ifBlank { "Cumplimiento continuo" }) }
    var autoridadTxt by remember(item.id) { mutableStateOf(item.autoridad) }
    var email by remember(item.id) { mutableStateOf(item.responsableEmail) }
    var problem by remember(item.id) { mutableStateOf("") }
    Div({ style { padding(18.px); marginBottom(18.px); backgroundColor(Color.white); borderRadius(10.px); property("border", "1px solid #e2e8f0") } }) {
        H3 { Text("Evaluar ${item.clave}: ${item.titulo}") }
        P({ style { color(Color("#475569")) } }) { Text("Anota la razón y responsable de la decisión. Pendiente no significa que la norma no aplique; la vigencia se refiere a tu evidencia, no a la vigencia legal de la norma.") }
        Div({ style { display(DisplayStyle.Flex); flexWrap(FlexWrap.Wrap); gap(8.px); alignItems(AlignItems.Center) } }) {
            Select({ onChange { applies = it.value ?: "Pendiente" }; style { padding(9.px) } }) {
                listOf("Pendiente", "Si", "No").forEach { choice -> Option(choice, { if (choice == applies) selected() }) { Text(choice) } }
            }
            Input(InputType.Text) { placeholder("Justificación de aplicabilidad"); value(reason); onInput { reason = it.value }; style { padding(9.px); property("min-width", "240px") } }
            Input(InputType.Text) { placeholder("Responsable de revisión"); value(owner); onInput { owner = it.value }; style { padding(9.px) } }
            Input(InputType.Text) { placeholder("Vigencia evidencia AAAA-MM-DD"); value(expiry); onInput { expiry = it.value }; style { padding(9.px) } }
        }
        Div({ style { display(DisplayStyle.Flex); flexWrap(FlexWrap.Wrap); gap(8.px); alignItems(AlignItems.Center); marginTop(8.px) } }) {
            Input(InputType.Text) { placeholder("Subcategoría"); value(subCat); onInput { subCat = it.value }; style { padding(9.px) } }
            Select({ onChange { tipoOb = it.value ?: "" }; style { padding(9.px) } }) {
                listOf("Cumplimiento continuo", "Permiso", "Registro", "Dictamen", "Manifiesto", "Informe", "Programa").forEach { c -> Option(c, { if (c == tipoOb) selected() }) { Text(c) } }
            }
            Input(InputType.Text) { placeholder("Autoridad emisora"); value(autoridadTxt); onInput { autoridadTxt = it.value }; style { padding(9.px); property("min-width", "220px") } }
            Input(InputType.Text) { placeholder("Correo del responsable (recordatorios)"); value(email); onInput { email = it.value }; style { padding(9.px); property("min-width", "200px") } }
        }
        Div({ style { marginTop(10.px); display(DisplayStyle.Flex); alignItems(AlignItems.Center); gap(8.px); flexWrap(FlexWrap.Wrap) } }) {
            Text("Permiso crítico:")
            Select({ onChange { critico = it.value ?: "No" }; style { padding(9.px) } }) {
                listOf("No", "Sí").forEach { c -> Option(c, { if (c == critico) selected() }) { Text(c) } }
            }
            Span({ style { fontSize(12.px); color(Color("#64748b")) } }) { Text("Marca si su vencimiento implica riesgo de clausura o multa (licencias, dictámenes). El correo recibe los recordatorios de vencimiento.") }
            Input(InputType.Text) { placeholder("URL HTTPS evidencia"); value(url); onInput { url = it.value }; style { padding(9.px); property("min-width", "200px") } }
        }
        Input(InputType.Text) { placeholder("URL al texto oficial de la norma (https://dof.gob.mx/...)"); value(urlNorma); onInput { urlNorma = it.value }; style { padding(9.px); marginTop(8.px); width(100.percent) } }
        if (problem.isNotBlank()) P({ style { color(Color("#b91c1c")) } }) { Text(problem) }
        Div({ style { marginTop(10.px) } }) {
            Button({
                onClick {
                    problem = if (applies != "Pendiente" && (reason.isBlank() || owner.isBlank())) "La decisión requiere justificación y responsable." else ""
                    if (problem.isBlank()) onSave(item.copy(
                        aplica = applies, justificacion = reason.trim(), responsable = owner.trim(),
                        fechaVigencia = expiry.trim(), documentoUrl = url.trim(), esCritico = critico == "Sí",
                        urlNorma = urlNorma.trim(), subCategoria = subCat.trim(), tipoObligacion = tipoOb.trim(),
                        autoridad = autoridadTxt.trim(), responsableEmail = email.trim()
                    ))
                }; style { padding(9.px) } }) { Text("Guardar evaluación") }
            Button({ onClick { onCancel() }; style { padding(9.px); marginLeft(8.px) } }) { Text("Cancelar") }
        }
    }
}

/**
 * FICHA de obligación (estilo EHSoft): marco legal por artículo, documentos
 * de cumplimiento con vigencia y recordatorio propios, y tareas vinculadas.
 */
@Composable
private fun LegalObligationDetail(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope, id: Int, onClose: () -> Unit, onChanged: () -> Unit) {
    var detalle by remember(id) { mutableStateOf<LegalMatrixDetalle?>(null) }
    var errorLocal by remember(id) { mutableStateOf("") }
    var busy by remember(id) { mutableStateOf(false) }

    fun load() {
        scope.launch {
            errorLocal = ""
            try {
                detalle = client.get("$BACKEND_URL/api/v1/ehs/matriz-legal/$id/detalle").body<LegalMatrixDetalle>()
            } catch (e: Exception) { errorLocal = "No se pudo cargar la ficha: ${e.message ?: "error"}" }
        }
    }
    LaunchedEffect(id) { load() }

    // ---- formularios ----
    var refNivel by remember(id) { mutableStateOf("Estatal") }
    var refReferencia by remember(id) { mutableStateOf("") }
    var refNombre by remember(id) { mutableStateOf("") }
    var refUrl by remember(id) { mutableStateOf("") }
    var docTipo by remember(id) { mutableStateOf("Dictamen") }
    var docExpedicion by remember(id) { mutableStateOf("") }
    var docVigencia by remember(id) { mutableStateOf("") }
    var docRecordatorio by remember(id) { mutableStateOf("30") }
    var docComentario by remember(id) { mutableStateOf("") }
    var docFileChosen by remember(id) { mutableStateOf<File?>(null) }
    var tareaTitulo by remember(id) { mutableStateOf("") }
    var tareaResponsable by remember(id) { mutableStateOf("") }
    var tareaFecha by remember(id) { mutableStateOf("") }

    val d = detalle
    Div({ style { padding(18.px); marginBottom(18.px); backgroundColor(Color.white); borderRadius(10.px); property("border", "1px solid #bfdbfe") } }) {
        Div({ style { display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); alignItems(AlignItems.FlexStart) } }) {
            Div {
                H3({ style { margin(0.px) } }) { Text("${d?.obligacion?.clave ?: "…"}: ${d?.obligacion?.titulo ?: ""}") }
                if (d != null) P({ style { color(Color("#64748b")); fontSize(13.px); marginTop(4.px) } }) {
                    Text("Autoridad: ${d.obligacion.autoridad.ifBlank { "—" }} · Tipo: ${d.obligacion.tipoObligacion.ifBlank { "—" }} · Estado: ${d.obligacion.estado.ifBlank { "Pendiente" }}")
                }
            }
            Button({ onClick { onClose() }; style { padding(6.px, 12.px) } }) { Text("Cerrar ficha") }
        }

        if (errorLocal.isNotBlank()) P({ style { color(Color("#b91c1c")) } }) { Text(errorLocal) }

        d?.let { det ->
            // ================= MARCO LEGAL =================
            H4({ style { marginTop(16.px); marginBottom(8.px); color(Color("#1e3a8a")) } }) { Text("Marco legal (fundamento por artículo)") }
            if (det.referencias.isEmpty()) P({ style { fontSize(13.px); color(Color("#94a3b8")); margin(0.px) } }) { Text("Sin referencias. Agrega el artículo de la ley federal, estatal o municipal que fundamenta esta obligación.") }
            det.referencias.forEach { ref ->
                Div({ style { display(DisplayStyle.Flex); gap(8.px); alignItems(AlignItems.Center); fontSize(13.px); padding(6.px, 0.px); property("border-bottom", "1px solid #f1f5f9") } }) {
                    Span({ style { backgroundColor(Color(if (ref.nivel == "Federal") "#dbeafe" else if (ref.nivel == "Estatal") "#dcfce7" else "#fef9c3")); padding(2.px, 8.px); borderRadius(10.px); fontSize(11.px); fontWeight("600") } }) { Text(ref.nivel) }
                    B { Text(ref.nombreLey) }
                    if (ref.referencia.isNotBlank()) Text("· ${ref.referencia}")
                    if (ref.url.isNotBlank()) A(href = ref.url, attrs = { target(ATarget.Blank); style { color(Color("#2563eb")); fontSize(12.px); textDecoration("underline") } }) { Text("Ver texto") }
                    Div({ style { flex(1) } })
                    Button({
                        style { padding(2.px, 8.px); fontSize(11.px); backgroundColor(Color("#ef4444")); color(Color.white); property("border", "none"); borderRadius(4.px); cursor("pointer") }
                        onClick { scope.launch { client.delete("$BACKEND_URL/api/v1/ehs/matriz-legal/$id/referencias/${ref.id}"); load(); onChanged() } }
                    }) { Text("Quitar") }
                }
            }
            Div({ style { display(DisplayStyle.Flex); gap(6.px); flexWrap(FlexWrap.Wrap); marginTop(8.px) } }) {
                Select({ onChange { refNivel = it.value ?: "Estatal" }; style { padding(8.px) } }) {
                    listOf("Federal", "Estatal", "Municipal").forEach { n -> Option(n, { if (n == refNivel) selected() }) { Text(n) } }
                }
                Input(InputType.Text) { placeholder("Ley o reglamento * (p. ej. Ley Ambiental del Estado)"); value(refNombre); onInput { refNombre = it.value }; style { padding(8.px); property("min-width", "240px") } }
                Input(InputType.Text) { placeholder("Artículo/fracción (p. ej. Art. 37 Fracc. II)"); value(refReferencia); onInput { refReferencia = it.value }; style { padding(8.px); property("min-width", "180px") } }
                Input(InputType.Text) { placeholder("URL al texto (opcional)"); value(refUrl); onInput { refUrl = it.value }; style { padding(8.px); property("min-width", "160px") } }
                Button({
                    style { padding(8.px, 14.px); backgroundColor(Color("#1e40af")); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") }
                    onClick {
                        if (refNombre.isBlank()) window.alert("Indica la ley o reglamento.")
                        else scope.launch {
                            try {
                                val resp = client.post("$BACKEND_URL/api/v1/ehs/matriz-legal/$id/referencias") {
                                    contentType(ContentType.Application.Json)
                                    setBody(LegalMatrixRef(nivel = refNivel, referencia = refReferencia.trim(), nombreLey = refNombre.trim(), url = refUrl.trim()))
                                }
                                if (!resp.status.isSuccess()) {
                                    val msg = runCatching { resp.body<Map<String, String>>()["error"] }.getOrNull()
                                    window.alert("No se agregó la referencia: ${msg ?: "error ${resp.status.value}"}")
                                } else { refReferencia = ""; refNombre = ""; refUrl = ""; load() }
                            } catch (e: Exception) { window.alert("Error: ${e.message}") }
                        }
                    }
                }) { Text("+ Referencia") }
            }

            // ================= DOCUMENTOS =================
            H4({ style { marginTop(20.px); marginBottom(8.px); color(Color("#166534")) } }) { Text("Documentos de cumplimiento (${det.documentos.size})") }
            if (det.documentos.isEmpty()) P({ style { fontSize(13.px); color(Color("#94a3b8")); margin(0.px) } }) { Text("Sin documentos aún. Puedes subir varios (dictamen vigente, historial de años anteriores, etc.); cada uno con su propia vigencia y recordatorio.") }
            det.documentos.forEach { doc ->
                Div({ style { fontSize(13.px); padding(8.px, 0.px); property("border-bottom", "1px solid #f1f5f9") } }) {
                    Div({ style { display(DisplayStyle.Flex); gap(8.px); alignItems(AlignItems.Center); flexWrap(FlexWrap.Wrap) } }) {
                        B { Text(doc.nombre) }
                        if (doc.tipoDocumento.isNotBlank()) Span({ style { backgroundColor(Color("#eff6ff")); color(Color("#1d4ed8")); padding(2.px, 8.px); borderRadius(10.px); fontSize(11.px) } }) { Text(doc.tipoDocumento) }
                        if (doc.fechaExpedicion.isNotBlank()) Span { Text("expedido ${doc.fechaExpedicion}") }
                        if (doc.fechaVigencia.isNotBlank()) Span({
                            style { fontWeight("700"); color(Color(if (doc.fechaVigencia < hoyIso()) "#dc2626" else "#16a34a")) }
                        }) { Text("vence ${doc.fechaVigencia}") }
                        Span({ style { color(Color("#64748b")); fontSize(12.px) } }) { Text("recordatorio ${doc.recordatorioDias} días antes") }
                        Div({ style { flex(1) } })
                        if (doc.documentId > 0) Button({
                            style { padding(2.px, 8.px); fontSize(11.px); backgroundColor(Color("#2563eb")); color(Color.white); property("border", "none"); borderRadius(4.px); cursor("pointer") }
                            onClick {
                                val urlD = "$BACKEND_URL/api/v1/ehs/documentos/${doc.documentId}/descargar"
                                val options = js("({})")
                                options.method = "GET"
                                options.headers = js("({})")
                                options.headers.Authorization = "Bearer $apiAuthToken"
                                window.asDynamic().fetch(urlD, options)
                                    .then { response: dynamic -> if (!response.ok) throw Exception("HTTP " + response.status) else response.blob() }
                                    .then { blob: dynamic ->
                                        val blobUrl = window.asDynamic().URL.createObjectURL(blob)
                                        window.open(blobUrl as String, "_blank")
                                        window.setTimeout({ window.asDynamic().URL.revokeObjectURL(blobUrl) }, 60000)
                                    }
                                    .`catch` { err: dynamic -> window.alert("No se pudo abrir el documento: " + (err.message ?: "error")) }
                            }
                        }) { Text("Ver") }
                        Button({
                            style { padding(2.px, 8.px); fontSize(11.px); backgroundColor(Color("#ef4444")); color(Color.white); property("border", "none"); borderRadius(4.px); cursor("pointer") }
                            onClick { scope.launch { client.delete("$BACKEND_URL/api/v1/ehs/matriz-legal/$id/documentos/${doc.id}"); load(); onChanged() } }
                        }) { Text("Quitar") }
                    }
                    if (doc.comentario.isNotBlank()) Div({ style { color(Color("#64748b")); fontSize(12.px) } }) { Text(doc.comentario) }
                }
            }
            Div({ style { property("border", "1px dashed #cbd5e1"); borderRadius(8.px); padding(12.px); marginTop(10.px) } }) {
                P({ style { margin(0.px, 0.px, 8.px, 0.px); fontSize(12.px); color(Color("#475569")) } }) { Text("Subir nuevo documento de cumplimiento (se guarda en Drive como evidencia de la categoría MatrizLegal):") }
                Div({ style { display(DisplayStyle.Flex); gap(6.px); flexWrap(FlexWrap.Wrap); alignItems(AlignItems.Center) } }) {
                    Input(InputType.File) {
                        id("matriz-doc-file-$id")
                        style { fontSize(13.px) }
                        onChange {
                            val files = (document.getElementById("matriz-doc-file-$id") as? HTMLInputElement)?.files
                            docFileChosen = if (files == null || files.length == 0) null else files.item(0)
                        }
                    }
                    Select({ onChange { docTipo = it.value ?: "Dictamen" }; style { padding(8.px) } }) {
                        listOf("Dictamen", "Permiso", "Estudio", "Registro", "Manifiesto", "Informe", "Otro").forEach { t -> Option(t, { if (t == docTipo) selected() }) { Text(t) } }
                    }
                    Input(InputType.Text) { placeholder("Expedición AAAA-MM-DD"); value(docExpedicion); onInput { docExpedicion = it.value }; style { padding(8.px); width(150.px) } }
                    Input(InputType.Text) { placeholder("Vence AAAA-MM-DD"); value(docVigencia); onInput { docVigencia = it.value }; style { padding(8.px); width(150.px) } }
                    Input(InputType.Text) { placeholder("Avisar N días antes"); value(docRecordatorio); onInput { docRecordatorio = it.value }; style { padding(8.px); width(150.px) } }
                    Input(InputType.Text) { placeholder("Comentario (opcional)"); value(docComentario); onInput { docComentario = it.value }; style { padding(8.px); property("min-width", "180px") } }
                    Button({
                        style { padding(8.px, 14.px); backgroundColor(Color("#166534")); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") }
                        onClick {
                            val file = docFileChosen
                            if (file == null) { window.alert("Selecciona el archivo primero."); return@onClick }
                            if (docVigencia.isNotBlank() && !docVigencia.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) { window.alert("La vigencia debe tener formato AAAA-MM-DD."); return@onClick }
                            if (docExpedicion.isNotBlank() && !docExpedicion.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) { window.alert("La expedición debe tener formato AAAA-MM-DD."); return@onClick }
                            if (busy) return@onClick
                            busy = true
                            scope.launch {
                                try {
                                    // 1) subir el archivo a la evidencia documental (Drive)
                                    val documentId = uploadEvidenceFile(file, mapOf(
                                        "categoria" to "MatrizLegal",
                                        "titulo" to file.name,
                                        "anio" to "-1",
                                        "moduleType" to "matriz_legal",
                                        "moduleRecordId" to id.toString()
                                    ))
                                    // 2) vincularlo a la obligación con sus metadatos normativos
                                    val resp = client.post("$BACKEND_URL/api/v1/ehs/matriz-legal/$id/documentos") {
                                        contentType(ContentType.Application.Json)
                                        setBody(LegalMatrixDoc(
                                            documentId = documentId.toInt(),
                                            nombre = file.name.take(300),
                                            tipoDocumento = docTipo,
                                            fechaExpedicion = docExpedicion.trim(),
                                            fechaVigencia = docVigencia.trim(),
                                            recordatorioDias = docRecordatorio.toIntOrNull() ?: 30,
                                            comentario = docComentario.trim()
                                        ))
                                    }
                                    if (!resp.status.isSuccess()) {
                                        val msg = runCatching { resp.body<Map<String, String>>()["error"] }.getOrNull()
                                        window.alert("El archivo se subió pero no se vinculó: ${msg ?: "error ${resp.status.value}"}")
                                    } else {
                                        docVigencia = ""; docExpedicion = ""; docComentario = ""
                                        docFileChosen = null
                                        load(); onChanged()
                                    }
                                } catch (e: Exception) {
                                    window.alert("No se pudo subir el documento: ${e.message ?: "error"}")
                                } finally { busy = false }
                            }
                        }
                    }) { Text(if (busy) "Subiendo…" else "↑ Subir documento") }
                }
                P({ style { margin(6.px, 0.px, 0.px, 0.px); fontSize(11.px); color(Color("#94a3b8")) } }) {
                    Text("La vigencia del documento se copia a la obligación si esta no tiene fecha asignada. El recordatorio llega por correo al responsable cuando el SMTP esté configurado.")
                }
            }

            // ================= TAREAS VINCULADAS =================
            H4({ style { marginTop(20.px); marginBottom(8.px); color(Color("#9a3412")) } }) { Text("Tareas y acciones correctivas (${det.tareas.size})") }
            if (det.tareas.isEmpty()) P({ style { fontSize(13.px); color(Color("#94a3b8")); margin(0.px) } }) { Text("Sin tareas vinculadas. Úsalas para dar seguimiento a trámites o renovaciones de esta obligación; aparecen también en el calendario y el módulo de acciones.") }
            det.tareas.forEach { t ->
                Div({ style { display(DisplayStyle.Flex); gap(8.px); alignItems(AlignItems.Center); fontSize(13.px); padding(6.px, 0.px); property("border-bottom", "1px solid #f1f5f9") } }) {
                    Span({ style { fontWeight("600") } }) { Text(t.titulo) }
                    Span { Text("· ${t.responsable}") }
                    Span { Text("· límite ${t.fechaLimite}") }
                    Span({ style { backgroundColor(Color(if (t.estado == "Cerrada") "#dcfce7" else "#ffedd5")); padding(2.px, 8.px); borderRadius(10.px); fontSize(11.px) } }) { Text(t.estado) }
                    Span({ style { fontSize(11.px); color(Color("#64748b")) } }) { Text(t.prioridad) }
                }
            }
            Div({ style { display(DisplayStyle.Flex); gap(6.px); flexWrap(FlexWrap.Wrap); marginTop(8.px); alignItems(AlignItems.Center) } }) {
                Input(InputType.Text) { placeholder("Título de la tarea *"); value(tareaTitulo); onInput { tareaTitulo = it.value }; style { padding(8.px); property("min-width", "220px") } }
                Input(InputType.Text) { placeholder("Responsable *"); value(tareaResponsable); onInput { tareaResponsable = it.value }; style { padding(8.px); property("min-width", "160px") } }
                Input(InputType.Text) { placeholder("Fecha límite AAAA-MM-DD *"); value(tareaFecha); onInput { tareaFecha = it.value }; style { padding(8.px); width(170.px) } }
                Button({
                    style { padding(8.px, 14.px); backgroundColor(Color("#9a3412")); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") }
                    onClick {
                        if (tareaTitulo.isBlank() || tareaResponsable.isBlank() || !tareaFecha.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                            window.alert("La tarea requiere título, responsable y fecha límite (AAAA-MM-DD).")
                        } else scope.launch {
                            try {
                                val resp = client.post("$BACKEND_URL/api/v1/ehs/acciones") {
                                    contentType(ContentType.Application.Json)
                                    setBody(NuevaTarea(
                                        titulo = tareaTitulo.trim(), responsable = tareaResponsable.trim(),
                                        fechaLimite = tareaFecha.trim(), origenTipo = "matriz_legal", origenId = id,
                                        descripcion = "Vinculada a la obligación ${det.obligacion.clave}"
                                    ))
                                }
                                if (!resp.status.isSuccess()) {
                                    val msg = runCatching { resp.body<Map<String, String>>()["error"] }.getOrNull()
                                    window.alert("No se creó la tarea: ${msg ?: "error ${resp.status.value}"}")
                                } else { tareaTitulo = ""; tareaFecha = ""; load(); onChanged() }
                            } catch (e: Exception) { window.alert("Error: ${e.message}") }
                        }
                    }
                }) { Text("+ Tarea") }
            }
        }
    }
}

private fun hoyIso(): String = js("new Date().toISOString().substring(0,10)")
