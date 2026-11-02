package dev.nova.assistant

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.content.ComponentName
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var status: TextView
    private var tts: TextToSpeech? = null
    private var recognizer: SpeechRecognizer? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(36, 48, 36, 48)
            setBackgroundColor(Color.rgb(14, 16, 20))
        }
        val title = TextView(this).apply {
            text = "nova."
            textSize = 58f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        status = TextView(this).apply {
            text = "Pulsa para hablar"
            textSize = 18f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 28, 0, 36)
        }
        val button = Button(this).apply {
            text = "Hablar"
            setOnClickListener { listen() }
        }
        root.addView(title)
        root.addView(status)
        root.addView(button)
        setContentView(root)
    }
    private fun listen() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 10)
            return
        }
        // Only use an explicitly installed Parakeet-backed RecognitionService.
        // Never silently fall back to Google's cloud-capable recognizer.
        val services = packageManager.queryIntentServices(
            Intent("android.speech.RecognitionService"), PackageManager.MATCH_ALL
        )
        val local = services.firstOrNull {
            it.serviceInfo.packageName.contains("parakeetype", ignoreCase = true)
        }?.serviceInfo
        if (local == null) {
            status.text = "Instala Parakeetype (F-Droid) e importa su modelo Parakeet. Nova no enviará tu voz a Google."
            return
        }
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(
            this, ComponentName(local.packageName, local.name)
        ).also { sr ->
            sr.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { status.text = "Escuchando localmente…" }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { status.text = "Transcribiendo con Parakeet…" }
                override fun onError(error: Int) { status.text = "Error de reconocimiento local: $error. Comprueba que Parakeetype tiene el modelo cargado." }
                override fun onResults(results: Bundle?) {
                    val phrase = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    if (phrase.isNullOrBlank()) { status.text = "No he entendido nada"; return }
                    status.text = "Has dicho: $phrase"
                    execute(phrase.lowercase(Locale("es", "ES")))
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()?.let { status.text = it }
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            status.text = "Preparando Parakeet…"
            sr.startListening(intent)
        }
    }
    private fun execute(command: String) {
        when {
            command.contains("alarma") || command.contains("temporizador") -> {
                val action = if (command.contains("temporizador")) "android.intent.action.SET_TIMER" else "android.intent.action.SET_ALARM"
                launch(Intent(action), "Abriendo alarmas")
            }
            command.startsWith("llama") || command.contains("teléfono") -> launch(Intent(Intent.ACTION_DIAL), "Abriendo el marcador")
            command.contains("ajustes") -> launch(Intent("android.settings.SETTINGS"), "Abriendo ajustes")
            command.contains("cámara") -> launch(Intent("android.media.action.STILL_IMAGE_CAMERA"), "Abriendo cámara")
            else -> respond("Todavía no sé hacer eso. Prueba con abrir cámara, ajustes, llamar o poner una alarma")
        }
    }
    private fun launch(intent: Intent, message: String) {
        try { startActivity(intent); respond(message) }
        catch (_: Exception) { respond("No he podido abrir esa función") }
    }
    private fun respond(message: String) {
        status.text = message
        tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "nova-response")
    }
    override fun onInit(status: Int) { if (status == TextToSpeech.SUCCESS) tts?.language = Locale("es", "ES") }
    override fun onDestroy() { recognizer?.destroy(); tts?.stop(); tts?.shutdown(); super.onDestroy() }
}
