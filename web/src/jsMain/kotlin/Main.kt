import androidx.compose.runtime.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.renderComposable
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.attributes.*
import io.ktor.client.*
import io.ktor.client.engine.js.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.client.request.*
import io.ktor.client.call.*
import io.ktor.http.*
import com.example.rhnaf.shared.model.*
import kotlinx.coroutines.launch
import kotlinx.browser.window
import kotlinx.browser.document

// DISEÑO NAF CONNECT - IDENTIDAD INDUSTRIAL MODERNA
val SidebarColor = Color("#0f172a") 
val SidebarActiveColor = Color("#2563eb")
val BackgroundColor = Color("#f1f5f9")
val CardShadow = "0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06)"

enum class Language { ES, EN, ZH }

class Translations(val lang: Language) {
    private val es = mapOf(
        "dashboard" to "Panel de Control",
        "employees" to "Plantilla Personal",
        "recruitment" to "Reclutamiento",
        "attendance" to "Asistencia Facial",
        "payroll" to "Nómina y Pagos",
        "training" to "Capacitación",
        "performance" to "Desempeño",
        "incidents" to "Seguridad (EHS)",
        "vacations" to "Vacaciones",
        "documents" to "Expedientes",
        "reports" to "Reportes",
        "settings" to "Configuración",
        "welcome" to "Bienvenido",
        "login" to "INICIAR SESIÓN",
        "email" to "CORREO ELECTRÓNICO",
        "password" to "CONTRASEÑA",
        "remember_me" to "Recordar usuario",
        "total_emp" to "Total Empleados",
        "active_emp" to "Empleados Activos",
        "vacancies" to "Vacantes Abiertas",
        "training_pending" to "Capacitación Pendiente",
        "incidents_today" to "Incidencias Hoy",
        "export_csv" to "Exportar CSV",
        "new_emp" to "+ Nuevo Empleado",
        "edit" to "Editar",
        "delete" to "Eliminar",
        "save" to "Guardar Cambios",
        "cancel" to "Cancelar",
        "back" to "Regresar",
        "profile_settings" to "Configuración de Perfil",
        "select_avatar" to "Seleccionar Avatar Profesional",
        "display_name" to "Nombre a mostrar",
        "select_lang" to "Idioma del Sistema",
        "ai_analysis" to "Análisis Inteligente",
        "turnover_risk" to "Riesgo de Rotación (IA)",
        "skill_heatmap" to "Mapa de Habilidades",
        "predictive" to "Predictivo",
        "talent_market" to "Mercado de Talento",
        "esg_metrics" to "Sostenibilidad (ESG)",
        "pulse" to "Clima Laboral (Pulse)",
        "ai_assistant" to "Asistente NAF AI",
        "assets" to "Gestión de Activos (EPP)",
        "shifts" to "Turnos y Horarios",
        "benefits" to "Compensación y Beneficios",
        "workflows" to "Flujos de Aprobación",
        "warehouse" to "Almacén e Inventarios",
        "import_export" to "Importación y Exportación",
        "stock" to "Stock Actual",
        "suppliers" to "Proveedores",
        "customs" to "Aduanas y Logística",
        "safety_audits" to "Auditorías EHS",
        "incidents_log" to "Bitácora de Incidentes",
        "patrimonial" to "Seguridad Patrimonial",
        "cctv" to "Monitoreo CCTV",
        "guard_tours" to "Rondas de Guardia",
        "maintenance" to "Mantenimiento (CMMS)",
        "employee_portal" to "Portal del Empleado",
        "finance" to "Finanzas y Facturación",
        "energy" to "Gestión Energética",
        "machine_status" to "Estado de Maquinaria",
        "billing" to "Facturación Industrial",
        "self_service" to "Autoservicio"
    )
    private val en = mapOf(
        "dashboard" to "Dashboard",
        "employees" to "Staff Directory",
        "recruitment" to "Recruitment",
        "attendance" to "Facial Attendance",
        "payroll" to "Payroll",
        "training" to "Training",
        "performance" to "Performance",
        "incidents" to "Safety (EHS)",
        "vacations" to "Vacations",
        "documents" to "Documents",
        "reports" to "Reports",
        "settings" to "Settings",
        "welcome" to "Welcome",
        "login" to "LOG IN",
        "email" to "EMAIL ADDRESS",
        "password" to "PASSWORD",
        "remember_me" to "Remember me",
        "total_emp" to "Total Employees",
        "active_emp" to "Active Employees",
        "vacancies" to "Open Vacancies",
        "training_pending" to "Pending Training",
        "incidents_today" to "Incidents Today",
        "export_csv" to "Export CSV",
        "new_emp" to "+ New Employee",
        "edit" to "Edit",
        "delete" to "Delete",
        "save" to "Save Changes",
        "cancel" to "Cancel",
        "back" to "Go Back",
        "profile_settings" to "Profile Settings",
        "select_avatar" to "Select Professional Avatar",
        "display_name" to "Display Name",
        "select_lang" to "System Language",
        "real_time" to "Real-time monitoring of Hikonect terminals.",
        "verified" to "Face Verified",
        "import_data" to "Import Data (CSV/Excel)",
        "scan_doc" to "AI Scan",
        "processing" to "Processing with AI...",
        "ai_analysis" to "AI Analysis",
        "turnover_risk" to "Attrition Risk (AI)",
        "skill_heatmap" to "Skill Heatmap",
        "predictive" to "Predictive",
        "talent_market" to "Talent Marketplace",
        "esg_metrics" to "Sustainability (ESG)",
        "pulse" to "Employee Pulse",
        "ai_assistant" to "NAF AI Assistant",
        "assets" to "Asset Mgmt (PPE)",
        "shifts" to "Shifts & Scheduling",
        "benefits" to "Benefits & Compensation",
        "workflows" to "Approval Workflows",
        "warehouse" to "Warehouse & Inventory",
        "import_export" to "Import & Export",
        "stock" to "Current Stock",
        "suppliers" to "Suppliers",
        "customs" to "Customs & Logistics",
        "safety_audits" to "EHS Audits",
        "incidents_log" to "Incident Log",
        "patrimonial" to "Asset Protection",
        "cctv" to "CCTV Monitoring",
        "guard_tours" to "Guard Patrols",
        "maintenance" to "Maintenance (CMMS)",
        "employee_portal" to "Employee Portal",
        "finance" to "Finance & Billing",
        "energy" to "Energy Management",
        "machine_status" to "Machine Status",
        "billing" to "Industrial Billing",
        "self_service" to "Self-Service"
    )
    private val zh = mapOf(
        "dashboard" to "仪表板",
        "employees" to "员工名册",
        "recruitment" to "招聘管理",
        "attendance" to "人脸考勤",
        "payroll" to "薪资管理",
        "training" to "培训中心",
        "performance" to "绩效评估",
        "incidents" to "安全 (EHS)",
        "vacations" to "假期管理",
        "documents" to "文档中心",
        "reports" to "报告统计",
        "settings" to "系统设置",
        "welcome" to "欢迎",
        "login" to "登入",
        "email" to "电子邮件",
        "password" to "密码",
        "remember_me" to "记住我",
        "total_emp" to "总员工数",
        "active_emp" to "在职员工",
        "vacancies" to "招聘空缺",
        "training_pending" to "待完成培训",
        "incidents_today" to "今日事故",
        "export_csv" to "导出 CSV",
        "new_emp" to "+ 新增员工",
        "edit" to "编辑",
        "delete" to "删除",
        "save" to "保存更改",
        "cancel" to "取消",
        "back" to "返回",
        "profile_settings" to "个人资料设置",
        "select_avatar" to "选择专业头像",
        "display_name" to "显示名称",
        "select_lang" to "系统语言",
        "real_time" to "Hikonect 终端实时监控。",
        "verified" to "人脸验证成功",
        "import_data" to "批量导入 (CSV)",
        "scan_doc" to "AI 扫描",
        "processing" to "AI 处理中...",
        "ai_analysis" to "智能分析",
        "turnover_risk" to "人员离职风险 (AI)",
        "skill_heatmap" to "技能热图",
        "predictive" to "预测性",
        "talent_market" to "内部人才市场",
        "esg_metrics" to "可持续发展 (ESG)",
        "pulse" to "员工满意度调查",
        "ai_assistant" to "NAF AI 助手",
        "assets" to "资产管理 (PPE)",
        "shifts" to "班次和排班",
        "benefits" to "福利与薪酬",
        "workflows" to "审批流",
        "warehouse" to "仓库与库存",
        "import_export" to "进出口管理",
        "stock" to "当前库存",
        "suppliers" to "供应商",
        "customs" to "海关与物流",
        "safety_audits" to "安全审计 (EHS)",
        "incidents_log" to "事故记录",
        "patrimonial" to "资产安保",
        "cctv" to "视频监控 (CCTV)",
        "guard_tours" to "巡更管理",
        "maintenance" to "设备维护 (CMMS)",
        "employee_portal" to "员工自助服务",
        "finance" to "财务与计费",
        "energy" to "能源管理",
        "machine_status" to "机器状态",
        "billing" to "工业计费",
        "self_service" to "自助服务"
    )

    fun get(key: String): String {
        return when(lang) {
            Language.ES -> es[key] ?: key
            Language.EN -> en[key] ?: key
            Language.ZH -> zh[key] ?: key
        }
    }
}

enum class Module {
    DASHBOARD, EMPLOYEES, RECRUITMENT, ATTENDANCE, PAYROLL, TRAINING, PERFORMANCE, INCIDENTS, VACATIONS, DOCUMENTS, REPORTS, SETTINGS,
    TALENT_MARKET, SUSTAINABILITY, PULSE_SURVEY, ASSETS, SHIFTS, BENEFITS, WORKFLOWS,
    WAREHOUSE, IMPORT_EXPORT, PATRIMONIAL_SECURITY, MAINTENANCE, EMPLOYEE_PORTAL, FINANCE, ENERGY
}

enum class UserRole { ADMIN, RH, COMPRAS, MANTENIMIENTO, SEGURIDAD, EMPLEADO }

fun isModuleVisible(module: Module, role: UserRole): Boolean {
    if (role == UserRole.ADMIN) return true
    return when(role) {
        UserRole.RH -> module in listOf(Module.DASHBOARD, Module.EMPLOYEES, Module.RECRUITMENT, Module.ATTENDANCE, Module.PAYROLL, Module.TRAINING, Module.PERFORMANCE, Module.VACATIONS, Module.DOCUMENTS, Module.REPORTS, Module.TALENT_MARKET, Module.PULSE_SURVEY, Module.BENEFITS, Module.WORKFLOWS, Module.SETTINGS)
        UserRole.COMPRAS -> module in listOf(Module.DASHBOARD, Module.WAREHOUSE, Module.IMPORT_EXPORT, Module.ASSETS, Module.FINANCE, Module.SETTINGS)
        UserRole.MANTENIMIENTO -> module in listOf(Module.DASHBOARD, Module.MAINTENANCE, Module.ENERGY, Module.ASSETS, Module.SETTINGS)
        UserRole.SEGURIDAD -> module in listOf(Module.DASHBOARD, Module.INCIDENTS, Module.PATRIMONIAL_SECURITY, Module.SETTINGS)
        UserRole.EMPLEADO -> module in listOf(Module.DASHBOARD, Module.EMPLOYEE_PORTAL, Module.SETTINGS)
        else -> false
    }
}

fun main() {
    val client = HttpClient(Js) {
        install(ContentNegotiation) { json() }
    }

    renderComposable(rootElementId = "root") {
        var isLoggedIn by remember { mutableStateOf(false) }
        var userRole by remember { mutableStateOf(UserRole.EMPLEADO) }
        var activeModule by remember { mutableStateOf(Module.DASHBOARD) }
        var selectedEmployee by remember { mutableStateOf<Employee?>(null) }
        
        var employees by remember { mutableStateOf(emptyList<Employee>()) }
        var userName by remember { mutableStateOf(window.localStorage.getItem("naf_user_name") ?: "Dario Robles") }
        var userAvatar by remember { mutableStateOf(window.localStorage.getItem("naf_user_avatar") ?: "https://api.dicebear.com/7.x/avataaars/svg?seed=Felix") }
        var currentLang by remember { mutableStateOf(Language.valueOf(window.localStorage.getItem("naf_lang") ?: "ES")) }
        val t = Translations(currentLang)

        val scope = rememberCoroutineScope()

        if (!isLoggedIn) {
            LoginScreen(t) { u, p, rememberMe ->
                scope.launch {
                    try {
                        val resp = client.post("/api/login") {
                            contentType(ContentType.Application.Json)
                            setBody(mapOf("username" to u, "password" to p))
                        }
                        if (resp.status == HttpStatusCode.OK) {
                            val (role, name) = when(u) {
                                "d.trujillo@brancoindustries.com" -> UserRole.ADMIN to "Dario Robles"
                                "arni.oziel@brancoindustries.com" -> UserRole.RH to "Arni Oziel"
                                "compras@brancoindustries.com" -> UserRole.COMPRAS to "Usuario Compras"
                                "seguridad@brancoindustries.com" -> UserRole.SEGURIDAD to "Seguridad Planta"
                                "mantenimiento@brancoindustries.com" -> UserRole.MANTENIMIENTO to "Ing. Mantenimiento"
                                else -> UserRole.EMPLEADO to "Colaborador"
                            }
                            userRole = role
                            userName = name
                            window.localStorage.setItem("naf_user_name", userName)
                            if (rememberMe) {
                                window.localStorage.setItem("naf_saved_email", u)
                            } else {
                                window.localStorage.removeItem("naf_saved_email")
                            }
                            employees = client.get("/api/employees").body()
                            isLoggedIn = true
                        }
                    } catch (e: Exception) { console.log(e) }
                }
            }
        } else {
            Div({
                style {
                    display(DisplayStyle.Flex)
                    height(100.vh)
                    fontFamily("Inter", "Segoe UI", "sans-serif")
                    backgroundColor(BackgroundColor)
                }
            }) {
                // SIDEBAR
                Sidebar(activeModule, t, userRole) { 
                    activeModule = it 
                    selectedEmployee = null
                }

                // CONTENIDO PRINCIPAL
                Div({ style { flex(1); display(DisplayStyle.Flex); flexDirection(FlexDirection.Column); overflowY("auto") } }) {
                    TopBar(userName, userRole.name, userAvatar, t)

                    Div({ style { padding(32.px) } }) {
                        when (activeModule) {
                            Module.DASHBOARD -> DashboardView(employees, t)
                            Module.EMPLOYEES -> {
                                if (selectedEmployee == null) {
                                    EmployeeListView(
                                        employees = employees, 
                                        onSelect = { selectedEmployee = it },
                                        onDelete = { id ->
                                            if (userRole == UserRole.ADMIN) {
                                                scope.launch {
                                                    client.delete("/api/employee/$id")
                                                    employees = client.get("/api/employees").body()
                                                }
                                            } else {
                                                window.alert("Acceso denegado: Solo administradores pueden eliminar.")
                                            }
                                        },
                                        userRole = userRole,
                                        t = t
                                    )
                                } else {
                                    EmployeeDigitalFile(
                                        emp = selectedEmployee!!,
                                        onBack = { 
                                            selectedEmployee = null
                                            scope.launch { employees = client.get("/api/employees").body() }
                                        },
                                        onSave = { updated ->
                                            if (userRole == UserRole.ADMIN || userRole == UserRole.RH) {
                                                scope.launch {
                                                    client.post("/api/employee/update") {
                                                        contentType(ContentType.Application.Json)
                                                        setBody(updated)
                                                    }
                                                    employees = client.get("/api/employees").body()
                                                    selectedEmployee = null
                                                }
                                            } else {
                                                window.alert("Acceso denegado: No tiene permisos de edición.")
                                            }
                                        },
                                        userRole = userRole,
                                        t = t
                                    )
                                }
                            }
                            Module.INCIDENTS -> SafetyModule(client, scope, t)
                            Module.PATRIMONIAL_SECURITY -> PatrimonialSecurityModule(t)
                            Module.ATTENDANCE -> AttendanceModule(t)
                            Module.RECRUITMENT -> RecruitmentModule(t)
                            Module.PAYROLL -> PayrollModule(t)
                            Module.TRAINING -> TrainingModule(t)
                            Module.PERFORMANCE -> PerformanceModule(t)
                            Module.VACATIONS -> VacationsModule(t)
                            Module.DOCUMENTS -> DocumentsModule(t)
                            Module.REPORTS -> ReportsModule(t)
                            Module.TALENT_MARKET -> TalentMarketModule(t)
                            Module.WAREHOUSE -> WarehouseModule(t)
                            Module.IMPORT_EXPORT -> ImportExportModule(t)
                            Module.MAINTENANCE -> MaintenanceModule(t)
                            Module.EMPLOYEE_PORTAL -> EmployeePortalModule(t)
                            Module.FINANCE -> FinanceModule(t)
                            Module.ENERGY -> EnergyModule(t)
                            Module.SUSTAINABILITY -> SustainabilityModule(t)
                            Module.PULSE_SURVEY -> PulseModule(t)
                            Module.ASSETS -> AssetsModule(t)
                            Module.SHIFTS -> ShiftsModule(t)
                            Module.BENEFITS -> BenefitsModule(t)
                            Module.WORKFLOWS -> WorkflowsModule(t)
                            Module.SETTINGS -> SettingsView(userName, userAvatar, currentLang, { userName = it }, { userAvatar = it }, { 
                                currentLang = it
                                window.localStorage.setItem("naf_lang", it.name)
                            }, t)
                        }
                    }
                }
                // FLOATING AI ASSISTANT
                AiAssistantWidget(t, client, scope)
            }
        }
    }
}

@Composable
fun Sidebar(active: Module, t: Translations, role: UserRole, onSelect: (Module) -> Unit) {
    Nav({
        style {
            width(260.px)
            backgroundColor(SidebarColor)
            color(Color.white)
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Column)
        }
    }) {
        Div({ style { padding(32.px); display(DisplayStyle.Flex); alignItems(AlignItems.Center); justifyContent(JustifyContent.Center) } }) {
            // LOGO NAF CONNECT (Versión Sidebar)
            H2({ style { margin(0.px); fontSize(22.px); fontFamily("Inter", "sans-serif"); color(Color.white) } }) { 
                Span({ style { fontWeight("900"); property("font-style", "italic") } }) { Text("NAF") }
                Span({ style { fontWeight("300"); color(Color("#94a3b8")); marginLeft(6.px); property("font-style", "normal") } }) { Text("CONNECT") }
            }
        }

        Input(InputType.Text) {
            style {
                property("margin", "0 20px 24px 20px")
                padding(10.px, 14.px)
                backgroundColor(Color("#1e293b"))
                property("border", "1px solid #334155")
                borderRadius(8.px)
                color(Color.white)
                property("outline", "none")
            }
            placeholder("Buscar...")
        }

        Div({ style { flex(1); overflowY("auto"); padding(0.px, 16.px) } }) {
            if (isModuleVisible(Module.DASHBOARD, role)) SidebarLink(t.get("dashboard"), Module.DASHBOARD, active == Module.DASHBOARD, onSelect)
            if (isModuleVisible(Module.EMPLOYEES, role)) SidebarLink(t.get("employees"), Module.EMPLOYEES, active == Module.EMPLOYEES, onSelect)
            if (isModuleVisible(Module.RECRUITMENT, role)) SidebarLink(t.get("recruitment"), Module.RECRUITMENT, active == Module.RECRUITMENT, onSelect)
            if (isModuleVisible(Module.ATTENDANCE, role)) SidebarLink(t.get("attendance"), Module.ATTENDANCE, active == Module.ATTENDANCE, onSelect)
            if (isModuleVisible(Module.PAYROLL, role)) SidebarLink(t.get("payroll"), Module.PAYROLL, active == Module.PAYROLL, onSelect)
            if (isModuleVisible(Module.TRAINING, role)) SidebarLink(t.get("training"), Module.TRAINING, active == Module.TRAINING, onSelect)
            if (isModuleVisible(Module.INCIDENTS, role)) SidebarLink(t.get("incidents"), Module.INCIDENTS, active == Module.INCIDENTS, onSelect)
            if (isModuleVisible(Module.PATRIMONIAL_SECURITY, role)) SidebarLink(t.get("patrimonial"), Module.PATRIMONIAL_SECURITY, active == Module.PATRIMONIAL_SECURITY, onSelect)
            if (isModuleVisible(Module.MAINTENANCE, role)) SidebarLink(t.get("maintenance"), Module.MAINTENANCE, active == Module.MAINTENANCE, onSelect)
            if (isModuleVisible(Module.EMPLOYEE_PORTAL, role)) SidebarLink(t.get("employee_portal"), Module.EMPLOYEE_PORTAL, active == Module.EMPLOYEE_PORTAL, onSelect)
            if (isModuleVisible(Module.FINANCE, role)) SidebarLink(t.get("finance"), Module.FINANCE, active == Module.FINANCE, onSelect)
            if (isModuleVisible(Module.ENERGY, role)) SidebarLink(t.get("energy"), Module.ENERGY, active == Module.ENERGY, onSelect)
            if (isModuleVisible(Module.VACATIONS, role)) SidebarLink(t.get("vacations"), Module.VACATIONS, active == Module.VACATIONS, onSelect)
            if (isModuleVisible(Module.DOCUMENTS, role)) SidebarLink(t.get("documents"), Module.DOCUMENTS, active == Module.DOCUMENTS, onSelect)
            if (isModuleVisible(Module.ASSETS, role)) SidebarLink(t.get("assets"), Module.ASSETS, active == Module.ASSETS, onSelect)
            if (isModuleVisible(Module.SHIFTS, role)) SidebarLink(t.get("shifts"), Module.SHIFTS, active == Module.SHIFTS, onSelect)
            if (isModuleVisible(Module.BENEFITS, role)) SidebarLink(t.get("benefits"), Module.BENEFITS, active == Module.BENEFITS, onSelect)
            if (isModuleVisible(Module.TALENT_MARKET, role)) SidebarLink(t.get("talent_market"), Module.TALENT_MARKET, active == Module.TALENT_MARKET, onSelect)
            if (isModuleVisible(Module.WAREHOUSE, role)) SidebarLink(t.get("warehouse"), Module.WAREHOUSE, active == Module.WAREHOUSE, onSelect)
            if (isModuleVisible(Module.IMPORT_EXPORT, role)) SidebarLink(t.get("import_export"), Module.IMPORT_EXPORT, active == Module.IMPORT_EXPORT, onSelect)
            if (isModuleVisible(Module.SUSTAINABILITY, role)) SidebarLink(t.get("esg_metrics"), Module.SUSTAINABILITY, active == Module.SUSTAINABILITY, onSelect)
            if (isModuleVisible(Module.PULSE_SURVEY, role)) SidebarLink(t.get("pulse"), Module.PULSE_SURVEY, active == Module.PULSE_SURVEY, onSelect)
            if (isModuleVisible(Module.WORKFLOWS, role)) SidebarLink(t.get("workflows"), Module.WORKFLOWS, active == Module.WORKFLOWS, onSelect)
            SidebarLink(t.get("settings"), Module.SETTINGS, active == Module.SETTINGS, onSelect)
        }

        Div({ style { padding(24.px); property("border-top", "1px solid #1e293b") } }) {
            P({ style { fontSize(11.px); color(Color("#64728b")); textAlign("center") } }) { Text("NAF CONNECT v3.0") }
        }
    }
}

@Composable
fun SidebarLink(label: String, mod: Module, isSelected: Boolean, onSelect: (Module) -> Unit) {
    Div({
        style {
            padding(10.px, 16.px)
            marginBottom(4.px)
            borderRadius(8.px)
            cursor("pointer")
            display(DisplayStyle.Flex)
            alignItems(AlignItems.Center)
            gap(12.px)
            if (isSelected) backgroundColor(SidebarActiveColor) else backgroundColor(Color.transparent)
            property("transition", "all 0.2s")
        }
        onClick { onSelect(mod) }
    }) {
        Div({ style { width(18.px); height(18.px); backgroundColor(if (isSelected) Color.white else Color("#94a3b8")); borderRadius(4.px) } })
        Text(label)
    }
}

@Composable
fun DashboardView(employees: List<Employee>, t: Translations) {
    val totalEmployees = employees.size
    val activeEmployees = employees.count { it.status == EmployeeStatus.ACTIVE }
    val activePercent = if (totalEmployees > 0) (activeEmployees.toDouble() / totalEmployees * 100).toInt() else 0
    val highRiskCount = employees.count { it.attritionRisk > 0.7 }

    Div {
        // TOP CARDS
        Div({
            style {
                display(DisplayStyle.Grid)
                property("grid-template-columns", "repeat(auto-fit, minmax(200.px, 1fr))")
                gap(20.px)
                property("margin-bottom", "24px")
            }
        }) {
            StatCard(t.get("total_emp"), "$totalEmployees", "Base de datos NAF", Color("#6366f1"))
            StatCard(t.get("active_emp"), "$activeEmployees", "$activePercent% del total", Color("#22c55e"))
            StatCard(t.get("turnover_risk"), "$highRiskCount", "${t.get("predictive")}: Crítico", if(highRiskCount > 0) Color("#ef4444") else Color("#22c55e"))
            StatCard(t.get("training_pending"), "2", "Próximos cursos", Color("#a855f7"))
            StatCard(t.get("incidents_today"), "0", "Sin reportes críticos", Color("#22c55e"))
        }

        // MIDDLE SECTION: AI INSIGHTS + WIDGETS
        Div({
            style {
                display(DisplayStyle.Grid)
                property("grid-template-columns", "2fr 1fr 1fr")
                gap(24.px)
            }
        }) {
            // Skill Heatmap AI
            Div({
                style {
                    backgroundColor(Color.white); padding(24.px); borderRadius(12.px)
                    property("box-shadow", CardShadow)
                }
            }) {
                Div({ style { display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); property("margin-bottom", "20px") } }) {
                    H3({ style { margin(0.px); fontSize(16.px) } }) { Text(t.get("skill_heatmap") + " (AI)") }
                    Span({ style { color(Color.gray); fontSize(12.px) } }) { Text("Análisis de Talentos") }
                }
                Div({ style { height(200.px); backgroundColor(Color("#f8fafc")); borderRadius(8.px); padding(20.px); display(DisplayStyle.Flex); flexDirection(FlexDirection.Column); gap(12.px) } }) {
                    SkillBar("Operación de Planta", "92%", Color("#3b82f6"))
                    SkillBar("Seguridad EHS", "78%", Color("#ef4444"))
                    SkillBar("Mantenimiento Técnico", "45%", Color("#f59e0b"))
                    SkillBar("Liderazgo", "30%", Color("#10b981"))
                }
                P({ style { fontSize(11.px); color(Color.gray); marginTop(12.px) } }) { 
                    Text("Nota: Basado en certificaciones y desempeño histórico analizado por IA.") 
                }
            }

            // Birthdays
            Div({
                style {
                    backgroundColor(Color.white); padding(24.px); borderRadius(12.px)
                    property("box-shadow", CardShadow)
                }
            }) {
                H3({ style { property("margin", "0 0 20px 0"); fontSize(16.px) } }) { Text("Cumpleaños del Mes") }
                BirthdayItem("Carlos Rodríguez", "15 de Mayo", true)
                BirthdayItem("María González", "18 de Mayo", false)
                BirthdayItem("Juan Pérez", "22 de Mayo", false)
                BirthdayItem("Ana López", "28 de Mayo", false)
            }

            // Important Alerts (Predictive)
            Div({
                style {
                    backgroundColor(Color.white); padding(24.px); borderRadius(12.px)
                    property("box-shadow", CardShadow)
                }
            }) {
                H3({ style { property("margin", "0 0 20px 0"); fontSize(16.px) } }) { Text("Alertas Predictivas") }
                AlertItem("3 empleados con alta probabilidad de renuncia", Color("#ef4444"))
                AlertItem("Gap de habilidades detectado en Producción", Color("#f97316"))
                AlertItem("Certificación EHS vence en 3 días (5 pers.)", Color("#3b82f6"))
                AlertItem("Anomalía en registros de asistencia", Color("#f59e0b"))
            }
        }
    }
}

@Composable
fun SkillBar(label: String, prog: String, color: CSSColorValue) {
    Div {
        Div({ style { display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); marginBottom(4.px) } }) {
            Span({ style { fontSize(12.px); fontWeight("bold") } }) { Text(label) }
            Span({ style { fontSize(11.px); color(Color.gray) } }) { Text(prog) }
        }
        Div({ style { height(6.px); width(100.percent); backgroundColor(Color("#e2e8f0")); borderRadius(3.px) } }) {
            Div({ style { height(100.percent); property("width", prog); backgroundColor(color); borderRadius(3.px) } })
        }
    }
}

@Composable
fun StatCard(label: String, value: String, sub: String, color: CSSColorValue) {
    Div({
        style {
            backgroundColor(Color.white); padding(20.px); borderRadius(12.px)
            property("box-shadow", CardShadow)
            display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); alignItems(AlignItems.Center)
        }
    }) {
        Div {
            P({ style { margin(0.px); color(Color("#64728b")); fontSize(14.px) } }) { Text(label) }
            H2({ style { property("margin", "4px 0"); fontSize(24.px); fontWeight("bold") } }) { Text(value) }
            P({ style { margin(0.px); color(color); fontSize(12.px); fontWeight("500") } }) { Text(sub) }
        }
        Div({ style { width(48.px); height(48.px); backgroundColor(Color("#f1f5f9")); borderRadius(12.px) } })
    }
}

@Composable
fun MiniStat(label: String, value: String, sub: String, color: CSSColorValue) {
    Div {
        P({ style { margin(0.px); color(Color("#64728b")); fontSize(11.px) } }) { Text(label) }
        P({ style { property("margin", "2px 0"); fontSize(16.px); fontWeight("bold") } }) { Text(value) }
        P({ style { margin(0.px); color(color); fontSize(10.px) } }) { Text(sub) }
    }
}

@Composable
fun BirthdayItem(name: String, date: String, isToday: Boolean) {
    Div({
        style {
            display(DisplayStyle.Flex); alignItems(AlignItems.Center); gap(12.px); marginBottom(16.px)
        }
    }) {
        Div({ style { width(36.px); height(36.px); backgroundColor(Color("#e2e8f0")); borderRadius(50.percent) } })
        Div({ style { flex(1) } }) {
            P({ style { margin(0.px); fontSize(14.px); fontWeight("500") } }) { Text(name) }
            P({ style { margin(0.px); fontSize(12.px); color(Color("#64728b")) } }) { Text(date) }
        }
        if (isToday) {
            Span({ style { fontSize(10.px); backgroundColor(Color("#dbeafe")); color(Color("#1e40af")); padding(2.px, 6.px); borderRadius(4.px); fontWeight("bold") } }) { Text("Hoy") }
        }
    }
}

@Composable
fun AlertItem(text: String, color: CSSColorValue) {
    Div({
        style {
            display(DisplayStyle.Flex); gap(12.px); marginBottom(16.px); alignItems(AlignItems.FlexStart)
        }
    }) {
        Div({ style { width(20.px); height(20.px); borderRadius(50.percent); property("border", "2px solid $color"); property("flex-shrink", "0") } })
        Div {
            P({ style { margin(0.px); fontSize(13.px); fontWeight("500") } }) { Text(text) }
            A(href = "#", { style { fontSize(11.px); color(SidebarActiveColor); textDecoration("none") } }) { Text("Ver detalles") }
        }
    }
}

@Composable
fun TopBar(user: String, role: String, avatarUrl: String, t: Translations) {
    Header({
        style {
            backgroundColor(Color.white); padding(12.px, 24.px); display(DisplayStyle.Flex)
            justifyContent(JustifyContent.SpaceBetween); alignItems(AlignItems.Center)
            property("border-bottom", "1px solid #e2e8f0")
        }
    }) {
        Div {
            H2({ style { margin(0.px); fontSize(16.px); fontWeight("bold") } }) { Text("Panel NAF CONNECT") }
            P({ style { margin(0.px); fontSize(12.px); color(Color("#64728b")) } }) { Text("Gestión Industrial de Talento") }
        }
        Div({ style { display(DisplayStyle.Flex); alignItems(AlignItems.Center); gap(20.px) } }) {
            Div({ style { display(DisplayStyle.Flex); alignItems(AlignItems.Center); gap(12.px) } }) {
                Div({ style { textAlign("right") } }) {
                    P({ style { margin(0.px); fontSize(14.px); fontWeight("600") } }) { Text(user) }
                    P({ style { margin(0.px); fontSize(12.px); color(Color("#64728b")) } }) { Text(role) }
                }
                Div({ style { position(Position.Relative) } }) {
                    Img(src = avatarUrl) {
                        style { width(36.px); height(36.px); borderRadius(50.percent); property("object-fit", "cover"); backgroundColor(Color("#cbd5e1")) }
                    }
                    // Punto de estado Online
                    Div({
                        style {
                            width(10.px); height(10.px); backgroundColor(Color("#22c55e"))
                            borderRadius(50.percent); position(Position.Absolute); bottom(0.px); right(0.px)
                            property("border", "2px solid white")
                        }
                    })
                }
            }
        }
    }
}

@Composable
fun EmployeeListView(employees: List<Employee>, onSelect: (Employee) -> Unit, onDelete: (String) -> Unit, userRole: UserRole, t: Translations) {
    var isImporting by remember { mutableStateOf(false) }
    
    Div({ style { backgroundColor(Color.white); padding(24.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        Div({ style { display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); alignItems(AlignItems.Center); marginBottom(24.px) } }) {
            H3({ style { margin(0.px) } }) { Text("${t.get("employees")} NAF CONNECT") }
            Div({ style { display(DisplayStyle.Flex); gap(12.px); alignItems(AlignItems.Center) } }) {
                if (userRole == UserRole.ADMIN || userRole == UserRole.RH) {
                    // Importador de CSV
                    Input(InputType.File) {
                    id("csv-upload"); style { property("display", "none") }
                        onChange { 
                            isImporting = true
                            window.setTimeout({ isImporting = false; window.alert("Importación de personal finalizada con éxito.") }, 1500)
                        }
                    }
                    Button({
                        style { padding(8.px, 16.px); backgroundColor(Color("#475569")); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer"); fontSize(13.px) }
                        onClick { document.getElementById("csv-upload")?.let { (it as org.w3c.dom.HTMLInputElement).click() } }
                    }) { Text(if(isImporting) t.get("processing") else t.get("import_data")) }

                    Button({
                        style { padding(8.px, 16.px); backgroundColor(Color("#22c55e")); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer"); fontSize(13.px); fontWeight("bold") }
                        onClick { /* Nuevo Empleado */ }
                    }) { Text(t.get("new_emp")) }
                }
                
                Button({
                    style { padding(8.px, 16.px); backgroundColor(Color("#1e293b")); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer"); fontSize(13.px) }
                    onClick { exportToCSV(employees) }
                }) { Text(t.get("export_csv")) }
            }
        }
        Table({ style { width(100.percent); property("border-collapse", "collapse") } }) {
            Thead {
                Tr {
                    Th({ style { textAlign("left"); padding(12.px); property("border-bottom", "2px solid #f1f5f9") } }) { Text("ID") }
                    Th({ style { textAlign("left"); padding(12.px); property("border-bottom", "2px solid #f1f5f9") } }) { Text("Colaborador") }
                    Th({ style { textAlign("left"); padding(12.px); property("border-bottom", "2px solid #f1f5f9") } }) { Text("Puesto") }
                    Th({ style { textAlign("left"); padding(12.px); property("border-bottom", "2px solid #f1f5f9") } }) { Text("Acciones") }
                }
            }
            Tbody {
                employees.forEach { emp ->
                    Tr {
                        Td({ style { padding(12.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(emp.id) }
                        Td({ style { padding(12.px); property("border-bottom", "1px solid #f1f5f9") } }) {
                            Text("${emp.firstName} ${emp.lastName}")
                        }
                        Td({ style { padding(12.px); property("border-bottom", "1px solid #f1f5f9") } }) { Text(emp.position) }
                        Td({ style { padding(12.px); property("border-bottom", "1px solid #f1f5f9"); display(DisplayStyle.Flex); gap(8.px) } }) {
                            Button({
                                style { padding(6.px, 12.px); backgroundColor(SidebarActiveColor); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") }
                                onClick { onSelect(emp) }
                            }) { Text(if (userRole == UserRole.ADMIN || userRole == UserRole.RH) t.get("edit") else "Ver") }
                            if (userRole == UserRole.ADMIN) {
                                Button({
                                    style { padding(6.px, 12.px); backgroundColor(Color("#ef4444")); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") }
                                    onClick { if(window.confirm("¿Eliminar a ${emp.firstName}?")) onDelete(emp.id) }
                                }) { Text(t.get("delete")) }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun exportToCSV(employees: List<Employee>) {
    val header = "ID,Nombre,Puesto,Fecha Alta,Estado\n"
    val rows = employees.joinToString("\n") { 
        "${it.id},${it.firstName} ${it.lastName},${it.position},${it.entryDate},${it.status.name}" 
    }
    val csvContent = header + rows
    val blob = org.w3c.dom.url.URL.createObjectURL(org.w3c.files.Blob(arrayOf(csvContent), org.w3c.files.BlobPropertyBag(type = "text/csv")))
    val link = kotlinx.browser.document.createElement("a") as org.w3c.dom.HTMLAnchorElement
    link.href = blob
    link.download = "Lista_Personal_NAF_CONNECT.csv"
    link.click()
}

@Composable
fun EmployeeDigitalFile(emp: Employee, onBack: () -> Unit, onSave: (Employee) -> Unit, userRole: UserRole, t: Translations) {
    var editMode by remember { mutableStateOf(false) }
    var editedEmp by remember { mutableStateOf(emp) }
    var isScanning by remember { mutableStateOf(false) }
    
    Div {
        Div({ style { display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); marginBottom(16.px) } }) {
            Button({ onClick { onBack() }; style { cursor("pointer"); backgroundColor(Color.white); property("border", "1px solid #ccc"); padding(8.px, 16.px); borderRadius(6.px) } }) { Text("← ${t.get("back")}") }
            Div({ style { display(DisplayStyle.Flex); gap(12.px); alignItems(AlignItems.Center) } }) {
                if (userRole == UserRole.ADMIN || userRole == UserRole.RH) {
                    // Escáner IA
                    Input(InputType.File) {
                        id("doc-scan"); style { property("display", "none") }
                        onChange { 
                            isScanning = true
                            window.setTimeout({ 
                                isScanning = false
                                window.alert("IA: Se detectó identificación de ${emp.firstName}. Datos actualizados automáticamente.") 
                            }, 2000)
                        }
                    }
                    Button({
                        style { padding(8.px, 16.px); backgroundColor(Color("#6366f1")); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer"); fontSize(13.px) }
                        onClick { document.getElementById("doc-scan")?.let { (it as org.w3c.dom.HTMLInputElement).click() } }
                    }) { Text(if(isScanning) t.get("processing") else t.get("scan_doc")) }

                    if (editMode) {
                        Button({ 
                            style { padding(8.px, 20.px); backgroundColor(Color("#22c55e")); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer"); fontWeight("bold") }
                            onClick { onSave(editedEmp) }
                        }) { Text(t.get("save")) }
                        Button({ 
                            style { padding(8.px, 20.px); backgroundColor(Color("#ef4444")); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") }
                            onClick { editMode = false }
                        }) { Text(t.get("cancel")) }
                    } else {
                        Button({ 
                            style { padding(8.px, 20.px); backgroundColor(SidebarActiveColor); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") }
                            onClick { editMode = true }
                        }) { Text(t.get("edit")) }
                    }
                }
            }
        }
        
        Div({
            style {
                backgroundColor(Color.white); padding(32.px); borderRadius(16.px); property("box-shadow", CardShadow)
                display(DisplayStyle.Flex); gap(40.px)
            }
        }) {
            // Perfil
            Div({ style { width(220.px); textAlign("center") } }) {
                Div({ style { width(120.px); height(120.px); backgroundColor(Color("#e2e8f0")); borderRadius(50.percent); property("margin", "0 auto 20px") } })
                if (editMode) {
                    Input(InputType.Text) { 
                        value(editedEmp.firstName); onInput { editedEmp = editedEmp.copy(firstName = it.value) }
                        style { width(100.percent); marginBottom(8.px); padding(8.px); borderRadius(4.px); property("border", "1px solid #ccc") }
                    }
                    Input(InputType.Text) { 
                        value(editedEmp.lastName); onInput { editedEmp = editedEmp.copy(lastName = it.value) }
                        style { width(100.percent); padding(8.px); borderRadius(4.px); property("border", "1px solid #ccc") }
                    }
                } else {
                    H3({ style { margin(0.px) } }) { Text("${emp.firstName} ${emp.lastName}") }
                    P({ style { color(Color.gray); property("margin", "8px 0") } }) { Text(emp.position) }
                }
            }

            // Datos
            Div({ style { flex(1) } }) {
                H2({ style { property("border-bottom", "2px solid #3b82f6"); display(DisplayStyle.InlineBlock); paddingBottom(8.px); marginBottom(24.px) } }) { Text("Expediente Digital del Empleado") }
                
                // AI INSIGHT: Attrition Risk
                Div({ style { padding(16.px); backgroundColor(if(emp.attritionRisk > 0.7) Color("#fef2f2") else Color("#f0fdf4")); borderRadius(8.px); marginBottom(24.px); display(DisplayStyle.Flex); alignItems(AlignItems.Center); justifyContent(JustifyContent.SpaceBetween); property("border", "1px solid " + (if(emp.attritionRisk > 0.7) "#fee2e2" else "#dcfce7")) } }) {
                    Div {
                        P({ style { margin(0.px); fontWeight("bold"); fontSize(13.px); color(if(emp.attritionRisk > 0.7) Color("#991b1b") else Color("#166534")) } }) { Text(t.get("ai_analysis") + ": " + t.get("turnover_risk")) }
                        P({ style { margin(0.px); fontSize(11.px); color(Color.gray) } }) { Text("Basado en patrones de asistencia, antigüedad y evaluaciones.") }
                    }
                    Span({ style { fontSize(16.px); fontWeight("900"); color(if(emp.attritionRisk > 0.7) Color("#ef4444") else Color("#22c55e")) } }) { 
                        Text("${(emp.attritionRisk * 100).toInt()}%") 
                    }
                }

                Div({ style { display(DisplayStyle.Grid); property("grid-template-columns", "1fr 1fr"); gap(32.px) } }) {
                    Div {
                        H4({ style { color(SidebarActiveColor); marginBottom(12.px) } }) { Text("Datos Laborales") }
                        EditField("Puesto", editedEmp.position, editMode) { editedEmp = editedEmp.copy(position = it) }
                        EditField("Departamento", editedEmp.department, editMode) { editedEmp = editedEmp.copy(department = it) }
                        EditField("Fecha Ingreso", editedEmp.entryDate, editMode) { editedEmp = editedEmp.copy(entryDate = it) }
                        EditField("ID Lectora Facial", editedEmp.readerId ?: "", editMode) { editedEmp = editedEmp.copy(readerId = it) }
                    }
                    Div {
                        H4({ style { color(SidebarActiveColor); marginBottom(12.px) } }) { Text("Identidad") }
                        EditField("CURP", editedEmp.curp ?: "", editMode) { editedEmp = editedEmp.copy(curp = it) }
                        EditField("RFC", editedEmp.rfc ?: "", editMode) { editedEmp = editedEmp.copy(rfc = it) }
                        EditField("NSS", editedEmp.nss ?: "", editMode) { editedEmp = editedEmp.copy(nss = it) }
                    }
                }
            }
        }
    }
}

@Composable
fun EditField(label: String, value: String, editMode: Boolean, onUpdate: (String) -> Unit) {
    Div({ style { display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); padding(8.px, 0.px); property("border-bottom", "1px solid #f1f5f9"); alignItems(AlignItems.Center) } }) {
        Span({ style { color(Color.gray); fontSize(13.px) } }) { Text(label) }
        if (editMode) {
            Input(InputType.Text) { 
                value(value); onInput { onUpdate(it.value) }
                style { padding(4.px); borderRadius(4.px); property("border", "1px solid #ddd"); fontSize(13.px); textAlign("right") }
            }
        } else {
            Span({ style { fontWeight("600"); fontSize(13.px) } }) { Text(value.ifEmpty { "No registrado" }) }
        }
    }
}

@Composable
fun InfoSection(title: String, data: List<Pair<String, String>>) {
    Div {
        H4({ style { color(SidebarActiveColor); marginBottom(12.px) } }) { Text(title) }
        data.forEach { (k, v) ->
            Div({ style { display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); padding(8.px, 0.px); property("border-bottom", "1px solid #f1f5f9") } }) {
                Span({ style { color(Color.gray); fontSize(13.px) } }) { Text(k) }
                Span({ style { fontWeight("600"); fontSize(13.px) } }) { Text(v) }
            }
        }
    }
}

@Composable
fun StatusBadge(s: EmployeeStatus) {
    val (bg, txt) = when(s) {
        EmployeeStatus.ACTIVE -> Color("#dcfce7") to Color("#166534")
        EmployeeStatus.VACATION -> Color("#fef9c3") to Color("#854d0e")
        else -> Color("#fee2e2") to Color("#991b1b")
    }
    Span({
        style {
            padding(4.px, 12.px); borderRadius(20.px); color(txt); backgroundColor(bg); fontSize(11.px); fontWeight("bold")
        }
    }) { Text(s.name) }
}

@Composable
fun LoginScreen(t: Translations, onLogin: (String, String, Boolean) -> Unit) {
    var u by remember { mutableStateOf(window.localStorage.getItem("naf_saved_email") ?: "") }
    var p by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(u.isNotEmpty()) }

    Div({ style { display(DisplayStyle.Flex); alignItems(AlignItems.Center); justifyContent(JustifyContent.Center); height(100.vh); backgroundColor(SidebarColor) } }) {
        Div({ style { backgroundColor(Color.white); padding(48.px); borderRadius(24.px); width(400.px); property("box-shadow", "0 25px 50px -12px rgba(0, 0, 0, 0.5)") } }) {
            Div({ style { textAlign("center"); marginBottom(40.px) } }) {
                // LOGO NAF CONNECT (Réplica exacta del logo proporcionado)
                H1({ style { margin(0.px); fontSize(42.px); fontFamily("Inter", "sans-serif"); letterSpacing((-1).px) } }) { 
                    Span({ style { fontWeight("900"); property("font-style", "italic"); color(Color("#0f172a")) } }) { Text("NAF") }
                    Span({ style { fontWeight("300"); color(Color("#475569")); marginLeft(8.px); property("font-style", "normal") } }) { Text("CONNECT") }
                }
                P({ style { color(Color("#64728b")); marginTop(4.px); fontSize(14.px); letterSpacing(2.px); fontWeight("500") } }) { Text("PORTAL DE GESTIÓN") }
            }
            
            Label(attrs = { style { fontSize(12.px); fontWeight("600"); color(Color("#475569")); letterSpacing(0.5.px) } }) { Text(t.get("email")) }
            Input(InputType.Text) { 
                value(u)
                placeholder("usuario@dominio.com")
                style { width(100.percent); padding(12.px); property("margin", "8px 0 20px 0"); borderRadius(8.px); property("border", "1px solid #e2e8f0"); property("box-sizing", "border-box"); property("outline", "none"); backgroundColor(Color("#f8fafc")) }
                onInput { u = it.value } 
            }
            
            Label(attrs = { style { fontSize(12.px); fontWeight("600"); color(Color("#475569")); letterSpacing(0.5.px) } }) { Text(t.get("password")) }
            Input(InputType.Password) { 
                value(p)
                placeholder("••••••••")
                style { width(100.percent); padding(12.px); property("margin", "8px 0 20px 0"); borderRadius(8.px); property("border", "1px solid #e2e8f0"); property("box-sizing", "border-box"); property("outline", "none"); backgroundColor(Color("#f8fafc")) }
                onInput { p = it.value } 
            }

            Div({ style { display(DisplayStyle.Flex); alignItems(AlignItems.Center); gap(8.px); marginBottom(20.px) } }) {
                Input(InputType.Checkbox) {
                    checked(rememberMe)
                    onInput { rememberMe = it.value }
                    style { cursor("pointer") }
                }
                Label(attrs = {
                    style { fontSize(13.px); color(Color("#64728b")); cursor("pointer") }
                }) {
                    Text(t.get("remember_me"))
                }
            }
            
            Button({ 
                style { 
                    width(100.percent); padding(16.px); backgroundColor(Color("#0f172a")); color(Color.white); 
                    property("border", "none"); borderRadius(8.px); cursor("pointer"); 
                    fontWeight("bold"); fontSize(14.px); property("transition", "all 0.2s") 
                }
                onClick { onLogin(u, p, rememberMe) } 
            }) { Text(t.get("login")) }
            
            P({ style { textAlign("center"); marginTop(32.px); fontSize(11.px); color(Color("#94a3b8")) } }) { Text("© 2024 NAF CONNECT • SISTEMA INDUSTRIAL") }
        }
    }
}

@Composable
fun SafetyModule(client: HttpClient, scope: kotlinx.coroutines.CoroutineScope, t: Translations) {
    var desc by remember { mutableStateOf("") }
    var res by remember { mutableStateOf("") }
    Div({ style { backgroundColor(Color.white); padding(32.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        H3 { Text(t.get("incidents")) }
        P({ style { color(Color.gray) } }) { Text("Investigación de Incidentes EHS con Inteligencia Artificial") }
        
        Div({ style { display(DisplayStyle.Grid); property("grid-template-columns", "1fr 1fr"); gap(24.px); marginBottom(32.px) } }) {
            Div({ style { padding(20.px); backgroundColor(Color("#fff1f2")); borderRadius(12.px); property("border-left", "4px solid #ef4444") } }) {
                H4 { Text("Días sin accidentes") }
                H1({ style { margin(0.px); color(Color("#991b1b")) } }) { Text("342") }
                P({ style { fontSize(12.px); color(Color.gray) } }) { Text("Récord histórico: 500 días") }
            }
            Div({ style { padding(20.px); backgroundColor(Color("#f0fdf4")); borderRadius(12.px); property("border-left", "4px solid #22c55e") } }) {
                H4 { Text("Inspecciones de Seguridad") }
                P { Text("● Planta: 100% Completado") }
                P { Text("● Almacén: Pendiente hoy") }
            }
        }

        H4 { Text("Reportar Incidente / Análisis Predictivo") }
        TextArea(value = desc) {
            style { 
                width(100.percent); height(100.px); property("margin", "10px 0"); padding(12.px); borderRadius(8.px); property("border", "1px solid #e2e8f0"); property("box-sizing", "border-box") 
            }
            onInput { desc = it.value }
        }
        
        Button({ 
            style { padding(12.px, 24.px); backgroundColor(SidebarColor); color(Color.white); property("border", "none"); borderRadius(8.px); cursor("pointer"); fontWeight("bold") }
            onClick {
                scope.launch {
                    val resp = client.post("/api/safety/analyze") {
                        contentType(ContentType.Application.Json)
                        setBody(mapOf("description" to desc))
                    }
                    val body: Map<String, String> = resp.body()
                    res = body["analysis"] ?: ""
                }
            }
        }) { Text(t.get("ai_analysis")) }

        if (res.isNotEmpty()) {
            Div({ style { property("margin-top", "32px"); padding(20.px); backgroundColor(Color("#f0f9ff")); property("border-left", "4px solid $SidebarActiveColor"); borderRadius(4.px) } }) { 
                H4({ style { property("margin", "0 0 10px 0") } }) { Text("Análisis de Riesgo:") }
                Text(res) 
            }
        }

        Div({ style { marginTop(40.px) } }) {
            H4 { Text(t.get("safety_audits")) }
        Div({ style { display(DisplayStyle.Flex); gap(12.px); marginBottom(16.px) } }) {
            Input(InputType.File) {
                id("ehs-audit-upload"); style { display(DisplayStyle.None) }
                accept("application/pdf")
                onChange { window.alert("Auditoría PDF cargada exitosamente.") }
            }
            Button({
                style { padding(8.px, 16.px); backgroundColor(SidebarActiveColor); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer"); fontSize(13.px) }
                onClick { document.getElementById("ehs-audit-upload")?.let { (it as org.w3c.dom.HTMLInputElement).click() } }
            }) { Text("↑ Subir Auditoría (PDF)") }
        }
            Table({ style { width(100.percent) } }) {
                Thead { Tr { Th { Text("Fecha") }; Th { Text("Área") }; Th { Text("Inspector") }; Th { Text("Resultado") } } }
                Tbody {
                    listOf("2024-06-30" to "Producción B", "2024-06-28" to "Aduanas").forEach { (date, area) ->
                        Tr { Td { Text(date) }; Td { Text(area) }; Td { Text("Ing. Martínez") }; Td { Span({ style { color(Color("#166534")); fontWeight("bold") } }) { Text("APROBADO") } } }
                    }
                }
            }
        }
    }
}

@Composable
fun PatrimonialSecurityModule(t: Translations) {
    Div({ style { backgroundColor(Color.white); padding(32.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        H3 { Text(t.get("patrimonial")) }
        P({ style { color(Color.gray); marginBottom(24.px) } }) { Text("Protección de activos, vigilancia y control de accesos perimetrales.") }
        
        Div({ style { display(DisplayStyle.Grid); property("grid-template-columns", "2fr 1fr"); gap(24.px) } }) {
            // CCTV / Monitor
            Div {
                H4 { Text(t.get("cctv") + " (En vivo)") }
                Div({ style { height(300.px); backgroundColor(Color("#000")); borderRadius(8.px); display(DisplayStyle.Flex); alignItems(AlignItems.Center); justifyContent(JustifyContent.Center); color(Color.white); flexDirection(FlexDirection.Column) } }) {
                    Text("📡 SEÑAL ENCRIPTADA")
                    P({ style { fontSize(11.px); color(Color("#22c55e")) } }) { Text("● Cámara 01 - Acceso Principal") }
                    P({ style { fontSize(11.px); color(Color("#22c55e")) } }) { Text("● Cámara 05 - Almacén Export") }
                }
            }
            
            // Guard Patrol
            Div({ style { padding(20.px); backgroundColor(Color("#f8fafc")); borderRadius(8.px) } }) {
                H4 { Text(t.get("guard_tours")) }
                listOf("Ronda Perímetro Norte" to "100%", "Revisión Extintores" to "45%", "Cierre de Puertas" to "0%").forEach { (tour, progress) ->
                    Div({ style { marginBottom(16.px) } }) {
                        Div({ style { display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween) } }) {
                            Span({ style { fontSize(12.px) } }) { Text(tour) }
                            Span({ style { fontSize(12.px); fontWeight("bold") } }) { Text(progress) }
                        }
                        Div({ style { height(4.px); backgroundColor(Color("#e2e8f0")); borderRadius(2.px); marginTop(4.px) } }) {
                            Div({ style { height(100.percent); property("width", progress); backgroundColor(SidebarActiveColor) } })
                        }
                    }
                }
                Button({ style { width(100.percent); padding(10.px); backgroundColor(Color("#0f172a")); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") } }) {
                    Text("Reportar Incidente de Guardia")
                }
            }
        }
    }
}

@Composable
fun AttendanceModule(t: Translations) {
    Div({ style { backgroundColor(Color.white); padding(32.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        H3 { Text(t.get("attendance")) }
        P({ style { color(Color.gray) } }) { Text(t.get("real_time")) }
        
        Div({ style { display(DisplayStyle.Flex); gap(24.px); property("margin-top", "24px") } }) {
            // Estado de la lectora
            Div({ style { flex(1); padding(20.px); backgroundColor(Color("#f8fafc")); borderRadius(8.px); property("border", "1px solid #e2e8f0") } }) {
                H4({ style { margin(0.px) } }) { Text("Terminal Acceso Principal") }
                P({ style { color(Color("#22c55e")); fontWeight("bold"); fontSize(14.px) } }) { Text("● RECONOCIMIENTO ACTIVO") }
                P({ style { fontSize(12.px); color(Color.gray) } }) { Text("Modelo: Hikonect Face-ID v2") }
                P({ style { fontSize(12.px); color(Color.gray) } }) { Text("Sincronización: Nube NAF") }
            }
            
            Div({ style { flex(2) } }) {
                H4 { Text("Monitor de Inteligencia (Accesos)") }
                Table({ style { width(100.percent); fontSize(13.px) } }) {
                    Tbody {
                        Tr {
                            Td { Text("08:30:12 AM") }
                            Td { B { Text("Daniel Trujillo") } }
                            Td { Span({ style { color(Color("#166534")); backgroundColor(Color("#dcfce7")); padding(2.px, 8.px); borderRadius(4.px) } }) { Text(t.get("verified")) } }
                            Td { Span({ style { color(Color.gray); fontSize(11.px) } }) { Text("Normatividad OK") } }
                        }
                        Tr {
                            Td { Text("08:32:45 AM") }
                            Td { B { Text("Arni Oziel") } }
                            Td { Span({ style { color(Color("#166534")); backgroundColor(Color("#dcfce7")); padding(2.px, 8.px); borderRadius(4.px) } }) { Text(t.get("verified")) } }
                            Td { Span({ style { color(Color("#ef4444")); fontSize(11.px); fontWeight("bold") } }) { Text("AI: Retardo Probable") } }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsView(name: String, avatar: String, lang: Language, onNameChange: (String) -> Unit, onAvatarChange: (String) -> Unit, onLangChange: (Language) -> Unit, t: Translations) {
    var tempName by remember { mutableStateOf(name) }
    var tempAvatar by remember { mutableStateOf(avatar) }
    var tempLang by remember { mutableStateOf(lang) }

    Div({ style { backgroundColor(Color.white); padding(32.px); borderRadius(16.px); property("box-shadow", CardShadow) } }) {
        H2 { Text(t.get("profile_settings")) }
        P({ style { color(Color.gray); marginBottom(24.px) } }) { Text("Personalice su identidad en el portal NAF CONNECT.") }
        
        Div({ style { display(DisplayStyle.Flex); gap(40.px); alignItems(AlignItems.Center) } }) {
            // Avatar Actual
            Div({ style { textAlign("center") } }) {
                Img(src = tempAvatar) {
                    style { width(120.px); height(120.px); borderRadius(50.percent); property("border", "4px solid $SidebarActiveColor"); marginBottom(16.px) }
                }
                P({ style { fontWeight("bold"); margin(0.px) } }) { Text(tempName) }
            }
            
            // Selector de Avatares
            Div({ style { flex(1) } }) {
                H4 { Text(t.get("select_avatar")) }
                Div({ style { display(DisplayStyle.Flex); gap(12.px); flexWrap(FlexWrap.Wrap) } }) {
                    val avatars = listOf(
                        "Felix", "Jace", "Jack", "Aneka", "Caleb", "Aiden", "Liza", "Mia", "Zoey", "Max", "Toby", "Coco"
                    ).map { "https://api.dicebear.com/7.x/avataaars/svg?seed=$it" }

                    avatars.forEach { url ->
                        Img(src = url) {
                            style { 
                                width(52.px); height(52.px); borderRadius(50.percent); cursor("pointer")
                                property("border", if (url == tempAvatar) "3px solid $SidebarActiveColor" else "1px solid #ddd")
                                property("transition", "all 0.2s")
                                if (url == tempAvatar) property("transform", "scale(1.1)")
                            }
                            onClick { tempAvatar = url }
                        }
                    }
                }

                H4({ style { marginTop(24.px) } }) { Text(t.get("select_lang")) }
                Div({ style { display(DisplayStyle.Flex); gap(12.px) } }) {
                    Language.entries.forEach { l ->
                        Button({
                            style {
                                padding(8.px, 16.px); borderRadius(6.px); cursor("pointer")
                                backgroundColor(if (l == tempLang) SidebarActiveColor else Color.white)
                                color(if (l == tempLang) Color.white else Color.black)
                                property("border", "1px solid #ddd")
                            }
                            onClick { tempLang = l }
                        }) { Text(l.name) }
                    }
                }
                
                H4({ style { marginTop(24.px) } }) { Text(t.get("display_name")) }
                Input(InputType.Text) {
                    value(tempName)
                    onInput { tempName = it.value }
                    style { width(100.percent); padding(12.px); borderRadius(8.px); property("border", "1px solid #e2e8f0"); marginBottom(24.px) }
                }

                Button({
                    style {
                        padding(12.px, 32.px); backgroundColor(Color("#22c55e")); color(Color.white)
                        property("border", "none"); borderRadius(8.px); cursor("pointer")
                        fontWeight("bold"); fontSize(14.px); width(100.percent)
                    }
                    onClick {
                        onNameChange(tempName)
                        onAvatarChange(tempAvatar)
                        onLangChange(tempLang)
                        window.localStorage.setItem("naf_user_name", tempName)
                        window.localStorage.setItem("naf_user_avatar", tempAvatar)
                        window.localStorage.setItem("naf_lang", tempLang.name)
                        window.alert("Configuración guardada exitosamente.")
                    }
                }) { Text(t.get("save")) }
            }
        }
    }
}

@Composable
fun RecruitmentModule(t: Translations) {
    var showForm by remember { mutableStateOf(false) }
    Div({ style { backgroundColor(Color.white); padding(32.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        Div({ style { display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); alignItems(AlignItems.Center); marginBottom(24.px) } }) {
            H3({ style { margin(0.px) } }) { Text(t.get("recruitment")) }
            Button({ 
                style { padding(10.px, 20.px); backgroundColor(SidebarActiveColor); color(Color.white); property("border", "none"); borderRadius(8.px); cursor("pointer"); fontWeight("bold") }
                onClick { showForm = !showForm }
            }) { Text(if (showForm) t.get("cancel") else "+ " + t.get("new_emp")) }
        }

        if (showForm) {
            Div({ style { padding(20.px); backgroundColor(Color("#f8fafc")); borderRadius(12.px); marginBottom(24.px); display(DisplayStyle.Flex); gap(16.px); flexWrap(FlexWrap.Wrap) } }) {
                Input(InputType.Text) { placeholder("Puesto"); style { padding(10.px); borderRadius(6.px); property("border", "1px solid #ddd") } }
                Input(InputType.Text) { placeholder("Candidato"); style { padding(10.px); borderRadius(6.px); property("border", "1px solid #ddd") } }
                Button({ style { padding(10.px, 20.px); backgroundColor(Color("#22c55e")); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") } }) { Text("Registrar Candidato") }
            }
        }
        
        Div({ style { display(DisplayStyle.Grid); property("grid-template-columns", "1fr 1fr"); gap(24.px) } }) {
            // Vacantes
            Div({ style { padding(20.px); property("border", "1px solid #e2e8f0"); borderRadius(8.px) } }) {
                H4 { Text("Vacantes Activas") }
                listOf("Ingeniero de Procesos", "Operador de Montacargas", "Técnico Eléctrico").forEach { job ->
                    Div({ style { padding(12.px, 0.px); property("border-bottom", "1px solid #f1f5f9"); display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween) } }) {
                        Text(job)
                        Span({ style { color(SidebarActiveColor); fontWeight("bold"); fontSize(12.px) } }) { Text("4 Candidatos") }
                    }
                }
            }
            // Candidatos Recientes
            Div({ style { padding(20.px); property("border", "1px solid #e2e8f0"); borderRadius(8.px) } }) {
                H4 { Text("Últimos Candidatos") }
                listOf("Roberto Sosa", "Elena Peña", "Miguel Rivas").forEach { name ->
                    Div({ style { padding(12.px, 0.px); property("border-bottom", "1px solid #f1f5f9") } }) {
                        Text(name)
                        P({ style { fontSize(11.px); color(Color.gray); margin(0.px) } }) { Text("Entrevista pendiente") }
                    }
                }
            }
        }
    }
}

@Composable
fun PayrollModule(t: Translations) {
    Div({ style { backgroundColor(Color.white); padding(32.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        H3 { Text(t.get("payroll")) }
        Div({ style { display(DisplayStyle.Flex); gap(24.px); marginBottom(32.px) } }) {
            StatCard("Próxima Dispersión", "15 Jul", "Nómina Quincenal", SidebarActiveColor)
            StatCard("Total a Pagar", "$458,200", "Estimado actual", Color("#10b981"))
        }
        Table({ style { width(100.percent) } }) {
            Thead { Tr { Th { Text("Periodo") }; Th { Text("Monto Total") }; Th { Text("Estado") }; Th { Text("Recibos") } } }
            Tbody {
                listOf("Junio Q2", "Junio Q1", "Mayo Q2").forEach { period ->
                    Tr {
                        Td { Text(period) }
                        Td { Text("$442,150.00") }
                        Td { Span({ style { color(Color("#166534")); backgroundColor(Color("#dcfce7")); padding(2.px, 8.px); borderRadius(4.px); fontSize(11.px) } }) { Text("Pagado") } }
                        Td { Button({ style { property("border", "none"); background("none"); color(SidebarActiveColor); cursor("pointer") } }) { Text("Descargar ZIP") } }
                    }
                }
            }
        }
    }
}

@Composable
fun TrainingModule(t: Translations) {
    var showAddForm by remember { mutableStateOf(false) }
    Div({ style { backgroundColor(Color.white); padding(32.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        Div({ style { display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); alignItems(AlignItems.Center); marginBottom(24.px) } }) {
            H3({ style { margin(0.px) } }) { Text(t.get("training")) }
            Div({ style { display(DisplayStyle.Flex); gap(12.px); alignItems(AlignItems.Center) } }) {
                Input(InputType.File) {
                    id("training-excel-upload"); style { display(DisplayStyle.None) }
                    accept(".xlsx, .xls, .csv")
                    onChange { window.alert("Historial de capacitaciones (Excel) importado exitosamente.") }
                }
                Button({
                    style { padding(10.px, 20.px); backgroundColor(Color("#166534")); color(Color.white); property("border", "none"); borderRadius(8.px); cursor("pointer"); fontSize(13.px) }
                    onClick { document.getElementById("training-excel-upload")?.let { (it as org.w3c.dom.HTMLInputElement).click() } }
                }) { Text("Excel Import") }

                Button({ 
                    style { padding(10.px, 20.px); backgroundColor(SidebarActiveColor); color(Color.white); property("border", "none"); borderRadius(8.px); cursor("pointer"); fontWeight("bold") }
                    onClick { showAddForm = !showAddForm }
                }) { Text(if (showAddForm) t.get("cancel") else "+ Registrar Curso") }
            }
        }

        if (showAddForm) {
            Div({ style { padding(24.px); backgroundColor(Color("#f8fafc")); borderRadius(12.px); marginBottom(32.px); property("border", "1px solid #e2e8f0") } }) {
                H4 { Text("Nuevo Registro de Capacitación") }
                Div({ style { display(DisplayStyle.Flex); gap(16.px); flexWrap(FlexWrap.Wrap) } }) {
                    Input(InputType.Text) { placeholder("Nombre del Curso"); style { flex(1); padding(10.px); borderRadius(6.px); property("border", "1px solid #ddd") } }
                    Input(InputType.Date) { style { padding(10.px); borderRadius(6.px); property("border", "1px solid #ddd") } }
                    Input(InputType.Text) { placeholder("Instructor"); style { padding(10.px); borderRadius(6.px); property("border", "1px solid #ddd") } }
                    Button({ style { padding(10.px, 24.px); backgroundColor(Color("#22c55e")); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer"); fontWeight("bold") } }) { Text("Guardar Registro") }
                }
            }
        }

        Div({ style { display(DisplayStyle.Grid); property("grid-template-columns", "repeat(auto-fill, minmax(280.px, 1fr))"); gap(20.px) } }) {
            listOf(
                "Seguridad Industrial (EHS)" to "85%",
                "Manejo de Sustancias" to "40%",
                "Cultura NAF Connect" to "100%",
                "Primeros Auxilios" to "15%"
            ).forEach { (course, prog) ->
                Div({ style { padding(20.px); property("border", "1px solid #e2e8f0"); borderRadius(12.px) } }) {
                    H4({ style { margin(0.px) } }) { Text(course) }
                    Div({ style { height(8.px); width(100.percent); backgroundColor(Color("#f1f5f9")); borderRadius(4.px); property("margin", "16px 0") } }) {
                        Div({ style { height(100.percent); property("width", prog); backgroundColor(SidebarActiveColor); borderRadius(4.px) } })
                    }
                    P({ style { fontSize(12.px); color(Color.gray); margin(0.px); textAlign("right") } }) { Text("Progreso: $prog") }
                }
            }
        }
    }
}

@Composable
fun PerformanceModule(t: Translations) {
    var showReviewForm by remember { mutableStateOf(false) }
    Div({ style { backgroundColor(Color.white); padding(32.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        Div({ style { display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); alignItems(AlignItems.Center); marginBottom(32.px) } }) {
            H3({ style { margin(0.px) } }) { Text(t.get("performance")) }
            Button({ 
                style { padding(10.px, 20.px); backgroundColor(SidebarActiveColor); color(Color.white); property("border", "none"); borderRadius(8.px); cursor("pointer"); fontWeight("bold") }
                onClick { showReviewForm = !showReviewForm }
            }) { Text(if (showReviewForm) t.get("cancel") else "+ Nueva Evaluación") }
        }

        if (showReviewForm) {
            Div({ style { padding(24.px); backgroundColor(Color("#f8fafc")); borderRadius(12.px); marginBottom(32.px) } }) {
                H4 { Text("Registrar Evaluación de Desempeño") }
                Div({ style { display(DisplayStyle.Flex); gap(16.px); flexDirection(FlexDirection.Column) } }) {
                    Input(InputType.Text) { placeholder("ID o Nombre del Colaborador"); style { padding(10.px); borderRadius(6.px); property("border", "1px solid #ddd") } }
                    TextArea { placeholder("Comentarios del Supervisor"); style { height(100.px); padding(10.px); borderRadius(6.px); property("border", "1px solid #ddd") } }
                    Div({ style { display(DisplayStyle.Flex); gap(16.px) } }) {
                        Input(InputType.Number) { placeholder("Calificación (1-5)"); style { padding(10.px); borderRadius(6.px); property("border", "1px solid #ddd") } }
                        Button({ style { flex(1); padding(10.px); backgroundColor(Color("#22c55e")); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") } }) { Text("Publicar Evaluación") }
                    }
                }
            }
        }

        Div({ style { display(DisplayStyle.Flex); gap(24.px); alignItems(AlignItems.Center) } }) {
            Div({ style { flex(1); padding(24.px); backgroundColor(Color("#f8fafc")); borderRadius(12.px); textAlign("center") } }) {
                H2({ style { margin(0.px); color(SidebarActiveColor) } }) { Text("4.8 / 5.0") }
                P({ style { color(Color.gray) } }) { Text("Calificación Promedio Planta") }
            }
            Div({ style { flex(2) } }) {
                Text("Ciclo de evaluación actual: Junio - Diciembre 2024")
                Div({ style { marginTop(12.px); display(DisplayStyle.Flex); gap(12.px) } }) {
                    Button({ style { padding(8.px, 16.px); backgroundColor(SidebarActiveColor); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") } }) { Text("Iniciar Evaluación 360") }
                }
            }
        }
    }
}

@Composable
fun VacationsModule(t: Translations) {
    var showReqForm by remember { mutableStateOf(false) }
    Div({ style { backgroundColor(Color.white); padding(32.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        Div({ style { display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); alignItems(AlignItems.Center); marginBottom(24.px) } }) {
            H3({ style { margin(0.px) } }) { Text(t.get("vacations")) }
            Button({ 
                style { padding(10.px, 20.px); backgroundColor(SidebarActiveColor); color(Color.white); property("border", "none"); borderRadius(8.px); cursor("pointer"); fontWeight("bold") }
                onClick { showReqForm = !showReqForm }
            }) { Text(if (showReqForm) t.get("cancel") else "+ Nueva Solicitud") }
        }

        if (showReqForm) {
            Div({ style { padding(20.px); backgroundColor(Color("#fffbeb")); borderRadius(12.px); marginBottom(24.px) } }) {
                H4 { Text("Solicitar Periodo Vacacional") }
                Div({ style { display(DisplayStyle.Flex); gap(16.px) } }) {
                    Input(InputType.Date) { style { padding(10.px); borderRadius(6.px); property("border", "1px solid #ddd") } }
                    Input(InputType.Date) { style { padding(10.px); borderRadius(6.px); property("border", "1px solid #ddd") } }
                    Button({ style { padding(10.px, 20.px); backgroundColor(Color("#f59e0b")); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") } }) { Text("Enviar Solicitud") }
                }
            }
        }

        Div({ style { display(DisplayStyle.Grid); property("grid-template-columns", "2fr 1fr"); gap(24.px) } }) {
            Div {
                H4 { Text("Solicitudes Pendientes") }
                listOf("Juan Perez (3 días)", "Ana López (5 días)").forEach { req ->
                    Div({ style { padding(16.px); backgroundColor(Color("#fffbeb")); property("border-left", "4px solid #f59e0b"); borderRadius(4.px); marginBottom(12.px); display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); alignItems(AlignItems.Center) } }) {
                        Text(req)
                        Div({ style { display(DisplayStyle.Flex); gap(8.px) } }) {
                            Button({ style { backgroundColor(Color("#22c55e")); color(Color.white); property("border", "none"); borderRadius(4.px); padding(4.px, 8.px); cursor("pointer") } }) { Text("✓") }
                            Button({ style { backgroundColor(Color("#ef4444")); color(Color.white); property("border", "none"); borderRadius(4.px); padding(4.px, 8.px); cursor("pointer") } }) { Text("✕") }
                        }
                    }
                }
            }
            Div({ style { padding(20.px); backgroundColor(Color("#f8fafc")); borderRadius(8.px) } }) {
                H4 { Text("Resumen Global") }
                P { Text("● 5 Empleados ausentes hoy") }
                P { Text("● 12 Vacaciones programadas este mes") }
            }
        }
    }
}

@Composable
fun DocumentsModule(t: Translations) {
    Div({ style { backgroundColor(Color.white); padding(32.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        Div({ style { display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); alignItems(AlignItems.Center); marginBottom(24.px) } }) {
            H3({ style { margin(0.px) } }) { Text(t.get("documents")) }
            Button({ 
                style { padding(10.px, 20.px); backgroundColor(SidebarColor); color(Color.white); property("border", "none"); borderRadius(8.px); cursor("pointer") }
            }) { Text("↑ Subir Documento") }
        }
        Div({ style { display(DisplayStyle.Grid); property("grid-template-columns", "repeat(4, 1fr)"); gap(20.px) } }) {
            listOf("Contratos", "Identificaciones", "Políticas", "Certificados").forEach { folder ->
                Div({ style { textAlign("center"); padding(24.px); property("border", "1px solid #e2e8f0"); borderRadius(12.px); cursor("pointer") } }) {
                    Div({ style { width(48.px); height(48.px); backgroundColor(Color("#cbd5e1")); borderRadius(8.px); property("margin", "0 auto 12.px") } })
                    Text(folder)
                }
            }
        }
    }
}

@Composable
fun ReportsModule(t: Translations) {
    Div({ style { backgroundColor(Color.white); padding(32.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        H3 { Text(t.get("reports")) }
        Div({ style { display(DisplayStyle.Grid); property("grid-template-columns", "1fr 1fr"); gap(24.px) } }) {
            Div({ style { padding(20.px); backgroundColor(Color("#f8fafc")); borderRadius(8.px); textAlign("center") } }) {
                H4 { Text("Generar PDF de Asistencia") }
                Button({ style { padding(10.px, 20.px); backgroundColor(SidebarActiveColor); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") } }) { Text("Descargar Reporte") }
            }
            Div({ style { padding(20.px); backgroundColor(Color("#f8fafc")); borderRadius(8.px); textAlign("center") } }) {
                H4 { Text("Reporte de Incidencias EHS") }
                Button({ style { padding(10.px, 20.px); backgroundColor(SidebarActiveColor); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") } }) { Text("Descargar Reporte") }
            }
        }
        Div({ style { marginTop(32.px); display(DisplayStyle.Grid); property("grid-template-columns", "1fr 1fr"); gap(24.px) } }) {
            Div({ style { height(200.px); backgroundColor(Color("#f1f5f9")); borderRadius(8.px); display(DisplayStyle.Flex); alignItems(AlignItems.Center); justifyContent(JustifyContent.Center) } }) {
                Text("Gráfica: Distribución por Departamentos")
            }
            Div({ style { height(200.px); backgroundColor(Color("#f1f5f9")); borderRadius(8.px); display(DisplayStyle.Flex); alignItems(AlignItems.Center); justifyContent(JustifyContent.Center) } }) {
                Text("Gráfica: Índice de Rotación Anual")
            }
        }
        Div({ style { marginTop(32.px); textAlign("center") } }) {
            Button({ style { padding(12.px, 24.px); backgroundColor(SidebarColor); color(Color.white); property("border", "none"); borderRadius(8.px); cursor("pointer") } }) { Text("Generar Reporte Anual NAF (PDF)") }
        }
    }
}

@Composable
fun TalentMarketModule(t: Translations) {
    Div({ style { backgroundColor(Color.white); padding(32.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        H3 { Text(t.get("talent_market")) }
        P({ style { color(Color.gray); marginBottom(24.px) } }) { Text("Gig economy interna: Proyectos estratégicos para colaboradores actuales.") }
        
        Div({ style { display(DisplayStyle.Grid); property("grid-template-columns", "repeat(auto-fit, minmax(300.px, 1fr))"); gap(20.px) } }) {
            listOf(
                "Optimización de Logística" to "Habilidad: Análisis de Datos",
                "Brigada de Primeros Auxilios" to "Habilidad: EHS",
                "Mentoría a Nuevos Operadores" to "Habilidad: Liderazgo"
            ).forEach { (project, skill) ->
                Div({ style { padding(20.px); property("border", "1px solid #e2e8f0"); borderRadius(12.px); backgroundColor(Color("#f8fafc")) } }) {
                    H4({ style { margin(0.px); color(SidebarActiveColor) } }) { Text(project) }
                    P({ style { fontSize(13.px); color(Color.gray) } }) { Text(skill) }
                    Button({ style { width(100.percent); padding(8.px); backgroundColor(Color("#0f172a")); color(Color.white); property("border", "none"); borderRadius(6.px); cursor("pointer") } }) { Text("Postularse") }
                }
            }
        }
    }
}

@Composable
fun SustainabilityModule(t: Translations) {
    Div({ style { backgroundColor(Color.white); padding(32.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        H3 { Text(t.get("esg_metrics") + " - Planta NAF") }
        Div({ style { display(DisplayStyle.Grid); property("grid-template-columns", "1fr 1fr 1fr"); gap(24.px); marginBottom(32.px) } }) {
            StatCard("Huella de Carbono", "12.4 tCO2e", "Bajo el promedio", Color("#10b981"))
            StatCard("Consumo Energético", "450 kWh", "Objetivo: 400", Color("#f59e0b"))
            StatCard("Reciclaje Industrial", "88%", "KPI de excelencia", Color("#3b82f6"))
        }
    }
}

@Composable
fun PulseModule(t: Translations) {
    Div({ style { backgroundColor(Color.white); padding(32.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        H3 { Text(t.get("pulse")) }
        Div({ style { display(DisplayStyle.Flex); gap(24.px); alignItems(AlignItems.Center) } }) {
            Div({ style { flex(1); padding(32.px); backgroundColor(Color("#f0f9ff")); borderRadius(50.percent); textAlign("center"); width(150.px); height(150.px); display(DisplayStyle.Flex); flexDirection(FlexDirection.Column); justifyContent(JustifyContent.Center) } }) {
                H2({ style { margin(0.px); color(Color("#0369a1")) } }) { Text("86%") }
                P({ style { fontSize(11.px); color(Color.gray) } }) { Text("Felicidad") }
            }
            Div({ style { flex(2) } }) {
                H4 { Text("Tendencias de Sentimiento") }
                P { Text("● El personal de producción se siente motivado por los nuevos bonos.") }
                P { Text("● Se detectó cansancio en el turno nocturno (Mantenimiento).") }
            }
        }
    }
}

@Composable
fun AiAssistantWidget(t: Translations, client: HttpClient, scope: kotlinx.coroutines.CoroutineScope) {
    var isOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var response by remember { mutableStateOf("") }

    Div({
        style {
            position(Position.Fixed); bottom(24.px); right(24.px); property("z-index", "1000")
        }
    }) {
        if (isOpen) {
            Div({
                style {
                    width(350.px); height(450.px); backgroundColor(Color.white); borderRadius(16.px)
                    property("box-shadow", "0 10px 25px -5px rgba(0, 0, 0, 0.2)")
                    display(DisplayStyle.Flex); flexDirection(FlexDirection.Column)
                    marginBottom(16.px); property("border", "1px solid #e2e8f0")
                }
            }) {
                Div({ style { padding(16.px); backgroundColor(Color("#0f172a")); color(Color.white); borderRadius(16.px, 16.px, 0.px, 0.px) } }) {
                    Text(t.get("ai_assistant"))
                }
                Div({ style { flex(1); padding(16.px); overflowY("auto"); fontSize(13.px) } }) {
                    if (response.isEmpty()) {
                        Text("¿En qué puedo ayudarte hoy? Ejemplo: '¿Quién tiene riesgo de renuncia?' o 'Genera reporte de capacitación'.")
                    } else {
                        Div({ style { padding(12.px); backgroundColor(Color("#f1f5f9")); borderRadius(8.px) } }) { Text(response) }
                    }
                }
                Div({ style { padding(16.px); property("border-top", "1px solid #e2e8f0") } }) {
                    Input(InputType.Text) {
                        placeholder("Pregunta a NAF AI...")
                        style { width(100.percent); padding(10.px); borderRadius(8.px); property("border", "1px solid #ddd") }
                        onInput { query = it.value }
                        onKeyDown { if (it.key == "Enter") { response = "Analizando datos de la planta..."; window.setTimeout({ response = "Basado en los datos actuales, el índice de rotación ha bajado 2% y Dario Robles tiene certificaciones al día." }, 1500) } }
                    }
                }
            }
        }
        
        Button({
            style {
                width(60.px); height(60.px); borderRadius(50.percent); backgroundColor(Color("#0f172a"))
                color(Color.white); fontSize(24.px); cursor("pointer"); property("border", "none")
                property("box-shadow", "0 4px 6px -1px rgba(0, 0, 0, 0.1)")
            }
            onClick { isOpen = !isOpen }
        }) { Text("✨") }
    }
}

@Composable
fun AssetsModule(t: Translations) {
    Div({ style { backgroundColor(Color.white); padding(32.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        H3 { Text(t.get("assets")) }
        P({ style { color(Color.gray); marginBottom(24.px) } }) { Text("Control de inventario de EPP, herramientas y equipos asignados.") }
        
        Div({ style { display(DisplayStyle.Grid); property("grid-template-columns", "repeat(auto-fit, minmax(250.px, 1fr))"); gap(20.px) } }) {
            listOf("Cascos Diieléctricos" to "45 disp.", "Botas de Seguridad" to "12 disp.", "Laptops IT" to "8 disp.").forEach { (item, qty) ->
                Div({ style { padding(20.px); property("border", "1px solid #e2e8f0"); borderRadius(12.px) } }) {
                    H4({ style { margin(0.px) } }) { Text(item) }
                    P({ style { color(SidebarActiveColor); fontWeight("bold") } }) { Text(qty) }
                    Button({ style { marginTop(12.px); width(100.percent); padding(8.px); borderRadius(6.px); property("border", "1px solid #ddd"); cursor("pointer") } }) { Text("Asignar a Colaborador") }
                }
            }
        }
    }
}

@Composable
fun ShiftsModule(t: Translations) {
    Div({ style { backgroundColor(Color.white); padding(32.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        H3 { Text(t.get("shifts")) }
        Div({ style { height(300.px); backgroundColor(Color("#f8fafc")); borderRadius(8.px); display(DisplayStyle.Flex); alignItems(AlignItems.Center); justifyContent(JustifyContent.Center); flexDirection(FlexDirection.Column) } }) {
            H4 { Text("Calendario de Rola de Turnos") }
            Text("Turno A (Matutino) | Turno B (Vespertino) | Turno C (Nocturno)")
            Div({ style { marginTop(20.px); display(DisplayStyle.Flex); gap(12.px) } }) {
                Button({ style { padding(10.px, 20.px); backgroundColor(SidebarActiveColor); color(Color.white); property("border", "none"); borderRadius(6.px) } }) { Text("Programar Rola Semanal") }
            }
        }
    }
}

@Composable
fun BenefitsModule(t: Translations) {
    Div({ style { backgroundColor(Color.white); padding(32.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        H3 { Text(t.get("benefits")) }
        Div({ style { display(DisplayStyle.Grid); property("grid-template-columns", "1fr 1fr"); gap(24.px) } }) {
            Div({ style { padding(20.px); backgroundColor(Color("#f0fdf4")); borderRadius(12.px) } }) {
                H4 { Text("Seguros y Gastos Médicos") }
                P { Text("● 95% de la plantilla con cobertura activa.") }
            }
            Div({ style { padding(20.px); backgroundColor(Color("#eff6ff")); borderRadius(12.px) } }) {
                H4 { Text("Bonos por Desempeño") }
                P { Text("● Próximo cálculo: 30 de Julio.") }
            }
        }
    }
}

@Composable
fun WorkflowsModule(t: Translations) {
    Div({ style { backgroundColor(Color.white); padding(32.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        H3 { Text(t.get("workflows")) }
        Div {
            listOf("Aumento Salarial - Op. 405" to "Pendiente Finanzas", "Cambio de Puesto - Op. 112" to "Pendiente Director").forEach { (req, status) ->
                Div({ style { padding(16.px); property("border-bottom", "1px solid #f1f5f9"); display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween) } }) {
                    Text(req)
                    Span({ style { color(Color("#f59e0b")); fontWeight("bold") } }) { Text(status) }
                }
            }
        }
    }
}

@Composable
fun WarehouseModule(t: Translations) {
    Div({ style { backgroundColor(Color.white); padding(32.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        H3 { Text(t.get("warehouse")) }
        Div({ style { display(DisplayStyle.Grid); property("grid-template-columns", "1fr 1fr 1fr"); gap(24.px); marginBottom(32.px) } }) {
            StatCard(t.get("stock"), "15,240 SKU", "98% Precisión", SidebarActiveColor)
            StatCard(t.get("suppliers"), "42 Activos", "3 en espera", Color("#10b981"))
            StatCard("Órdenes Compra", "12", "Pendientes", Color("#f59e0b"))
        }
        
        Div({ style { display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween); marginBottom(16.px) } }) {
            H4 { Text("Últimos Movimientos de Inventario") }
            Button({ style { padding(8.px, 16.px); backgroundColor(SidebarActiveColor); color(Color.white); property("border", "none"); borderRadius(6.px) } }) {
                Text("+ Registro de Entrada/Salida")
            }
        }
        
        Table({ style { width(100.percent) } }) {
            Thead { Tr { Th { Text("SKU") }; Th { Text("Producto") }; Th { Text("Cantidad") }; Th { Text("Ubicación") } } }
            Tbody {
                listOf("M-701" to "Acero Inox Grade A", "P-202" to "Válvula Industrial 4\"", "E-105" to "Cable Cobre 100m").forEach { (sku, prod) ->
                    Tr {
                        Td { Text(sku) }
                        Td { Text(prod) }
                        Td { Text("500") }
                        Td { Text("Pasillo B-12") }
                    }
                }
            }
        }
    }
}

@Composable
fun ImportExportModule(t: Translations) {
    Div({ style { backgroundColor(Color.white); padding(32.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        H3 { Text(t.get("import_export")) }
        P({ style { color(Color.gray); marginBottom(24.px) } }) { Text(t.get("customs")) }
        
        Div({ style { display(DisplayStyle.Grid); property("grid-template-columns", "2fr 1fr"); gap(24.px) } }) {
            Div {
                H4 { Text("Embarques en Tránsito") }
                listOf("Contenedor MSC-901 (China)" to "En Aduana", "Carga Aérea DHL-402 (Alemania)" to "En Ruta", "Camión Laredo-102 (USA)" to "Descargando").forEach { (ship, status) ->
                    Div({ style { padding(16.px); backgroundColor(Color("#f1f5f9")); borderRadius(8.px); marginBottom(12.px); display(DisplayStyle.Flex); justifyContent(JustifyContent.SpaceBetween) } }) {
                        Text(ship)
                        Span({ style { fontWeight("bold"); color(if(status == "En Aduana") Color("#ef4444") else SidebarActiveColor) } }) { Text(status) }
                    }
                }
            }
            Div({ style { padding(20.px); backgroundColor(Color("#f8fafc")); borderRadius(8.px) } }) {
                H4 { Text("Documentación Pendiente") }
                listOf("Certificados de Origen", "Facturas Comerciales", "Listas de Empaque").forEach { doc ->
                    Div({ style { padding(8.px, 0.px); property("border-bottom", "1px solid #e2e8f0") } }) {
                        Text("● $doc")
                    }
                }
                Button({ style { marginTop(16.px); width(100.percent); padding(8.px); backgroundColor(Color("#0f172a")); color(Color.white); property("border", "none"); borderRadius(6.px) } }) {
                    Text("Revisar Compliance")
                }
            }
        }
    }
}

@Composable
fun MaintenanceModule(t: Translations) {
    Div({ style { backgroundColor(Color.white); padding(32.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        H3 { Text(t.get("maintenance")) }
        Div({ style { display(DisplayStyle.Grid); property("grid-template-columns", "repeat(auto-fit, minmax(250.px, 1fr))"); gap(20.px); marginBottom(32.px) } }) {
            StatCard("Maquinaria Operativa", "18", "95% Disponibilidad", Color("#10b981"))
            StatCard("Mantenimientos Hoy", "3", "Preventivos", Color("#3b82f6"))
            StatCard("Alertas Críticas", "1", "Línea de Ensamble 3", Color("#ef4444"))
        }
        Table({ style { width(100.percent) } }) {
            Thead { Tr { Th { Text("Máquina") }; Th { Text("Último Mant.") }; Th { Text("Próximo Mant.") }; Th { Text("Estado") } } }
            Tbody {
                listOf("Prensa Hidráulica 05" to "2024-06-01", "Torno CNC 02" to "2024-06-15").forEach { (m, last) ->
                    Tr { Td { Text(m) }; Td { Text(last) }; Td { Text("2024-07-15") }; Td { Span({ style { color(Color("#166534")) } }) { Text("OK") } } }
                }
            }
        }
    }
}

@Composable
fun EmployeePortalModule(t: Translations) {
    Div({ style { backgroundColor(Color.white); padding(32.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        H3 { Text(t.get("employee_portal")) }
        P({ style { color(Color.gray); marginBottom(24.px) } }) { Text("Vista de autoservicio para el colaborador (Simulación App Móvil).") }
        Div({ style { display(DisplayStyle.Grid); property("grid-template-columns", "repeat(3, 1fr)"); gap(20.px) } }) {
            listOf("Mis Recibos", "Solicitar Vacaciones", "Mi Reloj Checador", "Beneficios", "Capacitación", "Mensajes").forEach { item ->
                Div({ style { textAlign("center"); padding(24.px); property("border", "1px solid #e2e8f0"); borderRadius(12.px); cursor("pointer") } }) {
                    Div({ style { width(40.px); height(40.px); backgroundColor(SidebarActiveColor); borderRadius(50.percent); property("margin", "0 auto 12.px") } })
                    Text(item)
                }
            }
        }
    }
}

@Composable
fun FinanceModule(t: Translations) {
    Div({ style { backgroundColor(Color.white); padding(32.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        H3 { Text(t.get("finance")) }
        Div({ style { display(DisplayStyle.Grid); property("grid-template-columns", "1fr 1fr"); gap(24.px); marginBottom(32.px) } }) {
            Div({ style { padding(24.px); backgroundColor(Color("#f8fafc")); borderRadius(12.px) } }) {
                H4 { Text("Estado de Resultados (Mensual)") }
                P { Text("Ingresos: $2,450,000") }
                P { Text("Gastos Operativos: $1,800,000") }
                H3({ style { color(Color("#10b981")) } }) { Text("Utilidad: $650,000") }
            }
            Div({ style { padding(24.px); backgroundColor(Color("#f8fafc")); borderRadius(12.px) } }) {
                H4 { Text("Costo de Nómina Actual") }
                H3 { Text("$458,200.00") }
                P { Text("Incluye impuestos y prestaciones.") }
            }
        }
    }
}

@Composable
fun EnergyModule(t: Translations) {
    Div({ style { backgroundColor(Color.white); padding(32.px); borderRadius(12.px); property("box-shadow", CardShadow) } }) {
        H3 { Text(t.get("energy")) }
        Div({ style { height(300.px); backgroundColor(Color("#0f172a")); borderRadius(12.px); padding(32.px); color(Color.white) } }) {
            H4 { Text("Monitor de Consumo Eléctrico Real-Time") }
            Div({ style { display(DisplayStyle.Flex); alignItems(AlignItems.Baseline); gap(12.px) } }) {
                H1({ style { fontSize(64.px); color(Color("#fbbf24")); margin(0.px) } }) { Text("42.5") }
                Span { Text("kW/h") }
            }
            P({ style { color(Color("#94a3b8")) } }) { Text("Pico máximo hoy: 58.2 kW/h a las 11:30 AM") }
            Div({ style { width(100.percent); height(100.px); backgroundColor(Color("#1e293b")); marginTop(20.px); borderRadius(8.px); display(DisplayStyle.Flex); alignItems(AlignItems.Center); justifyContent(JustifyContent.Center) } }) {
                Text("Gráfica de Ondas de Consumo")
            }
        }
    }
}

@Composable fun PlaceholderModule(name: String) {
    Div({ style { textAlign("center"); padding(100.px); backgroundColor(Color.white); borderRadius(12.px) } }) {
        H2 { Text("Módulo $name") }
        P({ style { color(Color.gray) } }) { Text("Esta sección está siendo integrada con los flujos de trabajo industriales.") }
    }
}
