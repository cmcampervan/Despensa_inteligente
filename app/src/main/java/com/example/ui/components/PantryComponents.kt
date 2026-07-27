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
