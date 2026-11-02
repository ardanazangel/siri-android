package dev.nova.assistant

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineTransducerModelConfig
import java.io.File

/** Offline, in-process Parakeet-TDT v3 via sherpa-onnx. No other app or speech service. */
class LocalParakeet(private val context: Context) {
    private val sampleRate = 16000
    private var recognizer: OfflineRecognizer? = null

    private fun installAsset(name: String): String {
        val destination = File(context.filesDir, "parakeet-v3/$name")
        if (!destination.exists() || destination.length() == 0L) {
            destination.parentFile?.mkdirs()
            val partial = File(destination.absolutePath + ".partial")
            try {
                context.assets.open("models/parakeet/$name").use { source ->
                    partial.outputStream().use { target -> source.copyTo(target, 1024 * 1024) }
                }
                check(partial.renameTo(destination)) { "No se pudo instalar $name" }
            } finally {
                partial.delete()
            }
        }
        return destination.absolutePath
    }

    fun prepare() {
        if (recognizer != null) return
        val encoder = installAsset("encoder.int8.onnx")
        val decoder = installAsset("decoder.int8.onnx")
        val joiner = installAsset("joiner.int8.onnx")
        val tokens = installAsset("tokens.txt")
        recognizer = OfflineRecognizer(
            config = OfflineRecognizerConfig(
                featConfig = FeatureConfig(sampleRate = sampleRate, featureDim = 80),
                modelConfig = OfflineModelConfig(
                    transducer = OfflineTransducerModelConfig(
                        encoder = encoder, decoder = decoder, joiner = joiner
                    ),
                    tokens = tokens,
                    modelType = "nemo_transducer",
                    numThreads = 4,
                    provider = "cpu"
                ),
                decodingMethod = "greedy_search"
            )
        )
    }

    @Suppress("MissingPermission")
    fun recordAndTranscribe(onRecording: () -> Unit): String {
        prepare()
        val min = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        check(min > 0) { "Micrófono no disponible" }
        val audio = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION, sampleRate,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
            maxOf(min, 4096)
        )
        try {
            check(audio.state == AudioRecord.STATE_INITIALIZED) { "No se puede abrir el micrófono" }
            audio.startRecording()
            onRecording()
            val collected = ArrayList<Float>(sampleRate * 6)
            val buffer = ShortArray(2048)
            val deadline = System.nanoTime() + 7_000_000_000L
            while (System.nanoTime() < deadline) {
                val count = audio.read(buffer, 0, buffer.size)
                if (count < 0) error("Error leyendo el micrófono: $count")
                for (i in 0 until count) collected.add(buffer[i] / 32768f)
            }
            audio.stop()
            val stream = checkNotNull(recognizer).createStream()
            return try {
                stream.acceptWaveform(collected.toFloatArray(), sampleRate)
                checkNotNull(recognizer).decode(stream)
                checkNotNull(recognizer).getResult(stream).text
            } finally {
                stream.release()
            }
        } finally {
            audio.release()
        }
    }

    fun release() {
        recognizer?.release()
        recognizer = null
    }
}
