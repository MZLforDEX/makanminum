package com.example.ui

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.data.DateTimeUtils
import com.example.data.MakanMinumRepository
import com.example.reminder.AlarmRingingController
import com.example.reminder.NotificationHelper
import com.example.reminder.ReminderScheduler
import com.example.ui.theme.MakanMinumTheme
import kotlinx.coroutines.launch

/**
 * Layar Alarm Penuh (Full-Screen Alarm) yang menyala di atas layar kunci (lockscreen)
 * disertai dering alarm & getaran ketika waktu makan atau minum tiba.
 */
class AlarmRingingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureLockScreenAndWakeFlags()
        enableEdgeToEdge()

        // Pastikan dering alarm & getaran berjalan saat layar alarm tampil
        AlarmRingingController.startRinging(this)

        renderFromIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        AlarmRingingController.startRinging(this)
        renderFromIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun configureLockScreenAndWakeFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun renderFromIntent(currentIntent: Intent?) {
        val alarmType = currentIntent?.getStringExtra(EXTRA_ALARM_TYPE)
            ?: ReminderScheduler.TYPE_MEAL
        val mealId = currentIntent?.getLongExtra(EXTRA_MEAL_ID, 1L) ?: 1L
        val mealName = currentIntent?.getStringExtra(EXTRA_MEAL_NAME) ?: "Waktu Makan"
        val mealTime = currentIntent?.getStringExtra(EXTRA_MEAL_TIME)
            ?: DateTimeUtils.formatTimeMillis(System.currentTimeMillis())
        val waterCurrentMl = currentIntent?.getIntExtra(EXTRA_WATER_CURRENT_ML, 0) ?: 0
        val waterTargetMl = currentIntent?.getIntExtra(EXTRA_WATER_TARGET_ML, 2000) ?: 2000
        val notificationId = currentIntent?.getIntExtra(
            EXTRA_NOTIFICATION_ID,
            if (alarmType == ReminderScheduler.TYPE_WATER) {
                NotificationHelper.WATER_NOTIFICATION_ID
            } else {
                NotificationHelper.mealNotificationId(mealId)
            }
        ) ?: NotificationHelper.WATER_NOTIFICATION_ID

        setContent {
            MakanMinumTheme {
                AlarmRingingScreen(
                    isMealAlarm = alarmType == ReminderScheduler.TYPE_MEAL,
                    title = if (alarmType == ReminderScheduler.TYPE_MEAL) {
                        "Waktunya $mealName!"
                    } else {
                        "Waktunya Minum Air Putih!"
                    },
                    timeText = mealTime,
                    subtitle = if (alarmType == ReminderScheduler.TYPE_MEAL) {
                        "Jangan lewatkan jam $mealName Anda agar tubuh tetap berenergi dan sehat hari ini."
                    } else {
                        val remaining = (waterTargetMl - waterCurrentMl).coerceAtLeast(0)
                        "Progres hari ini: $waterCurrentMl / $waterTargetMl ml. Yuk minum segelas air (250 ml) sekarang!${if (remaining > 0) " ($remaining ml lagi menuju target)" else ""}"
                    },
                    primaryActionLabel = if (alarmType == ReminderScheduler.TYPE_MEAL) {
                        "Sudah Makan (Matikan Alarm)"
                    } else {
                        "Minum 250 ml (Matikan Alarm)"
                    },
                    onPrimaryAction = {
                        lifecycleScope.launch {
                            val repo = MakanMinumRepository.getInstance(applicationContext)
                            val now = System.currentTimeMillis()
                            if (alarmType == ReminderScheduler.TYPE_MEAL) {
                                repo.markMealDoneToday(mealId, now)
                            } else {
                                repo.addWaterLog(250, now)
                            }
                            stopAlarmAndClose(notificationId)
                        }
                    },
                    onSnooze10Minutes = {
                        val now = System.currentTimeMillis()
                        if (alarmType == ReminderScheduler.TYPE_MEAL) {
                            ReminderScheduler.scheduleSnoozeMealAlarm(
                                context = applicationContext,
                                mealId = mealId,
                                mealName = mealName,
                                delayMinutes = 10,
                                nowMillis = now
                            )
                        } else {
                            ReminderScheduler.scheduleSnoozeWaterAlarm(
                                context = applicationContext,
                                delayMinutes = 10,
                                nowMillis = now
                            )
                        }
                        stopAlarmAndClose(notificationId)
                    },
                    onDismissAlarmOnly = {
                        stopAlarmAndClose(notificationId)
                    }
                )
            }
        }
    }

    private fun stopAlarmAndClose(notificationId: Int) {
        AlarmRingingController.stopRinging(applicationContext)
        NotificationHelper.dismissNotification(applicationContext, notificationId)
        finish()
    }

    companion object {
        const val EXTRA_ALARM_TYPE = "extra_ringing_alarm_type"
        const val EXTRA_MEAL_ID = "extra_ringing_meal_id"
        const val EXTRA_MEAL_NAME = "extra_ringing_meal_name"
        const val EXTRA_MEAL_TIME = "extra_ringing_meal_time"
        const val EXTRA_WATER_CURRENT_ML = "extra_ringing_water_current_ml"
        const val EXTRA_WATER_TARGET_ML = "extra_ringing_water_target_ml"
        const val EXTRA_NOTIFICATION_ID = "extra_ringing_notification_id"

        fun createIntent(
            context: Context,
            alarmType: String,
            mealId: Long = 1L,
            mealName: String = "Waktu Makan",
            mealTime: String = DateTimeUtils.formatTimeMillis(System.currentTimeMillis()),
            waterCurrentMl: Int = 0,
            waterTargetMl: Int = 2000,
            notificationId: Int = NotificationHelper.WATER_NOTIFICATION_ID
        ): Intent {
            return Intent(context, AlarmRingingActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_ALARM_TYPE, alarmType)
                putExtra(EXTRA_MEAL_ID, mealId)
                putExtra(EXTRA_MEAL_NAME, mealName)
                putExtra(EXTRA_MEAL_TIME, mealTime)
                putExtra(EXTRA_WATER_CURRENT_ML, waterCurrentMl)
                putExtra(EXTRA_WATER_TARGET_ML, waterTargetMl)
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            }
        }
    }
}

@Composable
fun AlarmRingingScreen(
    isMealAlarm: Boolean,
    title: String,
    timeText: String,
    subtitle: String,
    primaryActionLabel: String,
    onPrimaryAction: () -> Unit,
    onSnooze10Minutes: () -> Unit,
    onDismissAlarmOnly: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "AlarmPulseTransition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AlarmPulseScale"
    )

    val accentColor = if (isMealAlarm) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.primary
    }
    val containerColor = if (isMealAlarm) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        containerColor.copy(alpha = 0.65f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp)
            .testTag("alarm_ringing_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Badge Alarm Berdering
            Surface(
                shape = RoundedCornerShape(50),
                color = containerColor
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (isMealAlarm) "ALARM JADWAL MAKAN" else "ALARM PENGINGAT MINUM",
                        style = MaterialTheme.typography.labelLarge,
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Jam Besar
            Text(
                text = timeText,
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("alarm_time_text")
            )

            // Lingkaran Ikon Berdenyut (Pulsing Alarm Ring)
            Box(
                modifier = Modifier.size(150.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.22f))
                )
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .clip(CircleShape)
                        .background(accentColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isMealAlarm) {
                            Icons.Default.Restaurant
                        } else {
                            Icons.Default.WaterDrop
                        },
                        contentDescription = null,
                        tint = if (isMealAlarm) {
                            MaterialTheme.colorScheme.onSecondary
                        } else {
                            MaterialTheme.colorScheme.onPrimary
                        },
                        modifier = Modifier.size(50.dp)
                    )
                }
            }

            // Judul & Pesan Pengingat
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("alarm_title_text")
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Tombol Aksi Utama (Sudah Makan / Minum 250 ml)
            Button(
                onClick = onPrimaryAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = if (isMealAlarm) {
                        MaterialTheme.colorScheme.onSecondary
                    } else {
                        MaterialTheme.colorScheme.onPrimary
                    }
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .testTag("alarm_primary_action_button")
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = primaryActionLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Tombol Tunda 10 Menit (Snooze)
            OutlinedButton(
                onClick = onSnooze10Minutes,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("alarm_snooze_button")
            ) {
                Icon(imageVector = Icons.Default.Snooze, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tunda 10 Menit (Snooze)",
                    style = MaterialTheme.typography.titleSmall
                )
            }

            // Tombol Matikan Alarm Saja
            TextButton(
                onClick = onDismissAlarmOnly,
                modifier = Modifier.testTag("alarm_dismiss_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Matikan Alarm Saja")
            }
        }
    }
}
