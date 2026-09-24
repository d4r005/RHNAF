package com.example.rhnaf.services

import jakarta.mail.Authenticator
import jakarta.mail.Message
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import java.util.Properties

/**
 * Enviador de correo SMTP reutilizable para los recordatorios de vencimiento
 * EHS. Configuración por variables de entorno (secrets del Space):
 *
 *   SMTP_HOST     p.ej. smtp.gmail.com, smtp-brevo.com, smtp.office365.com
 *   SMTP_PORT     587 (STARTTLS) o 465 (SSL)
 *   SMTP_USER     usuario de la cuenta SMTP
 *   SMTP_PASS     contraseña o contraseña de aplicación
 *   SMTP_FROM     remitente visible (opcional, por defecto SMTP_USER)
 *   SMTP_SSL      "true" para forzar SSL en vez de STARTTLS (465)
 *   REMINDER_TO   destinatarios adicionales fijos, separados por coma
 *                 (p.ej. el responsable general de EHS), con copia de todo
 */
object EhsReminderMailer {

    fun isConfigured(): Boolean = !System.getenv("SMTP_HOST").isNullOrBlank()

    fun fromAddress(): String = System.getenv("SMTP_FROM") ?: (System.getenv("SMTP_USER") ?: "recordatorios@rhna.local")

    /** Destinatarios fijos configurados vía REMINDER_TO (copia de todo). */
    fun extraRecipients(): List<String> =
        System.getenv("REMINDER_TO").orEmpty().split(',', ';').map { it.trim() }.filter { it.contains("@") }

    /** Envía un correo HTML. Devuelve true solo si el SMTP aceptó el mensaje. */
    fun send(to: List<String>, subject: String, html: String): Boolean {
        val destinatarios = to.filter { it.isNotBlank() && it.contains("@") }.distinct()
        if (destinatarios.isEmpty()) return false
        val host = System.getenv("SMTP_HOST") ?: return false
        val port = System.getenv("SMTP_PORT") ?: "587"
        val user = System.getenv("SMTP_USER").orEmpty()
        val pass = System.getenv("SMTP_PASS").orEmpty()
        val useSsl = System.getenv("SMTP_SSL")?.equals("true", ignoreCase = true) ?: (port == "465")
        return try {
            val props = Properties().apply {
                put("mail.smtp.host", host)
                put("mail.smtp.port", port)
                put("mail.smtp.auth", user.isNotBlank().toString())
                put("mail.smtp.connectiontimeout", "15000")
                put("mail.smtp.timeout", "30000")
                put("mail.smtp.writetimeout", "30000")
                if (useSsl) {
                    put("mail.smtp.ssl.enable", "true")
                    put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3")
                } else {
                    put("mail.smtp.starttls.enable", "true")
                    put("mail.smtp.starttls.required", "true")
                }
            }
            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication() = PasswordAuthentication(user, pass)
            })
            val msg = MimeMessage(session).apply {
                setFrom(InternetAddress(fromAddress()))
                setRecipients(Message.RecipientType.TO, destinatarios.joinToString(", "))
                this.subject = subject
                setContent(html, "text/html; charset=utf-8")
            }
            Transport.send(msg)
            true
        } catch (e: Exception) {
            println("[EhsReminderMailer] No se pudo enviar el correo a $destinatarios: ${e.message}")
            false
        }
    }
}
