package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.PurchaseHistoryItem
import com.example.ui.components.ExpenseBarChartComponent
import com.example.ui.theme.IndigoPrimary
import com.example.ui.viewmodel.PantryViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PurchaseHistoryScreen(
    viewModel: PantryViewModel,
    modifier: Modifier = Modifier
) {
    val history by viewModel.purchaseHistory.collectAsState()
    val appSettings by viewModel.appSettings.collectAsState()
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    var showAddDialog by remember { mutableStateOf(false) }

    val totalSpent = remember(history) { history.sumOf { it.price * it.quantity } }
    val totalPurchasedItems = remember(history) { history.sumOf { it.quantity }.toInt() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // User Expense Calculation Analytics Breakdown Card
        UserExpenseAnalyticsCard(history = history, monthlyBudget = appSettings.monthlyBudget)

        // Weekly & Monthly Bar Chart Financial Component
        ExpenseBarChartComponent(history = history)

        // Summary Header Bar & Actions
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.85f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.9f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Registros en Base de Datos Room",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = IndigoPrimary
                    )
                    Text(
                        text = "$totalPurchasedItems artículos registrados en historial",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("add_history_entry_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Añadir Compra Directa", tint = IndigoPrimary)
                    }

                    if (history.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.clearPurchaseHistory() },
                            modifier = Modifier.testTag("clear_history_button")
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Limpiar Historial", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = IndigoPrimary.copy(alpha = 0.5f)
                    )
                    Text("Aún no tienes productos en la base de datos de compras.", style = MaterialTheme.typography.bodyMedium)
                    Text("Marca compras como realizadas o añade registros manuales para guardarlos localmente.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(history, key = { it.id }) { item ->
                    HistoryItemCard(
                        item = item,
                        dateFormat = dateFormat,
                        onDelete = { viewModel.deletePurchaseHistoryItem(item.id) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddManualPurchaseDialog(
            onDismiss = { showAddDialog = false },
            onSave = { newItem ->
                viewModel.addPurchaseHistoryItem(newItem)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun HistoryItemCard(
    item: PurchaseHistoryItem,
    dateFormat: SimpleDateFormat,
    onDelete: () -> Unit
) {
    val finalPrice = item.price * item.quantity

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_item_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.82f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.9f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(16.dp), tint = IndigoPrimary)
                    Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "Cantidad: ${item.quantity} ${item.unit} | Supermercado: ${item.supermarket}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Fecha de compra: ${dateFormat.format(Date(item.purchaseDateMillis))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "${String.format(Locale.US, "%.2f", finalPrice)}€",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = IndigoPrimary
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_history_item_button")
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar Registro",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AddManualPurchaseDialog(
    onDismiss: () -> Unit,
    onSave: (PurchaseHistoryItem) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var quantityText by remember { mutableStateOf("1.0") }
    var unit by remember { mutableStateOf("ud") }
    var priceText by remember { mutableStateOf("2.50") }
    var supermarket by remember { mutableStateOf("Mercadona") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Compra en Room DB") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del Producto") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("purchase_name_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text("Cantidad") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unidad") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Precio Final (€)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("purchase_price_input")
                    )

                    OutlinedTextField(
                        value = supermarket,
                        onValueChange = { supermarket = it },
                        label = { Text("Supermercado") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val qty = quantityText.toDoubleOrNull() ?: 1.0
                        val price = priceText.toDoubleOrNull() ?: 0.0
                        onSave(
                            PurchaseHistoryItem(
                                name = name.trim(),
                                quantity = qty,
                                unit = unit.ifBlank { "ud" },
                                price = price,
                                supermarket = supermarket.ifBlank { "Mercadona" },
                                purchaseDateMillis = System.currentTimeMillis()
                            )
                        )
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("save_manual_purchase_button")
            ) {
                Text("Guardar en Room")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserExpenseAnalyticsCard(
    history: List<PurchaseHistoryItem>,
    monthlyBudget: Double = 0.0
) {
    var selectedBreakdownTab by remember { mutableStateOf(0) } // 0 = Supermercados, 1 = Categorías

    val totalSpent = remember(history) { history.sumOf { it.price * it.quantity } }
    val avgPricePerItem = remember(history) { if (history.isNotEmpty()) totalSpent / history.size else 0.0 }

    val currentMonthSpent = remember(history) {
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)
        history.filter {
            val itemCal = Calendar.getInstance().apply { timeInMillis = it.purchaseDateMillis }
            itemCal.get(Calendar.MONTH) == currentMonth && itemCal.get(Calendar.YEAR) == currentYear
        }.sumOf { it.price * it.quantity }
    }

    val budgetRatio = if (monthlyBudget > 0) (currentMonthSpent / monthlyBudget).toFloat() else 0f

    val supermarketBreakdown = remember(history, totalSpent) {
        if (totalSpent == 0.0) emptyList()
        else history.groupBy { it.supermarket.ifBlank { "General" } }
            .mapValues { entry -> entry.value.sumOf { it.price * it.quantity } }
            .toList()
            .sortedByDescending { it.second }
    }

    val categoryBreakdown = remember(history, totalSpent) {
        if (totalSpent == 0.0) emptyList()
        else history.groupBy { it.foodCategory.ifBlank { "Otros" } }
            .mapValues { entry -> entry.value.sumOf { it.price * it.quantity } }
            .toList()
            .sortedByDescending { it.second }
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.88f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.9f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("user_expense_analytics_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        Icons.Default.Analytics,
                        contentDescription = null,
                        tint = IndigoPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Cálculo de Gastos del Usuario",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = IndigoPrimary
                    )
                }
            }

            // Stat Cards Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExpenseStatBox(
                    label = "Gasto Total",
                    value = "${String.format(Locale.US, "%.2f", totalSpent)} €",
                    color = IndigoPrimary,
                    modifier = Modifier.weight(1f)
                )
                ExpenseStatBox(
                    label = "Este Mes",
                    value = "${String.format(Locale.US, "%.2f", currentMonthSpent)} €",
                    color = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
                ExpenseStatBox(
                    label = "Prom. / Ítem",
                    value = "${String.format(Locale.US, "%.2f", avgPricePerItem)} €",
                    color = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
            }

            if (history.isNotEmpty()) {
                // Breakdown Selector (Supermercado vs Categoría)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedBreakdownTab == 0,
                        onClick = { selectedBreakdownTab = 0 },
                        label = { Text("Por Supermercado", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = IndigoPrimary, selectedLabelColor = Color.White)
                    )

                    FilterChip(
                        selected = selectedBreakdownTab == 1,
                        onClick = { selectedBreakdownTab = 1 },
                        label = { Text("Por Categoría", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = { Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = IndigoPrimary, selectedLabelColor = Color.White)
                    )
                }

                val currentBreakdown = if (selectedBreakdownTab == 0) supermarketBreakdown else categoryBreakdown

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    currentBreakdown.take(4).forEach { (label, spent) ->
                        val ratio = if (totalSpent > 0) (spent / totalSpent).toFloat() else 0f
                        val percentInt = (ratio * 100).toInt()

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${String.format(Locale.US, "%.2f", spent)} € ($percentInt%)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            LinearProgressIndicator(
                                progress = { ratio.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp),
                                color = IndigoPrimary,
                                trackColor = IndigoPrimary.copy(alpha = 0.12f),
                                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseStatBox(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.08f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

