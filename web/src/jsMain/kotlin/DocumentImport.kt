import org.jetbrains.compose.web.attributes.*
import io.ktor.http.*

import androidx.compose.runtime.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.css.*
import kotlinx.browser.window
import kotlinx.coroutines.launch
import kotlin.coroutines.suspendCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.int

// Importador automático de documentos: extrae filas de un PDF, Word (.docx) o
// Excel (.xlsx/.xls) y crea registros en CUALQUIER sección usando los mismos
// endpoints POST ya existentes. El mapeo es por encabezados: la primera fila
// del documento debe contener columnas reconocibles (Fecha, Tipo, Área...).
// Las filas que no tengan los campos obligatorios se omiten y se reportan.

private class ImportSpec(
    val label: String,
    val endpoint: String,
    val fields: Map<String, List<String>>, // campo JSON -> pistas de encabezado (minúsculas, sin acentos)
    val required: List<String> = listOf()
)

private fun norm(s: String): String {
    val flat = buildString {
        s.forEach { c ->
            when (c) {
                'á' -> append('a'); 'é' -> append('e'); 'í' -> append('i'); 'ó' -> append('o'); 'ú' -> append('u'); 'ü' -> append('u'); 'ñ' -> append('n')
                'Á' -> append('a'); 'É' -> append('e'); 'Í' -> append('i'); 'Ó' -> append('o'); 'Ú' -> append('u'); 'Ü' -> append('u'); 'Ñ' -> append('n')
                else -> append(c)
            }
        }
    }
    return flat.lowercase().trim()
}

private val IMPORT_SPECS: Map<String, ImportSpec> = mapOf(
    "inspecciones" to ImportSpec("EHS · Inspecciones", "/api/v1/sap/ehs/inspecciones", mapOf(
        "fecha" to listOf("fecha"), "tipoInspeccion" to listOf("tipo"), "area" to listOf("area"),
        "inspector" to listOf("inspector", "responsable"), "hallazgos" to listOf("hallazgo", "observaci"),
        "riesgo" to listOf("riesgo"), "accionesCorrectivas" to listOf("acci"), "fechaCierre" to listOf("cierre"),
        "estado" to listOf("estado", "estatus")), listOf("fecha")),
    "incidentes" to ImportSpec("EHS · Incidentes", "/api/v1/sap/ehs/incidentes", mapOf(
        "fecha" to listOf("fecha"), "tipo" to listOf("tipo"), "severidad" to listOf("severidad"),
        "personaAfectada" to listOf("persona", "afectad"), "departamento" to listOf("depto", "departament"),
        "parteCuerpo" to listOf("cuerpo"), "diasPerdidos" to listOf("dias"), "descripcion" to listOf("descripci"),
        "causaRaiz" to listOf("causa"), "accionesCorrectivas" to listOf("acci"), "estado" to listOf("estado")), listOf("fecha")),
    "permisos-trabajo" to ImportSpec("EHS · Permisos de Trabajo", "/api/v1/sap/ehs/permisos-trabajo", mapOf(
        "tipo" to listOf("tipo"), "solicitante" to listOf("solicitante"), "autorizadoPor" to listOf("autoriz"),
        "fechaInicio" to listOf("inicio"), "fechaFin" to listOf("fin"), "area" to listOf("area"),
        "riesgosIdentificados" to listOf("riesgo"), "eppRequerido" to listOf("epp"), "estado" to listOf("estado")), listOf("solicitante")),
    "entregas-epp" to ImportSpec("EHS · Entregas EPP", "/api/v1/sap/ehs/entregas-epp", mapOf(
        "fecha" to listOf("fecha"), "empleado" to listOf("empleado", "nombre"), "tipoEpp" to listOf("tipo", "epp"),
        "talla" to listOf("talla"), "proximaReposicion" to listOf("reposicion"), "firma" to listOf("firma")), listOf("fecha", "empleado")),
    "capacitaciones" to ImportSpec("EHS · Capacitaciones", "/api/v1/sap/ehs/capacitaciones", mapOf(
        "fecha" to listOf("fecha"), "tema" to listOf("tema"), "instructor" to listOf("instructor"),
        "asistentes" to listOf("asist"), "vigenciaMeses" to listOf("vigencia"), "proximaFecha" to listOf("prox"),
        "estado" to listOf("estado")), listOf("fecha")),
    "simulacros" to ImportSpec("EHS · Simulacros", "/api/v1/sap/ehs/simulacros", mapOf(
        "fecha" to listOf("fecha"), "tipo" to listOf("tipo"), "participantes" to listOf("particip"),
        "tiempoEvacuacion" to listOf("evacua"), "resultado" to listOf("resultado"),
        "observaciones" to listOf("observaci"), "estado" to listOf("estado")), listOf("fecha")),
    "matriz-riesgos" to ImportSpec("EHS · Matriz de Riesgos", "/api/v1/sap/ehs/matriz-riesgos", mapOf(
        "area" to listOf("area"), "proceso" to listOf("proceso"), "riesgoIdentificado" to listOf("riesgo"),
        "probabilidad" to listOf("probab"), "severidad" to listOf("severidad"), "nivelRiesgo" to listOf("nivel"),
        "controles" to listOf("control"), "responsable" to listOf("responsable"), "estado" to listOf("estado")), listOf("area")),
    "residuos" to ImportSpec("EHS · Residuos", "/api/v1/sap/ehs/residuos", mapOf(
        "fecha" to listOf("fecha"), "residuo" to listOf("residuo"), "tipo" to listOf("tipo"),
        "cantidad" to listOf("cant"), "unidad" to listOf("unidad"), "transportista" to listOf("transport"),
        "destinoFinal" to listOf("destino"), "numeroManifiesto" to listOf("manifiesto"), "estado" to listOf("estado")), listOf("fecha", "residuo")),
    "salud" to ImportSpec("EHS · Salud Ocupacional", "/api/v1/sap/ehs/salud", mapOf(
        "empleadoId" to listOf("empleado id", "id emp", "no. empleado", "numero empleado"), "nombreEmpleado" to listOf("nombre"),
        "fecha" to listOf("fecha"), "tipoExamen" to listOf("tipo examen", "examen"), "resultado" to listOf("resultado"),
        "observaciones" to listOf("observaci"), "proximaCita" to listOf("prox"), "medico" to listOf("medico")), listOf("empleadoId", "fecha")),
    "quimicos" to ImportSpec("EHS · Químicos", "/api/v1/sap/ehs/quimicos", mapOf(
        "nombre" to listOf("nombre", "producto"), "fabricante" to listOf("fabricante", "marca"), "areaUso" to listOf("area"),
        "nivelRiesgo" to listOf("riesgo", "nivel"), "hojaSeguridadUrl" to listOf("hoja", "msds"),
        "estado" to listOf("estado"), "ultimaRevision" to listOf("revision")), listOf("nombre")),
    "fi-asientos" to ImportSpec("FI · Asientos Contables", "/api/v1/sap/fi/asientos", mapOf(
        "fecha" to listOf("fecha"), "cuenta" to listOf("cuenta"), "concepto" to listOf("concepto"),
        "tipo" to listOf("tipo"), "monto" to listOf("monto", "importe"), "referencia" to listOf("referencia")), listOf("fecha", "cuenta")),
    "co-centros" to ImportSpec("CO · Centros de Costo", "/api/v1/sap/co/centros-costo", mapOf(
        "codigo" to listOf("codigo", "clave"), "nombre" to listOf("nombre"), "departamento" to listOf("departament"),
        "presupuestoMensual" to listOf("presupuesto"), "gastoActual" to listOf("gasto")), listOf("codigo")),
    "mm-compras" to ImportSpec("MM · Órdenes de Compra", "/api/v1/sap/mm/ordenes-compra", mapOf(
        "numero" to listOf("numero", "orden", "folio"), "proveedor" to listOf("proveedor"), "fecha" to listOf("fecha"),
        "descripcion" to listOf("descripci"), "montoTotal" to listOf("monto", "total", "importe"), "estado" to listOf("estado")), listOf("numero", "proveedor")),
    "pp-produccion" to ImportSpec("PP · Órdenes de Producción", "/api/v1/sap/pp/ordenes-produccion", mapOf(
        "numero" to listOf("numero", "orden"), "producto" to listOf("producto"), "cantidadPlan" to listOf("plan"),
        "cantidadProducida" to listOf("producida"), "centroTrabajo" to listOf("centro"), "fechaInicio" to listOf("inicio"),
        "fechaFin" to listOf("fin"), "estado" to listOf("estado")), listOf("numero", "producto")),
    "qm-inspecciones" to ImportSpec("QM · Inspecciones de Calidad", "/api/v1/sap/qm/inspecciones", mapOf(
        "fecha" to listOf("fecha"), "loteProducto" to listOf("lote", "producto"), "inspector" to listOf("inspector"),
        "resultado" to listOf("resultado"), "observaciones" to listOf("observaci")), listOf("fecha", "loteProducto")),
    "pm-mantenimiento" to ImportSpec("PM · Mantenimiento", "/api/v1/sap/pm/ordenes-mantenimiento", mapOf(
        "equipo" to listOf("equipo"), "tipo" to listOf("tipo"), "fechaProgramada" to listOf("programada"),
        "fechaRealizada" to listOf("realizada"), "tecnico" to listOf("tecnico"), "estado" to listOf("estado"),
        "notas" to listOf("notas")), listOf("equipo")),
    "ewm-tareas" to ImportSpec("EWM · Tareas de Almacén", "/api/v1/sap/ewm/tareas", mapOf(
        "tipo" to listOf("tipo"), "bin" to listOf("bin", "ubicac"), "sku" to listOf("sku"),
        "cantidad" to listOf("cant"), "asignadoA" to listOf("asignad"), "estado" to listOf("estado")), listOf("tipo")),
    "hcm-vacantes" to ImportSpec("HCM · Vacantes", "/api/v1/sap/hcm/vacantes", mapOf(
        "puesto" to listOf("puesto"), "departamento" to listOf("departament"), "fechaApertura" to listOf("apertura"),
        "vacantes" to listOf("vacante"), "candidatosPostulados" to listOf("candidat"), "estado" to listOf("estado")), listOf("puesto")),
    "gts-pedimentos" to ImportSpec("GTS · Pedimentos", "/api/v1/sap/gts/pedimentos", mapOf(
        "numeroPedimento" to listOf("pedimento", "numero"), "fecha" to listOf("fecha"), "cliente" to listOf("cliente"),
        "paisDestino" to listOf("pais"), "valorAduana" to listOf("valor", "aduana"), "regimen" to listOf("regimen"),
        "estado" to listOf("estado")), listOf("numeroPedimento")),
    "grc-auditoria" to ImportSpec("GRC · Auditoría de Accesos", "/api/v1/sap/grc/auditoria-accesos", mapOf(
        "fecha" to listOf("fecha"), "usuario" to listOf("usuario", "user"), "accion" to listOf("accion"),
        "modulo" to listOf("modulo"), "resultado" to listOf("resultado")), listOf("fecha", "usuario"))
)

private fun escapeJson(s: String) = s.replace("\\", "\\\\").replace("\"", "\\\"")
    .replace("\n", " ").replace("\r", " ").replace("\t", " ")

// Mapea los encabezados de la primera fila a campos del modelo. Devuelve
// pares (registroJson, valido) por cada fila posterior.
private fun mapRows(spec: ImportSpec, rows: List<List<String>>): List<Pair<String, Boolean>> {
    if (rows.isEmpty()) return emptyList()
    val headers = rows.first().map { norm(it) }
    val colIndex = spec.fields.mapValues { (_, hints) ->
        headers.indexOfFirst { h -> hints.any { h.contains(it) } }
    }
    if (colIndex.values.all { it < 0 }) throw IllegalStateException(
        "No se reconoció ningún encabezado. La primera fila debe nombrar las columnas (p. ej. ${spec.fields.keys.take(3).joinToString(", ")}).")
    return rows.drop(1).map { row ->
        val parts = spec.fields.mapNotNull { (field, _) ->
            val idx = colIndex[field] ?: return@mapNotNull null
            val value = row.getOrNull(idx)?.trim().orEmpty()
            if (value.isEmpty()) null else "\"$field\":\"${escapeJson(value)}\""
        }
        // Requerido válido si: la columna existe y tiene valor, o el campo llegó de otra forma.
        val requiredOk = spec.required.all { field ->
            val idx = colIndex[field] ?: -1
            (idx >= 0 && !row.getOrNull(idx)?.trim().isNullOrBlank()) ||
                parts.any { it.startsWith("\"$field\"") }
        }
        "{" + parts.joinToString(",") + "}" to (parts.isNotEmpty() && requiredOk)
    }.filter { it.first.length > 2 }
}

private suspend fun postJson(endpoint: String, json: String): Boolean =
    suspendCoroutine { continuation ->
        val options = js("({})")
        options.method = "POST"
        options.headers = js("({})")
        options.headers.Authorization = "Bearer $apiAuthToken"
        options.headers["Content-Type"] = "application/json"
        options.body = json
        window.asDynamic().fetch("$BACKEND_URL$endpoint", options).then(
            { response: dynamic -> continuation.resume(response.ok as Boolean) },
            { _: dynamic -> continuation.resumeWithException(IllegalStateException("Error de red")) }
        )
    }

private suspend fun extractRows(file: org.w3c.files.File): List<List<String>> =
    suspendCoroutine { continuation ->
        val form = js("new FormData()")
        form.append("file", file, file.name)
        val options = js("({})")
        options.method = "POST"
        options.headers = js("({})")
        options.headers.Authorization = "Bearer $apiAuthToken"
        options.body = form
        window.asDynamic().fetch("$BACKEND_URL/api/v1/documentos/extraer", options).then(
            { response: dynamic ->
                response.json().then({ result: dynamic ->
                    if (!response.ok) continuation.resumeWithException(IllegalStateException(result.message?.toString() ?: "Error HTTP ${response.status}"))
                    else {
                        val data: dynamic = result.rows
                        continuation.resume((0 until (data.length as Int)).map { i ->
                            val row: dynamic = data[i]
                            (0 until (row.length as Int)).map { j -> row[j].toString() }
                        })
                    }
                }, { _: dynamic -> continuation.resumeWithException(IllegalStateException("Respuesta ilegible")) })
            },
            { _: dynamic -> continuation.resumeWithException(IllegalStateException("Error de red")) }
        )
    }

/** Panel de importación: crea registros en cualquier sección desde un documento. */
@Composable
fun DocumentImportPanel(scope: kotlinx.coroutines.CoroutineScope, onImported: () -> Unit) {
    var entityKey by remember { mutableStateOf(IMPORT_SPECS.keys.first()) }
    var rows by remember { mutableStateOf(emptyList<List<String>>()) }
    var pending by remember { mutableStateOf(emptyList<Pair<String, Boolean>>()) }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }
    val spec = IMPORT_SPECS.getValue(entityKey)

    Div({ style { marginBottom(20.px); padding(12.px); backgroundColor(Color("#f8fafc")); borderRadius(8.px); property("border", "1px solid #e2e8f0") } }) {
        H4 { Text("Importar documento a cualquier sección") }
        P({ style { fontSize(13.px); color(Color.gray); marginBottom(8.px) } }) {
            Text("Carga un Excel, Word o PDF con encabezados en la primera fila (p. ej. Fecha, Tipo, Área). Los registros se crean automáticamente en la sección elegida; las filas sin datos obligatorios se omiten. Los archivos NO se guardan.")
        }
        Div({ style { display(DisplayStyle.Flex); gap(8.px); alignItems(AlignItems.Center); flexWrap(FlexWrap.Wrap) } }) {
            Select({
                onChange { entityKey = it.value ?: entityKey; rows = emptyList(); pending = emptyList() }
            }) {
                IMPORT_SPECS.forEach { (key, value) -> Option(key) { Text(value.label) } }
            }
            Input(InputType.File) {
                id("doc-import-file")
                attr("accept", ".pdf,.docx,.xlsx,.xls")
                style { fontSize(13.px) }
                onChange {
                    val file = (kotlinx.browser.document.getElementById("doc-import-file") as? org.w3c.dom.HTMLInputElement)?.files?.item(0)
                    if (file != null) {
                        if (file.size.toLong() > 15L * 1024 * 1024) msg = "Máximo 15 MiB."
                        else {
                            busy = true; msg = "Extrayendo ${file.name}..."
                            scope.launch {
                                try {
                                    rows = extractRows(file)
                                    pending = mapRows(spec, rows)
                                    msg = "${pending.count { it.second }} filas listas para crear en ${spec.label}; ${pending.count { !it.second }} se omitirán por faltar datos obligatorios. Revisa y confirma."
                                } catch (e: Exception) { rows = emptyList(); pending = emptyList(); msg = e.message ?: "No se pudo leer el documento" }
                                finally { busy = false }
                            }
                        }
                    }
                }
            }
            Button({
                style { padding(8.px, 16.px); backgroundColor(Color("#2563eb")); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") }
                onClick {
                    if (busy || pending.isEmpty()) return@onClick
                    busy = true; msg = "Creando registros..."
                    scope.launch {
                        var created = 0; var failed = 0; var skipped = 0
                        for ((json, valid) in pending) {
                            if (!valid) { skipped++; continue }
                            try { if (postJson(spec.endpoint, json)) created++ else failed++ }
                            catch (e: Exception) { failed++ }
                        }
                        busy = false; rows = emptyList(); pending = emptyList()
                        msg = "Importación terminada: $created creados, $failed fallidos, $skipped omitidos en ${spec.label}."
                        onImported()
                    }
                }
            }) { Text(if (busy) "Procesando..." else "Crear registros automáticamente") }
        }
        if (msg.isNotBlank()) P({ style { fontSize(13.px); color(Color("#2563eb")); marginBottom(0.px) } }) { Text(msg) }
        if (rows.isNotEmpty()) {
            P({ style { fontSize(12.px); color(Color.gray); marginBottom(4.px) } }) { Text("Vista previa (máx. 20 filas):") }
            Table {
                Tbody {
                    rows.take(20).forEach { row -> Tr { Td { Text(row.joinToString(" | ").take(400)) } } }
                }
            }
        }
    }
}

/**
 * Botón único para las evidencias que YA están en Drive (las que la app
 * muestra como "Evidencia sin registro vinculado"). No pide elegir archivo:
 * lee cada evidencia pendiente directamente desde Drive y crea su registro
 * en la sección correspondiente, dejando en blanco lo que el documento no
 * contenga (no se inventan datos). Cubre Inspecciones, Incidentes,
 * Capacitaciones, Simulacros, Medio Ambiente y Matriz de Riesgos.
 */
@Composable
fun AutoRegisterFromEvidencePanel(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope) {
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }

    Div({ style { marginBottom(20.px); padding(12.px); backgroundColor(Color("#ecfdf5")); borderRadius(8.px); property("border", "1px solid #a7f3d0") } }) {
        H4 { Text("Crear registros desde evidencias ya guardadas en Drive") }
        P({ style { fontSize(13.px); color(Color.gray); marginBottom(8.px) } }) {
            Text("No sube ni importa archivos nuevos: revisa las evidencias que ya están en Drive y aparecen como \"Evidencia sin registro vinculado\", y crea el registro correspondiente (Inspección, Incidente, Capacitación, Simulacro, Medio Ambiente, Matriz de Riesgos) usando los datos del propio documento. Los campos que el documento no tenga quedan vacíos para completarlos manualmente.")
        }
        Button({
            style { padding(8.px, 16.px); backgroundColor(Color("#059669")); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") }
            onClick {
                if (busy) return@onClick
                busy = true; msg = "Procesando evidencias pendientes..."
                scope.launch {
                    try {
                        val text = client.post("$BACKEND_URL/api/v1/ehs/documentos/generar-registros") {}.bodyAsText()
                        val obj = kotlinx.serialization.json.Json.parseToJsonElement(text).jsonObject
                        val creados = obj["creados"]?.jsonPrimitive?.int ?: 0
                        val omitidos = obj["omitidos"]?.jsonPrimitive?.int ?: 0
                        msg = "Listo: $creados registros creados y vinculados; $omitidos evidencias omitidas (revisa el detalle si esperabas más)."
                    } catch (e: Exception) {
                        msg = "No se pudo completar: ${e.message ?: "error del servidor"}"
                    } finally { busy = false }
                }
            }
        }) { Text(if (busy) "Procesando..." else "Crear registros automáticamente desde Drive") }
        if (msg.isNotBlank()) P({ style { fontSize(13.px); color(Color("#047857")); marginTop(8.px); marginBottom(0.px) } }) { Text(msg) }
    }
}
