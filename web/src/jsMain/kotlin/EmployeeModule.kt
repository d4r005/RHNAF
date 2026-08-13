import com.example.rhnaf.shared.model.*
import androidx.compose.runtime.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.attributes.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.call.*
import kotlinx.coroutines.launch

@Composable
fun EmployeeModule(
    employees: List<Employee>,
    client: HttpClient,
    scope: kotlinx.coroutines.CoroutineScope,
    t: Translations,
    onEmployeesUpdated: (List<Employee>) -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    fun refresh() {
        scope.launch {
            isLoading = true
            errorMsg = ""
            try {
                val fresh: List<Employee> = client.get("$BACKEND_URL/api/employees").body()
                onEmployeesUpdated(fresh)
            } catch (e: Exception) {
                errorMsg = "No se pudo conectar: ${e.message}"
            }
            isLoading = false
        }
    }

    val withPhoto = employees.count { !it.photoUrl.isNullOrBlank() }
    val withoutPhoto = employees.size - withPhoto

    val filtered = employees.filter { e ->
        searchQuery.isBlank() ||
            e.id.contains(searchQuery, ignoreCase = true) ||
            "${e.firstName} ${e.lastName}".contains(searchQuery, ignoreCase = true) ||
            e.department.contains(searchQuery, ignoreCase = true)
    }

    Div({ style { backgroundColor(Color.white); padding(32.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        H3({ style { margin(0.px, 0.px, 4.px, 0.px) } }) { Text("Plantilla de Empleados") }
        P({ style { color(Color.gray); margin(0.px, 0.px, 16.px, 0.px); fontSize(14.px) } }) {
            Text("Ficha de cada empleado con su foto, tomadas directamente de la lectora Hikvision (via employee_sync.py)")
        }

        Div({ style { display(DisplayStyle.Flex); gap(12.px); marginBottom(20.px); flexWrap(FlexWrap.Wrap) } }) {
            EmpStatCard("Total Empleados", employees.size.toString())
            EmpStatCard("Con Foto", withPhoto.toString())
            EmpStatCard("Sin Foto", withoutPhoto.toString())
        }

        Div({ style { display(DisplayStyle.Flex); gap(12.px); marginBottom(20.px); flexWrap(FlexWrap.Wrap); alignItems(AlignItems.Center) } }) {
            Input(InputType.Text) {
                style {
                    padding(8.px, 12.px); borderRadius(6.px); property("border", "1px solid #cbd5e1")
                    width(280.px); property("outline", "none")
                }
                placeholder("Buscar por nombre, ID o departamento...")
                value(searchQuery)
                onInput { searchQuery = it.value }
            }
            Button({
                style {
                    padding(8.px, 16.px); borderRadius(6.px); property("border", "none")
                    backgroundColor(SidebarActiveColor); color(Color.white); cursor("pointer")
                    fontWeight("bold")
                }
                onClick { refresh() }
            }) { Text(if (isLoading) "Actualizando..." else "Actualizar") }
        }

        if (withoutPhoto > 0) {
            Div({ style { padding(12.px, 16.px); backgroundColor(Color("#fffbeb")); borderRadius(8.px); marginBottom(20.px); property("border", "1px solid #fde68a") } }) {
                P({ style { margin(0.px); color(Color("#92400e")); fontSize(13.px) } }) {
                    Text("$withoutPhoto empleados aun no tienen foto. Corre employee_sync.py desde una PC en la red de la planta para jalar la foto y datos completos directo de la lectora.")
                }
            }
        }

        if (errorMsg.isNotEmpty()) {
            Div({ style { padding(16.px); backgroundColor(Color("#fef2f2")); borderRadius(8.px); color(Color("#dc2626")); marginBottom(20.px) } }) {
                P({ style { margin(0.px) } }) { Text(errorMsg) }
            }
        }

        if (employees.isEmpty()) {
            Div({ style { padding(40.px, 0.px); textAlign("center") } }) {
                P({ style { color(Color.gray); fontSize(16.px) } }) { Text("No hay empleados cargados aun") }
            }
        } else {
            Div({
                style {
                    display(DisplayStyle.Grid)
                    property("grid-template-columns", "repeat(auto-fill, minmax(220.px, 1fr))")
                    gap(16.px)
                }
            }) {
                filtered.sortedBy { it.id }.forEach { emp ->
                    EmployeeCard(emp)
                }
            }

            if (filtered.isEmpty()) {
                P({ style { textAlign("center"); color(Color("#94a3b8")); padding(24.px, 0.px) } }) {
                    Text("Ningun empleado coincide con la busqueda.")
                }
            }
        }
    }
}

@Composable
fun EmployeeCard(emp: Employee) {
    Div({
        style {
            padding(16.px); borderRadius(10.px)
            property("border", "1px solid #e2e8f0")
            display(DisplayStyle.Flex); flexDirection(FlexDirection.Column); alignItems(AlignItems.Center)
            gap(8.px)
            backgroundColor(Color("#f8fafc"))
        }
    }) {
        val photo = emp.photoUrl
        if (!photo.isNullOrBlank()) {
            Img(src = photo) {
                style {
                    width(72.px); height(72.px); borderRadius(50.percent)
                    property("object-fit", "cover")
                    property("border", "2px solid #e2e8f0")
                }
            }
        } else {
            val initials = "${emp.firstName.take(1)}${emp.lastName.take(1)}".uppercase().ifBlank { "?" }
            Div({
                style {
                    width(72.px); height(72.px); borderRadius(50.percent)
                    backgroundColor(SidebarActiveColor); color(Color.white)
                    display(DisplayStyle.Flex); alignItems(AlignItems.Center); justifyContent(JustifyContent.Center)
                    fontSize(22.px); fontWeight("bold")
                }
            }) { Text(initials) }
        }
        P({ style { margin(0.px); fontWeight("bold"); fontSize(14.px); textAlign("center") } }) {
            Text("${emp.firstName} ${emp.lastName}".trim().ifBlank { emp.id })
        }
        P({ style { margin(0.px); fontSize(12.px); color(Color("#64748b")) } }) { Text("ID: ${emp.id}") }
        Span({
            style {
                padding(2.px, 10.px); borderRadius(99.px); fontSize(11.px)
                backgroundColor(Color("#dbeafe")); color(Color("#1e40af"))
            }
        }) { Text(emp.department.ifBlank { "Sin depto" }) }
        P({ style { margin(0.px); fontSize(11.px); color(Color("#94a3b8")) } }) { Text(emp.position) }
    }
}

@Composable
fun EmpStatCard(label: String, value: String) {
    Div({
        style {
            flex(1); padding(16.px); borderRadius(8.px)
            backgroundColor(Color("#f8fafc")); property("border", "1px solid #e2e8f0")
            minWidth(140.px)
        }
    }) {
        P({ style { margin(0.px, 0.px, 4.px, 0.px); fontSize(11.px); color(Color("#94a3b8")); fontWeight("bold"); property("text-transform", "uppercase") } }) {
            Text(label)
        }
        P({ style { margin(0.px); fontSize(24.px); fontWeight("bold"); color(Color("#0f172a")) } }) {
            Text(value)
        }
    }
}
