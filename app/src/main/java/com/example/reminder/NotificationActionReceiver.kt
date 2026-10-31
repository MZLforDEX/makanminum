package com.example.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.MakanMinumRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val notificationId = intent.getIntExtra(
            EXTRA_NOTIFICATION_ID,
            NotificationHelper.WATER_NOTIFICATION_ID
        )
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appContext = context.applicationContext
                val repository = MakanMinumRepository.getInstance(appContext)
                val nowMillis = System.currentTimeMillis()

                when (action) {
                    ACTION_WATER_DRINK_250 -> {
                        repository.addWaterLog(amountMl = 250, nowMillis = nowMillis)
                        NotificationHelper.dismissNotification(appContext, notificationId)
                    }

                    ACTION_WATER_SNOOZE_10 -> {
                        ReminderScheduler.scheduleSnoozeWaterAlarm(
                            context = appContext,
                            delayMinutes = 10,
                            nowMillis = nowMillis
                        )
                        NotificationHelper.dismissNotification(appContext, notificationId)
                    }

                    ACTION_MEAL_ALREADY_ATE -> {
                        val mealId = intent.getLongExtra(EXTRA_MEAL_ID, -1L)
                        if (mealId != -1L) {
                            repository.markMealDoneToday(mealId, nowMillis)
                        }
                        NotificationHelper.dismissNotification(appContext, notificationId)
                    }

                    ACTION_MEAL_SNOOZE_10 -> {
                        val mealId = intent.getLongExtra(EXTRA_MEAL_ID, 1L)
                        val mealName = intent.getStringExtra(EXTRA_MEAL_NAME) ?: "Waktu Makan"
                        ReminderScheduler.scheduleSnoozeMealAlarm(
                            context = appContext,
                            mealId = mealId,
                            mealName = mealName,
                            delayMinutes = 10,
                            nowMillis = nowMillis
                        )
                        NotificationHelper.dismissNotification(appContext, notificationId)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_WATER_DRINK_250 = "com.example.reminder.ACTION_WATER_DRINK_250"
        const val ACTION_WATER_SNOOZE_10 = "com.example.reminder.ACTION_WATER_SNOOZE_10"
        const val ACTION_MEAL_ALREADY_ATE = "com.example.reminder.ACTION_MEAL_ALREADY_ATE"
        const val ACTION_MEAL_SNOOZE_10 = "com.example.reminder.ACTION_MEAL_SNOOZE_10"

        const val EXTRA_MEAL_ID = "extra_action_meal_id"
        const val EXTRA_MEAL_NAME = "extra_action_meal_name"
        const val EXTRA_NOTIFICATION_ID = "extra_action_notification_id"
    }
}
