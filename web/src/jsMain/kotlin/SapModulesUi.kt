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
fun ExtendedWarehouseModule(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope, t: Translations) {
    var items by remember { mutableStateOf(emptyList<WarehouseTask>()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey) {
        isLoading = true
        try {
            items = client.get("$BACKEND_URL/api/v1/sap/ewm/tareas").body()
        } catch (e: Exception) {
            println("Error cargando ExtendedWarehouseModule: ${e.message}")
        } finally {
            isLoading = false
        }
    }
    fun refresh() { refreshKey++ }

    var f_tipo by remember { mutableStateOf("") }
    var f_bin by remember { mutableStateOf("") }
    var f_sku by remember { mutableStateOf("") }
    var f_cantidad by remember { mutableStateOf("") }
    var f_asignadoA by remember { mutableStateOf("") }
    var f_estado by remember { mutableStateOf("") }

    Div({ style { backgroundColor(Color.white); padding(32.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        Div({ style { display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); alignItems(AlignItems.Center); marginBottom(16.px) } }) {
            H3({ style { margin(0.px) } }) { Text("EWM · Gestion Avanzada de Almacenes (Extended Warehouse Management)") }
            Span({ style { color(Color.gray); fontSize(13.px) } }) { Text("${items.size} registros") }
        }

        Div({ style { display(DisplayStyle.Flex); gap(8.px); marginBottom(16.px); flexWrap(FlexWrap.Wrap); alignItems(AlignItems.Center) } }) {
            Input(InputType.Text) { placeholder("Tipo (Picking/Putaway/Recuento) *"); value(f_tipo); onInput { f_tipo = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(220.px) } }
            Input(InputType.Text) { placeholder("Ubicacion / Bin"); value(f_bin); onInput { f_bin = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(140.px) } }
            Input(InputType.Text) { placeholder("SKU"); value(f_sku); onInput { f_sku = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(140.px) } }
            Input(InputType.Text) { placeholder("Cantidad"); value(f_cantidad); onInput { f_cantidad = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(110.px) } }
            Input(InputType.Text) { placeholder("Asignado a"); value(f_asignadoA); onInput { f_asignadoA = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(160.px) } }
            Input(InputType.Text) { placeholder("Estado"); value(f_estado); onInput { f_estado = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(120.px) } }
            Button({
                style { padding(8.px, 16.px); backgroundColor(SidebarActiveColor); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") }
                onClick {
                    if (f_tipo.isNotBlank()) {
                        scope.launch {
                            client.post("$BACKEND_URL/api/v1/sap/ewm/tareas") {
                                contentType(ContentType.Application.Json)
                                setBody(WarehouseTask(tipo = f_tipo, bin = f_bin, sku = f_sku, cantidad = f_cantidad, asignadoA = f_asignadoA, estado = f_estado))
                            }
                            f_tipo = ""
                            f_bin = ""
                            f_sku = ""
                            f_cantidad = ""
                            f_asignadoA = ""
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
                Thead { Tr { Th { Text("Tipo") }; Th { Text("Bin") }; Th { Text("SKU") }; Th { Text("Cantidad") }; Th { Text("Asignado a") }; Th { Text("Estado") }; Th { Text("") } } }
                Tbody {
                    items.forEach { row ->
                        Tr {
                            Td { Text(row.tipo) }; Td { Text(row.bin) }; Td { Text(row.sku) }; Td { Text(row.cantidad) }; Td { Text(row.asignadoA) }; Td { Text(row.estado) }
                            Td {
                                Button({
                                    style { backgroundColor(Color("#ef4444")); color(Color.white); property("border", "none"); borderRadius(4.px); padding(4.px, 10.px); cursor("pointer") }
                                    onClick { scope.launch { client.delete("$BACKEND_URL/api/v1/sap/ewm/tareas/${row.id}"); refresh() } }
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
    var items by remember { mutableStateOf(emptyList<SafetyInspection>()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey) {
        isLoading = true
        try {
            items = client.get("$BACKEND_URL/api/v1/sap/ehs/inspecciones").body()
        } catch (e: Exception) {
            println("Error cargando EhsAuditsModule: ${e.message}")
        } finally {
            isLoading = false
        }
    }
    fun refresh() { refreshKey++ }

    var f_fecha by remember { mutableStateOf("") }
    var f_area by remember { mutableStateOf("") }
    var f_inspector by remember { mutableStateOf("") }
    var f_hallazgos by remember { mutableStateOf("") }
    var f_riesgo by remember { mutableStateOf("") }
    var f_estado by remember { mutableStateOf("") }

    Div({ style { backgroundColor(Color.white); padding(32.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        Div({ style { display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); alignItems(AlignItems.Center); marginBottom(16.px) } }) {
            H3({ style { margin(0.px) } }) { Text("EHS · Auditorias de Seguridad, Salud y Ambiente") }
            Span({ style { color(Color.gray); fontSize(13.px) } }) { Text("${items.size} registros") }
        }

        Div({ style { display(DisplayStyle.Flex); gap(8.px); marginBottom(16.px); flexWrap(FlexWrap.Wrap); alignItems(AlignItems.Center) } }) {
            Input(InputType.Text) { placeholder("Fecha *"); value(f_fecha); onInput { f_fecha = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(130.px) } }
            Input(InputType.Text) { placeholder("Area"); value(f_area); onInput { f_area = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(160.px) } }
            Input(InputType.Text) { placeholder("Inspector"); value(f_inspector); onInput { f_inspector = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(160.px) } }
            Input(InputType.Text) { placeholder("Hallazgos"); value(f_hallazgos); onInput { f_hallazgos = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(220.px) } }
            Input(InputType.Text) { placeholder("Riesgo (Bajo/Medio/Alto/Critico)"); value(f_riesgo); onInput { f_riesgo = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(200.px) } }
            Input(InputType.Text) { placeholder("Estado"); value(f_estado); onInput { f_estado = it.value }; style { padding(8.px); borderRadius(6.px); property("border", "1px solid #cbd5e1"); width(120.px) } }
            Button({
                style { padding(8.px, 16.px); backgroundColor(SidebarActiveColor); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") }
                onClick {
                    if (f_fecha.isNotBlank()) {
                        scope.launch {
                            client.post("$BACKEND_URL/api/v1/sap/ehs/inspecciones") {
                                contentType(ContentType.Application.Json)
                                setBody(SafetyInspection(fecha = f_fecha, area = f_area, inspector = f_inspector, hallazgos = f_hallazgos, riesgo = f_riesgo, estado = f_estado))
                            }
                            f_fecha = ""
                            f_area = ""
                            f_inspector = ""
                            f_hallazgos = ""
                            f_riesgo = ""
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
                Thead { Tr { Th { Text("Fecha") }; Th { Text("Area") }; Th { Text("Inspector") }; Th { Text("Hallazgos") }; Th { Text("Riesgo") }; Th { Text("Estado") }; Th { Text("") } } }
                Tbody {
                    items.forEach { row ->
                        Tr {
                            Td { Text(row.fecha) }; Td { Text(row.area) }; Td { Text(row.inspector) }; Td { Text(row.hallazgos) }; Td { Text(row.riesgo) }; Td { Text(row.estado) }
                            Td {
                                Button({
                                    style { backgroundColor(Color("#ef4444")); color(Color.white); property("border", "none"); borderRadius(4.px); padding(4.px, 10.px); cursor("pointer") }
                                    onClick { scope.launch { client.delete("$BACKEND_URL/api/v1/sap/ehs/inspecciones/${row.id}"); refresh() } }
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
