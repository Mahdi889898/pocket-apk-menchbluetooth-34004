package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class GameFeedbackManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    var soundEnabled: Boolean = true

    @Volatile
    var vibrationEnabled: Boolean = true

    fun playDiceRoll() {
        vibrate(25L)
        if (!soundEnabled) return
        scope.launch {
            playToneSequence(
                listOf(
                    ToneSpec(340.0, 35, 0.22f),
                    ToneSpec(460.0, 35, 0.22f),
                    ToneSpec(580.0, 40, 0.25f),
                    ToneSpec(720.0, 55, 0.28f)
                )
            )
        }
    }

    fun playPawnHop(stepIndex: Int = 0) {
        if (!soundEnabled) return
        val baseFreq = 440.0 + (stepIndex % 6) * 55.0
        scope.launch {
            playToneSequence(listOf(ToneSpec(baseFreq, 55, 0.24f)))
        }
    }

    fun playSixRolled() {
        vibrate(45L)
        if (!soundEnabled) return
        scope.launch {
            playToneSequence(
                listOf(
                    ToneSpec(523.25, 70, 0.28f),
                    ToneSpec(659.25, 70, 0.28f),
                    ToneSpec(783.99, 110, 0.30f)
                )
            )
        }
    }

    fun playCapture() {
        vibrate(90L)
        if (!soundEnabled) return
        scope.launch {
            playToneSequence(
                listOf(
                    ToneSpec(620.0, 65, 0.30f),
                    ToneSpec(420.0, 65, 0.30f),
                    ToneSpec(880.0, 120, 0.32f)
                )
            )
        }
    }

    fun playStickerPop() {
        vibrate(18L)
        if (!soundEnabled) return
        scope.launch {
            playToneSequence(
                listOf(
                    ToneSpec(640.0, 45, 0.22f),
                    ToneSpec(920.0, 75, 0.24f)
                )
            )
        }
    }

    fun playVictory() {
        vibrate(140L)
        if (!soundEnabled) return
        scope.launch {
            playToneSequence(
                listOf(
                    ToneSpec(523.25, 100, 0.28f),
                    ToneSpec(659.25, 100, 0.28f),
                    ToneSpec(783.99, 100, 0.30f),
                    ToneSpec(1046.50, 240, 0.32f)
                )
            )
        }
    }

    private fun vibrate(durationMs: Long) {
        if (!vibrationEnabled) return
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(durationMs)
                }
            }
        }
    }

    private data class ToneSpec(val freqHz: Double, val durationMs: Int, val volume: Float)

    private fun playToneSequence(tones: List<ToneSpec>) {
        runCatching {
            val sampleRate = 22050
            val totalSamples = tones.sumOf { (sampleRate * it.durationMs) / 1000 }
            if (totalSamples <= 0) return

            val pcm = ShortArray(totalSamples)
            var offset = 0
            for (tone in tones) {
                val count = (sampleRate * tone.durationMs) / 1000
                for (i in 0 until count) {
                    val t = i.toDouble() / sampleRate
                    val env = exp(-5.5 * i.toDouble() / count.coerceAtLeast(1))
                    val wave = sin(2.0 * PI * tone.freqHz * t) + 0.25 * sin(4.0 * PI * tone.freqHz * t)
                    val sample = (wave * env * tone.volume * Short.MAX_VALUE).toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    pcm[offset + i] = sample.toShort()
                }
                offset += count
            }

            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = maxOf(minBuf, pcm.size * 2)
            val track = AudioTrack.Builder()
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
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(pcm, 0, pcm.size)
            track.play()
            val sleepMs = tones.sumOf { it.durationMs }.toLong() + 60L
            Thread.sleep(sleepMs)
            track.stop()
            track.release()
        }
    }
}
