package com.example.reminder

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.example.ui.AlarmRingingActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ActiveAlarmInfo(
    val alarmType: String = ReminderScheduler.TYPE_MEAL,
    val mealId: Long = 1L,
    val mealName: String = "Waktu Makan",
    val mealTimeFormatted: String = "",
    val notificationId: Int = 1001
)

/**
 * Mengelola dering suara alarm (USAGE_ALARM), getaran berulang, status alarm aktif,
 * serta banner overlay darurat (bila pengguna sedang membuka aplikasi lain yang memblokir
 * peluncuran Activity latar belakang).
 */
object AlarmRingingController {
    private val lock = Any()
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private val mainHandler: Handler? by lazy {
        runCatching { Handler(Looper.getMainLooper()) }.getOrNull()
    }
    private var autoStopRunnable: Runnable? = null
    private var overlayCheckRunnable: Runnable? = null
    private var overlayView: View? = null
    private var overlayWindowManager: WindowManager? = null

    @Volatile
    var isAlarmActivityVisible: Boolean = false
        private set

    private val _activeAlarm = MutableStateFlow<ActiveAlarmInfo?>(null)
    val activeAlarm: StateFlow<ActiveAlarmInfo?> = _activeAlarm.asStateFlow()

    private const val AUTO_STOP_TIMEOUT_MS = 60_000L
    private const val OVERLAY_FALLBACK_DELAY_MS = 450L

    fun onAlarmActivityResumed() {
        isAlarmActivityVisible = true
        removeFallbackOverlay()
    }

    fun onAlarmActivityPaused() {
        isAlarmActivityVisible = false
    }

    fun setActiveAlarmForTest(info: ActiveAlarmInfo?) {
        _activeAlarm.value = info
    }

    fun startRinging(context: Context, alarmInfo: ActiveAlarmInfo? = null) {
        val appContext = context.applicationContext ?: context
        synchronized(lock) {
            if (alarmInfo != null) {
                _activeAlarm.value = alarmInfo
            } else if (_activeAlarm.value == null) {
                _activeAlarm.value = ActiveAlarmInfo()
            }

            // Jika MediaPlayer sudah sedang berbunyi (mis. dipanggil dari NotificationHelper lalu
            // disusul oleh AlarmRingingActivity.onCreate), jangan hentikan & ulangi dari awal
            // agar suara alarm tidak terputus-putus.
            val alreadyPlaying = runCatching { mediaPlayer?.isPlaying == true }.getOrDefault(false)
            if (!alreadyPlaying) {
                stopAudioAndVibrationInternal()

                // 1. Dapatkan Audio Manager & Minta Audio Focus Sementara
                runCatching {
                    val am = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                    audioManager = am
                    val audioAttrs = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                            .setAudioAttributes(audioAttrs)
                            .setOnAudioFocusChangeListener { /* Pertahankan alarm */ }
                            .build()
                        audioFocusRequest = focusRequest
                        am?.requestAudioFocus(focusRequest)
                    } else {
                        @Suppress("DEPRECATION")
                        am?.requestAudioFocus(
                            null,
                            AudioManager.STREAM_ALARM,
                            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                        )
                    }
                }

                // 2. Mulai suara dering alarm berulang (looping)
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

                // 3. Mulai pola getaran alarm berulang dengan atribut alarm
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
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val vibAttrs = VibrationAttributes.Builder()
                                .setUsage(VibrationAttributes.USAGE_ALARM)
                                .build()
                            vib.vibrate(VibrationEffect.createWaveform(pattern, 0), vibAttrs)
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vib.vibrate(VibrationEffect.createWaveform(pattern, 0))
                        } else {
                            @Suppress("DEPRECATION")
                            vib.vibrate(pattern, 0)
                        }
                        vibrator = vib
                    }
                }
            }

            // 4. Jadwalkan pemeriksaan tampilan visual (Overlay Fallback & Anti-Ghost Alarm)
            overlayCheckRunnable?.let { mainHandler?.removeCallbacks(it) }
            val checkTask = Runnable {
                val currentInfo = _activeAlarm.value ?: return@Runnable
                if (!isAlarmActivityVisible) {
                    // Coba luncurkan ulang AlarmRingingActivity sekali lagi dari main thread
                    runCatching {
                        val retryIntent = AlarmRingingActivity.createIntent(
                            context = appContext,
                            alarmType = currentInfo.alarmType,
                            mealId = currentInfo.mealId,
                            mealName = currentInfo.mealName,
                            mealTime = currentInfo.mealTimeFormatted,
                            notificationId = currentInfo.notificationId
                        )
                        appContext.startActivity(retryIntent)
                    }

                    // Jika izin Overlay ("Tampilkan di atas aplikasi lain") aktif, tampilkan kartu
                    // alarm melayang di bagian atas layar agar pengguna yang sedang membuka
                    // aplikasi lain (game/video/chat) tetap melihat peringatan & tombol aksi.
                    if (NotificationHelper.canDrawOverlays(appContext)) {
                        showFallbackOverlayIfNeeded(appContext, currentInfo)
                    } else if (!NotificationHelper.hasNotificationPermission(appContext)) {
                        // Jika baik Activity, Overlay, maupun Notifikasi tidak bisa tampil sama sekali,
                        // hentikan dering agar tidak terjadi "ghost alarm" tanpa tampilan.
                        stopRinging(appContext)
                    }
                }
            }
            overlayCheckRunnable = checkTask
            mainHandler?.postDelayed(checkTask, OVERLAY_FALLBACK_DELAY_MS)

            // 5. Batas waktu otomatis 60 detik agar tidak menguras baterai bila HP ditinggalkan
            autoStopRunnable?.let { mainHandler?.removeCallbacks(it) }
            val timeoutTask = Runnable {
                stopRinging(appContext)
            }
            autoStopRunnable = timeoutTask
            mainHandler?.postDelayed(timeoutTask, AUTO_STOP_TIMEOUT_MS)
        }
    }

    fun stopRinging(context: Context? = null) {
        synchronized(lock) {
            _activeAlarm.value = null
            overlayCheckRunnable?.let { mainHandler?.removeCallbacks(it) }
            overlayCheckRunnable = null
            removeFallbackOverlay()
            stopAudioAndVibrationInternal()
        }
    }

    private fun stopAudioAndVibrationInternal() {
        autoStopRunnable?.let { mainHandler?.removeCallbacks(it) }
        autoStopRunnable = null

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager?.abandonAudioFocus(null)
            }
        }
        audioFocusRequest = null
        audioManager = null

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

    private fun showFallbackOverlayIfNeeded(context: Context, info: ActiveAlarmInfo) {
        mainHandler?.post {
            if (isAlarmActivityVisible || _activeAlarm.value == null || overlayView != null) {
                return@post
            }
            runCatching {
                val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                    ?: return@runCatching
                val density = context.resources.displayMetrics.density
                fun dp(value: Int): Int = (value * density).toInt()

                val cardBg = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(20).toFloat()
                    setColor(Color.parseColor("#1E293B"))
                    setStroke(dp(2), Color.parseColor("#38BDF8"))
                }

                val rootLayout = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    background = cardBg
                    setPadding(dp(18), dp(16), dp(18), dp(16))
                    elevation = dp(12).toFloat()
                    setOnClickListener {
                        runCatching {
                            val openAlarmIntent = AlarmRingingActivity.createIntent(
                                context = context,
                                alarmType = info.alarmType,
                                mealId = info.mealId,
                                mealName = info.mealName,
                                mealTime = info.mealTimeFormatted,
                                notificationId = info.notificationId
                            )
                            context.startActivity(openAlarmIntent)
                        }
                        removeFallbackOverlay()
                    }
                }

                val badgeView = TextView(context).apply {
                    text = "⏰ ALARM JADWAL MAKAN • ${info.mealTimeFormatted}"
                    setTextColor(Color.parseColor("#38BDF8"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    setTypeface(typeface, Typeface.BOLD)
                }

                val titleView = TextView(context).apply {
                    text = "Waktunya ${info.mealName}!"
                    setTextColor(Color.WHITE)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                    setTypeface(typeface, Typeface.BOLD)
                    setPadding(0, dp(4), 0, dp(2))
                }

                val subtitleView = TextView(context).apply {
                    text = "Jangan lewatkan jam makan Anda. Ketuk kartu ini untuk layar penuh atau pilih aksi:"
                    setTextColor(Color.parseColor("#CBD5E1"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    setPadding(0, 0, 0, dp(12))
                }

                val actionsRow = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.END
                }

                fun createOverlayButton(
                    label: String,
                    bgColorHex: String,
                    textColorHex: String,
                    onClick: () -> Unit
                ): Button {
                    val btnBg = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = dp(12).toFloat()
                        setColor(Color.parseColor(bgColorHex))
                    }
                    return Button(context).apply {
                        text = label
                        isAllCaps = false
                        setTextColor(Color.parseColor(textColorHex))
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                        background = btnBg
                        setPadding(dp(12), dp(6), dp(12), dp(6))
                        setOnClickListener { onClick() }
                    }
                }

                val doneBtn = createOverlayButton(
                    label = "Sudah Makan",
                    bgColorHex = "#38BDF8",
                    textColorHex = "#0F172A"
                ) {
                    val intent = Intent(context, NotificationActionReceiver::class.java).apply {
                        action = NotificationActionReceiver.ACTION_MEAL_ALREADY_ATE
                        putExtra(NotificationActionReceiver.EXTRA_MEAL_ID, info.mealId)
                        putExtra(NotificationActionReceiver.EXTRA_MEAL_NAME, info.mealName)
                        putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, info.notificationId)
                    }
                    context.sendBroadcast(intent)
                    stopRinging(context)
                }

                val snoozeBtn = createOverlayButton(
                    label = "Tunda 10m",
                    bgColorHex = "#334155",
                    textColorHex = "#F8FAFC"
                ) {
                    val intent = Intent(context, NotificationActionReceiver::class.java).apply {
                        action = NotificationActionReceiver.ACTION_MEAL_SNOOZE_10
                        putExtra(NotificationActionReceiver.EXTRA_MEAL_ID, info.mealId)
                        putExtra(NotificationActionReceiver.EXTRA_MEAL_NAME, info.mealName)
                        putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, info.notificationId)
                    }
                    context.sendBroadcast(intent)
                    stopRinging(context)
                }

                val dismissBtn = createOverlayButton(
                    label = "Matikan",
                    bgColorHex = "#475569",
                    textColorHex = "#F8FAFC"
                ) {
                    val intent = Intent(context, NotificationActionReceiver::class.java).apply {
                        action = NotificationActionReceiver.ACTION_MEAL_DISMISS
                        putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, info.notificationId)
                    }
                    context.sendBroadcast(intent)
                    stopRinging(context)
                }

                val spacerParams = LinearLayout.LayoutParams(0, dp(38), 1f).apply {
                    marginEnd = dp(6)
                }
                val lastParams = LinearLayout.LayoutParams(0, dp(38), 1f)

                actionsRow.addView(doneBtn, spacerParams)
                actionsRow.addView(snoozeBtn, spacerParams)
                actionsRow.addView(dismissBtn, lastParams)

                rootLayout.addView(badgeView)
                rootLayout.addView(titleView)
                rootLayout.addView(subtitleView)
                rootLayout.addView(actionsRow)

                val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                }

                val layoutParams = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    windowType,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    y = dp(16)
                }

                val container = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(12), dp(8), dp(12), dp(8))
                    addView(
                        rootLayout,
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    )
                }

                wm.addView(container, layoutParams)
                overlayWindowManager = wm
                overlayView = container
            }
        }
    }

    private fun removeFallbackOverlay() {
        val removeTask = Runnable {
            runCatching {
                val view = overlayView
                val wm = overlayWindowManager
                if (view != null && wm != null) {
                    wm.removeViewImmediate(view)
                }
            }
            overlayView = null
            overlayWindowManager = null
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            removeTask.run()
        } else {
            mainHandler?.post(removeTask)
        }
    }
}
