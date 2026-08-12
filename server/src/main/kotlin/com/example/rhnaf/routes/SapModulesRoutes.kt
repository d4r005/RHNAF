package com.example.rhnaf.routes

import com.example.rhnaf.database.DatabaseFactory
import com.example.rhnaf.database.JournalEntryTable
import com.example.rhnaf.database.CostCenterTable
import com.example.rhnaf.database.PurchaseOrderTable
import com.example.rhnaf.database.ProductionOrderTable
import com.example.rhnaf.database.QualityInspectionTable
import com.example.rhnaf.database.MaintenanceOrderTable
import com.example.rhnaf.database.WarehouseTaskTable
import com.example.rhnaf.database.RecruitmentVacancyTable
import com.example.rhnaf.database.CustomsDeclarationTable
import com.example.rhnaf.database.SafetyInspectionTable
import com.example.rhnaf.database.AccessAuditLogTable
import com.example.rhnaf.shared.model.JournalEntry
import com.example.rhnaf.shared.model.CostCenter
import com.example.rhnaf.shared.model.PurchaseOrder
import com.example.rhnaf.shared.model.ProductionOrder
import com.example.rhnaf.shared.model.QualityInspection
import com.example.rhnaf.shared.model.MaintenanceOrder
import com.example.rhnaf.shared.model.WarehouseTask
import com.example.rhnaf.shared.model.RecruitmentVacancy
import com.example.rhnaf.shared.model.CustomsDeclaration
import com.example.rhnaf.shared.model.SafetyInspection
import com.example.rhnaf.shared.model.AccessAuditLog
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll

// ============================================================
// Endpoints REST de los modulos estilo SAP integrados a RHNAF
// Prefijo comun: /api/v1/sap/{modulo}/{entidad}
// ============================================================
fun Route.sapModulesRouting() {
    // ---------- FI: Financial Accounting (Contabilidad Financiera) ----------
    route("/api/v1/sap/fi/asientos") {
        get {
            val items = DatabaseFactory.dbQuery {
                JournalEntryTable.selectAll().map {
                    JournalEntry(
                        id = it[JournalEntryTable.id],
                        fecha = it[JournalEntryTable.fecha],
                        cuenta = it[JournalEntryTable.cuenta],
                        concepto = it[JournalEntryTable.concepto],
                        tipo = it[JournalEntryTable.tipo],
                        monto = it[JournalEntryTable.monto],
                        referencia = it[JournalEntryTable.referencia],
                    )
                }
            }
            call.respond(items)
        }
        post {
            val item = call.receive<JournalEntry>()
            DatabaseFactory.dbQuery {
                JournalEntryTable.insert {
                    it[fecha] = item.fecha
                    it[cuenta] = item.cuenta
                    it[concepto] = item.concepto
                    it[tipo] = item.tipo
                    it[monto] = item.monto
                    it[referencia] = item.referencia
                }
            }
            call.respond(HttpStatusCode.Created, mapOf("status" to "ok"))
        }
        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
            DatabaseFactory.dbQuery {
                JournalEntryTable.deleteWhere { JournalEntryTable.id eq id }
            }
            call.respond(mapOf("status" to "ok"))
        }
    }

    // ---------- CO: Controlling (Control de Costos) ----------
    route("/api/v1/sap/co/centros-costo") {
        get {
            val items = DatabaseFactory.dbQuery {
                CostCenterTable.selectAll().map {
                    CostCenter(
                        id = it[CostCenterTable.id],
                        codigo = it[CostCenterTable.codigo],
                        nombre = it[CostCenterTable.nombre],
                        departamento = it[CostCenterTable.departamento],
                        presupuestoMensual = it[CostCenterTable.presupuestoMensual],
                        gastoActual = it[CostCenterTable.gastoActual],
                    )
                }
            }
            call.respond(items)
        }
        post {
            val item = call.receive<CostCenter>()
            DatabaseFactory.dbQuery {
                CostCenterTable.insert {
                    it[codigo] = item.codigo
                    it[nombre] = item.nombre
                    it[departamento] = item.departamento
                    it[presupuestoMensual] = item.presupuestoMensual
                    it[gastoActual] = item.gastoActual
                }
            }
            call.respond(HttpStatusCode.Created, mapOf("status" to "ok"))
        }
        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
            DatabaseFactory.dbQuery {
                CostCenterTable.deleteWhere { CostCenterTable.id eq id }
            }
            call.respond(mapOf("status" to "ok"))
        }
    }

    // ---------- MM: Materials Management (Compras) ----------
    route("/api/v1/sap/mm/ordenes-compra") {
        get {
            val items = DatabaseFactory.dbQuery {
                PurchaseOrderTable.selectAll().map {
                    PurchaseOrder(
                        id = it[PurchaseOrderTable.id],
                        numero = it[PurchaseOrderTable.numero],
                        proveedor = it[PurchaseOrderTable.proveedor],
                        fecha = it[PurchaseOrderTable.fecha],
                        descripcion = it[PurchaseOrderTable.descripcion],
                        montoTotal = it[PurchaseOrderTable.montoTotal],
                        estado = it[PurchaseOrderTable.estado],
                    )
                }
            }
            call.respond(items)
        }
        post {
            val item = call.receive<PurchaseOrder>()
            DatabaseFactory.dbQuery {
                PurchaseOrderTable.insert {
                    it[numero] = item.numero
                    it[proveedor] = item.proveedor
                    it[fecha] = item.fecha
                    it[descripcion] = item.descripcion
                    it[montoTotal] = item.montoTotal
                    it[estado] = item.estado
                }
            }
            call.respond(HttpStatusCode.Created, mapOf("status" to "ok"))
        }
        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
            DatabaseFactory.dbQuery {
                PurchaseOrderTable.deleteWhere { PurchaseOrderTable.id eq id }
            }
            call.respond(mapOf("status" to "ok"))
        }
    }

    // ---------- PP: Production Planning (Planificación de Producción) ----------
    route("/api/v1/sap/pp/ordenes-produccion") {
        get {
            val items = DatabaseFactory.dbQuery {
                ProductionOrderTable.selectAll().map {
                    ProductionOrder(
                        id = it[ProductionOrderTable.id],
                        numero = it[ProductionOrderTable.numero],
                        producto = it[ProductionOrderTable.producto],
                        cantidadPlan = it[ProductionOrderTable.cantidadPlan],
                        cantidadProducida = it[ProductionOrderTable.cantidadProducida],
                        centroTrabajo = it[ProductionOrderTable.centroTrabajo],
                        fechaInicio = it[ProductionOrderTable.fechaInicio],
                        fechaFin = it[ProductionOrderTable.fechaFin],
                        estado = it[ProductionOrderTable.estado],
                    )
                }
            }
            call.respond(items)
        }
        post {
            val item = call.receive<ProductionOrder>()
            DatabaseFactory.dbQuery {
                ProductionOrderTable.insert {
                    it[numero] = item.numero
                    it[producto] = item.producto
                    it[cantidadPlan] = item.cantidadPlan
                    it[cantidadProducida] = item.cantidadProducida
                    it[centroTrabajo] = item.centroTrabajo
                    it[fechaInicio] = item.fechaInicio
                    it[fechaFin] = item.fechaFin
                    it[estado] = item.estado
                }
            }
            call.respond(HttpStatusCode.Created, mapOf("status" to "ok"))
        }
        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
            DatabaseFactory.dbQuery {
                ProductionOrderTable.deleteWhere { ProductionOrderTable.id eq id }
            }
            call.respond(mapOf("status" to "ok"))
        }
    }

    // ---------- QM: Quality Management (Gestión de Calidad) ----------
    route("/api/v1/sap/qm/inspecciones") {
        get {
            val items = DatabaseFactory.dbQuery {
                QualityInspectionTable.selectAll().map {
                    QualityInspection(
                        id = it[QualityInspectionTable.id],
                        fecha = it[QualityInspectionTable.fecha],
                        loteProducto = it[QualityInspectionTable.loteProducto],
                        inspector = it[QualityInspectionTable.inspector],
                        resultado = it[QualityInspectionTable.resultado],
                        observaciones = it[QualityInspectionTable.observaciones],
                    )
                }
            }
            call.respond(items)
        }
        post {
            val item = call.receive<QualityInspection>()
            DatabaseFactory.dbQuery {
                QualityInspectionTable.insert {
                    it[fecha] = item.fecha
                    it[loteProducto] = item.loteProducto
                    it[inspector] = item.inspector
                    it[resultado] = item.resultado
                    it[observaciones] = item.observaciones
                }
            }
            call.respond(HttpStatusCode.Created, mapOf("status" to "ok"))
        }
        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
            DatabaseFactory.dbQuery {
                QualityInspectionTable.deleteWhere { QualityInspectionTable.id eq id }
            }
            call.respond(mapOf("status" to "ok"))
        }
    }

    // ---------- PM: Plant Maintenance (Mantenimiento de Planta) ----------
    route("/api/v1/sap/pm/ordenes-mantenimiento") {
        get {
            val items = DatabaseFactory.dbQuery {
                MaintenanceOrderTable.selectAll().map {
                    MaintenanceOrder(
                        id = it[MaintenanceOrderTable.id],
                        equipo = it[MaintenanceOrderTable.equipo],
                        tipo = it[MaintenanceOrderTable.tipo],
                        fechaProgramada = it[MaintenanceOrderTable.fechaProgramada],
                        fechaRealizada = it[MaintenanceOrderTable.fechaRealizada],
                        tecnico = it[MaintenanceOrderTable.tecnico],
                        estado = it[MaintenanceOrderTable.estado],
                        notas = it[MaintenanceOrderTable.notas],
                    )
                }
            }
            call.respond(items)
        }
        post {
            val item = call.receive<MaintenanceOrder>()
            DatabaseFactory.dbQuery {
                MaintenanceOrderTable.insert {
                    it[equipo] = item.equipo
                    it[tipo] = item.tipo
                    it[fechaProgramada] = item.fechaProgramada
                    it[fechaRealizada] = item.fechaRealizada
                    it[tecnico] = item.tecnico
                    it[estado] = item.estado
                    it[notas] = item.notas
                }
            }
            call.respond(HttpStatusCode.Created, mapOf("status" to "ok"))
        }
        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
            DatabaseFactory.dbQuery {
                MaintenanceOrderTable.deleteWhere { MaintenanceOrderTable.id eq id }
            }
            call.respond(mapOf("status" to "ok"))
        }
    }

    // ---------- EWM: Extended Warehouse Management (Gestión Avanzada de Almacenes) ----------
    route("/api/v1/sap/ewm/tareas") {
        get {
            val items = DatabaseFactory.dbQuery {
                WarehouseTaskTable.selectAll().map {
                    WarehouseTask(
                        id = it[WarehouseTaskTable.id],
                        tipo = it[WarehouseTaskTable.tipo],
                        bin = it[WarehouseTaskTable.bin],
                        sku = it[WarehouseTaskTable.sku],
                        cantidad = it[WarehouseTaskTable.cantidad],
                        asignadoA = it[WarehouseTaskTable.asignadoA],
                        estado = it[WarehouseTaskTable.estado],
                    )
                }
            }
            call.respond(items)
        }
        post {
            val item = call.receive<WarehouseTask>()
            DatabaseFactory.dbQuery {
                WarehouseTaskTable.insert {
                    it[tipo] = item.tipo
                    it[bin] = item.bin
                    it[sku] = item.sku
                    it[cantidad] = item.cantidad
                    it[asignadoA] = item.asignadoA
                    it[estado] = item.estado
                }
            }
            call.respond(HttpStatusCode.Created, mapOf("status" to "ok"))
        }
        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
            DatabaseFactory.dbQuery {
                WarehouseTaskTable.deleteWhere { WarehouseTaskTable.id eq id }
            }
            call.respond(mapOf("status" to "ok"))
        }
    }

    // ---------- HCM: Human Capital Management (Reclutamiento) ----------
    route("/api/v1/sap/hcm/vacantes") {
        get {
            val items = DatabaseFactory.dbQuery {
                RecruitmentVacancyTable.selectAll().map {
                    RecruitmentVacancy(
                        id = it[RecruitmentVacancyTable.id],
                        puesto = it[RecruitmentVacancyTable.puesto],
                        departamento = it[RecruitmentVacancyTable.departamento],
                        fechaApertura = it[RecruitmentVacancyTable.fechaApertura],
                        vacantes = it[RecruitmentVacancyTable.vacantes],
                        candidatosPostulados = it[RecruitmentVacancyTable.candidatosPostulados],
                        estado = it[RecruitmentVacancyTable.estado],
                    )
                }
            }
            call.respond(items)
        }
        post {
            val item = call.receive<RecruitmentVacancy>()
            DatabaseFactory.dbQuery {
                RecruitmentVacancyTable.insert {
                    it[puesto] = item.puesto
                    it[departamento] = item.departamento
                    it[fechaApertura] = item.fechaApertura
                    it[vacantes] = item.vacantes
                    it[candidatosPostulados] = item.candidatosPostulados
                    it[estado] = item.estado
                }
            }
            call.respond(HttpStatusCode.Created, mapOf("status" to "ok"))
        }
        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
            DatabaseFactory.dbQuery {
                RecruitmentVacancyTable.deleteWhere { RecruitmentVacancyTable.id eq id }
            }
            call.respond(mapOf("status" to "ok"))
        }
    }

    // ---------- GTS: Global Trade Services (Comercio Exterior) ----------
    route("/api/v1/sap/gts/pedimentos") {
        get {
            val items = DatabaseFactory.dbQuery {
                CustomsDeclarationTable.selectAll().map {
                    CustomsDeclaration(
                        id = it[CustomsDeclarationTable.id],
                        numeroPedimento = it[CustomsDeclarationTable.numeroPedimento],
                        fecha = it[CustomsDeclarationTable.fecha],
                        cliente = it[CustomsDeclarationTable.cliente],
                        paisDestino = it[CustomsDeclarationTable.paisDestino],
                        valorAduana = it[CustomsDeclarationTable.valorAduana],
                        regimen = it[CustomsDeclarationTable.regimen],
                        estado = it[CustomsDeclarationTable.estado],
                    )
                }
            }
            call.respond(items)
        }
        post {
            val item = call.receive<CustomsDeclaration>()
            DatabaseFactory.dbQuery {
                CustomsDeclarationTable.insert {
                    it[numeroPedimento] = item.numeroPedimento
                    it[fecha] = item.fecha
                    it[cliente] = item.cliente
                    it[paisDestino] = item.paisDestino
                    it[valorAduana] = item.valorAduana
                    it[regimen] = item.regimen
                    it[estado] = item.estado
                }
            }
            call.respond(HttpStatusCode.Created, mapOf("status" to "ok"))
        }
        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
            DatabaseFactory.dbQuery {
                CustomsDeclarationTable.deleteWhere { CustomsDeclarationTable.id eq id }
            }
            call.respond(mapOf("status" to "ok"))
        }
    }

    // ---------- EHS: Environment, Health & Safety ----------
    route("/api/v1/sap/ehs/inspecciones") {
        get {
            val items = DatabaseFactory.dbQuery {
                SafetyInspectionTable.selectAll().map {
                    SafetyInspection(
                        id = it[SafetyInspectionTable.id],
                        fecha = it[SafetyInspectionTable.fecha],
                        area = it[SafetyInspectionTable.area],
                        inspector = it[SafetyInspectionTable.inspector],
                        hallazgos = it[SafetyInspectionTable.hallazgos],
                        riesgo = it[SafetyInspectionTable.riesgo],
                        estado = it[SafetyInspectionTable.estado],
                    )
                }
            }
            call.respond(items)
        }
        post {
            val item = call.receive<SafetyInspection>()
            DatabaseFactory.dbQuery {
                SafetyInspectionTable.insert {
                    it[fecha] = item.fecha
                    it[area] = item.area
                    it[inspector] = item.inspector
                    it[hallazgos] = item.hallazgos
                    it[riesgo] = item.riesgo
                    it[estado] = item.estado
                }
            }
            call.respond(HttpStatusCode.Created, mapOf("status" to "ok"))
        }
        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
            DatabaseFactory.dbQuery {
                SafetyInspectionTable.deleteWhere { SafetyInspectionTable.id eq id }
            }
            call.respond(mapOf("status" to "ok"))
        }
    }

    // ---------- GRC: SAP Security / GRC (Gobierno, Riesgo y Cumplimiento) ----------
    route("/api/v1/sap/grc/auditoria-accesos") {
        get {
            val items = DatabaseFactory.dbQuery {
                AccessAuditLogTable.selectAll().map {
                    AccessAuditLog(
                        id = it[AccessAuditLogTable.id],
                        fecha = it[AccessAuditLogTable.fecha],
                        usuario = it[AccessAuditLogTable.usuario],
                        accion = it[AccessAuditLogTable.accion],
                        modulo = it[AccessAuditLogTable.modulo],
                        resultado = it[AccessAuditLogTable.resultado],
                    )
                }
            }
            call.respond(items)
        }
        post {
            val item = call.receive<AccessAuditLog>()
            DatabaseFactory.dbQuery {
                AccessAuditLogTable.insert {
                    it[fecha] = item.fecha
                    it[usuario] = item.usuario
                    it[accion] = item.accion
                    it[modulo] = item.modulo
                    it[resultado] = item.resultado
                }
            }
            call.respond(HttpStatusCode.Created, mapOf("status" to "ok"))
        }
        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
            DatabaseFactory.dbQuery {
                AccessAuditLogTable.deleteWhere { AccessAuditLogTable.id eq id }
            }
            call.respond(mapOf("status" to "ok"))
        }
    }

}