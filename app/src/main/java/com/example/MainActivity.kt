package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.reminder.AlarmRingingController
import com.example.ui.MakanMinumUiState
import com.example.ui.MakanMinumViewModel
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MealScheduleScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MakanMinumTheme

enum class MakanMinumTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    BERANDA("Beranda", Icons.Filled.Home, Icons.Outlined.Home, "nav_tab_home"),
    MAKAN("Makan", Icons.Filled.Restaurant, Icons.Outlined.Restaurant, "nav_tab_meals"),
    RIWAYAT("Riwayat", Icons.Filled.BarChart, Icons.Outlined.BarChart, "nav_tab_history"),
    PENGATURAN("Pengaturan", Icons.Filled.Settings, Icons.Outlined.Settings, "nav_tab_settings")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.reminder.NotificationHelper.ensureChannelsCreated(this)
        enableEdgeToEdge()
        setContent {
            val viewModel: MakanMinumViewModel = viewModel(factory = MakanMinumViewModel.Factory)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            MakanMinumTheme(themeMode = uiState.settings.tema) {
                MakanMinumApp(
                    uiState = uiState,
                    viewModel = viewModel
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MakanMinumApp(
    uiState: MakanMinumUiState,
    viewModel: MakanMinumViewModel
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var currentTab by rememberSaveable { mutableStateOf(MakanMinumTab.BERANDA) }
    val activeAlarm by AlarmRingingController.activeAlarm.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            viewModel.clearSnackbar()
        }
    }

    if (activeAlarm != null) {
        val alarm = activeAlarm!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissActiveAlarmOnly(alarm) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Alarm,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Waktunya ${alarm.mealName}! (${alarm.mealTimeFormatted})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Alarm jadwal ${alarm.mealName} sedang berbunyi. Jangan lewatkan jam makan Anda agar tubuh tetap sehat dan berenergi.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = { viewModel.confirmActiveAlarmDone(alarm) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("in_app_alarm_done_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Sudah Makan (Matikan Alarm)")
                    }
                    OutlinedButton(
                        onClick = { viewModel.snoozeActiveAlarm10Minutes(alarm) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("in_app_alarm_snooze_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Tunda 10 Menit (Snooze)")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissActiveAlarmOnly(alarm) },
                    modifier = Modifier.testTag("in_app_alarm_dismiss_button")
                ) {
                    Text("Matikan Alarm Saja")
                }
            }
        )
    }

    if (uiState.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (!uiState.settings.onboardingSelesai) {
        OnboardingScreen(
            initialWeightKg = uiState.settings.beratBadanKg,
            initialWakeMinutes = uiState.settings.jamBangunMenit,
            initialSleepMinutes = uiState.settings.jamTidurMenit,
            initialIntervalMinutes = uiState.settings.intervalMenit,
            onCompleteOnboarding = { beratKg, targetMl, otomatis, bangun, tidur, interval ->
                viewModel.completeOnboarding(
                    beratBadanKg = beratKg,
                    targetMl = targetMl,
                    targetOtomatis = otomatis,
                    jamBangunMenit = bangun,
                    jamTidurMenit = tidur,
                    intervalMenit = interval
                )
            }
        )
        return
    }

    BackHandler(enabled = currentTab != MakanMinumTab.BERANDA) {
        currentTab = MakanMinumTab.BERANDA
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = when (currentTab) {
                                MakanMinumTab.BERANDA -> "MakanMinum"
                                MakanMinumTab.MAKAN -> "Jadwal Makan"
                                MakanMinumTab.RIWAYAT -> "Riwayat Minum"
                                MakanMinumTab.PENGATURAN -> "Pengaturan"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar {
                MakanMinumTab.entries.forEach { tab ->
                    val selected = currentTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title
                            )
                        },
                        label = { Text(tab.title) },
                        modifier = Modifier.testTag(tab.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)

        when (currentTab) {
            MakanMinumTab.BERANDA -> {
                HomeScreen(
                    uiState = uiState,
                    onAddWater = { ml -> viewModel.addWater(ml) },
                    onUndoLastWater = { viewModel.undoLatestWaterToday() },
                    onMarkMealDone = { meal -> viewModel.markMealDoneToday(meal) },
                    onSnoozeMeal = { meal -> viewModel.snoozeMeal10Minutes(meal) },
                    onNavigateToMeals = { currentTab = MakanMinumTab.MAKAN },
                    onNavigateToSettings = { currentTab = MakanMinumTab.PENGATURAN },
                    modifier = contentModifier
                )
            }

            MakanMinumTab.MAKAN -> {
                MealScheduleScreen(
                    meals = uiState.meals,
                    todayDate = uiState.todayDate,
                    isMealDoneToday = { mealId ->
                        uiState.settings.isMealDoneToday(mealId, uiState.todayDate)
                    },
                    onSaveMeal = { meal -> viewModel.saveMealReminder(meal) },
                    onToggleMealActive = { meal, active ->
                        viewModel.toggleMealActive(meal, active)
                    },
                    onDeleteMeal = { meal -> viewModel.deleteMealReminder(meal) },
                    onTestMealNotification = { meal -> viewModel.sendTestMealNotification(meal) },
                    modifier = contentModifier
                )
            }

            MakanMinumTab.RIWAYAT -> {
                HistoryScreen(
                    summaries7Days = uiState.last7DaysSummaries,
                    selectedDate = uiState.selectedHistoryDate,
                    selectedDateLogs = uiState.selectedDateLogs,
                    targetMl = uiState.settings.targetMl,
                    onSelectDate = { dateStr -> viewModel.selectHistoryDate(dateStr) },
                    onDeleteWaterLog = { log -> viewModel.deleteWaterLog(log) },
                    modifier = contentModifier
                )
            }

            MakanMinumTab.PENGATURAN -> {
                SettingsScreen(
                    settings = uiState.settings,
                    onSaveWeightAndTarget = { beratKg, targetMl, otomatis ->
                        viewModel.updateWeightAndTarget(beratKg, targetMl, otomatis)
                    },
                    onSaveWaterSchedule = { bangun, tidur, interval, aktif, stopTarget ->
                        viewModel.updateWaterScheduleSettings(
                            jamBangunMenit = bangun,
                            jamTidurMenit = tidur,
                            intervalMenit = interval,
                            pengingatAktif = aktif,
                            stopSetelahTarget = stopTarget
                        )
                    },
                    onUpdateTheme = { mode -> viewModel.updateTheme(mode) },
                    onTestWaterNotification = { viewModel.sendTestWaterNotification() },
                    onTestMealNotification = { viewModel.sendTestMealNotification() },
                    modifier = contentModifier
                )
            }
        }
    }
}
