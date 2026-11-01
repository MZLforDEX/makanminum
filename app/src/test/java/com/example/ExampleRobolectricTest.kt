package com.example

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.DateTimeUtils
import com.example.data.MakanMinumDatabase
import com.example.data.MealReminderEntity
import com.example.data.UserSettings
import com.example.data.WaterLogEntity
import com.example.reminder.AlarmTriggerReceiver
import com.example.reminder.NotificationActionReceiver
import com.example.reminder.NotificationHelper
import com.example.reminder.ReminderScheduler
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var db: MakanMinumDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val app = context as Application
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        db = Room.inMemoryDatabaseBuilder(context, MakanMinumDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun appMetadataAndSecurityManifest_areConfiguredProperly() {
        val appName = context.getString(R.string.app_name)
        assertEquals("MakanMinum", appName)

        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_RECEIVERS
        )
        val receivers = packageInfo.receivers?.associateBy { it.name }.orEmpty()

        val alarmReceiver = receivers[AlarmTriggerReceiver::class.java.name]
        assertNotNull(alarmReceiver)
        assertFalse(alarmReceiver!!.exported)

        val actionReceiver = receivers[NotificationActionReceiver::class.java.name]
        assertNotNull(actionReceiver)
        assertFalse(actionReceiver!!.exported)
    }

    @Test
    fun roomDatabase_crudMealsAndWaterLogsWorkCorrectly() = runBlocking {
        val mealDao = db.mealReminderDao()
        val waterDao = db.waterLogDao()

        mealDao.insertMeals(MakanMinumDatabase.DEFAULT_MEAL_REMINDERS)
        val meals = mealDao.getAllMealsSync()
        assertEquals(3, meals.size)
        assertEquals("Sarapan", meals[0].nama)
        assertEquals("Makan Siang", meals[1].nama)
        assertEquals("Makan Malam", meals[2].nama)

        val today = DateTimeUtils.todayDateString()
        val now = System.currentTimeMillis()
        waterDao.insertWaterLog(WaterLogEntity(tanggal = today, waktu = now, jumlahMl = 250))
        waterDao.insertWaterLog(WaterLogEntity(tanggal = today, waktu = now + 1000L, jumlahMl = 500))

        val totalToday = waterDao.getTotalWaterForDateSync(today)
        assertEquals(750, totalToday)

        val latest = waterDao.getLatestWaterLogForDate(today)
        assertNotNull(latest)
        assertEquals(500, latest!!.jumlahMl)
        waterDao.deleteWaterLog(latest)

        val totalAfterUndo = waterDao.getTotalWaterForDateSync(today)
        assertEquals(250, totalAfterUndo)

        val tomorrowTotal = waterDao.getTotalWaterForDateSync("2099-01-01")
        assertEquals(0, tomorrowTotal)
    }

    @Test
    fun reminderSchedulerAndNotificationManager_deliverMealRemindersWithActions() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager = shadowOf(alarmManager)
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowNotificationManager = shadowOf(notificationManager)

        val sampleMeal = MealReminderEntity(
            id = 42L,
            nama = "Makan Siang",
            jam = 12,
            menit = 0,
            hariAktif = MealReminderEntity.ALL_DAYS_MASK,
            aktif = true
        )

        // 1. Verifikasi penjadwalan AlarmManager
        ReminderScheduler.scheduleMealAlarm(context, sampleMeal)
        assertTrue(shadowAlarmManager.scheduledAlarms.isNotEmpty())

        // 2. Verifikasi pengiriman notifikasi lewat NotificationManager beserta tombol aksi
        NotificationHelper.showMealNotification(
            context = context,
            mealId = sampleMeal.id,
            mealName = sampleMeal.nama,
            mealTimeFormatted = sampleMeal.formattedTime()
        )
        val expectedNotifId = NotificationHelper.mealNotificationId(sampleMeal.id)
        val postedNotification = shadowNotificationManager.getNotification(expectedNotifId)
        assertNotNull(postedNotification)
        assertNotNull(postedNotification.fullScreenIntent)
        assertEquals(2, postedNotification.actions.size)
        assertEquals("Sudah makan", postedNotification.actions[0].title.toString())
        assertEquals("Tunda 10 menit", postedNotification.actions[1].title.toString())

        // 3. Verifikasi pembatalan alarm makan sekaligus menutup notifikasi aktif
        ReminderScheduler.scheduleSnoozeMealAlarm(context, sampleMeal.id, sampleMeal.nama, 10)
        ReminderScheduler.cancelMealAlarm(context, sampleMeal.id)
        assertTrue(shadowAlarmManager.scheduledAlarms.isEmpty())

        // 4. Verifikasi penjadwalan dan pembatalan alarm minum
        val settings = UserSettings(pengingatMinumAktif = true, targetMl = 2000)
        ReminderScheduler.scheduleWaterAlarm(context, settings, todayTotalMl = 500)
        assertTrue(shadowAlarmManager.scheduledAlarms.isNotEmpty())

        ReminderScheduler.cancelWaterAlarm(context)
        assertTrue(shadowAlarmManager.scheduledAlarms.isEmpty())
    }
}
