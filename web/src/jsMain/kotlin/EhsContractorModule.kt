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
private data class ContractorView(
    val id: Int = 0,
    val empresa: String,
    val actividad: String,
    val centroTrabajo: String,
    val responsableInterno: String,
    val documentoUrl: String = "",
    val vigenciaDocumento: String = "",
    val estado: String = "Pendiente",
    val notas: String = "",
    val estadoDocumental: String = "SinDocumento"
)

@Composable
fun EhsContractorModule(client: HttpClient, scope: CoroutineScope) {
    var contractors by remember { mutableStateOf(emptyList<ContractorView>()) }
    var company by remember { mutableStateOf("") }
    var activity by remember { mutableStateOf("") }
    var site by remember { mutableStateOf("") }
    var owner by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    fun refresh() {
        scope.launch {
            try { contractors = client.get("$BACKEND_URL/api/v1/ehs/contratistas").body() }
            catch (e: Exception) { error = "No se pudieron cargar contratistas: ${e.message ?: "error"}" }
        }
    }
    LaunchedEffect(Unit) { refresh() }
    Div {
        H1({ style { color(Color("#0f172a")) } }) { Text("Contratistas y proveedores") }
        P({ style { color(Color("#475569")) } }) { Text("Expediente por empresa y centro de trabajo. Aprobar un expediente no concede acceso físico ni sustituye la validación de seguridad.") }
        if (error.isNotBlank()) P({ style { color(Color("#b91c1c")) } }) { Text(error) }
        if (notice.isNotBlank()) P { Text(notice) }
        Div({ style { display(DisplayStyle.Flex); flexWrap(FlexWrap.Wrap); gap(10.px); padding(18.px); backgroundColor(Color.white); borderRadius(10.px); property("border", "1px solid #e2e8f0") } }) {
            Input(InputType.Text) { placeholder("Empresa *"); value(company); onInput { company = it.value }; style { padding(9.px) } }
            Input(InputType.Text) { placeholder("Actividad *"); value(activity); onInput { activity = it.value }; style { padding(9.px) } }
            Input(InputType.Text) { placeholder("Centro de trabajo *"); value(site); onInput { site = it.value }; style { padding(9.px) } }
            Input(InputType.Text) { placeholder("Responsable interno *"); value(owner); onInput { owner = it.value }; style { padding(9.px) } }
            Button({
                onClick {
                    if (saving) return@onClick
                    error = ""; notice = ""
                    if (company.isBlank() || activity.isBlank() || site.isBlank() || owner.isBlank()) error = "Completa empresa, actividad, centro y responsable."
                    else {
                        saving = true
                        scope.launch {
                            try {
                                val r = client.post("$BACKEND_URL/api/v1/ehs/contratistas") {
                                    contentType(ContentType.Application.Json)
                                    setBody(ContractorView(empresa = company.trim(), actividad = activity.trim(), centroTrabajo = site.trim(), responsableInterno = owner.trim()))
                                }
                                if (!r.status.isSuccess()) error = "No se creó el expediente (${r.status.value})."
                                else { company = ""; activity = ""; site = ""; owner = ""; notice = "Expediente creado, pendiente de revisión."; refresh() }
                            } catch (e: Exception) { error = "No se pudo guardar: ${e.message ?: "error"}" }
                            finally { saving = false }
                        }
                    }
                }
                style { padding(9.px, 18.px); backgroundColor(Color("#2563eb")); color(Color.white); border(0.px); borderRadius(8.px); cursor("pointer") }
            }) { Text("Crear expediente") }
        }
        if (contractors.isEmpty()) P { Text("No hay empresas registradas.") }
        contractors.forEach { item ->
            ContractorCard(item) { changed ->
                scope.launch {
                    error = ""; notice = ""
                    try {
                        val r = client.put("$BACKEND_URL/api/v1/ehs/contratistas/${item.id}") {
                            contentType(ContentType.Application.Json); setBody(changed)
                        }
                        if (!r.status.isSuccess()) error = "No se actualizó #${item.id} (${r.status.value}). Para aprobar se requiere un documento HTTPS vigente."
                        else { notice = "Expediente #${item.id} actualizado."; refresh() }
                    } catch (e: Exception) { error = "Error al actualizar #${item.id}: ${e.message ?: "error"}" }
                }
            }
        }
    }
}

@Composable
private fun ContractorCard(item: ContractorView, onSave: (ContractorView) -> Unit) {
    var documentUrl by remember(item.id, item.documentoUrl) { mutableStateOf(item.documentoUrl) }
    var expiry by remember(item.id, item.vigenciaDocumento) { mutableStateOf(item.vigenciaDocumento) }
    var status by remember(item.id, item.estado) { mutableStateOf(item.estado) }
    Div({ style { marginTop(12.px); padding(18.px); backgroundColor(Color.white); borderRadius(10.px); property("border", "1px solid #e2e8f0") } }) {
        H3 { Text("#${item.id} ${item.empresa}") }
        P { Text("${item.actividad} | ${item.centroTrabajo} | Responsable: ${item.responsableInterno} | Expediente: ${item.estado} | Documento: ${item.estadoDocumental}") }
        Input(InputType.Text) { placeholder("URL HTTPS del documento"); value(documentUrl); onInput { documentUrl = it.value }; style { padding(9.px); property("min-width", "260px") } }
        Input(InputType.Text) { placeholder("Vigencia AAAA-MM-DD"); value(expiry); onInput { expiry = it.value }; style { padding(9.px); marginLeft(8.px) } }
        Select({ onChange { status = it.value ?: "Pendiente" }; style { padding(9.px); marginLeft(8.px) } }) {
            listOf("Pendiente", "Aprobado", "Suspendido").forEach { Option(it, { if (status == it) selected() }) { Text(it) } }
        }
        Button({ onClick { onSave(item.copy(documentoUrl = documentUrl.trim(), vigenciaDocumento = expiry.trim(), estado = status)) }; style { padding(9.px); marginLeft(8.px) } }) { Text("Guardar revisión") }
    }
}
