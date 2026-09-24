import androidx.compose.runtime.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.css.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.Serializable

@Serializable
private data class IncidenteMesView(
    val mes: String = "",
    val incidentes: Int = 0,
    val diasPerdidos: Int = 0
)

@Serializable
private data class EhsMetricsView(
    val incidentes: Int = 0,
    val diasPerdidosRegistrados: Int = 0,
    val incidentesSinDiasValidos: Int = 0,
    val inspeccionesAbiertas: Int = 0,
    val hallazgosAuditoriaAbiertos: Int = 0,
    val capacitacionesVencidas: Int = 0,
    val capacitacionesPorVencer30Dias: Int = 0,
    val capacitacionesSinFechaValida: Int = 0,
    val horasTrabajadasDisponibles: Boolean = false,
    val incidentesPorMes: List<IncidenteMesView> = emptyList(),
    val periodoTasas: String = "",
    val tasaFrecuencia: Double? = null,
    val tasaGravedad: Double? = null,
    val criticasTotal: Int = 0,
    val criticasVencidas: Int = 0,
    val criticasPorVencer: Int = 0,
    val aplicables: Int = 0,
    val porcentajeCumplimientoLegal: Double = 0.0
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
                    "Auditorías sin cierre" to m.inspeccionesAbiertas,
                    "Hallazgos de auditoría abiertos" to m.hallazgosAuditoriaAbiertos,
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

            // ==== KPIs de accidentabilidad ====
            Div({ style { display(DisplayStyle.Grid); property("grid-template-columns", "repeat(auto-fit, minmax(210px, 1fr))"); gap(14.px); marginTop(26.px) } }) {
                Div({ style { backgroundColor(Color.white); property("border", "1px solid #e2e8f0"); borderRadius(10.px); padding(20.px) } }) {
                    P({ style { color(Color("#475569")); margin(0.px) } }) { Text("Índice de frecuencia (IF)") }
                    H2({ style { color(Color("#0f172a")); marginBottom(2.px) } }) {
                        Text(m.tasaFrecuencia?.let { (kotlin.math.round(it * 100.0) / 100.0).toString() } ?: "—")
                    }
                    P({ style { color(Color("#64748b")); fontSize(12.px); margin(0.px) } }) {
                        Text(if (m.periodoTasas.isBlank()) "Registra horas trabajadas en Tasas EHS" else "Accidentes × 200,000 / horas · ${m.periodoTasas}")
                    }
                }
                Div({ style { backgroundColor(Color.white); property("border", "1px solid #e2e8f0"); borderRadius(10.px); padding(20.px) } }) {
                    P({ style { color(Color("#475569")); margin(0.px) } }) { Text("Índice de gravedad (IG)") }
                    H2({ style { color(Color("#0f172a")); marginBottom(2.px) } }) {
                        Text(m.tasaGravedad?.let { (kotlin.math.round(it * 100.0) / 100.0).toString() } ?: "—")
                    }
                    P({ style { color(Color("#64748b")); fontSize(12.px); margin(0.px) } }) {
                        Text(if (m.periodoTasas.isBlank()) "Días perdidos × 200,000 / horas" else "Días perdidos × 200,000 / horas · ${m.periodoTasas}")
                    }
                }
                Div({ style { backgroundColor(Color.white); property("border", "1px solid #e2e8f0"); borderRadius(10.px); padding(20.px) } }) {
                    P({ style { color(Color("#475569")); margin(0.px) } }) { Text("Cumplimiento legal") }
                    H2({ style { color(Color("#16a34a")); marginBottom(2.px) } }) { Text("${m.porcentajeCumplimientoLegal.toInt()}%") }
                    P({ style { color(Color("#64748b")); fontSize(12.px); margin(0.px) } }) {
                        Text("${m.aplicables} obligaciones aplicables en la matriz")
                    }
                }
                Div({ style { backgroundColor(Color.white); property("border", "1px solid #e2e8f0"); borderRadius(10.px); padding(20.px) } }) {
                    P({ style { color(Color("#475569")); margin(0.px) } }) { Text("Permisos críticos") }
                    H2({ style { color(if (m.criticasVencidas > 0) Color("#dc2626") else Color("#0f172a")); marginBottom(2.px) } }) { Text(m.criticasTotal.toString()) }
                    P({ style { color(Color(if (m.criticasVencidas > 0) "#dc2626" else "#64748b")); fontSize(12.px); margin(0.px) } }) {
                        Text("${m.criticasVencidas} vencidos · ${m.criticasPorVencer} por vencer")
                    }
                }
            }

            // ==== Grafica mensual de incidentes (12 meses) ====
            if (m.incidentesPorMes.isNotEmpty()) {
                val maxInc = (m.incidentesPorMes.maxOf { it.incidentes }).coerceAtLeast(1)
                Div({ style { marginTop(28.px); backgroundColor(Color.white); property("border", "1px solid #e2e8f0"); borderRadius(12.px); padding(22.px) } }) {
                    H3({ style { marginTop(0.px); color(Color("#0f172a")) } }) { Text("Incidentes por mes (últimos 12 meses)") }
                    Div({ style { display(DisplayStyle.Flex); alignItems(AlignItems.FlexEnd); gap(8.px); height(140.px); marginTop(16.px) } }) {
                        m.incidentesPorMes.forEach { mes ->
                            Div({ style { flex(1); display(DisplayStyle.Flex); flexDirection(FlexDirection.Column); alignItems(AlignItems.Center); justifyContent(JustifyContent.FlexEnd); height(100.percent) } }) {
                                if (mes.incidentes > 0) Span({ style { fontSize(11.px); fontWeight("600"); color(Color("#0f172a")); marginBottom(2.px) } }) { Text(mes.incidentes.toString()) }
                                Div({
                                    style {
                                        width(100.percent); borderRadius(4.px, 4.px, 0.px, 0.px); backgroundColor(Color("#2563eb"))
                                        height(((mes.incidentes.toDouble() / maxInc) * 100).percent)
                                        property("min-height", if (mes.incidentes > 0) "4px" else "0px")
                                    }
                                })
                                Span({ style { fontSize(10.px); color(Color("#64748b")); marginTop(4.px); whiteSpace("nowrap") } }) {
                                    Text("${mes.mes.substring(5)}/${mes.mes.substring(2, 4)}")
                                }
                            }
                        }
                    }
                    P({ style { color(Color("#64748b")); fontSize(12.px); marginTop(14.px) } }) {
                        Text("Incluye ${m.incidentesPorMes.sumOf { it.diasPerdidos }} días perdidos en el período. Si la serie se ve vacía, registra la fecha de tus incidentes.")
                    }
                }
            }
        }
    }
}
