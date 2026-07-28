package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.RedExpired
import com.example.ui.theme.GreenFresh
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.testTag
import com.example.data.local.PantryItem

@Composable
fun FrostedGlassMeshBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Soft ambient mesh glows
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Top-Left Soft Blue/Indigo Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFBFDBFE).copy(alpha = 0.45f),
                        Color(0xFF818CF8).copy(alpha = 0.25f),
                        Color.Transparent
                    ),
                    center = androidx.compose.ui.geometry.Offset(width * 0.1f, height * 0.15f),
                    radius = width * 0.7f
                ),
                center = androidx.compose.ui.geometry.Offset(width * 0.1f, height * 0.15f),
                radius = width * 0.7f
            )

            // Right-Middle Soft Purple Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFE9D5FF).copy(alpha = 0.40f),
                        Color(0xFFC084FC).copy(alpha = 0.20f),
                        Color.Transparent
                    ),
                    center = androidx.compose.ui.geometry.Offset(width * 0.9f, height * 0.45f),
                    radius = width * 0.8f
                ),
                center = androidx.compose.ui.geometry.Offset(width * 0.9f, height * 0.45f),
                radius = width * 0.8f
            )
        }

        content()
    }
}

@Composable
fun HeroPantryBanner(
    totalItems: Int,
    expiringCount: Int,
    lowStockCount: Int,
    totalValue: Double = 0.0
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.img_pantry_hero_1784898458252),
                contentDescription = "Pantry Hero Banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.35f),
                                Color.Black.copy(alpha = 0.65f)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DespensaSmart",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    if (totalValue > 0) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.25f)
                        ) {
                            Text(
                                text = "Valor: ${String.format(java.util.Locale.US, "%.2f", totalValue)}€",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatPill(
                        label = "Total",
                        value = totalItems.toString(),
                        bgColor = Color.White.copy(alpha = 0.25f),
                        textColor = Color.White
                    )
                    if (expiringCount > 0) {
                        StatPill(
                            label = "Por Vencer",
                            value = expiringCount.toString(),
                            bgColor = RedExpired.copy(alpha = 0.9f),
                            textColor = Color.White
                        )
                    }
                    if (lowStockCount > 0) {
                        StatPill(
                            label = "Stock Bajo",
                            value = lowStockCount.toString(),
                            bgColor = AmberWarning.copy(alpha = 0.9f),
                            textColor = Color.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatPill(
    label: String,
    value: String,
    bgColor: Color,
    textColor: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(0.5.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = "$label: $value",
            style = MaterialTheme.typography.bodySmall,
            color = textColor
        )
    }
}

@Composable
fun ExpirationStatusTag(daysLeft: Int) {
    val (bgColor, text, textColor, borderColor) = when {
        daysLeft <= 0 -> Quadruple(RedExpired.copy(alpha = 0.15f), "VENCIDO", RedExpired, RedExpired.copy(alpha = 0.3f))
        daysLeft <= 3 -> Quadruple(AmberWarning.copy(alpha = 0.15f), "$daysLeft d (Próximo)", AmberWarning, AmberWarning.copy(alpha = 0.3f))
        else -> Quadruple(Color(0xFFECFDF5), "Frescos ($daysLeft d)", Color(0xFF059669), Color(0xFFA7F3D0))
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = textColor
        )
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun DuplicateWarningBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.85f)),
        border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = AmberWarning)
                Text(message, style = MaterialTheme.typography.bodyMedium)
            }
            TextButton(onClick = onDismiss) {
                Text("Entendido", color = IndigoPrimary)
            }
        }
    }
}

@Composable
fun FreshnessSummaryCard(
    items: List<PantryItem>,
    selectedFilter: String,
    onSelectFilter: (String) -> Unit,
    onOpenDetail: () -> Unit,
    modifier: Modifier = Modifier
) {
    val now = remember { System.currentTimeMillis() }
    
    // Group items into 3 freshness buckets
    val redItems = remember(items, now) {
        items.filter {
            val days = (it.expirationDateMillis - now) / (1000 * 3600 * 24)
            days <= 3
        }
    }
    val yellowItems = remember(items, now) {
        items.filter {
            val days = (it.expirationDateMillis - now) / (1000 * 3600 * 24)
            days in 4..7
        }
    }
    val greenItems = remember(items, now) {
        items.filter {
            val days = (it.expirationDateMillis - now) / (1000 * 3600 * 24)
            days > 7
        }
    }

    val total = items.size
    val greenPct = if (total > 0) greenItems.size.toFloat() / total else 0f
    val yellowPct = if (total > 0) yellowItems.size.toFloat() / total else 0f
    val redPct = if (total > 0) redItems.size.toFloat() / total else 0f

    val freshnessHealthScore = if (total > 0) {
        ((greenItems.size * 100 + yellowItems.size * 50) / total)
    } else 100

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("freshness_summary_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.85f)
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.9f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row
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
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(IndigoPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = "Frescura",
                            tint = IndigoPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Estado de Frescura",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Índice de frescura: $freshnessHealthScore% óptimo",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                TextButton(
                    onClick = onOpenDetail,
                    modifier = Modifier.testTag("freshness_detail_button")
                ) {
                    Text("Detalles", style = MaterialTheme.typography.labelLarge, color = IndigoPrimary)
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = IndigoPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Segmented Multi-Color Progress Bar (Verde / Amarillo / Rojo)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE2E8F0)),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (total == 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFCBD5E1))
                    )
                } else {
                    if (greenPct > 0) {
                        Box(
                            modifier = Modifier
                                .weight(greenPct)
                                .fillMaxHeight()
                                .background(GreenFresh)
                        )
                    }
                    if (yellowPct > 0) {
                        Box(
                            modifier = Modifier
                                .weight(yellowPct)
                                .fillMaxHeight()
                                .background(AmberWarning)
                        )
                    }
                    if (redPct > 0) {
                        Box(
                            modifier = Modifier
                                .weight(redPct)
                                .fillMaxHeight()
                                .background(RedExpired)
                        )
                    }
                }
            }

            // 3 Interactive Color Stat Cards (Verde / Amarillo / Rojo)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // VERDE (Frescos)
                FreshnessStatChip(
                    title = "Frescos",
                    subtitle = "> 7 días",
                    count = greenItems.size,
                    dotColor = GreenFresh,
                    isSelected = selectedFilter == "FRESCOS",
                    onClick = {
                        onSelectFilter(if (selectedFilter == "FRESCOS") "TODOS" else "FRESCOS")
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("freshness_chip_green")
                )

                // AMARILLO (Precaución)
                FreshnessStatChip(
                    title = "Próximos",
                    subtitle = "4-7 días",
                    count = yellowItems.size,
                    dotColor = AmberWarning,
                    isSelected = selectedFilter == "CADUCAN_7_DIAS" || selectedFilter == "POR_CADUCAR",
                    onClick = {
                        onSelectFilter(if (selectedFilter == "CADUCAN_7_DIAS") "TODOS" else "CADUCAN_7_DIAS")
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("freshness_chip_yellow")
                )

                // ROJO (Urgentes)
                FreshnessStatChip(
                    title = "Urgentes",
                    subtitle = "≤ 3d / Venc.",
                    count = redItems.size,
                    dotColor = RedExpired,
                    isSelected = selectedFilter == "CADUCAN_3_DIAS" || selectedFilter == "CADUCADOS",
                    onClick = {
                        onSelectFilter(if (selectedFilter == "CADUCAN_3_DIAS") "TODOS" else "CADUCAN_3_DIAS")
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("freshness_chip_red")
                )
            }
        }
    }
}

@Composable
fun FreshnessStatChip(
    title: String,
    subtitle: String,
    count: Int,
    dotColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) dotColor.copy(alpha = 0.22f) else dotColor.copy(alpha = 0.08f),
        border = BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) dotColor else dotColor.copy(alpha = 0.35f)
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                color = dotColor
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreshnessDetailDialog(
    items: List<PantryItem>,
    onDismiss: () -> Unit,
    onAddItemsToShoppingList: (List<PantryItem>) -> Unit,
    onFilterSelect: (String) -> Unit
) {
    val now = remember { System.currentTimeMillis() }
    val dateFormat = remember { java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()) }

    var selectedTab by remember { mutableStateOf(0) } // 0: Todos, 1: 🔴 Urgentes, 2: 🟡 Próximos, 3: 🟢 Frescos

    val redItems = remember(items, now) {
        items.filter { (it.expirationDateMillis - now) / (1000 * 3600 * 24) <= 3 }
            .sortedBy { it.expirationDateMillis }
    }
    val yellowItems = remember(items, now) {
        items.filter {
            val d = (it.expirationDateMillis - now) / (1000 * 3600 * 24)
            d in 4..7
        }.sortedBy { it.expirationDateMillis }
    }
    val greenItems = remember(items, now) {
        items.filter { (it.expirationDateMillis - now) / (1000 * 3600 * 24) > 7 }
            .sortedBy { it.expirationDateMillis }
    }

    val displayList = when (selectedTab) {
        1 -> redItems
        2 -> yellowItems
        3 -> greenItems
        else -> items.sortedBy { it.expirationDateMillis }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = IndigoPrimary)
                Text("Desglose de Frescura")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Batch Action Button if red/yellow items exist
                val urgentCount = redItems.size + yellowItems.size
                if (urgentCount > 0) {
                    Button(
                        onClick = {
                            onAddItemsToShoppingList(redItems + yellowItems)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Añadir $urgentCount urgentes/próximos a compras")
                    }
                }

                // Filter Tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Todos (${items.size})") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("🔴 Urgentes (${redItems.size})", color = RedExpired) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("🟡 Próximos (${yellowItems.size})", color = AmberWarning) }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("🟢 Frescos (${greenItems.size})", color = GreenFresh) }
                    )
                }

                if (displayList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No hay productos en esta categoría.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(displayList.size) { idx ->
                            val item = displayList[idx]
                            val daysLeft = ((item.expirationDateMillis - now) / (1000 * 3600 * 24)).toInt()
                            val (badgeBg, badgeText, badgeColor) = when {
                                daysLeft <= 0 -> Triple(RedExpired.copy(alpha = 0.15f), "🔴 VENCIDO", RedExpired)
                                daysLeft <= 3 -> Triple(RedExpired.copy(alpha = 0.15f), "🔴 $daysLeft d (Urgente)", RedExpired)
                                daysLeft <= 7 -> Triple(AmberWarning.copy(alpha = 0.15f), "🟡 $daysLeft d (Próximo)", AmberWarning)
                                else -> Triple(GreenFresh.copy(alpha = 0.15f), "🟢 $daysLeft d (Fresco)", GreenFresh)
                            }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            item.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                        )
                                        Text(
                                            "${item.locationCategory} • ${item.quantity} ${item.unit} | Caduca: ${dateFormat.format(java.util.Date(item.expirationDateMillis))}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = badgeBg,
                                        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            badgeText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = badgeColor,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}
