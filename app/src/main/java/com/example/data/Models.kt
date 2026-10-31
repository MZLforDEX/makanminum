package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

enum class ThemeMode(val label: String) {
    SISTEM("Ikuti Sistem"),
    TERANG("Terang"),
    GELAP("Gelap")
}

/**
 * Representasi entitas pengingat makan di database Room.
 * [hariAktif] menggunakan bitmask 7-bit:
 * Bit 0 (1) = Senin, Bit 1 (2) = Selasa, Bit 2 (4) = Rabu,
 * Bit 3 (8) = Kamis, Bit 4 (16) = Jumat, Bit 5 (32) = Sabtu, Bit 6 (64) = Minggu.
 * Nilai 127 (0b1111111) berarti aktif setiap hari.
 */
@Entity(tableName = "meal_reminders")
data class MealReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nama: String,
    val jam: Int,
    val menit: Int,
    val hariAktif: Int = ALL_DAYS_MASK,
    val aktif: Boolean = true
) {
    fun formattedTime(): String = String.format(Locale.getDefault(), "%02d:%02d", jam, menit)

    fun isActiveOnDayIndex(dayIndexZeroToSix: Int): Boolean {
        if (dayIndexZeroToSix !in 0..6) return false
        return (hariAktif and (1 shl dayIndexZeroToSix)) != 0
    }

    fun activeDaysSummary(): String {
        if (hariAktif == ALL_DAYS_MASK) return "Setiap hari"
        if (hariAktif == WEEKDAYS_MASK) return "Sen – Jum"
        if (hariAktif == WEEKEND_MASK) return "Sab – Min"
        if (hariAktif == 0) return "Tidak ada hari dipilih"
        return DAY_SHORT_NAMES.indices
            .filter { isActiveOnDayIndex(it) }
            .joinToString(", ") { DAY_SHORT_NAMES[it] }
    }

    companion object {
        const val ALL_DAYS_MASK = 0b1111111 // 127
        const val WEEKDAYS_MASK = 0b0011111 // 31 (Sen-Jum)
        const val WEEKEND_MASK = 0b1100000 // 96 (Sab-Min)

        val DAY_SHORT_NAMES = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
        val DAY_FULL_NAMES = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")

        fun toggleDayBit(currentMask: Int, dayIndex: Int): Int {
            return currentMask xor (1 shl dayIndex)
        }
    }
}

/**
 * Representasi catatan minum air putih di database Room.
 * [tanggal] berformat yyyy-MM-dd waktu lokal perangkat.
 */
@Entity(tableName = "water_logs")
data class WaterLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tanggal: String,
    val waktu: Long,
    val jumlahMl: Int
) {
    fun formattedTime(): String = DateTimeUtils.formatTimeMillis(waktu)
}

/**
 * Model pengaturan pengguna yang disimpan di Preferences DataStore.
 */
data class UserSettings(
    val onboardingSelesai: Boolean = false,
    val beratBadanKg: Int = 60,
    val targetMl: Int = 2000,
    val targetOtomatis: Boolean = true,
    val jamBangunMenit: Int = 6 * 60, // 06:00 -> 360 menit dari 00:00
    val jamTidurMenit: Int = 22 * 60, // 22:00 -> 1320 menit dari 00:00
    val intervalMenit: Int = 90, // 90 menit (1.5 jam)
    val pengingatMinumAktif: Boolean = true,
    val stopSetelahTarget: Boolean = true,
    val tema: ThemeMode = ThemeMode.SISTEM,
    val makanSelesaiHariIni: Set<String> = emptySet()
) {
    val jamBangunFormatted: String
        get() = DateTimeUtils.formatMinutesOfDay(jamBangunMenit)

    val jamTidurFormatted: String
        get() = DateTimeUtils.formatMinutesOfDay(jamTidurMenit)

    fun isMealDoneToday(mealId: Long, todayDate: String): Boolean {
        return makanSelesaiHariIni.contains("$todayDate:$mealId")
    }
}

/**
 * Ringkasan hidrasi satu hari untuk grafik 7 hari terakhir di tab Riwayat.
 */
data class DailyWaterSummary(
    val tanggal: String,
    val labelHariSingkat: String,
    val labelTanggalLengkap: String,
    val totalMl: Int,
    val targetMl: Int,
    val isToday: Boolean
) {
    val progressRatio: Float
        get() = if (targetMl <= 0) 0f else (totalMl.toFloat() / targetMl.toFloat()).coerceAtLeast(0f)

    val isTargetReached: Boolean
        get() = totalMl >= targetMl && targetMl > 0
}

/**
 * Informasi jadwal makan berikutnya untuk ditampilkan di kartu Beranda.
 */
data class NextMealInfo(
    val meal: MealReminderEntity,
    val triggerTimeMillis: Long,
    val isToday: Boolean,
    val countdownLabel: String,
    val isAlreadyAteToday: Boolean
)

object DateTimeUtils {
    private val dateFormatLock = Any()
    private val isoDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun calculateRecommendedWaterMl(weightKg: Int): Int {
        val clampedKg = weightKg.coerceIn(20, 250)
        // Rata-rata 33 ml x kg (di antara 30-35 ml x kg sesuai PRD), dibulatkan ke kelipatan 50 ml
        val raw = clampedKg * 33f
        return ((raw / 50f).roundToInt() * 50).coerceIn(1000, 6000)
    }

    fun todayDateString(nowMillis: Long = System.currentTimeMillis()): String {
        synchronized(dateFormatLock) {
            return isoDateFormatter.format(Date(nowMillis))
        }
    }

    fun formatTimeMillis(timeMillis: Long): String {
        synchronized(dateFormatLock) {
            return timeFormatter.format(Date(timeMillis))
        }
    }

    fun formatMinutesOfDay(totalMinutes: Int): String {
        val normalized = ((totalMinutes % 1440) + 1440) % 1440
        val h = normalized / 60
        val m = normalized % 60
        return String.format(Locale.getDefault(), "%02d:%02d", h, m)
    }

    /**
     * Mengonversi Calendar.DAY_OF_WEEK menjadi indeks 0..6 (0 = Senin, 6 = Minggu).
     */
    fun calendarDayToMondayZeroIndex(calendarDayOfWeek: Int): Int {
        return when (calendarDayOfWeek) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
    }

    /**
     * Menghasilkan daftar 7 tanggal terakhir (dari 6 hari lalu hingga hari ini)
     * beserta label hari Bahasa Indonesia.
     */
    fun getLast7DaysDates(nowMillis: Long = System.currentTimeMillis()): List<Triple<String, String, String>> {
        val monthNamesShort = arrayOf(
            "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
            "Jul", "Agu", "Sep", "Okt", "Nov", "Des"
        )
        val result = mutableListOf<Triple<String, String, String>>()
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -6)
        }

        for (i in 0 until 7) {
            val dateStr = todayDateString(cal.timeInMillis)
            val dayIdx = calendarDayToMondayZeroIndex(cal.get(Calendar.DAY_OF_WEEK))
            val shortDay = MealReminderEntity.DAY_SHORT_NAMES[dayIdx]
            val fullDay = MealReminderEntity.DAY_FULL_NAMES[dayIdx]
            val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
            val monthName = monthNamesShort[cal.get(Calendar.MONTH)]
            val fullLabel = if (i == 6) {
                "Hari Ini, $dayOfMonth $monthName"
            } else if (i == 5) {
                "Kemarin, $dayOfMonth $monthName"
            } else {
                "$fullDay, $dayOfMonth $monthName"
            }
            result.add(Triple(dateStr, shortDay, fullLabel))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return result
    }

    /**
     * Menghitung waktu kemunculan berikutnya (epoch ms) untuk suatu [MealReminderEntity]
     * setelah [nowMillis]. Mengembalikan null bila tidak aktif atau tidak ada hari yang dipilih.
     */
    fun computeNextMealTriggerMillis(
        meal: MealReminderEntity,
        nowMillis: Long = System.currentTimeMillis()
    ): Long? {
        if (!meal.aktif || meal.hariAktif == 0) return null

        val candidate = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.HOUR_OF_DAY, meal.jam)
            set(Calendar.MINUTE, meal.menit)
        }

        for (offsetDays in 0..7) {
            val checkCal = (candidate.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, offsetDays)
            }
            val dayIndex = calendarDayToMondayZeroIndex(checkCal.get(Calendar.DAY_OF_WEEK))
            if (meal.isActiveOnDayIndex(dayIndex) && checkCal.timeInMillis > nowMillis) {
                return checkCal.timeInMillis
            }
        }
        return null
    }

    /**
     * Mencari jadwal makan terdekat berikutnya untuk ditampilkan di kartu Beranda.
     */
    fun findNextMealInfo(
        meals: List<MealReminderEntity>,
        settings: UserSettings,
        nowMillis: Long = System.currentTimeMillis()
    ): NextMealInfo? {
        val todayStr = todayDateString(nowMillis)
        val activeMeals = meals.filter { it.aktif && it.hariAktif != 0 }
        if (activeMeals.isEmpty()) return null

        var bestMeal: MealReminderEntity? = null
        var bestTriggerMillis = Long.MAX_VALUE

        for (meal in activeMeals) {
            // Jika jadwal makan hari ini sudah ditandai "Sudah makan", cari jadwal berikutnya
            val nextMillis = computeNextMealTriggerMillis(meal, nowMillis) ?: continue
            val isTriggerToday = todayDateString(nextMillis) == todayStr
            val alreadyAte = isTriggerToday && settings.isMealDoneToday(meal.id, todayStr)

            val effectiveMillis = if (alreadyAte) {
                // Jadwalkan ke hari aktif berikutnya
                val endOfTodayCal = Calendar.getInstance().apply {
                    timeInMillis = nextMillis + 60_000L
                }
                computeNextMealTriggerMillis(meal, endOfTodayCal.timeInMillis) ?: nextMillis
            } else {
                nextMillis
            }

            if (effectiveMillis < bestTriggerMillis) {
                bestTriggerMillis = effectiveMillis
                bestMeal = meal
            }
        }

        val chosen = bestMeal ?: return null
        val isToday = todayDateString(bestTriggerMillis) == todayStr
        val diffMinutes = ((bestTriggerMillis - nowMillis) / 60_000L).coerceAtLeast(1L)
        val hours = diffMinutes / 60
        val mins = diffMinutes % 60
        val countdown = when {
            !isToday && hours >= 24 -> "Besok pukul ${chosen.formattedTime()}"
            !isToday -> "Besok (${hours}j ${mins}m lagi)"
            hours > 0 && mins > 0 -> "Dalam ${hours} jam ${mins} menit"
            hours > 0 -> "Dalam ${hours} jam"
            else -> "Dalam ${mins} menit"
        }

        return NextMealInfo(
            meal = chosen,
            triggerTimeMillis = bestTriggerMillis,
            isToday = isToday,
            countdownLabel = countdown,
            isAlreadyAteToday = settings.isMealDoneToday(chosen.id, todayStr)
        )
    }

    /**
     * Memeriksa apakah menit ke-[minutesOfDay] berada di antara [wakeMinutes] dan [sleepMinutes].
     * Mendukung jadwal normal (mis. 06:00 - 22:00) maupun jadwal lintas tengah malam.
     */
    fun isWithinWakeSleepWindow(
        minutesOfDay: Int,
        wakeMinutes: Int,
        sleepMinutes: Int
    ): Boolean {
        val safeWake = wakeMinutes.coerceIn(0, 1439)
        val safeSleep = sleepMinutes.coerceIn(0, 1439)
        val safeCurrent = minutesOfDay.coerceIn(0, 1439)
        return when {
            safeWake < safeSleep -> safeCurrent in safeWake..safeSleep
            safeWake > safeSleep -> safeCurrent >= safeWake || safeCurrent <= safeSleep
            else -> true
        }
    }

    /**
     * Menghitung waktu pengingat minum berikutnya di antara jam bangun dan jam tidur.
     * Jika target hari ini sudah tercapai dan [stopSetelahTarget] aktif, pengingat dijadwalkan
     * ke hari berikutnya setelah jam bangun.
     */
    fun computeNextWaterReminderMillis(
        settings: UserSettings,
        todayTotalMl: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): Long? {
        if (!settings.pengingatMinumAktif) return null
        val intervalMinutes = settings.intervalMenit.coerceIn(30, 240)
        val wakeMinutes = settings.jamBangunMenit.coerceIn(0, 1439)
        val sleepMinutes = settings.jamTidurMenit.coerceIn(0, 1439)

        val targetReachedToday = settings.stopSetelahTarget &&
            settings.targetMl > 0 &&
            todayTotalMl >= settings.targetMl

        val nowCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val currentMinutesOfDay = nowCal.get(Calendar.HOUR_OF_DAY) * 60 + nowCal.get(Calendar.MINUTE)
        val firstReminderAfterWake = if (wakeMinutes < sleepMinutes) {
            (wakeMinutes + intervalMinutes).coerceAtMost(sleepMinutes)
        } else {
            (wakeMinutes + intervalMinutes) % 1440
        }

        // Jika target hari ini sudah tercapai, jadwalkan untuk besok pada (jamBangun + interval)
        if (targetReachedToday) {
            return buildDayTimeMillis(nowMillis, dayOffset = 1, minutesOfDay = firstReminderAfterWake)
        }

        if (wakeMinutes < sleepMinutes) {
            if (currentMinutesOfDay < wakeMinutes) {
                return buildDayTimeMillis(nowMillis, dayOffset = 0, minutesOfDay = firstReminderAfterWake)
            }
            val nextCandidateMinutes = currentMinutesOfDay + intervalMinutes
            return if (nextCandidateMinutes <= sleepMinutes) {
                buildDayTimeMillis(nowMillis, dayOffset = 0, minutesOfDay = nextCandidateMinutes)
            } else {
                buildDayTimeMillis(nowMillis, dayOffset = 1, minutesOfDay = firstReminderAfterWake)
            }
        } else {
            // Rentang melewati tengah malam
            return if (isWithinWakeSleepWindow(currentMinutesOfDay, wakeMinutes, sleepMinutes)) {
                Calendar.getInstance().apply {
                    timeInMillis = nowMillis + intervalMinutes * 60_000L
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            } else {
                buildDayTimeMillis(nowMillis, dayOffset = 0, minutesOfDay = wakeMinutes)
            }
        }
    }

    private fun buildDayTimeMillis(baseMillis: Long, dayOffset: Int, minutesOfDay: Int): Long {
        val safeMinutes = minutesOfDay.coerceIn(0, 1439)
        return Calendar.getInstance().apply {
            timeInMillis = baseMillis
            add(Calendar.DAY_OF_YEAR, dayOffset)
            set(Calendar.HOUR_OF_DAY, safeMinutes / 60)
            set(Calendar.MINUTE, safeMinutes % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
