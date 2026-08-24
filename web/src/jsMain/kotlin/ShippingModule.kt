import androidx.compose.runtime.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.css.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.call.*
import io.ktor.http.*
import com.example.rhnaf.shared.model.*
import kotlinx.coroutines.launch

// ============================================================
// Modulo EMBARQUES: pedidos, rutas de entrega y trazabilidad.
// Permisos: ALMACEN y ADMIN pueden editar; IMPORT_EXPORT y
// FINANZAS solo lectura.
// ============================================================

private enum class ShippingTab { PEDIDOS, RUTAS, TRAZABILIDAD, RESUMEN }

@Composable
fun ShippingModule(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope, t: Translations, userRole: UserRole) {
    val canEdit = userRole == UserRole.ADMIN || userRole == UserRole.ALMACEN
    var activeTab by remember { mutableStateOf(ShippingTab.RESUMEN) }

    Div({ style { padding(24.px) } }) {
        Div({ style { display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); alignItems(AlignItems.Center); marginBottom(20.px) } }) {
            H2({ style { margin(0.px); fontSize(20.px); color(Color("#0f172a")) } }) { Text("Embarques y Logística") }
            if (!canEdit) {
                Span({ style { fontSize(12.px); color(Color("#64748b")); backgroundColor(Color("#f1f5f9")); padding(4.px, 12.px); borderRadius(6.px) } }) { Text("Modo solo lectura") }
            }
        }

        Div({ style { display(DisplayStyle.Flex); gap(4.px); marginBottom(20.px); borderBottom(1.px, LineStyle.Solid, Color("#e2e8f0")) } }) {
            ShippingTab.values().forEach { tab ->
                val label = when (tab) {
                    ShippingTab.RESUMEN -> "Resumen"
                    ShippingTab.PEDIDOS -> "Pedidos"
                    ShippingTab.RUTAS -> "Rutas de Entrega"
                    ShippingTab.TRAZABILIDAD -> "Trazabilidad"
                }
                Button({
                    style {
                        padding(8.px, 16.px); property("border", "none")
                        property("border-bottom", if (activeTab == tab) "2px solid #2563eb" else "2px solid transparent")
                        backgroundColor(Color.transparent)
                        color(if (activeTab == tab) Color("#2563eb") else Color("#64748b"))
                        cursor("pointer"); fontSize(13.px)
                        fontWeight(if (activeTab == tab) "600" else "400")
                    }
                    onClick { activeTab = tab }
                }) { Text(label) }
            }
        }

        when (activeTab) {
            ShippingTab.RESUMEN -> ShippingResumenTab(client)
            ShippingTab.PEDIDOS -> ShippingPedidosTab(client, scope, canEdit)
            ShippingTab.RUTAS -> ShippingRutasTab(client, scope, canEdit)
            ShippingTab.TRAZABILIDAD -> ShippingTrazabilidadTab(client, scope, canEdit)
        }
    }
}

// ---------- RESUMEN ----------
@Composable
private fun ShippingResumenTab(client: HttpClient) {
    val BASE = BACKEND_URL
    var resumen by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try { resumen = client.get("$BASE/api/v1/embarques/logistica/resumen").body() } catch (e: Exception) { println("Error: ${e.message}") } finally { isLoading = false }
    }

    Div({ style { backgroundColor(Color.white); padding(24.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        if (isLoading) { P { Text("Cargando resumen...") } } else {
            Div({ style { display(DisplayStyle.Grid); property("grid-template-columns", "repeat(auto-fit, minmax(200.px, 1fr))"); gap(16.px) } }) {
                resumenCard("Pedidos Totales", resumen["pedidosTotal"] ?: "0")
                resumenCard("Pedidos Pendientes", resumen["pedidosPendientes"] ?: "0")
                resumenCard("En Preparación", resumen["pedidosEnPreparacion"] ?: "0")
                resumenCard("Listos para Embarque", resumen["pedidosListos"] ?: "0")
                resumenCard("Entregados", resumen["pedidosEntregados"] ?: "0")
                resumenCard("Rutas Totales", resumen["rutasTotal"] ?: "0")
                resumenCard("Rutas en Curso", resumen["rutasEnCurso"] ?: "0")
                resumenCard("Rutas Retrasadas", resumen["rutasRetrasadas"] ?: "0")
                resumenCard("Envíos Totales", resumen["enviosTotal"] ?: "0")
            }
        }
    }
}

private fun resumenCard(label: String, value: String) {
    Div({ style { backgroundColor(Color("#f8fafc")); padding(20.px); borderRadius(10.px); property("border", "1px solid #e2e8f0") } }) {
        P({ style { fontSize(12.px); color(Color("#64748b")); margin(0.px, 0.px, 8.px, 0.px) } }) { Text(label) }
        P({ style { fontSize(28.px); fontWeight("700"); color(Color("#0f172a")); margin(0.px) } }) { Text(value) }
    }
}

// ---------- PEDIDOS ----------
@Composable
private fun ShippingPedidosTab(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope, canEdit: Boolean) {
    val BASE = BACKEND_URL
    var items by remember { mutableStateOf(emptyList<Order>()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey) {
        isLoading = true
        try { items = client.get("$BASE/api/v1/embarques/pedidos").body() } catch (e: Exception) { println("Error: ${e.message}") } finally { isLoading = false }
    }
    fun refresh() { refreshKey++ }

    var f_numero by remember { mutableStateOf("") }
    var f_cliente by remember { mutableStateOf("") }
    var f_fechaPedido by remember { mutableStateOf("") }
    var f_fechaEntrega by remember { mutableStateOf("") }
    var f_modelo by remember { mutableStateOf("") }
    var f_cantidad by remember { mutableStateOf("") }
    var f_prioridad by remember { mutableStateOf("Normal") }
    var f_estado by remember { mutableStateOf("Pendiente") }
    var f_notas by remember { mutableStateOf("") }

    Div({ style { backgroundColor(Color.white); padding(24.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        H3({ style { margin(0.px, 0.px, 12.px, 0.px); fontSize(15.px) } }) { Text("Pedidos de Clientes") }
        if (canEdit) {
            Div({ style { display(DisplayStyle.Flex); gap(8.px); marginBottom(16.px); flexWrap(FlexWrap.Wrap); alignItems(AlignItems.Center) } }) {
                shipInput("No. Pedido *", f_numero) { f_numero = it }
                shipInput("Cliente *", f_cliente) { f_cliente = it }
                shipInput("Fecha Pedido", f_fechaPedido) { f_fechaPedido = it }
                shipInput("Fecha Entrega", f_fechaEntrega) { f_fechaEntrega = it }
                shipInput("Modelo", f_modelo) { f_modelo = it }
                shipInput("Cantidad", f_cantidad) { f_cantidad = it }
                shipInput("Prioridad", f_prioridad) { f_prioridad = it }
                shipInput("Estado", f_estado) { f_estado = it }
                shipInput("Notas", f_notas) { f_notas = it }
                Button({
                    style { padding(8.px, 16.px); backgroundColor(SidebarActiveColor); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") }
                    onClick {
                        if (f_numero.isNotBlank() && f_cliente.isNotBlank()) {
                            scope.launch {
                                client.post("$BASE/api/v1/embarques/pedidos") { contentType(ContentType.Application.Json); setBody(Order(numeroPedido = f_numero, cliente = f_cliente, fechaPedido = f_fechaPedido, fechaEntregaSolicitada = f_fechaEntrega, modelo = f_modelo, cantidad = f_cantidad, prioridad = f_prioridad, estado = f_estado, notas = f_notas)) }
                                f_numero = ""; f_cliente = ""; f_fechaPedido = ""; f_fechaEntrega = ""; f_modelo = ""; f_cantidad = ""; f_prioridad = "Normal"; f_estado = "Pendiente"; f_notas = ""; refresh()
                            }
                        }
                    }
                }) { Text("+ Agregar") }
            }
        }
        if (isLoading) { P { Text("Cargando...") } } else {
            Div({ style { overflowX("auto") } }) {
                Table({ style { width(100.percent); property("border-collapse", "collapse") } }) {
                    Thead { Tr {
                        Th { shipTh("Pedido") }; Th { shipTh("Cliente") }; Th { shipTh("F. Pedido") }; Th { shipTh("F. Entrega") }
                        Th { shipTh("Modelo") }; Th { shipTh("Cant.") }; Th { shipTh("Prioridad") }; Th { shipTh("Estado") }; Th { shipTh("Notas") }; Th { shipTh("") }
                    } }
                    Tbody {
                        items.forEach { row ->
                            Tr {
                                Td { shipTd(row.numeroPedido) }; Td { shipTd(row.cliente) }; Td { shipTd(row.fechaPedido) }; Td { shipTd(row.fechaEntregaSolicitada) }
                                Td { shipTd(row.modelo) }; Td { shipTd(row.cantidad) }; Td { shipTd(row.prioridad) }; Td { shipTd(row.estado) }; Td { shipTd(row.notas) }
                                if (canEdit) { Td { shipDeleteBtn(scope) { scope.launch { client.delete("$BASE/api/v1/embarques/pedidos/${row.id}"); refresh() } } } }
                            }
                        }
                    }
                }
            }
            P({ style { fontSize(12.px); color(Color("#64748b")); marginTop(12.px) } }) { Text("${items.size} pedidos") }
        }
    }
}

// ---------- RUTAS ----------
@Composable
private fun ShippingRutasTab(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope, canEdit: Boolean) {
    val BASE = BACKEND_URL
    var items by remember { mutableStateOf(emptyList<DeliveryRoute>()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey) {
        isLoading = true
        try { items = client.get("$BASE/api/v1/embarques/rutas").body() } catch (e: Exception) { println("Error: ${e.message}") } finally { isLoading = false }
    }
    fun refresh() { refreshKey++ }

    var f_nombre by remember { mutableStateOf("") }
    var f_fechaSalida by remember { mutableStateOf("") }
    var f_fechaEntrega by remember { mutableStateOf("") }
    var f_conductor by remember { mutableStateOf("") }
    var f_vehiculo by remember { mutableStateOf("") }
    var f_placa by remember { mutableStateOf("") }
    var f_paradas by remember { mutableStateOf("") }
    var f_pedidos by remember { mutableStateOf("") }
    var f_estado by remember { mutableStateOf("Programada") }
    var f_notas by remember { mutableStateOf("") }

    Div({ style { backgroundColor(Color.white); padding(24.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        H3({ style { margin(0.px, 0.px, 12.px, 0.px); fontSize(15.px) } }) { Text("Rutas de Entrega") }
        if (canEdit) {
            Div({ style { display(DisplayStyle.Flex); gap(8.px); marginBottom(16.px); flexWrap(FlexWrap.Wrap); alignItems(AlignItems.Center) } }) {
                shipInput("Nombre Ruta *", f_nombre) { f_nombre = it }
                shipInput("Fecha Salida", f_fechaSalida) { f_fechaSalida = it }
                shipInput("Fecha Entrega", f_fechaEntrega) { f_fechaEntrega = it }
                shipInput("Conductor", f_conductor) { f_conductor = it }
                shipInput("Vehículo", f_vehiculo) { f_vehiculo = it }
                shipInput("Placa", f_placa) { f_placa = it }
                shipInput("Paradas", f_paradas) { f_paradas = it }
                shipInput("Pedidos Asoc.", f_pedidos) { f_pedidos = it }
                shipInput("Estado", f_estado) { f_estado = it }
                shipInput("Notas", f_notas) { f_notas = it }
                Button({
                    style { padding(8.px, 16.px); backgroundColor(SidebarActiveColor); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") }
                    onClick {
                        if (f_nombre.isNotBlank()) {
                            scope.launch {
                                client.post("$BASE/api/v1/embarques/rutas") { contentType(ContentType.Application.Json); setBody(DeliveryRoute(nombreRuta = f_nombre, fechaSalida = f_fechaSalida, fechaEntregaEstimada = f_fechaEntrega, conductor = f_conductor, vehiculo = f_vehiculo, placa = f_placa, paradas = f_paradas, pedidosAsociados = f_pedidos, estado = f_estado, notas = f_notas)) }
                                f_nombre = ""; f_fechaSalida = ""; f_fechaEntrega = ""; f_conductor = ""; f_vehiculo = ""; f_placa = ""; f_paradas = ""; f_pedidos = ""; f_estado = "Programada"; f_notas = ""; refresh()
                            }
                        }
                    }
                }) { Text("+ Agregar") }
            }
        }
        if (isLoading) { P { Text("Cargando...") } } else {
            Div({ style { overflowX("auto") } }) {
                Table({ style { width(100.percent); property("border-collapse", "collapse") } }) {
                    Thead { Tr {
                        Th { shipTh("Ruta") }; Th { shipTh("Salida") }; Th { shipTh("Entrega Est.") }; Th { shipTh("Conductor") }
                        Th { shipTh("Vehículo") }; Th { shipTh("Placa") }; Th { shipTh("Paradas") }; Th { shipTh("Pedidos") }; Th { shipTh("Estado") }; Th { shipTh("") }
                    } }
                    Tbody {
                        items.forEach { row ->
                            Tr {
                                Td { shipTd(row.nombreRuta) }; Td { shipTd(row.fechaSalida) }; Td { shipTd(row.fechaEntregaEstimada) }
                                Td { shipTd(row.conductor) }; Td { shipTd(row.vehiculo) }; Td { shipTd(row.placa) }
                                Td { shipTd(row.paradas) }; Td { shipTd(row.pedidosAsociados) }; Td { shipTd(row.estado) }
                                if (canEdit) { Td { shipDeleteBtn(scope) { scope.launch { client.delete("$BASE/api/v1/embarques/rutas/${row.id}"); refresh() } } } }
                            }
                        }
                    }
                }
            }
            P({ style { fontSize(12.px); color(Color("#64748b")); marginTop(12.px) } }) { Text("${items.size} rutas") }
        }
    }
}

// ---------- TRAZABILIDAD ----------
@Composable
private fun ShippingTrazabilidadTab(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope, canEdit: Boolean) {
    val BASE = BACKEND_URL
    var items by remember { mutableStateOf(emptyList<TraceabilityEvent>()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey) {
        isLoading = true
        try { items = client.get("$BASE/api/v1/embarques/trazabilidad").body() } catch (e: Exception) { println("Error: ${e.message}") } finally { isLoading = false }
    }
    fun refresh() { refreshKey++ }

    var f_referencia by remember { mutableStateOf("") }
    var f_tipoRef by remember { mutableStateOf("Pedido") }
    var f_evento by remember { mutableStateOf("") }
    var f_fecha by remember { mutableStateOf("") }
    var f_ubicacion by remember { mutableStateOf("") }
    var f_responsable by remember { mutableStateOf("") }
    var f_notas by remember { mutableStateOf("") }

    Div({ style { backgroundColor(Color.white); padding(24.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        H3({ style { margin(0.px, 0.px, 12.px, 0.px); fontSize(15.px) } }) { Text("Bitácora de Trazabilidad") }
        if (canEdit) {
            Div({ style { display(DisplayStyle.Flex); gap(8.px); marginBottom(16.px); flexWrap(FlexWrap.Wrap); alignItems(AlignItems.Center) } }) {
                shipInput("Referencia *", f_referencia) { f_referencia = it }
                shipInput("Tipo Ref.", f_tipoRef) { f_tipoRef = it }
                shipInput("Evento", f_evento) { f_evento = it }
                shipInput("Fecha", f_fecha) { f_fecha = it }
                shipInput("Ubicación", f_ubicacion) { f_ubicacion = it }
                shipInput("Responsable", f_responsable) { f_responsable = it }
                shipInput("Notas", f_notas) { f_notas = it }
                Button({
                    style { padding(8.px, 16.px); backgroundColor(SidebarActiveColor); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") }
                    onClick {
                        if (f_referencia.isNotBlank()) {
                            scope.launch {
                                client.post("$BASE/api/v1/embarques/trazabilidad") { contentType(ContentType.Application.Json); setBody(TraceabilityEvent(referencia = f_referencia, tipoReferencia = f_tipoRef, evento = f_evento, fecha = f_fecha, ubicacion = f_ubicacion, responsable = f_responsable, notas = f_notas)) }
                                f_referencia = ""; f_tipoRef = "Pedido"; f_evento = ""; f_fecha = ""; f_ubicacion = ""; f_responsable = ""; f_notas = ""; refresh()
                            }
                        }
                    }
                }) { Text("+ Agregar") }
            }
        }
        if (isLoading) { P { Text("Cargando...") } } else {
            Div({ style { overflowX("auto") } }) {
                Table({ style { width(100.percent); property("border-collapse", "collapse") } }) {
                    Thead { Tr {
                        Th { shipTh("Referencia") }; Th { shipTh("Tipo") }; Th { shipTh("Evento") }; Th { shipTh("Fecha") }
                        Th { shipTh("Ubicación") }; Th { shipTh("Responsable") }; Th { shipTh("Notas") }; Th { shipTh("") }
                    } }
                    Tbody {
                        items.forEach { row ->
                            Tr {
                                Td { shipTd(row.referencia) }; Td { shipTd(row.tipoReferencia) }; Td { shipTd(row.evento) }
                                Td { shipTd(row.fecha) }; Td { shipTd(row.ubicacion) }; Td { shipTd(row.responsable) }; Td { shipTd(row.notas) }
                                if (canEdit) { Td { shipDeleteBtn(scope) { scope.launch { client.delete("$BASE/api/v1/embarques/trazabilidad/${row.id}"); refresh() } } } }
                            }
                        }
                    }
                }
            }
            P({ style { fontSize(12.px); color(Color("#64748b")); marginTop(12.px) } }) { Text("${items.size} eventos") }
        }
    }
}

// Helpers
private inline fun shipInput(label: String, value: String, crossinline onChange: (String) -> Unit) {
    Input(InputType.Text) {
        placeholder(label)
        value(value)
        onInput { onChange(it.value) }
        style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(140.px) }
    }
}

private fun shipTh(label: String) {
    Th({ style { padding(8.px, 6.px); textAlign(TextAlign.Left); fontSize(12.px); color(Color("#64748b")); borderBottom(2.px, LineStyle.Solid, Color("#e2e8f0")) } }) { Text(label) }
}

private fun shipTd(text: String) {
    Td({ style { padding(8.px, 6.px); fontSize(13.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(text) }
}

private inline fun shipDeleteBtn(scope: kotlinx.coroutines.CoroutineScope, crossinline action: () -> Unit) {
    Button({
        style { padding(4.px, 8.px); backgroundColor(Color("#ef4444")); color(Color.white); property("border", "none"); borderRadius(4.px); cursor("pointer"); fontSize(12.px) }
        onClick { action() }
    }) { Text("✕") }
}
