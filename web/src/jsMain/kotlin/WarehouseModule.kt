import androidx.compose.runtime.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.attributes.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.call.*
import io.ktor.http.*
import com.example.rhnaf.shared.model.*
import kotlinx.coroutines.launch
import kotlinx.browser.window

// ============================================================
// Modulo ALMACEN: inventario, entradas, salidas, ubicaciones,
// auditorias y envios detallados. Todo con pestañas.
// Permisos: ALMACEN y ADMIN pueden editar; IMPORT_EXPORT y FINANZAS
// solo pueden ver (readOnly = true).
// ============================================================

private enum class WarehouseTab { INVENTARIO, ENTRADAS, SALIDAS, UBICACIONES, AUDITORIAS, ENVIOS }

@Composable
fun WarehouseModule(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope, t: Translations, userRole: UserRole) {
    val canEdit = userRole == UserRole.ADMIN || userRole == UserRole.ALMACEN
    var activeTab by remember { mutableStateOf(WarehouseTab.INVENTARIO) }

    Div({ style { padding(24.px) } }) {
        Div({ style { display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); alignItems(AlignItems.Center); marginBottom(20.px) } }) {
            H2({ style { margin(0.px); fontSize(20.px); color(Color("#0f172a")) } }) { Text("Almacén e Inventarios") }
            if (!canEdit) {
                Span({ style { fontSize(12.px); color(Color("#64748b")); backgroundColor(Color("#f1f5f9")); padding(4.px, 12.px); borderRadius(6.px) } }) { Text("Modo solo lectura") }
            }
        }

        Div({ style { display(DisplayStyle.Flex); gap(4.px); marginBottom(20.px); borderBottom(1.px, LineStyle.Solid, Color("#e2e8f0")) } }) {
            WarehouseTab.values().forEach { tab ->
                val label = when (tab) {
                    WarehouseTab.INVENTARIO -> "Inventario"
                    WarehouseTab.ENTRADAS -> "Entradas"
                    WarehouseTab.SALIDAS -> "Salidas"
                    WarehouseTab.UBICACIONES -> "Ubicaciones"
                    WarehouseTab.AUDITORIAS -> "Auditorías"
                    WarehouseTab.ENVIOS -> "Envíos Detallados"
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
            WarehouseTab.INVENTARIO -> WarehouseInventoryTab(client, scope, canEdit)
            WarehouseTab.ENTRADAS -> WarehouseIncomingTab(client, scope, canEdit)
            WarehouseTab.SALIDAS -> WarehouseOutgoingTab(client, scope, canEdit)
            WarehouseTab.UBICACIONES -> WarehouseLocationsTab(client, scope, canEdit)
            WarehouseTab.AUDITORIAS -> WarehouseAuditsTab(client, scope, canEdit)
            WarehouseTab.ENVIOS -> WarehouseShipmentsTab(client, scope, canEdit)
        }
    }
}

private fun inputStyle(): org.jetbrains.compose.web.css.CSSBuilder.() -> Unit = {
    padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(140.px)
}

private fun btnAddStyle(): org.jetbrains.compose.web.css.CSSBuilder.() -> Unit = {
    padding(8.px, 16.px); backgroundColor(SidebarActiveColor); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer")
}

private fun btnDelStyle(): org.jetbrains.compose.web.css.CSSBuilder.() -> Unit = {
    padding(4.px, 8.px); backgroundColor(Color("#ef4444")); color(Color.white); property("border", "none"); borderRadius(4.px); cursor("pointer"); fontSize(12.px)
}

// ---------- INVENTARIO ----------
@Composable
private fun WarehouseInventoryTab(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope, canEdit: Boolean) {
    val BASE = BACKEND_URL
    var items by remember { mutableStateOf(emptyList<WarehouseInventoryItem>()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey) {
        isLoading = true
        try { items = client.get("$BASE/api/v1/almacen/inventario").body() } catch (e: Exception) { println("Error: ${e.message}") } finally { isLoading = false }
    }
    fun refresh() { refreshKey++ }

    var f_lugar by remember { mutableStateOf("") }
    var f_po by remember { mutableStateOf("") }
    var f_modelo by remember { mutableStateOf("") }
    var f_cantidad by remember { mutableStateOf("") }
    var f_falta by remember { mutableStateOf("") }
    var f_existencia by remember { mutableStateOf("") }

    Div({ style { backgroundColor(Color.white); padding(24.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        H3({ style { margin(0.px, 0.px, 12.px, 0.px); fontSize(15.px) } }) { Text("Inventario de Producto Terminado por Ubicación") }
        if (canEdit) {
            Div({ style { display(DisplayStyle.Flex); gap(8.px); marginBottom(16.px); flexWrap(FlexWrap.Wrap); alignItems(AlignItems.Center) } }) {
                Input(InputType.Text) { placeholder("Lugar *"); value(f_lugar); onInput { f_lugar = it.value }; style(inputStyle()) }
                Input(InputType.Text) { placeholder("PO"); value(f_po); onInput { f_po = it.value }; style(inputStyle()) }
                Input(InputType.Text) { placeholder("Modelo"); value(f_modelo); onInput { f_modelo = it.value }; style(inputStyle()) }
                Input(InputType.Text) { placeholder("Cantidad"); value(f_cantidad); onInput { f_cantidad = it.value }; style(inputStyle()) }
                Input(InputType.Text) { placeholder("Falta"); value(f_falta); onInput { f_falta = it.value }; style(inputStyle()) }
                Input(InputType.Text) { placeholder("Existencia"); value(f_existencia); onInput { f_existencia = it.value }; style(inputStyle()) }
                Button({
                    style(btnAddStyle())
                    onClick {
                        if (f_lugar.isNotBlank()) { scope.launch {
                            client.post("$BASE/api/v1/almacen/inventario") { contentType(ContentType.Application.Json); setBody(WarehouseInventoryItem(lugar = f_lugar, po = f_po, modelo = f_modelo, cantidad = f_cantidad, falta = f_falta, existencia = f_existencia)) }
                            f_lugar = ""; f_po = ""; f_modelo = ""; f_cantidad = ""; f_falta = ""; f_existencia = ""; refresh()
                        } }
                    }
                }) { Text("+ Agregar") }
            }
        }
        if (isLoading) { P { Text("Cargando...") } } else {
            Div({ style { overflowX("auto") } }) {
                Table({ style { width(100.percent); property("border-collapse", "collapse") } }) {
                    Thead { Tr {
                        Th({ style { padding(8.px, 6.px); textAlign(TextAlign.Left); fontSize(12.px); color(Color("#64748b")); borderBottom(2.px, LineStyle.Solid, Color("#e2e8f0")) } }) { Text("Lugar") }
                        Th({ style { padding(8.px, 6.px); textAlign(TextAlign.Left); fontSize(12.px); color(Color("#64748b")); borderBottom(2.px, LineStyle.Solid, Color("#e2e8f0")) } }) { Text("PO") }
                        Th({ style { padding(8.px, 6.px); textAlign(TextAlign.Left); fontSize(12.px); color(Color("#64748b")); borderBottom(2.px, LineStyle.Solid, Color("#e2e8f0")) } }) { Text("Modelo") }
                        Th({ style { padding(8.px, 6.px); textAlign(TextAlign.Left); fontSize(12.px); color(Color("#64748b")); borderBottom(2.px, LineStyle.Solid, Color("#e2e8f0")) } }) { Text("Cantidad") }
                        Th({ style { padding(8.px, 6.px); textAlign(TextAlign.Left); fontSize(12.px); color(Color("#64748b")); borderBottom(2.px, LineStyle.Solid, Color("#e2e8f0")) } }) { Text("Falta") }
                        Th({ style { padding(8.px, 6.px); textAlign(TextAlign.Left); fontSize(12.px); color(Color("#64748b")); borderBottom(2.px, LineStyle.Solid, Color("#e2e8f0")) } }) { Text("Existencia") }
                        Th { Text("") }
                    } }
                    Tbody {
                        items.forEach { row ->
                            Tr {
                                Td({ style { padding(8.px, 6.px); fontSize(13.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(row.lugar) }
                                Td({ style { padding(8.px, 6.px); fontSize(13.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(row.po) }
                                Td({ style { padding(8.px, 6.px); fontSize(13.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(row.modelo) }
                                Td({ style { padding(8.px, 6.px); fontSize(13.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(row.cantidad) }
                                Td({ style { padding(8.px, 6.px); fontSize(13.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(row.falta) }
                                Td({ style { padding(8.px, 6.px); fontSize(13.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(row.existencia) }
                                if (canEdit) { Td { Button({ style(btnDelStyle()); onClick { scope.launch { client.delete("$BASE/api/v1/almacen/inventario/${row.id}"); refresh() } } }) { Text("✕") } } }
                            }
                        }
                    }
                }
            }
            P({ style { fontSize(12.px); color(Color("#64748b")); marginTop(12.px) } }) { Text("${items.size} registros") }
        }
    }
}

// ---------- ENTRADAS ----------
@Composable
private fun WarehouseIncomingTab(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope, canEdit: Boolean) {
    val BASE = BACKEND_URL
    var items by remember { mutableStateOf(emptyList<WarehouseIncomingLog>()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey) {
        isLoading = true
        try { items = client.get("$BASE/api/v1/almacen/entradas").body() } catch (e: Exception) { println("Error: ${e.message}") } finally { isLoading = false }
    }
    fun refresh() { refreshKey++ }

    var f_fecha by remember { mutableStateOf("") }
    var f_po by remember { mutableStateOf("") }
    var f_modelo by remember { mutableStateOf("") }
    var f_cantidad by remember { mutableStateOf("") }
    var f_ubicacion by remember { mutableStateOf("") }

    Div({ style { backgroundColor(Color.white); padding(24.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        H3({ style { margin(0.px, 0.px, 12.px, 0.px); fontSize(15.px) } }) { Text("Bitácora de Entradas al Almacén") }
        if (canEdit) {
            Div({ style { display(DisplayStyle.Flex); gap(8.px); marginBottom(16.px); flexWrap(FlexWrap.Wrap); alignItems(AlignItems.Center) } }) {
                Input(InputType.Text) { placeholder("Fecha"); value(f_fecha); onInput { f_fecha = it.value }; style(inputStyle()) }
                Input(InputType.Text) { placeholder("PO"); value(f_po); onInput { f_po = it.value }; style(inputStyle()) }
                Input(InputType.Text) { placeholder("Modelo"); value(f_modelo); onInput { f_modelo = it.value }; style(inputStyle()) }
                Input(InputType.Text) { placeholder("Cantidad"); value(f_cantidad); onInput { f_cantidad = it.value }; style(inputStyle()) }
                Input(InputType.Text) { placeholder("Ubicación"); value(f_ubicacion); onInput { f_ubicacion = it.value }; style(inputStyle()) }
                Button({
                    style(btnAddStyle())
                    onClick { scope.launch {
                        client.post("$BASE/api/v1/almacen/entradas") { contentType(ContentType.Application.Json); setBody(WarehouseIncomingLog(fecha = f_fecha, po = f_po, modelo = f_modelo, cantidad = f_cantidad, ubicacion = f_ubicacion)) }
                        f_fecha = ""; f_po = ""; f_modelo = ""; f_cantidad = ""; f_ubicacion = ""; refresh()
                    } }
                }) { Text("+ Agregar") }
            }
        }
        if (isLoading) { P { Text("Cargando...") } } else {
            Div({ style { overflowX("auto") } }) {
                Table({ style { width(100.percent); property("border-collapse", "collapse") } }) {
                    Thead { Tr {
                        Th({ style { padding(8.px, 6.px); textAlign(TextAlign.Left); fontSize(12.px); color(Color("#64748b")); borderBottom(2.px, LineStyle.Solid, Color("#e2e8f0")) } }) { Text("Fecha") }
                        Th({ style { padding(8.px, 6.px); textAlign(TextAlign.Left); fontSize(12.px); color(Color("#64748b")); borderBottom(2.px, LineStyle.Solid, Color("#e2e8f0")) } }) { Text("PO") }
                        Th({ style { padding(8.px, 6.px); textAlign(TextAlign.Left); fontSize(12.px); color(Color("#64748b")); borderBottom(2.px, LineStyle.Solid, Color("#e2e8f0")) } }) { Text("Modelo") }
                        Th({ style { padding(8.px, 6.px); textAlign(TextAlign.Left); fontSize(12.px); color(Color("#64748b")); borderBottom(2.px, LineStyle.Solid, Color("#e2e8f0")) } }) { Text("Cantidad") }
                        Th({ style { padding(8.px, 6.px); textAlign(TextAlign.Left); fontSize(12.px); color(Color("#64748b")); borderBottom(2.px, LineStyle.Solid, Color("#e2e8f0")) } }) { Text("Ubicación") }
                        Th { Text("") }
                    } }
                    Tbody {
                        items.forEach { row ->
                            Tr {
                                Td({ style { padding(8.px, 6.px); fontSize(13.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(row.fecha) }
                                Td({ style { padding(8.px, 6.px); fontSize(13.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(row.po) }
                                Td({ style { padding(8.px, 6.px); fontSize(13.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(row.modelo) }
                                Td({ style { padding(8.px, 6.px); fontSize(13.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(row.cantidad) }
                                Td({ style { padding(8.px, 6.px); fontSize(13.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(row.ubicacion) }
                                if (canEdit) { Td { Button({ style(btnDelStyle()); onClick { scope.launch { client.delete("$BASE/api/v1/almacen/entradas/${row.id}"); refresh() } } }) { Text("✕") } } }
                            }
                        }
                    }
                }
            }
            P({ style { fontSize(12.px); color(Color("#64748b")); marginTop(12.px) } }) { Text("${items.size} registros") }
        }
    }
}

// ---------- SALIDAS ----------
@Composable
private fun WarehouseOutgoingTab(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope, canEdit: Boolean) {
    val BASE = BACKEND_URL
    var items by remember { mutableStateOf(emptyList<WarehouseOutgoingLog>()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey) {
        isLoading = true
        try { items = client.get("$BASE/api/v1/almacen/salidas").body() } catch (e: Exception) { println("Error: ${e.message}") } finally { isLoading = false }
    }
    fun refresh() { refreshKey++ }

    var f_fecha by remember { mutableStateOf("") }
    var f_po by remember { mutableStateOf("") }
    var f_modelo by remember { mutableStateOf("") }
    var f_cantidad by remember { mutableStateOf("") }
    var f_ubicacion by remember { mutableStateOf("") }
    var f_motivo by remember { mutableStateOf("") }
    var f_responsable by remember { mutableStateOf("") }

    Div({ style { backgroundColor(Color.white); padding(24.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        H3({ style { margin(0.px, 0.px, 12.px, 0.px); fontSize(15.px) } }) { Text("Salidas de Almacén (Merma, Consumo Interno, Devoluciones)") }
        if (canEdit) {
            Div({ style { display(DisplayStyle.Flex); gap(8.px); marginBottom(16.px); flexWrap(FlexWrap.Wrap); alignItems(AlignItems.Center) } }) {
                Input(InputType.Text) { placeholder("Fecha"); value(f_fecha); onInput { f_fecha = it.value }; style(inputStyle()) }
                Input(InputType.Text) { placeholder("PO"); value(f_po); onInput { f_po = it.value }; style(inputStyle()) }
                Input(InputType.Text) { placeholder("Modelo"); value(f_modelo); onInput { f_modelo = it.value }; style(inputStyle()) }
                Input(InputType.Text) { placeholder("Cantidad"); value(f_cantidad); onInput { f_cantidad = it.value }; style(inputStyle()) }
                Input(InputType.Text) { placeholder("Ubicación"); value(f_ubicacion); onInput { f_ubicacion = it.value }; style(inputStyle()) }
                Input(InputType.Text) { placeholder("Motivo"); value(f_motivo); onInput { f_motivo = it.value }; style(inputStyle()) }
                Input(InputType.Text) { placeholder("Responsable"); value(f_responsable); onInput { f_responsable = it.value }; style(inputStyle()) }
                Button({
                    style(btnAddStyle())
                    onClick { scope.launch {
                        client.post("$BASE/api/v1/almacen/salidas") { contentType(ContentType.Application.Json); setBody(WarehouseOutgoingLog(fecha = f_fecha, po = f_po, modelo = f_modelo, cantidad = f_cantidad, ubicacion = f_ubicacion, motivo = f_motivo, responsable = f_responsable)) }
                        f_fecha = ""; f_po = ""; f_modelo = ""; f_cantidad = ""; f_ubicacion = ""; f_motivo = ""; f_responsable = ""; refresh()
                    } }
                }) { Text("+ Agregar") }
            }
        }
        if (isLoading) { P { Text("Cargando...") } } else {
            Div({ style { overflowX("auto") } }) {
                Table({ style { width(100.percent); property("border-collapse", "collapse") } }) {
                    Thead { Tr {
                        listOf("Fecha", "PO", "Modelo", "Cantidad", "Ubicación", "Motivo", "Responsable").forEach { h ->
                            Th({ style { padding(8.px, 6.px); textAlign(TextAlign.Left); fontSize(12.px); color(Color("#64748b")); borderBottom(2.px, LineStyle.Solid, Color("#e2e8f0")) } }) { Text(h) }
                        }
                        Th { Text("") }
                    } }
                    Tbody {
                        items.forEach { row ->
                            Tr {
                                Td({ style { padding(8.px, 6.px); fontSize(13.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(row.fecha) }
                                Td({ style { padding(8.px, 6.px); fontSize(13.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(row.po) }
                                Td({ style { padding(8.px, 6.px); fontSize(13.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(row.modelo) }
                                Td({ style { padding(8.px, 6.px); fontSize(13.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(row.cantidad) }
                                Td({ style { padding(8.px, 6.px); fontSize(13.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(row.ubicacion) }
                                Td({ style { padding(8.px, 6.px); fontSize(13.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(row.motivo) }
                                Td({ style { padding(8.px, 6.px); fontSize(13.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(row.responsable) }
                                if (canEdit) { Td { Button({ style(btnDelStyle()); onClick { scope.launch { client.delete("$BASE/api/v1/almacen/salidas/${row.id}"); refresh() } } }) { Text("✕") } } }
                            }
                        }
                    }
                }
            }
            P({ style { fontSize(12.px); color(Color("#64748b")); marginTop(12.px) } }) { Text("${items.size} registros") }
        }
    }
}

// ---------- UBICACIONES ----------
@Composable
private fun WarehouseLocationsTab(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope, canEdit: Boolean) {
    val BASE = BACKEND_URL
    var items by remember { mutableStateOf(emptyList<WarehouseLocation>()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey) {
        isLoading = true
        try { items = client.get("$BASE/api/v1/almacen/ubicaciones").body() } catch (e: Exception) { println("Error: ${e.message}") } finally { isLoading = false }
    }
    fun refresh() { refreshKey++ }

    var f_codigo by remember { mutableStateOf("") }
    var f_zona by remember { mutableStateOf("") }
    var f_tipo by remember { mutableStateOf("") }
    var f_capacidad by remember { mutableStateOf("") }
    var f_ocupacion by remember { mutableStateOf("") }
    var f_estado by remember { mutableStateOf("Activa") }
    var f_notas by remember { mutableStateOf("") }

    Div({ style { backgroundColor(Color.white); padding(24.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        H3({ style { margin(0.px, 0.px, 12.px, 0.px); fontSize(15.px) } }) { Text("Ubicaciones Físicas del Almacén") }
        if (canEdit) {
            Div({ style { display(DisplayStyle.Flex); gap(8.px); marginBottom(16.px); flexWrap(FlexWrap.Wrap); alignItems(AlignItems.Center) } }) {
                Input(InputType.Text) { placeholder("Código *"); value(f_codigo); onInput { f_codigo = it.value }; style(inputStyle()) }
                Input(InputType.Text) { placeholder("Zona"); value(f_zona); onInput { f_zona = it.value }; style(inputStyle()) }
                Input(InputType.Text) { placeholder("Tipo"); value(f_tipo); onInput { f_tipo = it.value }; style(inputStyle()) }
                Input(InputType.Text) { placeholder("Capacidad"); value(f_capacidad); onInput { f_capacidad = it.value }; style(inputStyle()) }
                Input(InputType.Text) { placeholder("Ocupación"); value(f_ocupacion); onInput { f_ocupacion = it.value }; style(inputStyle()) }
                Input(InputType.Text) { placeholder("Estado"); value(f_estado); onInput { f_estado = it.value }; style(inputStyle()) }
                Input(InputType.Text) { placeholder("Notas"); value(f_notas); onInput { f_notas = it.value }; style(inputStyle()) }
                Button({
                    style(btnAddStyle())
                    onClick {
                        if (f_codigo.isNotBlank()) { scope.launch {
                            client.post("$BASE/api/v1/almacen/ubicaciones") { contentType(ContentType.Application.Json); setBody(WarehouseLocation(codigo = f_codigo, zona = f_zona, tipo = f_tipo, capacidad = f_capacidad, ocupacion = f_ocupacion, estado = f_estado, notas = f_notas)) }
                            f_codigo = ""; f_zona = ""; f_tipo = ""; f_capacidad = ""; f_ocupacion = ""; f_estado = "Activa"; f_notas = ""; refresh()
                        } }
                    }
                }) { Text("+ Agregar") }
            }
        }
        if (isLoading) { P { Text("Cargando...") } } else {
            Div({ style { overflowX("auto") } }) {
                Table({ style { width(100.percent); property("border-collapse", "collapse") } }) {
                    Thead { Tr {
                        listOf("Código", "Zona", "Tipo", "Capacidad", "Ocupación", "Estado", "Notas").forEach { h ->
                            Th({ style { padding(8.px, 6.px); textAlign(TextAlign.Left); fontSize(12.px); color(Color("#64748b")); borderBottom(2.px, LineStyle.Solid, Color("#e2e8f0")) } }) { Text(h) }
                        }
                        Th { Text("") }
                    } }
                    Tbody {
                        items.forEach { row ->
                            Tr {
                                Td({ style { padding(8.px, 6.px); fontSize(13.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(row.codigo) }
                                Td({ style { padding(8.px, 6.px); fontSize(13.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(row.zona) }
                                Td({ style { padding(8.px, 6.px); fontSize(13.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(row.tipo) }
                                Td({ style { padding(8.px, 6.px); fontSize(13.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(row.capacidad) }
                                Td({ style { padding(8.px, 6.px); fontSize(13.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(row.ocupacion) }
                                Td({ style { padding(8.px, 6.px); fontSize(13.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(row.estado) }
                                Td({ style { padding(8.px, 6.px); fontSize(13.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(row.notas) }
                                if (canEdit) { Td { Button({ style(btnDelStyle()); onClick { scope.launch { client.delete("$BASE/api/v1/almacen/ubicaciones/${row.id}"); refresh() } } }) { Text("✕") } } }
                            }
                        }
                    }
                }
            }
            P({ style { fontSize(12.px); color(Color("#64748b")); marginTop(12.px) } }) { Text("${items.size} registros") }
        }
    }
}

// ---------- AUDITORIAS ----------
@Composable
private fun WarehouseAuditsTab(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope, canEdit: Boolean) {
    val BASE = BACKEND_URL
    var items by remember { mutableStateOf(emptyList<WarehouseAudit>()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey) {
        isLoading = true
        try { items = client.get("$BASE/api/v1/almacen/auditorias").body() } catch (e: Exception) { println("Error: ${e.message}") } finally { isLoading = false }
    }
    fun refresh() { refreshKey++ }

    var f_fecha by remember { mutableStateOf("") }
    var f_ubicacion by remember { mutableStateOf("") }
    var f_modelo by remember { mutableStateOf("") }
    var f_cantidadSistema by remember { mutableStateOf("") }
    var f_cantidadFisica by remember { mutableStateOf("") }
    var f_diferencia by remember { mutableStateOf("") }
    var f_responsable by remember { mutableStateOf("") }
    var f_observaciones by remember { mutableStateOf("") }
    var f_estado by remember { mutableStateOf("Pendiente") }

    Div({ style { backgroundColor(Color.white); padding(24.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        H3({ style { margin(0.px, 0.px, 12.px, 0.px); fontSize(15.px) } }) { Text("Auditorías de Inventario (Sistema vs. Físico)") }
        if (canEdit) {
            Div({ style { display(DisplayStyle.Flex); gap(8.px); marginBottom(16.px); flexWrap(FlexWrap.Wrap); alignItems(AlignItems.Center) } }) {
                Input(InputType.Text) { placeholder("Fecha"); value(f_fecha); onInput { f_fecha = it.value }; style(inputStyle()) }
                Input(InputType.Text) { placeholder("Ubicación"); value(f_ubicacion); onInput { f_ubicacion = it.value }; style(inputStyle()) }
                Input(InputType.Text) { placeholder("Modelo"); value(f_modelo); onInput { f_modelo = it.value }; style(inputStyle()) }
                Input(InputType.Text) { placeholder("Cant. Sistema"); value(f_cantidadSistema); onInput { f_cantidadSistema = it.value }; style(inputStyle()) }
                Input(InputType.Text) { placeholder("Cant. Física"); value(f_cantidadFisica); onInput { f_cantidadFisica = it.value }; style(inputStyle()) }
                Input(InputType.Text) { placeholder("Diferencia"); value(f_diferencia); onInput { f_diferencia = it.value }; style(inputStyle()) }
                Input(InputType.Text) { placeholder("Responsable"); value(f_responsable); onInput { f_responsable = it.value }; style(inputStyle()) }
                Input(InputType.Text) { placeholder("Estado"); value(f_estado); onInput { f_estado = it.value }; style(inputStyle()) }
                Input(InputType.Text) { placeholder("Observaciones"); value(f_observaciones); onInput { f_observaciones = it.value }; style(inputStyle()) }
                Button({
                    style(btnAddStyle())
                    onClick { scope.launch {
                        client.post("$BASE/api/v1/almacen/auditorias") { contentType(ContentType.Application.Json); setBody(WarehouseAudit(fecha = f_fecha, ubicacion = f_ubicacion, modelo = f_modelo, cantidadSistema = f_cantidadSistema, cantidadFisica = f_cantidadFisica, diferencia = f_diferencia, responsable = f_responsable, observaciones = f_observaciones, estado = f_estado)) }
                        f_fecha = ""; f_ubicacion = ""; f_modelo = ""; f_cantidadSistema = ""; f_cantidadFisica = ""; f_diferencia = ""; f_responsable = ""; f_observaciones = ""; f_estado = "Pendiente"; refresh()
                    } }
                }) { Text("+ Agregar") }
            }
        }
        if (isLoading) { P { Text("Cargando...") } } else {
            Div({ style { overflowX("auto") } }) {
                Table({ style { width(100.percent); property("border-collapse", "collapse") } }) {
                    Thead { Tr {
                        listOf("Fecha", "Ubicación", "Modelo", "Sistema", "Físico", "Diferencia", "Responsable", "Estado").forEach { h ->
                            Th({ style { padding(8.px, 6.px); textAlign(TextAlign.Left); fontSize(12.px); color(Color("#64748b")); borderBottom(2.px, LineStyle.Solid, Color("#e2e8f0")) } }) { Text(h) }
                        }
                        Th { Text("") }
                    } }
                    Tbody {
                        items.forEach { row ->
                            Tr {
                                Td({ style { padding(8.px, 6.px); fontSize(13.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(row.fecha) }
                                Td({ style { padding(8.px, 6.px); fontSize(13.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(row.ubicacion) }
                                Td({ style { padding(8.px, 6.px); fontSize(13.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(row.modelo) }
                                Td({ style { padding(8.px, 6.px); fontSize(13.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(row.cantidadSistema) }
                                Td({ style { padding(8.px, 6.px); fontSize(13.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(row.cantidadFisica) }
                                Td({ style { padding(8.px, 6.px); fontSize(13.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(row.diferencia) }
                                Td({ style { padding(8.px, 6.px); fontSize(13.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(row.responsable) }
                                Td({ style { padding(8.px, 6.px); fontSize(13.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(row.estado) }
                                if (canEdit) { Td { Button({ style(btnDelStyle()); onClick { scope.launch { client.delete("$BASE/api/v1/almacen/auditorias/${row.id}"); refresh() } } }) { Text("✕") } } }
                            }
                        }
                    }
                }
            }
            P({ style { fontSize(12.px); color(Color("#64748b")); marginTop(12.px) } }) { Text("${items.size} registros") }
        }
    }
}

// ---------- ENVIOS DETALLADOS ----------
@Composable
private fun WarehouseShipmentsTab(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope, canEdit: Boolean) {
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
                Input(InputType.Text) { placeholder("Cliente *"); value(f_cliente); onInput { f_cliente = it.value }; style(inputStyle()) }
                Input(InputType.Text) { placeholder("Fecha Carga"); value(f_fechaCarga); onInput { f_fechaCarga = it.value }; style(inputStyle()) }
                Input(InputType.Text) { placeholder("PO/Contenedor"); value(f_poContenedor); onInput { f_poContenedor = it.value }; style(inputStyle()) }
                Input(InputType.Text) { placeholder("SKU"); value(f_sku); onInput { f_sku = it.value }; style(inputStyle()) }
                Input(InputType.Text) { placeholder("Producto"); value(f_nombreProducto); onInput { f_nombreProducto = it.value }; style(inputStyle()) }
                Input(InputType.Text) { placeholder("Sello"); value(f_numeroSello); onInput { f_numeroSello = it.value }; style(inputStyle()) }
                Input(InputType.Text) { placeholder("Placa"); value(f_placa); onInput { f_placa = it.value }; style(inputStyle()) }
                Input(InputType.Text) { placeholder("Cantidad"); value(f_cantidad); onInput { f_cantidad = it.value }; style(inputStyle()) }
                Input(InputType.Text) { placeholder("Gabinetes"); value(f_gabinetes); onInput { f_gabinetes = it.value }; style(inputStyle()) }
                Input(InputType.Text) { placeholder("Conductor"); value(f_conductor); onInput { f_conductor = it.value }; style(inputStyle()) }
                Input(InputType.Text) { placeholder("Hora Inicio"); value(f_horaInicio); onInput { f_horaInicio = it.value }; style(inputStyle()) }
                Input(InputType.Text) { placeholder("Hora Fin"); value(f_horaFin); onInput { f_horaFin = it.value }; style(inputStyle()) }
                Input(InputType.Text) { placeholder("Operador"); value(f_operador); onInput { f_operador = it.value }; style(inputStyle()) }
                Input(InputType.Text) { placeholder("Inspector"); value(f_inspector); onInput { f_inspector = it.value }; style(inputStyle()) }
                Button({
                    style(btnAddStyle())
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
                        listOf("Cliente", "Fecha", "PO", "SKU", "Producto", "Sello", "Placa", "Cant.", "Gab.", "Conductor", "Inicio", "Fin", "Operador", "Inspector").forEach { h ->
                            Th({ style { padding(8.px, 6.px); textAlign(TextAlign.Left); fontSize(12.px); color(Color("#64748b")); borderBottom(2.px, LineStyle.Solid, Color("#e2e8f0")) } }) { Text(h) }
                        }
                        Th { Text("") }
                    } }
                    Tbody {
                        items.forEach { row ->
                            Tr {
                                Td({ style { padding(8.px, 6.px); fontSize(13.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(row.cliente) }
                                Td({ style { padding(8.px, 6.px); fontSize(13.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(row.fechaCarga) }
                                Td({ style { padding(8.px, 6.px); fontSize(13.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(row.poContenedor) }
                                Td({ style { padding(8.px, 6.px); fontSize(13.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(row.sku) }
                                Td({ style { padding(8.px, 6.px); fontSize(13.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(row.nombreProducto) }
                                Td({ style { padding(8.px, 6.px); fontSize(13.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(row.numeroSello) }
                                Td({ style { padding(8.px, 6.px); fontSize(13.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(row.placa) }
                                Td({ style { padding(8.px, 6.px); fontSize(13.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(row.cantidad) }
                                Td({ style { padding(8.px, 6.px); fontSize(13.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(row.gabinetes) }
                                Td({ style { padding(8.px, 6.px); fontSize(13.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(row.conductor) }
                                Td({ style { padding(8.px, 6.px); fontSize(13.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(row.horaInicio) }
                                Td({ style { padding(8.px, 6.px); fontSize(13.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(row.horaFin) }
                                Td({ style { padding(8.px, 6.px); fontSize(13.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(row.operador) }
                                Td({ style { padding(8.px, 6.px); fontSize(13.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(row.inspector) }
                                if (canEdit) { Td { Button({ style(btnDelStyle()); onClick { scope.launch { client.delete("$BASE/api/v1/almacen/envios/${row.id}"); refresh() } } }) { Text("✕") } } }
                            }
                        }
                    }
                }
            }
            P({ style { fontSize(12.px); color(Color("#64748b")); marginTop(12.px) } }) { Text("${items.size} registros") }
        }
    }
}
