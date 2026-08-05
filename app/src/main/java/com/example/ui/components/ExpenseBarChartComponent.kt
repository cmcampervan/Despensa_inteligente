package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.PurchaseHistoryItem
import com.example.ui.theme.IndigoContainer
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.IndigoSecondary
import java.text.SimpleDateFormat
import java.util.*

data class DailyExpenseData(
    val dateMillis: Long,
    val dateLabel: String,
    val fullDateStr: String,
    val totalSpent: Double,
    val itemCount: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseBarChartComponent(
    history: List<PurchaseHistoryItem>,
    modifier: Modifier = Modifier
) {
    var isWeeklyView by remember { mutableStateOf(true) } // true = Semanal (7 días), false = Mensual (30 días)
    var selectedDayData by remember { mutableStateOf<DailyExpenseData?>(null) }

    val daysCount = if (isWeeklyView) 7 else 30
    val dateFormat = remember { SimpleDateFormat("EEE d", Locale("es", "ES")) }
    val fullDateFormat = remember { SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "ES")) }
    val monthDayFormat = remember { SimpleDateFormat("d/MM", Locale.getDefault()) }

    val dailyDataList = remember(history, isWeeklyView) {
        val calendar = Calendar.getInstance()
        val list = mutableListOf<DailyExpenseData>()

        for (i in (daysCount - 1) downTo 0) {
            val dayCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -i)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = dayCal.timeInMillis

            val endOfDayCal = (dayCal.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            val endOfDay = endOfDayCal.timeInMillis

            val dayItems = history.filter { it.purchaseDateMillis in startOfDay..endOfDay }
            val daySpent = dayItems.sumOf { it.price * it.quantity }
            val label = if (isWeeklyView) dateFormat.format(dayCal.time).replaceFirstChar { it.uppercase() } else monthDayFormat.format(dayCal.time)
            val fullStr = fullDateFormat.format(dayCal.time).replaceFirstChar { it.uppercase() }

            list.add(
                DailyExpenseData(
                    dateMillis = startOfDay,
                    dateLabel = label,
                    fullDateStr = fullStr,
                    totalSpent = daySpent,
                    itemCount = dayItems.sumOf { it.quantity }.toInt()
                )
            )
        }
        list
    }

    val maxSpent = remember(dailyDataList) {
        val max = dailyDataList.maxOfOrNull { it.totalSpent } ?: 0.0
        if (max <= 0.0) 10.0 else max
    }

    val totalPeriodSpent = remember(dailyDataList) { dailyDataList.sumOf { it.totalSpent } }
    val avgDailySpent = remember(dailyDataList, totalPeriodSpent) { totalPeriodSpent / daysCount }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.2f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("expense_bar_chart_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Title & Weekly/Monthly Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(IndigoContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.BarChart,
                            contentDescription = null,
                            tint = IndigoPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Hábitos Financieros",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = IndigoPrimary
                        )
                        Text(
                            text = if (isWeeklyView) "Gasto diario (Últimos 7 días)" else "Gasto diario (Últimos 30 días)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Filter Segmented Control (Semanal / Mensual)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isWeeklyView) IndigoPrimary else Color.Transparent)
                            .clickable {
                                isWeeklyView = true
                                selectedDayData = null
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("toggle_weekly_chart_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Semanal",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isWeeklyView) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (!isWeeklyView) IndigoPrimary else Color.Transparent)
                            .clickable {
                                isWeeklyView = false
                                selectedDayData = null
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("toggle_monthly_chart_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Mensual",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (!isWeeklyView) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Summary Info Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = IndigoPrimary.copy(alpha = 0.08f),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(16.dp))
                        Column {
                            Text("Total Período", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${String.format(Locale.US, "%.2f", totalPeriodSpent)} €", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = IndigoPrimary)
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = IndigoSecondary.copy(alpha = 0.08f),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.ShowChart, contentDescription = null, tint = IndigoSecondary, modifier = Modifier.size(16.dp))
                        Column {
                            Text("Promedio Diario", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${String.format(Locale.US, "%.2f", avgDailySpent)} €", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = IndigoSecondary)
                        }
                    }
                }
            }

            // Interactive Bar Chart Canvas/Row Container
            val scrollState = rememberScrollState()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF8FAFC))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                    .padding(vertical = 12.dp, horizontal = 8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Average dashed line indicator background Canvas
                    val avgLineRatio = if (maxSpent > 0) (avgDailySpent / maxSpent).toFloat().coerceIn(0.05f, 0.95f) else 0f
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        if (avgDailySpent > 0) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val yPos = size.height * (1f - avgLineRatio)
                                drawLine(
                                    color = IndigoSecondary.copy(alpha = 0.5f),
                                    start = Offset(0f, yPos),
                                    end = Offset(size.width, yPos),
                                    strokeWidth = 2f,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                )
                            }
                        }

                        // Bar Row container
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(if (!isWeeklyView) Modifier.horizontalScroll(scrollState) else Modifier),
                            horizontalArrangement = if (isWeeklyView) Arrangement.SpaceEvenly else Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            dailyDataList.forEach { dayData ->
                                val barHeightRatio = if (maxSpent > 0) (dayData.totalSpent / maxSpent).toFloat().coerceIn(0.02f, 1f) else 0.02f
                                val animatedRatio by animateFloatAsState(
                                    targetValue = barHeightRatio,
                                    animationSpec = tween(durationMillis = 600),
                                    label = "barHeight"
                                )
                                val isSelected = selectedDayData?.dateMillis == dayData.dateMillis

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom,
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(if (isWeeklyView) 38.dp else 22.dp)
                                        .clickable { selectedDayData = dayData }
                                ) {
                                    // Price badge above bar
                                    if (dayData.totalSpent > 0) {
                                        Text(
                                            text = "${dayData.totalSpent.toInt()}€",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Bar Graphics
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f, fill = false)
                                            .fillMaxHeight(animatedRatio)
                                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                            .background(
                                                brush = if (isSelected) {
                                                    Brush.verticalGradient(colors = listOf(Color(0xFFF59E0B), Color(0xFFD97706)))
                                                } else if (dayData.totalSpent > 0) {
                                                    Brush.verticalGradient(colors = listOf(IndigoSecondary, IndigoPrimary))
                                                } else {
                                                    Brush.verticalGradient(colors = listOf(Color(0xFFCBD5E1), Color(0xFFE2E8F0)))
                                                }
                                            )
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Day X-Axis Label
                                    Text(
                                        text = dayData.dateLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = if (isWeeklyView) 10.sp else 8.sp),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Selected Day Breakdown Card
            selectedDayData?.let { day ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = IndigoContainer.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = day.fullDateStr,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = IndigoPrimary
                            )
                            Text(
                                text = "${day.itemCount} artículo(s) comprados",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = "${String.format(Locale.US, "%.2f", day.totalSpent)} €",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = IndigoPrimary
                        )
                    }
                }
            } ?: run {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "Toca cualquier barra para ver el desglose del día.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
