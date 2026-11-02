package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.BuildConfig
import com.example.R
import com.example.data.DateTimeUtils
import com.example.data.ThemeMode
import com.example.data.UserSettings
import com.example.reminder.NotificationHelper
import com.example.reminder.ReminderScheduler
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: UserSettings,
    onSaveWeightAndTarget: (beratKg: Int, targetMl: Int, otomatis: Boolean) -> Unit,
    onSaveWaterSchedule: (jamBangunMenit: Int, jamTidurMenit: Int, intervalMenit: Int, aktif: Boolean, stopSetelahTarget: Boolean) -> Unit,
    onUpdateTheme: (ThemeMode) -> Unit,
    onTestWaterNotification: () -> Unit,
    onTestMealNotification: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var weightKg by rememberSaveable { mutableIntStateOf(settings.beratBadanKg) }
    var autoTarget by rememberSaveable { mutableStateOf(settings.targetOtomatis) }
    var targetInputText by rememberSaveable { mutableStateOf(settings.targetMl.toString()) }

    var wakeHour by rememberSaveable { mutableIntStateOf((settings.jamBangunMenit / 60).coerceIn(0, 23)) }
    var sleepHour by rememberSaveable { mutableIntStateOf((settings.jamTidurMenit / 60).coerceIn(0, 23)) }
    var intervalMinutes by rememberSaveable { mutableIntStateOf(settings.intervalMenit) }
    var waterReminderEnabled by rememberSaveable { mutableStateOf(settings.pengingatMinumAktif) }
    var stopAfterTarget by rememberSaveable { mutableStateOf(settings.stopSetelahTarget) }

    LaunchedEffect(settings) {
        weightKg = settings.beratBadanKg
        autoTarget = settings.targetOtomatis
        targetInputText = settings.targetMl.toString()
        wakeHour = (settings.jamBangunMenit / 60).coerceIn(0, 23)
        sleepHour = (settings.jamTidurMenit / 60).coerceIn(0, 23)
        intervalMinutes = settings.intervalMenit
        waterReminderEnabled = settings.pengingatMinumAktif
        stopAfterTarget = settings.stopSetelahTarget
    }

    var hasNotificationPermission by remember {
        mutableStateOf(NotificationHelper.hasNotificationPermission(context))
    }
    var canExactAlarm by remember {
        mutableStateOf(ReminderScheduler.canScheduleExactAlarms(context))
    }
    var isIgnoringBatteryOpt by remember {
        mutableStateOf(ReminderScheduler.isIgnoringBatteryOptimizations(context))
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotificationPermission = NotificationHelper.hasNotificationPermission(context)
                canExactAlarm = ReminderScheduler.canScheduleExactAlarms(context)
                isIgnoringBatteryOpt = ReminderScheduler.isIgnoringBatteryOptimizations(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }

    val recommendedMl = DateTimeUtils.calculateRecommendedWaterMl(weightKg)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Kartu Berat Badan & Target Minum Harian
            item(key = "settings_weight_target_card") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Berat Badan & Target Minum",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Berat Badan: $weightKg kg",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "Target: ${if (autoTarget) recommendedMl else (targetInputText.toIntOrNull() ?: recommendedMl)} ml",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Slider(
                            value = weightKg.toFloat(),
                            onValueChange = {
                                weightKg = it.roundToInt()
                                if (autoTarget) {
                                    targetInputText = DateTimeUtils.calculateRecommendedWaterMl(weightKg).toString()
                                }
                            },
                            valueRange = 30f..140f,
                            modifier = Modifier.testTag("settings_weight_slider")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Hitung Target Otomatis dari Berat Badan",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Rekomendasi untuk $weightKg kg adalah $recommendedMl ml",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = autoTarget,
                                onCheckedChange = {
                                    autoTarget = it
                                    if (it) {
                                        targetInputText = recommendedMl.toString()
                                    }
                                },
                                modifier = Modifier.testTag("settings_auto_target_switch")
                            )
                        }

                        if (!autoTarget) {
                            OutlinedTextField(
                                value = targetInputText,
                                onValueChange = { text ->
                                    targetInputText = text.filter { it.isDigit() }.take(4)
                                },
                                label = { Text("Target Minum Manual (ml)") },
                                suffix = { Text("ml") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_manual_target_input")
                            )
                        }

                        Text(
                            text = stringResource(R.string.medical_disclaimer),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = {
                                val resolvedTarget = if (autoTarget) {
                                    recommendedMl
                                } else {
                                    targetInputText.toIntOrNull()?.coerceIn(500, 8000) ?: recommendedMl
                                }
                                onSaveWeightAndTarget(weightKg, resolvedTarget, autoTarget)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_weight_target_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Simpan Target Minum")
                        }
                    }
                }
            }

            // 2. Kartu Jadwal & Interval Pengingat Minum
            item(key = "settings_water_schedule_card") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Pengingat Minum Berkala",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Switch(
                                checked = waterReminderEnabled,
                                onCheckedChange = { waterReminderEnabled = it },
                                modifier = Modifier.testTag("settings_water_reminder_switch")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Jam Bangun: ${String.format("%02d:00", wakeHour)}",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Slider(
                                    value = wakeHour.toFloat(),
                                    onValueChange = { wakeHour = it.roundToInt().coerceIn(3, 12) },
                                    valueRange = 3f..12f,
                                    steps = 8
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Jam Tidur: ${String.format("%02d:00", sleepHour)}",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Slider(
                                    value = sleepHour.toFloat(),
                                    onValueChange = { sleepHour = it.roundToInt().coerceIn(18, 23) },
                                    valueRange = 18f..23f,
                                    steps = 4
                                )
                            }
                        }

                        Text(
                            text = "Interval Pengingat (30 menit – 4 jam)",
                            style = MaterialTheme.typography.labelLarge
                        )

                        val intervals = listOf(
                            30 to "30 mnt",
                            60 to "1 jam",
                            90 to "1.5 jam",
                            120 to "2 jam",
                            180 to "3 jam",
                            240 to "4 jam"
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            intervals.forEach { (mins, label) ->
                                FilterChip(
                                    selected = intervalMinutes == mins,
                                    onClick = { intervalMinutes = mins },
                                    label = { Text(label) }
                                )
                            }
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Hentikan Setelah Target Tercapai",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Pengingat minum berhenti untuk hari itu jika target ml harian sudah terpenuhi",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = stopAfterTarget,
                                onCheckedChange = { stopAfterTarget = it },
                                modifier = Modifier.testTag("settings_stop_after_target_switch")
                            )
                        }

                        Button(
                            onClick = {
                                onSaveWaterSchedule(
                                    wakeHour * 60,
                                    sleepHour * 60,
                                    intervalMinutes,
                                    waterReminderEnabled,
                                    stopAfterTarget
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_water_schedule_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Simpan Pengingat Minum")
                        }
                    }
                }
            }

            // 3. Kartu Mode Tema (Terang, Gelap, Ikuti Sistem)
            item(key = "settings_theme_card") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DarkMode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Tampilan & Mode Tema",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ThemeMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = settings.tema == mode,
                                    onClick = { onUpdateTheme(mode) },
                                    label = { Text(mode.label) },
                                    modifier = Modifier.testTag("theme_chip_${mode.name}")
                                )
                            }
                        }
                    }
                }
            }

            // 4. Kartu Pintasan Izin Sistem, Penghemat Baterai & Uji Notifikasi
            item(key = "settings_permissions_and_battery_card") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Izin Sistem & Keandalan Pengingat",
                            style = MaterialTheme.typography.titleMedium
                        )

                        // Status & Pintasan Izin Notifikasi
                        PermissionShortcutRow(
                            title = "Izin Notifikasi",
                            subtitle = if (hasNotificationPermission) {
                                "Aktif • Notifikasi makan dan minum diizinkan"
                            } else {
                                "Belum diizinkan • Ketuk untuk membuka izin notifikasi"
                            },
                            isGranted = hasNotificationPermission,
                            buttonLabel = if (hasNotificationPermission) "Pengaturan" else "Izinkan",
                            onClick = {
                                if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    runCatching {
                                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                        }
                                        context.startActivity(intent)
                                    }
                                }
                            }
                        )

                        HorizontalDivider()

                        // Status & Pintasan Izin Alarm Tepat Waktu
                        PermissionShortcutRow(
                            title = "Alarm & Pengingat Tepat Waktu",
                            subtitle = if (canExactAlarm) {
                                "Aktif • Alarm muncul tepat pada menit yang dijadwalkan"
                            } else {
                                "Mode fallback aktif • Izinkan exact alarm agar jadwal presisi"
                            },
                            isGranted = canExactAlarm,
                            buttonLabel = "Pengaturan",
                            onClick = {
                                runCatching {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        val intent = Intent(
                                            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                        context.startActivity(intent)
                                    } else {
                                        val intent = Intent(
                                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                        context.startActivity(intent)
                                    }
                                }
                            }
                        )

                        HorizontalDivider()

                        // Status & Pintasan Penghemat Baterai
                        PermissionShortcutRow(
                            title = "Optimasi & Penghemat Baterai",
                            subtitle = if (isIgnoringBatteryOpt) {
                                "Tidak dibatasi • Pengingat aman berjalan di latar belakang"
                            } else {
                                "Buka pengaturan baterai bila alarm sering dimatikan oleh sistem HP"
                            },
                            isGranted = isIgnoringBatteryOpt,
                            buttonLabel = "Baterai",
                            onClick = {
                                runCatching {
                                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                    context.startActivity(intent)
                                }.onFailure {
                                    runCatching {
                                        val fallback = Intent(
                                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                        context.startActivity(fallback)
                                    }
                                }
                            }
                        )

                        HorizontalDivider()

                        Text(
                            text = "Uji Notifikasi Minum & Alarm Makan",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = "Pengingat minum tampil sebagai notifikasi ('Minum 250 ml' / 'Tunda 10 menit'), sedangkan pengingat makan muncul sebagai alarm penuh.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onTestWaterNotification,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("test_water_notification_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Uji Notif Minum")
                            }
                            OutlinedButton(
                                onClick = onTestMealNotification,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("test_meal_notification_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Uji Alarm Makan")
                            }
                        }
                    }
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "MakanMinum v${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("app_version_text")
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionShortcutRow(
    title: String,
    subtitle: String,
    isGranted: Boolean,
    buttonLabel: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = if (isGranted) {
                Icons.Default.CheckCircle
            } else {
                Icons.Default.BatteryAlert
            },
            contentDescription = null,
            tint = if (isGranted) {
                MaterialTheme.colorScheme.tertiary
            } else {
                MaterialTheme.colorScheme.secondary
            },
            modifier = Modifier.size(22.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        OutlinedButton(
            onClick = onClick,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(buttonLabel)
        }
    }
}
