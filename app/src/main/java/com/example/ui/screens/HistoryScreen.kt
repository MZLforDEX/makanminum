package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.DailyWaterSummary
import com.example.data.WaterLogEntity
import kotlin.math.max

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryScreen(
    summaries7Days: List<DailyWaterSummary>,
    selectedDate: String,
    selectedDateLogs: List<WaterLogEntity>,
    targetMl: Int,
    onSelectDate: (String) -> Unit,
    onDeleteWaterLog: (WaterLogEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedSummary = summaries7Days.find { it.tanggal == selectedDate }
        ?: summaries7Days.lastOrNull()

    val weeklyTotalMl = summaries7Days.sumOf { it.totalMl }
    val weeklyAverageMl = if (summaries7Days.isNotEmpty()) weeklyTotalMl / summaries7Days.size else 0
    val daysReachedTarget = summaries7Days.count { it.isTargetReached }

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
            // Ringkasan Statistik 7 Hari Terakhir
            item(key = "weekly_stats_row") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatSummaryCard(
                        title = "Rata-rata / Hari",
                        value = "$weeklyAverageMl ml",
                        modifier = Modifier.weight(1f)
                    )
                    StatSummaryCard(
                        title = "Target Tercapai",
                        value = "$daysReachedTarget / 7 hari",
                        modifier = Modifier.weight(1f)
                    )
                    StatSummaryCard(
                        title = "Total 7 Hari",
                        value = "${String.format("%.1f", weeklyTotalMl / 1000f)} L",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Kartu Grafik Batang 7 Hari Terakhir + Garis Target
            item(key = "seven_day_bar_chart") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("history_bar_chart_card"),
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
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BarChart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                        text = "Grafik Minum 7 Hari Terakhir",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Garis putus-putus = Target harian ($targetMl ml)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        SevenDayWaterBarChart(
                            summaries = summaries7Days,
                            selectedDate = selectedDate,
                            targetMl = targetMl,
                            onSelectDate = onSelectDate
                        )

                        // Chip pemilih hari 7 hari terakhir
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            summaries7Days.forEach { day ->
                                FilterChip(
                                    selected = day.tanggal == selectedDate,
                                    onClick = { onSelectDate(day.tanggal) },
                                    label = {
                                        Text(
                                            text = if (day.isToday) "Hari Ini" else day.labelHariSingkat,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Header Daftar Catatan pada Hari yang Dipilih
            item(key = "selected_day_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = selectedSummary?.labelTanggalLengkap ?: "Daftar Catatan Minum",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Total tercatat: ${selectedSummary?.totalMl ?: 0} ml dari target $targetMl ml",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (selectedSummary?.isTargetReached == true) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Tercapai",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            }

            if (selectedDateLogs.isEmpty()) {
                item(key = "empty_logs_for_day") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = "Tidak ada catatan minum pada tanggal ini.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(selectedDateLogs, key = { it.id }) { log ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("history_log_item_${log.id}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WaterDrop,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "${log.jumlahMl} ml",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Dicatat pukul ${log.formattedTime()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            IconButton(
                                onClick = { onDeleteWaterLog(log) },
                                modifier = Modifier.testTag("delete_water_log_${log.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Hapus catatan ${log.jumlahMl} ml",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatSummaryCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SevenDayWaterBarChart(
    summaries: List<DailyWaterSummary>,
    selectedDate: String,
    targetMl: Int,
    onSelectDate: (String) -> Unit
) {
    if (summaries.isEmpty()) return

    val maxDataMl = summaries.maxOfOrNull { it.totalMl } ?: 0
    val chartMaxMl = max(max(targetMl, maxDataMl), 1000) * 1.15f

    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    val targetLineColor = MaterialTheme.colorScheme.secondary

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Label nilai ml di atas tiap batang
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            summaries.forEach { day ->
                Text(
                    text = if (day.totalMl > 0) "${day.totalMl}" else "0",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (day.tanggal == selectedDate) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = if (day.tanggal == selectedDate) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Area Canvas Grafik Batang + Garis Target
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val count = summaries.size
                if (count == 0) return@Canvas
                val slotWidth = size.width / count
                val barWidth = (slotWidth * 0.52f).coerceAtMost(36.dp.toPx())

                summaries.forEachIndexed { index, summary ->
                    val centerX = slotWidth * index + slotWidth / 2f
                    val barLeft = centerX - barWidth / 2f

                    // Latar belakang kolom batang
                    drawRoundRect(
                        color = trackColor,
                        topLeft = Offset(barLeft, 0f),
                        size = Size(barWidth, size.height),
                        cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                    )

                    // Isi batang sesuai jumlah ml
                    val ratio = (summary.totalMl / chartMaxMl).coerceIn(0f, 1f)
                    val barHeight = size.height * ratio
                    if (barHeight > 0f) {
                        val barColor = if (summary.isTargetReached) {
                            tertiaryColor
                        } else {
                            primaryColor
                        }
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(barLeft, size.height - barHeight),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                        )
                    }
                }

                // Garis Target Harian (putus-putus)
                if (targetMl > 0) {
                    val targetRatio = (targetMl / chartMaxMl).coerceIn(0f, 1f)
                    val targetY = size.height * (1f - targetRatio)
                    drawLine(
                        color = targetLineColor,
                        start = Offset(0f, targetY),
                        end = Offset(size.width, targetY),
                        strokeWidth = 2.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                    )
                }
            }

            // Area klik transparan per kolom hari
            Row(modifier = Modifier.fillMaxSize()) {
                summaries.forEach { day ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clickable { onSelectDate(day.tanggal) }
                    )
                }
            }
        }

        // Label nama hari di bawah tiap batang
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            summaries.forEach { day ->
                Text(
                    text = if (day.isToday) "Hari Ini" else day.labelHariSingkat,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (day.tanggal == selectedDate) FontWeight.Bold else FontWeight.Normal,
                    color = if (day.tanggal == selectedDate) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
