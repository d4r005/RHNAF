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
import io.ktor.client.statement.*
import io.ktor.http.*
import com.example.rhnaf.shared.model.*
import com.example.rhnaf.domain.model.*
import kotlinx.coroutines.launch
import kotlinx.browser.window
import kotlinx.browser.document
import androidx.compose.runtime.mutableStateListOf

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
        "shipping" to "Embarques y Logística",
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
        "self_service" to "Autoservicio",
        "user_mgmt" to "Gestión de Usuarios",
        "controlling" to "Controlling (CO)",
        "purchasing" to "Compras (MM)",
        "production_planning" to "Producción (PP)",
        "quality_management" to "Calidad (QM)",
        "extended_warehouse" to "Almacén Avanzado (EWM)",
        "gts_trade" to "Comercio Exterior (GTS)",
        "it_security_grc" to "Seguridad SAP / GRC",
        "financial_accounting" to "Contabilidad (FI)",
        "plant_maintenance" to "Mantenimiento Planta (PM)",
        "recruitment_sap" to "Reclutamiento (HCM)"
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
        "shipping" to "Shipping & Logistics",
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
        "self_service" to "Self-Service",
        "user_mgmt" to "User Management",
        "controlling" to "Controlling (CO)",
        "purchasing" to "Purchasing (MM)",
        "production_planning" to "Production (PP)",
        "quality_management" to "Quality (QM)",
        "extended_warehouse" to "Extended Warehouse (EWM)",
        "gts_trade" to "Global Trade (GTS)",
        "it_security_grc" to "SAP Security / GRC",
        "financial_accounting" to "Accounting (FI)",
        "plant_maintenance" to "Plant Maintenance (PM)",
        "recruitment_sap" to "Recruitment (HCM)"
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
        "shipping" to "发货与物流",
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
        "self_service" to "自助服务",
        "user_mgmt" to "用户管理",
        "controlling" to "成本控制 (CO)",
        "purchasing" to "采购 (MM)",
        "production_planning" to "生产计划 (PP)",
        "quality_management" to "质量管理 (QM)",
        "extended_warehouse" to "高级仓储 (EWM)",
        "it_security_grc" to "SAP安全 / GRC",
        "financial_accounting" to "财务会计 (FI)",
        "plant_maintenance" to "工厂维护 (PM)",
        "recruitment_sap" to "招聘 (HCM)"
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
    DASHBOARD, EHS_AUDITS, GRC_SECURITY,
    CONTROLLING, PURCHASING, PRODUCTION, QUALITY, EXTENDED_WAREHOUSE, GTS_TRADE,
    FINANCIAL_ACCOUNTING, PLANT_MAINTENANCE, RECRUITMENT_SAP, EMPLOYEES, ATTENDANCE, PRE_NOMINA,
    WAREHOUSE, SHIPPING, SETTINGS, USER_MGMT
}

enum class UserRole { ADMIN, RH, COMPRAS, MANTENIMIENTO, SEGURIDAD, EMPLEADO, ALMACEN, IMPORT_EXPORT, FINANZAS }

fun isModuleVisible(module: Module, role: UserRole): Boolean {
    if (role == UserRole.ADMIN) return true
    if (role == UserRole.RH) return module in listOf(Module.DASHBOARD, Module.EMPLOYEES, Module.ATTENDANCE, Module.PRE_NOMINA, Module.SETTINGS)
    if (role == UserRole.ALMACEN) return module in listOf(Module.DASHBOARD, Module.WAREHOUSE, Module.SETTINGS)
    if (role == UserRole.IMPORT_EXPORT) return module in listOf(Module.DASHBOARD, Module.WAREHOUSE, Module.SHIPPING, Module.SETTINGS)
    if (role == UserRole.FINANZAS) return module in listOf(Module.DASHBOARD, Module.WAREHOUSE, Module.SHIPPING, Module.SETTINGS)
    return module in listOf(Module.DASHBOARD, Module.EHS_AUDITS, Module.SETTINGS)
}

// CONFIGURACIÓN DE URL DE BACKEND
// Apuntamos directamente al servidor de Hugging Face para que funcione desde nafconnect.pages.dev
val BACKEND_URL = "https://d4r005-rhnaf-industrial.hf.space"

fun main() {
    val client = HttpClient(Js) {
        install(ContentNegotiation) { json() }
    }

    renderComposable(rootElementId = "root") {
        var isLoggedIn by remember { mutableStateOf(false) }
        var userRole by remember { mutableStateOf(UserRole.EMPLEADO) }
        var activeModule by remember { mutableStateOf(Module.DASHBOARD) }
        
        var employees by remember { mutableStateOf(emptyList<Employee>()) }
        var userName by remember { mutableStateOf(window.localStorage.getItem("naf_user_name") ?: "Dario Robles") }
        var userAvatar by remember { mutableStateOf(window.localStorage.getItem("naf_user_avatar") ?: "https://api.dicebear.com/7.x/avataaars/svg?seed=Felix") }
        var currentLang by remember { mutableStateOf(Language.valueOf(window.localStorage.getItem("naf_lang") ?: "ES")) }
        val t = Translations(currentLang)

        var authToken by remember { mutableStateOf("") }
        val scope = rememberCoroutineScope()

        if (!isLoggedIn) {
            LoginScreen(t) { u, p, rememberMe ->
                scope.launch {
                    try {
                        val resp = client.post("$BACKEND_URL/api/login") {
                            contentType(ContentType.Application.Json)
                            setBody(mapOf("username" to u, "password" to p))
                        }
                        if (resp.status == HttpStatusCode.OK) {
                            // El servidor es la fuente de verdad del rol/nombre (viene de la tabla
                            // de usuarios real). Ya NO adivinamos el rol aquí con una lista fija de
                            // correos: eso causaba que cualquier cuenta nueva (Carlos, Andrea,
                            // Liliana, etc.) cayera siempre en el rol EMPLEADO por defecto.
                            val body: Map<String, String> = resp.body()
                            val roleFromServer = body["role"] ?: "EMPLEADO"
                            val name = body["name"] ?: "Colaborador"
                            val role = try { UserRole.valueOf(roleFromServer) } catch (e: Exception) { UserRole.EMPLEADO }
                            userRole = role
                            authToken = body["token"] ?: ""

                            // Siempre sobreescribimos el nombre mostrado con el que regresa el
                            // servidor para esta cuenta (evita que quede pegado el nombre de otra
                            // persona que inició sesión antes en el mismo navegador).
                            userName = name
                            window.localStorage.setItem("naf_user_name", name)

                            if (rememberMe) {
                                window.localStorage.setItem("naf_saved_email", u)
                            } else {
                                window.localStorage.removeItem("naf_saved_email")
                            }
                            employees = client.get("$BACKEND_URL/api/employees").body()
                            isLoggedIn = true
                        } else {
                            window.alert("El servidor respondió con error ${resp.status}. Es probable que el Space de Hugging Face no tenga este usuario registrado o esté corriendo la App de Inspección de Vehículos en lugar de RHNAF.")
                        }
                    } catch (e: Exception) { 
                        window.alert("Error de conexión: No se pudo alcanzar el servidor en $BACKEND_URL")
                    }
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
                }

                // CONTENIDO PRINCIPAL
                Div({ style { flex(1); display(DisplayStyle.Flex); flexDirection(FlexDirection.Column); overflowY("auto") } }) {
                    TopBar(userName, userRole.name, userAvatar, t)

                    Div({ style { padding(32.px) } }) {
                        when (activeModule) {
                            Module.DASHBOARD -> DashboardView(employees, t)
                            Module.EHS_AUDITS -> EhsAuditsModule(client, scope, t)
                            Module.GRC_SECURITY -> GrcSecurityModule(client, scope, t)
                            Module.CONTROLLING -> ControllingModule(client, scope, t)
                            Module.PURCHASING -> PurchasingModule(client, scope, t)
                            Module.PRODUCTION -> ProductionModule(client, scope, t)
                            Module.QUALITY -> QualityModule(client, scope, t)
                            Module.EXTENDED_WAREHOUSE -> ExtendedWarehouseModule(client, scope, t)
                            Module.GTS_TRADE -> GtsTradeModule(client, scope, t)
                            Module.FINANCIAL_ACCOUNTING -> FinancialAccountingModule(client, scope, t)
                            Module.PLANT_MAINTENANCE -> PlantMaintenanceModule(client, scope, t)
                            Module.RECRUITMENT_SAP -> RecruitmentSapModule(client, scope, t)
                            Module.EMPLOYEES -> EmployeeModule(employees, client, scope, t, userRole, authToken) { employees = it }
                            Module.ATTENDANCE -> AttendanceModule(client, scope, t)
                            Module.PRE_NOMINA -> PreNominaModule(client, scope, t, userRole)
                            Module.WAREHOUSE -> WarehouseModule(client, scope, t, userRole)
                            Module.SHIPPING -> ShippingModule(client, scope, t, userRole)
                            Module.USER_MGMT -> UserMgmtModule(client, scope, t, authToken)
                            Module.SETTINGS -> SettingsView(userName, userAvatar, currentLang, { userName = it }, { userAvatar = it }, { 
                                currentLang = it
                                window.localStorage.setItem("naf_lang", it.name)
                            }, t)
                        }
                    }
                }
                // FLOATING AI ASSISTANT - deshabilitado, estorbaba
                // AiAssistantWidget(t, client, scope)
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
            if (isModuleVisible(Module.EHS_AUDITS, role)) SidebarLink(t.get("safety_audits"), Module.EHS_AUDITS, active == Module.EHS_AUDITS, onSelect)
            if (isModuleVisible(Module.GRC_SECURITY, role)) SidebarLink(t.get("it_security_grc"), Module.GRC_SECURITY, active == Module.GRC_SECURITY, onSelect)
            if (isModuleVisible(Module.CONTROLLING, role)) SidebarLink(t.get("controlling"), Module.CONTROLLING, active == Module.CONTROLLING, onSelect)
            if (isModuleVisible(Module.PURCHASING, role)) SidebarLink(t.get("purchasing"), Module.PURCHASING, active == Module.PURCHASING, onSelect)
            if (isModuleVisible(Module.PRODUCTION, role)) SidebarLink(t.get("production_planning"), Module.PRODUCTION, active == Module.PRODUCTION, onSelect)
            if (isModuleVisible(Module.QUALITY, role)) SidebarLink(t.get("quality_management"), Module.QUALITY, active == Module.QUALITY, onSelect)
            if (isModuleVisible(Module.EXTENDED_WAREHOUSE, role)) SidebarLink(t.get("extended_warehouse"), Module.EXTENDED_WAREHOUSE, active == Module.EXTENDED_WAREHOUSE, onSelect)
            if (isModuleVisible(Module.WAREHOUSE, role)) SidebarLink(t.get("warehouse"), Module.WAREHOUSE, active == Module.WAREHOUSE, onSelect)
            if (isModuleVisible(Module.SHIPPING, role)) SidebarLink(t.get("shipping"), Module.SHIPPING, active == Module.SHIPPING, onSelect)
            if (isModuleVisible(Module.GTS_TRADE, role)) SidebarLink(t.get("gts_trade"), Module.GTS_TRADE, active == Module.GTS_TRADE, onSelect)
            if (isModuleVisible(Module.FINANCIAL_ACCOUNTING, role)) SidebarLink(t.get("financial_accounting"), Module.FINANCIAL_ACCOUNTING, active == Module.FINANCIAL_ACCOUNTING, onSelect)
            if (isModuleVisible(Module.PLANT_MAINTENANCE, role)) SidebarLink(t.get("plant_maintenance"), Module.PLANT_MAINTENANCE, active == Module.PLANT_MAINTENANCE, onSelect)
            if (isModuleVisible(Module.RECRUITMENT_SAP, role)) SidebarLink(t.get("recruitment_sap"), Module.RECRUITMENT_SAP, active == Module.RECRUITMENT_SAP, onSelect)
            if (isModuleVisible(Module.EMPLOYEES, role)) SidebarLink("Empleados", Module.EMPLOYEES, active == Module.EMPLOYEES, onSelect)
            if (isModuleVisible(Module.ATTENDANCE, role)) SidebarLink("Asistencia", Module.ATTENDANCE, active == Module.ATTENDANCE, onSelect)
            if (isModuleVisible(Module.PRE_NOMINA, role)) SidebarLink("Pre-Nómina", Module.PRE_NOMINA, active == Module.PRE_NOMINA, onSelect)
            if (isModuleVisible(Module.USER_MGMT, role)) SidebarLink(t.get("user_mgmt"), Module.USER_MGMT, active == Module.USER_MGMT, onSelect)
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

private fun parseCSV(text: String): List<Employee> {
    val lines = text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
    if (lines.size <= 1) return emptyList()
    
    val firstLine = lines[0]
    val delimiter = if (firstLine.contains("\t")) "\t" else if (firstLine.contains(";")) ";" else ","
    
    return lines.drop(1).mapNotNull { line ->
        try {
            val parts = line.split(delimiter)
            if (parts.size < 2) return@mapNotNull null
            
            val id = parts.getOrNull(0)?.trim() ?: ""
            val fullName = parts.getOrNull(1)?.trim() ?: ""
            
            // Intentamos separar nombres de apellidos (Aproximación simple)
            val nameParts = fullName.split(" ")
            val fName = if (nameParts.size >= 3) nameParts.drop(2).joinToString(" ") else nameParts.lastOrNull() ?: ""
            val lName = if (nameParts.size >= 3) nameParts.take(2).joinToString(" ") else nameParts.dropLast(1).joinToString(" ")
            
            Employee(
                id = id,
                firstName = fName.ifEmpty { fullName },
                lastName = lName,
                rfc = parts.getOrNull(2)?.trim(),
                curp = parts.getOrNull(3)?.trim(),
                nss = parts.getOrNull(4)?.trim(),
                position = parts.getOrNull(6)?.trim() ?: "Operativo",
                department = "Producción", 
                entryDate = parts.getOrNull(8)?.trim() ?: parts.getOrNull(7)?.trim() ?: "2024-01-01",
                status = EmployeeStatus.ACTIVE,
                readerId = id
            )
        } catch (e: Exception) {
            null
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
