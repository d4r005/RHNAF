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
import com.example.rhnaf.database.SafetyIncidentTable
import com.example.rhnaf.database.WorkPermitTable
import com.example.rhnaf.database.PpeDeliveryTable
import com.example.rhnaf.database.SafetyTrainingTable
import com.example.rhnaf.database.EmergencyDrillTable
import com.example.rhnaf.database.RiskMatrixTable
import com.example.rhnaf.database.AccessAuditLogTable
import com.example.rhnaf.database.EnvironmentalWasteTable
import com.example.rhnaf.database.OccupationalHealthTable
import com.example.rhnaf.database.ChemicalInventoryTable
import com.example.rhnaf.shared.model.*
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
    // 1. Inspecciones de Seguridad (mejorada)
    route("/api/v1/sap/ehs/inspecciones") {
        get {
            val items = DatabaseFactory.dbQuery {
                SafetyInspectionTable.selectAll().map {
                    SafetyInspection(
                        id = it[SafetyInspectionTable.id],
                        fecha = it[SafetyInspectionTable.fecha],
                        tipoInspeccion = it[SafetyInspectionTable.tipoInspeccion],
                        area = it[SafetyInspectionTable.area],
                        inspector = it[SafetyInspectionTable.inspector],
                        hallazgos = it[SafetyInspectionTable.hallazgos],
                        riesgo = it[SafetyInspectionTable.riesgo],
                        accionesCorrectivas = it[SafetyInspectionTable.accionesCorrectivas],
                        fechaCierre = it[SafetyInspectionTable.fechaCierre],
                        evidencia = it[SafetyInspectionTable.evidencia],
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
                    it[tipoInspeccion] = item.tipoInspeccion
                    it[area] = item.area
                    it[inspector] = item.inspector
                    it[hallazgos] = item.hallazgos
                    it[riesgo] = item.riesgo
                    it[accionesCorrectivas] = item.accionesCorrectivas
                    it[fechaCierre] = item.fechaCierre
                    it[evidencia] = item.evidencia
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

    // 2. Incidentes y Accidentes
    route("/api/v1/sap/ehs/incidentes") {
        get {
            val items = DatabaseFactory.dbQuery {
                SafetyIncidentTable.selectAll().map {
                    SafetyIncident(
                        id = it[SafetyIncidentTable.id],
                        fecha = it[SafetyIncidentTable.fecha],
                        tipo = it[SafetyIncidentTable.tipo],
                        severidad = it[SafetyIncidentTable.severidad],
                        personaAfectada = it[SafetyIncidentTable.personaAfectada],
                        departamento = it[SafetyIncidentTable.departamento],
                        parteCuerpo = it[SafetyIncidentTable.parteCuerpo],
                        diasPerdidos = it[SafetyIncidentTable.diasPerdidos],
                        descripcion = it[SafetyIncidentTable.descripcion],
                        causaRaiz = it[SafetyIncidentTable.causaRaiz],
                        accionesCorrectivas = it[SafetyIncidentTable.accionesCorrectivas],
                        estado = it[SafetyIncidentTable.estado],
                    )
                }
            }
            call.respond(items)
        }
        post {
            val item = call.receive<SafetyIncident>()
            DatabaseFactory.dbQuery {
                SafetyIncidentTable.insert {
                    it[fecha] = item.fecha
                    it[tipo] = item.tipo
                    it[severidad] = item.severidad
                    it[personaAfectada] = item.personaAfectada
                    it[departamento] = item.departamento
                    it[parteCuerpo] = item.parteCuerpo
                    it[diasPerdidos] = item.diasPerdidos
                    it[descripcion] = item.descripcion
                    it[causaRaiz] = item.causaRaiz
                    it[accionesCorrectivas] = item.accionesCorrectivas
                    it[estado] = item.estado
                }
            }
            call.respond(HttpStatusCode.Created, mapOf("status" to "ok"))
        }
        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
            DatabaseFactory.dbQuery {
                SafetyIncidentTable.deleteWhere { SafetyIncidentTable.id eq id }
            }
            call.respond(mapOf("status" to "ok"))
        }
    }

    // 3. Permisos de Trabajo
    route("/api/v1/sap/ehs/permisos-trabajo") {
        get {
            val items = DatabaseFactory.dbQuery {
                WorkPermitTable.selectAll().map {
                    WorkPermit(
                        id = it[WorkPermitTable.id],
                        tipo = it[WorkPermitTable.tipo],
                        solicitante = it[WorkPermitTable.solicitante],
                        autorizadoPor = it[WorkPermitTable.autorizadoPor],
                        fechaInicio = it[WorkPermitTable.fechaInicio],
                        fechaFin = it[WorkPermitTable.fechaFin],
                        area = it[WorkPermitTable.area],
                        riesgosIdentificados = it[WorkPermitTable.riesgosIdentificados],
                        eppRequerido = it[WorkPermitTable.eppRequerido],
                        estado = it[WorkPermitTable.estado],
                    )
                }
            }
            call.respond(items)
        }
        post {
            val item = call.receive<WorkPermit>()
            DatabaseFactory.dbQuery {
                WorkPermitTable.insert {
                    it[tipo] = item.tipo
                    it[solicitante] = item.solicitante
                    it[autorizadoPor] = item.autorizadoPor
                    it[fechaInicio] = item.fechaInicio
                    it[fechaFin] = item.fechaFin
                    it[area] = item.area
                    it[riesgosIdentificados] = item.riesgosIdentificados
                    it[eppRequerido] = item.eppRequerido
                    it[estado] = item.estado
                }
            }
            call.respond(HttpStatusCode.Created, mapOf("status" to "ok"))
        }
        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
            DatabaseFactory.dbQuery {
                WorkPermitTable.deleteWhere { WorkPermitTable.id eq id }
            }
            call.respond(mapOf("status" to "ok"))
        }
    }

    // 4. Entrega de EPP
    route("/api/v1/sap/ehs/entregas-epp") {
        get {
            val items = DatabaseFactory.dbQuery {
                PpeDeliveryTable.selectAll().map {
                    PpeDelivery(
                        id = it[PpeDeliveryTable.id],
                        fecha = it[PpeDeliveryTable.fecha],
                        empleado = it[PpeDeliveryTable.empleado],
                        tipoEpp = it[PpeDeliveryTable.tipoEpp],
                        talla = it[PpeDeliveryTable.talla],
                        proximaReposicion = it[PpeDeliveryTable.proximaReposicion],
                        firma = it[PpeDeliveryTable.firma],
                    )
                }
            }
            call.respond(items)
        }
        post {
            val item = call.receive<PpeDelivery>()
            DatabaseFactory.dbQuery {
                PpeDeliveryTable.insert {
                    it[fecha] = item.fecha
                    it[empleado] = item.empleado
                    it[tipoEpp] = item.tipoEpp
                    it[talla] = item.talla
                    it[proximaReposicion] = item.proximaReposicion
                    it[firma] = item.firma
                }
            }
            call.respond(HttpStatusCode.Created, mapOf("status" to "ok"))
        }
        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
            DatabaseFactory.dbQuery {
                PpeDeliveryTable.deleteWhere { PpeDeliveryTable.id eq id }
            }
            call.respond(mapOf("status" to "ok"))
        }
    }

    // 5. Capacitaciones de Seguridad
    route("/api/v1/sap/ehs/capacitaciones") {
        get {
            val items = DatabaseFactory.dbQuery {
                SafetyTrainingTable.selectAll().map {
                    SafetyTraining(
                        id = it[SafetyTrainingTable.id],
                        fecha = it[SafetyTrainingTable.fecha],
                        tema = it[SafetyTrainingTable.tema],
                        instructor = it[SafetyTrainingTable.instructor],
                        asistentes = it[SafetyTrainingTable.asistentes],
                        vigenciaMeses = it[SafetyTrainingTable.vigenciaMeses],
                        proximaFecha = it[SafetyTrainingTable.proximaFecha],
                        estado = it[SafetyTrainingTable.estado],
                    )
                }
            }
            call.respond(items)
        }
        post {
            val item = call.receive<SafetyTraining>()
            DatabaseFactory.dbQuery {
                SafetyTrainingTable.insert {
                    it[fecha] = item.fecha
                    it[tema] = item.tema
                    it[instructor] = item.instructor
                    it[asistentes] = item.asistentes
                    it[vigenciaMeses] = item.vigenciaMeses
                    it[proximaFecha] = item.proximaFecha
                    it[estado] = item.estado
                }
            }
            call.respond(HttpStatusCode.Created, mapOf("status" to "ok"))
        }
        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
            DatabaseFactory.dbQuery {
                SafetyTrainingTable.deleteWhere { SafetyTrainingTable.id eq id }
            }
            call.respond(mapOf("status" to "ok"))
        }
    }

    // 6. Simulacros de Emergencia
    route("/api/v1/sap/ehs/simulacros") {
        get {
            val items = DatabaseFactory.dbQuery {
                EmergencyDrillTable.selectAll().map {
                    EmergencyDrill(
                        id = it[EmergencyDrillTable.id],
                        fecha = it[EmergencyDrillTable.fecha],
                        tipo = it[EmergencyDrillTable.tipo],
                        participantes = it[EmergencyDrillTable.participantes],
                        tiempoEvacuacion = it[EmergencyDrillTable.tiempoEvacuacion],
                        resultado = it[EmergencyDrillTable.resultado],
                        observaciones = it[EmergencyDrillTable.observaciones],
                        estado = it[EmergencyDrillTable.estado],
                    )
                }
            }
            call.respond(items)
        }
        post {
            val item = call.receive<EmergencyDrill>()
            DatabaseFactory.dbQuery {
                EmergencyDrillTable.insert {
                    it[fecha] = item.fecha
                    it[tipo] = item.tipo
                    it[participantes] = item.participantes
                    it[tiempoEvacuacion] = item.tiempoEvacuacion
                    it[resultado] = item.resultado
                    it[observaciones] = item.observaciones
                    it[estado] = item.estado
                }
            }
            call.respond(HttpStatusCode.Created, mapOf("status" to "ok"))
        }
        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
            DatabaseFactory.dbQuery {
                EmergencyDrillTable.deleteWhere { EmergencyDrillTable.id eq id }
            }
            call.respond(mapOf("status" to "ok"))
        }
    }

    // 7. Matriz de Riesgos / IPER
    route("/api/v1/sap/ehs/matriz-riesgos") {
        get {
            val items = DatabaseFactory.dbQuery {
                RiskMatrixTable.selectAll().map {
                    RiskMatrix(
                        id = it[RiskMatrixTable.id],
                        area = it[RiskMatrixTable.area],
                        proceso = it[RiskMatrixTable.proceso],
                        riesgoIdentificado = it[RiskMatrixTable.riesgoIdentificado],
                        probabilidad = it[RiskMatrixTable.probabilidad],
                        severidad = it[RiskMatrixTable.severidad],
                        nivelRiesgo = it[RiskMatrixTable.nivelRiesgo],
                        controles = it[RiskMatrixTable.controles],
                        responsable = it[RiskMatrixTable.responsable],
                        estado = it[RiskMatrixTable.estado],
                    )
                }
            }
            call.respond(items)
        }
        post {
            val item = call.receive<RiskMatrix>()
            DatabaseFactory.dbQuery {
                RiskMatrixTable.insert {
                    it[area] = item.area
                    it[proceso] = item.proceso
                    it[riesgoIdentificado] = item.riesgoIdentificado
                    it[probabilidad] = item.probabilidad
                    it[severidad] = item.severidad
                    it[nivelRiesgo] = item.nivelRiesgo
                    it[controles] = item.controles
                    it[responsable] = item.responsable
                    it[estado] = item.estado
                }
            }
            call.respond(HttpStatusCode.Created, mapOf("status" to "ok"))
        }
        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
            DatabaseFactory.dbQuery {
                RiskMatrixTable.deleteWhere { RiskMatrixTable.id eq id }
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

    // --- ENDPOINTS EXPANSION EHS ---

    // 8. Gestion Ambiental (Residuos)
    route("/api/v1/sap/ehs/residuos") {
        get {
            val items = DatabaseFactory.dbQuery {
                EnvironmentalWasteTable.selectAll().map {
                    WasteManifest(
                        id = it[EnvironmentalWasteTable.id],
                        fecha = it[EnvironmentalWasteTable.fecha],
                        residuo = it[EnvironmentalWasteTable.residuo],
                        tipo = it[EnvironmentalWasteTable.tipo],
                        cantidad = it[EnvironmentalWasteTable.cantidad],
                        unidad = it[EnvironmentalWasteTable.unidad],
                        transportista = it[EnvironmentalWasteTable.transportista],
                        destinoFinal = it[EnvironmentalWasteTable.destinoFinal],
                        numeroManifiesto = it[EnvironmentalWasteTable.numeroManifiesto],
                        estado = it[EnvironmentalWasteTable.estado]
                    )
                }
            }
            call.respond(items)
        }
        post {
            val item = call.receive<WasteManifest>()
            DatabaseFactory.dbQuery {
                EnvironmentalWasteTable.insert {
                    it[fecha] = item.fecha
                    it[residuo] = item.residuo
                    it[tipo] = item.tipo
                    it[cantidad] = item.cantidad
                    it[unidad] = item.unidad
                    it[transportista] = item.transportista
                    it[destinoFinal] = item.destinoFinal
                    it[numeroManifiesto] = item.numeroManifiesto
                    it[estado] = item.estado
                }
            }
            call.respond(HttpStatusCode.Created, mapOf("status" to "ok"))
        }
        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
            DatabaseFactory.dbQuery {
                EnvironmentalWasteTable.deleteWhere { EnvironmentalWasteTable.id eq id }
            }
            call.respond(mapOf("status" to "ok"))
        }
    }

    // 9. Salud Ocupacional (Examenes Medicos)
    route("/api/v1/sap/ehs/salud") {
        get {
            val items = DatabaseFactory.dbQuery {
                OccupationalHealthTable.selectAll().map {
                    MedicalExam(
                        id = it[OccupationalHealthTable.id],
                        empleadoId = it[OccupationalHealthTable.empleadoId],
                        nombreEmpleado = it[OccupationalHealthTable.nombreEmpleado],
                        fecha = it[OccupationalHealthTable.fecha],
                        tipoExamen = it[OccupationalHealthTable.tipoExamen],
                        resultado = it[OccupationalHealthTable.resultado],
                        observaciones = it[OccupationalHealthTable.observaciones],
                        proximaCita = it[OccupationalHealthTable.proximaCita],
                        medico = it[OccupationalHealthTable.medico]
                    )
                }
            }
            call.respond(items)
        }
        post {
            val item = call.receive<MedicalExam>()
            DatabaseFactory.dbQuery {
                OccupationalHealthTable.insert {
                    it[empleadoId] = item.empleadoId
                    it[nombreEmpleado] = item.nombreEmpleado
                    it[fecha] = item.fecha
                    it[tipoExamen] = item.tipoExamen
                    it[resultado] = item.resultado
                    it[observaciones] = item.observaciones
                    it[proximaCita] = item.proximaCita
                    it[medico] = item.medico
                }
            }
            call.respond(HttpStatusCode.Created, mapOf("status" to "ok"))
        }
        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
            DatabaseFactory.dbQuery {
                OccupationalHealthTable.deleteWhere { OccupationalHealthTable.id eq id }
            }
            call.respond(mapOf("status" to "ok"))
        }
    }

    // 10. Inventario de Quimicos (MSDS)
    route("/api/v1/sap/ehs/quimicos") {
        get {
            val items = DatabaseFactory.dbQuery {
                ChemicalInventoryTable.selectAll().map {
                    ChemicalProduct(
                        id = it[ChemicalInventoryTable.id],
                        nombre = it[ChemicalInventoryTable.nombre],
                        fabricante = it[ChemicalInventoryTable.fabricante],
                        areaUso = it[ChemicalInventoryTable.areaUso],
                        nivelRiesgo = it[ChemicalInventoryTable.nivelRiesgo],
                        hojaSeguridadUrl = it[ChemicalInventoryTable.hojaSeguridadUrl],
                        estado = it[ChemicalInventoryTable.estado],
                        ultimaRevision = it[ChemicalInventoryTable.ultimaRevision]
                    )
                }
            }
            call.respond(items)
        }
        post {
            val item = call.receive<ChemicalProduct>()
            DatabaseFactory.dbQuery {
                ChemicalInventoryTable.insert {
                    it[nombre] = item.nombre
                    it[fabricante] = item.fabricante
                    it[areaUso] = item.areaUso
                    it[nivelRiesgo] = item.nivelRiesgo
                    it[hojaSeguridadUrl] = item.hojaSeguridadUrl
                    it[estado] = item.estado
                    it[ultimaRevision] = item.ultimaRevision
                }
            }
            call.respond(HttpStatusCode.Created, mapOf("status" to "ok"))
        }
        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
            DatabaseFactory.dbQuery {
                ChemicalInventoryTable.deleteWhere { ChemicalInventoryTable.id eq id }
            }
            call.respond(mapOf("status" to "ok"))
        }
    }

}