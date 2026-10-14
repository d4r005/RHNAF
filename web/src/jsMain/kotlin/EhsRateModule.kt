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
private data class EhsRateView(
    val id: Int = 0,
    val periodo: String,
    val horasTrabajadas: Double,
    val accidentesRegistrables: Int,
    val diasPerdidos: Int,
    val fuenteHoras: String,
    val validadoPor: String,
    val motivoRevision: String = "",
    val version: Int = 0,
    val frecuenciaPorMillon: Double = 0.0,
    val gravedadPorMillon: Double = 0.0
)

@Composable
fun EhsRateModule(client: HttpClient, scope: CoroutineScope) {
    var periods by remember { mutableStateOf(emptyList<EhsRateView>()) }
    var period by remember { mutableStateOf("") }
    var hours by remember { mutableStateOf("") }
    var accidents by remember { mutableStateOf("") }
    var days by remember { mutableStateOf("") }
    var source by remember { mutableStateOf("") }
    var validator by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var showHistory by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    fun refresh() {
        scope.launch {
            try { periods = client.get("$BACKEND_URL/api/v1/ehs/tasas?historial=$showHistory").body() }
            catch (e: Exception) { error = "No se pudieron cargar tasas: ${e.message ?: "error"}" }
        }
    }
    LaunchedEffect(showHistory) { refresh() }
    Div {
        H1({ style { color(Color("#0f172a")) } }) { Text(I18n.get("rate_titulo")) }
        P({ style { color(Color("#475569")) } }) { Text(I18n.get("rate_intro")) }
        if (error.isNotBlank()) P({ style { color(Color("#b91c1c")) } }) { Text(error) }
        if (notice.isNotBlank()) P { Text(notice) }
        Div({ style { display(DisplayStyle.Flex); flexWrap(FlexWrap.Wrap); gap(10.px); padding(18.px); backgroundColor(Color.white); borderRadius(10.px); property("border", "1px solid #e2e8f0") } }) {
            Input(InputType.Text) { placeholder(I18n.get("rate_ph_periodo")); value(period); onInput { period = it.value }; style { padding(9.px) } }
            Input(InputType.Text) { placeholder(I18n.get("rate_ph_horas")); value(hours); onInput { hours = it.value }; style { padding(9.px) } }
            Input(InputType.Text) { placeholder(I18n.get("rate_ph_accidentes")); value(accidents); onInput { accidents = it.value }; style { padding(9.px) } }
            Input(InputType.Text) { placeholder(I18n.get("rate_ph_dias")); value(days); onInput { days = it.value }; style { padding(9.px) } }
            Input(InputType.Text) { placeholder(I18n.get("rate_ph_fuente")); value(source); onInput { source = it.value }; style { padding(9.px) } }
            Input(InputType.Text) { placeholder(I18n.get("rate_ph_validado")); value(validator); onInput { validator = it.value }; style { padding(9.px) } }
            Input(InputType.Text) { placeholder(I18n.get("rate_ph_motivo")); value(reason); onInput { reason = it.value }; style { padding(9.px); property("min-width", "280px") } }
            Button({
                onClick {
                    if (saving) return@onClick
                    error = ""; notice = ""
                    val parsedHours = hours.toDoubleOrNull()
                    val parsedAccidents = accidents.toIntOrNull()
                    val parsedDays = days.toIntOrNull()
                    if (parsedHours == null || parsedHours <= 0 || parsedAccidents == null || parsedAccidents < 0 || parsedDays == null || parsedDays < 0 || source.isBlank() || validator.isBlank())
                        error = "Completa horas positivas, conteos no negativos, fuente y validador."
                    else {
                        saving = true
                        scope.launch {
                            try {
                                val r = client.post("$BACKEND_URL/api/v1/ehs/tasas") {
                                    contentType(ContentType.Application.Json)
                                    setBody(EhsRateView(periodo = period.trim(), horasTrabajadas = parsedHours, accidentesRegistrables = parsedAccidents, diasPerdidos = parsedDays, fuenteHoras = source.trim(), validadoPor = validator.trim(), motivoRevision = reason.trim()))
                                }
                                if (!r.status.isSuccess()) error = "No se guardó (${r.status.value}). Revisa el periodo; para corregir un mes existente escribe el motivo."
                                else { period = ""; hours = ""; accidents = ""; days = ""; source = ""; validator = ""; reason = ""; notice = "Periodo registrado con versión histórica."; refresh() }
                            } catch (e: Exception) { error = "No se pudo guardar: ${e.message ?: "error"}" }
                            finally { saving = false }
                        }
                    }
                }
                style { padding(9.px, 18.px); backgroundColor(Color("#2563eb")); color(Color.white); border(0.px); borderRadius(8.px) }
            }) { Text(I18n.get("rate_guardar")) }
        }
        Button({ onClick { showHistory = !showHistory }; style { padding(9.px); marginTop(12.px) } }) { Text(if (showHistory) "Ver última versión" else "Ver historial de revisiones") }
        if (periods.isEmpty()) P { Text(I18n.get("rate_sin_horas")) }
        periods.forEach { row ->
            Div({ style { marginTop(12.px); padding(16.px); backgroundColor(Color.white); property("border", "1px solid #e2e8f0"); borderRadius(8.px) } }) {
                H3 { Text("${row.periodo} | " + I18n.get("rate_version") + " ${row.version}") }
                P { Text(I18n.get("rate_horas").replace("{h}", "${row.horasTrabajadas}").replace("{a}", "${row.accidentesRegistrables}").replace("{d}", "${row.diasPerdidos}")) }
                P { Text(I18n.get("rate_frecuencia").replace("{f}", "${row.frecuenciaPorMillon}").replace("{g}", "${row.gravedadPorMillon}")) }
                P { Text("Fuente: ${row.fuenteHoras} | Validado por: ${row.validadoPor}${if (row.motivoRevision.isNotBlank()) " | Cambio: ${row.motivoRevision}" else ""}") }
            }
        }
    }
}
