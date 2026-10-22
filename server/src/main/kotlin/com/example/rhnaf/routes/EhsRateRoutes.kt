package com.example.rhnaf.routes

import com.example.rhnaf.auth.Roles
import com.example.rhnaf.auth.requireRoleOr403
import com.example.rhnaf.database.DatabaseFactory
import com.example.rhnaf.database.EhsRatePeriodTable
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

@Serializable
data class EhsRatePeriod(
    val id: Int = 0,
    val periodo: String,
    val horasTrabajadas: Double,
    val accidentesRegistrables: Int,
    val diasPerdidos: Int,
    val fuenteHoras: String,
    val validadoPor: String,
    val motivoRevision: String = "",
    val version: Int = 0,
    val frecuenciaPorMillon: Double = 0.0,
    val gravedadPorMillon: Double = 0.0
)

private fun rateFrom(row: ResultRow): EhsRatePeriod {
    val hours = row[EhsRatePeriodTable.horasTrabajadas]
    return EhsRatePeriod(
        id = row[EhsRatePeriodTable.id],
        periodo = row[EhsRatePeriodTable.periodo],
        horasTrabajadas = hours,
        accidentesRegistrables = row[EhsRatePeriodTable.accidentesRegistrables],
        diasPerdidos = row[EhsRatePeriodTable.diasPerdidos],
        fuenteHoras = row[EhsRatePeriodTable.fuenteHoras],
        validadoPor = row[EhsRatePeriodTable.validadoPor],
        motivoRevision = row[EhsRatePeriodTable.motivoRevision],
        version = row[EhsRatePeriodTable.version],
        frecuenciaPorMillon = row[EhsRatePeriodTable.accidentesRegistrables] * 1_000_000.0 / hours,
        gravedadPorMillon = row[EhsRatePeriodTable.diasPerdidos] * 1_000_000.0 / hours
    )
}

fun Route.ehsRateRouting() {
    route("/api/v1/ehs/tasas") {
        get {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.EHS_WRITE) ?: return@safeApiCall
                val history = call.request.queryParameters["historial"] == "true"
                val periods = DatabaseFactory.dbQuery {
                    val all = EhsRatePeriodTable.selectAll().map(::rateFrom)
                    val selected = if (history) all else all.groupBy { it.periodo }.values.map { it.maxBy { entry -> entry.version } }
                    selected.sortedWith(compareByDescending<EhsRatePeriod> { it.periodo }.thenByDescending { it.version })
                }
                call.respond(periods)
            }
        }
        post {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.EHS_WRITE) ?: return@safeApiCall
                val item = call.receive<EhsRatePeriod>()
                val error = when {
                    !item.periodo.matches(Regex("\\d{4}-(0[1-9]|1[0-2])")) -> "Periodo inválido (AAAA-MM)"
                    !item.horasTrabajadas.isFinite() || item.horasTrabajadas <= 0 || item.horasTrabajadas > 1_000_000_000 -> "Horas trabajadas deben ser positivas"
                    item.accidentesRegistrables !in 0..100_000 || item.diasPerdidos !in 0..10_000_000 -> "Conteos inválidos"
                    item.fuenteHoras.isBlank() || item.fuenteHoras.length > 300 -> "Fuente de horas obligatoria"
                    item.validadoPor.isBlank() || item.validadoPor.length > 200 -> "Responsable validador obligatorio"
                    item.motivoRevision.length > 500 -> "Motivo de revisión demasiado largo"
                    item.id != 0 || item.version != 0 -> "ID y versión son asignados por el servidor"
                    else -> null
                }
                if (error != null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to error))
                    return@safeApiCall
                }
                val result = DatabaseFactory.dbQuery {
                    val existing = EhsRatePeriodTable.selectAll().where { EhsRatePeriodTable.periodo eq item.periodo }.map(::rateFrom)
                    if (existing.isNotEmpty() && item.motivoRevision.isBlank()) null else {
                        val record = EhsRatePeriodTable.insert {
                            it[periodo] = item.periodo
                            it[version] = (existing.maxOfOrNull { rate -> rate.version } ?: 0) + 1
                            it[horasTrabajadas] = item.horasTrabajadas
                            it[accidentesRegistrables] = item.accidentesRegistrables
                            it[diasPerdidos] = item.diasPerdidos
                            it[fuenteHoras] = item.fuenteHoras.trim()
                            it[validadoPor] = item.validadoPor.trim()
                            it[motivoRevision] = item.motivoRevision.trim()
                        }
                        rateFrom(record.resultedValues!!.first())
                    }
                }
                if (result == null) call.respond(HttpStatusCode.Conflict, mapOf("error" to "Periodo existente: indica motivo de revisión para conservar ambas versiones"))
                else call.respond(HttpStatusCode.Created, result)
            }
        }

        // ---------- QOS DE ACCIDENTES EN EXCEL (.xlsx) ----------
        // Reporte descargable con el formato del QOS de la empresa: encabezado
        // (site/owner/frecuencia), IFR anual y tabla mensual de horas,
        // accidentes, dias perdidos e IFR. Usa la version vigente de cada mes.
        get("/qos/xlsx") {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.EHS_WRITE) ?: return@safeApiCall
                val anio = call.request.queryParameters["anio"]?.trim()?.takeIf { it.isNotBlank() } ?: "2026"
                val meses = listOf("Enero","Febrero","Marzo","Abril","Mayo","Junio","Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre")
                val vigentes = DatabaseFactory.dbQuery { EhsRatePeriodTable.selectAll().map(::rateFrom) }
                    .groupBy { it.periodo }.values.map { it.maxBy { e -> e.version } }
                val t = vigentes.filter { it.periodo.startsWith("$anio-") }.sortedBy { it.periodo }
                val hTot = t.sumOf { it.horasTrabajadas }
                val aTot = t.sumOf { it.accidentesRegistrables }
                val dTot = t.sumOf { it.diasPerdidos }
                val ifrAnual = if (hTot > 0) aTot * 200_000.0 / hTot else 0.0
                fun r2(x: Double) = kotlin.math.round(x * 100.0) / 100.0
                org.apache.poi.xssf.usermodel.XSSFWorkbook().use { wb ->
                    val hoja = wb.createSheet("QOS Accidents")
                    fun texto(r: Int, c: Int, v: String, negrita: Boolean = false) {
                        // getRow ?: createRow: createRow borra la fila existente y
                        // con varias escrituras en la misma fila se pisaban.
                        val cell = (hoja.getRow(r) ?: hoja.createRow(r)).createCell(c)
                        cell.setCellValue(v)
                        if (negrita) cell.cellStyle = wb.createCellStyle().apply { setFont(wb.createFont().apply { bold = true }) }
                    }
                    texto(0, 0, "SAFETY / SEGURIDAD INDUSTRIAL", true)
                    texto(1, 0, "QOS Title: Accidents — Indice de Frecuencia (IFR) $anio", true)
                    texto(2, 0, "SITE: Escobedo"); texto(2, 2, "OWNER: Dario Robles"); texto(2, 5, "FREQUENCY: Mensual")
                    texto(3, 0, "IFR ANUAL $anio: ${r2(ifrAnual)}", true)
                    texto(3, 2, "Horas trabajadas: ${r2(hTot)}")
                    texto(3, 4, "Accidentes: $aTot")
                    val hdr = hoja.createRow(5)
                    listOf("MES","HORAS TRABAJADAS","ACCIDENTES","DIAS PERDIDOS","IFR MENSUAL").forEachIndexed { i, h ->
                        val c = hdr.createCell(i); c.setCellValue(h)
                        c.cellStyle = wb.createCellStyle().apply { setFont(wb.createFont().apply { bold = true }) }
                    }
                    meses.forEachIndexed { i, nombre ->
                        val per = t.firstOrNull { it.periodo == "$anio-" + "%02d".format(i + 1) }
                        val row = hoja.createRow(6 + i)
                        row.createCell(0).setCellValue(nombre)
                        if (per != null) {
                            row.createCell(1).setCellValue(per.horasTrabajadas)
                            row.createCell(2).setCellValue(per.accidentesRegistrables.toDouble())
                            row.createCell(3).setCellValue(per.diasPerdidos.toDouble())
                            row.createCell(4).setCellValue(r2(if (per.horasTrabajadas > 0) per.accidentesRegistrables * 200_000.0 / per.horasTrabajadas else 0.0))
                        } else {
                            (1..4).forEach { row.createCell(it).setCellValue("—") }
                        }
                    }
                    val tot = hoja.createRow(18)
                    tot.createCell(0).setCellValue("TOTAL $anio")
                    tot.createCell(1).setCellValue(hTot)
                    tot.createCell(2).setCellValue(aTot.toDouble())
                    tot.createCell(3).setCellValue(dTot.toDouble())
                    tot.createCell(4).setCellValue(r2(ifrAnual))
                    (0..4).forEach { hoja.autoSizeColumn(it) }
                    val out = java.io.ByteArrayOutputStream()
                    wb.write(out)
                    call.response.headers.append(HttpHeaders.ContentDisposition, "attachment; filename=\"qos_accidents_${anio}.xlsx\"")
                    call.respondBytes(out.toByteArray(), ContentType.parse("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                }
            }
        }
    }
}
