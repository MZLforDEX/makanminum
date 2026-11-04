package com.example.reminder

import android.Manifest
import android.app.ActivityOptions
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.ui.AlarmRingingActivity

object NotificationHelper {
    const val CHANNEL_MEAL = "meal_alarm_channel_v3"
    const val CHANNEL_WATER = "water_reminder_channel_v3"

    const val WATER_NOTIFICATION_ID = 2001
    private const val MEAL_NOTIFICATION_BASE_ID = 1000

    fun ensureChannelsCreated(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Bersihkan kanal lama yang mungkin ter-cache tanpa prioritas kepala (heads-up)
            runCatching {
                manager.deleteNotificationChannel("meal_alarm_channel_v1")
                manager.deleteNotificationChannel("meal_alarm_channel_v2")
            }

            val alarmSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val alarmAudioAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val mealChannel = NotificationChannel(
                CHANNEL_MEAL,
                "Alarm Jadwal Makan",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alarm & peringatan penuh waktu sarapan, makan siang, makan malam, dan camilan"
                enableVibration(true)
                vibrationPattern = longArrayOf(0L, 500L, 300L, 500L, 600L)
                setSound(alarmSoundUri, alarmAudioAttrs)
                setBypassDnd(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            // Saluran khusus Pengingat Minum: berupa notifikasi biasa (bukan alarm berdering)
            val waterChannel = NotificationChannel(
                CHANNEL_WATER,
                "Notifikasi Pengingat Minum",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi pengingat minum air putih berkala antara jam bangun dan tidur"
                enableVibration(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            manager.createNotificationChannel(mealChannel)
            manager.createNotificationChannel(waterChannel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                manager.areNotificationsEnabled()
            } else {
                true
            }
        }
    }

    fun canUseFullScreenIntent(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.canUseFullScreenIntent() ?: true
        } else {
            true
        }
    }

    fun canDrawOverlays(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            runCatching { Settings.canDrawOverlays(context) }.getOrDefault(false)
        } else {
            true
        }
    }

    fun mealNotificationId(mealId: Long): Int = MEAL_NOTIFICATION_BASE_ID + (mealId.toInt() and 0x7FFF)

    /**
     * Menampilkan Pengingat Makan berupa Alarm Penuh (layar alarm + banner overlay + notifikasi heads-up + dering).
     */
    fun showMealNotification(
        context: Context,
        mealId: Long,
        mealName: String,
        mealTimeFormatted: String,
        launchAlarmScreen: Boolean = true
    ) {
        ensureChannelsCreated(context)
        val notificationId = mealNotificationId(mealId)

        val alarmInfo = ActiveAlarmInfo(
            alarmType = ReminderScheduler.TYPE_MEAL,
            mealId = mealId,
            mealName = mealName,
            mealTimeFormatted = mealTimeFormatted,
            notificationId = notificationId
        )

        val alarmScreenIntent = AlarmRingingActivity.createIntent(
            context = context,
            alarmType = ReminderScheduler.TYPE_MEAL,
            mealId = mealId,
            mealName = mealName,
            mealTime = mealTimeFormatted,
            notificationId = notificationId
        )

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            alarmScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alreadyAteIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_MEAL_ALREADY_ATE
            putExtra(NotificationActionReceiver.EXTRA_MEAL_ID, mealId)
            putExtra(NotificationActionReceiver.EXTRA_MEAL_NAME, mealName)
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val alreadyAtePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 1,
            alreadyAteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_MEAL_SNOOZE_10
            putExtra(NotificationActionReceiver.EXTRA_MEAL_ID, mealId)
            putExtra(NotificationActionReceiver.EXTRA_MEAL_NAME, mealName)
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_MEAL_DISMISS
            putExtra(NotificationActionReceiver.EXTRA_MEAL_ID, mealId)
            putExtra(NotificationActionReceiver.EXTRA_MEAL_NAME, mealName)
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 3,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val hasNotifPerm = hasNotificationPermission(context)

        // 1. Tampilkan notifikasi Heads-Up / FullScreenIntent terlebih dahulu sebelum mengambil AudioFocus
        // agar SystemUI di semua merek HP tidak menahan pop-up notifikasi saat pengguna membuka aplikasi lain.
        if (hasNotifPerm) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val summaryText = "Jangan lewatkan jam makan Anda. Ketuk untuk membuka atau matikan alarm."
            val expandedText =
                "Alarm jadwal $mealName pukul $mealTimeFormatted sedang berbunyi. Pilih 'Sudah makan' untuk mencatat, 'Tunda 10 menit', atau 'Matikan' untuk menghentikan suara alarm."

            val alarmSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val notification = NotificationCompat.Builder(context, CHANNEL_MEAL)
                .setSmallIcon(R.drawable.ic_notification_meal)
                .setContentTitle("Waktunya $mealName ($mealTimeFormatted)")
                .setContentText(summaryText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(expandedText))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSound(alarmSoundUri)
                .setVibrate(longArrayOf(0L, 500L, 300L, 500L, 600L))
                .setOngoing(true)
                .setAutoCancel(true)
                .setContentIntent(fullScreenPendingIntent)
                .setDeleteIntent(dismissPendingIntent)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .addAction(
                    R.drawable.ic_notification_meal,
                    context.getString(R.string.action_already_ate),
                    alreadyAtePendingIntent
                )
                .addAction(
                    R.drawable.ic_notification_meal,
                    context.getString(R.string.action_snooze_10),
                    snoozePendingIntent
                )
                .addAction(
                    R.drawable.ic_notification_meal,
                    context.getString(R.string.action_dismiss_alarm),
                    dismissPendingIntent
                )
                .build()

            runCatching {
                notificationManager.notify(notificationId, notification)
            }
        }

        // 2. Luncurkan layar AlarmRingingActivity di atas aplikasi lain
        var activityLaunchAttempted = false
        if (launchAlarmScreen) {
            runCatching {
                context.startActivity(alarmScreenIntent)
                activityLaunchAttempted = true
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                runCatching {
                    val options = ActivityOptions.makeBasic()
                        .setPendingIntentBackgroundActivityStartMode(
                            ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                        )
                        .toBundle()
                    fullScreenPendingIntent.send(
                        context,
                        0,
                        null,
                        null,
                        null,
                        null,
                        options
                    )
                    activityLaunchAttempted = true
                }
            }
        }

        // 3. Mulai dering alarm & pemeriksaan overlay fallback (hanya bila notifikasi aktif,
        // izin overlay aktif, atau peluncuran Activity dicoba).
        if (hasNotifPerm || canDrawOverlays(context) || activityLaunchAttempted) {
            AlarmRingingController.startRinging(context, alarmInfo)
        }
    }

    /**
     * Menampilkan Pengingat Minum berupa Notifikasi biasa (tanpa layar alarm penuh dan tanpa dering alarm berulang).
     */
    fun showWaterNotification(
        context: Context,
        currentMl: Int,
        targetMl: Int
    ) {
        ensureChannelsCreated(context)
        if (!hasNotificationPermission(context)) return

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            WATER_NOTIFICATION_ID,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val drink250Intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_WATER_DRINK_250
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, WATER_NOTIFICATION_ID)
        }
        val drink250PendingIntent = PendingIntent.getBroadcast(
            context,
            WATER_NOTIFICATION_ID * 10 + 1,
            drink250Intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_WATER_SNOOZE_10
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, WATER_NOTIFICATION_ID)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            WATER_NOTIFICATION_ID * 10 + 2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val remainingMl = (targetMl - currentMl).coerceAtLeast(0)
        val bodyText = if (remainingMl > 0) {
            "Progres hari ini: $currentMl / $targetMl ml ($remainingMl ml lagi menuju target)."
        } else {
            "Progres hari ini: $currentMl / $targetMl ml. Tetap terhidrasi dengan baik!"
        }

        val defaultNotifSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, CHANNEL_WATER)
            .setSmallIcon(R.drawable.ic_notification_water)
            .setContentTitle("Waktunya Minum Air Putih!")
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(defaultNotifSound)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                R.drawable.ic_notification_water,
                context.getString(R.string.action_drink_250),
                drink250PendingIntent
            )
            .addAction(
                R.drawable.ic_notification_water,
                context.getString(R.string.action_snooze_10),
                snoozePendingIntent
            )
            .build()

        runCatching {
            notificationManager.notify(WATER_NOTIFICATION_ID, notification)
        }
    }

    fun dismissNotification(context: Context, notificationId: Int) {
        if (notificationId != WATER_NOTIFICATION_ID) {
            AlarmRingingController.stopRinging(context)
        }
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)
    }
}
