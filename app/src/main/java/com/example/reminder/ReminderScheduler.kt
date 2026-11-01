package com.example.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import com.example.data.DateTimeUtils
import com.example.data.MealReminderEntity
import com.example.data.UserSettings

object ReminderScheduler {
    const val EXTRA_ALARM_TYPE = "extra_alarm_type"
    const val TYPE_MEAL = "type_meal"
    const val TYPE_WATER = "type_water"

    const val EXTRA_MEAL_ID = "extra_meal_id"
    const val EXTRA_MEAL_NAME = "extra_meal_name"
    const val EXTRA_MEAL_TIME = "extra_meal_time"
    const val EXTRA_IS_SNOOZE = "extra_is_snooze"

    private const val WATER_REQUEST_CODE = 9001
    private const val WATER_SNOOZE_REQUEST_CODE = 9002
    private const val MEAL_BASE_REQUEST_CODE = 5000
    private const val MEAL_SNOOZE_BASE_REQUEST_CODE = 7000

    fun canScheduleExactAlarms(context: Context): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun scheduleMealAlarm(
        context: Context,
        meal: MealReminderEntity,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        if (!meal.aktif || meal.hariAktif == 0) {
            cancelMealAlarm(context, meal.id)
            return
        }

        val nextTriggerMillis = DateTimeUtils.computeNextMealTriggerMillis(meal, nowMillis) ?: return
        val intent = Intent(context, AlarmTriggerReceiver::class.java).apply {
            putExtra(EXTRA_ALARM_TYPE, TYPE_MEAL)
            putExtra(EXTRA_MEAL_ID, meal.id)
            putExtra(EXTRA_MEAL_NAME, meal.nama.take(40))
            putExtra(EXTRA_MEAL_TIME, meal.formattedTime())
            putExtra(EXTRA_IS_SNOOZE, false)
        }
        val requestCode = MEAL_BASE_REQUEST_CODE + (meal.id.toInt() and 0x0FFF)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setExactOrFallbackAlarm(context, nextTriggerMillis, pendingIntent)
    }

    fun cancelMealAlarm(context: Context, mealId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val regularRequestCode = MEAL_BASE_REQUEST_CODE + (mealId.toInt() and 0x0FFF)
        val snoozeRequestCode = MEAL_SNOOZE_BASE_REQUEST_CODE + (mealId.toInt() and 0x0FFF)

        cancelBroadcastPendingIntent(context, alarmManager, regularRequestCode)
        cancelBroadcastPendingIntent(context, alarmManager, snoozeRequestCode)
        NotificationHelper.dismissNotification(context, NotificationHelper.mealNotificationId(mealId))
    }

    fun scheduleSnoozeMealAlarm(
        context: Context,
        mealId: Long,
        mealName: String,
        delayMinutes: Int = 10,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        val safeDelay = delayMinutes.coerceIn(1, 120)
        val triggerAtMillis = nowMillis + safeDelay * 60_000L
        val intent = Intent(context, AlarmTriggerReceiver::class.java).apply {
            putExtra(EXTRA_ALARM_TYPE, TYPE_MEAL)
            putExtra(EXTRA_MEAL_ID, mealId)
            putExtra(EXTRA_MEAL_NAME, mealName.take(40))
            putExtra(EXTRA_MEAL_TIME, DateTimeUtils.formatTimeMillis(triggerAtMillis))
            putExtra(EXTRA_IS_SNOOZE, true)
        }
        val requestCode = MEAL_SNOOZE_BASE_REQUEST_CODE + (mealId.toInt() and 0x0FFF)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setExactOrFallbackAlarm(context, triggerAtMillis, pendingIntent)
    }

    fun scheduleWaterAlarm(
        context: Context,
        settings: UserSettings,
        todayTotalMl: Int,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        if (!settings.pengingatMinumAktif) {
            cancelWaterAlarm(context)
            return
        }

        // Jika target hari ini sudah tercapai dan stopSetelahTarget aktif, batalkan snooze hari ini bila ada
        if (settings.stopSetelahTarget && settings.targetMl > 0 && todayTotalMl >= settings.targetMl) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            if (alarmManager != null) {
                cancelBroadcastPendingIntent(context, alarmManager, WATER_SNOOZE_REQUEST_CODE)
            }
            NotificationHelper.dismissNotification(context, NotificationHelper.WATER_NOTIFICATION_ID)
        }

        val nextTriggerMillis = DateTimeUtils.computeNextWaterReminderMillis(
            settings = settings,
            todayTotalMl = todayTotalMl,
            nowMillis = nowMillis
        )
        if (nextTriggerMillis == null) {
            cancelWaterAlarm(context)
            return
        }

        val intent = Intent(context, AlarmTriggerReceiver::class.java).apply {
            putExtra(EXTRA_ALARM_TYPE, TYPE_WATER)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            WATER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setExactOrFallbackAlarm(context, nextTriggerMillis, pendingIntent)
    }

    fun scheduleSnoozeWaterAlarm(
        context: Context,
        delayMinutes: Int = 10,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        val safeDelay = delayMinutes.coerceIn(1, 120)
        val triggerAtMillis = nowMillis + safeDelay * 60_000L
        val intent = Intent(context, AlarmTriggerReceiver::class.java).apply {
            putExtra(EXTRA_ALARM_TYPE, TYPE_WATER)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            WATER_SNOOZE_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setExactOrFallbackAlarm(context, triggerAtMillis, pendingIntent)
    }

    fun cancelWaterAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        cancelBroadcastPendingIntent(context, alarmManager, WATER_REQUEST_CODE)
        cancelBroadcastPendingIntent(context, alarmManager, WATER_SNOOZE_REQUEST_CODE)
        NotificationHelper.dismissNotification(context, NotificationHelper.WATER_NOTIFICATION_ID)
    }

    private fun cancelBroadcastPendingIntent(
        context: Context,
        alarmManager: AlarmManager,
        requestCode: Int
    ) {
        val intent = Intent(context, AlarmTriggerReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun setExactOrFallbackAlarm(
        context: Context,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        try {
            if (canScheduleExactAlarms(context)) {
                val showAppIntent = Intent(context, com.example.MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val showAppPendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    showAppIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, showAppPendingIntent)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            // Fallback ke alarm tidak tepat bila izin dicabut tiba-tiba
            runCatching {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        }
    }
}
