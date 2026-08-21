package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class SoundEffectManager {

    private var isMuted: Boolean = false
    private val scope = CoroutineScope(Dispatchers.Default)

    fun setMuted(muted: Boolean) {
        isMuted = muted
    }

    private fun playTone(
        frequencies: List<Float>,
        durationMs: Int,
        sampleRate: Int = 44100,
        volume: Float = 0.7f,
        type: ToneType = ToneType.SINE
    ) {
        if (isMuted) return

        scope.launch {
            try {
                val numSamples = (durationMs * sampleRate) / 1000
                val buffer = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val t = i.toFloat() / sampleRate
                    var sample = 0f

                    for (f in frequencies) {
                        sample += when (type) {
                            ToneType.SINE -> sin(2.0 * PI * f * t).toFloat()
                            ToneType.CLICK -> {
                                val decay = exp(-t * 80.0).toFloat()
                                (sin(2.0 * PI * f * t) * decay).toFloat()
                            }
                            ToneType.CHORD -> {
                                val envelope = (1f - (i.toFloat() / numSamples))
                                (sin(2.0 * PI * f * t) * envelope).toFloat()
                            }
                            ToneType.FANFARE -> {
                                val envelope = minOf(1f, i.toFloat() / (sampleRate * 0.05f)) * (1f - (i.toFloat() / numSamples))
                                (sin(2.0 * PI * f * t) * envelope).toFloat()
                            }
                        }
                    }

                    sample = (sample / frequencies.size.coerceAtLeast(1)) * volume
                    sample = sample.coerceIn(-1.0f, 1.0f)
                    buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
                }

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(buffer, 0, buffer.size)
                audioTrack.play()
                // Auto release after sound finishes
                Thread.sleep(durationMs.toLong() + 50)
                audioTrack.release()
            } catch (_: Exception) {
                // Ignore audio errors gracefully
            }
        }
    }

    fun playDiceRoll() {
        playTone(listOf(400f, 600f, 850f), durationMs = 60, type = ToneType.CLICK, volume = 0.5f)
    }

    fun playDiceLand() {
        playTone(listOf(220f, 330f), durationMs = 120, type = ToneType.CLICK, volume = 0.8f)
    }

    fun playTokenStep() {
        playTone(listOf(523.25f, 659.25f), durationMs = 70, type = ToneType.CHORD, volume = 0.6f)
    }

    fun playTokenOut() {
        playTone(listOf(440f, 554.37f, 659.25f), durationMs = 150, type = ToneType.CHORD, volume = 0.8f)
    }

    fun playCapture() {
        playTone(listOf(150f, 220f, 110f), durationMs = 250, type = ToneType.CLICK, volume = 0.9f)
    }

    fun playSafeStar() {
        playTone(listOf(659.25f, 880.0f, 1046.50f), durationMs = 200, type = ToneType.CHORD, volume = 0.75f)
    }

    fun playHomeEntry() {
        playTone(listOf(523.25f, 659.25f, 783.99f, 1046.50f), durationMs = 350, type = ToneType.FANFARE, volume = 0.85f)
    }

    fun playVictory() {
        scope.launch {
            val chord1 = listOf(523.25f, 659.25f, 783.99f) // C major
            val chord2 = listOf(587.33f, 739.99f, 880.00f) // D major
            val chord3 = listOf(659.25f, 830.61f, 987.77f) // E major
            val chord4 = listOf(783.99f, 987.77f, 1174.66f, 1567.98f) // High G Major

            playTone(chord1, 200, type = ToneType.FANFARE, volume = 0.8f)
            Thread.sleep(220)
            playTone(chord2, 200, type = ToneType.FANFARE, volume = 0.8f)
            Thread.sleep(220)
            playTone(chord3, 200, type = ToneType.FANFARE, volume = 0.8f)
            Thread.sleep(220)
            playTone(chord4, 600, type = ToneType.FANFARE, volume = 0.9f)
        }
    }

    enum class ToneType {
        SINE,
        CLICK,
        CHORD,
        FANFARE
    }
}
