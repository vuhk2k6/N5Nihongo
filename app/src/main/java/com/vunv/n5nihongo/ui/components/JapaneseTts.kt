package com.vunv.n5nihongo.ui.components

import android.content.Context
import android.content.pm.PackageManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.util.Locale

private const val TAG = "JapaneseTts"

/** Engines that ship with a Japanese voice on most Android devices, in preferred order. */
private val PREFERRED_TTS_ENGINES = listOf(
    "com.google.android.tts",   // Google TTS — best Japanese coverage
    "com.samsung.SMT",          // Samsung TTS — has Japanese on Samsung devices
)

/**
 * Returns a `(text) -> Unit` that speaks Japanese via Android TextToSpeech.
 *
 * Resilience features:
 *  - Explicitly picks Google TTS (or Samsung TTS) engine if installed — this is critical on
 *    devices whose default TTS is a Chinese engine that would otherwise read kanji as Mandarin.
 *  - Engine is created once per composable, shut down on dispose.
 *  - If `speak()` is called BEFORE OnInit fires, the request is queued and played as soon as the
 *    engine is ready. This avoids the common "first tap is silent" race condition.
 *  - `setLanguage(JAPANESE)` is re-asserted on every call so the engine never falls back to
 *    Mandarin/English between calls.
 *  - All errors are logged with TAG="JapaneseTts" and a one-shot Toast is shown so the user gets
 *    a clear signal instead of silent failure.
 */
/**
 * Khởi tạo TTS sau khi màn đã vẽ xong để không chặn chuyển trang / compose lần đầu.
 */
@Composable
fun rememberDeferredJapaneseSpeaker(): (String) -> Unit {
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(32)
        ready = true
    }
    if (!ready) {
        return remember { { _: String -> } }
    }
    return rememberJapaneseSpeaker()
}

@Composable
fun rememberJapaneseSpeaker(): (String) -> Unit {
    val context = LocalContext.current
    val ttsRef = remember { mutableStateOf<TextToSpeech?>(null) }
    val readyRef = remember { mutableStateOf(false) }
    val pendingRef = remember { mutableStateOf<String?>(null) }
    val warnedRef = remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val appCtx = context.applicationContext
        val enginePkg = pickPreferredEnginePackage(appCtx)
        Log.d(TAG, "Creating TextToSpeech with engine=$enginePkg")

        var engine: TextToSpeech? = null
        try {
            val listener = TextToSpeech.OnInitListener { status ->
                val e = engine
                if (status != TextToSpeech.SUCCESS || e == null) {
                    Log.w(TAG, "TTS init failed status=$status engine=$enginePkg")
                    showWarnOnce(appCtx, warnedRef, "Không khởi tạo được TTS. Hãy cài Google Text-to-speech.")
                    return@OnInitListener
                }
                val langResult = e.setLanguage(Locale.JAPANESE)
                Log.d(TAG, "setLanguage(JAPANESE) result=$langResult on engine=${e.defaultEngine}")
                if (langResult == TextToSpeech.LANG_MISSING_DATA ||
                    langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    showWarnOnce(
                        appCtx,
                        warnedRef,
                        "TTS chưa có gói tiếng Nhật. Mở Settings → Languages → Text-to-speech → Google TTS → Install voice data → Japanese."
                    )
                }
                applyJapaneseVoice(e)
                e.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) { Log.d(TAG, "onStart id=$utteranceId") }
                    override fun onDone(utteranceId: String?) { Log.d(TAG, "onDone id=$utteranceId") }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) { Log.w(TAG, "onError id=$utteranceId") }
                    override fun onError(utteranceId: String?, errorCode: Int) {
                        Log.w(TAG, "onError id=$utteranceId code=$errorCode")
                        showWarnOnce(appCtx, warnedRef, "Engine không phát được (code=$errorCode).")
                    }
                })
                readyRef.value = true
                Log.d(TAG, "TTS ready. voice=${e.voice?.name} locale=${e.voice?.locale}")
                pendingRef.value?.let { queued ->
                    pendingRef.value = null
                    doSpeak(e, queued)
                }
            }
            engine = if (enginePkg != null) {
                TextToSpeech(appCtx, listener, enginePkg)
            } else {
                TextToSpeech(appCtx, listener)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "TTS constructor threw", t)
            showWarnOnce(appCtx, warnedRef, "TTS lỗi khởi tạo: ${t.message}")
        }
        ttsRef.value = engine

        onDispose {
            runCatching { engine?.stop() }
            runCatching { engine?.shutdown() }
        }
    }

    return remember(context) {
        { raw: String ->
            val text = raw.trim()
            if (text.isNotBlank()) {
                val engine = ttsRef.value
                when {
                    engine == null -> {
                        Log.w(TAG, "TTS engine null, queuing: \"$text\"")
                        pendingRef.value = text
                    }
                    !readyRef.value -> {
                        Log.d(TAG, "TTS not ready, queuing: \"$text\"")
                        pendingRef.value = text
                    }
                    else -> doSpeak(engine, text)
                }
            }
        }
    }
}

/** Pick the first preferred TTS engine that is installed; returns null to use the system default. */
private fun pickPreferredEnginePackage(context: Context): String? {
    val pm: PackageManager = context.packageManager
    for (pkg in PREFERRED_TTS_ENGINES) {
        val installed = runCatching {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(pkg, 0)
            true
        }.getOrDefault(false)
        if (installed) return pkg
    }
    return null
}

private fun doSpeak(engine: TextToSpeech, text: String) {
    runCatching { applyJapaneseVoice(engine) }
    val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, text.hashCode().toString())
    Log.d(TAG, "speak text=\"$text\" result=$result (0=SUCCESS, -1=ERROR)")
}

private fun applyJapaneseVoice(engine: TextToSpeech) {
    engine.setLanguage(Locale.JAPANESE)
    runCatching {
        val ja = engine.voices?.firstOrNull { isJapaneseVoice(it) }
        if (ja != null && engine.voice?.name != ja.name) {
            engine.voice = ja
        }
    }
}

private fun showWarnOnce(
    appCtx: Context,
    warnedRef: androidx.compose.runtime.MutableState<Boolean>,
    msg: String
) {
    if (warnedRef.value) return
    warnedRef.value = true
    Toast.makeText(appCtx, msg, Toast.LENGTH_LONG).show()
}

private fun isJapaneseVoice(voice: Voice): Boolean {
    val locale = voice.locale ?: return false
    return locale.language.equals("ja", ignoreCase = true) ||
        runCatching { locale.isO3Language.equals("jpn", ignoreCase = true) }.getOrDefault(false)
}

/** Remove okurigana separators so TTS reads the clean reading. "まな.ぶ" -> "まなぶ". */
fun cleanKanjiReading(reading: String): String =
    reading.replace(".", "").replace("-", "").trim()

/**
 * Pick the best Japanese reading for a kanji entry. Prefers kunyomi, falls back to onyomi, finally
 * the kanji character itself.
 */
fun bestJapaneseReading(character: String, kunyomi: String, onyomi: String): String {
    val kun = cleanKanjiReading(kunyomi)
    if (kun.isNotBlank()) return kun
    val on = cleanKanjiReading(onyomi)
    if (on.isNotBlank()) return on
    return character
}

/** Compact speaker button that plays Japanese text via TTS. */
@Composable
fun JapaneseAudioButton(
    text: String,
    onSpeak: (String) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp
) {
    IconButton(
        onClick = { onSpeak(text) },
        enabled = text.isNotBlank(),
        modifier = modifier.size(size),
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
            contentDescription = "Phát âm"
        )
    }
}
