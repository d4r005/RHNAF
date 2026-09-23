import androidx.compose.runtime.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.attributes.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import com.example.rhnaf.shared.model.*

@Serializable
private data class LegalMatrixPage(
    val items: List<LegalMatrixItem> = emptyList(),
    val page: Int = 1,
    val pageSize: Int = 50,
    val total: Long = 0,
    val totalPages: Int = 0
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
        Div({ style { display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); alignItems(AlignItems.Center); marginBottom(24.px) } }) {
            Div {
                H1({ style { margin(0.px); fontSize(28.px); color(Color("#0f172a")) } }) { Text(if (fixedCategory == "STPS") "Normas STPS" else "Matriz Legal EHS") }
                P({ style { marginTop(6.px); color(Color("#64748b")) } }) { Text(if (fixedCategory == "STPS") "Seguimiento de obligaciones STPS aplicables. Valida la aplicabilidad y vigencia con el responsable legal." else "Cumplimiento normativo, vigencias y evidencias documentales") }
            }
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

        if (fixedCategory.isBlank()) Div({ style { display(DisplayStyle.Grid); property("grid-template-columns", "repeat(5, minmax(130px, 1fr))"); gap(14.px); marginBottom(22.px) } }) {
            LegalStat("Cumplimiento", "${summary.porcentajeCumplimiento.toInt()}%", "#2563eb")
            LegalStat("Vigentes", summary.vigentes.toString(), "#16a34a")
            LegalStat("Por vencer", summary.porVencer.toString(), "#d97706")
            LegalStat("Vencidas", summary.vencidos.toString(), "#dc2626")
            LegalStat("Pendientes", summary.pendientes.toString(), "#64748b")
        }

        Div({ style { display(DisplayStyle.Flex); gap(12.px); marginBottom(18.px) } }) {
            if (fixedCategory.isBlank()) Select({
                style { padding(9.px, 12.px); borderRadius(8.px); property("border", "1px solid #cbd5e1") }
                onChange { category = it.value ?: "" }
            }) {
                Option("") { Text("Todas las categorías") }
                listOf("STPS", "SEMARNAT", "PROFEPA", "ProteccionCivil", "Estatal").forEach { Option(it) { Text(it) } }
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

        if (loading) {
            P { Text("Cargando matriz legal...") }
        } else if (items.isEmpty()) {
            Div({ style { padding(38.px); textAlign("center"); backgroundColor(Color.white); borderRadius(12.px); property("border", "1px solid #e2e8f0") } }) {
                H3 { Text(if (fixedCategory == "STPS") "No hay obligaciones STPS registradas" else "La matriz legal está vacía") }
                P({ style { color(Color("#64748b")) } }) { Text("Usa “Cargar catálogo de NOMs” para agregar las obligaciones iniciales.") }
            }
        } else {
            Div({ style { backgroundColor(Color.white); borderRadius(12.px); property("border", "1px solid #e2e8f0"); overflow("auto") } }) {
                Table({ style { width(100.percent); property("border-collapse", "collapse"); fontSize(13.px) } }) {
                    Thead { Tr { listOf("Clave", "Obligación", "Categoría", "Aplica", "Estado", "Vigencia", "Responsable", "Evaluar").forEach { Th({ style { padding(12.px); textAlign("left"); backgroundColor(Color("#f8fafc")); color(Color("#475569")); property("border-bottom", "1px solid #e2e8f0") } }) { Text(it) } } } }
                    Tbody {
                        items.forEach { item ->
                            Tr {
                                Td({ style { padding(12.px); fontWeight("600"); property("border-bottom", "1px solid #f1f5f9") } }) { Text(item.clave) }
                                Td({ style { padding(12.px); property("min-width", "320px"); property("border-bottom", "1px solid #f1f5f9") } }) { Text(item.titulo) }
                                Td({ style { padding(12.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(item.categoria) }
                                Td({ style { padding(12.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(item.aplica) }
                                Td({ style { padding(12.px); property("border-bottom", "1px solid #f1f5f9") } }) { LegalStatus(item.estado) }
                                Td({ style { padding(12.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(item.fechaVigencia.ifBlank { "Sin fecha" }) }
                                Td({ style { padding(12.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(item.responsable.ifBlank { "Sin asignar" }) }
                                Td { Button({ onClick { editing = item } }) { Text("Evaluar") } }
                            }
                        }
                    }
                }
            }
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
    var problem by remember(item.id) { mutableStateOf("") }
    Div({ style { padding(18.px); marginBottom(18.px); backgroundColor(Color.white); borderRadius(10.px); property("border", "1px solid #e2e8f0") } }) {
        H3 { Text("Evaluar ${item.clave}: ${item.titulo}") }
        P({ style { color(Color("#475569")) } }) { Text("Anota la razón y responsable de la decisión. Pendiente no significa que la norma no aplique; la vigencia se refiere a tu evidencia, no a la vigencia legal de la norma.") }
        Select({ onChange { applies = it.value ?: "Pendiente" }; style { padding(9.px) } }) {
            listOf("Pendiente", "Si", "No").forEach { choice -> Option(choice, { if (choice == applies) selected() }) { Text(choice) } }
        }
        Input(InputType.Text) { placeholder("Justificación de aplicabilidad"); value(reason); onInput { reason = it.value }; style { padding(9.px); marginLeft(8.px); property("min-width", "260px") } }
        Input(InputType.Text) { placeholder("Responsable de revisión"); value(owner); onInput { owner = it.value }; style { padding(9.px); marginLeft(8.px) } }
        Input(InputType.Text) { placeholder("Vigencia evidencia AAAA-MM-DD"); value(expiry); onInput { expiry = it.value }; style { padding(9.px); marginTop(8.px) } }
        Input(InputType.Text) { placeholder("URL HTTPS evidencia"); value(url); onInput { url = it.value }; style { padding(9.px); marginLeft(8.px) } }
        if (problem.isNotBlank()) P({ style { color(Color("#b91c1c")) } }) { Text(problem) }
        Div({ style { marginTop(10.px) } }) {
            Button({ onClick {
                problem = if (applies != "Pendiente" && (reason.isBlank() || owner.isBlank())) "La decisión requiere justificación y responsable." else ""
                if (problem.isBlank()) onSave(item.copy(aplica = applies, justificacion = reason.trim(), responsable = owner.trim(), fechaVigencia = expiry.trim(), documentoUrl = url.trim()))
            }; style { padding(9.px) } }) { Text("Guardar evaluación") }
            Button({ onClick { onCancel() }; style { padding(9.px); marginLeft(8.px) } }) { Text("Cancelar") }
        }
    }
}
