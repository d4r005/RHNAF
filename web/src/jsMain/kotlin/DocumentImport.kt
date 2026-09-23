import androidx.compose.runtime.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.css.*
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.int

// El panel generico "Importar documento a cualquier seccion" (subir un
// Excel/Word/PDF y mapear encabezados) se elimino a peticion del usuario:
// solo queda el flujo de evidencias ya guardadas en Drive.

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
                        val status = obj["status"]?.jsonPrimitive?.content ?: "ok"
                        if (status == "error") {
                            val errMsg = obj["message"]?.jsonPrimitive?.content ?: "error del servidor"
                            msg = "No se pudo completar: $errMsg"
                        } else {
                            val creados = obj["creados"]?.jsonPrimitive?.int ?: 0
                            val omitidos = obj["omitidos"]?.jsonPrimitive?.int ?: 0
                            msg = "Listo: $creados registros creados y vinculados; $omitidos evidencias omitidas (revisa el detalle si esperabas más)."
                        }
                    } catch (e: Exception) {
                        msg = "No se pudo completar: ${e.message ?: "error del servidor"}"
                    } finally { busy = false }
                }
            }
        }) { Text(if (busy) "Procesando..." else "Crear registros automáticamente desde Drive") }
        if (msg.isNotBlank()) P({ style { fontSize(13.px); color(Color("#047857")); marginTop(8.px); marginBottom(0.px) } }) { Text(msg) }
    }
}
