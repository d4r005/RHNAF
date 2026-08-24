
import androidx.compose.runtime.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.attributes.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.call.*
import io.ktor.http.*
import com.example.rhnaf.shared.model.*
import com.example.rhnaf.shared.model.Shipment
import kotlinx.coroutines.launch

private enum class ShippingTab { RESUMEN, PEDIDOS, RUTAS, TRAZABILIDAD, ENVIOS, CHINA }

@Composable
fun ShippingModule(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope, t: Translations, userRole: UserRole) {
    var activeTab by remember { mutableStateOf(ShippingTab.RESUMEN) }
    val canEdit = userRole == UserRole.ALMACEN || userRole == UserRole.ADMIN

    Div({ style { padding(24.px) } }) {
        Div({ style { display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); alignItems(AlignItems.Center); marginBottom(20.px) } }) {
            H2({ style { margin(0.px); fontSize(20.px); color(Color("#0f172a")) } }) { Text("Embarques y Logística") }
            if (!canEdit) {
                Span({ style { fontSize(12.px); color(Color("#64748b")); backgroundColor(Color("#f1f5f9")); padding(4.px, 12.px); borderRadius(6.px) } }) { Text("Modo solo lectura") }
            }
        }

        Div({ style { display(DisplayStyle.Flex); gap(4.px); marginBottom(20.px); property("border-bottom", "1px solid #e2e8f0") } }) {
            ShippingTab.values().forEach { tab ->
                val label = when (tab) {
                    ShippingTab.RESUMEN -> "Resumen"
                    ShippingTab.PEDIDOS -> "Pedidos"
                    ShippingTab.RUTAS -> "Rutas de Entrega"
                    ShippingTab.TRAZABILIDAD -> "Trazabilidad"
                    ShippingTab.ENVIOS -> "Envíos Detallados"
                    ShippingTab.CHINA -> "Contenedores China"
                }
                Button({
                    style {
                        padding(8.px, 16.px); property("border", "none")
                        backgroundColor(if (activeTab == tab) SidebarActiveColor else Color.transparent)
                        color(if (activeTab == tab) Color.white else Color("#64748b"))
                        fontSize(13.px); fontWeight(if (activeTab == tab) "600" else "400"); cursor("pointer")
                        borderRadius(6.px, 6.px, 0.px, 0.px)
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
            ShippingTab.ENVIOS -> ShippingEnviosTab(client, scope, canEdit)
            ShippingTab.CHINA -> ContenedoresChinaTab(client, scope, canEdit)
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
        isLoading = true
        try { resumen = client.get("$BASE/api/v1/embarques/logistica/resumen").body() } catch (e: Exception) { println("Error: ${e.message}") } finally { isLoading = false }
    }

    Div({ style { backgroundColor(Color.white); padding(24.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        H3({ style { margin(0.px, 0.px, 12.px, 0.px); fontSize(15.px) } }) { Text("Resumen Logístico") }
        if (isLoading) { P { Text("Cargando...") } } else {
            Div({ style { display(DisplayStyle.Flex); gap(16.px); flexWrap(FlexWrap.Wrap) } }) {
                resumenCard("Pedidos Totales", resumen["pedidosTotal"] ?: "0")
                resumenCard("Rutas Activas", resumen["rutasActivas"] ?: "0")
                resumenCard("Rutas Completadas", resumen["rutasCompletadas"] ?: "0")
                resumenCard("Envíos Totales", resumen["enviosTotal"] ?: "0")
            }
        }
    }
}

@Composable
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
                        shipTh("Pedido"); shipTh("Cliente"); shipTh("F. Pedido"); shipTh("F. Entrega")
                        shipTh("Modelo"); shipTh("Cant."); shipTh("Prioridad"); shipTh("Estado"); shipTh("Notas"); shipTh("")
                    } }
                    Tbody {
                        items.forEach { row ->
                            Tr {
                                shipTd(row.numeroPedido); shipTd(row.cliente); shipTd(row.fechaPedido); shipTd(row.fechaEntregaSolicitada)
                                shipTd(row.modelo); shipTd(row.cantidad); shipTd(row.prioridad); shipTd(row.estado); shipTd(row.notas)
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
                        shipTh("Ruta"); shipTh("Salida"); shipTh("Entrega Est."); shipTh("Conductor")
                        shipTh("Vehículo"); shipTh("Placa"); shipTh("Paradas"); shipTh("Pedidos"); shipTh("Estado"); shipTh("")
                    } }
                    Tbody {
                        items.forEach { row ->
                            Tr {
                                shipTd(row.nombreRuta); shipTd(row.fechaSalida); shipTd(row.fechaEntregaEstimada)
                                shipTd(row.conductor); shipTd(row.vehiculo); shipTd(row.placa)
                                shipTd(row.paradas); shipTd(row.pedidosAsociados); shipTd(row.estado)
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
                        shipTh("Referencia"); shipTh("Tipo"); shipTh("Evento"); shipTh("Fecha")
                        shipTh("Ubicación"); shipTh("Responsable"); shipTh("Notas"); shipTh("")
                    } }
                    Tbody {
                        items.forEach { row ->
                            Tr {
                                shipTd(row.referencia); shipTd(row.tipoReferencia); shipTd(row.evento)
                                shipTd(row.fecha); shipTd(row.ubicacion); shipTd(row.responsable); shipTd(row.notas)
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

// ---------- ENVIOS DETALLADOS (FD, SF, AJ, EVF, RBT) ----------
@Composable
private fun ShippingEnviosTab(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope, canEdit: Boolean) {
    val BASE = BACKEND_URL
    var items by remember { mutableStateOf(emptyList<Shipment>()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey) {
        isLoading = true
        try { items = client.get("$BASE/api/v1/almacen/envios").body() } catch (e: Exception) { println("Error: ${e.message}") } finally { isLoading = false }
    }
    fun refresh() { refreshKey++ }

    var f_cliente by remember { mutableStateOf("") }
    var f_fechaCarga by remember { mutableStateOf("") }
    var f_poContenedor by remember { mutableStateOf("") }
    var f_sku by remember { mutableStateOf("") }
    var f_nombreProducto by remember { mutableStateOf("") }
    var f_numeroSello by remember { mutableStateOf("") }
    var f_placa by remember { mutableStateOf("") }
    var f_cantidad by remember { mutableStateOf("") }
    var f_gabinetes by remember { mutableStateOf("") }
    var f_conductor by remember { mutableStateOf("") }
    var f_horaInicio by remember { mutableStateOf("") }
    var f_horaFin by remember { mutableStateOf("") }
    var f_operador by remember { mutableStateOf("") }
    var f_inspector by remember { mutableStateOf("") }

    Div({ style { backgroundColor(Color.white); padding(24.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        H3({ style { margin(0.px, 0.px, 12.px, 0.px); fontSize(15.px) } }) { Text("Envíos Detallados (FD, SF, AJ, EVF, RBT)") }
        if (canEdit) {
            Div({ style { display(DisplayStyle.Flex); gap(8.px); marginBottom(16.px); flexWrap(FlexWrap.Wrap); alignItems(AlignItems.Center) } }) {
                shipInput("Cliente *", f_cliente) { f_cliente = it }
                shipInput("Fecha Carga", f_fechaCarga) { f_fechaCarga = it }
                shipInput("PO/Contenedor", f_poContenedor) { f_poContenedor = it }
                shipInput("SKU", f_sku) { f_sku = it }
                shipInput("Producto", f_nombreProducto) { f_nombreProducto = it }
                shipInput("Sello", f_numeroSello) { f_numeroSello = it }
                shipInput("Placa", f_placa) { f_placa = it }
                shipInput("Cantidad", f_cantidad) { f_cantidad = it }
                shipInput("Gabinetes", f_gabinetes) { f_gabinetes = it }
                shipInput("Conductor", f_conductor) { f_conductor = it }
                shipInput("Hora Inicio", f_horaInicio) { f_horaInicio = it }
                shipInput("Hora Fin", f_horaFin) { f_horaFin = it }
                shipInput("Operador", f_operador) { f_operador = it }
                shipInput("Inspector", f_inspector) { f_inspector = it }
                Button({
                    style { padding(8.px, 16.px); backgroundColor(SidebarActiveColor); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") }
                    onClick {
                        if (f_cliente.isNotBlank()) { scope.launch {
                            client.post("$BASE/api/v1/almacen/envios") { contentType(ContentType.Application.Json); setBody(Shipment(cliente = f_cliente, fechaCarga = f_fechaCarga, poContenedor = f_poContenedor, sku = f_sku, nombreProducto = f_nombreProducto, numeroSello = f_numeroSello, placa = f_placa, cantidad = f_cantidad, gabinetes = f_gabinetes, conductor = f_conductor, horaInicio = f_horaInicio, horaFin = f_horaFin, operador = f_operador, inspector = f_inspector)) }
                            f_cliente = ""; f_fechaCarga = ""; f_poContenedor = ""; f_sku = ""; f_nombreProducto = ""; f_numeroSello = ""; f_placa = ""; f_cantidad = ""; f_gabinetes = ""; f_conductor = ""; f_horaInicio = ""; f_horaFin = ""; f_operador = ""; f_inspector = ""; refresh()
                        } }
                    }
                }) { Text("+ Agregar") }
            }
        }
        if (isLoading) { P { Text("Cargando...") } } else {
            Div({ style { overflowX("auto") } }) {
                Table({ style { width(100.percent); property("border-collapse", "collapse") } }) {
                    Thead { Tr {
                        shipTh("Cliente"); shipTh("Fecha"); shipTh("PO"); shipTh("SKU")
                        shipTh("Producto"); shipTh("Sello"); shipTh("Placa"); shipTh("Cant.")
                        shipTh("Gab."); shipTh("Conductor"); shipTh("Inicio"); shipTh("Fin")
                        shipTh("Operador"); shipTh("Inspector"); shipTh("")
                    } }
                    Tbody {
                        items.forEach { row ->
                            Tr {
                                shipTd(row.cliente); shipTd(row.fechaCarga); shipTd(row.poContenedor); shipTd(row.sku)
                                shipTd(row.nombreProducto); shipTd(row.numeroSello); shipTd(row.placa); shipTd(row.cantidad)
                                shipTd(row.gabinetes); shipTd(row.conductor); shipTd(row.horaInicio); shipTd(row.horaFin)
                                shipTd(row.operador); shipTd(row.inspector)
                                if (canEdit) { Td { shipDeleteBtn(scope) { scope.launch { client.delete("$BASE/api/v1/almacen/envios/${row.id}"); refresh() } } } }
                            }
                        }
                    }
                }
            }
            P({ style { fontSize(12.px); color(Color("#64748b")); marginTop(12.px) } }) { Text("${items.size} registros") }
        }
    }
}

// Helpers
@Composable
private fun shipInput(label: String, value: String, onChange: (String) -> Unit) {
    Input(InputType.Text) {
        placeholder(label)
        value(value)
        onInput { onChange(it.value) }
        style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(140.px) }
    }
}

@Composable
private fun shipTh(label: String) {
    Th({ style { padding(8.px, 6.px); textAlign("left"); fontSize(12.px); color(Color("#64748b")); property("border-bottom", "2px solid #e2e8f0") } }) { Text(label) }
}

@Composable
private fun shipTd(text: String) {
    Td({ style { padding(8.px, 6.px); fontSize(13.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(text) }
}

@Composable
private fun shipDeleteBtn(scope: kotlinx.coroutines.CoroutineScope, action: () -> Unit) {
    Button({
        style { padding(4.px, 8.px); backgroundColor(Color("#ef4444")); color(Color.white); property("border", "none"); borderRadius(4.px); cursor("pointer"); fontSize(12.px) }
        onClick { action() }
    }) { Text("✕") }
}
