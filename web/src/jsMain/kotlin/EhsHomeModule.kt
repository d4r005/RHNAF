import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.attributes.*
import io.ktor.client.HttpClient

/** Punto de entrada a capacidades existentes, sin prometer cumplimiento automático. */
@Composable
fun EhsHomeModule(role: UserRole, onSelect: (Module) -> Unit) {
    Div {
        H1({ style { marginBottom(4.px); color(Color("#0f172a")) } }) { Text("Centro EHS y cumplimiento") }
        P({ style { color(Color("#475569")); marginBottom(24.px) } }) {
            Text("Seguridad, salud, ambiente y control documental en una sola operación.")
        }
        H2({ style { margin(0.px, 0.px, 10.px, 0.px); fontSize(18.px); color(Color("#0f172a")) } }) { Text("Operación por pilar") }
        Div({ style { display(DisplayStyle.Grid); property("grid-template-columns", "repeat(auto-fit, minmax(230px, 1fr))"); gap(16.px); marginBottom(26.px) } }) {
            listOf(
                Triple(Module.EHS_SEGURIDAD, "Seguridad", "Auditorías internas, incidentes, permisos de trabajo, EPP, capacitación interna, simulacros, matriz de riesgos, químicos, DC-3, dictámenes y normativa."),
                Triple(Module.EHS_SALUD, "Salud Ocupacional", "Registros de salud ocupacional e inventario químico con hojas de seguridad."),
                Triple(Module.EHS_AMBIENTE, "Medio Ambiente", "Residuos, huella de carbono y estudios ambientales.")
            ).filter { (module, _, _) -> isModuleVisible(module, role) }.forEach { (module, title, description) ->
                Button({
                    style {
                        padding(22.px); textAlign("left"); backgroundColor(Color.white)
                        property("border", "1px solid #e2e8f0"); borderRadius(12.px)
                        cursor("pointer"); property("min-height", "150px")
                        property("border-top", "3px solid #2563eb")
                    }
                    onClick { onSelect(module) }
                }) {
                    H3({ style { margin(0.px, 0.px, 10.px, 0.px); color(Color("#0f172a")) } }) { Text(title) }
                    P({ style { margin(0.px); color(Color("#475569")) } }) { Text(description) }
                }
            }
        }
        H2({ style { margin(0.px, 0.px, 10.px, 0.px); fontSize(18.px); color(Color("#0f172a")) } }) { Text("Gestión y cumplimiento") }
        Div({ style { display(DisplayStyle.Grid); property("grid-template-columns", "repeat(auto-fit, minmax(230px, 1fr))"); gap(16.px) } }) {
            listOf(
                Triple(Module.EHS_CALENDAR, "Calendario", "Vencimientos de obligaciones, capacitaciones y simulacros."),
                Triple(Module.EHS_DOCUMENTS, "Evidencia documental", "Cargar, consultar y vincular archivos en Google Drive."),
                Triple(Module.LEGAL_MATRIX, "Matriz legal", "Obligaciones por categoría, aplicabilidad, responsables y vigencias."),
                Triple(Module.STPS, "Normas STPS", "Vista enfocada en obligaciones de seguridad y salud laboral.")
            ).plus(
                if (role == UserRole.ADMIN || role == UserRole.SEGURIDAD) listOf(
                    Triple(Module.EHS_METRICS, "Indicadores EHS", "Incidentes, días perdidos declarados, auditorías internas y capacitaciones por vencer."),
                    Triple(Module.EHS_ACTIONS, "Planes de acción", "Acciones correctivas, responsables, vencimientos y evidencia de cierre."),
                    Triple(Module.EHS_CONTRACTORS, "Contratistas", "Empresas, actividades, expedientes y vigencia documental."),
                    Triple(Module.EHS_ALERTS, "Avisos EHS", "Vencimientos de acciones, contratistas, capacitaciones y obligaciones."),
                    Triple(Module.EHS_RATES, "Tasas EHS", "Frecuencia y gravedad por millón de horas validadas.")) else emptyList()
            ).filter { (module, _, _) -> isModuleVisible(module, role) }.forEach { (module, title, description) ->
                Button({
                    style {
                        padding(22.px); textAlign("left"); backgroundColor(Color.white)
                        property("border", "1px solid #e2e8f0"); borderRadius(12.px)
                        cursor("pointer"); property("min-height", "150px")
                    }
                    onClick { onSelect(module) }
                }) {
                    H3({ style { margin(0.px, 0.px, 10.px, 0.px); color(Color("#0f172a")) } }) { Text(title) }
                    P({ style { margin(0.px); color(Color("#475569")) } }) { Text(description) }
                }
            }
        }
        P({ style { color(Color("#64748b")); marginTop(22.px); fontSize(13.px) } }) {
            Text("La matriz es una herramienta de seguimiento; no determina por sí sola el cumplimiento legal.")
        }
    }
}

@Composable
fun EhsDocumentsModule(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope, role: UserRole) {
    Div {
        H1({ style { marginBottom(4.px); color(Color("#0f172a")) } }) { Text("Evidencia documental") }
        P({ style { color(Color("#475569")); marginBottom(20.px) } }) {
            Text("Archivos en Google Drive, metadatos y vínculos con auditorías internas, simulacros y capacitación.")
        }
        if (role == UserRole.ADMIN || role == UserRole.SEGURIDAD) {
            EhsDocumentsTab(client, scope)
        } else {
            P { Text("Solo el personal de Seguridad puede cargar o gestionar evidencia documental.") }
        }
    }
}
