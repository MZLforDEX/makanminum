package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.data.DateTimeUtils
import com.example.reminder.NotificationHelper
import com.example.reminder.ReminderScheduler
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    initialWeightKg: Int,
    initialWakeMinutes: Int,
    initialSleepMinutes: Int,
    initialIntervalMinutes: Int,
    onCompleteOnboarding: (
        beratBadanKg: Int,
        targetMl: Int,
        targetOtomatis: Boolean,
        jamBangunMenit: Int,
        jamTidurMenit: Int,
        intervalMenit: Int
    ) -> Unit
) {
    val context = LocalContext.current
    var currentStep by rememberSaveable { mutableIntStateOf(1) }

    var weightKg by rememberSaveable { mutableIntStateOf(initialWeightKg.coerceIn(30, 150)) }
    var autoTarget by rememberSaveable { mutableStateOf(true) }
    var customTargetText by rememberSaveable {
        mutableStateOf(DateTimeUtils.calculateRecommendedWaterMl(initialWeightKg).toString())
    }
    var wakeHour by rememberSaveable { mutableIntStateOf((initialWakeMinutes / 60).coerceIn(4, 11)) }
    var sleepHour by rememberSaveable { mutableIntStateOf((initialSleepMinutes / 60).coerceIn(19, 23)) }
    var intervalMinutes by rememberSaveable { mutableIntStateOf(initialIntervalMinutes) }

    val calculatedTargetMl = DateTimeUtils.calculateRecommendedWaterMl(weightKg)
    val finalTargetMl = if (autoTarget) {
        calculatedTargetMl
    } else {
        customTargetText.toIntOrNull()?.coerceIn(500, 8000) ?: calculatedTargetMl
    }

    var hasNotifPermission by remember {
        mutableStateOf(NotificationHelper.hasNotificationPermission(context))
    }
    var canExactAlarm by remember {
        mutableStateOf(ReminderScheduler.canScheduleExactAlarms(context))
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotifPermission = NotificationHelper.hasNotificationPermission(context)
                canExactAlarm = ReminderScheduler.canScheduleExactAlarms(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPermission = granted
    }

    BackHandler(enabled = currentStep == 2) {
        currentStep = 1
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header identitas MakanMinum
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = "Ikon MakanMinum",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "MakanMinum",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Langkah $currentStep dari 2 • Offline & Tanpa Akun",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    TextButton(
                        onClick = {
                            onCompleteOnboarding(
                                weightKg,
                                finalTargetMl,
                                autoTarget,
                                wakeHour * 60,
                                sleepHour * 60,
                                intervalMinutes
                            )
                        },
                        modifier = Modifier.testTag("onboarding_skip_button")
                    ) {
                        Text("Lewati")
                    }
                }

                AnimatedContent(
                    targetState = currentStep,
                    label = "OnboardingStepTransition"
                ) { step ->
                    if (step == 1) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                text = "Atur Target Minum & Jam Aktif Harian",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Kami akan menghitung kebutuhan air harian Anda secara otomatis dan menyiapkan jadwal pengingat makan bawaan (Sarapan 07:00, Makan Siang 12:00, Makan Malam 19:00).",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Kartu Berat Badan & Target Air
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Berat Badan Anda",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = "$weightKg kg",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                        }
                                    }

                                    Slider(
                                        value = weightKg.toFloat(),
                                        onValueChange = {
                                            weightKg = it.roundToInt()
                                            if (autoTarget) {
                                                customTargetText = DateTimeUtils
                                                    .calculateRecommendedWaterMl(weightKg)
                                                    .toString()
                                            }
                                        },
                                        valueRange = 35f..130f,
                                        modifier = Modifier.testTag("onboarding_weight_slider")
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Hitung Target Otomatis",
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = "Rekomendasi: $calculatedTargetMl ml/hari (~33 ml × kg)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = autoTarget,
                                            onCheckedChange = {
                                                autoTarget = it
                                                if (it) {
                                                    customTargetText = calculatedTargetMl.toString()
                                                }
                                            },
                                            modifier = Modifier.testTag("onboarding_auto_target_switch")
                                        )
                                    }

                                    if (!autoTarget) {
                                        OutlinedTextField(
                                            value = customTargetText,
                                            onValueChange = { value ->
                                                customTargetText = value.filter { ch -> ch.isDigit() }.take(4)
                                            },
                                            label = { Text("Target Minum Harian (ml)") },
                                            suffix = { Text("ml") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("onboarding_custom_target_input")
                                        )
                                    }

                                    Text(
                                        text = stringResource(R.string.medical_disclaimer),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Kartu Jam Bangun, Tidur & Interval Minum
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Rentang Pengingat Minum",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }

                                    Text(
                                        text = "Pengingat minum hanya akan berbunyi di antara jam bangun dan jam tidur Anda.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

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
                                                onValueChange = { wakeHour = it.roundToInt() },
                                                valueRange = 4f..10f,
                                                steps = 5
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Jam Tidur: ${String.format("%02d:00", sleepHour)}",
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                            Slider(
                                                value = sleepHour.toFloat(),
                                                onValueChange = { sleepHour = it.roundToInt() },
                                                valueRange = 20f..23f,
                                                steps = 2
                                            )
                                        }
                                    }

                                    Text(
                                        text = "Interval Pengingat Minum",
                                        style = MaterialTheme.typography.labelLarge
                                    )

                                    val intervalOptions = listOf(
                                        30 to "30 mnt",
                                        60 to "1 jam",
                                        90 to "1.5 jam",
                                        120 to "2 jam",
                                        180 to "3 jam"
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        intervalOptions.forEach { (mins, label) ->
                                            FilterChip(
                                                selected = intervalMinutes == mins,
                                                onClick = { intervalMinutes = mins },
                                                label = { Text(label) }
                                            )
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = { currentStep = 2 },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("onboarding_next_button"),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Lanjut ke Pengaturan Izin")
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                text = "Agar Pengingat Selalu Tepat Waktu",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Aktifkan izin notifikasi dan alarm agar pengingat makan & minum muncul tepat waktu dan Anda bisa mencatat langsung dari bilah notifikasi.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Izin Notifikasi
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (hasNotifPermission) {
                                                Icons.Default.CheckCircle
                                            } else {
                                                Icons.Default.NotificationsActive
                                            },
                                            contentDescription = null,
                                            tint = if (hasNotifPermission) {
                                                MaterialTheme.colorScheme.tertiary
                                            } else {
                                                MaterialTheme.colorScheme.primary
                                            }
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Izin Notifikasi",
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Text(
                                                text = if (hasNotifPermission) {
                                                    "Sudah aktif — siap menampilkan tombol 'Minum 250 ml' & 'Sudah makan'"
                                                } else {
                                                    "Diperlukan agar pengingat makan dan minum dapat muncul"
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    if (!hasNotifPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        OutlinedButton(
                                            onClick = {
                                                notificationPermissionLauncher.launch(
                                                    Manifest.permission.POST_NOTIFICATIONS
                                                )
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("request_notification_permission_button")
                                        ) {
                                            Text("Izinkan Notifikasi")
                                        }
                                    }
                                }
                            }

                            // Izin Alarm Tepat Waktu (Exact Alarm)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (canExactAlarm) {
                                                Icons.Default.CheckCircle
                                            } else {
                                                Icons.Default.Alarm
                                            },
                                            contentDescription = null,
                                            tint = if (canExactAlarm) {
                                                MaterialTheme.colorScheme.tertiary
                                            } else {
                                                MaterialTheme.colorScheme.secondary
                                            }
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Alarm & Pengingat Tepat Waktu",
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Text(
                                                text = if (canExactAlarm) {
                                                    "Aktif — pengingat muncul tepat pada menit yang dijadwalkan"
                                                } else {
                                                    "Buka pengaturan untuk mengizinkan alarm presisi di Android 12+"
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    if (!canExactAlarm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        OutlinedButton(
                                            onClick = {
                                                runCatching {
                                                    val intent = Intent(
                                                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                                        Uri.parse("package:${context.packageName}")
                                                    )
                                                    context.startActivity(intent)
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("request_exact_alarm_button")
                                        ) {
                                            Text("Buka Pengaturan Alarm Tepat Waktu")
                                        }
                                    }
                                }
                            }

                            // Tips Penghemat Baterai Vendor
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.BatteryChargingFull,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "Tips Pengguna Xiaomi, Oppo, Vivo & Samsung",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Text(
                                            text = "Jika pengingat terlambat saat layar mati, kecualikan MakanMinum dari Penghemat Baterai melalui tab Pengaturan kapan saja.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { currentStep = 1 },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .testTag("onboarding_back_button"),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Kembali")
                                }

                                Button(
                                    onClick = {
                                        onCompleteOnboarding(
                                            weightKg,
                                            finalTargetMl,
                                            autoTarget,
                                            wakeHour * 60,
                                            sleepHour * 60,
                                            intervalMinutes
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1.4f)
                                        .height(52.dp)
                                        .testTag("onboarding_finish_button"),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Restaurant,
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Mulai MakanMinum")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
