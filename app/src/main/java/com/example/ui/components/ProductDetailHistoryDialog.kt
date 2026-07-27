package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.PriceHistoryPoint
import com.example.data.local.ProductDetailPriceHistory
import com.example.data.local.SupermarketPriceTrend
import com.example.ui.theme.IndigoPrimary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailHistoryDialog(
    historyData: ProductDetailPriceHistory?,
    isLoading: Boolean,
    initialProductName: String = "",
    onDismiss: () -> Unit,
    onSearchProduct: (String) -> Unit,
    onAddOfferToShoppingList: (supermarket: String, price: Double) -> Unit = { _, _ -> }
) {
    var searchQuery by remember(initialProductName) { mutableStateOf(initialProductName) }
    var selectedSupermarketFilter by remember { mutableStateOf("TODOS") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Column {
                            Text(
                                "Detalle e Histórico de Precios",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Evolución histórica y tendencias por supermercado",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_detail_history_dialog")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar producto (ej. Aceite de Oliva)...") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("history_search_input")
                    )

                    Button(
                        onClick = {
                            if (searchQuery.isNotBlank()) {
                                onSearchProduct(searchQuery.trim())
                            }
                        },
                        enabled = !isLoading && searchQuery.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("history_search_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Text("Analizar")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                color = IndigoPrimary
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                "Analizando precios históricos de los últimos 6 meses...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (historyData == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.ShowChart,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "Escribe el nombre de un producto para generar su gráfica de evolución de precios y tendencias.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Product Title & Summary Stat Cards
                        item {
                            ProductHeaderStats(historyData)
                        }

                        // AI Trend Recommendation Banner
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Psychology,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Análisis de Tendencia Inteligente",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            historyData.overallRecommendation,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                                        )
                                    }
                                }
                            }
                        }

                        // Chart section header
                        item {
                            Text(
                                "Evolución de Precio (Últimos 6 meses)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Supermarket Filter Chips for Chart
                        item {
                            val listSupermarkets = listOf("TODOS") + historyData.supermarketTrends.map { it.supermarket }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(listSupermarkets) { sup ->
                                    val isSelected = selectedSupermarketFilter == sup
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedSupermarketFilter = sup },
                                        label = { Text(sup) },
                                        leadingIcon = if (isSelected) {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                        } else null
                                    )
                                }
                            }
                        }

                        // Price Chart Canvas
                        item {
                            val displayedTrends = if (selectedSupermarketFilter == "TODOS") {
                                historyData.supermarketTrends
                            } else {
                                historyData.supermarketTrends.filter { it.supermarket == selectedSupermarketFilter }
                            }

                            PriceEvolutionChartCard(trends = displayedTrends)
                        }

                        // Supermarket Breakdown Title
                        item {
                            Text(
                                "Tendencias por Supermercado",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Supermarket Cards
                        items(historyData.supermarketTrends) { trend ->
                            SupermarketTrendCard(
                                trend = trend,
                                onAddToList = {
                                    onAddOfferToShoppingList(trend.supermarket, trend.currentPrice)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductHeaderStats(historyData: ProductDetailPriceHistory) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        historyData.productName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        "Categoría: ${historyData.foodCategory}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Storefront,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "Mejor: ${historyData.bestSupermarketToBuy}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Stat 1: Lowest Current Price
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFFE8F5E9), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Mínimo Actual", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E7D32))
                    Text(
                        "${String.format(Locale.US, "%.2f", historyData.currentLowestPrice)}€",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF2E7D32)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Stat 2: Average Price
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Precio Medio", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${String.format(Locale.US, "%.2f", historyData.averageMarketPrice)}€",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun PriceEvolutionChartCard(trends: List<SupermarketPriceTrend>) {
    val allPoints = trends.flatMap { it.history }
    if (allPoints.isEmpty()) return

    val minPrice = (allPoints.minOf { it.price } * 0.90).coerceAtLeast(0.1)
    val maxPrice = (allPoints.maxOf { it.price } * 1.10)
    val monthsList = trends.firstOrNull()?.history?.map { it.monthLabel } ?: listOf("Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Actual")

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                trends.forEach { t ->
                    val color = getSupermarketColor(t.supermarket)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(color, CircleShape)
                        )
                        Text(t.supermarket, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Chart Drawing
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                val width = size.width
                val height = size.height

                val paddingX = 40f
                val paddingY = 20f
                val graphWidth = width - (paddingX * 2)
                val graphHeight = height - (paddingY * 2)

                val priceRange = maxPrice - minPrice
                val stepX = if (monthsList.size > 1) graphWidth / (monthsList.size - 1) else graphWidth

                // Draw Grid Lines
                val gridLineCount = 3
                for (i in 0..gridLineCount) {
                    val y = paddingY + (graphHeight / gridLineCount) * i
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.4f),
                        start = Offset(paddingX, y),
                        end = Offset(width - paddingX, y),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                    )
                }

                // Draw lines for each supermarket
                trends.forEach { t ->
                    val strokeColor = getSupermarketColor(t.supermarket)
                    val points = t.history

                    if (points.isNotEmpty()) {
                        val path = Path()

                        points.forEachIndexed { index, point ->
                            val x = paddingX + (index * stepX)
                            val normalizedY = (point.price - minPrice) / (if (priceRange > 0) priceRange else 1.0)
                            val y = height - paddingY - (normalizedY.toFloat() * graphHeight)

                            if (index == 0) {
                                path.moveTo(x, y)
                            } else {
                                path.lineTo(x, y)
                            }

                            // Draw circle point
                            drawCircle(
                                color = strokeColor,
                                radius = 4.dp.toPx(),
                                center = Offset(x, y)
                            )
                        }

                        drawPath(
                            path = path,
                            color = strokeColor,
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Month Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                monthsList.forEach { month ->
                    Text(
                        month,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SupermarketTrendCard(
    trend: SupermarketPriceTrend,
    onAddToList: () -> Unit
) {
    val isDown = trend.trend == "DOWN"
    val isUp = trend.trend == "UP"

    val trendColor = when {
        isDown -> Color(0xFF2E7D32)
        isUp -> Color(0xFFC62828)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val trendIcon = when {
        isDown -> Icons.Default.TrendingDown
        isUp -> Icons.Default.TrendingUp
        else -> Icons.Default.TrendingFlat
    }

    val trendText = when {
        isDown -> "BAJANDO (${String.format(Locale.US, "%.1f", trend.percentageChange)}%)"
        isUp -> "SUBIENDO (+${String.format(Locale.US, "%.1f", trend.percentageChange)}%)"
        else -> "PRECIO ESTABLE"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val badgeColor = getSupermarketColor(trend.supermarket)
                    Box(
                        modifier = Modifier
                            .background(badgeColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            trend.supermarket,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Trend badge
                    Surface(
                        color = trendColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(trendIcon, contentDescription = null, tint = trendColor, modifier = Modifier.size(14.dp))
                            Text(
                                trendText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = trendColor
                            )
                        }
                    }
                }

                // Price
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${String.format(Locale.US, "%.2f", trend.currentPrice)}€",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        "Antes: ${String.format(Locale.US, "%.2f", trend.previousPrice)}€",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Min & Max range bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Mín: ${String.format(Locale.US, "%.2f", trend.lowestInPeriod)}€",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Máx 6 meses: ${String.format(Locale.US, "%.2f", trend.highestInPeriod)}€",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TextButton(
                    onClick = onAddToList,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Comprar aquí", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

fun getSupermarketColor(supermarket: String): Color {
    return when (supermarket.lowercase(Locale.getDefault())) {
        "mercadona" -> Color(0xFF007A33)
        "carrefour" -> Color(0xFF003399)
        "lidl" -> Color(0xFF0050AA)
        "dia" -> Color(0xFFE2001A)
        "alcampo" -> Color(0xFFD32F2F)
        "eroski" -> Color(0xFFE30613)
        else -> IndigoPrimary
    }
}
