package com.example.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {
    const val CHANNEL_MEAL = "meal_reminder_channel"
    const val CHANNEL_WATER = "water_reminder_channel"

    const val WATER_NOTIFICATION_ID = 2001
    private const val MEAL_NOTIFICATION_BASE_ID = 1000

    fun ensureChannelsCreated(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val mealChannel = NotificationChannel(
                CHANNEL_MEAL,
                "Pengingat Jadwal Makan",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi pengingat waktu sarapan, makan siang, makan malam, dan camilan"
                enableVibration(true)
                vibrationPattern = longArrayOf(0L, 300L, 150L, 300L)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            val waterChannel = NotificationChannel(
                CHANNEL_WATER,
                "Pengingat Minum Air",
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

    fun mealNotificationId(mealId: Long): Int = MEAL_NOTIFICATION_BASE_ID + (mealId.toInt() and 0x7FFF)

    fun showMealNotification(
        context: Context,
        mealId: Long,
        mealName: String,
        mealTimeFormatted: String
    ) {
        ensureChannelsCreated(context)
        if (!hasNotificationPermission(context)) return

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = mealNotificationId(mealId)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
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

        val summaryText = "Jangan lewatkan jam makan Anda. Sudah makan?"
        val expandedText =
            "Jadwal $mealName pukul $mealTimeFormatted telah tiba. Jaga pola makan teratur agar tubuh tetap berenergi. Ketuk 'Sudah makan' untuk mencatat atau 'Tunda 10 menit' bila sedang sibuk."

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, CHANNEL_MEAL)
            .setSmallIcon(R.drawable.ic_notification_meal)
            .setContentTitle("Waktunya $mealName ($mealTimeFormatted)")
            .setContentText(summaryText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(defaultSoundUri)
            .setVibrate(longArrayOf(0L, 300L, 150L, 300L))
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
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
            .build()

        runCatching {
            notificationManager.notify(notificationId, notification)
        }
    }

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

        val notification = NotificationCompat.Builder(context, CHANNEL_WATER)
            .setSmallIcon(R.drawable.ic_notification_water)
            .setContentTitle("Waktunya Minum Air Putih!")
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
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
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)
    }
}
