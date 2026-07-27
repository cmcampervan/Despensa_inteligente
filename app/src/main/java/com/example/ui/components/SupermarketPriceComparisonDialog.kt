package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.ProductSupermarketComparison
import com.example.data.local.SupermarketOffer
import com.example.ui.theme.IndigoPrimary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupermarketPriceComparisonDialog(
    comparison: ProductSupermarketComparison?,
    isLoading: Boolean,
    initialProductName: String = "",
    onDismiss: () -> Unit,
    onSearchProduct: (String) -> Unit,
    onSelectSupermarketOffer: (SupermarketOffer) -> Unit = {},
    onViewPriceHistory: (String) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf(initialProductName) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f)
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
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
                                    .size(42.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Compare,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    "Comparador de Precios y Ofertas",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Mercadona, Carrefour, Lidl, Dia, Alcampo, etc.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Search input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Buscar producto (ej. Aceite, Leche, Atún)...") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("comparison_search_input")
                        )

                        Button(
                            onClick = {
                                if (searchQuery.isNotBlank()) {
                                    onSearchProduct(searchQuery.trim())
                                }
                            },
                            enabled = !isLoading && searchQuery.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("comparison_search_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Text("Comparar")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Content
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(44.dp),
                                    color = IndigoPrimary
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "Buscando precios y ofertas activas en directo...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else if (comparison == null) {
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
                                    Icons.Default.Storefront,
                                    contentDescription = null,
                                    modifier = Modifier.size(52.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    "Escribe un producto arriba o selecciona uno de tu lista para comparar los precios con ofertas incluidas en supermercados de España.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        // Summary Banner
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            comparison.productName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        AssistChip(
                                            onClick = {},
                                            label = { Text("Ahorra hasta ${comparison.maxSavingsPercentage}%") },
                                            colors = AssistChipDefaults.assistChipColors(
                                                containerColor = Color(0xFFE8F5E9),
                                                labelColor = Color(0xFF2E7D32)
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        comparison.comparisonSummary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = { onViewPriceHistory(comparison.productName) },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("view_history_from_comparison_button")
                                    ) {
                                        Icon(
                                            Icons.Default.TrendingDown,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "Ver Histórico (6 meses)",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Offers List
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(comparison.offers) { offer ->
                                SupermarketOfferCard(
                                    offer = offer,
                                    onSelect = { onSelectSupermarketOffer(offer) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Text("Cerrar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun SupermarketOfferCard(
    offer: SupermarketOffer,
    onSelect: () -> Unit
) {
    val isCheapest = offer.isCheapest
    val containerColor = if (isCheapest) {
        Color(0xFFF1F8E9)
    } else {
        MaterialTheme.colorScheme.surface
    }

    val borderColor = if (isCheapest) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outlineVariant

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(if (isCheapest) 2.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
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
                    // Supermarket icon/badge
                    val badgeColor = when (offer.supermarket.lowercase(Locale.getDefault())) {
                        "mercadona" -> Color(0xFF007A33)
                        "carrefour" -> Color(0xFF003399)
                        "lidl" -> Color(0xFF0050AA)
                        "dia" -> Color(0xFFE2001A)
                        "alcampo" -> Color(0xFFE2001A)
                        "eroski" -> Color(0xFFE30613)
                        else -> IndigoPrimary
                    }

                    Box(
                        modifier = Modifier
                            .background(badgeColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            offer.supermarket,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    if (isCheapest) {
                        Surface(
                            color = Color(0xFF2E7D32),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "★ MÁS BARATO",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Prices
                Column(horizontalAlignment = Alignment.End) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (offer.isPromotion && offer.regularPrice > offer.offerPrice) {
                            Text(
                                "${String.format(Locale.US, "%.2f", offer.regularPrice)}€",
                                style = MaterialTheme.typography.bodySmall,
                                textDecoration = TextDecoration.LineThrough,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Text(
                            "${String.format(Locale.US, "%.2f", offer.offerPrice)}€",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isCheapest) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (offer.unitPriceInfo.isNotBlank()) {
                        Text(
                            offer.unitPriceInfo,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Promotion Tag & Select action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (offer.isPromotion && offer.offerDescription.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.LocalOffer,
                            contentDescription = null,
                            tint = Color(0xFFE65100),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            offer.offerDescription,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFE65100)
                        )
                    }
                } else {
                    Text(
                        offer.validUntil,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                TextButton(
                    onClick = onSelect,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("select_supermarket_${offer.supermarket.lowercase(Locale.getDefault())}")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Comprar en ${offer.supermarket}", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
