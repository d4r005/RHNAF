import androidx.compose.runtime.*
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.css.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.contentType
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
private data class IncidenteMesView(
    val mes: String = "",
    val incidentes: Int = 0,
    val diasPerdidos: Int = 0
)

// Debe incluir TODOS los campos de EhsRatePeriod del servidor: el Json del
// cliente es estricto y falla si la respuesta trae claves desconocidas.
@Serializable
private data class TasaPeriodoView(
    val id: Int = 0,
    val periodo: String = "",
    val horasTrabajadas: Double = 0.0,
    val accidentesRegistrables: Int = 0,
    val diasPerdidos: Int = 0,
    val fuenteHoras: String = "",
    val validadoPor: String = "",
    val motivoRevision: String = "",
    val version: Int = 0,
    val frecuenciaPorMillon: Double = 0.0,
    val gravedadPorMillon: Double = 0.0
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
fun EhsMetricsModule(client: HttpClient, scope: CoroutineScope, usuario: String = "Portal RH", canEdit: Boolean = false) {
    var data by remember { mutableStateOf<EhsMetricsView?>(null) }
    var error by remember { mutableStateOf("") }
    // Periodos de tasas cargados en el modulo de Tasas EHS (para el QOS de IFR).
    var tasas by remember { mutableStateOf(emptyList<TasaPeriodoView>()) }
    var hayTasas by remember { mutableStateOf(false) }
    // Actualizacion de un mes desde el QOS (crea version nueva del periodo).
    var f_mes by remember { mutableStateOf("10") }
    var f_horas by remember { mutableStateOf("") }
    var f_acc by remember { mutableStateOf("0") }
    var f_dias by remember { mutableStateOf("0") }
    var f_motivo by remember { mutableStateOf("") }
    var qosMsg by remember { mutableStateOf("") }
    var qosGuardando by remember { mutableStateOf(false) }
    suspend fun recargarTasas() { tasas = client.get("$BACKEND_URL/api/v1/ehs/tasas").body() }
    LaunchedEffect(Unit) {
        try {
            data = client.get("$BACKEND_URL/api/v1/ehs/indicadores").body()
        } catch (e: Exception) {
            error = "No se pudieron cargar los indicadores: ${e.message ?: "error del servidor"}"
        }
        try {
            tasas = client.get("$BACKEND_URL/api/v1/ehs/tasas").body()
            hayTasas = tasas.isNotEmpty()
        } catch (e: Exception) { hayTasas = false }
    }
    Div {
        H1({ style { color(Color("#0f172a")); marginBottom(4.px) } }) { Text(I18n.get("met_titulo")) }
        P({ style { color(Color("#475569")) } }) { Text(I18n.get("met_intro")) }
        if (error.isNotBlank()) P({ style { color(Color("#b91c1c")) } }) { Text(error) }
        if (data == null && error.isBlank()) P { Text(I18n.get("met_cargando")) }
        data?.let { m ->
            Div({ style { display(DisplayStyle.Grid); property("grid-template-columns", "repeat(auto-fit, minmax(210px, 1fr))"); gap(14.px); marginTop(20.px) } }) {
                listOf(
                    "Accidentes registrados (año en curso)" to m.incidentes,
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
                Text(I18n.get("met_calidad_datos").replace("{a}", "${m.incidentesSinDiasValidos}").replace("{b}", "${m.capacitacionesSinFechaValida}"))
            }

            // ==== KPIs de accidentabilidad ====
            Div({ style { display(DisplayStyle.Grid); property("grid-template-columns", "repeat(auto-fit, minmax(210px, 1fr))"); gap(14.px); marginTop(26.px) } }) {
                Div({ style { backgroundColor(Color.white); property("border", "1px solid #e2e8f0"); borderRadius(10.px); padding(20.px) } }) {
                    P({ style { color(Color("#475569")); margin(0.px) } }) { Text(I18n.get("met_if")) }
                    H2({ style { color(Color("#0f172a")); marginBottom(2.px) } }) {
                        Text(m.tasaFrecuencia?.let { (kotlin.math.round(it * 100.0) / 100.0).toString() } ?: "—")
                    }
                    P({ style { color(Color("#64748b")); fontSize(12.px); margin(0.px) } }) {
                        Text(if (m.periodoTasas.isBlank()) "Registra horas trabajadas en Tasas EHS" else "Accidentes × 200,000 / horas · ${m.periodoTasas}")
                    }
                }
                Div({ style { backgroundColor(Color.white); property("border", "1px solid #e2e8f0"); borderRadius(10.px); padding(20.px) } }) {
                    P({ style { color(Color("#475569")); margin(0.px) } }) { Text(I18n.get("met_ig")) }
                    H2({ style { color(Color("#0f172a")); marginBottom(2.px) } }) {
                        Text(m.tasaGravedad?.let { (kotlin.math.round(it * 100.0) / 100.0).toString() } ?: "—")
                    }
                    P({ style { color(Color("#64748b")); fontSize(12.px); margin(0.px) } }) {
                        Text(if (m.periodoTasas.isBlank()) "Días perdidos × 200,000 / horas" else "Días perdidos × 200,000 / horas · ${m.periodoTasas}")
                    }
                }
                Div({ style { backgroundColor(Color.white); property("border", "1px solid #e2e8f0"); borderRadius(10.px); padding(20.px) } }) {
                    P({ style { color(Color("#475569")); margin(0.px) } }) { Text(I18n.get("met_cumplimiento")) }
                    H2({ style { color(Color("#16a34a")); marginBottom(2.px) } }) { Text("${m.porcentajeCumplimientoLegal.toInt()}%") }
                    P({ style { color(Color("#64748b")); fontSize(12.px); margin(0.px) } }) {
                        Text(I18n.get("met_aplicables").replace("{n}", "${m.aplicables}"))
                    }
                }
                Div({ style { backgroundColor(Color.white); property("border", "1px solid #e2e8f0"); borderRadius(10.px); padding(20.px) } }) {
                    P({ style { color(Color("#475569")); margin(0.px) } }) { Text(I18n.get("met_permisos")) }
                    H2({ style { color(if (m.criticasVencidas > 0) Color("#dc2626") else Color("#0f172a")); marginBottom(2.px) } }) { Text(m.criticasTotal.toString()) }
                    P({ style { color(Color(if (m.criticasVencidas > 0) "#dc2626" else "#64748b")); fontSize(12.px); margin(0.px) } }) {
                        Text(I18n.get("met_criticos").replace("{v}", "${m.criticasVencidas}").replace("{p}", "${m.criticasPorVencer}"))
                    }
                }
            }

            // ==== Grafica mensual de incidentes (12 meses) ====
            if (m.incidentesPorMes.isNotEmpty()) {
                val maxInc = (m.incidentesPorMes.maxOf { it.incidentes }).coerceAtLeast(1)
                Div({ style { marginTop(28.px); backgroundColor(Color.white); property("border", "1px solid #e2e8f0"); borderRadius(12.px); padding(22.px) } }) {
                    H3({ style { marginTop(0.px); color(Color("#0f172a")) } }) { Text(I18n.get("met_incidentes_mes")) }
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
                        Text(I18n.get("met_dias_perdidos").replace("{n}", "${m.incidentesPorMes.sumOf { it.diasPerdidos }}"))
                    }
                }
            }

            // ==== QOS: IFR anual y mensual (formato QOS de la empresa) ====
            if (hayTasas) {
                val meses = listOf("Enero","Febrero","Marzo","Abril","Mayo","Junio","Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre")
                val t26 = tasas.filter { it.periodo.startsWith("2026-") }
                val hTot = t26.sumOf { it.horasTrabajadas }
                val accTot = t26.sumOf { it.accidentesRegistrables }
                val ifrAnual = if (hTot > 0) accTot * 200_000.0 / hTot else 0.0
                fun fmt(d: Double): String {
                    val r = kotlin.math.round(d * 100.0) / 100.0
                    return r.toString()
                }
                fun miles(d: Double): String {
                    val ent = d.toLong().toString()
                    return ent.reversed().chunked(3).joinToString(",").reversed()
                }
                Div({ style { marginTop(28.px); backgroundColor(Color.white); property("border", "1px solid #e2e8f0"); borderRadius(12.px); padding(22.px) } }) {
                    Div({ style { display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); alignItems(AlignItems.Baseline); flexWrap(FlexWrap.Wrap); gap(8.px) } }) {
                        H3({ style { marginTop(0.px); marginBottom(0.px); color(Color("#0f172a")) } }) { Text("QOS — Accidents / IFR 2026") }
                        Span({ style { fontSize(12.px); color(Color("#64748b")) } }) { Text("SITE: Escobedo · OWNER: Dario Robles · FREQUENCY: Mensual") }
                    }
                    Div({ style { display(DisplayStyle.Flex); gap(14.px); marginTop(14.px); marginBottom(18.px); flexWrap(FlexWrap.Wrap) } }) {
                        Div({ style { padding(14.px, 22.px); borderRadius(10.px); backgroundColor(Color("#0f172a")); color(Color.white) } }) {
                            P({ style { margin(0.px); fontSize(12.px); color(Color("#cbd5e1")) } }) { Text("IFR ANUAL 2026 (acumulado a la fecha)") }
                            H2({ style { marginTop(4.px); color(Color.white); fontSize(30.px) } }) { Text(fmt(ifrAnual)) }
                        }
                        Div({ style { padding(14.px, 22.px); borderRadius(10.px); backgroundColor(Color("#f8fafc")); property("border", "1px solid #e2e8f0") } }) {
                            P({ style { margin(0.px); fontSize(12.px); color(Color("#475569")) } }) { Text("Horas trabajadas 2026") }
                            H2({ style { marginTop(4.px); color(Color("#0f172a")) } }) { Text(miles(hTot)) }
                        }
                        Div({ style { padding(14.px, 22.px); borderRadius(10.px); backgroundColor(Color("#f8fafc")); property("border", "1px solid #e2e8f0") } }) {
                            P({ style { margin(0.px); fontSize(12.px); color(Color("#475569")) } }) { Text("Accidentes registrables 2026") }
                            H2({ style { marginTop(4.px); color(Color("#b45309")) } }) { Text(accTot.toString()) }
                        }
                    }
                    // Tabla mensual
                    Div({ style { property("border", "1px solid #e2e8f0"); borderRadius(8.px); overflow("hidden") } }) {
                        Div({ style { display(DisplayStyle.Grid); property("grid-template-columns", "1.4fr 1fr 1fr 1fr"); backgroundColor(Color("#1e293b")); color(Color.white); fontWeight("600"); fontSize(13.px) } }) {
                            listOf("MES","HORAS TRABAJADAS","ACCIDENTES","IFR MENSUAL").forEach { h ->
                                Div({ style { padding(10.px, 12.px) } }) { Text(h) }
                            }
                        }
                        meses.forEachIndexed { i, nombre ->
                            val mm = if (i + 1 < 10) "0" + (i + 1) else "" + (i + 1)
                            val per = t26.firstOrNull { it.periodo == "2026-$mm" }
                            val hrs = per?.horasTrabajadas ?: 0.0
                            val acc = per?.accidentesRegistrables ?: 0
                            val ifr = if (per != null && hrs > 0) acc * 200_000.0 / hrs else null
                            Div({ style { display(DisplayStyle.Grid); property("grid-template-columns", "1.4fr 1fr 1fr 1fr"); fontSize(13.px); backgroundColor(if (i % 2 == 0) Color.white else Color("#f8fafc")); property("border-top", "1px solid #e2e8f0") } }) {
                                Div({ style { padding(9.px, 12.px); fontWeight("600"); color(Color("#0f172a")) } }) { Text(nombre) }
                                Div({ style { padding(9.px, 12.px); color(Color("#334155")) } }) { Text(if (per != null) miles(hrs) else "—") }
                                Div({ style { padding(9.px, 12.px); color(if (acc > 0) Color("#b45309") else Color("#334155")); fontWeight(if (acc > 0) "700" else "400") } }) { Text(if (per != null) acc.toString() else "—") }
                                Div({ style { padding(9.px, 12.px); color(if (ifr != null && ifr > 0) Color("#b91c1c") else Color("#334155")); fontWeight(if (ifr != null && ifr > 0) "700" else "400") } }) { Text(ifr?.let { fmt(it) } ?: "—") }
                            }
                        }
                        Div({ style { display(DisplayStyle.Grid); property("grid-template-columns", "1.4fr 1fr 1fr 1fr"); fontSize(13.px); backgroundColor(Color("#e2e8f0")); fontWeight("700"); property("border-top", "2px solid #1e293b") } }) {
                            Div({ style { padding(10.px, 12.px); color(Color("#0f172a")) } }) { Text("TOTAL 2026") }
                            Div({ style { padding(10.px, 12.px); color(Color("#0f172a")) } }) { Text(miles(hTot)) }
                            Div({ style { padding(10.px, 12.px); color(Color("#0f172a")) } }) { Text(accTot.toString()) }
                            Div({ style { padding(10.px, 12.px); color(Color("#b91c1c")) } }) { Text(fmt(ifrAnual)) }
                        }
                    }
                    // Descarga + actualizacion
                    Div({ style { display(DisplayStyle.Flex); gap(10.px); marginTop(16.px); flexWrap(FlexWrap.Wrap); alignItems(AlignItems.FlexEnd) } }) {
                        Button({
                            style { padding(8.px, 16.px); backgroundColor(Color("#2563eb")); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer"); fontWeight("bold") }
                            onClick {
                                val opts = js("({})"); opts.headers = js("({})"); opts.headers.Authorization = "Bearer $apiAuthToken"
                                window.asDynamic().fetch("$BACKEND_URL/api/v1/ehs/tasas/qos/xlsx?anio=2026", opts)
                                    .then { r: dynamic -> r.blob() }
                                    .then { b: dynamic ->
                                        val url = window.asDynamic().URL.createObjectURL(b) as String
                                        val a = js("document.createElement('a')")
                                        a.href = url; a.download = "qos_accidents_2026.xlsx"
                                        js("document.body.appendChild(a)"); a.click(); js("document.body.removeChild(a)")
                                        window.setTimeout({ window.asDynamic().URL.revokeObjectURL(url) }, 3000)
                                    }
                                    .`catch` { e: dynamic -> qosMsg = "No se pudo descargar: " + (e.message ?: "error") }
                            }
                        }) { Text("Descargar Excel (QOS)") }
                        if (canEdit) {
                            org.jetbrains.compose.web.dom.Select({
                                style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); backgroundColor(Color.white) }
                                onChange { f_mes = it.value ?: "01" }
                            }) {
                                val opcionesMeses = listOf(
                                    "01" to "Enero", "02" to "Febrero", "03" to "Marzo", "04" to "Abril",
                                    "05" to "Mayo", "06" to "Junio", "07" to "Julio", "08" to "Agosto",
                                    "09" to "Septiembre", "10" to "Octubre", "11" to "Noviembre", "12" to "Diciembre")
                                opcionesMeses.forEach { par ->
                                    org.jetbrains.compose.web.dom.Option(value = par.first) { Text(par.second) }
                                }
                            }
                            Input(InputType.Text) {
                                placeholder("Horas trabajadas")
                                style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(130.px) }
                                onInput { ev -> f_horas = ev.value }
                            }
                            Input(InputType.Text) {
                                placeholder("Accidentes")
                                style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(90.px) }
                                onInput { ev -> f_acc = ev.value }
                            }
                            Input(InputType.Text) {
                                placeholder("Días perdidos")
                                style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(100.px) }
                                onInput { ev -> f_dias = ev.value }
                            }
                            org.jetbrains.compose.web.dom.Input(type = org.jetbrains.compose.web.attributes.InputType.Text) {
                                placeholder("Motivo de la revisión")
                                style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(180.px) }
                                onInput { f_motivo = it.value }
                            }
                            Button({
                                style { padding(8.px, 16.px); backgroundColor(if (qosGuardando) Color("#94a3b8") else Color("#0f766e")); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer"); fontWeight("bold") }
                                onClick {
                                    val horas = f_horas.replace(",", "").toDoubleOrNull()
                                    val acc = f_acc.trim().toIntOrNull()
                                    val dias = f_dias.trim().toIntOrNull()
                                    val valido = !qosGuardando && f_horas.isNotBlank() && horas != null && horas > 0 && acc != null && acc >= 0 && dias != null && dias >= 0
                                    if (!valido && f_horas.isNotBlank()) {
                                        qosMsg = "Revisa los valores: horas mayor a 0 (tal cual las da Intelectión) y accidentes/días enteros."
                                    }
                                    if (valido) {
                                        qosGuardando = true; qosMsg = ""
                                        scope.launch {
                                            try {
                                                val resp: String = client.post("$BACKEND_URL/api/v1/ehs/tasas") {
                                                    contentType(ContentType.Application.Json)
                                                    setBody(TasaPeriodoView(
                                                        periodo = "2026-$f_mes",
                                                        horasTrabajadas = horas,
                                                        accidentesRegistrables = acc,
                                                        diasPerdidos = dias,
                                                        fuenteHoras = "Sistema Intelectión — actualizado desde el QOS del portal",
                                                        validadoPor = usuario,
                                                        motivoRevision = if (f_motivo.isBlank()) "Actualización desde el QOS del portal" else f_motivo
                                                    ))
                                                }.body()
                                                recargarTasas()
                                                qosMsg = "Mes 2026-$f_mes actualizado."
                                                f_horas = ""; f_acc = "0"; f_dias = "0"; f_motivo = ""
                                            } catch (e: Exception) { qosMsg = "No se pudo guardar: ${e.message}" }
                                            qosGuardando = false
                                        }
                                    }
                                }
                            }) { Text(if (qosGuardando) "Guardando..." else "Actualizar mes") }
                        }
                    }
                    if (qosMsg.isNotBlank()) P({ style { fontSize(13.px); color(Color("#047857")); marginTop(10.px) } }) { Text(qosMsg) }
                    P({ style { color(Color("#64748b")); fontSize(12.px); marginTop(10.px); marginBottom(0.px) } }) {
                        Text("IFR = Accidentes × 200,000 / horas trabajadas (convención QOS). Las horas se capturan tal cual las entrega el sistema Intelectión.")
                    }
                }
            }
        }
    }
}
