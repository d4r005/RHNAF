package com.example.rhnaf.service

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.ConcurrentHashMap
import java.io.File
import java.io.FileInputStream

/**
 * Integracion con Google Drive para reemplazar el almacenamiento de archivos
 * (evidencia documental EHS, y a futuro fotos de empleado) como base64 dentro
 * de Postgres/Supabase -- eso fue lo que agoto la cuota de espacio y puso la
 * base en modo solo-lectura.
 *
 * Flujo de autorizacion (una sola vez, la hace un ADMIN desde el navegador):
 *  1. GET /api/v1/google-drive/auth-url  -> redirige a la pantalla de consentimiento de Google
 *  2. Google redirige de vuelta a /api/v1/google-drive/callback?code=...
 *  3. Intercambiamos el code por access_token + refresh_token
 *  4. Creamos (o encontramos) la carpeta "NAF Connect/Evidencia Documental"
 *  5. Se muestran refresh_token y folder id para guardarlos como Secrets en
 *     Hugging Face (GOOGLE_REFRESH_TOKEN y GOOGLE_DRIVE_FOLDER_ID) y reiniciar el Space.
 *
 * De ahi en adelante este servicio usa el refresh_token (via variable de entorno)
 * para pedir access_tokens nuevos cuando hacen falta -- no hay que re-autorizar
 * a mano de nuevo salvo que se revoque el acceso desde la cuenta de Google.
 */
object GoogleDriveService {
    private const val TOKEN_URL = "https://oauth2.googleapis.com/token"
    private const val AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth"
    private const val DRIVE_FILES_URL = "https://www.googleapis.com/drive/v3/files"
    private const val DRIVE_UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files"
    private const val SCOPE = "https://www.googleapis.com/auth/drive.file"

    private val client = HttpClient(CIO)

    private val clientId get() = System.getenv("GOOGLE_CLIENT_ID")
    private val clientSecret get() = System.getenv("GOOGLE_CLIENT_SECRET")
    private val refreshTokenEnv get() = System.getenv("GOOGLE_REFRESH_TOKEN")
    val folderId get() = System.getenv("GOOGLE_DRIVE_FOLDER_ID")

    fun redirectUri(baseUrl: String) = "$baseUrl/api/v1/google-drive/callback"

    fun hasOAuthCredentials(): Boolean = !clientId.isNullOrBlank() && !clientSecret.isNullOrBlank()

    fun isConfigured(): Boolean =
        hasOAuthCredentials() && !refreshTokenEnv.isNullOrBlank() && !folderId.isNullOrBlank()

    fun buildAuthUrl(baseUrl: String): String {
        val redirect = redirectUri(baseUrl)
        return "$AUTH_URL?" + listOf(
            "client_id=$clientId",
            "redirect_uri=${redirect.encodeURLParameter()}",
            "response_type=code",
            "scope=${SCOPE.encodeURLParameter()}",
            "access_type=offline",
            "prompt=consent"
        ).joinToString("&")
    }

    // --- Cache de access_token en memoria (dura ~1h, se refresca solo) ---
    private data class CachedToken(val token: String, val expiresAtMillis: Long)
    private val cached = AtomicReference<CachedToken?>(null)

    suspend fun getAccessToken(): String? {
        val rt = refreshTokenEnv
        val cid = clientId
        val secret = clientSecret
        if (rt.isNullOrBlank() || cid.isNullOrBlank() || secret.isNullOrBlank()) return null

        cached.get()?.let { if (it.expiresAtMillis > System.currentTimeMillis() + 30_000) return it.token }

        val response: HttpResponse = client.post(TOKEN_URL) {
            setBody(FormDataContent(Parameters.build {
                append("client_id", cid)
                append("client_secret", secret)
                append("refresh_token", rt)
                append("grant_type", "refresh_token")
            }))
        }
        if (response.status != HttpStatusCode.OK) {
            println("[GoogleDriveService] Error refrescando token: ${response.status} ${response.bodyAsText()}")
            return null
        }
        val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val accessToken = json["access_token"]?.jsonPrimitive?.content ?: return null
        val expiresIn = json["expires_in"]?.jsonPrimitive?.longOrNull ?: 3600L
        cached.set(CachedToken(accessToken, System.currentTimeMillis() + expiresIn * 1000))
        return accessToken
    }

    /** Intercambia el codigo de autorizacion por tokens. Solo se usa una vez, en el callback. */
    suspend fun exchangeCode(code: String, baseUrl: String): Map<String, String>? {
        val cid = clientId ?: return null
        val secret = clientSecret ?: return null
        val response: HttpResponse = client.post(TOKEN_URL) {
            setBody(FormDataContent(Parameters.build {
                append("client_id", cid)
                append("client_secret", secret)
                append("code", code)
                append("grant_type", "authorization_code")
                append("redirect_uri", redirectUri(baseUrl))
            }))
        }
        if (response.status != HttpStatusCode.OK) {
            println("[GoogleDriveService] Error intercambiando code: ${response.status} ${response.bodyAsText()}")
            return null
        }
        val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val result = mutableMapOf<String, String>()
        json["access_token"]?.jsonPrimitive?.content?.let { result["access_token"] = it }
        json["refresh_token"]?.jsonPrimitive?.content?.let { result["refresh_token"] = it }
        return result
    }

    /** Busca una carpeta por nombre dentro de parentId (o raiz si null). La crea si no existe. */
    suspend fun findOrCreateFolder(name: String, parentId: String?, accessToken: String): String? {
        val safeName = name.replace("'", "\\'")
        val q = buildString {
            append("mimeType='application/vnd.google-apps.folder' and trashed=false and name='$safeName'")
            if (parentId != null) append(" and '$parentId' in parents")
        }
        val searchResp: HttpResponse = client.get(DRIVE_FILES_URL) {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            parameter("q", q)
            parameter("fields", "files(id,name)")
        }
        if (searchResp.status == HttpStatusCode.OK) {
            val json = Json.parseToJsonElement(searchResp.bodyAsText()).jsonObject
            val files = json["files"]?.jsonArray
            if (!files.isNullOrEmpty()) {
                return files[0].jsonObject["id"]?.jsonPrimitive?.content
            }
        } else {
            println("[GoogleDriveService] Error buscando carpeta '$name': ${searchResp.status} ${searchResp.bodyAsText()}")
            return null // No crear duplicados cuando la búsqueda falla por permisos o cuota.
        }

        val metadata = buildJsonObject {
            put("name", name)
            put("mimeType", "application/vnd.google-apps.folder")
            if (parentId != null) putJsonArray("parents") { add(parentId) }
        }
        val createResp: HttpResponse = client.post(DRIVE_FILES_URL) {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            header(HttpHeaders.ContentType, "application/json")
            setBody(metadata.toString())
        }
        if (createResp.status != HttpStatusCode.OK) {
            println("[GoogleDriveService] Error creando carpeta '$name': ${createResp.status} ${createResp.bodyAsText()}")
            return null
        }
        val json = Json.parseToJsonElement(createResp.bodyAsText()).jsonObject
        return json["id"]?.jsonPrimitive?.content
    }

    /** Carpeta <Categoria>/AAAA o <Categoria>/General para archivos sin año.
     * Cada categoria (Inspeccion, Simulacro, Capacitacion, Dictamen, etc.) tiene
     * su propia carpeta de nivel superior en vez de agruparse todo bajo Normativa. */
    private val categoryYearFolders = ConcurrentHashMap<Pair<String, Int>, String>()

    private fun sanitizeCategoryFolderName(categoria: String): String {
        val safe = categoria.trim()
        return if (safe.isBlank()) "Otro" else safe
    }

    suspend fun categoryYearFolder(categoria: String, year: Int): String? {
        if (year != -1 && year !in 1900..2100) return null
        val categoryFolderName = sanitizeCategoryFolderName(categoria)
        val key = categoryFolderName to year
        categoryYearFolders[key]?.let { return it }
        val token = getAccessToken() ?: return null
        val root = folderId ?: return null
        val categoryFolder = findOrCreateFolder(categoryFolderName, root, token) ?: return null
        val folder = findOrCreateFolder(if (year == -1) "General" else year.toString(), categoryFolder, token) ?: return null
        categoryYearFolders[key] = folder
        return folder
    }

    /** Compatibilidad retro: llamadas antiguas que no pasan categoria van a "Otro". */
    suspend fun normativeYearFolder(year: Int): String? = categoryYearFolder("Otro", year)

    /** Subida reanudable a Drive en fragmentos de 8 MiB, sin duplicar todo el archivo en RAM. */
    suspend fun uploadLargeFile(file: File, fileName: String, mimeType: String, targetFolderId: String): String? {
        val token = getAccessToken() ?: return null
        val type = mimeType.ifBlank { "application/octet-stream" }
        val metadata = buildJsonObject {
            put("name", fileName)
            putJsonArray("parents") { add(targetFolderId) }
        }.toString()
        val start: HttpResponse = client.post(DRIVE_UPLOAD_URL) {
            header(HttpHeaders.Authorization, "Bearer $token")
            header("X-Upload-Content-Type", type)
            header("X-Upload-Content-Length", file.length().toString())
            contentType(ContentType.Application.Json)
            parameter("uploadType", "resumable")
            parameter("fields", "id")
            setBody(metadata)
        }
        val session = start.headers[HttpHeaders.Location]
        if (start.status != HttpStatusCode.OK || session.isNullOrBlank() ||
            !session.startsWith("https://www.googleapis.com/upload/drive/v3/files")) {
            println("[GoogleDriveService] No se pudo iniciar la subida reanudable: ${start.status}")
            return null
        }
        val total = file.length()
        val chunk = ByteArray(8 * 1024 * 1024)
        var offset = 0L
        FileInputStream(file).use { input ->
            while (offset < total) {
                var count = 0
                val expected = minOf(chunk.size.toLong(), total - offset).toInt()
                while (count < expected) {
                    val n = input.read(chunk, count, expected - count)
                    if (n < 0) return null
                    count += n
                }
                val response: HttpResponse = client.put(session) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    header(HttpHeaders.ContentType, type)
                    header(HttpHeaders.ContentRange, "bytes $offset-${offset + count - 1}/$total")
                    setBody(if (count == chunk.size) chunk else chunk.copyOf(count))
                }
                offset += count
                if (offset < total) {
                    if (response.status.value != 308) {
                        println("[GoogleDriveService] Error en fragmento $offset/$total: ${response.status}")
                        return null
                    }
                } else {
                    if (response.status != HttpStatusCode.OK && response.status != HttpStatusCode.Created) {
                        println("[GoogleDriveService] Error finalizando subida: ${response.status}")
                        return null
                    }
                    return Json.parseToJsonElement(response.bodyAsText()).jsonObject["id"]?.jsonPrimitive?.content
                }
            }
        }
        return null
    }

    /** Sube un archivo a la carpeta indicada. Devuelve el fileId de Drive, o null si fallo. */
    suspend fun uploadFile(bytes: ByteArray, fileName: String, mimeType: String, targetFolderId: String): String? {
        val accessToken = getAccessToken() ?: return null
        val boundary = "rhnaf_boundary_${System.currentTimeMillis()}"
        val metadata = buildJsonObject {
            put("name", fileName)
            putJsonArray("parents") { add(targetFolderId) }
        }.toString()

        val head = buildString {
            append("--$boundary\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            append(metadata)
            append("\r\n--$boundary\r\n")
            append("Content-Type: ${mimeType.ifBlank { "application/octet-stream" }}\r\n\r\n")
        }.toByteArray(Charsets.UTF_8)
        val tail = "\r\n--$boundary--".toByteArray(Charsets.UTF_8)
        val body = head + bytes + tail

        val response: HttpResponse = client.post(DRIVE_UPLOAD_URL) {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            header(HttpHeaders.ContentType, "multipart/related; boundary=$boundary")
            parameter("uploadType", "multipart")
            parameter("fields", "id")
            setBody(body)
        }
        if (response.status != HttpStatusCode.OK) {
            println("[GoogleDriveService] Error subiendo '$fileName': ${response.status} ${response.bodyAsText()}")
            return null
        }
        val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        return json["id"]?.jsonPrimitive?.content
    }

    /** Descarga el contenido binario de un archivo por su fileId. Null si fallo. */
    suspend fun downloadFile(fileId: String): ByteArray? {
        val accessToken = getAccessToken() ?: return null
        val response: HttpResponse = client.get("$DRIVE_FILES_URL/$fileId") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            parameter("alt", "media")
        }
        if (response.status != HttpStatusCode.OK) {
            println("[GoogleDriveService] Error descargando fileId=$fileId: ${response.status}")
            return null
        }
        return response.readBytes()
    }

    /** Elimina un archivo de Drive por su fileId. */
    suspend fun deleteFile(fileId: String): Boolean {
        val accessToken = getAccessToken() ?: return false
        val response: HttpResponse = client.delete("$DRIVE_FILES_URL/$fileId") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }
        return response.status == HttpStatusCode.NoContent || response.status == HttpStatusCode.OK
    }
}
