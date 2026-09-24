import androidx.compose.runtime.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.attributes.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/**
 * EHS - Calendario de vencimientos y actividades.
 * Vista mensual con todas las fechas límite: obligaciones de la matriz
 * legal (permisos críticos destacados), capacitaciones, simulacros,
 * acciones, inspecciones y contratistas. Estilo calendario de EHSoft.
 */

@Serializable
private data class CalEvento(
    val fecha: String,
    val tipo: String,
    val id: Int,
    val titulo: String,
    val detalle: String = "",
    val categoria: String = "",
    val esCritico: Boolean = false,
    val estado: String = ""
)

@Serializable
private data class CalEventoPost(
    val fecha: String,
    val tipo: String = "evento",
    val titulo: String = "",
    val detalle: String = "",
    val responsable: String = "",
    val estado: String = ""
)

@Serializable
private data class CalRespuesta(
    val mes: String,
    val diasEnMes: Int,
    val primerDiaSemana: Int,
    val hoy: String,
    val eventos: List<CalEvento> = emptyList()
)

private val MESES_ES = listOf(
    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
)

// String.format no existe en Kotlin/JS: usamos padStart para el formato yyyy-MM
private fun ym(y: Int, m: Int): String = y.toString().padStart(4, '0') + "-" + m.toString().padStart(2, '0')

private fun mesAnterior(mes: String): String {
    val (y, m) = mes.split("-").map { it.toInt() }
    return if (m == 1) ym(y - 1, 12) else ym(y, m - 1)
}

private fun mesSiguiente(mes: String): String {
    val (y, m) = mes.split("-").map { it.toInt() }
    return if (m == 12) ym(y + 1, 1) else ym(y, m + 1)
}

private fun nombreMes(mes: String): String {
    val (_, m) = mes.split("-").map { it.toInt() }
    return MESES_ES[m - 1]
}

private fun colorEvento(e: CalEvento): String = when {
    e.esCritico || e.estado == "Vencido" -> "#dc2626"
    e.tipo == "obligacion" -> if (e.estado == "PorVencer") "#d97706" else "#2563eb"
    e.tipo == "capacitacion" -> "#0d9488"
    e.tipo == "simulacro" -> "#7c3aed"
    e.tipo == "accion" -> "#ea580c"
    e.tipo == "inspeccion" -> "#0369a1"
    e.tipo == "evento" -> "#059669"
    e.tipo == "contratista" -> "#64748b"
    else -> "#475569"
}

private fun etiquetaTipo(tipo: String): String = when (tipo) {
    "obligacion" -> "Obligación"
    "capacitacion" -> "Capacitación"
    "simulacro" -> "Simulacro"
    "accion" -> "Acción"
    "inspeccion" -> "Auditoría interna"
    "evento" -> "Evento"
    "contratista" -> "Contratista"
    else -> tipo
}

@Composable
fun EhsCalendarModule(client: HttpClient, scope: CoroutineScope) {
    var mes by remember { mutableStateOf("") }        // "yyyy-MM"; vacío = mes actual (lo decide el servidor)
    var data by remember { mutableStateOf<CalRespuesta?>(null) }
    // Captura de eventos propios: al dar clic en un día se abre el panel.
    var selectedFecha by remember { mutableStateOf("") }
    var evTitulo by remember { mutableStateOf("") }
    var evDetalle by remember { mutableStateOf("") }
    var evResponsable by remember { mutableStateOf(window.localStorage.getItem("naf_user_name") ?: "") }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }

    fun refresh(target: String) {
        scope.launch {
            loading = true
            error = ""
            try {
                val params = if (target.isBlank()) "" else "?mes=$target"
                val resp = client.get("$BACKEND_URL/api/v1/ehs/calendario$params").body<CalRespuesta>()
                mes = resp.mes
                data = resp
            } catch (e: Exception) {
                error = "No se pudo cargar el calendario: ${e.message ?: "error"}"
            } finally { loading = false }
        }
    }

    LaunchedEffect(Unit) { refresh(mes) }

    val resp = data
    Div {
        // Encabezado + navegacion de mes
        Div({ style { display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); alignItems(AlignItems.Center); marginBottom(20.px) } }) {
            Div {
                H1({ style { margin(0.px); fontSize(26.px); color(Color("#0f172a")) } }) { Text("Calendario de vencimientos") }
                P({ style { margin(4.px, 0.px, 0.px, 0.px); color(Color("#64748b")) } }) {
                    Text("Obligaciones legales, capacitaciones, simulacros, acciones, auditorías internas y contratistas. Da clic en un día para agregar un evento propio. Los permisos críticos se muestran en rojo.")
                }
            }
            Div({ style { display(DisplayStyle.Flex); alignItems(AlignItems.Center); gap(8.px) } }) {
                Button({
                    style { padding(8.px, 14.px); borderRadius(8.px); border(0.px); backgroundColor(Color("#e2e8f0")); cursor("pointer") }
                    onClick { refresh(mesAnterior(mes)) }
                }) { Text("‹") }
                Span({ style { fontWeight("600"); color(Color("#0f172a")); fontSize(15.px) } }) {
                    Text(if (mes.isBlank()) "…" else "${nombreMes(mes)} ${mes.split("-")[0]}")
                }
                Button({
                    style { padding(8.px, 14.px); borderRadius(8.px); border(0.px); backgroundColor(Color("#e2e8f0")); cursor("pointer") }
                    onClick { refresh(mesSiguiente(mes)) }
                }) { Text("›") }
                Button({
                    style { padding(8.px, 14.px); borderRadius(8.px); border(0.px); backgroundColor(Color("#2563eb")); color(Color.white); cursor("pointer") }
                    onClick { refresh("") }
                }) { Text("Hoy") }
            }
        }

        if (error.isNotBlank()) P({ style { color(Color("#dc2626")); padding(12.px); backgroundColor(Color("#fef2f2")); borderRadius(8.px) } }) { Text(error) }
        if (loading) P { Text("Cargando calendario...") }

        resp?.let { r ->
            val eventosPorDia = r.eventos.groupBy { it.fecha }

            // Leyenda
            Div({ style { display(DisplayStyle.Flex); gap(16.px); flexWrap(FlexWrap.Wrap); marginBottom(14.px); fontSize(12.px); color(Color("#475569")) } }) {
                listOf(
                    "Permiso crítico / Vencido" to "#dc2626",
                    "Por vencer" to "#d97706",
                    "Obligación legal" to "#2563eb",
                    "Capacitación" to "#0d9488",
                    "Simulacro" to "#7c3aed",
                    "Acción" to "#ea580c",
                    "Auditoría int." to "#0369a1",
                    "Evento propio" to "#059669",
                    "Contratista" to "#64748b"
                ).forEach { (label, color) ->
                    Div({ style { display(DisplayStyle.Flex); alignItems(AlignItems.Center); gap(6.px) } }) {
                        Div({ style { width(10.px); height(10.px); borderRadius(50.percent); backgroundColor(Color(color)) } })
                        Text(label)
                    }
                }
            }

            // Cuadricula mensual
            Div({ style { display(DisplayStyle.Grid); property("grid-template-columns", "repeat(7, 1fr)"); gap(6.px) } }) {
                listOf("Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb").forEach { d ->
                    Div({ style { padding(8.px, 4.px); textAlign("center"); fontWeight("600"); fontSize(12.px); color(Color("#64748b")) } }) { Text(d) }
                }
                // celdas vacias antes del dia 1
                repeat(r.primerDiaSemana) {
                    Div({ style { minHeight(96.px); borderRadius(8.px); backgroundColor(Color("#f8fafc")) } })
                }
                (1..r.diasEnMes).forEach { dia ->
                    val fechaCelda = r.mes + "-" + dia.toString().padStart(2, '0')
                    val eventos = eventosPorDia[fechaCelda] ?: emptyList()
                    val esHoy = fechaCelda == r.hoy
                    val hayCritico = eventos.any { it.esCritico }
                    val hayVencido = eventos.any { it.estado == "Vencido" }
                    Div({
                        style {
                            minHeight(96.px)
                            padding(6.px)
                            borderRadius(8.px)
                            backgroundColor(if (esHoy) Color("#eff6ff") else Color.white)
                            property("border", if (selectedFecha == fechaCelda) "2px solid #059669" else if (esHoy) "2px solid #2563eb" else "1px solid #e2e8f0")
                            property("box-shadow", if (hayCritico) "inset 3px 0 0 #dc2626" else if (hayVencido) "inset 3px 0 0 #f59e0b" else "none")
                            overflow("hidden")
                            cursor("pointer")
                        }
                        onClick { selectedFecha = fechaCelda }
                    }) {
                        Div({ style { fontWeight(if (esHoy) "700" else "500"); fontSize(12.px); color(if (esHoy) Color("#2563eb") else Color("#475569")); marginBottom(4.px) } }) { Text(dia.toString()) }
                        eventos.take(3).forEach { ev ->
                            Div({
                                style {
                                    fontSize(10.px); marginBottom(3.px); padding(2.px, 4.px); borderRadius(4.px)
                                    backgroundColor(Color(colorEvento(ev)))
                                    color(Color.white)
                                    whiteSpace("nowrap"); overflow("hidden"); property("text-overflow", "ellipsis")
                                }
                            }) { Text(if (ev.esCritico) "⚠ ${ev.titulo}" else ev.titulo) }
                        }
                        if (eventos.size > 3) Span({ style { fontSize(10.px); color(Color("#64748b")) } }) { Text("+${eventos.size - 3} más") }
                    }
                }
            }

            // Panel de captura del día seleccionado
            if (selectedFecha.isNotBlank()) {
                val delDia = eventosPorDia[selectedFecha] ?: emptyList()
                Div({ style { marginTop(16.px); padding(14.px); borderRadius(10.px); property("border", "1px solid #d1fae5"); backgroundColor(Color("#f0fdf4") ) } }) {
                    Div({ style { display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); alignItems(AlignItems.Center) } }) {
                        H2({ style { margin(0.px); fontSize(15.px); color(Color("#065f46")) } }) { Text("Eventos del $selectedFecha") }
                        Button({
                            style { padding(4.px, 10.px); borderRadius(6.px); border(0.px); backgroundColor(Color("#e2e8f0")); cursor("pointer") }
                            onClick { selectedFecha = "" }
                        }) { Text("Cerrar") }
                    }
                    if (delDia.isNotEmpty()) {
                        Div({ style { marginTop(8.px) } }) {
                            delDia.forEach { ev ->
                                Div({ style { display(DisplayStyle.Flex); alignItems(AlignItems.Center); gap(8.px); marginTop(4.px); fontSize(13.px) } }) {
                                    Div({ style { width(8.px); height(8.px); borderRadius(50.percent); backgroundColor(Color(colorEvento(ev))) } })
                                    Text("${etiquetaTipo(ev.tipo)}: ${ev.titulo}")
                                    if (ev.detalle.isNotBlank()) Span({ style { color(Color("#64748b")) } }) { Text(" — ${ev.detalle}") }
                                    if (ev.tipo == "evento") Button({
                                        style { padding(2.px, 8.px); borderRadius(4.px); border(0.px); backgroundColor(Color("#ef4444")); color(Color.white); cursor("pointer"); fontSize(11.px) }
                                        onClick { scope.launch { client.delete("$BACKEND_URL/api/v1/ehs/calendario/eventos/${ev.id}"); refresh(mes) } }
                                    }) { Text("Borrar") }
                                }
                            }
                        }
                    } else {
                        P({ style { margin(8.px, 0.px, 0.px, 0.px); fontSize(13.px); color(Color("#64748b")) } }) { Text("Sin eventos ese día todavía.") }
                    }
                    Div({ style { display(DisplayStyle.Flex); gap(8.px); flexWrap(FlexWrap.Wrap); alignItems(AlignItems.Center); marginTop(10.px) } }) {
                        Input(InputType.Text) { placeholder("Título del evento *"); value(evTitulo); onInput { evTitulo = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(220.px) } }
                        Input(InputType.Text) { placeholder("Detalle"); value(evDetalle); onInput { evDetalle = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(220.px) } }
                        Input(InputType.Text) { placeholder("Responsable"); value(evResponsable); onInput { evResponsable = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(160.px) } }
                        Button({
                            style { padding(8.px, 16.px); borderRadius(6.px); border(0.px); backgroundColor(Color("#059669")); color(Color.white); cursor("pointer") }
                            onClick {
                                if (evTitulo.isBlank()) { window.alert("El título es obligatorio.") }
                                else {
                                    scope.launch {
                                        client.post("$BACKEND_URL/api/v1/ehs/calendario/eventos") {
                                            contentType(ContentType.Application.Json)
                                            setBody(CalEventoPost(fecha = selectedFecha, titulo = evTitulo, detalle = evDetalle, responsable = evResponsable))
                                        }
                                        evTitulo = ""; evDetalle = ""; refresh(mes)
                                    }
                                }
                            }
                        }) { Text("+ Agregar evento") }
                    }
                }
            }

            // Detalle del mes (lista legible)
            val delMes = r.eventos
            if (delMes.isNotEmpty()) {
                H2({ style { marginTop(26.px); marginBottom(12.px); color(Color("#0f172a")); fontSize(18.px) } }) { Text("Detalles de ${nombreMes(r.mes)}") }
                Div({ style { backgroundColor(Color.white); borderRadius(12.px); property("border", "1px solid #e2e8f0"); overflow("hidden") } }) {
                    Table({ style { width(100.percent); property("border-collapse", "collapse"); fontSize(13.px) } }) {
                        Thead { Tr { listOf("Día", "Tipo", "Título", "Estado", "Crítico").forEach { Th({ style { padding(10.px, 12.px); textAlign("left"); backgroundColor(Color("#f8fafc")); color(Color("#475569")); property("border-bottom", "1px solid #e2e8f0") } }) { Text(it) } } } }
                        Tbody {
                            delMes.forEach { ev ->
                                Tr {
                                    Td({ style { padding(10.px, 12.px); property("border-bottom", "1px solid #f1f5f9"); whiteSpace("nowrap") } }) { Text(ev.fecha.substring(8)) }
                                    Td({ style { padding(10.px, 12.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(etiquetaTipo(ev.tipo)) }
                                    Td({ style { padding(10.px, 12.px); property("border-bottom", "1px solid #f1f5f9") } }) {
                                        Text(ev.titulo)
                                        if (ev.detalle.isNotBlank()) Span({ style { color(Color("#64748b")); fontSize(12.px) } }) { Text(" — ${ev.detalle}") }
                                    }
                                    Td({ style { padding(10.px, 12.px); property("border-bottom", "1px solid #f1f5f9"); color(Color(if (ev.estado == "Vencido") "#dc2626" else if (ev.estado == "PorVencer") "#d97706" else "#475569")) } }) {
                                        Text(ev.estado.ifBlank { "-" })
                                    }
                                    Td({ style { padding(10.px, 12.px); property("border-bottom", "1px solid #f1f5f9") } }) {
                                        if (ev.esCritico) Span({ style { color(Color("#dc2626")); fontWeight("700") } }) { Text("⚠ Sí") } else Text("—")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
