package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.DateTimeUtils
import com.example.data.MealReminderEntity
import kotlin.math.roundToInt

@Composable
fun MealScheduleScreen(
    meals: List<MealReminderEntity>,
    todayDate: String,
    isMealDoneToday: (Long) -> Boolean,
    onSaveMeal: (MealReminderEntity) -> Unit,
    onToggleMealActive: (MealReminderEntity, Boolean) -> Unit,
    onDeleteMeal: (MealReminderEntity) -> Unit,
    onTestMealNotification: (MealReminderEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Simpan ID primitif di rememberSaveable agar aman saat rotasi layar / configuration change:
    // null = dialog tertutup, 0L = tambah jadwal baru, > 0L = edit jadwal dengan ID tersebut
    var editingMealId by rememberSaveable { mutableStateOf<Long?>(null) }
    var defaultNewMealName by rememberSaveable { mutableStateOf("Camilan Sore") }
    var defaultNewMealHour by rememberSaveable { mutableIntStateOf(16) }

    val currentEditingMeal: MealReminderEntity? = when (val id = editingMealId) {
        null -> null
        0L -> MealReminderEntity(
            id = 0L,
            nama = defaultNewMealName,
            jam = defaultNewMealHour,
            menit = 0,
            hariAktif = MealReminderEntity.ALL_DAYS_MASK,
            aktif = true
        )
        else -> meals.find { it.id == id }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "meals_header_card") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.secondary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restaurant,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondary
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Pengingat Jadwal Makan",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            val activeCount = meals.count { it.aktif && it.hariAktif != 0 }
                            Text(
                                text = "$activeCount dari ${meals.size} alarm makan aktif • Dijadwalkan presisi lewat AlarmManager & NotificationManager",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            if (meals.isEmpty()) {
                item(key = "empty_meals_state") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restaurant,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "Belum Ada Jadwal Makan",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Tambahkan jadwal sarapan, makan siang, makan malam, atau camilan sesuai rutinitas harian Anda.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = {
                                    defaultNewMealName = "Sarapan"
                                    defaultNewMealHour = 7
                                    editingMealId = 0L
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Tambah Jadwal Pertama")
                            }
                        }
                    }
                }
            } else {
                items(meals, key = { it.id }) { meal ->
                    MealReminderItemCard(
                        meal = meal,
                        todayDate = todayDate,
                        doneToday = isMealDoneToday(meal.id),
                        onToggleActive = { active -> onToggleMealActive(meal, active) },
                        onEdit = {
                            editingMealId = meal.id
                        },
                        onDelete = { onDeleteMeal(meal) },
                        onTestNotification = { onTestMealNotification(meal) }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = {
                defaultNewMealName = "Camilan Sore"
                defaultNewMealHour = 16
                editingMealId = 0L
            },
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_meal_fab")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Tambah pengingat makan"
                )
                Text(
                    text = "Tambah Jadwal",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }

    if (currentEditingMeal != null) {
        MealEditorDialog(
            initialMeal = currentEditingMeal,
            isNew = editingMealId == 0L,
            onDismiss = { editingMealId = null },
            onSave = { updated ->
                editingMealId = null
                onSaveMeal(updated)
            }
        )
    }
}

@Composable
private fun MealReminderItemCard(
    meal: MealReminderEntity,
    todayDate: String,
    doneToday: Boolean,
    onToggleActive: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTestNotification: () -> Unit
) {
    val nextTriggerMillis = DateTimeUtils.computeNextMealTriggerMillis(meal)
    val nextAlarmLabel = if (!meal.aktif || nextTriggerMillis == null) {
        "Pengingat nonaktif"
    } else {
        val isToday = DateTimeUtils.todayDateString(nextTriggerMillis) == todayDate
        if (isToday && !doneToday) {
            "Alarm aktif • Hari ini pukul ${meal.formattedTime()}"
        } else {
            "Alarm aktif • Dijadwalkan pukul ${meal.formattedTime()} berikutnya"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .testTag("meal_item_card_${meal.id}"),
        shape = RoundedCornerShape(20.dp),
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
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = meal.nama,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (meal.aktif) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        if (doneToday) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Text(
                                    text = "Selesai hari ini",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = if (meal.aktif) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = meal.formattedTime(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = if (meal.aktif) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }

                    Text(
                        text = nextAlarmLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = meal.aktif,
                    onCheckedChange = onToggleActive,
                    modifier = Modifier.testTag("meal_switch_${meal.id}")
                )
            }

            // Indikator Hari Aktif (Sen - Min) + Tombol Uji Notifikasi, Edit & Hapus
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    MealReminderEntity.DAY_SHORT_NAMES.forEachIndexed { index, shortName ->
                        val isDayActive = meal.isActiveOnDayIndex(index)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isDayActive && meal.aktif) {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    } else if (isDayActive) {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = shortName.take(3),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isDayActive) FontWeight.Bold else FontWeight.Normal,
                                color = if (isDayActive && meal.aktif) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }

                Row {
                    IconButton(
                        onClick = onTestNotification,
                        modifier = Modifier.testTag("test_meal_button_${meal.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Uji notifikasi ${meal.nama}",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("edit_meal_button_${meal.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Ubah ${meal.nama}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_meal_button_${meal.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Hapus ${meal.nama}",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MealEditorDialog(
    initialMeal: MealReminderEntity,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (MealReminderEntity) -> Unit
) {
    var nama by rememberSaveable(initialMeal.id) { mutableStateOf(initialMeal.nama) }
    var jam by rememberSaveable(initialMeal.id) { mutableIntStateOf(initialMeal.jam) }
    var menit by rememberSaveable(initialMeal.id) { mutableIntStateOf(initialMeal.menit) }
    var hariAktifMask by rememberSaveable(initialMeal.id) { mutableIntStateOf(initialMeal.hariAktif) }

    val isValid = nama.trim().isNotEmpty() && hariAktifMask != 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isNew) "Tambah Pengingat Makan" else "Ubah Pengingat Makan")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nama,
                    onValueChange = { nama = it.take(30) },
                    label = { Text("Nama Jadwal Makan") },
                    placeholder = { Text("Contoh: Sarapan, Camilan Sore") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("meal_name_input")
                )

                // Preset nama cepat
                val presets = listOf("Sarapan", "Makan Siang", "Camilan Sore", "Makan Malam")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { preset ->
                        FilterChip(
                            selected = nama.equals(preset, ignoreCase = true),
                            onClick = { nama = preset },
                            label = { Text(preset, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                // Pemilih Jam & Menit
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Waktu Pengingat: ${String.format("%02d:%02d", jam, menit)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Jam ($jam)",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Slider(
                            value = jam.toFloat(),
                            onValueChange = { jam = it.roundToInt().coerceIn(0, 23) },
                            valueRange = 0f..23f,
                            steps = 22,
                            modifier = Modifier.testTag("meal_hour_slider")
                        )
                        Text(
                            text = "Menit (${String.format("%02d", menit)})",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Slider(
                            value = menit.toFloat(),
                            onValueChange = {
                                // Snap ke kelipatan 5 menit agar mudah dipilih
                                val raw = it.roundToInt().coerceIn(0, 59)
                                menit = ((raw / 5.0).roundToInt() * 5).coerceIn(0, 55)
                            },
                            valueRange = 0f..55f,
                            steps = 10,
                            modifier = Modifier.testTag("meal_minute_slider")
                        )
                    }
                }

                // Pemilih Hari Aktif (Sen - Min)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Hari Aktif",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(
                            onClick = { hariAktifMask = MealReminderEntity.ALL_DAYS_MASK }
                        ) {
                            Text("Semua", style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(
                            onClick = { hariAktifMask = MealReminderEntity.WEEKDAYS_MASK }
                        ) {
                            Text("Sen–Jum", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MealReminderEntity.DAY_SHORT_NAMES.forEachIndexed { idx, dayName ->
                        val selected = (hariAktifMask and (1 shl idx)) != 0
                        FilterChip(
                            selected = selected,
                            onClick = {
                                hariAktifMask = MealReminderEntity.toggleDayBit(hariAktifMask, idx)
                            },
                            label = { Text(dayName) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid) {
                        onSave(
                            initialMeal.copy(
                                nama = nama.trim(),
                                jam = jam.coerceIn(0, 23),
                                menit = menit.coerceIn(0, 59),
                                hariAktif = hariAktifMask and MealReminderEntity.ALL_DAYS_MASK,
                                aktif = true
                            )
                        )
                    }
                },
                enabled = isValid,
                modifier = Modifier.testTag("save_meal_dialog_button")
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
