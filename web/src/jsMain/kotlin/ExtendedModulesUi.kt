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

// ============================================================
// MÓDULOS EXTENDIDOS: Ferretería, Recepción MP, Tarimas,
// Contenedores China, Sellos, Huella de Carbono, Personal Tallas
// ============================================================

// ============ FERRETERÍA ============
@Composable
fun FerreteriaModule(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope, t: Translations) {
    var items by remember { mutableStateOf(emptyList<FerreteriaItem>()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey) {
        isLoading = true
        try { items = client.get("$BACKEND_URL/api/v1/ferreteria").body() } catch (e: Exception) { println("Error: ${e.message}") } finally { isLoading = false }
    }
    fun refresh() { refreshKey++ }

    var f_desc by remember { mutableStateOf("") }
    var f_cat by remember { mutableStateOf("") }
    var f_stock by remember { mutableStateOf("") }
    var f_unidad by remember { mutableStateOf("pza") }
    var f_ubic by remember { mutableStateOf("") }

    Div({ style { padding(24.px) } }) {
        Div({ style { display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); alignItems(AlignItems.Center); marginBottom(20.px) } }) {
            H2({ style { margin(0.px); fontSize(20.px); color(Color("#0f172a")) } }) { Text("Ferretería e Insumos") }
            Span({ style { fontSize(12.px); color(Color("#64748b")) } }) { Text("${items.size} items") }
        }

        Div({ style { backgroundColor(Color.white); padding(24.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
            Div({ style { display(DisplayStyle.Flex); gap(8.px); marginBottom(16.px); flexWrap(FlexWrap.Wrap); alignItems(AlignItems.Center) } }) {
                extInput("Descripción *", f_desc) { f_desc = it }
                extInput("Categoría", f_cat) { f_cat = it }
                extInput("Stock", f_stock) { f_stock = it }
                extInput("Unidad", f_unidad) { f_unidad = it }
                extInput("Ubicación", f_ubic) { f_ubic = it }
                Button({
                    style { padding(8.px, 16.px); backgroundColor(SidebarActiveColor); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") }
                    onClick {
                        if (f_desc.isNotBlank()) { scope.launch {
                            client.post("$BACKEND_URL/api/v1/ferreteria") { contentType(ContentType.Application.Json); setBody(FerreteriaItem(descripcion = f_desc, categoria = f_cat, stock = f_stock, unidad = f_unidad, ubicacion = f_ubic)) }
                            f_desc = ""; f_cat = ""; f_stock = ""; f_unidad = "pza"; f_ubic = ""; refresh()
                        } }
                    }
                }) { Text("+ Agregar") }
            }
            if (isLoading) { P { Text("Cargando...") } } else {
                Div({ style { overflowX("auto") } }) {
                    Table({ style { width(100.percent); property("border-collapse", "collapse") } }) {
                        Thead { Tr { extTh("Descripción"); extTh("Categoría"); extTh("Stock"); extTh("Unidad"); extTh("Ubicación"); extTh("") } }
                        Tbody {
                            items.forEach { row -> Tr {
                                extTd(row.descripcion); extTd(row.categoria); extTd(row.stock); extTd(row.unidad); extTd(row.ubicacion)
                                Td { extDelBtn { scope.launch { client.delete("$BACKEND_URL/api/v1/ferreteria/${row.id}"); refresh() } } }
                            } }
                        }
                    }
                }
            }
        }
    }
}

// ============ RECEPCIÓN DE MATERIAS PRIMAS ============
@Composable
fun RecepcionMpModule(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope, t: Translations) {
    var activeTab by remember { mutableStateOf(0) }
    val tabs = listOf("CALCIO", "PVC", "CARTON", "Todos")

    Div({ style { padding(24.px) } }) {
        Div({ style { display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); alignItems(AlignItems.Center); marginBottom(20.px) } }) {
            H2({ style { margin(0.px); fontSize(20.px); color(Color("#0f172a")) } }) { Text("Recepción de Materias Primas") }
        }

        Div({ style { display(DisplayStyle.Flex); gap(4.px); marginBottom(20.px); property("border-bottom", "1px solid #e2e8f0") } }) {
            tabs.forEachIndexed { idx, label ->
                Button({
                    style {
                        padding(8.px, 16.px); property("border", "none")
                        backgroundColor(if (activeTab == idx) SidebarActiveColor else Color.transparent)
                        color(if (activeTab == idx) Color.white else Color("#64748b"))
                        fontSize(13.px); fontWeight(if (activeTab == idx) "600" else "400"); cursor("pointer")
                        borderRadius(6.px, 6.px, 0.px, 0.px)
                    }
                    onClick { activeTab = idx }
                }) { Text(label) }
            }
        }

        val tipoFiltro = if (activeTab < 3) tabs[activeTab] else null
        RecepcionMpTab(client, scope, tipoFiltro)
    }
}

@Composable
private fun RecepcionMpTab(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope, tipo: String?) {
    var items by remember { mutableStateOf(emptyList<RecepcionMP>()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey, tipo) {
        isLoading = true
        try {
            items = if (tipo != null) client.get("$BACKEND_URL/api/v1/recepcion-mp?tipo=$tipo").body()
                    else client.get("$BACKEND_URL/api/v1/recepcion-mp").body()
        } catch (e: Exception) { println("Error: ${e.message}") } finally { isLoading = false }
    }
    fun refresh() { refreshKey++ }

    var f_fecha by remember { mutableStateOf("") }
    var f_desc by remember { mutableStateOf("") }
    var f_prov by remember { mutableStateOf("") }
    var f_cant by remember { mutableStateOf("") }
    var f_unidad by remember { mutableStateOf("") }
    var f_folio by remember { mutableStateOf("") }

    Div({ style { backgroundColor(Color.white); padding(24.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        Div({ style { display(DisplayStyle.Flex); gap(8.px); marginBottom(16.px); flexWrap(FlexWrap.Wrap); alignItems(AlignItems.Center) } }) {
            extInput("Fecha *", f_fecha) { f_fecha = it }
            extInput("Descripción", f_desc) { f_desc = it }
            extInput("Proveedor", f_prov) { f_prov = it }
            extInput("Cantidad", f_cant) { f_cant = it }
            extInput("Unidad (kg/pza/m2)", f_unidad) { f_unidad = it }
            extInput("Folio", f_folio) { f_folio = it }
            Button({
                style { padding(8.px, 16.px); backgroundColor(SidebarActiveColor); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") }
                onClick {
                    if (f_fecha.isNotBlank()) { scope.launch {
                        val t = tipo ?: ""
                        client.post("$BACKEND_URL/api/v1/recepcion-mp") { contentType(ContentType.Application.Json); setBody(RecepcionMP(fecha = f_fecha, tipo = t, descripcion = f_desc, proveedor = f_prov, cantidad = f_cant, unidad = f_unidad, folio = f_folio)) }
                        f_fecha = ""; f_desc = ""; f_prov = ""; f_cant = ""; f_unidad = ""; f_folio = ""; refresh()
                    } }
                }
            }) { Text("+ Agregar") }
        }
        if (isLoading) { P { Text("Cargando...") } } else {
            Div({ style { overflowX("auto") } }) {
                Table({ style { width(100.percent); property("border-collapse", "collapse") } }) {
                    Thead { Tr { extTh("Fecha"); extTh("Tipo"); extTh("Descripción"); extTh("Proveedor"); extTh("Cant."); extTh("Unidad"); extTh("Folio"); extTh("") } }
                    Tbody {
                        items.forEach { row -> Tr {
                            extTd(row.fecha); extTd(row.tipo); extTd(row.descripcion); extTd(row.proveedor); extTd(row.cantidad); extTd(row.unidad); extTd(row.folio)
                            Td { extDelBtn { scope.launch { client.delete("$BACKEND_URL/api/v1/recepcion-mp/${row.id}"); refresh() } } }
                        } }
                    }
                }
            }
            P({ style { fontSize(12.px); color(Color("#64748b")); marginTop(12.px) } }) { Text("${items.size} registros") }
        }
    }
}

// ============ HUELLA DE CARBONO (EHS) ============
@Composable
fun CarbonFootprintTab(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope) {
    var summary by remember { mutableStateOf<CarbonFootprintSummary?>(null) }
    var gasRecords by remember { mutableStateOf(emptyList<GasConsumo>()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }
    var litrosPorTanque by remember { mutableStateOf("1000") }

    LaunchedEffect(refreshKey) {
        isLoading = true
        try {
            summary = client.get("$BACKEND_URL/api/v1/ehs/gas/huella-carbono?litrosPorTanque=$litrosPorTanque").body()
            gasRecords = client.get("$BACKEND_URL/api/v1/ehs/gas").body()
        } catch (e: Exception) { println("Error: ${e.message}") } finally { isLoading = false }
    }
    fun refresh() { refreshKey++ }

    var f_fecha by remember { mutableStateOf("") }
    var f_tanques by remember { mutableStateOf("") }
    var f_dia by remember { mutableStateOf("") }

    Div({ style { marginBottom(24.px) } }) {
        H3({ style { margin(0.px, 0.px, 12.px, 0.px); fontSize(16.px); color(Color("#0f172a")) } }) { Text("Huella de Carbono · Consumo de Gas (COA)") }

        // Configuracion de litros por tanque
        Div({ style { display(DisplayStyle.Flex); gap(8.px); marginBottom(16.px); alignItems(AlignItems.Center) } }) {
            Text("Litros por tanque: ")
            Input(InputType.Text) {
                value(litrosPorTanque); onInput { litrosPorTanque = it.value }
                style { padding(4.px, 8.px); borderRadius(4.px); property("border", "1px solid #cbd5e1"); width(80.px) }
            }
            Button({
                style { padding(4.px, 12.px); backgroundColor(SidebarActiveColor); color(Color.white); property("border", "none"); borderRadius(4.px); cursor("pointer") }
                onClick { refresh() }
            }) { Text("Recalcular") }
        }

        if (isLoading) { P { Text("Cargando...") } } else {
            summary?.let { s ->
                // Tarjetas de resumen
                Div({ style { display(DisplayStyle.Flex); gap(16.px); flexWrap(FlexWrap.Wrap); marginBottom(24.px) } }) {
                    carbonCard("Tanques (Anual)", "${s.totalTanquesAnual}")
                    carbonCard("Litros (Anual)", "${s.totalLitrosAnual} L")
                    carbonCard("CO₂ (Anual)", "${"${(s.co2AnualKg * 100).toInt() / 100.0}"} kg")
                    carbonCard("CO₂ (Anual)", "${"${(s.co2AnualTon * 100).toInt() / 100.0}"} ton")
                }

                P({ style { fontSize(12.px); color(Color("#64748b")); marginBottom(16.px) } }) {
                    Text("Factor de emisión: ${s.factorEmision} kg CO₂/L LPG (IPCC) · Litros/tanque: ${s.litrosPorTanque}")
                }

                // Tabla mensual
                H4({ style { margin(0.px, 0.px, 12.px, 0.px); fontSize(14.px); color(Color("#0f172a")) } }) { Text("Consumo Mensual de Gas") }
                Div({ style { overflowX("auto") } }) {
                    Table({ style { width(100.percent); property("border-collapse", "collapse") } }) {
                        Thead { Tr { extTh("Mes"); extTh("Tanques"); extTh("Litros"); extTh("CO₂ (kg)"); extTh("CO₂ (ton)") } }
                        Tbody {
                            s.meses.forEach { m -> Tr {
                                extTd(m.mes); extTd("${m.tanques}"); extTd("${m.litros} L")
                                extTd("${(m.co2Kg * 100).toInt() / 100.0}")
                                extTd("${(m.co2Kg / 10.0).toInt() / 100.0}")
                            } }
                        }
                    }
                }
            }
        }
    }

    // Formulario para registrar consumo de gas
    Div({ style { backgroundColor(Color.white); padding(24.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        H3({ style { margin(0.px, 0.px, 12.px, 0.px); fontSize(15.px) } }) { Text("Registrar Consumo de Gas") }
        Div({ style { display(DisplayStyle.Flex); gap(8.px); marginBottom(16.px); flexWrap(FlexWrap.Wrap); alignItems(AlignItems.Center) } }) {
            extInput("Fecha *", f_fecha) { f_fecha = it }
            extInput("No. Tanques *", f_tanques) { f_tanques = it }
            extInput("Día de carga", f_dia) { f_dia = it }
            Button({
                style { padding(8.px, 16.px); backgroundColor(SidebarActiveColor); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") }
                onClick {
                    if (f_fecha.isNotBlank() && f_tanques.isNotBlank()) { scope.launch {
                        client.post("$BACKEND_URL/api/v1/ehs/gas") { contentType(ContentType.Application.Json); setBody(GasConsumo(fecha = f_fecha, cantidadTexto = "$f_tanques TANQUES", diaCarga = f_dia)) }
                        f_fecha = ""; f_tanques = ""; f_dia = ""; refresh()
                    } }
                }
            }) { Text("+ Registrar") }
        }
        if (gasRecords.isNotEmpty()) {
            Div({ style { overflowX("auto") } }) {
                Table({ style { width(100.percent); property("border-collapse", "collapse") } }) {
                    Thead { Tr { extTh("Fecha"); extTh("Tanques"); extTh("Litros"); extTh("Día"); extTh("") } }
                    Tbody {
                        gasRecords.take(20).forEach { row -> Tr {
                            extTd(row.fecha); extTd("${row.numeroTanques}"); extTd("${row.litrosTotales} L"); extTd(row.diaCarga)
                            Td { extDelBtn { scope.launch { client.delete("$BACKEND_URL/api/v1/ehs/gas/${row.id}"); refresh() } } }
                        } }
                    }
                }
            }
            P({ style { fontSize(12.px); color(Color("#64748b")); marginTop(8.px) } }) { Text("Mostrando últimos 20 de ${gasRecords.size} registros") }
        }
    }
}

@Composable
private fun carbonCard(label: String, value: String) {
    Div({ style { backgroundColor(Color("#f0fdf4")); padding(20.px); borderRadius(10.px); property("border", "1px solid #bbf7d0"); minWidth(160.px) } }) {
        P({ style { fontSize(12.px); color(Color("#64748b")); margin(0.px, 0.px, 8.px, 0.px) } }) { Text(label) }
        P({ style { fontSize(24.px); fontWeight("700"); color(Color("#0f172a")); margin(0.px) } }) { Text(value) }
    }
}

// ============ TARIMAS (tab para Almacén) ============
@Composable
fun TarimasTab(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope, canEdit: Boolean) {
    var items by remember { mutableStateOf(emptyList<Tarima>()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey) {
        isLoading = true
        try { items = client.get("$BACKEND_URL/api/v1/almacen/tarimas").body() } catch (e: Exception) { println("Error: ${e.message}") } finally { isLoading = false }
    }
    fun refresh() { refreshKey++ }

    var f_compania by remember { mutableStateOf("") }
    var f_fechaLlegada by remember { mutableStateOf("") }
    var f_folio by remember { mutableStateOf("") }
    var f_cantidad by remember { mutableStateOf("") }
    var f_medidas by remember { mutableStateOf("") }
    var f_rechazadas by remember { mutableStateOf("") }
    var f_fechaRegreso by remember { mutableStateOf("") }

    Div({ style { backgroundColor(Color.white); padding(24.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        H3({ style { margin(0.px, 0.px, 12.px, 0.px); fontSize(15.px) } }) { Text("Control de Tarimas / Pallets") }
        if (canEdit) {
            Div({ style { display(DisplayStyle.Flex); gap(8.px); marginBottom(16.px); flexWrap(FlexWrap.Wrap); alignItems(AlignItems.Center) } }) {
                extInput("Compañía", f_compania) { f_compania = it }
                extInput("Fecha Llegada", f_fechaLlegada) { f_fechaLlegada = it }
                extInput("Folio", f_folio) { f_folio = it }
                extInput("Cantidad", f_cantidad) { f_cantidad = it }
                extInput("Medidas", f_medidas) { f_medidas = it }
                extInput("Rechazadas", f_rechazadas) { f_rechazadas = it }
                extInput("Fecha Regreso", f_fechaRegreso) { f_fechaRegreso = it }
                Button({
                    style { padding(8.px, 16.px); backgroundColor(SidebarActiveColor); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") }
                    onClick {
                        if (f_folio.isNotBlank()) { scope.launch {
                            client.post("$BACKEND_URL/api/v1/almacen/tarimas") { contentType(ContentType.Application.Json); setBody(Tarima(compania = f_compania, fechaLlegada = f_fechaLlegada, folio = f_folio, cantidad = f_cantidad, medidas = f_medidas, rechazadas = f_rechazadas, fechaRegreso = f_fechaRegreso)) }
                            f_compania = ""; f_fechaLlegada = ""; f_folio = ""; f_cantidad = ""; f_medidas = ""; f_rechazadas = ""; f_fechaRegreso = ""; refresh()
                        } }
                    }
                }) { Text("+ Agregar") }
            }
        }
        if (isLoading) { P { Text("Cargando...") } } else {
            Div({ style { overflowX("auto") } }) {
                Table({ style { width(100.percent); property("border-collapse", "collapse") } }) {
                    Thead { Tr { extTh("Compañía"); extTh("Llegada"); extTh("Folio"); extTh("Cant."); extTh("Medidas"); extTh("Rechaz."); extTh("Regreso"); extTh("") } }
                    Tbody {
                        items.forEach { row -> Tr {
                            extTd(row.compania); extTd(row.fechaLlegada); extTd(row.folio); extTd(row.cantidad); extTd(row.medidas); extTd(row.rechazadas); extTd(row.fechaRegreso)
                            if (canEdit) { Td { extDelBtn { scope.launch { client.delete("$BACKEND_URL/api/v1/almacen/tarimas/${row.id}"); refresh() } } } }
                        } }
                    }
                }
            }
            P({ style { fontSize(12.px); color(Color("#64748b")); marginTop(12.px) } }) { Text("${items.size} tarimas") }
        }
    }
}

// ============ SELLOS EN STOCK (tab para Almacén) ============
@Composable
fun SellosTab(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope, canEdit: Boolean) {
    var items by remember { mutableStateOf(emptyList<SelloStock>()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey) {
        isLoading = true
        try { items = client.get("$BACKEND_URL/api/v1/almacen/sellos").body() } catch (e: Exception) { println("Error: ${e.message}") } finally { isLoading = false }
    }
    fun refresh() { refreshKey++ }

    var f_fecha by remember { mutableStateOf("") }
    var f_inicial by remember { mutableStateOf("") }
    var f_final by remember { mutableStateOf("") }
    var f_cantidad by remember { mutableStateOf("") }

    Div({ style { backgroundColor(Color.white); padding(24.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        H3({ style { margin(0.px, 0.px, 12.px, 0.px); fontSize(15.px) } }) { Text("Sellos en Stock") }
        if (canEdit) {
            Div({ style { display(DisplayStyle.Flex); gap(8.px); marginBottom(16.px); flexWrap(FlexWrap.Wrap); alignItems(AlignItems.Center) } }) {
                extInput("Fecha", f_fecha) { f_fecha = it }
                extInput("# Inicial", f_inicial) { f_inicial = it }
                extInput("# Final", f_final) { f_final = it }
                extInput("Cantidad", f_cantidad) { f_cantidad = it }
                Button({
                    style { padding(8.px, 16.px); backgroundColor(SidebarActiveColor); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") }
                    onClick {
                        if (f_inicial.isNotBlank()) { scope.launch {
                            client.post("$BACKEND_URL/api/v1/almacen/sellos") { contentType(ContentType.Application.Json); setBody(SelloStock(fecha = f_fecha, numeroInicial = f_inicial, numeroFinal = f_final, cantidad = f_cantidad)) }
                            f_fecha = ""; f_inicial = ""; f_final = ""; f_cantidad = ""; refresh()
                        } }
                    }
                }) { Text("+ Agregar") }
            }
        }
        if (isLoading) { P { Text("Cargando...") } } else {
            Div({ style { overflowX("auto") } }) {
                Table({ style { width(100.percent); property("border-collapse", "collapse") } }) {
                    Thead { Tr { extTh("Fecha"); extTh("# Inicial"); extTh("# Final"); extTh("Cantidad"); extTh("") } }
                    Tbody {
                        items.forEach { row -> Tr {
                            extTd(row.fecha); extTd(row.numeroInicial); extTd(row.numeroFinal); extTd(row.cantidad)
                            if (canEdit) { Td { extDelBtn { scope.launch { client.delete("$BACKEND_URL/api/v1/almacen/sellos/${row.id}"); refresh() } } } }
                        } }
                    }
                }
            }
        }
    }
}

// ============ CONTENEDORES DE CHINA (tab para Embarques) ============
@Composable
fun ContenedoresChinaTab(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope, canEdit: Boolean) {
    var items by remember { mutableStateOf(emptyList<ContenedorChina>()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey) {
        isLoading = true
        try { items = client.get("$BACKEND_URL/api/v1/embarques/contenedores-china").body() } catch (e: Exception) { println("Error: ${e.message}") } finally { isLoading = false }
    }
    fun refresh() { refreshKey++ }

    var f_fecha by remember { mutableStateOf("") }
    var f_codigo by remember { mutableStateOf("") }
    var f_nombre by remember { mutableStateOf("") }
    var f_spec by remember { mutableStateOf("") }
    var f_modelo by remember { mutableStateOf("") }
    var f_cantidad by remember { mutableStateOf("") }
    var f_unidad by remember { mutableStateOf("") }

    Div({ style { backgroundColor(Color.white); padding(24.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        H3({ style { margin(0.px, 0.px, 12.px, 0.px); fontSize(15.px) } }) { Text("Contenedores de China") }
        if (canEdit) {
            Div({ style { display(DisplayStyle.Flex); gap(8.px); marginBottom(16.px); flexWrap(FlexWrap.Wrap); alignItems(AlignItems.Center) } }) {
                extInput("Fecha", f_fecha) { f_fecha = it }
                extInput("Código Producto", f_codigo) { f_codigo = it }
                extInput("Nombre", f_nombre) { f_nombre = it }
                extInput("Especificación", f_spec) { f_spec = it }
                extInput("Modelo", f_modelo) { f_modelo = it }
                extInput("Cantidad", f_cantidad) { f_cantidad = it }
                extInput("Unidad (kg/pza/m2)", f_unidad) { f_unidad = it }
                Button({
                    style { padding(8.px, 16.px); backgroundColor(SidebarActiveColor); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") }
                    onClick {
                        if (f_codigo.isNotBlank()) { scope.launch {
                            client.post("$BACKEND_URL/api/v1/embarques/contenedores-china") { contentType(ContentType.Application.Json); setBody(ContenedorChina(fecha = f_fecha, codigoProducto = f_codigo, nombre = f_nombre, especificacion = f_spec, modelo = f_modelo, cantidad = f_cantidad, unidad = f_unidad)) }
                            f_fecha = ""; f_codigo = ""; f_nombre = ""; f_spec = ""; f_modelo = ""; f_cantidad = ""; f_unidad = ""; refresh()
                        } }
                    }
                }) { Text("+ Agregar") }
            }
        }
        if (isLoading) { P { Text("Cargando...") } } else {
            Div({ style { overflowX("auto") } }) {
                Table({ style { width(100.percent); property("border-collapse", "collapse") } }) {
                    Thead { Tr { extTh("Fecha"); extTh("Código"); extTh("Nombre"); extTh("Spec"); extTh("Modelo"); extTh("Cant."); extTh("Unidad"); extTh("") } }
                    Tbody {
                        items.forEach { row -> Tr {
                            extTd(row.fecha); extTd(row.codigoProducto); extTd(row.nombre); extTd(row.especificacion); extTd(row.modelo); extTd(row.cantidad); extTd(row.unidad)
                            if (canEdit) { Td { extDelBtn { scope.launch { client.delete("$BACKEND_URL/api/v1/embarques/contenedores-china/${row.id}"); refresh() } } } }
                        } }
                    }
                }
            }
            P({ style { fontSize(12.px); color(Color("#64748b")); marginTop(12.px) } }) { Text("${items.size} registros") }
        }
    }
}

// ============ HELPERS ============
@Composable
private fun extInput(label: String, value: String, onChange: (String) -> Unit) {
    Input(InputType.Text) {
        placeholder(label); value(value); onInput { onChange(it.value) }
        style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(140.px) }
    }
}

@Composable
private fun extTh(label: String) {
    Th({ style { padding(8.px, 6.px); textAlign("left"); fontSize(12.px); color(Color("#64748b")); property("border-bottom", "2px solid #e2e8f0") } }) { Text(label) }
}

@Composable
private fun extTd(text: String) {
    Td({ style { padding(8.px, 6.px); fontSize(13.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(text) }
}

@Composable
private fun extDelBtn(action: () -> Unit) {
    Button({
        style { padding(4.px, 8.px); backgroundColor(Color("#ef4444")); color(Color.white); property("border", "none"); borderRadius(4.px); cursor("pointer"); fontSize(12.px) }
        onClick { action() }
    }) { Text("✕") }
}
