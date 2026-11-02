package com.example.reminder

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Mengelola dering suara alarm (USAGE_ALARM) dan getaran berulang saat waktu pengingat
 * makan tiba, serta menghentikannya saat pengguna merespons.
 */
object AlarmRingingController {
    private val lock = Any()
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var autoStopRunnable: Runnable? = null

    private const val AUTO_STOP_TIMEOUT_MS = 60_000L

    fun startRinging(context: Context) {
        val appContext = context.applicationContext
        synchronized(lock) {
            stopInternal()

            // 1. Mulai suara dering alarm berulang (looping)
            runCatching {
                val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

                if (alarmUri != null) {
                    val player = MediaPlayer().apply {
                        setDataSource(appContext, alarmUri)
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                        isLooping = true
                        prepare()
                        start()
                    }
                    mediaPlayer = player
                }
            }

            // 2. Mulai pola getaran alarm berulang
            runCatching {
                val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vm?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }

                if (vib != null && vib.hasVibrator()) {
                    val pattern = longArrayOf(0L, 500L, 400L, 500L, 800L)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vib.vibrate(VibrationEffect.createWaveform(pattern, 0))
                    } else {
                        @Suppress("DEPRECATION")
                        vib.vibrate(pattern, 0)
                    }
                    vibrator = vib
                }
            }

            // 3. Batas waktu otomatis 60 detik agar tidak menguras baterai bila HP tidak di dekat pengguna
            val timeoutTask = Runnable {
                stopRinging(appContext)
            }
            autoStopRunnable = timeoutTask
            mainHandler.postDelayed(timeoutTask, AUTO_STOP_TIMEOUT_MS)
        }
    }

    fun stopRinging(context: Context? = null) {
        synchronized(lock) {
            stopInternal()
        }
    }

    private fun stopInternal() {
        autoStopRunnable?.let { mainHandler.removeCallbacks(it) }
        autoStopRunnable = null

        runCatching {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            }
        }
        mediaPlayer = null

        runCatching {
            vibrator?.cancel()
        }
        vibrator = null
    }
}
