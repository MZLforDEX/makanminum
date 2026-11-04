package com.example.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.DateTimeUtils
import com.example.data.MakanMinumDatabase
import com.example.data.UserPreferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class AlarmTriggerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmType = intent.getStringExtra(ReminderScheduler.EXTRA_ALARM_TYPE) ?: return
        val isSnooze = intent.getBooleanExtra(ReminderScheduler.EXTRA_IS_SNOOZE, false)
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
        val wakeLock = powerManager?.newWakeLock(
            android.os.PowerManager.PARTIAL_WAKE_LOCK,
            "makanminum:alarm_trigger_receiver"
        )
        wakeLock?.acquire(10_000L)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                runCatching {
                    val db = MakanMinumDatabase.getInstance(appContext)
                    val prefs = UserPreferencesDataStore(appContext)
                    val nowMillis = System.currentTimeMillis()
                    val today = DateTimeUtils.todayDateString(nowMillis)

                    when (alarmType) {
                        ReminderScheduler.TYPE_MEAL -> {
                            val mealId = intent.getLongExtra(ReminderScheduler.EXTRA_MEAL_ID, -1L)
                            val mealFromDb = if (mealId != -1L) {
                                db.mealReminderDao().getMealById(mealId)
                            } else {
                                null
                            }
                            val settings = prefs.getSettingsSync()
                            val alreadyAteToday = mealId != -1L && settings.isMealDoneToday(mealId, today)

                            val mealName = (mealFromDb?.nama
                                ?: intent.getStringExtra(ReminderScheduler.EXTRA_MEAL_NAME)
                                ?: "Waktu Makan").take(40)
                            val mealTime = mealFromDb?.formattedTime()
                                ?: intent.getStringExtra(ReminderScheduler.EXTRA_MEAL_TIME)
                                ?: DateTimeUtils.formatTimeMillis(nowMillis)

                            // Tampilkan notifikasi bila pengingat aktif dan belum ditandai "Sudah makan" hari ini
                            // (atau bila alarm berasal dari tombol "Tunda 10 menit")
                            if ((mealFromDb == null || mealFromDb.aktif) && (!alreadyAteToday || isSnooze)) {
                                NotificationHelper.showMealNotification(
                                    context = appContext,
                                    mealId = if (mealId != -1L) mealId else 1L,
                                    mealName = mealName,
                                    mealTimeFormatted = mealTime
                                )
                            }

                            // Jadwalkan otomatis kemunculan hari aktif berikutnya melalui AlarmManager
                            if (mealFromDb != null && mealFromDb.aktif) {
                                ReminderScheduler.scheduleMealAlarm(
                                    context = appContext,
                                    meal = mealFromDb,
                                    nowMillis = nowMillis + 60_000L
                                )
                            }
                        }

                        ReminderScheduler.TYPE_WATER -> {
                            val settings = prefs.getSettingsSync()
                            if (!settings.pengingatMinumAktif) return@runCatching

                            val todayTotalMl = db.waterLogDao().getTotalWaterForDateSync(today)

                            val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
                            val minutesOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
                            val withinWakeSleepWindow = DateTimeUtils.isWithinWakeSleepWindow(
                                minutesOfDay = minutesOfDay,
                                wakeMinutes = settings.jamBangunMenit,
                                sleepMinutes = settings.jamTidurMenit
                            )
                            val shouldStopForToday = settings.stopSetelahTarget &&
                                settings.targetMl > 0 &&
                                todayTotalMl >= settings.targetMl

                            if (withinWakeSleepWindow && !shouldStopForToday) {
                                NotificationHelper.showWaterNotification(
                                    context = appContext,
                                    currentMl = todayTotalMl,
                                    targetMl = settings.targetMl
                                )
                            }

                            ReminderScheduler.scheduleWaterAlarm(
                                context = appContext,
                                settings = settings,
                                todayTotalMl = todayTotalMl,
                                nowMillis = nowMillis + 60_000L
                            )
                        }
                    }
                }
            } finally {
                runCatching {
                    if (wakeLock?.isHeld == true) {
                        wakeLock.release()
                    }
                }
                pendingResult.finish()
            }
        }
    }
}
