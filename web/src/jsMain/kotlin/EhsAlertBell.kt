import androidx.compose.runtime.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.css.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.Serializable

/**
 * Campanita de notificaciones de vencimientos EHS en la barra superior.
 * Muestra cuántos avisos hay (vencidos o por vencer): los permisos
 * críticos en rojo. Solo para roles con permiso EHS (ADMIN/SEGURIDAD).
 */

@Serializable
private data class BellAviso(
    val tipo: String,
    val origenId: Int,
    val titulo: String,
    val fechaLimite: String,
    val diasRestantes: Long,
    val estado: String = "",
    val esCritico: Boolean = false
)

@Composable
fun EhsAlertBell(client: HttpClient, canSeeEhs: Boolean, onOpenAvisos: () -> Unit) {
    if (!canSeeEhs) return
    var avisos by remember { mutableStateOf(emptyList<BellAviso>()) }
    var abierto by remember { mutableStateOf(false) }

    // Refrescar avisos al montar y cada 5 minutos; también al reabrir el menu
    LaunchedEffect(abierto) {
        try {
            avisos = client.get("$BACKEND_URL/api/v1/ehs/avisos").body()
        } catch (e: Exception) { /* sin avisos si falla; no bloquear la UI */ }
    }

    val criticos = avisos.count { it.esCritico }
    val total = avisos.size
    val colorBadge = if (criticos > 0) "#dc2626" else if (total > 0) "#d97706" else "#94a3b8"

    Div({ style { position(Position.Relative) } }) {
        Button({
            style {
                border(0.px); backgroundColor(Color.white); cursor("pointer")
                fontSize(19.px); padding(4.px, 6.px); borderRadius(8.px)
                position(Position.Relative)
            }
            onClick { abierto = !abierto }
            title("Avisos de vencimiento EHS")
        }) {
            Text("🔔")
            if (total > 0) Span({
                style {
                    position(Position.Absolute); top((-4).px); right((-4).px)
                    fontSize(10.px); fontWeight("700"); color(Color.white)
                    backgroundColor(Color(colorBadge)); borderRadius(50.percent)
                    padding(1.px, 5.px)
                }
            }) { Text(total.toString()) }
        }
        if (abierto) {
            Div({
                style {
                    position(Position.Absolute); top(44.px); right(0.px); property("z-index", "50")
                    width(360.px); maxHeight(420.px); overflowY("auto")
                    backgroundColor(Color.white); borderRadius(10.px)
                    property("border", "1px solid #e2e8f0")
                    property("box-shadow", "0 10px 30px rgba(0,0,0,0.12)")
                    padding(10.px)
                }
            }) {
                P({ style { margin(2.px, 4.px, 8.px, 4.px); fontSize(12.px); fontWeight("700"); color(Color("#475569")) } }) {
                    Text(if (total == 0) "Sin vencimientos próximos" else "$total avisos${if (criticos > 0) " · $criticos críticos" else ""}")
                }
                if (total == 0) {
                    P({ style { margin(6.px, 4.px); fontSize(13.px); color(Color("#64748b")) } }) { Text("Nada vencido ni por vencer en los próximos días.") }
                }
                avisos.take(8).forEach { a ->
                    Div({
                        style {
                            padding(8.px); marginBottom(6.px); borderRadius(8.px)
                            backgroundColor(if (a.esCritico) Color("#fef2f2") else Color("#f8fafc"))
                            property("border-left", "3px solid ${if (a.esCritico) "#dc2626" else if (a.estado == "Vencido") "#f59e0b" else "#94a3b8"}")
                            cursor("pointer")
                        }
                        onClick { abierto = false; onOpenAvisos() }
                    }) {
                        P({ style { margin(0.px); fontSize(13.px); fontWeight("600"); color(Color("#0f172a")) } }) {
                            Text(if (a.esCritico) "⚠ ${a.titulo}" else a.titulo)
                        }
                        P({ style { margin(2.px, 0.px, 0.px, 0.px); fontSize(11.px); color(Color("#64748b")) } }) {
                            Text(
                                "${a.tipo} · ${if (a.diasRestantes < 0) "vencido hace ${-a.diasRestantes} d" else "en ${a.diasRestantes} d"} · ${a.fechaLimite}"
                            )
                        }
                    }
                }
                if (total > 8) {
                    Button({
                        style { margin(6.px, 4.px); fontSize(12.px); color(Color("#2563eb")); border(0.px); backgroundColor(Color.white); cursor("pointer") }
                        onClick { abierto = false; onOpenAvisos() }
                    }) { Text("Ver todos los avisos →") }
                }
            }
        }
    }
}
