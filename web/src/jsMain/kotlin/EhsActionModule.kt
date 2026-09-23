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
import kotlinx.serialization.Serializable

@Serializable
private data class EhsActionView(
    val id: Int = 0,
    val titulo: String,
    val descripcion: String = "",
    val origenTipo: String = "manual",
    val origenId: Int = 0,
    val responsable: String,
    val fechaLimite: String,
    val prioridad: String = "Media",
    val estado: String = "Abierta",
    val evidenciaUrl: String = "",
    val fechaCierre: String = ""
)

@Composable
fun EhsActionModule(client: HttpClient, scope: CoroutineScope) {
    var actions by remember { mutableStateOf(emptyList<EhsActionView>()) }
    var title by remember { mutableStateOf("") }
    var owner by remember { mutableStateOf("") }
    var due by remember { mutableStateOf("") }
    var origin by remember { mutableStateOf("manual") }
    var originId by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("Media") }
    var error by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    fun refresh() {
        scope.launch {
            try { actions = client.get("$BACKEND_URL/api/v1/ehs/acciones").body() }
            catch (e: Exception) { error = "No se pudieron cargar acciones: ${e.message ?: "error"}" }
        }
    }
    LaunchedEffect(Unit) { refresh() }

    Div {
        H1({ style { color(Color("#0f172a")) } }) { Text("Planes de acción EHS") }
        P({ style { color(Color("#475569")) } }) {
            Text("Da seguimiento a hallazgos, incidentes y obligaciones. El cierre exige una URL HTTPS de evidencia.")
        }
        if (error.isNotBlank()) P({ style { color(Color("#b91c1c")) } }) { Text(error) }
        if (notice.isNotBlank()) P { Text(notice) }
        Div({ style { display(DisplayStyle.Flex); flexWrap(FlexWrap.Wrap); gap(10.px); padding(18.px); backgroundColor(Color.white); borderRadius(10.px); property("border", "1px solid #e2e8f0") } }) {
            Input(InputType.Text) { placeholder("Acción correctiva *"); value(title); onInput { title = it.value }; style { padding(9.px); property("min-width", "230px") } }
            Input(InputType.Text) { placeholder("Responsable *"); value(owner); onInput { owner = it.value }; style { padding(9.px) } }
            Input(InputType.Text) { placeholder("Fecha límite AAAA-MM-DD *"); value(due); onInput { due = it.value }; style { padding(9.px) } }
            Select({ onChange { priority = it.value ?: "Media" }; style { padding(9.px) } }) {
                listOf("Media", "Alta", "Baja").forEach { Option(it) { Text(it) } }
            }
            Select({ onChange { origin = it.value ?: "manual" }; style { padding(9.px) } }) {
                listOf("manual" to "Sin registro origen", "matriz_legal" to "Matriz legal", "inspeccion" to "Inspección", "incidente" to "Incidente").forEach { (key, label) -> Option(key) { Text(label) } }
            }
            if (origin != "manual") Input(InputType.Number) {
                placeholder("ID del origen *"); value(originId); onInput { originId = it.value }; style { padding(9.px) }
            }
            Button({
                style { padding(9.px, 18.px); backgroundColor(Color("#2563eb")); color(Color.white); border(0.px); borderRadius(8.px); cursor("pointer") }
                onClick {
                    if (busy) return@onClick
                    error = ""; notice = ""
                    if (title.isBlank() || owner.isBlank() || due.isBlank() || (origin != "manual" && (originId.toIntOrNull() ?: 0) <= 0)) {
                        error = "Completa acción, responsable, fecha e ID de origen cuando corresponda."
                    } else {
                        busy = true
                        scope.launch {
                            try {
                                val response = client.post("$BACKEND_URL/api/v1/ehs/acciones") {
                                    contentType(ContentType.Application.Json)
                                    setBody(EhsActionView(titulo = title.trim(), responsable = owner.trim(), fechaLimite = due.trim(), prioridad = priority, origenTipo = origin, origenId = if (origin == "manual") 0 else originId.toInt()))
                                }
                                if (!response.status.isSuccess()) error = "No se creó la acción (${response.status.value}). Revisa fecha y origen."
                                else { title = ""; owner = ""; due = ""; originId = ""; notice = "Acción creada."; refresh() }
                            } catch (e: Exception) { error = "No se pudo guardar: ${e.message ?: "error"}" }
                            finally { busy = false }
                        }
                    }
                }
            }) { Text(if (busy) "Guardando..." else "Crear acción") }
        }
        if (actions.isEmpty()) P { Text("No hay planes de acción registrados.") }
        actions.forEach { item ->
            EhsActionCard(item) { changed ->
                scope.launch {
                    error = ""; notice = ""
                    try {
                        val response = client.put("$BACKEND_URL/api/v1/ehs/acciones/${item.id}") {
                            contentType(ContentType.Application.Json); setBody(changed)
                        }
                        if (!response.status.isSuccess()) error = "No se actualizó la acción #${item.id} (${response.status.value}). El cierre necesita evidencia HTTPS."
                        else { notice = "Acción #${item.id} actualizada."; refresh() }
                    } catch (e: Exception) { error = "Error al actualizar #${item.id}: ${e.message ?: "error"}" }
                }
            }
        }
    }
}

@Composable
private fun EhsActionCard(item: EhsActionView, onSave: (EhsActionView) -> Unit) {
    var evidence by remember(item.id) { mutableStateOf(item.evidenciaUrl) }
    Div({ style { marginTop(12.px); padding(18.px); backgroundColor(Color.white); borderRadius(10.px); property("border", "1px solid #e2e8f0") } }) {
        H3 { Text("#${item.id} ${item.titulo}") }
        P { Text("${item.origenTipo}${if (item.origenId > 0) " #${item.origenId}" else ""} | ${item.prioridad} | ${item.estado} | Responsable: ${item.responsable} | Límite: ${item.fechaLimite}") }
        if (item.estado != "Cerrada") {
            Input(InputType.Text) { placeholder("URL HTTPS de evidencia para cerrar"); value(evidence); onInput { evidence = it.value }; style { padding(9.px); property("min-width", "260px") } }
            Button({ onClick { onSave(item.copy(estado = "EnProgreso", evidenciaUrl = evidence)) }; style { marginLeft(8.px); padding(9.px) } }) { Text("En progreso") }
            Button({ onClick { onSave(item.copy(estado = "Cerrada", evidenciaUrl = evidence)) }; style { marginLeft(8.px); padding(9.px) } }) { Text("Cerrar con evidencia") }
        } else P { Text("Cerrada ${item.fechaCierre}. Evidencia: ${item.evidenciaUrl}") }
    }
}
