package com.example.rhnaf.routes

import com.example.rhnaf.auth.Roles
import com.example.rhnaf.auth.requireRoleOr403
import com.example.rhnaf.database.DatabaseFactory
import com.example.rhnaf.database.EhsActionTable
import com.example.rhnaf.database.EhsReminderLogTable
import com.example.rhnaf.database.LegalMatrixDocTable
import com.example.rhnaf.database.LegalMatrixTable
import com.example.rhnaf.services.EhsReminderMailer
import com.example.rhnaf.shared.model.EhsReminderSummary
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * EHS - Recordatorios de vencimiento por correo (estilo EHSoft).
 *
 * Concentra los avisos de la obligaciones de la matriz legal y de cada
 * documento de cumplimiento (con su propio plazo de recordatorio), agrupa
 * por destinatario y envía un digest por correo. La bitácora
 * ehs_reminder_log evita reenviar el mismo aviso el mismo día.
 */

data class AvisoVencimiento(
    val tipo: String,           // "obligacion" | "documento"
    val refId: Int,             // id de la obligación o del doc de cumplimiento
    val matrizId: Int,
    val clave: String,
    val titulo: String,
    val fechaVigencia: LocalDate,
    val diasRestantes: Long,    // negativo = vencido
    val esCritico: Boolean,
    val recordatorioDias: Int,
    val destinatario: String
)

/** Recolecta todos los avisos vigentes: obligaciones y documentos con vencimiento próximo o vencido. */
suspend fun recolectarAvisos(hoy: LocalDate): List<AvisoVencimiento> {
    val avisos = mutableListOf<AvisoVencimiento>()
    val obligaciones = DatabaseFactory.dbQuery {
        LegalMatrixTable.selectAll().where { LegalMatrixTable.aplica eq "Si" }.toList()
    }
    obligaciones.forEach { row ->
        val email = row[LegalMatrixTable.responsableEmail].trim()
        if (email.contains("@")) {
            val v = parseFechaCorreo(row[LegalMatrixTable.fechaVigencia])
            if (v != null) {
                val dias = Duration.between(hoy.atStartOfDay(), v.atStartOfDay()).toDays()
                val alertaPrevia = row[LegalMatrixTable.diasAlertaPrevia].toLong()
                if (dias <= alertaPrevia) {
                    avisos.add(
                        AvisoVencimiento(
                            "obligacion", row[LegalMatrixTable.id], row[LegalMatrixTable.id],
                            row[LegalMatrixTable.clave], row[LegalMatrixTable.titulo], v, dias,
                            row[LegalMatrixTable.esCritico], row[LegalMatrixTable.diasAlertaPrevia], email
                        )
                    )
                }
            }
        }
    }
    // Documentos de cumplimiento: cada archivo tiene su propia vigencia y plazo.
    DatabaseFactory.dbQuery {
        LegalMatrixDocTable.selectAll().toList()
    }.forEach { drow ->
        val v = parseFechaCorreo(drow[LegalMatrixDocTable.fechaVigencia]) ?: return@forEach
        val dias = Duration.between(hoy.atStartOfDay(), v.atStartOfDay()).toDays()
        val recordatorio = drow[LegalMatrixDocTable.recordatorioDias].toLong()
        if (dias in 0..recordatorio || dias < 0) {
            val obligacion = obligaciones.firstOrNull { it[LegalMatrixTable.id] == drow[LegalMatrixDocTable.matrizId] }
            val email = obligacion?.get(LegalMatrixTable.responsableEmail)?.trim().orEmpty()
            if (email.contains("@")) {
                avisos.add(
                    AvisoVencimiento(
                        "documento", drow[LegalMatrixDocTable.id], drow[LegalMatrixDocTable.matrizId],
                        obligacion?.get(LegalMatrixTable.clave) ?: "Obligación ${drow[LegalMatrixDocTable.matrizId]}",
                        drow[LegalMatrixDocTable.nombre].ifBlank { drow[LegalMatrixDocTable.tipoDocumento] },
                        v, dias, obligacion?.get(LegalMatrixTable.esCritico) ?: false,
                        drow[LegalMatrixDocTable.recordatorioDias], email
                    )
                )
            }
        }
    }
    return avisos.sortedWith(compareBy<AvisoVencimiento> { it.fechaVigencia }.thenBy { it.clave })
}

private fun parseFechaCorreo(s: String): LocalDate? {
    val t = s.trim()
    if (t.isBlank()) return null
    return try { LocalDate.parse(t.substring(0, 10)) } catch (e: Exception) { null }
}

private fun htmlEscape(t: String) = t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

/** Construye el cuerpo HTML del digest de recordatorios para un destinatario. */
private fun construirHtml(avisos: List<AvisoVencimiento>): String {
    val filas = avisos.joinToString("") { a ->
        val diasTxt = if (a.diasRestantes < 0) "<b style=\"color:#b91c1c\">Vencido hace ${-a.diasRestantes} día(s)</b>"
        else if (a.diasRestantes == 0L) "<b style=\"color:#b91c1c\">Vence hoy</b>"
        else "<b style=\"color:#b45309\">Faltan ${a.diasRestantes} día(s)</b>"
        val critico = if (a.esCritico) " <span style=\"color:#b91c1c\">⚠ CRÍTICO</span>" else ""
        val tipo = if (a.tipo == "documento") "Documento de cumplimiento" else "Obligación"
        """
        <tr style="border-bottom:1px solid #e5e7eb;">
          <td style="padding:8px 10px;font-size:13px;color:#334155;">$tipo</td>
          <td style="padding:8px 10px;font-size:13px;"><b>${htmlEscape(a.clave)}</b> — ${htmlEscape(a.titulo)}$critico</td>
          <td style="padding:8px 10px;font-size:13px;white-space:nowrap;">${a.fechaVigencia}</td>
          <td style="padding:8px 10px;font-size:13px;white-space:nowrap;">$diasTxt</td>
        </tr>
        """.trimIndent()
    }
    return """
    <html><body style="font-family:Arial,Helvetica,sans-serif;color:#0f172a;">
      <h2 style="margin:0 0 4px;">Recordatorios de cumplimiento EHS</h2>
      <p style="color:#64748b;font-size:13px;margin:0 0 16px;">
        Obligaciones y documentos con vencimiento próximo o vencido. Entra al módulo
        Matriz Legal de RH-NAF para actualizar la evidencia.
      </p>
      <table style="border-collapse:collapse;width:100%;max-width:720px;border:1px solid #e2e8f0;">
        <tr style="background:#f8fafc;">
          <th align="left" style="padding:8px 10px;font-size:12px;color:#475569;">Tipo</th>
          <th align="left" style="padding:8px 10px;font-size:12px;color:#475569;">Obligación / Documento</th>
          <th align="left" style="padding:8px 10px;font-size:12px;color:#475569;">Vence</th>
          <th align="left" style="padding:8px 10px;font-size:12px;color:#475569;">Estatus</th>
        </tr>
        $filas
      </table>
      <p style="color:#94a3b8;font-size:11px;margin-top:16px;">Correo automático de RH-NAF · Matriz Legal EHS</p>
    </body></html>
    """.trimIndent()
}

/** Envía los recordatorios pendientes del día (usado por el job diario y por el botón manual). */
suspend fun enviarRecordatorios(): EhsReminderSummary {
    if (!EhsReminderMailer.isConfigured()) {
        return EhsReminderSummary(
            smtpConfigurado = false,
            mensaje = "SMTP no configurado. Define SMTP_HOST, SMTP_PORT, SMTP_USER y SMTP_PASS como secrets del Space."
        )
    }
    val hoy = LocalDate.now()
    val todos = recolectarAvisos(hoy)
    val yaEnviados = DatabaseFactory.dbQuery {
        EhsReminderLogTable.selectAll().where { EhsReminderLogTable.enviado eq hoy.toString() }
            .map { "${it[EhsReminderLogTable.tipo]}:${it[EhsReminderLogTable.refId]}" }
    }.toSet()
    val pendientes = todos.filter { "${it.tipo}:${it.refId}" !in yaEnviados }

    // Digest por destinatario
    val porDestinatario = pendientes.groupBy { it.destinatario }
    var enviados = 0
    val destinatarios = mutableListOf<String>()
    porDestinatario.forEach { (destino, avisos) ->
        val ok = EhsReminderMailer.send(
            listOf(destino),
            "[RH-NAF] Recordatorios EHS: ${avisos.size} aviso(s) de vencimiento",
            construirHtml(avisos)
        )
        if (ok) {
            enviados++
            destinatarios.add(destino)
        }
    }
    // Copia de todo al destinatario fijo (responsable general de EHS).
    val extra = EhsReminderMailer.extraRecipients()
    if (extra.isNotEmpty() && pendientes.isNotEmpty()) {
        EhsReminderMailer.send(extra, "[RH-NAF] Recordatorios EHS (copia general)", construirHtml(pendientes))
    }
    // Bitácora: un aviso por fila, evita reenvíos el mismo día.
    if (pendientes.isNotEmpty()) {
        DatabaseFactory.dbQuery {
            pendientes.forEach { a ->
                EhsReminderLogTable.insert {
                    it[tipo] = a.tipo
                    it[refId] = a.refId
                    it[diasRestantes] = a.diasRestantes.toInt()
                    it[destinatario] = a.destinatario
                    it[enviado] = hoy.toString()
                }
            }
        }
    }
    return EhsReminderSummary(
        smtpConfigurado = true,
        enviados = enviados,
        destinatarios = destinatarios,
        avisosDetectados = todos.size,
        mensaje = if (pendientes.isEmpty()) "Sin recordatorios pendientes hoy (${todos.size} avisos ya enviados previamente)."
        else "Enviados $enviados correo(s) con ${pendientes.size} recordatorio(s)."
    )
}

/** Job diario: dispara los recordatorios cada mañana (06:30 hora del servidor). */
fun Application.recordatoriosJob() {
    launch(Dispatchers.IO) {
        while (true) {
            try {
                val ahora = LocalDateTime.now()
                var proximo = ahora.toLocalDate().atTime(6, 30)
                if (!proximo.isAfter(ahora)) proximo = proximo.plusDays(1)
                delay(Duration.between(ahora, proximo).toMillis())
                val resumen = runCatching { enviarRecordatorios() }
                resumen.onSuccess {
                    if (it.smtpConfigurado && it.enviados > 0)
                        println("[Recordatorios] ${it.enviados} correo(s) enviados: ${it.destinatarios.joinToString()}")
                }
                resumen.onFailure { println("[Recordatorios] Fallo del job diario: ${it.message}") }
            } catch (e: Exception) {
                println("[Recordatorios] Error inesperado en el loop: ${e.message}")
            }
        }
    }
}

fun Route.ehsReminderRouting() {
    route("/api/v1/ehs/recordatorios") {

        // Estado: SMTP configurado y avisos activos ahora mismo
        get("/estado") {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.EHS_WRITE) ?: return@safeApiCall
                val avisos = recolectarAvisos(LocalDate.now())
                call.respond(
                    EhsReminderSummary(
                        smtpConfigurado = EhsReminderMailer.isConfigured(),
                        avisosDetectados = avisos.size,
                        mensaje = if (avisos.isEmpty()) "No hay avisos de vencimiento activos."
                        else "${avisos.count { it.diasRestantes < 0 }} vencido(s) y ${avisos.count { it.diasRestantes >= 0 }} por vencer."
                    )
                )
            }
        }

        // Enviar ahora (mismo motor del job diario; no duplica lo ya enviado hoy)
        post("/enviar") {
            safeApiCall(call) {
                requireRoleOr403(call, Roles.EHS_WRITE) ?: return@safeApiCall
                call.respond(enviarRecordatorios())
            }
        }
    }
}
