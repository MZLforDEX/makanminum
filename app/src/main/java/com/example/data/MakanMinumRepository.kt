package com.example.data

import android.content.Context
import com.example.reminder.ReminderScheduler
import kotlinx.coroutines.flow.Flow

class MakanMinumRepository(
    private val context: Context,
    private val mealDao: MealReminderDao,
    private val waterDao: WaterLogDao,
    private val preferences: UserPreferencesDataStore
) {
    val allMealsFlow: Flow<List<MealReminderEntity>> = mealDao.observeAllMeals()
    val settingsFlow: Flow<UserSettings> = preferences.settingsFlow

    fun observeLogsForDate(tanggal: String): Flow<List<WaterLogEntity>> =
        waterDao.observeLogsForDate(tanggal)

    fun observeLogsSince(startDate: String): Flow<List<WaterLogEntity>> =
        waterDao.observeLogsSince(startDate)

    suspend fun ensureDefaultMealsSeededAndSyncAlarms() {
        if (!preferences.isDefaultMealsSeeded()) {
            if (mealDao.countMeals() == 0) {
                mealDao.insertMeals(MakanMinumDatabase.DEFAULT_MEAL_REMINDERS)
            }
            preferences.markDefaultMealsSeeded()
        }
        rescheduleAllAlarms()
    }

    suspend fun addWaterLog(amountMl: Int, nowMillis: Long = System.currentTimeMillis()): WaterLogEntity {
        val safeAmount = amountMl.coerceIn(10, 5000)
        val today = DateTimeUtils.todayDateString(nowMillis)
        val entity = WaterLogEntity(
            tanggal = today,
            waktu = nowMillis,
            jumlahMl = safeAmount
        )
        val insertedId = waterDao.insertWaterLog(entity)
        rescheduleWaterAlarmOnly(nowMillis)
        return entity.copy(id = insertedId)
    }

    suspend fun undoLatestWaterLogToday(nowMillis: Long = System.currentTimeMillis()): WaterLogEntity? {
        val today = DateTimeUtils.todayDateString(nowMillis)
        val latest = waterDao.getLatestWaterLogForDate(today) ?: return null
        waterDao.deleteWaterLog(latest)
        rescheduleWaterAlarmOnly(nowMillis)
        return latest
    }

    suspend fun deleteWaterLog(log: WaterLogEntity) {
        waterDao.deleteWaterLog(log)
        rescheduleWaterAlarmOnly()
    }

    suspend fun saveMealReminder(meal: MealReminderEntity): Long {
        val id = if (meal.id == 0L) {
            mealDao.insertMeal(meal)
        } else {
            mealDao.updateMeal(meal)
            meal.id
        }
        val saved = meal.copy(id = id)
        if (saved.aktif && saved.hariAktif != 0) {
            ReminderScheduler.scheduleMealAlarm(context, saved)
        } else {
            ReminderScheduler.cancelMealAlarm(context, saved.id)
        }
        return id
    }

    suspend fun toggleMealActive(meal: MealReminderEntity, active: Boolean) {
        val updated = meal.copy(aktif = active)
        mealDao.updateMeal(updated)
        if (updated.aktif && updated.hariAktif != 0) {
            ReminderScheduler.scheduleMealAlarm(context, updated)
        } else {
            ReminderScheduler.cancelMealAlarm(context, updated.id)
        }
    }

    suspend fun deleteMealReminder(meal: MealReminderEntity) {
        mealDao.deleteMeal(meal)
        ReminderScheduler.cancelMealAlarm(context, meal.id)
    }

    suspend fun markMealDoneToday(mealId: Long, nowMillis: Long = System.currentTimeMillis()) {
        val today = DateTimeUtils.todayDateString(nowMillis)
        preferences.markMealDoneForDate(mealId, today)
        val meal = mealDao.getMealById(mealId)
        if (meal != null && meal.aktif) {
            ReminderScheduler.scheduleMealAlarm(context, meal, nowMillis + 60_000L)
        }
    }

    suspend fun completeOnboarding(
        beratBadanKg: Int,
        targetMl: Int,
        targetOtomatis: Boolean,
        jamBangunMenit: Int,
        jamTidurMenit: Int,
        intervalMenit: Int
    ) {
        preferences.completeOnboarding(
            beratBadanKg = beratBadanKg,
            targetMl = targetMl,
            targetOtomatis = targetOtomatis,
            jamBangunMenit = jamBangunMenit,
            jamTidurMenit = jamTidurMenit,
            intervalMenit = intervalMenit
        )
        rescheduleAllAlarms()
    }

    suspend fun updateWeightAndTarget(
        beratBadanKg: Int,
        targetMl: Int,
        targetOtomatis: Boolean
    ) {
        preferences.updateWeightAndTarget(beratBadanKg, targetMl, targetOtomatis)
        rescheduleWaterAlarmOnly()
    }

    suspend fun updateWaterScheduleSettings(
        jamBangunMenit: Int,
        jamTidurMenit: Int,
        intervalMenit: Int,
        pengingatAktif: Boolean,
        stopSetelahTarget: Boolean
    ) {
        preferences.updateWaterScheduleSettings(
            jamBangunMenit = jamBangunMenit,
            jamTidurMenit = jamTidurMenit,
            intervalMenit = intervalMenit,
            pengingatAktif = pengingatAktif,
            stopSetelahTarget = stopSetelahTarget
        )
        rescheduleWaterAlarmOnly()
    }

    suspend fun updateTheme(themeMode: ThemeMode) {
        preferences.updateTheme(themeMode)
    }

    suspend fun rescheduleWaterAlarmOnly(nowMillis: Long = System.currentTimeMillis()) {
        val settings = preferences.getSettingsSync()
        val today = DateTimeUtils.todayDateString(nowMillis)
        val todayTotalMl = waterDao.getTotalWaterForDateSync(today)
        ReminderScheduler.scheduleWaterAlarm(context, settings, todayTotalMl, nowMillis)
    }

    suspend fun rescheduleAllAlarms(nowMillis: Long = System.currentTimeMillis()) {
        val meals = mealDao.getAllMealsSync()
        meals.forEach { meal ->
            if (meal.aktif && meal.hariAktif != 0) {
                ReminderScheduler.scheduleMealAlarm(context, meal, nowMillis)
            } else {
                ReminderScheduler.cancelMealAlarm(context, meal.id)
            }
        }
        rescheduleWaterAlarmOnly(nowMillis)
    }

    companion object {
        @Volatile
        private var INSTANCE: MakanMinumRepository? = null

        fun getInstance(context: Context): MakanMinumRepository {
            return INSTANCE ?: synchronized(this) {
                val appContext = context.applicationContext
                val db = MakanMinumDatabase.getInstance(appContext)
                val prefs = UserPreferencesDataStore(appContext)
                MakanMinumRepository(
                    context = appContext,
                    mealDao = db.mealReminderDao(),
                    waterDao = db.waterLogDao(),
                    preferences = prefs
                ).also { INSTANCE = it }
            }
        }
    }
}
