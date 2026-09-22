package com.example.rhnaf.routes

import com.example.rhnaf.service.GoogleDriveService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.origin
import io.ktor.server.response.*
import io.ktor.server.routing.*

private fun ApplicationCall.publicBaseUrl(): String {
    val forwardedProto = request.headers["X-Forwarded-Proto"]?.substringBefore(',')?.trim()
    val forwardedHost = request.headers["X-Forwarded-Host"]?.substringBefore(',')?.trim()
    if (!forwardedProto.isNullOrBlank() && !forwardedHost.isNullOrBlank()) {
        return "$forwardedProto://$forwardedHost"
    }
    val origin = request.origin
    val defaultPort = (origin.scheme == "https" && origin.serverPort == 443) ||
        (origin.scheme == "http" && origin.serverPort == 80)
    return if (defaultPort) "${origin.scheme}://${origin.serverHost}"
    else "${origin.scheme}://${origin.serverHost}:${origin.serverPort}"
}

fun Route.googleDriveRouting() {
    route("/api/v1/google-drive") {
        get("/auth-url") {
            if (!GoogleDriveService.hasOAuthCredentials()) {
                call.respond(
                    HttpStatusCode.ServiceUnavailable,
                    mapOf("status" to "error", "message" to "GOOGLE_CLIENT_ID y GOOGLE_CLIENT_SECRET no estan configurados")
                )
                return@get
            }
            call.respondRedirect(GoogleDriveService.buildAuthUrl(call.publicBaseUrl()))
        }

        get("/callback") {
            val googleError = call.request.queryParameters["error"]
            if (!googleError.isNullOrBlank()) {
                call.respondText(
                    setupPage("Autorizacion cancelada", "Google devolvio: ${googleError.escapeHtml()}"),
                    ContentType.Text.Html,
                    HttpStatusCode.BadRequest
                )
                return@get
            }
            val code = call.request.queryParameters["code"]
            if (code.isNullOrBlank()) {
                call.respondText(
                    setupPage("Codigo faltante", "Reinicia la autorizacion desde /api/v1/google-drive/auth-url."),
                    ContentType.Text.Html,
                    HttpStatusCode.BadRequest
                )
                return@get
            }

            val tokens = GoogleDriveService.exchangeCode(code, call.publicBaseUrl())
            val accessToken = tokens?.get("access_token")
            if (accessToken.isNullOrBlank()) {
                call.respondText(
                    setupPage("No se pudo conectar", "No fue posible intercambiar el codigo de Google. Revisa los logs del servidor."),
                    ContentType.Text.Html,
                    HttpStatusCode.BadGateway
                )
                return@get
            }

            val nafFolder = GoogleDriveService.findOrCreateFolder("NAF Connect", null, accessToken)
            val evidenceFolder = nafFolder?.let {
                GoogleDriveService.findOrCreateFolder("Evidencia Documental", it, accessToken)
            }
            val refreshToken = tokens["refresh_token"]
            val body = buildString {
                append("<p>La cuenta y la carpeta de Google Drive quedaron autorizadas.</p>")
                if (refreshToken.isNullOrBlank()) {
                    append("<p class='error'>Google no devolvio un refresh token. Revoca el acceso de NAF Connect en tu cuenta de Google y autoriza nuevamente.</p>")
                } else {
                    append("<p>Guarda este valor como secreto <b>GOOGLE_REFRESH_TOKEN</b> en Hugging Face:</p>")
                    append("<pre>${refreshToken.escapeHtml()}</pre>")
                }
                if (evidenceFolder.isNullOrBlank()) {
                    append("<p class='error'>No se pudo crear o localizar la carpeta Evidencia Documental.</p>")
                } else {
                    append("<p>Guarda este valor como variable <b>GOOGLE_DRIVE_FOLDER_ID</b>:</p>")
                    append("<pre>${evidenceFolder.escapeHtml()}</pre>")
                }
                append("<p>Despues reinicia el Space. Estos valores solo se muestran en esta pantalla.</p>")
            }
            call.respondText(setupPage("Google Drive conectado", body), ContentType.Text.Html)
        }

        get("/status") {
            call.respond(
                mapOf(
                    "configured" to GoogleDriveService.isConfigured(),
                    "oauthCredentials" to GoogleDriveService.hasOAuthCredentials(),
                    "folderConfigured" to !GoogleDriveService.folderId.isNullOrBlank()
                )
            )
        }
    }
}

private fun setupPage(title: String, body: String): String = """
<!doctype html><html lang="es"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>$title</title><style>body{font-family:Arial,sans-serif;max-width:760px;margin:48px auto;padding:0 20px;line-height:1.5;color:#222}pre{padding:14px;background:#f2f2f2;border-radius:8px;white-space:pre-wrap;word-break:break-all}.error{color:#a00}</style></head>
<body><h1>$title</h1>$body</body></html>
""".trimIndent()

private fun String.escapeHtml(): String = replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
    .replace("'", "&#39;")
