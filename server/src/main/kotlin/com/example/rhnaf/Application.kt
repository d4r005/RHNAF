package com.example.rhnaf

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import com.example.rhnaf.shared.model.Employee
import com.example.rhnaf.shared.model.EmployeeStatus
import com.example.rhnaf.database.DatabaseFactory
import com.example.rhnaf.database.EmployeeTable
import com.example.rhnaf.database.UserTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import com.example.rhnaf.service.HuggingFaceService
import com.example.rhnaf.service.AttendanceUseCase
import com.example.rhnaf.routes.attendanceRouting
import com.example.rhnaf.routes.employeeSyncRouting
import com.example.rhnaf.routes.warehouseRouting
import com.example.rhnaf.routes.shippingRouting
import com.example.rhnaf.routes.prePayrollRouting
import com.example.rhnaf.routes.sapModulesRouting
import com.example.rhnaf.auth.Roles
import com.example.rhnaf.auth.requireRoleOr403
import io.ktor.server.request.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.http.content.*
import java.io.File

import com.example.rhnaf.database.IncidentTable
import com.example.rhnaf.shared.model.WeeklyIncident
import com.example.rhnaf.shared.model.AttendanceReport

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    DatabaseFactory.init()
    
    val hfApiKey = environment.config.propertyOrNull("huggingface.api_key")?.getString() ?: ""
    val hfService = HuggingFaceService(hfApiKey)
    val attendanceUseCase = AttendanceUseCase()

    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Get)
        allowCredentials = true
        allowNonSimpleContentTypes = true
    }

    install(ContentNegotiation) {
        json()
    }
    
    routing {
        attendanceRouting(attendanceUseCase)
        employeeSyncRouting()
        warehouseRouting()
        shippingRouting()
        prePayrollRouting()
        sapModulesRouting()

        // Sirve la Web App (Compose HTML) desde una carpeta física
        staticFiles("/", File("static"), index = "index.html")

        route("/api") {
            post("/login") {
                val credentials = call.receive<Map<String, String>>()
                val username = credentials["username"] ?: ""
                val password = credentials["password"] ?: ""
                
                val user = DatabaseFactory.dbQuery {
                    UserTable.selectAll().where { 
                        (UserTable.email eq username) and 
                        (UserTable.password eq password) 
                    }.singleOrNull()
                }
                
                if (user != null) {
                    call.respond(mapOf(
                        "status" to "success", 
                        "token" to user[UserTable.email],
                        "role" to user[UserTable.role],
                        "name" to user[UserTable.name]
                    ))
                } else {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("status" to "error", "message" to "Credenciales incorrectas"))
                }
            }

            // --- LECTURA (sin restricción de rol) ---

            get("/employees") {
                val employees = DatabaseFactory.dbQuery {
                    EmployeeTable.selectAll().map {
                        Employee(
                            id = it[EmployeeTable.id],
                            firstName = it[EmployeeTable.firstName],
                            lastName = it[EmployeeTable.lastName],
                            position = it[EmployeeTable.position],
                            department = it[EmployeeTable.department],
                            entryDate = it[EmployeeTable.entryDate],
                            status = it[EmployeeTable.status],
                            rfc = it[EmployeeTable.rfc],
                            curp = it[EmployeeTable.curp],
                            nss = it[EmployeeTable.nss],
                            readerId = it[EmployeeTable.readerId],
                            photoUrl = it[EmployeeTable.photoUrl],
                            attritionRisk = it[EmployeeTable.attritionRisk]
                        )
                    }
                }
                call.respond(employees)
            }

            get("/users") {
                val users = DatabaseFactory.dbQuery {
                    UserTable.selectAll().map {
                        mapOf(
                            "email" to it[UserTable.email],
                            "role" to it[UserTable.role],
                            "name" to it[UserTable.name]
                        )
                    }
                }
                call.respond(users)
            }

            // --- ESCRITURA / BAJA / ELIMINACIÓN (requiere rol) ---

            post("/user/add") {
                if (requireRoleOr403(call, Roles.CAN_MANAGE_USERS) == null) return@post
                val data = call.receive<Map<String, String>>()
                DatabaseFactory.dbQuery {
                    UserTable.insert {
                        it[email] = data["email"] ?: ""
                        it[password] = data["password"] ?: ""
                        it[role] = data["role"] ?: "EMPLEADO"
                        it[name] = data["name"] ?: "Usuario Nuevo"
                    }
                }
                call.respond(HttpStatusCode.Created, mapOf("status" to "success"))
            }

            post("/user/update") {
                if (requireRoleOr403(call, Roles.CAN_MANAGE_USERS) == null) return@post
                val data = call.receive<Map<String, String>>()
                val email = data["email"] ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "email requerido"))
                DatabaseFactory.dbQuery {
                    UserTable.update({ UserTable.email eq email }) {
                        if (data.containsKey("role")) it[role] = data["role"]!!
                        if (data.containsKey("name")) it[name] = data["name"]!!
                        if (data.containsKey("password")) it[password] = data["password"]!!
                    }
                }
                call.respond(mapOf("status" to "success"))
            }

            delete("/user/{email}") {
                if (requireRoleOr403(call, Roles.CAN_MANAGE_USERS) == null) return@delete
                val email = call.parameters["email"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                DatabaseFactory.dbQuery {
                    UserTable.deleteWhere { UserTable.email eq email }
                }
                call.respond(mapOf("status" to "success"))
            }

            post("/employee/add") {
                if (requireRoleOr403(call, Roles.CAN_MANAGE_EMPLOYEES) == null) return@post
                val emp = call.receive<Employee>()
                DatabaseFactory.dbQuery {
                    EmployeeTable.insert {
                        it[id] = emp.id
                        it[firstName] = emp.firstName
                        it[lastName] = emp.lastName
                        it[position] = emp.position
                        it[department] = emp.department
                        it[entryDate] = emp.entryDate
                        it[status] = emp.status
                        it[rfc] = emp.rfc
                        it[curp] = emp.curp
                        it[nss] = emp.nss
                        it[readerId] = emp.readerId ?: emp.id
                        it[photoUrl] = emp.photoUrl
                        it[attritionRisk] = emp.attritionRisk
                    }
                }
                call.respond(HttpStatusCode.Created, mapOf("status" to "success"))
            }

            post("/employee/update") {
                if (requireRoleOr403(call, Roles.CAN_MANAGE_EMPLOYEES) == null) return@post
                val emp = call.receive<Employee>()
                DatabaseFactory.dbQuery {
                    EmployeeTable.update({ EmployeeTable.id eq emp.id }) {
                        it[firstName] = emp.firstName
                        it[lastName] = emp.lastName
                        it[position] = emp.position
                        it[department] = emp.department
                        it[entryDate] = emp.entryDate
                        it[status] = emp.status
                        it[rfc] = emp.rfc
                        it[curp] = emp.curp
                        it[nss] = emp.nss
                        it[readerId] = emp.readerId
                        it[photoUrl] = emp.photoUrl
                        it[attritionRisk] = emp.attritionRisk
                    }
                }
                call.respond(mapOf("status" to "success"))
            }

            delete("/employee/{id}") {
                if (requireRoleOr403(call, Roles.CAN_MANAGE_EMPLOYEES) == null) return@delete
                val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                DatabaseFactory.dbQuery {
                    EmployeeTable.deleteWhere { EmployeeTable.id eq id }
                }
                call.respond(mapOf("status" to "success"))
            }

            // --- SIN RESTRICCIÓN ---

            post("/attendance/biometric") {
                val data = call.receive<Map<String, String>>()
                val employeeId = data["employeeNo"] ?: data["userId"] ?: "UNKNOWN"
                val authType = data["authType"] ?: "Face"
                val deviceSerial = data["deviceSerial"] ?: "MOBILE_APP"
                
                attendanceUseCase.registerCheckIn(
                    employeeId = employeeId,
                    timestamp = java.time.LocalDateTime.now().toString(),
                    deviceSerial = deviceSerial,
                    verifyMode = authType
                )
                
                println("Registro biométrico guardado: Empleado $employeeId via $authType")
                
                call.respond(mapOf(
                    "status" to "success", 
                    "message" to "Asistencia registrada correctamente en el ERP",
                    "timestamp" to System.currentTimeMillis()
                ))
            }
            
            post("/safety/analyze") {
                val params = call.receive<Map<String, String>>()
                val desc = params["description"] ?: ""
                val analysis = hfService.analyzeIncident(desc)
                call.respond(mapOf("analysis" to analysis))
            }

            get("/incidents/week/{week}") {
                val week = call.parameters["week"]?.toIntOrNull() ?: 1
                val incidents = DatabaseFactory.dbQuery {
                    IncidentTable.selectAll().where { IncidentTable.weekNumber eq week }.map {
                        WeeklyIncident(
                            employeeId = it[IncidentTable.employeeId],
                            weekNumber = it[IncidentTable.weekNumber],
                            year = it[IncidentTable.year],
                            attendance = it[IncidentTable.attendance].split(","),
                            punctualityBonus = it[IncidentTable.punctualityBonus],
                            attendanceBonus = it[IncidentTable.attendanceBonus],
                            sundayPremium = it[IncidentTable.sundayPremium],
                            extraHours = it[IncidentTable.extraHours],
                            foodAllowance = it[IncidentTable.foodAllowance],
                            weekendBonus = it[IncidentTable.weekendBonus],
                            perfectAttendance = it[IncidentTable.perfectAttendance],
                            absences = it[IncidentTable.absences],
                            observations = it[IncidentTable.observations],
                            deductions = it[IncidentTable.deductions],
                            pending = it[IncidentTable.pending],
                            infonavit = it[IncidentTable.infonavit],
                            otherDiscounts = it[IncidentTable.otherDiscounts]
                        )
                    }
                }
                call.respond(incidents)
            }

            post("/incidents/save") {
                val incident = call.receive<WeeklyIncident>()
                DatabaseFactory.dbQuery {
                    val exists = IncidentTable.selectAll().where {
                        (IncidentTable.employeeId eq incident.employeeId) and 
                        (IncidentTable.weekNumber eq incident.weekNumber) 
                    }.count() > 0

                    if (exists) {
                        IncidentTable.update({ 
                            (IncidentTable.employeeId eq incident.employeeId) and 
                            (IncidentTable.weekNumber eq incident.weekNumber) 
                        }) {
                            it[attendance] = incident.attendance.joinToString(",")
                            it[punctualityBonus] = incident.punctualityBonus
                            it[attendanceBonus] = incident.attendanceBonus
                            it[sundayPremium] = incident.sundayPremium
                            it[extraHours] = incident.extraHours
                            it[foodAllowance] = incident.foodAllowance
                            it[weekendBonus] = incident.weekendBonus
                            it[perfectAttendance] = incident.perfectAttendance
                            it[absences] = incident.absences
                            it[observations] = incident.observations
                            it[deductions] = incident.deductions
                            it[pending] = incident.pending
                            it[infonavit] = incident.infonavit
                            it[otherDiscounts] = incident.otherDiscounts
                        }
                    } else {
                        IncidentTable.insert {
                            it[employeeId] = incident.employeeId
                            it[weekNumber] = incident.weekNumber
                            it[year] = incident.year
                            it[attendance] = incident.attendance.joinToString(",")
                            it[punctualityBonus] = incident.punctualityBonus
                            it[attendanceBonus] = incident.attendanceBonus
                            it[sundayPremium] = incident.sundayPremium
                            it[extraHours] = incident.extraHours
                            it[foodAllowance] = incident.foodAllowance
                            it[weekendBonus] = incident.weekendBonus
                            it[perfectAttendance] = incident.perfectAttendance
                            it[absences] = incident.absences
                            it[observations] = incident.observations
                            it[deductions] = incident.deductions
                            it[pending] = incident.pending
                            it[infonavit] = incident.infonavit
                            it[otherDiscounts] = incident.otherDiscounts
                        }
                    }
                }
                call.respond(mapOf("status" to "success"))
            }

            post("/employee/ocr") {
                val multipart = call.receiveMultipart()
                var imageBytes: ByteArray? = null
                
                multipart.forEachPart { part ->
                    if (part is PartData.FileItem) {
                        imageBytes = part.streamProvider().readBytes()
                    }
                    part.dispose()
                }

                if (imageBytes != null) {
                    val extractedText = hfService.extractTextFromImage(imageBytes!!)
                    call.respond(mapOf("extractedText" to extractedText))
                } else {
                    call.respond(HttpStatusCode.BadRequest, "No se encontró imagen en la petición")
                }
            }
        }
    }
}
