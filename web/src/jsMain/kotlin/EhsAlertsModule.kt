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
    val estado: String
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
        H1({ style { color(Color("#0f172a")) } }) { Text("Avisos de vencimiento EHS") }
        P({ style { color(Color("#475569")) } }) { Text("Avisos dentro de la aplicación, para registros vencidos o que vencen en 30 días. No se han enviado correos.") }
        if (error.isNotBlank()) P({ style { color(Color("#b91c1c")) } }) { Text(error) }
        if (loading) P { Text("Cargando avisos...") }
        else if (alerts.isEmpty()) P { Text("No hay vencimientos registrados dentro de los próximos 30 días.") }
        else alerts.forEach { alert ->
            Div({ style { padding(15.px); marginBottom(10.px); backgroundColor(Color.white); property("border", "1px solid #e2e8f0"); borderRadius(8.px) } }) {
                H3 { Text("${alert.tipo} #${alert.origenId}: ${alert.titulo}") }
                P { Text("${alert.estado} | ${alert.fechaLimite} | ${if (alert.diasRestantes < 0) "${-alert.diasRestantes} días vencido" else "${alert.diasRestantes} días restantes"}") }
            }
        }
    }
}
