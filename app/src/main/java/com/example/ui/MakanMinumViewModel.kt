package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.DailyWaterSummary
import com.example.data.DateTimeUtils
import com.example.data.MakanMinumRepository
import com.example.data.MealReminderEntity
import com.example.data.NextMealInfo
import com.example.data.ThemeMode
import com.example.data.UserSettings
import com.example.data.WaterLogEntity
import com.example.reminder.NotificationHelper
import com.example.reminder.ReminderScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MakanMinumUiState(
    val isLoading: Boolean = true,
    val todayDate: String = DateTimeUtils.todayDateString(),
    val settings: UserSettings = UserSettings(),
    val meals: List<MealReminderEntity> = emptyList(),
    val todayWaterLogs: List<WaterLogEntity> = emptyList(),
    val todayTotalMl: Int = 0,
    val last7DaysSummaries: List<DailyWaterSummary> = emptyList(),
    val selectedHistoryDate: String = DateTimeUtils.todayDateString(),
    val selectedDateLogs: List<WaterLogEntity> = emptyList(),
    val nextMealInfo: NextMealInfo? = null,
    val nextWaterReminderFormatted: String? = null,
    val snackbarMessage: String? = null
) {
    val todayProgressFraction: Float
        get() = if (settings.targetMl <= 0) 0f else (todayTotalMl.toFloat() / settings.targetMl.toFloat()).coerceAtLeast(0f)

    val todayProgressPercent: Int
        get() = (todayProgressFraction * 100f).toInt()

    val remainingMlToday: Int
        get() = (settings.targetMl - todayTotalMl).coerceAtLeast(0)
}

@OptIn(ExperimentalCoroutinesApi::class)
class MakanMinumViewModel(
    application: Application,
    private val repository: MakanMinumRepository
) : AndroidViewModel(application) {

    private val currentTimeMillisFlow = MutableStateFlow(System.currentTimeMillis())
    private val todayDateFlow = MutableStateFlow(DateTimeUtils.todayDateString())
    private val selectedHistoryDateFlow = MutableStateFlow(DateTimeUtils.todayDateString())
    private val snackbarMessageFlow = MutableStateFlow<String?>(null)
    private val isInitializedFlow = MutableStateFlow(false)

    init {
        NotificationHelper.ensureChannelsCreated(application)
        viewModelScope.launch {
            repository.ensureDefaultMealsSeededAndSyncAlarms()
            isInitializedFlow.value = true
        }
        viewModelScope.launch {
            while (true) {
                delay(30_000L)
                val now = System.currentTimeMillis()
                currentTimeMillisFlow.value = now
                val newToday = DateTimeUtils.todayDateString(now)
                if (newToday != todayDateFlow.value) {
                    val oldToday = todayDateFlow.value
                    todayDateFlow.value = newToday
                    if (selectedHistoryDateFlow.value == oldToday) {
                        selectedHistoryDateFlow.value = newToday
                    }
                    repository.rescheduleAllAlarms(now)
                }
            }
        }
    }

    private val logsSince7DaysFlow = todayDateFlow.flatMapLatest { _ ->
        val sevenDays = DateTimeUtils.getLast7DaysDates(currentTimeMillisFlow.value)
        val startDate = sevenDays.firstOrNull()?.first ?: DateTimeUtils.todayDateString()
        repository.observeLogsSince(startDate)
    }

    private val coreDataFlow = combine(
        repository.settingsFlow,
        repository.allMealsFlow,
        logsSince7DaysFlow,
        todayDateFlow,
        currentTimeMillisFlow
    ) { settings, meals, logs7Days, todayDate, nowMillis ->
        CoreDataSnapshot(
            settings = settings,
            meals = meals,
            logs7Days = logs7Days,
            todayDate = todayDate,
            nowMillis = nowMillis
        )
    }

    val uiState: StateFlow<MakanMinumUiState> = combine(
        coreDataFlow,
        selectedHistoryDateFlow,
        snackbarMessageFlow,
        isInitializedFlow
    ) { core, selectedDate, snackbar, isInitialized ->
        val logsByDate = core.logs7Days.groupBy { it.tanggal }
        val todayLogs = logsByDate[core.todayDate].orEmpty()
        val todayTotal = todayLogs.sumOf { it.jumlahMl }

        val sevenDaysMeta = DateTimeUtils.getLast7DaysDates(core.nowMillis)
        val summaries = sevenDaysMeta.map { (dateStr, shortDay, fullLabel) ->
            val dayTotal = logsByDate[dateStr]?.sumOf { it.jumlahMl } ?: 0
            DailyWaterSummary(
                tanggal = dateStr,
                labelHariSingkat = shortDay,
                labelTanggalLengkap = fullLabel,
                totalMl = dayTotal,
                targetMl = core.settings.targetMl,
                isToday = dateStr == core.todayDate
            )
        }

        val validSelectedDate = if (summaries.any { it.tanggal == selectedDate }) {
            selectedDate
        } else {
            core.todayDate
        }

        val nextMeal = DateTimeUtils.findNextMealInfo(
            meals = core.meals,
            settings = core.settings,
            nowMillis = core.nowMillis
        )

        val nextWaterMillis = DateTimeUtils.computeNextWaterReminderMillis(
            settings = core.settings,
            todayTotalMl = todayTotal,
            nowMillis = core.nowMillis
        )
        val nextWaterFormatted = nextWaterMillis?.let { millis ->
            val isToday = DateTimeUtils.todayDateString(millis) == core.todayDate
            val timeStr = DateTimeUtils.formatTimeMillis(millis)
            if (isToday) "Hari ini pukul $timeStr" else "Besok pukul $timeStr"
        }

        MakanMinumUiState(
            isLoading = !isInitialized,
            todayDate = core.todayDate,
            settings = core.settings,
            meals = core.meals,
            todayWaterLogs = todayLogs,
            todayTotalMl = todayTotal,
            last7DaysSummaries = summaries,
            selectedHistoryDate = validSelectedDate,
            selectedDateLogs = logsByDate[validSelectedDate].orEmpty(),
            nextMealInfo = nextMeal,
            nextWaterReminderFormatted = nextWaterFormatted,
            snackbarMessage = snackbar
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MakanMinumUiState()
    )

    fun refreshTimeState() {
        val now = System.currentTimeMillis()
        currentTimeMillisFlow.value = now
        val today = DateTimeUtils.todayDateString(now)
        if (today != todayDateFlow.value) {
            todayDateFlow.value = today
            selectedHistoryDateFlow.value = today
        }
    }

    fun completeOnboarding(
        beratBadanKg: Int,
        targetMl: Int,
        targetOtomatis: Boolean,
        jamBangunMenit: Int,
        jamTidurMenit: Int,
        intervalMenit: Int
    ) {
        viewModelScope.launch {
            repository.completeOnboarding(
                beratBadanKg = beratBadanKg,
                targetMl = targetMl,
                targetOtomatis = targetOtomatis,
                jamBangunMenit = jamBangunMenit,
                jamTidurMenit = jamTidurMenit,
                intervalMenit = intervalMenit
            )
            refreshTimeState()
            snackbarMessageFlow.value = "Profil dan jadwal pengingat berhasil disimpan!"
        }
    }

    fun addWater(amountMl: Int) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            refreshTimeState()
            repository.addWaterLog(amountMl, now)
            snackbarMessageFlow.value = "+$amountMl ml air berhasil dicatat"
        }
    }

    fun undoLatestWaterToday() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val removed = repository.undoLatestWaterLogToday(now)
            if (removed != null) {
                snackbarMessageFlow.value = "Catatan terakhir (${removed.jumlahMl} ml) dibatalkan"
            }
        }
    }

    fun deleteWaterLog(log: WaterLogEntity) {
        viewModelScope.launch {
            repository.deleteWaterLog(log)
            snackbarMessageFlow.value = "Catatan ${log.jumlahMl} ml dihapus"
        }
    }

    fun saveMealReminder(meal: MealReminderEntity) {
        viewModelScope.launch {
            val isNew = meal.id == 0L
            repository.saveMealReminder(meal)
            refreshTimeState()
            snackbarMessageFlow.value = if (isNew) {
                "Pengingat \"${meal.nama}\" ditambahkan"
            } else {
                "Pengingat \"${meal.nama}\" diperbarui"
            }
        }
    }

    fun toggleMealActive(meal: MealReminderEntity, active: Boolean) {
        viewModelScope.launch {
            repository.toggleMealActive(meal, active)
            refreshTimeState()
        }
    }

    fun deleteMealReminder(meal: MealReminderEntity) {
        viewModelScope.launch {
            repository.deleteMealReminder(meal)
            refreshTimeState()
            snackbarMessageFlow.value = "Pengingat \"${meal.nama}\" dihapus"
        }
    }

    fun markMealDoneToday(meal: MealReminderEntity) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            repository.markMealDoneToday(meal.id, now)
            refreshTimeState()
            snackbarMessageFlow.value = "Selamat makan! \"${meal.nama}\" tercatat selesai hari ini."
        }
    }

    fun snoozeMeal10Minutes(meal: MealReminderEntity) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            ReminderScheduler.scheduleSnoozeMealAlarm(
                context = getApplication(),
                mealId = meal.id,
                mealName = meal.nama,
                delayMinutes = 10,
                nowMillis = now
            )
            snackbarMessageFlow.value = "Pengingat \"${meal.nama}\" ditunda 10 menit"
        }
    }

    fun confirmActiveAlarmDone(info: com.example.reminder.ActiveAlarmInfo) {
        viewModelScope.launch {
            val appContext = getApplication<Application>()
            val now = System.currentTimeMillis()
            repository.markMealDoneToday(info.mealId, now)
            com.example.reminder.AlarmRingingController.stopRinging(appContext)
            NotificationHelper.dismissNotification(appContext, info.notificationId)
            refreshTimeState()
            snackbarMessageFlow.value = "Selamat makan! \"${info.mealName}\" tercatat selesai hari ini."
        }
    }

    fun snoozeActiveAlarm10Minutes(info: com.example.reminder.ActiveAlarmInfo) {
        viewModelScope.launch {
            val appContext = getApplication<Application>()
            val now = System.currentTimeMillis()
            ReminderScheduler.scheduleSnoozeMealAlarm(
                context = appContext,
                mealId = info.mealId,
                mealName = info.mealName,
                delayMinutes = 10,
                nowMillis = now
            )
            com.example.reminder.AlarmRingingController.stopRinging(appContext)
            NotificationHelper.dismissNotification(appContext, info.notificationId)
            snackbarMessageFlow.value = "Alarm \"${info.mealName}\" ditunda 10 menit"
        }
    }

    fun dismissActiveAlarmOnly(info: com.example.reminder.ActiveAlarmInfo) {
        val appContext = getApplication<Application>()
        com.example.reminder.AlarmRingingController.stopRinging(appContext)
        NotificationHelper.dismissNotification(appContext, info.notificationId)
        snackbarMessageFlow.value = "Alarm \"${info.mealName}\" dimatikan"
    }

    fun selectHistoryDate(dateStr: String) {
        selectedHistoryDateFlow.value = dateStr
    }

    fun updateWeightAndTarget(
        beratBadanKg: Int,
        targetMl: Int,
        targetOtomatis: Boolean
    ) {
        viewModelScope.launch {
            repository.updateWeightAndTarget(beratBadanKg, targetMl, targetOtomatis)
            refreshTimeState()
            snackbarMessageFlow.value = "Target minum harian diperbarui"
        }
    }

    fun updateWaterScheduleSettings(
        jamBangunMenit: Int,
        jamTidurMenit: Int,
        intervalMenit: Int,
        pengingatAktif: Boolean,
        stopSetelahTarget: Boolean
    ) {
        viewModelScope.launch {
            repository.updateWaterScheduleSettings(
                jamBangunMenit = jamBangunMenit,
                jamTidurMenit = jamTidurMenit,
                intervalMenit = intervalMenit,
                pengingatAktif = pengingatAktif,
                stopSetelahTarget = stopSetelahTarget
            )
            refreshTimeState()
            snackbarMessageFlow.value = "Pengaturan pengingat minum disimpan"
        }
    }

    fun updateTheme(themeMode: ThemeMode) {
        viewModelScope.launch {
            repository.updateTheme(themeMode)
        }
    }

    fun sendTestWaterNotification() {
        val state = uiState.value
        val appContext = getApplication<Application>()
        if (!NotificationHelper.hasNotificationPermission(appContext)) {
            snackbarMessageFlow.value = "Aktifkan izin notifikasi terlebih dahulu"
            return
        }
        NotificationHelper.showWaterNotification(
            context = appContext,
            currentMl = state.todayTotalMl,
            targetMl = state.settings.targetMl
        )
        snackbarMessageFlow.value = "Notifikasi pengingat minum dikirim"
    }

    fun sendTestMealNotification(specificMeal: MealReminderEntity? = null) {
        val state = uiState.value
        val appContext = getApplication<Application>()
        val sampleMeal = specificMeal ?: state.meals.firstOrNull() ?: MealReminderEntity(
            id = 1L,
            nama = "Makan Siang",
            jam = 12,
            menit = 0
        )
        NotificationHelper.showMealNotification(
            context = appContext,
            mealId = sampleMeal.id,
            mealName = sampleMeal.nama,
            mealTimeFormatted = sampleMeal.formattedTime(),
            launchAlarmScreen = true
        )
    }

    fun clearSnackbar() {
        snackbarMessageFlow.value = null
    }

    private data class CoreDataSnapshot(
        val settings: UserSettings,
        val meals: List<MealReminderEntity>,
        val logs7Days: List<WaterLogEntity>,
        val todayDate: String,
        val nowMillis: Long
    )

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application)
                val repo = MakanMinumRepository.getInstance(app)
                MakanMinumViewModel(app, repo)
            }
        }
    }
}
