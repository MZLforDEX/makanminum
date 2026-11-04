package com.example

import com.example.data.DateTimeUtils
import com.example.data.MealReminderEntity
import com.example.data.UserSettings
import com.example.data.WaterLogEntity
import com.example.reminder.NotificationHelper
import com.example.reminder.ReminderScheduler
import com.example.util.BrandCompatibilityHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ExampleUnitTest {
    @Test
    fun calculateRecommendedWaterMl_computesWithin30To35MlPerKgAndClampsBounds() {
        val recommended60Kg = DateTimeUtils.calculateRecommendedWaterMl(60)
        // 60 * 33 = 1980 -> dibulatkan ke kelipatan 50 terdekat = 2000 ml
        assertEquals(2000, recommended60Kg)

        val recommended70Kg = DateTimeUtils.calculateRecommendedWaterMl(70)
        // 70 * 33 = 2310 -> 2300 ml
        assertEquals(2300, recommended70Kg)

        // Uji batas ekstrem (input di luar kewajaran harus di-clamp aman 1000..6000 ml)
        assertTrue(DateTimeUtils.calculateRecommendedWaterMl(-10) >= 1000)
        assertTrue(DateTimeUtils.calculateRecommendedWaterMl(999) <= 6000)
    }

    @Test
    fun isWithinWakeSleepWindow_handlesDaytimeAndOvernightWindows() {
        val wake0600 = 6 * 60
        val sleep2200 = 22 * 60
        assertTrue(DateTimeUtils.isWithinWakeSleepWindow(12 * 60, wake0600, sleep2200))
        assertFalse(DateTimeUtils.isWithinWakeSleepWindow(5 * 60, wake0600, sleep2200))
        assertFalse(DateTimeUtils.isWithinWakeSleepWindow(23 * 60, wake0600, sleep2200))

        // Jadwal lintas tengah malam (mis. bangun 20:00 malam, tidur 04:00 pagi)
        val wake2000 = 20 * 60
        val sleep0400 = 4 * 60
        assertTrue(DateTimeUtils.isWithinWakeSleepWindow(22 * 60, wake2000, sleep0400))
        assertTrue(DateTimeUtils.isWithinWakeSleepWindow(2 * 60, wake2000, sleep0400))
        assertFalse(DateTimeUtils.isWithinWakeSleepWindow(12 * 60, wake2000, sleep0400))
    }

    @Test
    fun computeNextWaterReminderMillis_stopsForTodayWhenTargetReached() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 31, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val settings = UserSettings(
            targetMl = 2000,
            jamBangunMenit = 6 * 60,
            jamTidurMenit = 22 * 60,
            intervalMenit = 60,
            pengingatMinumAktif = true,
            stopSetelahTarget = true
        )

        // Saat belum tercapai (1000 ml), pengingat berikutnya adalah hari yang sama (+60 menit)
        val nextWhenUnderTarget = DateTimeUtils.computeNextWaterReminderMillis(
            settings = settings,
            todayTotalMl = 1000,
            nowMillis = cal.timeInMillis
        )
        assertNotNull(nextWhenUnderTarget)
        assertEquals(
            DateTimeUtils.todayDateString(cal.timeInMillis),
            DateTimeUtils.todayDateString(nextWhenUnderTarget!!)
        )

        // Saat sudah mencapai target (2000 ml), pengingat dijadwalkan untuk hari esok
        val nextWhenTargetMet = DateTimeUtils.computeNextWaterReminderMillis(
            settings = settings,
            todayTotalMl = 2000,
            nowMillis = cal.timeInMillis
        )
        assertNotNull(nextWhenTargetMet)
        assertTrue(nextWhenTargetMet!! > cal.timeInMillis + 12 * 3600_000L)

        // Saat pengingat dinonaktifkan, harus mengembalikan null
        assertNull(
            DateTimeUtils.computeNextWaterReminderMillis(
                settings = settings.copy(pengingatMinumAktif = false),
                todayTotalMl = 500,
                nowMillis = cal.timeInMillis
            )
        )
    }

    @Test
    fun mealReminderEntity_dayBitmaskAndNextTriggerWorkCorrectly() {
        val meal = MealReminderEntity(
            id = 1L,
            nama = "Sarapan",
            jam = 7,
            menit = 0,
            hariAktif = MealReminderEntity.WEEKDAYS_MASK,
            aktif = true
        )
        assertEquals("Sen – Jum", meal.activeDaysSummary())
        assertTrue(meal.isActiveOnDayIndex(0)) // Senin
        assertTrue(meal.isActiveOnDayIndex(4)) // Jumat
        assertFalse(meal.isActiveOnDayIndex(5)) // Sabtu

        // Jika tidak ada hari aktif yang dipilih (0), computeNextMealTriggerMillis harus null
        val noDayMeal = meal.copy(hariAktif = 0)
        assertNull(DateTimeUtils.computeNextMealTriggerMillis(noDayMeal))
    }

    @Test
    fun brandCompatibilityHelper_providesValidTipsForSupportedBrands() {
        val tips = BrandCompatibilityHelper.getCompatibilityTips()
        assertTrue(tips.isNotEmpty())
        assertTrue(tips.size >= 3)

        val brandLabel = BrandCompatibilityHelper.getBrandNameLabel()
        assertTrue(brandLabel.isNotEmpty())

        val deviceDisplay = BrandCompatibilityHelper.getDeviceDisplayName()
        assertTrue(deviceDisplay.isNotEmpty())
    }

    @Test
    fun notificationHelper_constantsAndIdsAreValid() {
        assertEquals("meal_alarm_channel_v3", NotificationHelper.CHANNEL_MEAL)
        assertEquals("water_reminder_channel_v3", NotificationHelper.CHANNEL_WATER)
        assertEquals(2001, NotificationHelper.WATER_NOTIFICATION_ID)

        val meal10NotifId = NotificationHelper.mealNotificationId(10L)
        assertEquals(1010, meal10NotifId)
    }

    @Test
    fun waterLogEntity_andUserSettings_modelPropertiesWorkCorrectly() {
        val entity = WaterLogEntity(
            id = 100L,
            tanggal = "2026-11-03",
            waktu = 1700000000000L,
            jumlahMl = 250
        )
        assertEquals(250, entity.jumlahMl)
        assertEquals("2026-11-03", entity.tanggal)

        val settings = UserSettings(
            beratBadanKg = 65,
            targetMl = 2150,
            jamBangunMenit = 7 * 60,
            jamTidurMenit = 23 * 60,
            intervalMenit = 90
        )
        assertEquals(65, settings.beratBadanKg)
        assertEquals(2150, settings.targetMl)
        assertFalse(settings.isMealDoneToday(1L, "2026-11-03"))
    }
}
