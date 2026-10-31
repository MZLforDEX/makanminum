package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.userSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "makan_minum_settings"
)

class UserPreferencesDataStore(private val context: Context) {

    private object Keys {
        val ONBOARDING_SELESAI = booleanPreferencesKey("onboarding_selesai")
        val DEFAULT_MEALS_SEEDED = booleanPreferencesKey("default_meals_seeded")
        val BERAT_BADAN_KG = intPreferencesKey("berat_badan_kg")
        val TARGET_ML = intPreferencesKey("target_ml")
        val TARGET_OTOMATIS = booleanPreferencesKey("target_otomatis")
        val JAM_BANGUN_MENIT = intPreferencesKey("jam_bangun_menit")
        val JAM_TIDUR_MENIT = intPreferencesKey("jam_tidur_menit")
        val INTERVAL_MENIT = intPreferencesKey("interval_menit")
        val PENGINGAT_MINUM_AKTIF = booleanPreferencesKey("pengingat_minum_aktif")
        val STOP_SETELAH_TARGET = booleanPreferencesKey("stop_setelah_target")
        val TEMA = stringPreferencesKey("tema")
        val MAKAN_SELESAI_HARI_INI = stringSetPreferencesKey("makan_selesai_hari_ini")
    }

    val settingsFlow: Flow<UserSettings> = context.userSettingsDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val berat = prefs[Keys.BERAT_BADAN_KG] ?: 60
            val otomatis = prefs[Keys.TARGET_OTOMATIS] ?: true
            val defaultTarget = DateTimeUtils.calculateRecommendedWaterMl(berat)
            val target = prefs[Keys.TARGET_ML] ?: defaultTarget
            val temaStr = prefs[Keys.TEMA] ?: ThemeMode.SISTEM.name
            val temaMode = runCatching { ThemeMode.valueOf(temaStr) }.getOrDefault(ThemeMode.SISTEM)

            UserSettings(
                onboardingSelesai = prefs[Keys.ONBOARDING_SELESAI] ?: false,
                beratBadanKg = berat,
                targetMl = target,
                targetOtomatis = otomatis,
                jamBangunMenit = prefs[Keys.JAM_BANGUN_MENIT] ?: (6 * 60),
                jamTidurMenit = prefs[Keys.JAM_TIDUR_MENIT] ?: (22 * 60),
                intervalMenit = prefs[Keys.INTERVAL_MENIT] ?: 90,
                pengingatMinumAktif = prefs[Keys.PENGINGAT_MINUM_AKTIF] ?: true,
                stopSetelahTarget = prefs[Keys.STOP_SETELAH_TARGET] ?: true,
                tema = temaMode,
                makanSelesaiHariIni = prefs[Keys.MAKAN_SELESAI_HARI_INI] ?: emptySet()
            )
        }

    suspend fun getSettingsSync(): UserSettings = settingsFlow.first()

    suspend fun isDefaultMealsSeeded(): Boolean {
        val prefs = context.userSettingsDataStore.data.first()
        return prefs[Keys.DEFAULT_MEALS_SEEDED] ?: false
    }

    suspend fun markDefaultMealsSeeded() {
        context.userSettingsDataStore.edit { prefs ->
            prefs[Keys.DEFAULT_MEALS_SEEDED] = true
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
        context.userSettingsDataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_SELESAI] = true
            prefs[Keys.BERAT_BADAN_KG] = beratBadanKg.coerceIn(20, 250)
            prefs[Keys.TARGET_ML] = targetMl.coerceIn(500, 8000)
            prefs[Keys.TARGET_OTOMATIS] = targetOtomatis
            prefs[Keys.JAM_BANGUN_MENIT] = jamBangunMenit.coerceIn(0, 1439)
            prefs[Keys.JAM_TIDUR_MENIT] = jamTidurMenit.coerceIn(0, 1439)
            prefs[Keys.INTERVAL_MENIT] = intervalMenit.coerceIn(30, 240)
            prefs[Keys.PENGINGAT_MINUM_AKTIF] = true
        }
    }

    suspend fun updateWeightAndTarget(
        beratBadanKg: Int,
        targetMl: Int,
        targetOtomatis: Boolean
    ) {
        context.userSettingsDataStore.edit { prefs ->
            val clampedWeight = beratBadanKg.coerceIn(20, 250)
            prefs[Keys.BERAT_BADAN_KG] = clampedWeight
            prefs[Keys.TARGET_OTOMATIS] = targetOtomatis
            prefs[Keys.TARGET_ML] = if (targetOtomatis) {
                DateTimeUtils.calculateRecommendedWaterMl(clampedWeight)
            } else {
                targetMl.coerceIn(500, 8000)
            }
        }
    }

    suspend fun updateWaterScheduleSettings(
        jamBangunMenit: Int,
        jamTidurMenit: Int,
        intervalMenit: Int,
        pengingatAktif: Boolean,
        stopSetelahTarget: Boolean
    ) {
        context.userSettingsDataStore.edit { prefs ->
            prefs[Keys.JAM_BANGUN_MENIT] = jamBangunMenit.coerceIn(0, 1439)
            prefs[Keys.JAM_TIDUR_MENIT] = jamTidurMenit.coerceIn(0, 1439)
            prefs[Keys.INTERVAL_MENIT] = intervalMenit.coerceIn(30, 240)
            prefs[Keys.PENGINGAT_MINUM_AKTIF] = pengingatAktif
            prefs[Keys.STOP_SETELAH_TARGET] = stopSetelahTarget
        }
    }

    suspend fun updateTheme(themeMode: ThemeMode) {
        context.userSettingsDataStore.edit { prefs ->
            prefs[Keys.TEMA] = themeMode.name
        }
    }

    suspend fun markMealDoneForDate(mealId: Long, dateString: String) {
        context.userSettingsDataStore.edit { prefs ->
            val currentSet = prefs[Keys.MAKAN_SELESAI_HARI_INI] ?: emptySet()
            // Simpan hanya entri untuk tanggal hari ini agar set tetap ramping
            val filteredToday = currentSet.filter { it.startsWith("$dateString:") }.toMutableSet()
            filteredToday.add("$dateString:$mealId")
            prefs[Keys.MAKAN_SELESAI_HARI_INI] = filteredToday
        }
    }
}
