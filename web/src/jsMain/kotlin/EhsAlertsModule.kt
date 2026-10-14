import androidx.compose.runtime.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.css.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.Serializable

@Serializable
private data class EhsAlertView(
    val tipo: String,
    val origenId: Int,
    val titulo: String,
    val fechaLimite: String,
    val diasRestantes: Long,
    val estado: String,
    val esCritico: Boolean = false
)

@Composable
fun EhsAlertsModule(client: HttpClient) {
    var alerts by remember { mutableStateOf(emptyList<EhsAlertView>()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        try { alerts = client.get("$BACKEND_URL/api/v1/ehs/avisos").body() }
        catch (e: Exception) { error = "No se pudieron cargar avisos: ${e.message ?: "error"}" }
        finally { loading = false }
    }
    Div {
        H1({ style { color(Color("#0f172a")) } }) { Text(I18n.get("alert_titulo")) }
        P({ style { color(Color("#475569")) } }) { Text(I18n.get("alert_intro")) }
        if (error.isNotBlank()) P({ style { color(Color("#b91c1c")) } }) { Text(error) }
        if (loading) P { Text(I18n.get("alert_cargando")) }
        else if (alerts.isEmpty()) P { Text(I18n.get("alert_vacio")) }
        val criticos = alerts.count { it.esCritico }
        if (criticos > 0) P({ style { padding(12.px); backgroundColor(Color("#fef2f2")); property("border", "1px solid #fecaca"); borderRadius(8.px); color(Color("#b91c1c")); fontWeight("600") } }) {
            Text(I18n.get("alert_criticos").replace("{n}", "$criticos"))
        }
        else alerts.forEach { alert ->
            Div({
                style {
                    padding(15.px); marginBottom(10.px); backgroundColor(Color.white)
                    property("border", "1px solid #e2e8f0"); borderRadius(8.px)
                    property("border-left", if (alert.esCritico) "4px solid #dc2626" else if (alert.estado == "Vencido") "4px solid #f59e0b" else "4px solid #94a3b8")
                }
            }) {
                H3 { Text(if (alert.esCritico) "⚠ ${alert.tipo} #${alert.origenId}: ${alert.titulo} (CRÍTICO)" else "${alert.tipo} #${alert.origenId}: ${alert.titulo}") }
                P { Text("${alert.estado} | ${alert.fechaLimite} | ${if (alert.diasRestantes < 0) "${-alert.diasRestantes} días vencido" else "${alert.diasRestantes} días restantes"}") }
            }
        }
    }
}
