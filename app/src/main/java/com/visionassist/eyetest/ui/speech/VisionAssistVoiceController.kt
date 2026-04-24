package com.visionassist.eyetest.ui.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Wraps Android [TextToSpeech] for spoken instructions and letter read-out during screening.
 * Uses [applicationContext]; call [shutdown] when the host composable leaves composition.
 */
class VisionAssistVoiceController(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private val ready = AtomicBoolean(false)
    private val pending = mutableListOf<String>()

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            ready.set(false)
            pending.clear()
            return
        }
        tts?.language = Locale.getDefault()
        ready.set(true)
        synchronized(pending) {
            pending.forEach { speakNow(it) }
            pending.clear()
        }
    }

    fun speak(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        if (ready.get()) {
            speakNow(trimmed)
        } else {
            synchronized(pending) { pending += trimmed }
        }
    }

    /** Speaks each character with a short pause so letters are distinct for Snellen rows. */
    fun speakLetterLine(line: String) {
        val letters = line.filter { !it.isWhitespace() }
        if (letters.isEmpty()) return
        val forSpeech = letters.map { it.toString() }.joinToString(" … ")
        speak(forSpeech)
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        ready.set(false)
        synchronized(pending) { pending.clear() }
    }

    private fun speakNow(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "va_${System.nanoTime()}")
    }
}

@Composable
fun rememberVisionAssistVoice(): VisionAssistVoiceController {
    val context = LocalContext.current
    val voice = remember { VisionAssistVoiceController(context) }
    DisposableEffect(Unit) {
        onDispose { voice.shutdown() }
    }
    return voice
}
