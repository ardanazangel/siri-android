package dev.nova.assistant

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.json.JSONObject
import java.net.ServerSocket
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID

/**
 * Experimental OSS Sign in with ChatGPT registration.
 * Tokens are deliberately kept in memory until secure persistent storage is implemented.
 */
class ChatGptSignIn(private val context: Context) {
    var accessToken: String? = null
        private set
    var clientId: String? = null
        private set

    fun signIn(onResult: (String) -> Unit) {
        Thread {
            var server: ServerSocket? = null
            try {
                server = ServerSocket(0, 1, java.net.InetAddress.getByName("127.0.0.1"))
                server.soTimeout = 120000
                val redirect = "http://127.0.0.1:${server.localPort}/auth/callback"
                val state = randomUrlSafe()
                val nonce = randomUrlSafe()
                val verifier = randomUrlSafe()
                val challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(
                    MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
                )
                val hostId = context.getSharedPreferences("nova_auth", Context.MODE_PRIVATE)
                    .let { prefs ->
                        prefs.getString("host_id", null) ?: ("urn:uuid:" + UUID.randomUUID()).also {
                            prefs.edit().putString("host_id", it).apply()
                        }
                    }
                val params = linkedMapOf(
                    "client_id" to "dynamic_agent_client",
                    "agent_name_hint" to "Nova",
                    "ext_agent_host_id" to hostId,
                    "response_type" to "code",
                    "redirect_uri" to redirect,
                    "scope" to "openid profile email offline_access resource.invoke chatgpt.tokens.use.direct",
                    "resource" to "https://api.openai.com/v1",
                    "state" to state,
                    "nonce" to nonce,
                    "code_challenge_method" to "S256",
                    "code_challenge" to challenge
                )
                val uri = Uri.parse("https://auth.openai.com/api/accounts/authorize").buildUpon().apply {
                    params.forEach { (key, value) -> appendQueryParameter(key, value) }
                }.build()
                context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                val socket = server.accept()
                val requestLine = socket.getInputStream().bufferedReader().readLine() ?: error("Callback vacío")
                val target = requestLine.split(" ").getOrNull(1) ?: error("Callback inválido")
                val callback = Uri.parse("http://127.0.0.1${target}")
                val valid = callback.path == "/auth/callback" && callback.getQueryParameter("state") == state
                val body = if (valid) "Puedes volver a Nova." else "Autorización no válida."
                val response = "HTTP/1.1 200 OK\r\nContent-Type: text/plain; charset=utf-8\r\nContent-Length: ${body.toByteArray().size}\r\nConnection: close\r\n\r\n${body}"
                socket.getOutputStream().write(response.toByteArray())
                socket.close()
                if (!valid) error("El estado OAuth no coincide")
                callback.getQueryParameter("error")?.let { error("OpenAI: $it") }
                val code = callback.getQueryParameter("code") ?: error("Falta el código OAuth")
                val issuedId = callback.getQueryParameter("client_id") ?: error("Falta el client_id emitido")
                val form = listOf(
                    "grant_type" to "authorization_code", "client_id" to issuedId,
                    "code" to code, "code_verifier" to verifier,
                    "redirect_uri" to redirect, "resource" to "https://api.openai.com/v1"
                ).joinToString("&") { (k, v) ->
                    java.net.URLEncoder.encode(k, "UTF-8") + "=" + java.net.URLEncoder.encode(v, "UTF-8")
                }
                val connection = URL("https://auth.openai.com/api/accounts/oauth/token").openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                connection.outputStream.use { it.write(form.toByteArray()) }
                val status = connection.responseCode
                val raw = (if (status in 200..299) connection.inputStream else connection.errorStream)
                    .bufferedReader().use { it.readText() }
                if (status !in 200..299) error("Intercambio OAuth HTTP $status: ${raw.take(200)}")
                val tokens = JSONObject(raw)
                val scopes = tokens.optString("scope")
                if (!scopes.split(" ").contains("chatgpt.tokens.use.direct")) {
                    error("Sesión iniciada, pero no se autorizó el uso del plan ChatGPT")
                }
                // Do not expose tokens until ID-token validation has been implemented.
                onResult("OAuth autorizado. Falta validar el ID token antes de activar la conexión.")
            } catch (e: Exception) {
                onResult("Error de conexión: ${e.message}")
            } finally {
                server?.close()
            }
        }.start()
    }

    private fun randomUrlSafe(): String = ByteArray(32).also { SecureRandom().nextBytes(it) }
        .let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) }
}
