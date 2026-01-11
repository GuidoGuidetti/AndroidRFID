package com.rfid.reader.utils

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log

/**
 * Helper per generare beep software controllati
 * Emette beep SOLO su eventi specifici (nuovo tag, connessione, etc.)
 * NON durante il polling continuo
 */
class BeepHelper(private val context: Context) {
    private val settingsManager = SettingsManager(context)
    private var toneGenerator: ToneGenerator? = null
    private var currentVolumeSetting: String = ""

    companion object {
        private const val TAG = "BeepHelper"
        private const val BEEP_DURATION_MS = 200  // Aumentato per essere più udibile

        @Volatile
        private var INSTANCE: BeepHelper? = null

        fun getInstance(context: Context): BeepHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BeepHelper(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    init {
        Log.d(TAG, "BeepHelper initializing...")
        initToneGenerator()
    }

    private fun initToneGenerator() {
        try {
            // Release existing first
            toneGenerator?.release()

            currentVolumeSetting = settingsManager.getBeepVolume()
            val volume = getToneVolume()

            // ✅ Usa STREAM_ALARM - sempre udibile, non può essere silenziato
            toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, volume)

            Log.d(TAG, "✅ ToneGenerator initialized SUCCESS (STREAM_ALARM, volume: $volume)")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error initializing ToneGenerator", e)
        }
    }

    private fun getToneVolume(): Int {
        // Mappa volume settings (low/medium/high) a volume ToneGenerator (0-100)
        return when (settingsManager.getBeepVolume()) {
            "low" -> 70
            "medium" -> 90
            "high" -> 100
            else -> 90
        }
    }

    /**
     * Emette un beep singolo
     * Usare SOLO per eventi specifici (nuovo tag, connessione, etc.)
     */
    fun playBeep() {
        try {
            Log.d(TAG, "🔊 playBeep() called")

            // ✅ Reinizializza SOLO se il volume è cambiato nei settings
            val volumeSetting = settingsManager.getBeepVolume()
            if (volumeSetting != currentVolumeSetting || toneGenerator == null) {
                Log.d(TAG, "Volume changed or null ($currentVolumeSetting -> $volumeSetting), reinitializing...")
                initToneGenerator()
            }

            // ✅ Usa TONE_DTMF_5 - tono acuto e ben udibile (1209 Hz)
            val result = toneGenerator?.startTone(ToneGenerator.TONE_DTMF_5, BEEP_DURATION_MS)

            Log.d(TAG, "✅ Beep startTone result: $result (volume: $volumeSetting = ${getToneVolume()})")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error playing beep", e)
        }
    }

    /**
     * Emette un doppio beep (per eventi speciali come connessione)
     */
    fun playDoubleBeep() {
        try {
            Log.d(TAG, "🔊🔊 playDoubleBeep() called")
            playBeep()
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                playBeep()
            }, 250)
        } catch (e: Exception) {
            Log.e(TAG, "Error playing double beep", e)
        }
    }

    /**
     * Rilascia le risorse
     */
    fun release() {
        try {
            toneGenerator?.release()
            toneGenerator = null
            Log.d(TAG, "ToneGenerator released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing ToneGenerator", e)
        }
    }

    /**
     * Aggiorna il volume del beep (chiamare dopo cambio settings)
     */
    fun updateVolume() {
        release()
        initToneGenerator()
    }
}
