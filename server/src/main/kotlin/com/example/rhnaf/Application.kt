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
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import com.example.rhnaf.service.HuggingFaceService
import io.ktor.server.request.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.http.content.*
import java.io.File

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    DatabaseFactory.init()
    
    val hfApiKey = environment.config.propertyOrNull("huggingface.api_key")?.getString() ?: ""
    val hfService = HuggingFaceService(hfApiKey)

    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
    }

    install(ContentNegotiation) {
        json()
    }
    
    routing {
        // Sirve la Web App (Compose HTML) desde una carpeta física
        staticFiles("/", File("static"), index = "index.html")

        route("/api") {
            post("/login") {
                val credentials = call.receive<Map<String, String>>()
                val username = credentials["username"]
                val password = credentials["password"]
                
                // Login con usuarios y roles (Demo)
                val validUsers = mapOf(
                    "d.trujillo@brancoindustries.com" to "Branco2025",
                    "arni.oziel@brancoindustries.com" to "Branco2025",
                    "compras@brancoindustries.com" to "Branco2025",
                    "seguridad@brancoindustries.com" to "Branco2025",
                    "mantenimiento@brancoindustries.com" to "Branco2025"
                )
                
                if (validUsers[username] == password) {
                    call.respond(mapOf("status" to "success", "token" to "mock-jwt-token-nafconnect"))
                } else {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("status" to "error", "message" to "Credenciales incorrectas"))
                }
            }

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
                            readerId = it[EmployeeTable.readerId],
                            attritionRisk = it[EmployeeTable.attritionRisk]
                        )
                    }
                }
                call.respond(employees)
            }

            post("/employee/add") {
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
                        it[readerId] = emp.readerId ?: emp.id
                        it[attritionRisk] = emp.attritionRisk
                    }
                }
                call.respond(HttpStatusCode.Created, mapOf("status" to "success"))
            }

            post("/employee/update") {
                val emp = call.receive<Employee>()
                DatabaseFactory.dbQuery {
                    EmployeeTable.update({ EmployeeTable.id eq emp.id }) {
                        it[firstName] = emp.firstName
                        it[lastName] = emp.lastName
                        it[position] = emp.position
                        it[department] = emp.department
                        it[entryDate] = emp.entryDate
                        it[status] = emp.status
                        it[readerId] = emp.readerId
                        it[attritionRisk] = emp.attritionRisk
                    }
                }
                call.respond(mapOf("status" to "success"))
            }

            delete("/employee/{id}") {
                val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                DatabaseFactory.dbQuery {
                    EmployeeTable.deleteWhere { EmployeeTable.id eq id }
                }
                call.respond(mapOf("status" to "success"))
            }

            post("/attendance/biometric") {
                val data = call.receive<Map<String, String>>()
                val readerId = data["employeeNo"] ?: data["userId"]
                val authType = data["authType"] ?: "Face"
                
                // Registro de asistencia por biometría (Rostro)
                println("Evento Biométrico recibido - ID Lectora: $readerId, Tipo: $authType")
                
                // Aquí se buscaría en la DB al empleado con ese readerId y se insertaría en una tabla de logs
                call.respond(mapOf(
                    "status" to "success", 
                    "message" to "Acceso verificado por Rostro",
                    "timestamp" to System.currentTimeMillis()
                ))
            }
            
            post("/safety/analyze") {
                val params = call.receive<Map<String, String>>()
                val desc = params["description"] ?: ""
                val analysis = hfService.analyzeIncident(desc)
                call.respond(mapOf("analysis" to analysis))
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
