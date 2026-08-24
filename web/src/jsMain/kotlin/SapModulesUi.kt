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

// Modulos estilo SAP integrados a RHNAF: CO, MM (Compras), PP, QM, EWM, GTS, EHS (Auditorias), SAP Security/GRC
// Sigue el mismo patron que los modulos existentes en Main.kt (Warehouse/Attendance): HttpClient + LaunchedEffect + formulario inline.

@Composable
fun ControllingModule(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope, t: Translations) {
    var items by remember { mutableStateOf(emptyList<CostCenter>()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey) {
        isLoading = true
        try {
            items = client.get("$BACKEND_URL/api/v1/sap/co/centros-costo").body()
        } catch (e: Exception) {
            println("Error cargando ControllingModule: ${e.message}")
        } finally {
            isLoading = false
        }
    }
    fun refresh() { refreshKey++ }

    var f_codigo by remember { mutableStateOf("") }
    var f_nombre by remember { mutableStateOf("") }
    var f_departamento by remember { mutableStateOf("") }
    var f_presupuestoMensual by remember { mutableStateOf("") }
    var f_gastoActual by remember { mutableStateOf("") }

    Div({ style { backgroundColor(Color.white); padding(32.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        Div({ style { display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); alignItems(AlignItems.Center); marginBottom(16.px) } }) {
            H3({ style { margin(0.px) } }) { Text("CO · Control de Costos (Controlling)") }
            Span({ style { color(Color.gray); fontSize(13.px) } }) { Text("${items.size} registros") }
        }

        Div({ style { display(DisplayStyle.Flex); gap(8.px); marginBottom(16.px); flexWrap(FlexWrap.Wrap); alignItems(AlignItems.Center) } }) {
            Input(InputType.Text) { placeholder("Codigo *"); value(f_codigo); onInput { f_codigo = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(100.px) } }
            Input(InputType.Text) { placeholder("Nombre del centro de costo"); value(f_nombre); onInput { f_nombre = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(220.px) } }
            Input(InputType.Text) { placeholder("Departamento"); value(f_departamento); onInput { f_departamento = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(180.px) } }
            Input(InputType.Text) { placeholder("Presupuesto mensual"); value(f_presupuestoMensual); onInput { f_presupuestoMensual = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(160.px) } }
            Input(InputType.Text) { placeholder("Gasto actual"); value(f_gastoActual); onInput { f_gastoActual = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(140.px) } }
            Button({
                style { padding(8.px, 16.px); backgroundColor(SidebarActiveColor); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") }
                onClick {
                    if (f_codigo.isNotBlank()) {
                        scope.launch {
                            client.post("$BACKEND_URL/api/v1/sap/co/centros-costo") {
                                contentType(ContentType.Application.Json)
                                setBody(CostCenter(codigo = f_codigo, nombre = f_nombre, departamento = f_departamento, presupuestoMensual = f_presupuestoMensual, gastoActual = f_gastoActual))
                            }
                            f_codigo = ""
                            f_nombre = ""
                            f_departamento = ""
                            f_presupuestoMensual = ""
                            f_gastoActual = ""
                            refresh()
                        }
                    } else {
                        window.alert("Completa el campo obligatorio para agregar el registro.")
                    }
                }
            }) { Text("+ Agregar") }
        }

        if (isLoading) {
            P { Text("Cargando...") }
        } else {
            Table({ style { width(100.percent) } }) {
                Thead { Tr { Th { Text("Codigo") }; Th { Text("Nombre") }; Th { Text("Departamento") }; Th { Text("Presupuesto") }; Th { Text("Gasto Actual") }; Th { Text("") } } }
                Tbody {
                    items.forEach { row ->
                        Tr {
                            Td { Text(row.codigo) }; Td { Text(row.nombre) }; Td { Text(row.departamento) }; Td { Text(row.presupuestoMensual) }; Td { Text(row.gastoActual) }
                            Td {
                                Button({
                                    style { backgroundColor(Color("#ef4444")); color(Color.white); property("border", "none"); borderRadius(4.px); padding(4.px, 10.px); cursor("pointer") }
                                    onClick { scope.launch { client.delete("$BACKEND_URL/api/v1/sap/co/centros-costo/${row.id}"); refresh() } }
                                }) { Text("Eliminar") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PurchasingModule(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope, t: Translations) {
    var items by remember { mutableStateOf(emptyList<PurchaseOrder>()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey) {
        isLoading = true
        try {
            items = client.get("$BACKEND_URL/api/v1/sap/mm/ordenes-compra").body()
        } catch (e: Exception) {
            println("Error cargando PurchasingModule: ${e.message}")
        } finally {
            isLoading = false
        }
    }
    fun refresh() { refreshKey++ }

    var f_numero by remember { mutableStateOf("") }
    var f_proveedor by remember { mutableStateOf("") }
    var f_fecha by remember { mutableStateOf("") }
    var f_descripcion by remember { mutableStateOf("") }
    var f_montoTotal by remember { mutableStateOf("") }
    var f_estado by remember { mutableStateOf("") }

    Div({ style { backgroundColor(Color.white); padding(32.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        Div({ style { display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); alignItems(AlignItems.Center); marginBottom(16.px) } }) {
            H3({ style { margin(0.px) } }) { Text("MM · Compras y Abastecimiento (Materials Management)") }
            Span({ style { color(Color.gray); fontSize(13.px) } }) { Text("${items.size} registros") }
        }

        Div({ style { display(DisplayStyle.Flex); gap(8.px); marginBottom(16.px); flexWrap(FlexWrap.Wrap); alignItems(AlignItems.Center) } }) {
            Input(InputType.Text) { placeholder("No. Orden *"); value(f_numero); onInput { f_numero = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(120.px) } }
            Input(InputType.Text) { placeholder("Proveedor"); value(f_proveedor); onInput { f_proveedor = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(200.px) } }
            Input(InputType.Text) { placeholder("Fecha"); value(f_fecha); onInput { f_fecha = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(130.px) } }
            Input(InputType.Text) { placeholder("Descripcion"); value(f_descripcion); onInput { f_descripcion = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(220.px) } }
            Input(InputType.Text) { placeholder("Monto total"); value(f_montoTotal); onInput { f_montoTotal = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(130.px) } }
            Input(InputType.Text) { placeholder("Estado"); value(f_estado); onInput { f_estado = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(120.px) } }
            Button({
                style { padding(8.px, 16.px); backgroundColor(SidebarActiveColor); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") }
                onClick {
                    if (f_numero.isNotBlank()) {
                        scope.launch {
                            client.post("$BACKEND_URL/api/v1/sap/mm/ordenes-compra") {
                                contentType(ContentType.Application.Json)
                                setBody(PurchaseOrder(numero = f_numero, proveedor = f_proveedor, fecha = f_fecha, descripcion = f_descripcion, montoTotal = f_montoTotal, estado = f_estado))
                            }
                            f_numero = ""
                            f_proveedor = ""
                            f_fecha = ""
                            f_descripcion = ""
                            f_montoTotal = ""
                            f_estado = ""
                            refresh()
                        }
                    } else {
                        window.alert("Completa el campo obligatorio para agregar el registro.")
                    }
                }
            }) { Text("+ Agregar") }
        }

        if (isLoading) {
            P { Text("Cargando...") }
        } else {
            Table({ style { width(100.percent) } }) {
                Thead { Tr { Th { Text("No. Orden") }; Th { Text("Proveedor") }; Th { Text("Fecha") }; Th { Text("Descripcion") }; Th { Text("Monto Total") }; Th { Text("Estado") }; Th { Text("") } } }
                Tbody {
                    items.forEach { row ->
                        Tr {
                            Td { Text(row.numero) }; Td { Text(row.proveedor) }; Td { Text(row.fecha) }; Td { Text(row.descripcion) }; Td { Text(row.montoTotal) }; Td { Text(row.estado) }
                            Td {
                                Button({
                                    style { backgroundColor(Color("#ef4444")); color(Color.white); property("border", "none"); borderRadius(4.px); padding(4.px, 10.px); cursor("pointer") }
                                    onClick { scope.launch { client.delete("$BACKEND_URL/api/v1/sap/mm/ordenes-compra/${row.id}"); refresh() } }
                                }) { Text("Eliminar") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductionModule(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope, t: Translations) {
    var items by remember { mutableStateOf(emptyList<ProductionOrder>()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey) {
        isLoading = true
        try {
            items = client.get("$BACKEND_URL/api/v1/sap/pp/ordenes-produccion").body()
        } catch (e: Exception) {
            println("Error cargando ProductionModule: ${e.message}")
        } finally {
            isLoading = false
        }
    }
    fun refresh() { refreshKey++ }

    var f_numero by remember { mutableStateOf("") }
    var f_producto by remember { mutableStateOf("") }
    var f_cantidadPlan by remember { mutableStateOf("") }
    var f_cantidadProducida by remember { mutableStateOf("") }
    var f_centroTrabajo by remember { mutableStateOf("") }
    var f_fechaInicio by remember { mutableStateOf("") }
    var f_fechaFin by remember { mutableStateOf("") }
    var f_estado by remember { mutableStateOf("") }

    Div({ style { backgroundColor(Color.white); padding(32.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        Div({ style { display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); alignItems(AlignItems.Center); marginBottom(16.px) } }) {
            H3({ style { margin(0.px) } }) { Text("PP · Planificacion de Produccion (Production Planning)") }
            Span({ style { color(Color.gray); fontSize(13.px) } }) { Text("${items.size} registros") }
        }

        Div({ style { display(DisplayStyle.Flex); gap(8.px); marginBottom(16.px); flexWrap(FlexWrap.Wrap); alignItems(AlignItems.Center) } }) {
            Input(InputType.Text) { placeholder("No. Orden *"); value(f_numero); onInput { f_numero = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(120.px) } }
            Input(InputType.Text) { placeholder("Producto"); value(f_producto); onInput { f_producto = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(200.px) } }
            Input(InputType.Text) { placeholder("Cant. planeada"); value(f_cantidadPlan); onInput { f_cantidadPlan = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(130.px) } }
            Input(InputType.Text) { placeholder("Cant. producida"); value(f_cantidadProducida); onInput { f_cantidadProducida = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(130.px) } }
            Input(InputType.Text) { placeholder("Centro de trabajo"); value(f_centroTrabajo); onInput { f_centroTrabajo = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(160.px) } }
            Input(InputType.Text) { placeholder("Fecha inicio"); value(f_fechaInicio); onInput { f_fechaInicio = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(130.px) } }
            Input(InputType.Text) { placeholder("Fecha fin"); value(f_fechaFin); onInput { f_fechaFin = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(130.px) } }
            Input(InputType.Text) { placeholder("Estado"); value(f_estado); onInput { f_estado = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(120.px) } }
            Button({
                style { padding(8.px, 16.px); backgroundColor(SidebarActiveColor); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") }
                onClick {
                    if (f_numero.isNotBlank()) {
                        scope.launch {
                            client.post("$BACKEND_URL/api/v1/sap/pp/ordenes-produccion") {
                                contentType(ContentType.Application.Json)
                                setBody(ProductionOrder(numero = f_numero, producto = f_producto, cantidadPlan = f_cantidadPlan, cantidadProducida = f_cantidadProducida, centroTrabajo = f_centroTrabajo, fechaInicio = f_fechaInicio, fechaFin = f_fechaFin, estado = f_estado))
                            }
                            f_numero = ""
                            f_producto = ""
                            f_cantidadPlan = ""
                            f_cantidadProducida = ""
                            f_centroTrabajo = ""
                            f_fechaInicio = ""
                            f_fechaFin = ""
                            f_estado = ""
                            refresh()
                        }
                    } else {
                        window.alert("Completa el campo obligatorio para agregar el registro.")
                    }
                }
            }) { Text("+ Agregar") }
        }

        if (isLoading) {
            P { Text("Cargando...") }
        } else {
            Table({ style { width(100.percent) } }) {
                Thead { Tr { Th { Text("No. Orden") }; Th { Text("Producto") }; Th { Text("Cant. Plan") }; Th { Text("Cant. Producida") }; Th { Text("Centro Trabajo") }; Th { Text("Inicio") }; Th { Text("Fin") }; Th { Text("Estado") }; Th { Text("") } } }
                Tbody {
                    items.forEach { row ->
                        Tr {
                            Td { Text(row.numero) }; Td { Text(row.producto) }; Td { Text(row.cantidadPlan) }; Td { Text(row.cantidadProducida) }; Td { Text(row.centroTrabajo) }; Td { Text(row.fechaInicio) }; Td { Text(row.fechaFin) }; Td { Text(row.estado) }
                            Td {
                                Button({
                                    style { backgroundColor(Color("#ef4444")); color(Color.white); property("border", "none"); borderRadius(4.px); padding(4.px, 10.px); cursor("pointer") }
                                    onClick { scope.launch { client.delete("$BACKEND_URL/api/v1/sap/pp/ordenes-produccion/${row.id}"); refresh() } }
                                }) { Text("Eliminar") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QualityModule(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope, t: Translations) {
    var items by remember { mutableStateOf(emptyList<QualityInspection>()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey) {
        isLoading = true
        try {
            items = client.get("$BACKEND_URL/api/v1/sap/qm/inspecciones").body()
        } catch (e: Exception) {
            println("Error cargando QualityModule: ${e.message}")
        } finally {
            isLoading = false
        }
    }
    fun refresh() { refreshKey++ }

    var f_fecha by remember { mutableStateOf("") }
    var f_loteProducto by remember { mutableStateOf("") }
    var f_inspector by remember { mutableStateOf("") }
    var f_resultado by remember { mutableStateOf("") }
    var f_observaciones by remember { mutableStateOf("") }

    Div({ style { backgroundColor(Color.white); padding(32.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        Div({ style { display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); alignItems(AlignItems.Center); marginBottom(16.px) } }) {
            H3({ style { margin(0.px) } }) { Text("QM · Gestion de Calidad (Quality Management)") }
            Span({ style { color(Color.gray); fontSize(13.px) } }) { Text("${items.size} registros") }
        }

        Div({ style { display(DisplayStyle.Flex); gap(8.px); marginBottom(16.px); flexWrap(FlexWrap.Wrap); alignItems(AlignItems.Center) } }) {
            Input(InputType.Text) { placeholder("Fecha *"); value(f_fecha); onInput { f_fecha = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(130.px) } }
            Input(InputType.Text) { placeholder("Lote / Producto"); value(f_loteProducto); onInput { f_loteProducto = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(180.px) } }
            Input(InputType.Text) { placeholder("Inspector"); value(f_inspector); onInput { f_inspector = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(160.px) } }
            Input(InputType.Text) { placeholder("Resultado (Aprobado/Rechazado)"); value(f_resultado); onInput { f_resultado = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(200.px) } }
            Input(InputType.Text) { placeholder("Observaciones"); value(f_observaciones); onInput { f_observaciones = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(220.px) } }
            Button({
                style { padding(8.px, 16.px); backgroundColor(SidebarActiveColor); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") }
                onClick {
                    if (f_fecha.isNotBlank()) {
                        scope.launch {
                            client.post("$BACKEND_URL/api/v1/sap/qm/inspecciones") {
                                contentType(ContentType.Application.Json)
                                setBody(QualityInspection(fecha = f_fecha, loteProducto = f_loteProducto, inspector = f_inspector, resultado = f_resultado, observaciones = f_observaciones))
                            }
                            f_fecha = ""
                            f_loteProducto = ""
                            f_inspector = ""
                            f_resultado = ""
                            f_observaciones = ""
                            refresh()
                        }
                    } else {
                        window.alert("Completa el campo obligatorio para agregar el registro.")
                    }
                }
            }) { Text("+ Agregar") }
        }

        if (isLoading) {
            P { Text("Cargando...") }
        } else {
            Table({ style { width(100.percent) } }) {
                Thead { Tr { Th { Text("Fecha") }; Th { Text("Lote/Producto") }; Th { Text("Inspector") }; Th { Text("Resultado") }; Th { Text("Observaciones") }; Th { Text("") } } }
                Tbody {
                    items.forEach { row ->
                        Tr {
                            Td { Text(row.fecha) }; Td { Text(row.loteProducto) }; Td { Text(row.inspector) }; Td { Text(row.resultado) }; Td { Text(row.observaciones) }
                            Td {
                                Button({
                                    style { backgroundColor(Color("#ef4444")); color(Color.white); property("border", "none"); borderRadius(4.px); padding(4.px, 10.px); cursor("pointer") }
                                    onClick { scope.launch { client.delete("$BACKEND_URL/api/v1/sap/qm/inspecciones/${row.id}"); refresh() } }
                                }) { Text("Eliminar") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GtsTradeModule(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope, t: Translations) {
    var items by remember { mutableStateOf(emptyList<CustomsDeclaration>()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey) {
        isLoading = true
        try {
            items = client.get("$BACKEND_URL/api/v1/sap/gts/pedimentos").body()
        } catch (e: Exception) {
            println("Error cargando GtsTradeModule: ${e.message}")
        } finally {
            isLoading = false
        }
    }
    fun refresh() { refreshKey++ }

    var f_numeroPedimento by remember { mutableStateOf("") }
    var f_fecha by remember { mutableStateOf("") }
    var f_cliente by remember { mutableStateOf("") }
    var f_paisDestino by remember { mutableStateOf("") }
    var f_valorAduana by remember { mutableStateOf("") }
    var f_regimen by remember { mutableStateOf("") }
    var f_estado by remember { mutableStateOf("") }

    Div({ style { backgroundColor(Color.white); padding(32.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        Div({ style { display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); alignItems(AlignItems.Center); marginBottom(16.px) } }) {
            H3({ style { margin(0.px) } }) { Text("GTS · Comercio Exterior (Global Trade Services)") }
            Span({ style { color(Color.gray); fontSize(13.px) } }) { Text("${items.size} registros") }
        }

        Div({ style { display(DisplayStyle.Flex); gap(8.px); marginBottom(16.px); flexWrap(FlexWrap.Wrap); alignItems(AlignItems.Center) } }) {
            Input(InputType.Text) { placeholder("No. Pedimento *"); value(f_numeroPedimento); onInput { f_numeroPedimento = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(150.px) } }
            Input(InputType.Text) { placeholder("Fecha"); value(f_fecha); onInput { f_fecha = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(130.px) } }
            Input(InputType.Text) { placeholder("Cliente"); value(f_cliente); onInput { f_cliente = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(180.px) } }
            Input(InputType.Text) { placeholder("Pais destino"); value(f_paisDestino); onInput { f_paisDestino = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(140.px) } }
            Input(InputType.Text) { placeholder("Valor aduana"); value(f_valorAduana); onInput { f_valorAduana = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(140.px) } }
            Input(InputType.Text) { placeholder("Regimen"); value(f_regimen); onInput { f_regimen = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(140.px) } }
            Input(InputType.Text) { placeholder("Estado"); value(f_estado); onInput { f_estado = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(120.px) } }
            Button({
                style { padding(8.px, 16.px); backgroundColor(SidebarActiveColor); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") }
                onClick {
                    if (f_numeroPedimento.isNotBlank()) {
                        scope.launch {
                            client.post("$BACKEND_URL/api/v1/sap/gts/pedimentos") {
                                contentType(ContentType.Application.Json)
                                setBody(CustomsDeclaration(numeroPedimento = f_numeroPedimento, fecha = f_fecha, cliente = f_cliente, paisDestino = f_paisDestino, valorAduana = f_valorAduana, regimen = f_regimen, estado = f_estado))
                            }
                            f_numeroPedimento = ""
                            f_fecha = ""
                            f_cliente = ""
                            f_paisDestino = ""
                            f_valorAduana = ""
                            f_regimen = ""
                            f_estado = ""
                            refresh()
                        }
                    } else {
                        window.alert("Completa el campo obligatorio para agregar el registro.")
                    }
                }
            }) { Text("+ Agregar") }
        }

        if (isLoading) {
            P { Text("Cargando...") }
        } else {
            Table({ style { width(100.percent) } }) {
                Thead { Tr { Th { Text("No. Pedimento") }; Th { Text("Fecha") }; Th { Text("Cliente") }; Th { Text("Pais Destino") }; Th { Text("Valor Aduana") }; Th { Text("Regimen") }; Th { Text("Estado") }; Th { Text("") } } }
                Tbody {
                    items.forEach { row ->
                        Tr {
                            Td { Text(row.numeroPedimento) }; Td { Text(row.fecha) }; Td { Text(row.cliente) }; Td { Text(row.paisDestino) }; Td { Text(row.valorAduana) }; Td { Text(row.regimen) }; Td { Text(row.estado) }
                            Td {
                                Button({
                                    style { backgroundColor(Color("#ef4444")); color(Color.white); property("border", "none"); borderRadius(4.px); padding(4.px, 10.px); cursor("pointer") }
                                    onClick { scope.launch { client.delete("$BACKEND_URL/api/v1/sap/gts/pedimentos/${row.id}"); refresh() } }
                                }) { Text("Eliminar") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EhsAuditsModule(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope, t: Translations) {
    var activeTab by remember { mutableStateOf(0) }
    val tabs = listOf("Inspecciones", "Incidentes", "Permisos Trabajo", "EPP", "Capacitaciones", "Simulacros", "Matriz Riesgos", "Medio Ambiente", "Huella de Carbono", "Salud Ocupacional", "Químicos")

    Div({ style { backgroundColor(Color.white); padding(32.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        H3({ style { margin(0.px); marginBottom(16.px) } }) { Text("EHS \u00b7 Seguridad, Salud y Ambiente") }

        // Tab bar
        Div({ style { display(DisplayStyle.Flex); gap(4.px); marginBottom(20.px); flexWrap(FlexWrap.Wrap) } }) {
            tabs.forEachIndexed { idx, label ->
                Button({
                    style {
                        padding(8.px, 14.px); borderRadius(6.px); cursor("pointer")
                        property("border", "none")
                        if (idx == activeTab) { backgroundColor(SidebarActiveColor); color(Color.white) } else { backgroundColor(Color("#f1f5f9")); color(Color("#475569")) }
                    }
                    onClick { activeTab = idx }
                }) { Text(label) }
            }
        }

        when (activeTab) {
            0 -> EhsInspectionsTab(client, scope)
            1 -> EhsIncidentsTab(client, scope)
            2 -> EhsWorkPermitsTab(client, scope)
            3 -> EhsPpeTab(client, scope)
            4 -> EhsTrainingsTab(client, scope)
            5 -> EhsDrillsTab(client, scope)
            6 -> EhsRiskMatrixTab(client, scope)
            7 -> EhsEnvironmentTab(client, scope)
            8 -> CarbonFootprintTab(client, scope)
            9 -> EhsOccupationalHealthTab(client, scope)
            10 -> EhsChemicalsTab(client, scope)
        }
    }
}

// EHS-8. Medio Ambiente (Residuos)
@Composable
fun EhsEnvironmentTab(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope) {
    var items by remember { mutableStateOf(emptyList<WasteManifest>()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }
    LaunchedEffect(refreshKey) {
        isLoading = true
        try { items = client.get("$BACKEND_URL/api/v1/sap/ehs/residuos").body() } catch (e: Exception) { println("Err: ${e.message}") } finally { isLoading = false }
    }
    fun refresh() { refreshKey++ }

    var f_fecha by remember { mutableStateOf("") }
    var f_residuo by remember { mutableStateOf("") }
    var f_tipo by remember { mutableStateOf("") }
    var f_cantidad by remember { mutableStateOf("") }
    var f_transportista by remember { mutableStateOf("") }
    var f_destino by remember { mutableStateOf("") }
    var f_manifiesto by remember { mutableStateOf("") }

    Span({ style { color(Color.gray); fontSize(13.px); marginBottom(8.px); display(DisplayStyle.Block) } }) { Text("${items.size} manifiestos registrados") }
    Div({ style { display(DisplayStyle.Flex); gap(8.px); marginBottom(16.px); flexWrap(FlexWrap.Wrap); alignItems(AlignItems.Center) } }) {
        Input(InputType.Text) { placeholder("Fecha *"); value(f_fecha); onInput { f_fecha = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(120.px) } }
        Input(InputType.Text) { placeholder("Residuo *"); value(f_residuo); onInput { f_residuo = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(180.px) } }
        Input(InputType.Text) { placeholder("Tipo"); value(f_tipo); onInput { f_tipo = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(140.px) } }
        Input(InputType.Text) { placeholder("Cant"); value(f_cantidad); onInput { f_cantidad = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(80.px) } }
        Input(InputType.Text) { placeholder("Transportista"); value(f_transportista); onInput { f_transportista = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(160.px) } }
        Input(InputType.Text) { placeholder("Destino Final"); value(f_destino); onInput { f_destino = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(160.px) } }
        Input(InputType.Text) { placeholder("No. Manifiesto"); value(f_manifiesto); onInput { f_manifiesto = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(140.px) } }
        Button({
            style { padding(8.px, 16.px); backgroundColor(SidebarActiveColor); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") }
            onClick {
                if (f_fecha.isNotBlank() && f_residuo.isNotBlank()) {
                    scope.launch {
                        client.post("$BACKEND_URL/api/v1/sap/ehs/residuos") {
                            contentType(ContentType.Application.Json)
                            setBody(WasteManifest(fecha = f_fecha, residuo = f_residuo, tipo = f_tipo, cantidad = f_cantidad, transportista = f_transportista, destinoFinal = f_destino, numeroManifiesto = f_manifiesto))
                        }
                        f_fecha = ""; f_residuo = ""; f_tipo = ""; f_cantidad = ""; f_transportista = ""; f_destino = ""; f_manifiesto = ""; refresh()
                    }
                }
            }
        }) { Text("+ Registrar") }
    }
    if (isLoading) { P { Text("Cargando...") } } else {
        Table({ style { width(100.percent) } }) {
            Thead { Tr { Th { Text("Fecha") }; Th { Text("Residuo") }; Th { Text("Tipo") }; Th { Text("Cant") }; Th { Text("Transportista") }; Th { Text("Destino") }; Th { Text("Manifiesto") }; Th { Text("") } } }
            Tbody { items.forEach { row -> Tr { Td { Text(row.fecha) }; Td { Text(row.residuo) }; Td { Text(row.tipo) }; Td { Text(row.cantidad) }; Td { Text(row.transportista) }; Td { Text(row.destinoFinal) }; Td { Text(row.numeroManifiesto) }; Td { Button({ style { backgroundColor(Color("#ef4444")); color(Color.white); property("border", "none"); borderRadius(4.px); padding(4.px, 10.px); cursor("pointer") }; onClick { scope.launch { client.delete("$BACKEND_URL/api/v1/sap/ehs/residuos/${row.id}"); refresh() } } }) { Text("X") } } } } }
        }
    }
}

// EHS-9. Salud Ocupacional (Examenes)
@Composable
fun EhsOccupationalHealthTab(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope) {
    var items by remember { mutableStateOf(emptyList<MedicalExam>()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }
    LaunchedEffect(refreshKey) {
        isLoading = true
        try { items = client.get("$BACKEND_URL/api/v1/sap/ehs/salud").body() } catch (e: Exception) { println("Err: ${e.message}") } finally { isLoading = false }
    }
    fun refresh() { refreshKey++ }

    var f_empId by remember { mutableStateOf("") }
    var f_nombre by remember { mutableStateOf("") }
    var f_fecha by remember { mutableStateOf("") }
    var f_tipo by remember { mutableStateOf("") }
    var f_resultado by remember { mutableStateOf("") }
    var f_prox by remember { mutableStateOf("") }

    Span({ style { color(Color.gray); fontSize(13.px); marginBottom(8.px); display(DisplayStyle.Block) } }) { Text("${items.size} examenes registrados") }
    Div({ style { display(DisplayStyle.Flex); gap(8.px); marginBottom(16.px); flexWrap(FlexWrap.Wrap); alignItems(AlignItems.Center) } }) {
        Input(InputType.Text) { placeholder("ID Empleado *"); value(f_empId); onInput { f_empId = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(120.px) } }
        Input(InputType.Text) { placeholder("Nombre"); value(f_nombre); onInput { f_nombre = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(180.px) } }
        Input(InputType.Text) { placeholder("Fecha *"); value(f_fecha); onInput { f_fecha = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(120.px) } }
        Input(InputType.Text) { placeholder("Tipo (Ingreso/Periodico)"); value(f_tipo); onInput { f_tipo = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(160.px) } }
        Input(InputType.Text) { placeholder("Resultado (Apto/...)"); value(f_resultado); onInput { f_resultado = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(140.px) } }
        Input(InputType.Text) { placeholder("Prox. Cita"); value(f_prox); onInput { f_prox = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(120.px) } }
        Button({
            style { padding(8.px, 16.px); backgroundColor(SidebarActiveColor); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") }
            onClick {
                if (f_empId.isNotBlank() && f_fecha.isNotBlank()) {
                    scope.launch {
                        client.post("$BACKEND_URL/api/v1/sap/ehs/salud") {
                            contentType(ContentType.Application.Json)
                            setBody(MedicalExam(empleadoId = f_empId, nombreEmpleado = f_nombre, fecha = f_fecha, tipoExamen = f_tipo, resultado = f_resultado, proximaCita = f_prox))
                        }
                        f_empId = ""; f_nombre = ""; f_fecha = ""; f_tipo = ""; f_resultado = ""; f_prox = ""; refresh()
                    }
                }
            }
        }) { Text("+ Registrar") }
    }
    if (isLoading) { P { Text("Cargando...") } } else {
        Table({ style { width(100.percent) } }) {
            Thead { Tr { Th { Text("ID Emp") }; Th { Text("Nombre") }; Th { Text("Fecha") }; Th { Text("Tipo") }; Th { Text("Resultado") }; Th { Text("Prox. Cita") }; Th { Text("") } } }
            Tbody { items.forEach { row -> Tr { Td { Text(row.empleadoId) }; Td { Text(row.nombreEmpleado) }; Td { Text(row.fecha) }; Td { Text(row.tipoExamen) }; Td { Text(row.resultado) }; Td { Text(row.proximaCita) }; Td { Button({ style { backgroundColor(Color("#ef4444")); color(Color.white); property("border", "none"); borderRadius(4.px); padding(4.px, 10.px); cursor("pointer") }; onClick { scope.launch { client.delete("$BACKEND_URL/api/v1/sap/ehs/salud/${row.id}"); refresh() } } }) { Text("X") } } } } }
        }
    }
}

// EHS-10. Quimicos (MSDS)
@Composable
fun EhsChemicalsTab(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope) {
    var items by remember { mutableStateOf(emptyList<ChemicalProduct>()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }
    LaunchedEffect(refreshKey) {
        isLoading = true
        try { items = client.get("$BACKEND_URL/api/v1/sap/ehs/quimicos").body() } catch (e: Exception) { println("Err: ${e.message}") } finally { isLoading = false }
    }
    fun refresh() { refreshKey++ }

    var f_nombre by remember { mutableStateOf("") }
    var f_fabricante by remember { mutableStateOf("") }
    var f_area by remember { mutableStateOf("") }
    var f_riesgo by remember { mutableStateOf("") }
    var f_url by remember { mutableStateOf("") }

    Span({ style { color(Color.gray); fontSize(13.px); marginBottom(8.px); display(DisplayStyle.Block) } }) { Text("${items.size} productos químicos") }
    Div({ style { display(DisplayStyle.Flex); gap(8.px); marginBottom(16.px); flexWrap(FlexWrap.Wrap); alignItems(AlignItems.Center) } }) {
        Input(InputType.Text) { placeholder("Producto *"); value(f_nombre); onInput { f_nombre = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(180.px) } }
        Input(InputType.Text) { placeholder("Fabricante"); value(f_fabricante); onInput { f_fabricante = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(160.px) } }
        Input(InputType.Text) { placeholder("Area uso"); value(f_area); onInput { f_area = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(140.px) } }
        Input(InputType.Text) { placeholder("Nivel Riesgo (0-4)"); value(f_riesgo); onInput { f_riesgo = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(120.px) } }
        Input(InputType.Text) { placeholder("URL Hoja Seguridad"); value(f_url); onInput { f_url = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(220.px) } }
        Button({
            style { padding(8.px, 16.px); backgroundColor(SidebarActiveColor); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") }
            onClick {
                if (f_nombre.isNotBlank()) {
                    scope.launch {
                        client.post("$BACKEND_URL/api/v1/sap/ehs/quimicos") {
                            contentType(ContentType.Application.Json)
                            setBody(ChemicalProduct(nombre = f_nombre, fabricante = f_fabricante, areaUso = f_area, nivelRiesgo = f_riesgo, hojaSeguridadUrl = f_url))
                        }
                        f_nombre = ""; f_fabricante = ""; f_area = ""; f_riesgo = ""; f_url = ""; refresh()
                    }
                }
            }
        }) { Text("+ Agregar") }
    }
    if (isLoading) { P { Text("Cargando...") } } else {
        Table({ style { width(100.percent) } }) {
            Thead { Tr { Th { Text("Producto") }; Th { Text("Fabricante") }; Th { Text("Area") }; Th { Text("Riesgo") }; Th { Text("MSDS") }; Th { Text("") } } }
            Tbody { items.forEach { row -> Tr { Td { Text(row.nombre) }; Td { Text(row.fabricante) }; Td { Text(row.areaUso) }; Td { Text(row.nivelRiesgo) }; Td { if(row.hojaSeguridadUrl.isNotEmpty()) A(href = row.hojaSeguridadUrl) { Text("Ver PDF") } else Text("-") }; Td { Button({ style { backgroundColor(Color("#ef4444")); color(Color.white); property("border", "none"); borderRadius(4.px); padding(4.px, 10.px); cursor("pointer") }; onClick { scope.launch { client.delete("$BACKEND_URL/api/v1/sap/ehs/quimicos/${row.id}"); refresh() } } }) { Text("X") } } } } }
        }
    }
}

// EHS-1. Inspecciones de Seguridad
@Composable
fun EhsInspectionsTab(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope) {
    var items by remember { mutableStateOf(emptyList<SafetyInspection>()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }
    LaunchedEffect(refreshKey) {
        isLoading = true
        try { items = client.get("$BACKEND_URL/api/v1/sap/ehs/inspecciones").body() } catch (e: Exception) { println("Err: ${e.message}") } finally { isLoading = false }
    }
    fun refresh() { refreshKey++ }
    var f_fecha by remember { mutableStateOf("") }
    var f_tipoInspeccion by remember { mutableStateOf("") }
    var f_area by remember { mutableStateOf("") }
    var f_inspector by remember { mutableStateOf("") }
    var f_hallazgos by remember { mutableStateOf("") }
    var f_riesgo by remember { mutableStateOf("") }
    var f_accionesCorrectivas by remember { mutableStateOf("") }
    var f_fechaCierre by remember { mutableStateOf("") }
    var f_estado by remember { mutableStateOf("") }
    Span({ style { color(Color.gray); fontSize(13.px); marginBottom(8.px); display(DisplayStyle.Block) } }) { Text("${items.size} registros") }
    Div({ style { display(DisplayStyle.Flex); gap(8.px); marginBottom(16.px); flexWrap(FlexWrap.Wrap); alignItems(AlignItems.Center) } }) {
        Input(InputType.Text) { placeholder("Fecha *"); value(f_fecha); onInput { f_fecha = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(120.px) } }
        Input(InputType.Text) { placeholder("Tipo (Programada/No prog.)"); value(f_tipoInspeccion); onInput { f_tipoInspeccion = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(180.px) } }
        Input(InputType.Text) { placeholder("Area"); value(f_area); onInput { f_area = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(140.px) } }
        Input(InputType.Text) { placeholder("Inspector"); value(f_inspector); onInput { f_inspector = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(140.px) } }
        Input(InputType.Text) { placeholder("Hallazgos"); value(f_hallazgos); onInput { f_hallazgos = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(200.px) } }
        Input(InputType.Text) { placeholder("Riesgo"); value(f_riesgo); onInput { f_riesgo = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(140.px) } }
        Input(InputType.Text) { placeholder("Acciones correctivas"); value(f_accionesCorrectivas); onInput { f_accionesCorrectivas = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(200.px) } }
        Input(InputType.Text) { placeholder("Fecha cierre"); value(f_fechaCierre); onInput { f_fechaCierre = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(120.px) } }
        Input(InputType.Text) { placeholder("Estado"); value(f_estado); onInput { f_estado = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(100.px) } }
        Button({ style { padding(8.px, 16.px); backgroundColor(SidebarActiveColor); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") }; onClick { if (f_fecha.isNotBlank()) { scope.launch { client.post("$BACKEND_URL/api/v1/sap/ehs/inspecciones") { contentType(ContentType.Application.Json); setBody(SafetyInspection(fecha = f_fecha, tipoInspeccion = f_tipoInspeccion, area = f_area, inspector = f_inspector, hallazgos = f_hallazgos, riesgo = f_riesgo, accionesCorrectivas = f_accionesCorrectivas, fechaCierre = f_fechaCierre, estado = f_estado)) }; f_fecha = ""; f_tipoInspeccion = ""; f_area = ""; f_inspector = ""; f_hallazgos = ""; f_riesgo = ""; f_accionesCorrectivas = ""; f_fechaCierre = ""; f_estado = ""; refresh() } } else { window.alert("La fecha es obligatoria.") } } }) { Text("+ Agregar") }
    }
    if (isLoading) { P { Text("Cargando...") } } else {
        Table({ style { width(100.percent) } }) {
            Thead { Tr { Th { Text("Fecha") }; Th { Text("Tipo") }; Th { Text("Area") }; Th { Text("Inspector") }; Th { Text("Hallazgos") }; Th { Text("Riesgo") }; Th { Text("Acciones") }; Th { Text("F.Cierre") }; Th { Text("Estado") }; Th { Text("") } } }
            Tbody { items.forEach { row -> Tr { Td { Text(row.fecha) }; Td { Text(row.tipoInspeccion) }; Td { Text(row.area) }; Td { Text(row.inspector) }; Td { Text(row.hallazgos) }; Td { Text(row.riesgo) }; Td { Text(row.accionesCorrectivas) }; Td { Text(row.fechaCierre) }; Td { Text(row.estado) }; Td { Button({ style { backgroundColor(Color("#ef4444")); color(Color.white); property("border", "none"); borderRadius(4.px); padding(4.px, 10.px); cursor("pointer") }; onClick { scope.launch { client.delete("$BACKEND_URL/api/v1/sap/ehs/inspecciones/${row.id}"); refresh() } } }) { Text("X") } } } } }
        }
    }
}

// EHS-2. Incidentes y Accidentes
@Composable
fun EhsIncidentsTab(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope) {
    var items by remember { mutableStateOf(emptyList<SafetyIncident>()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }
    LaunchedEffect(refreshKey) { isLoading = true; try { items = client.get("$BACKEND_URL/api/v1/sap/ehs/incidentes").body() } catch (e: Exception) { println("Err: ${e.message}") } finally { isLoading = false } }
    fun refresh() { refreshKey++ }
    var f_fecha by remember { mutableStateOf("") }
    var f_tipo by remember { mutableStateOf("") }
    var f_severidad by remember { mutableStateOf("") }
    var f_personaAfectada by remember { mutableStateOf("") }
    var f_departamento by remember { mutableStateOf("") }
    var f_parteCuerpo by remember { mutableStateOf("") }
    var f_diasPerdidos by remember { mutableStateOf("") }
    var f_descripcion by remember { mutableStateOf("") }
    var f_causaRaiz by remember { mutableStateOf("") }
    var f_estado by remember { mutableStateOf("") }
    Span({ style { color(Color.gray); fontSize(13.px); marginBottom(8.px); display(DisplayStyle.Block) } }) { Text("${items.size} registros") }
    Div({ style { display(DisplayStyle.Flex); gap(8.px); marginBottom(16.px); flexWrap(FlexWrap.Wrap); alignItems(AlignItems.Center) } }) {
        Input(InputType.Text) { placeholder("Fecha *"); value(f_fecha); onInput { f_fecha = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(120.px) } }
        Input(InputType.Text) { placeholder("Tipo (Accidente/Incidente/Casi)"); value(f_tipo); onInput { f_tipo = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(200.px) } }
        Input(InputType.Text) { placeholder("Severidad (Leve/Mod/Grave/Fatal)"); value(f_severidad); onInput { f_severidad = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(200.px) } }
        Input(InputType.Text) { placeholder("Persona afectada"); value(f_personaAfectada); onInput { f_personaAfectada = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(160.px) } }
        Input(InputType.Text) { placeholder("Departamento"); value(f_departamento); onInput { f_departamento = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(140.px) } }
        Input(InputType.Text) { placeholder("Parte cuerpo"); value(f_parteCuerpo); onInput { f_parteCuerpo = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(140.px) } }
        Input(InputType.Text) { placeholder("Dias perdidos"); value(f_diasPerdidos); onInput { f_diasPerdidos = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(100.px) } }
        Input(InputType.Text) { placeholder("Descripcion"); value(f_descripcion); onInput { f_descripcion = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(200.px) } }
        Input(InputType.Text) { placeholder("Causa raiz"); value(f_causaRaiz); onInput { f_causaRaiz = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(200.px) } }
        Input(InputType.Text) { placeholder("Estado"); value(f_estado); onInput { f_estado = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(100.px) } }
        Button({ style { padding(8.px, 16.px); backgroundColor(SidebarActiveColor); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") }; onClick { if (f_fecha.isNotBlank()) { scope.launch { client.post("$BACKEND_URL/api/v1/sap/ehs/incidentes") { contentType(ContentType.Application.Json); setBody(SafetyIncident(fecha = f_fecha, tipo = f_tipo, severidad = f_severidad, personaAfectada = f_personaAfectada, departamento = f_departamento, parteCuerpo = f_parteCuerpo, diasPerdidos = f_diasPerdidos, descripcion = f_descripcion, causaRaiz = f_causaRaiz, estado = f_estado)) }; f_fecha = ""; f_tipo = ""; f_severidad = ""; f_personaAfectada = ""; f_departamento = ""; f_parteCuerpo = ""; f_diasPerdidos = ""; f_descripcion = ""; f_causaRaiz = ""; f_estado = ""; refresh() } } else { window.alert("La fecha es obligatoria.") } } }) { Text("+ Agregar") }
    }
    if (isLoading) { P { Text("Cargando...") } } else {
        Table({ style { width(100.percent) } }) {
            Thead { Tr { Th { Text("Fecha") }; Th { Text("Tipo") }; Th { Text("Severidad") }; Th { Text("Persona") }; Th { Text("Depto") }; Th { Text("Cuerpo") }; Th { Text("Dias") }; Th { Text("Descripcion") }; Th { Text("Causa") }; Th { Text("Estado") }; Th { Text("") } } }
            Tbody { items.forEach { row -> Tr { Td { Text(row.fecha) }; Td { Text(row.tipo) }; Td { Text(row.severidad) }; Td { Text(row.personaAfectada) }; Td { Text(row.departamento) }; Td { Text(row.parteCuerpo) }; Td { Text(row.diasPerdidos) }; Td { Text(row.descripcion) }; Td { Text(row.causaRaiz) }; Td { Text(row.estado) }; Td { Button({ style { backgroundColor(Color("#ef4444")); color(Color.white); property("border", "none"); borderRadius(4.px); padding(4.px, 10.px); cursor("pointer") }; onClick { scope.launch { client.delete("$BACKEND_URL/api/v1/sap/ehs/incidentes/${row.id}"); refresh() } } }) { Text("X") } } } } }
        }
    }
}

// EHS-3. Permisos de Trabajo
@Composable
fun EhsWorkPermitsTab(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope) {
    var items by remember { mutableStateOf(emptyList<WorkPermit>()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }
    LaunchedEffect(refreshKey) { isLoading = true; try { items = client.get("$BACKEND_URL/api/v1/sap/ehs/permisos-trabajo").body() } catch (e: Exception) { println("Err: ${e.message}") } finally { isLoading = false } }
    fun refresh() { refreshKey++ }
    var f_tipo by remember { mutableStateOf("") }
    var f_solicitante by remember { mutableStateOf("") }
    var f_autorizadoPor by remember { mutableStateOf("") }
    var f_fechaInicio by remember { mutableStateOf("") }
    var f_fechaFin by remember { mutableStateOf("") }
    var f_area by remember { mutableStateOf("") }
    var f_riesgosIdentificados by remember { mutableStateOf("") }
    var f_eppRequerido by remember { mutableStateOf("") }
    var f_estado by remember { mutableStateOf("") }
    Span({ style { color(Color.gray); fontSize(13.px); marginBottom(8.px); display(DisplayStyle.Block) } }) { Text("${items.size} registros") }
    Div({ style { display(DisplayStyle.Flex); gap(8.px); marginBottom(16.px); flexWrap(FlexWrap.Wrap); alignItems(AlignItems.Center) } }) {
        Input(InputType.Text) { placeholder("Tipo (Caliente/Espacio confinado/Alturas)"); value(f_tipo); onInput { f_tipo = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(220.px) } }
        Input(InputType.Text) { placeholder("Solicitante *"); value(f_solicitante); onInput { f_solicitante = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(150.px) } }
        Input(InputType.Text) { placeholder("Autorizado por"); value(f_autorizadoPor); onInput { f_autorizadoPor = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(150.px) } }
        Input(InputType.Text) { placeholder("F. Inicio"); value(f_fechaInicio); onInput { f_fechaInicio = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(120.px) } }
        Input(InputType.Text) { placeholder("F. Fin"); value(f_fechaFin); onInput { f_fechaFin = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(120.px) } }
        Input(InputType.Text) { placeholder("Area"); value(f_area); onInput { f_area = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(140.px) } }
        Input(InputType.Text) { placeholder("Riesgos identificados"); value(f_riesgosIdentificados); onInput { f_riesgosIdentificados = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(200.px) } }
        Input(InputType.Text) { placeholder("EPP requerido"); value(f_eppRequerido); onInput { f_eppRequerido = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(200.px) } }
        Input(InputType.Text) { placeholder("Estado"); value(f_estado); onInput { f_estado = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(100.px) } }
        Button({ style { padding(8.px, 16.px); backgroundColor(SidebarActiveColor); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") }; onClick { if (f_solicitante.isNotBlank()) { scope.launch { client.post("$BACKEND_URL/api/v1/sap/ehs/permisos-trabajo") { contentType(ContentType.Application.Json); setBody(WorkPermit(tipo = f_tipo, solicitante = f_solicitante, autorizadoPor = f_autorizadoPor, fechaInicio = f_fechaInicio, fechaFin = f_fechaFin, area = f_area, riesgosIdentificados = f_riesgosIdentificados, eppRequerido = f_eppRequerido, estado = f_estado)) }; f_tipo = ""; f_solicitante = ""; f_autorizadoPor = ""; f_fechaInicio = ""; f_fechaFin = ""; f_area = ""; f_riesgosIdentificados = ""; f_eppRequerido = ""; f_estado = ""; refresh() } } else { window.alert("El solicitante es obligatorio.") } } }) { Text("+ Agregar") }
    }
    if (isLoading) { P { Text("Cargando...") } } else {
        Table({ style { width(100.percent) } }) {
            Thead { Tr { Th { Text("Tipo") }; Th { Text("Solicitante") }; Th { Text("Autorizado") }; Th { Text("F.Inicio") }; Th { Text("F.Fin") }; Th { Text("Area") }; Th { Text("Riesgos") }; Th { Text("EPP") }; Th { Text("Estado") }; Th { Text("") } } }
            Tbody { items.forEach { row -> Tr { Td { Text(row.tipo) }; Td { Text(row.solicitante) }; Td { Text(row.autorizadoPor) }; Td { Text(row.fechaInicio) }; Td { Text(row.fechaFin) }; Td { Text(row.area) }; Td { Text(row.riesgosIdentificados) }; Td { Text(row.eppRequerido) }; Td { Text(row.estado) }; Td { Button({ style { backgroundColor(Color("#ef4444")); color(Color.white); property("border", "none"); borderRadius(4.px); padding(4.px, 10.px); cursor("pointer") }; onClick { scope.launch { client.delete("$BACKEND_URL/api/v1/sap/ehs/permisos-trabajo/${row.id}"); refresh() } } }) { Text("X") } } } } }
        }
    }
}

// EHS-4. Entrega de EPP
@Composable
fun EhsPpeTab(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope) {
    var items by remember { mutableStateOf(emptyList<PpeDelivery>()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }
    LaunchedEffect(refreshKey) { isLoading = true; try { items = client.get("$BACKEND_URL/api/v1/sap/ehs/entregas-epp").body() } catch (e: Exception) { println("Err: ${e.message}") } finally { isLoading = false } }
    fun refresh() { refreshKey++ }
    var f_fecha by remember { mutableStateOf("") }
    var f_empleado by remember { mutableStateOf("") }
    var f_tipoEpp by remember { mutableStateOf("") }
    var f_talla by remember { mutableStateOf("") }
    var f_proximaReposicion by remember { mutableStateOf("") }
    var f_firma by remember { mutableStateOf("") }
    Span({ style { color(Color.gray); fontSize(13.px); marginBottom(8.px); display(DisplayStyle.Block) } }) { Text("${items.size} registros") }
    Div({ style { display(DisplayStyle.Flex); gap(8.px); marginBottom(16.px); flexWrap(FlexWrap.Wrap); alignItems(AlignItems.Center) } }) {
        Input(InputType.Text) { placeholder("Fecha *"); value(f_fecha); onInput { f_fecha = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(120.px) } }
        Input(InputType.Text) { placeholder("Empleado *"); value(f_empleado); onInput { f_empleado = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(160.px) } }
        Input(InputType.Text) { placeholder("Tipo EPP (Casco/Lentes/Guantes...)"); value(f_tipoEpp); onInput { f_tipoEpp = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(220.px) } }
        Input(InputType.Text) { placeholder("Talla"); value(f_talla); onInput { f_talla = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(80.px) } }
        Input(InputType.Text) { placeholder("Prox. reposicion"); value(f_proximaReposicion); onInput { f_proximaReposicion = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(130.px) } }
        Input(InputType.Text) { placeholder("Firma"); value(f_firma); onInput { f_firma = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(140.px) } }
        Button({ style { padding(8.px, 16.px); backgroundColor(SidebarActiveColor); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") }; onClick { if (f_fecha.isNotBlank() && f_empleado.isNotBlank()) { scope.launch { client.post("$BACKEND_URL/api/v1/sap/ehs/entregas-epp") { contentType(ContentType.Application.Json); setBody(PpeDelivery(fecha = f_fecha, empleado = f_empleado, tipoEpp = f_tipoEpp, talla = f_talla, proximaReposicion = f_proximaReposicion, firma = f_firma)) }; f_fecha = ""; f_empleado = ""; f_tipoEpp = ""; f_talla = ""; f_proximaReposicion = ""; f_firma = ""; refresh() } } else { window.alert("Fecha y empleado son obligatorios.") } } }) { Text("+ Agregar") }
    }
    if (isLoading) { P { Text("Cargando...") } } else {
        Table({ style { width(100.percent) } }) {
            Thead { Tr { Th { Text("Fecha") }; Th { Text("Empleado") }; Th { Text("Tipo EPP") }; Th { Text("Talla") }; Th { Text("Prox. Reposicion") }; Th { Text("Firma") }; Th { Text("") } } }
            Tbody { items.forEach { row -> Tr { Td { Text(row.fecha) }; Td { Text(row.empleado) }; Td { Text(row.tipoEpp) }; Td { Text(row.talla) }; Td { Text(row.proximaReposicion) }; Td { Text(row.firma) }; Td { Button({ style { backgroundColor(Color("#ef4444")); color(Color.white); property("border", "none"); borderRadius(4.px); padding(4.px, 10.px); cursor("pointer") }; onClick { scope.launch { client.delete("$BACKEND_URL/api/v1/sap/ehs/entregas-epp/${row.id}"); refresh() } } }) { Text("X") } } } } }
        }
    }
}

// EHS-5. Capacitaciones de Seguridad
@Composable
fun EhsTrainingsTab(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope) {
    var items by remember { mutableStateOf(emptyList<SafetyTraining>()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }
    LaunchedEffect(refreshKey) { isLoading = true; try { items = client.get("$BACKEND_URL/api/v1/sap/ehs/capacitaciones").body() } catch (e: Exception) { println("Err: ${e.message}") } finally { isLoading = false } }
    fun refresh() { refreshKey++ }
    var f_fecha by remember { mutableStateOf("") }
    var f_tema by remember { mutableStateOf("") }
    var f_instructor by remember { mutableStateOf("") }
    var f_asistentes by remember { mutableStateOf("") }
    var f_vigenciaMeses by remember { mutableStateOf("") }
    var f_proximaFecha by remember { mutableStateOf("") }
    var f_estado by remember { mutableStateOf("") }
    Span({ style { color(Color.gray); fontSize(13.px); marginBottom(8.px); display(DisplayStyle.Block) } }) { Text("${items.size} registros") }
    Div({ style { display(DisplayStyle.Flex); gap(8.px); marginBottom(16.px); flexWrap(FlexWrap.Wrap); alignItems(AlignItems.Center) } }) {
        Input(InputType.Text) { placeholder("Fecha *"); value(f_fecha); onInput { f_fecha = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(120.px) } }
        Input(InputType.Text) { placeholder("Tema (Extintores/Primeros auxil.)"); value(f_tema); onInput { f_tema = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(220.px) } }
        Input(InputType.Text) { placeholder("Instructor"); value(f_instructor); onInput { f_instructor = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(150.px) } }
        Input(InputType.Text) { placeholder("Asistentes"); value(f_asistentes); onInput { f_asistentes = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(100.px) } }
        Input(InputType.Text) { placeholder("Vigencia (meses)"); value(f_vigenciaMeses); onInput { f_vigenciaMeses = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(120.px) } }
        Input(InputType.Text) { placeholder("Prox. fecha"); value(f_proximaFecha); onInput { f_proximaFecha = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(120.px) } }
        Input(InputType.Text) { placeholder("Estado"); value(f_estado); onInput { f_estado = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(100.px) } }
        Button({ style { padding(8.px, 16.px); backgroundColor(SidebarActiveColor); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") }; onClick { if (f_fecha.isNotBlank()) { scope.launch { client.post("$BACKEND_URL/api/v1/sap/ehs/capacitaciones") { contentType(ContentType.Application.Json); setBody(SafetyTraining(fecha = f_fecha, tema = f_tema, instructor = f_instructor, asistentes = f_asistentes, vigenciaMeses = f_vigenciaMeses, proximaFecha = f_proximaFecha, estado = f_estado)) }; f_fecha = ""; f_tema = ""; f_instructor = ""; f_asistentes = ""; f_vigenciaMeses = ""; f_proximaFecha = ""; f_estado = ""; refresh() } } else { window.alert("La fecha es obligatoria.") } } }) { Text("+ Agregar") }
    }
    if (isLoading) { P { Text("Cargando...") } } else {
        Table({ style { width(100.percent) } }) {
            Thead { Tr { Th { Text("Fecha") }; Th { Text("Tema") }; Th { Text("Instructor") }; Th { Text("Asist.") }; Th { Text("Vigencia") }; Th { Text("Prox.Fecha") }; Th { Text("Estado") }; Th { Text("") } } }
            Tbody { items.forEach { row -> Tr { Td { Text(row.fecha) }; Td { Text(row.tema) }; Td { Text(row.instructor) }; Td { Text(row.asistentes) }; Td { Text(row.vigenciaMeses) }; Td { Text(row.proximaFecha) }; Td { Text(row.estado) }; Td { Button({ style { backgroundColor(Color("#ef4444")); color(Color.white); property("border", "none"); borderRadius(4.px); padding(4.px, 10.px); cursor("pointer") }; onClick { scope.launch { client.delete("$BACKEND_URL/api/v1/sap/ehs/capacitaciones/${row.id}"); refresh() } } }) { Text("X") } } } } }
        }
    }
}

// EHS-6. Simulacros de Emergencia
@Composable
fun EhsDrillsTab(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope) {
    var items by remember { mutableStateOf(emptyList<EmergencyDrill>()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }
    LaunchedEffect(refreshKey) { isLoading = true; try { items = client.get("$BACKEND_URL/api/v1/sap/ehs/simulacros").body() } catch (e: Exception) { println("Err: ${e.message}") } finally { isLoading = false } }
    fun refresh() { refreshKey++ }
    var f_fecha by remember { mutableStateOf("") }
    var f_tipo by remember { mutableStateOf("") }
    var f_participantes by remember { mutableStateOf("") }
    var f_tiempoEvacuacion by remember { mutableStateOf("") }
    var f_resultado by remember { mutableStateOf("") }
    var f_observaciones by remember { mutableStateOf("") }
    var f_estado by remember { mutableStateOf("") }
    Span({ style { color(Color.gray); fontSize(13.px); marginBottom(8.px); display(DisplayStyle.Block) } }) { Text("${items.size} registros") }
    Div({ style { display(DisplayStyle.Flex); gap(8.px); marginBottom(16.px); flexWrap(FlexWrap.Wrap); alignItems(AlignItems.Center) } }) {
        Input(InputType.Text) { placeholder("Fecha *"); value(f_fecha); onInput { f_fecha = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(120.px) } }
        Input(InputType.Text) { placeholder("Tipo (Incendio/Sismo/Derrame)"); value(f_tipo); onInput { f_tipo = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(200.px) } }
        Input(InputType.Text) { placeholder("Participantes"); value(f_participantes); onInput { f_participantes = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(120.px) } }
        Input(InputType.Text) { placeholder("Tiempo evacuacion"); value(f_tiempoEvacuacion); onInput { f_tiempoEvacuacion = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(140.px) } }
        Input(InputType.Text) { placeholder("Resultado"); value(f_resultado); onInput { f_resultado = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(160.px) } }
        Input(InputType.Text) { placeholder("Observaciones"); value(f_observaciones); onInput { f_observaciones = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(200.px) } }
        Input(InputType.Text) { placeholder("Estado"); value(f_estado); onInput { f_estado = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(100.px) } }
        Button({ style { padding(8.px, 16.px); backgroundColor(SidebarActiveColor); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") }; onClick { if (f_fecha.isNotBlank()) { scope.launch { client.post("$BACKEND_URL/api/v1/sap/ehs/simulacros") { contentType(ContentType.Application.Json); setBody(EmergencyDrill(fecha = f_fecha, tipo = f_tipo, participantes = f_participantes, tiempoEvacuacion = f_tiempoEvacuacion, resultado = f_resultado, observaciones = f_observaciones, estado = f_estado)) }; f_fecha = ""; f_tipo = ""; f_participantes = ""; f_tiempoEvacuacion = ""; f_resultado = ""; f_observaciones = ""; f_estado = ""; refresh() } } else { window.alert("La fecha es obligatoria.") } } }) { Text("+ Agregar") }
    }
    if (isLoading) { P { Text("Cargando...") } } else {
        Table({ style { width(100.percent) } }) {
            Thead { Tr { Th { Text("Fecha") }; Th { Text("Tipo") }; Th { Text("Particip.") }; Th { Text("T.Evacuac.") }; Th { Text("Resultado") }; Th { Text("Observaciones") }; Th { Text("Estado") }; Th { Text("") } } }
            Tbody { items.forEach { row -> Tr { Td { Text(row.fecha) }; Td { Text(row.tipo) }; Td { Text(row.participantes) }; Td { Text(row.tiempoEvacuacion) }; Td { Text(row.resultado) }; Td { Text(row.observaciones) }; Td { Text(row.estado) }; Td { Button({ style { backgroundColor(Color("#ef4444")); color(Color.white); property("border", "none"); borderRadius(4.px); padding(4.px, 10.px); cursor("pointer") }; onClick { scope.launch { client.delete("$BACKEND_URL/api/v1/sap/ehs/simulacros/${row.id}"); refresh() } } }) { Text("X") } } } } }
        }
    }
}

// EHS-7. Matriz de Riesgos / IPER
@Composable
fun EhsRiskMatrixTab(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope) {
    var items by remember { mutableStateOf(emptyList<RiskMatrix>()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }
    LaunchedEffect(refreshKey) { isLoading = true; try { items = client.get("$BACKEND_URL/api/v1/sap/ehs/matriz-riesgos").body() } catch (e: Exception) { println("Err: ${e.message}") } finally { isLoading = false } }
    fun refresh() { refreshKey++ }
    var f_area by remember { mutableStateOf("") }
    var f_proceso by remember { mutableStateOf("") }
    var f_riesgoIdentificado by remember { mutableStateOf("") }
    var f_probabilidad by remember { mutableStateOf("") }
    var f_severidad by remember { mutableStateOf("") }
    var f_nivelRiesgo by remember { mutableStateOf("") }
    var f_controles by remember { mutableStateOf("") }
    var f_responsable by remember { mutableStateOf("") }
    var f_estado by remember { mutableStateOf("") }
    Span({ style { color(Color.gray); fontSize(13.px); marginBottom(8.px); display(DisplayStyle.Block) } }) { Text("${items.size} registros") }
    Div({ style { display(DisplayStyle.Flex); gap(8.px); marginBottom(16.px); flexWrap(FlexWrap.Wrap); alignItems(AlignItems.Center) } }) {
        Input(InputType.Text) { placeholder("Area *"); value(f_area); onInput { f_area = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(140.px) } }
        Input(InputType.Text) { placeholder("Proceso"); value(f_proceso); onInput { f_proceso = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(140.px) } }
        Input(InputType.Text) { placeholder("Riesgo identificado"); value(f_riesgoIdentificado); onInput { f_riesgoIdentificado = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(200.px) } }
        Input(InputType.Text) { placeholder("Probabilidad (B/M/A)"); value(f_probabilidad); onInput { f_probabilidad = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(160.px) } }
        Input(InputType.Text) { placeholder("Severidad (B/M/A)"); value(f_severidad); onInput { f_severidad = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(160.px) } }
        Input(InputType.Text) { placeholder("Nivel riesgo"); value(f_nivelRiesgo); onInput { f_nivelRiesgo = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(120.px) } }
        Input(InputType.Text) { placeholder("Controles"); value(f_controles); onInput { f_controles = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(200.px) } }
        Input(InputType.Text) { placeholder("Responsable"); value(f_responsable); onInput { f_responsable = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(150.px) } }
        Input(InputType.Text) { placeholder("Estado"); value(f_estado); onInput { f_estado = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(100.px) } }
        Button({ style { padding(8.px, 16.px); backgroundColor(SidebarActiveColor); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") }; onClick { if (f_area.isNotBlank()) { scope.launch { client.post("$BACKEND_URL/api/v1/sap/ehs/matriz-riesgos") { contentType(ContentType.Application.Json); setBody(RiskMatrix(area = f_area, proceso = f_proceso, riesgoIdentificado = f_riesgoIdentificado, probabilidad = f_probabilidad, severidad = f_severidad, nivelRiesgo = f_nivelRiesgo, controles = f_controles, responsable = f_responsable, estado = f_estado)) }; f_area = ""; f_proceso = ""; f_riesgoIdentificado = ""; f_probabilidad = ""; f_severidad = ""; f_nivelRiesgo = ""; f_controles = ""; f_responsable = ""; f_estado = ""; refresh() } } else { window.alert("El area es obligatoria.") } } }) { Text("+ Agregar") }
    }
    if (isLoading) { P { Text("Cargando...") } } else {
        Table({ style { width(100.percent) } }) {
            Thead { Tr { Th { Text("Area") }; Th { Text("Proceso") }; Th { Text("Riesgo") }; Th { Text("Prob.") }; Th { Text("Sev.") }; Th { Text("Nivel") }; Th { Text("Controles") }; Th { Text("Responsable") }; Th { Text("Estado") }; Th { Text("") } } }
            Tbody { items.forEach { row -> Tr { Td { Text(row.area) }; Td { Text(row.proceso) }; Td { Text(row.riesgoIdentificado) }; Td { Text(row.probabilidad) }; Td { Text(row.severidad) }; Td { Text(row.nivelRiesgo) }; Td { Text(row.controles) }; Td { Text(row.responsable) }; Td { Text(row.estado) }; Td { Button({ style { backgroundColor(Color("#ef4444")); color(Color.white); property("border", "none"); borderRadius(4.px); padding(4.px, 10.px); cursor("pointer") }; onClick { scope.launch { client.delete("$BACKEND_URL/api/v1/sap/ehs/matriz-riesgos/${row.id}"); refresh() } } }) { Text("X") } } } } }
        }
    }
}

@Composable
fun GrcSecurityModule(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope, t: Translations) {
    var items by remember { mutableStateOf(emptyList<AccessAuditLog>()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey) {
        isLoading = true
        try {
            items = client.get("$BACKEND_URL/api/v1/sap/grc/auditoria-accesos").body()
        } catch (e: Exception) {
            println("Error cargando GrcSecurityModule: ${e.message}")
        } finally {
            isLoading = false
        }
    }
    fun refresh() { refreshKey++ }

    var f_fecha by remember { mutableStateOf("") }
    var f_usuario by remember { mutableStateOf("") }
    var f_accion by remember { mutableStateOf("") }
    var f_modulo by remember { mutableStateOf("") }
    var f_resultado by remember { mutableStateOf("") }

    Div({ style { backgroundColor(Color.white); padding(32.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        Div({ style { display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); alignItems(AlignItems.Center); marginBottom(16.px) } }) {
            H3({ style { margin(0.px) } }) { Text("SAP Security / GRC · Gobierno, Riesgo y Cumplimiento") }
            Span({ style { color(Color.gray); fontSize(13.px) } }) { Text("${items.size} registros") }
        }

        Div({ style { display(DisplayStyle.Flex); gap(8.px); marginBottom(16.px); flexWrap(FlexWrap.Wrap); alignItems(AlignItems.Center) } }) {
            Input(InputType.Text) { placeholder("Fecha *"); value(f_fecha); onInput { f_fecha = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(130.px) } }
            Input(InputType.Text) { placeholder("Usuario *"); value(f_usuario); onInput { f_usuario = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(180.px) } }
            Input(InputType.Text) { placeholder("Accion realizada"); value(f_accion); onInput { f_accion = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(220.px) } }
            Input(InputType.Text) { placeholder("Modulo"); value(f_modulo); onInput { f_modulo = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(140.px) } }
            Input(InputType.Text) { placeholder("Resultado (Permitido/Denegado)"); value(f_resultado); onInput { f_resultado = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(200.px) } }
            Button({
                style { padding(8.px, 16.px); backgroundColor(SidebarActiveColor); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") }
                onClick {
                    if (f_fecha.isNotBlank()) {
                        scope.launch {
                            client.post("$BACKEND_URL/api/v1/sap/grc/auditoria-accesos") {
                                contentType(ContentType.Application.Json)
                                setBody(AccessAuditLog(fecha = f_fecha, usuario = f_usuario, accion = f_accion, modulo = f_modulo, resultado = f_resultado))
                            }
                            f_fecha = ""
                            f_usuario = ""
                            f_accion = ""
                            f_modulo = ""
                            f_resultado = ""
                            refresh()
                        }
                    } else {
                        window.alert("Completa el campo obligatorio para agregar el registro.")
                    }
                }
            }) { Text("+ Agregar") }
        }

        if (isLoading) {
            P { Text("Cargando...") }
        } else {
            Table({ style { width(100.percent) } }) {
                Thead { Tr { Th { Text("Fecha") }; Th { Text("Usuario") }; Th { Text("Accion") }; Th { Text("Modulo") }; Th { Text("Resultado") }; Th { Text("") } } }
                Tbody {
                    items.forEach { row ->
                        Tr {
                            Td { Text(row.fecha) }; Td { Text(row.usuario) }; Td { Text(row.accion) }; Td { Text(row.modulo) }; Td { Text(row.resultado) }
                            Td {
                                Button({
                                    style { backgroundColor(Color("#ef4444")); color(Color.white); property("border", "none"); borderRadius(4.px); padding(4.px, 10.px); cursor("pointer") }
                                    onClick { scope.launch { client.delete("$BACKEND_URL/api/v1/sap/grc/auditoria-accesos/${row.id}"); refresh() } }
                                }) { Text("Eliminar") }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Modulos estilo SAP: FI (Contabilidad Financiera), PM (Mantenimiento de Planta), HCM (Reclutamiento)
// Mismo patron que SapModulesUi.kt: HttpClient + LaunchedEffect + formulario inline + tabla + delete

@Composable
fun FinancialAccountingModule(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope, t: Translations) {
    var items by remember { mutableStateOf(emptyList<JournalEntry>()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey) {
        isLoading = true
        try {
            items = client.get("$BACKEND_URL/api/v1/sap/fi/asientos").body()
        } catch (e: Exception) {
            println("Error cargando FinancialAccountingModule: ${e.message}")
        } finally {
            isLoading = false
        }
    }
    fun refresh() { refreshKey++ }

    var f_fecha by remember { mutableStateOf("") }
    var f_cuenta by remember { mutableStateOf("") }
    var f_concepto by remember { mutableStateOf("") }
    var f_tipo by remember { mutableStateOf("") }
    var f_monto by remember { mutableStateOf("") }
    var f_referencia by remember { mutableStateOf("") }

    Div({ style { backgroundColor(Color.white); padding(32.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        Div({ style { display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); alignItems(AlignItems.Center); marginBottom(16.px) } }) {
            H3({ style { margin(0.px) } }) { Text("FI · Contabilidad Financiera (Financial Accounting)") }
            Span({ style { color(Color.gray); fontSize(13.px) } }) { Text("${items.size} registros") }
        }

        Div({ style { display(DisplayStyle.Flex); gap(8.px); marginBottom(16.px); flexWrap(FlexWrap.Wrap); alignItems(AlignItems.Center) } }) {
            Input(InputType.Text) { placeholder("Fecha *"); value(f_fecha); onInput { f_fecha = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(130.px) } }
            Input(InputType.Text) { placeholder("Cuenta contable *"); value(f_cuenta); onInput { f_cuenta = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(180.px) } }
            Input(InputType.Text) { placeholder("Concepto"); value(f_concepto); onInput { f_concepto = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(220.px) } }
            Input(InputType.Text) { placeholder("Tipo (Cargo/Abono)"); value(f_tipo); onInput { f_tipo = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(150.px) } }
            Input(InputType.Text) { placeholder("Monto"); value(f_monto); onInput { f_monto = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(130.px) } }
            Input(InputType.Text) { placeholder("Referencia"); value(f_referencia); onInput { f_referencia = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(160.px) } }
            Button({
                style { padding(8.px, 16.px); backgroundColor(SidebarActiveColor); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") }
                onClick {
                    if (f_fecha.isNotBlank()) {
                        scope.launch {
                            client.post("$BACKEND_URL/api/v1/sap/fi/asientos") {
                                contentType(ContentType.Application.Json)
                                setBody(JournalEntry(fecha = f_fecha, cuenta = f_cuenta, concepto = f_concepto, tipo = f_tipo, monto = f_monto, referencia = f_referencia))
                            }
                            f_fecha = ""
                            f_cuenta = ""
                            f_concepto = ""
                            f_tipo = ""
                            f_monto = ""
                            f_referencia = ""
                            refresh()
                        }
                    } else {
                        window.alert("Completa el campo obligatorio para agregar el registro.")
                    }
                }
            }) { Text("+ Agregar") }
        }

        if (isLoading) {
            P { Text("Cargando...") }
        } else {
            Table({ style { width(100.percent) } }) {
                Thead { Tr { Th { Text("Fecha") }; Th { Text("Cuenta") }; Th { Text("Concepto") }; Th { Text("Tipo") }; Th { Text("Monto") }; Th { Text("Referencia") }; Th { Text("") } } }
                Tbody {
                    items.forEach { row ->
                        Tr {
                            Td { Text(row.fecha) }; Td { Text(row.cuenta) }; Td { Text(row.concepto) }; Td { Text(row.tipo) }; Td { Text(row.monto) }; Td { Text(row.referencia) }
                            Td {
                                Button({
                                    style { backgroundColor(Color("#ef4444")); color(Color.white); property("border", "none"); borderRadius(4.px); padding(4.px, 10.px); cursor("pointer") }
                                    onClick { scope.launch { client.delete("$BACKEND_URL/api/v1/sap/fi/asientos/${row.id}"); refresh() } }
                                }) { Text("Eliminar") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlantMaintenanceModule(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope, t: Translations) {
    var items by remember { mutableStateOf(emptyList<MaintenanceOrder>()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey) {
        isLoading = true
        try {
            items = client.get("$BACKEND_URL/api/v1/sap/pm/ordenes-mantenimiento").body()
        } catch (e: Exception) {
            println("Error cargando PlantMaintenanceModule: ${e.message}")
        } finally {
            isLoading = false
        }
    }
    fun refresh() { refreshKey++ }

    var f_equipo by remember { mutableStateOf("") }
    var f_tipo by remember { mutableStateOf("") }
    var f_fechaProgramada by remember { mutableStateOf("") }
    var f_fechaRealizada by remember { mutableStateOf("") }
    var f_tecnico by remember { mutableStateOf("") }
    var f_estado by remember { mutableStateOf("") }
    var f_notas by remember { mutableStateOf("") }

    Div({ style { backgroundColor(Color.white); padding(32.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        Div({ style { display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); alignItems(AlignItems.Center); marginBottom(16.px) } }) {
            H3({ style { margin(0.px) } }) { Text("PM · Mantenimiento de Planta (Plant Maintenance)") }
            Span({ style { color(Color.gray); fontSize(13.px) } }) { Text("${items.size} registros") }
        }

        Div({ style { display(DisplayStyle.Flex); gap(8.px); marginBottom(16.px); flexWrap(FlexWrap.Wrap); alignItems(AlignItems.Center) } }) {
            Input(InputType.Text) { placeholder("Equipo *"); value(f_equipo); onInput { f_equipo = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(180.px) } }
            Input(InputType.Text) { placeholder("Tipo (Preventivo/Correctivo)"); value(f_tipo); onInput { f_tipo = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(200.px) } }
            Input(InputType.Text) { placeholder("Fecha programada"); value(f_fechaProgramada); onInput { f_fechaProgramada = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(140.px) } }
            Input(InputType.Text) { placeholder("Fecha realizada"); value(f_fechaRealizada); onInput { f_fechaRealizada = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(140.px) } }
            Input(InputType.Text) { placeholder("Tecnico"); value(f_tecnico); onInput { f_tecnico = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(160.px) } }
            Input(InputType.Text) { placeholder("Estado"); value(f_estado); onInput { f_estado = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(120.px) } }
            Input(InputType.Text) { placeholder("Notas"); value(f_notas); onInput { f_notas = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(220.px) } }
            Button({
                style { padding(8.px, 16.px); backgroundColor(SidebarActiveColor); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") }
                onClick {
                    if (f_equipo.isNotBlank()) {
                        scope.launch {
                            client.post("$BACKEND_URL/api/v1/sap/pm/ordenes-mantenimiento") {
                                contentType(ContentType.Application.Json)
                                setBody(MaintenanceOrder(equipo = f_equipo, tipo = f_tipo, fechaProgramada = f_fechaProgramada, fechaRealizada = f_fechaRealizada, tecnico = f_tecnico, estado = f_estado, notas = f_notas))
                            }
                            f_equipo = ""
                            f_tipo = ""
                            f_fechaProgramada = ""
                            f_fechaRealizada = ""
                            f_tecnico = ""
                            f_estado = ""
                            f_notas = ""
                            refresh()
                        }
                    } else {
                        window.alert("Completa el campo obligatorio para agregar el registro.")
                    }
                }
            }) { Text("+ Agregar") }
        }

        if (isLoading) {
            P { Text("Cargando...") }
        } else {
            Table({ style { width(100.percent) } }) {
                Thead { Tr { Th { Text("Equipo") }; Th { Text("Tipo") }; Th { Text("F. Programada") }; Th { Text("F. Realizada") }; Th { Text("Tecnico") }; Th { Text("Estado") }; Th { Text("Notas") }; Th { Text("") } } }
                Tbody {
                    items.forEach { row ->
                        Tr {
                            Td { Text(row.equipo) }; Td { Text(row.tipo) }; Td { Text(row.fechaProgramada) }; Td { Text(row.fechaRealizada) }; Td { Text(row.tecnico) }; Td { Text(row.estado) }; Td { Text(row.notas) }
                            Td {
                                Button({
                                    style { backgroundColor(Color("#ef4444")); color(Color.white); property("border", "none"); borderRadius(4.px); padding(4.px, 10.px); cursor("pointer") }
                                    onClick { scope.launch { client.delete("$BACKEND_URL/api/v1/sap/pm/ordenes-mantenimiento/${row.id}"); refresh() } }
                                }) { Text("Eliminar") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecruitmentSapModule(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope, t: Translations) {
    var items by remember { mutableStateOf(emptyList<RecruitmentVacancy>()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey) {
        isLoading = true
        try {
            items = client.get("$BACKEND_URL/api/v1/sap/hcm/vacantes").body()
        } catch (e: Exception) {
            println("Error cargando RecruitmentSapModule: ${e.message}")
        } finally {
            isLoading = false
        }
    }
    fun refresh() { refreshKey++ }

    var f_puesto by remember { mutableStateOf("") }
    var f_departamento by remember { mutableStateOf("") }
    var f_fechaApertura by remember { mutableStateOf("") }
    var f_vacantes by remember { mutableStateOf("") }
    var f_candidatosPostulados by remember { mutableStateOf("") }
    var f_estado by remember { mutableStateOf("") }

    Div({ style { backgroundColor(Color.white); padding(32.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        Div({ style { display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); alignItems(AlignItems.Center); marginBottom(16.px) } }) {
            H3({ style { margin(0.px) } }) { Text("HCM · Reclutamiento (Human Capital Management)") }
            Span({ style { color(Color.gray); fontSize(13.px) } }) { Text("${items.size} registros") }
        }

        Div({ style { display(DisplayStyle.Flex); gap(8.px); marginBottom(16.px); flexWrap(FlexWrap.Wrap); alignItems(AlignItems.Center) } }) {
            Input(InputType.Text) { placeholder("Puesto *"); value(f_puesto); onInput { f_puesto = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(180.px) } }
            Input(InputType.Text) { placeholder("Departamento"); value(f_departamento); onInput { f_departamento = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(160.px) } }
            Input(InputType.Text) { placeholder("Fecha apertura"); value(f_fechaApertura); onInput { f_fechaApertura = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(130.px) } }
            Input(InputType.Text) { placeholder("No. vacantes"); value(f_vacantes); onInput { f_vacantes = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(110.px) } }
            Input(InputType.Text) { placeholder("Candidatos postulados"); value(f_candidatosPostulados); onInput { f_candidatosPostulados = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(160.px) } }
            Input(InputType.Text) { placeholder("Estado"); value(f_estado); onInput { f_estado = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(120.px) } }
            Button({
                style { padding(8.px, 16.px); backgroundColor(SidebarActiveColor); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") }
                onClick {
                    if (f_puesto.isNotBlank()) {
                        scope.launch {
                            client.post("$BACKEND_URL/api/v1/sap/hcm/vacantes") {
                                contentType(ContentType.Application.Json)
                                setBody(RecruitmentVacancy(puesto = f_puesto, departamento = f_departamento, fechaApertura = f_fechaApertura, vacantes = f_vacantes, candidatosPostulados = f_candidatosPostulados, estado = f_estado))
                            }
                            f_puesto = ""
                            f_departamento = ""
                            f_fechaApertura = ""
                            f_vacantes = ""
                            f_candidatosPostulados = ""
                            f_estado = ""
                            refresh()
                        }
                    } else {
                        window.alert("Completa el campo obligatorio para agregar el registro.")
                    }
                }
            }) { Text("+ Agregar") }
        }

        if (isLoading) {
            P { Text("Cargando...") }
        } else {
            Table({ style { width(100.percent) } }) {
                Thead { Tr { Th { Text("Puesto") }; Th { Text("Departamento") }; Th { Text("Fecha Apertura") }; Th { Text("Vacantes") }; Th { Text("Candidatos") }; Th { Text("Estado") }; Th { Text("") } } }
                Tbody {
                    items.forEach { row ->
                        Tr {
                            Td { Text(row.puesto) }; Td { Text(row.departamento) }; Td { Text(row.fechaApertura) }; Td { Text(row.vacantes) }; Td { Text(row.candidatosPostulados) }; Td { Text(row.estado) }
                            Td {
                                Button({
                                    style { backgroundColor(Color("#ef4444")); color(Color.white); property("border", "none"); borderRadius(4.px); padding(4.px, 10.px); cursor("pointer") }
                                    onClick { scope.launch { client.delete("$BACKEND_URL/api/v1/sap/hcm/vacantes/${row.id}"); refresh() } }
                                }) { Text("Eliminar") }
                            }
                        }
                    }
                }
            }
        }
    }
}
