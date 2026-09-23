import androidx.compose.runtime.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.css.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.Serializable

@Serializable
private data class EhsMetricsView(
    val incidentes: Int = 0,
    val diasPerdidosRegistrados: Int = 0,
    val incidentesSinDiasValidos: Int = 0,
    val inspeccionesAbiertas: Int = 0,
    val capacitacionesVencidas: Int = 0,
    val capacitacionesPorVencer30Dias: Int = 0,
    val capacitacionesSinFechaValida: Int = 0,
    val horasTrabajadasDisponibles: Boolean = false
)

@Composable
fun EhsMetricsModule(client: HttpClient, scope: CoroutineScope) {
    var data by remember { mutableStateOf<EhsMetricsView?>(null) }
    var error by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        try {
            data = client.get("$BACKEND_URL/api/v1/ehs/indicadores").body()
        } catch (e: Exception) {
            error = "No se pudieron cargar los indicadores: ${e.message ?: "error del servidor"}"
        }
    }
    Div {
        H1({ style { color(Color("#0f172a")); marginBottom(4.px) } }) { Text("Indicadores EHS") }
        P({ style { color(Color("#475569")) } }) { Text("Conteos derivados de los registros actuales, no tasas oficiales de accidentabilidad.") }
        if (error.isNotBlank()) P({ style { color(Color("#b91c1c")) } }) { Text(error) }
        if (data == null && error.isBlank()) P { Text("Cargando indicadores...") }
        data?.let { m ->
            Div({ style { display(DisplayStyle.Grid); property("grid-template-columns", "repeat(auto-fit, minmax(210px, 1fr))"); gap(14.px); marginTop(20.px) } }) {
                listOf(
                    "Incidentes registrados" to m.incidentes,
                    "Días perdidos declarados" to m.diasPerdidosRegistrados,
                    "Inspecciones sin cierre" to m.inspeccionesAbiertas,
                    "Capacitaciones vencidas" to m.capacitacionesVencidas,
                    "Capacitaciones por vencer (30 días)" to m.capacitacionesPorVencer30Dias
                ).forEach { (label, value) ->
                    Div({ style { backgroundColor(Color.white); property("border", "1px solid #e2e8f0"); borderRadius(10.px); padding(20.px) } }) {
                        P({ style { color(Color("#475569")); margin(0.px) } }) { Text(label) }
                        H2({ style { color(Color("#0f172a")); marginBottom(0.px) } }) { Text(value.toString()) }
                    }
                }
            }
            P({ style { color(Color("#475569")); marginTop(22.px) } }) {
                Text("Calidad de datos: ${m.incidentesSinDiasValidos} incidentes sin días perdidos válidos y ${m.capacitacionesSinFechaValida} capacitaciones sin próxima fecha válida.")
            }
            P({ style { color(Color("#475569")) } }) {
                Text("Frecuencia y gravedad: no calculables hasta registrar horas trabajadas por período y acordar qué incidentes son registrables.")
            }
        }
    }
}
