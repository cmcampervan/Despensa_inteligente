package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.data.local.PantryItem
import com.example.ui.components.*
import com.example.ui.theme.BlueFridge
import com.example.ui.viewmodel.PantryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: PantryViewModel,
    modifier: Modifier = Modifier
) {
    val items by viewModel.pantryItems.collectAsState()
    val rawItems by viewModel.rawPantryItems.collectAsState()
    val expiringItems by viewModel.expiringItems.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedLocation by viewModel.selectedLocationFilter.collectAsState()
    val selectedFoodCategory by viewModel.selectedFoodCategoryFilter.collectAsState()
    val selectedExpirationFilter by viewModel.selectedExpirationFilter.collectAsState()
    val isAnalyzingImage by viewModel.isAnalyzingImage.collectAsState()
    val scannedProducts by viewModel.scannedProducts.collectAsState()
    val priceComparison by viewModel.priceComparison.collectAsState()
    val isComparingPrices by viewModel.isComparingPrices.collectAsState()
    val productPriceHistory by viewModel.productPriceHistory.collectAsState()
    val isLoadingPriceHistory by viewModel.isLoadingPriceHistory.collectAsState()

    var showAddEditDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<PantryItem?>(null) }
    var showVoiceDialog by remember { mutableStateOf(false) }
    var showBatchVoiceDialog by remember { mutableStateOf(false) }
    var showPriceComparisonDialog by remember { mutableStateOf(false) }
    var comparisonInitialProduct by remember { mutableStateOf("") }
    var showPriceHistoryDialog by remember { mutableStateOf(false) }
    var historyInitialProduct by remember { mutableStateOf("") }
    var showScanDialog by remember { mutableStateOf(false) }
    var showLabelScannerDialog by remember { mutableStateOf(false) }
    var showBarcodeScannerDialog by remember { mutableStateOf(false) }
    var showFreshnessDetailDialog by remember { mutableStateOf(false) }

    val foodCategories = listOf("TODAS", "Lácteos", "Carnes y Pescados", "Frutas y Verduras", "Granos y Cereales", "Bebidas", "Enlatados", "Snacks", "Congelados", "Otros")
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SmallFloatingActionButton(
                    onClick = {
                        comparisonInitialProduct = ""
                        showPriceComparisonDialog = true
                    },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = Color.White.copy(alpha = 0.9f),
                    contentColor = com.example.ui.theme.IndigoPrimary,
                    modifier = Modifier.testTag("inventory_compare_fab")
                ) {
                    Icon(Icons.Default.Compare, contentDescription = "Comparar Precios y Ofertas")
                }

                SmallFloatingActionButton(
                    onClick = { showBatchVoiceDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = Color.White.copy(alpha = 0.9f),
                    contentColor = com.example.ui.theme.IndigoPrimary,
                    modifier = Modifier.testTag("batch_voice_fab")
                ) {
                    Icon(Icons.Default.RecordVoiceOver, contentDescription = "Carga Masiva por Voz")
                }

                SmallFloatingActionButton(
                    onClick = { showVoiceDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = Color.White.copy(alpha = 0.9f),
                    contentColor = com.example.ui.theme.IndigoPrimary,
                    modifier = Modifier.testTag("voice_command_fab")
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "Comando de voz")
                }

                SmallFloatingActionButton(
                    onClick = { showBarcodeScannerDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = Color.White.copy(alpha = 0.9f),
                    contentColor = com.example.ui.theme.IndigoPrimary,
                    modifier = Modifier.testTag("scan_barcode_fab")
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Escanear Código de Barras ML Kit")
                }

                SmallFloatingActionButton(
                    onClick = { showLabelScannerDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = Color.White.copy(alpha = 0.9f),
                    contentColor = com.example.ui.theme.IndigoPrimary,
                    modifier = Modifier.testTag("scan_label_fab")
                ) {
                    Icon(Icons.Default.DocumentScanner, contentDescription = "Escanear Etiqueta de Ingredientes")
                }

                SmallFloatingActionButton(
                    onClick = { showScanDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = Color.White.copy(alpha = 0.9f),
                    contentColor = com.example.ui.theme.IndigoPrimary,
                    modifier = Modifier.testTag("scan_photo_fab")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "Escanear ticket o foto")
                }

                FloatingActionButton(
                    onClick = {
                        itemToEdit = null
                        showAddEditDialog = true
                    },
                    shape = RoundedCornerShape(20.dp),
                    containerColor = com.example.ui.theme.IndigoPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_product_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Agregar Producto")
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))

                // Hero Banner with Stats & Expense Calculation
                val lowStockCount = items.count { it.quantity <= it.minThreshold }
                val totalPantryValue = items.sumOf { it.price * it.quantity }
                HeroPantryBanner(
                    totalItems = items.size,
                    expiringCount = expiringItems.size,
                    lowStockCount = lowStockCount,
                    totalValue = totalPantryValue
                )
            }

            item {
                // Resumen Visual de Frescura (Código de colores: Verde / Amarillo / Rojo)
                FreshnessSummaryCard(
                    items = rawItems,
                    selectedFilter = selectedExpirationFilter,
                    onSelectFilter = { filter -> viewModel.setExpirationFilter(filter) },
                    onOpenDetail = { showFreshnessDetailDialog = true }
                )
            }

            item {
                // Glass Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Buscar producto, marca o supermercado...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = com.example.ui.theme.IndigoPrimary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color.White.copy(alpha = 0.75f),
                        focusedContainerColor = Color.White.copy(alpha = 0.92f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.8f),
                        focusedBorderColor = com.example.ui.theme.IndigoPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("inventory_search_input")
                )
            }

            item {
                // Location Filters (TODOS, ALACENA, NEVERA)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedLocation == "TODOS",
                        onClick = { viewModel.setLocationFilter("TODOS") },
                        label = { Text("Todos") },
                        modifier = Modifier.weight(1f).testTag("filter_todos")
                    )
                    FilterChip(
                        selected = selectedLocation == "ALACENA",
                        onClick = { viewModel.setLocationFilter("ALACENA") },
                        label = { Text("Alacena") },
                        leadingIcon = { Icon(Icons.Default.Kitchen, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.weight(1f).testTag("filter_alacena")
                    )
                    FilterChip(
                        selected = selectedLocation == "NEVERA",
                        onClick = { viewModel.setLocationFilter("NEVERA") },
                        label = { Text("Nevera") },
                        leadingIcon = { Icon(Icons.Default.AcUnit, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.weight(1f).testTag("filter_nevera")
                    )
                }
            }

            item {
                // Food Categories Horizontal Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(foodCategories) { cat ->
                        FilterChip(
                            selected = selectedFoodCategory == cat,
                            onClick = { viewModel.setFoodCategoryFilter(cat) },
                            label = { Text(cat, style = MaterialTheme.typography.bodySmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = com.example.ui.theme.IndigoPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            item {
                // Expiration Status Filters Horizontal Chips
                val expirationFilters = listOf(
                    "TODOS" to "Todos",
                    "CADUCAN_3_DIAS" to "⚡ Caducan ≤ 3 días",
                    "CADUCAN_7_DIAS" to "⏱️ Caducan ≤ 7 días",
                    "CADUCAN_15_DIAS" to "📅 Caducan ≤ 15 días",
                    "CADUCADOS" to "🔴 Caducados",
                    "POR_CADUCAR" to "⚠️ Por Caducar",
                    "FRESCOS" to "🟢 En buen estado",
                    "SIN_STOCK" to "📦 Sin stock"
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(expirationFilters) { (key, label) ->
                        FilterChip(
                            selected = selectedExpirationFilter == key,
                            onClick = { viewModel.setExpirationFilter(key) },
                            label = { Text(label, style = MaterialTheme.typography.bodySmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = when (key) {
                                    "CADUCADOS", "SIN_STOCK" -> Color(0xFFDC2626)
                                    "CADUCAN_3_DIAS" -> Color(0xFFE53935)
                                    "CADUCAN_7_DIAS", "POR_CADUCAR" -> Color(0xFFD97706)
                                    "CADUCAN_15_DIAS" -> Color(0xFF2563EB)
                                    else -> com.example.ui.theme.IndigoPrimary
                                },
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("filter_expiration_$key")
                        )
                    }
                }
            }

            val hasActiveFilters = searchQuery.isNotBlank() || selectedLocation != "TODOS" || selectedFoodCategory != "TODAS" || selectedExpirationFilter != "TODOS"

            if (hasActiveFilters) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Mostrando ${items.size} resultado(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(
                            onClick = { viewModel.clearAllFilters() },
                            modifier = Modifier.testTag("clear_all_filters_button")
                        ) {
                            Icon(Icons.Default.FilterAltOff, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Limpiar filtros", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // Inventory List
            if (items.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Inventory,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                            Text(
                                text = if (hasActiveFilters) "No se encontraron productos con estos filtros." else "No hay productos en la alacena.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (hasActiveFilters) {
                                OutlinedButton(
                                    onClick = { viewModel.clearAllFilters() },
                                    modifier = Modifier.testTag("reset_filters_empty_button")
                                ) {
                                    Text("Restablecer Filtros")
                                }
                            } else {
                                Text(
                                    "Usa el botón '+' o escanea con la cámara para agregar.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                items(items, key = { it.id }) { item ->
                    PantryItemCard(
                        item = item,
                        dateFormat = dateFormat,
                        onEdit = {
                            itemToEdit = item
                            showAddEditDialog = true
                        },
                        onMarkMissing = {
                            viewModel.markPantryItemAsMissing(item)
                        },
                        onComparePrice = {
                            viewModel.comparePricesForProduct(item.name)
                            comparisonInitialProduct = item.name
                            showPriceComparisonDialog = true
                        },
                        onViewHistory = {
                            viewModel.fetchPriceHistoryForProduct(item.name)
                            historyInitialProduct = item.name
                            showPriceHistoryDialog = true
                        },
                        onDelete = { viewModel.deletePantryItem(item) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Dialogs
    if (showBarcodeScannerDialog) {
        CameraXBarcodeScannerDialog(
            onDismiss = { showBarcodeScannerDialog = false },
            onBarcodeScanned = { scannedProduct ->
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, 10)
                viewModel.addOrUpdatePantryItem(
                    PantryItem(
                        name = scannedProduct.name,
                        quantity = 1.0,
                        unit = "ud",
                        locationCategory = scannedProduct.category,
                        foodCategory = scannedProduct.foodCategory,
                        expirationDateMillis = cal.timeInMillis,
                        price = scannedProduct.estimatedPrice,
                        supermarket = scannedProduct.supermarket,
                        isPromotion = scannedProduct.isPromotion,
                        conservationTip = scannedProduct.conservationTip
                    )
                )
                showBarcodeScannerDialog = false
            },
            onFetchProductByBarcode = { code ->
                viewModel.fetchProductByBarcode(code)
            }
        )
    }

    if (showAddEditDialog) {
        AddEditProductDialog(
            initialItem = itemToEdit,
            onDismiss = { showAddEditDialog = false },
            onSave = { newItem ->
                viewModel.addOrUpdatePantryItem(newItem)
                showAddEditDialog = false
            },
            onFetchOnlinePrice = { productName, supermarket ->
                viewModel.fetchOnlinePrice(productName, supermarket)
            },
            onFetchProductByBarcode = { code ->
                viewModel.fetchProductByBarcode(code)
            }
        )
    }

    if (showLabelScannerDialog) {
        CameraXLabelIngredientScannerDialog(
            onDismiss = { showLabelScannerDialog = false },
            onAnalyzeLabel = { bitmap ->
                viewModel.analyzeProductLabelAndIngredients(bitmap)
            },
            onSaveToPantry = { productName, ingredients ->
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, 10)
                viewModel.addOrUpdatePantryItem(
                    PantryItem(
                        name = productName,
                        quantity = 1.0,
                        unit = "ud",
                        locationCategory = "Alacena",
                        foodCategory = "Otros",
                        expirationDateMillis = cal.timeInMillis,
                        price = 2.50,
                        supermarket = "General",
                        conservationTip = "Ingredientes identificados: " + ingredients.take(4).joinToString(", ")
                    )
                )
                showLabelScannerDialog = false
            }
        )
    }

    if (showVoiceDialog) {
        VoiceCommandDialog(
            onDismiss = { showVoiceDialog = false },
            onCommandRecognized = { command ->
                viewModel.processVoiceCommand(command)
                showVoiceDialog = false
            }
        )
    }

    if (showBatchVoiceDialog) {
        BatchVoiceInputDialog(
            onDismiss = { showBatchVoiceDialog = false },
            onSaveBatch = { items ->
                viewModel.addBatchPantryItems(items)
                showBatchVoiceDialog = false
            }
        )
    }

    if (showPriceComparisonDialog) {
        SupermarketPriceComparisonDialog(
            comparison = priceComparison,
            isLoading = isComparingPrices,
            initialProductName = comparisonInitialProduct,
            onDismiss = {
                showPriceComparisonDialog = false
                viewModel.clearPriceComparison()
            },
            onSearchProduct = { query ->
                viewModel.comparePricesForProduct(query)
            },
            onSelectSupermarketOffer = { offer ->
                if (comparisonInitialProduct.isNotBlank()) {
                    viewModel.addShoppingItem(
                        name = comparisonInitialProduct,
                        quantity = 1.0,
                        unit = "ud",
                        location = "ALACENA",
                        foodCategory = priceComparison?.foodCategory ?: "Otros",
                        supermarket = offer.supermarket,
                        price = offer.offerPrice,
                        isPromo = offer.isPromotion
                    )
                }
                showPriceComparisonDialog = false
            },
            onViewPriceHistory = { prodName ->
                viewModel.fetchPriceHistoryForProduct(prodName)
                historyInitialProduct = prodName
                showPriceHistoryDialog = true
            }
        )
    }

    if (showPriceHistoryDialog) {
        ProductDetailHistoryDialog(
            historyData = productPriceHistory,
            isLoading = isLoadingPriceHistory,
            initialProductName = historyInitialProduct,
            onDismiss = {
                showPriceHistoryDialog = false
                viewModel.clearProductPriceHistory()
            },
            onSearchProduct = { query ->
                viewModel.fetchPriceHistoryForProduct(query)
            },
            onAddOfferToShoppingList = { supermarket, price ->
                if (historyInitialProduct.isNotBlank()) {
                    viewModel.addShoppingItem(
                        name = historyInitialProduct,
                        quantity = 1.0,
                        unit = "ud",
                        location = "ALACENA",
                        foodCategory = productPriceHistory?.foodCategory ?: "Otros",
                        supermarket = supermarket,
                        price = price,
                        isPromo = true
                    )
                }
            }
        )
    }

    if (showScanDialog) {
        ImageScanDialog(
            isAnalyzing = isAnalyzingImage,
            scannedResults = scannedProducts,
            onDismiss = {
                viewModel.clearScannedProducts()
                showScanDialog = false
            },
            onImageSelected = { bmp -> viewModel.analyzeImage(bmp) },
            onAddProductsToInventory = { products ->
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, 7)
                products.forEach { p ->
                    viewModel.addOrUpdatePantryItem(
                        PantryItem(
                            name = p.name,
                            quantity = 1.0,
                            locationCategory = p.category,
                            foodCategory = p.foodCategory,
                            expirationDateMillis = cal.timeInMillis,
                            supermarket = p.supermarket,
                            price = p.estimatedPrice,
                            isPromotion = p.isPromotion,
                            conservationTip = p.conservationTip
                        )
                    )
                }
            },
            onAddProductsToShoppingList = { products ->
                products.forEach { p ->
                    viewModel.addShoppingItem(
                        name = p.name,
                        quantity = 1.0,
                        unit = "ud",
                        location = p.category,
                        foodCategory = p.foodCategory,
                        supermarket = p.supermarket,
                        price = p.estimatedPrice,
                        isPromo = p.isPromotion
                    )
                }
            }
        )
    }

    if (showFreshnessDetailDialog) {
        FreshnessDetailDialog(
            items = rawItems,
            onDismiss = { showFreshnessDetailDialog = false },
            onAddItemsToShoppingList = { itemsToAdd ->
                itemsToAdd.forEach { p ->
                    viewModel.addShoppingItem(
                        name = p.name,
                        quantity = 1.0,
                        unit = p.unit,
                        location = p.locationCategory,
                        foodCategory = p.foodCategory,
                        supermarket = p.supermarket,
                        price = p.price,
                        isPromo = p.isPromotion
                    )
                }
            },
            onFilterSelect = { filter ->
                viewModel.setExpirationFilter(filter)
                showFreshnessDetailDialog = false
            }
        )
    }
}

@Composable
fun PantryItemCard(
    item: PantryItem,
    dateFormat: SimpleDateFormat,
    onEdit: () -> Unit,
    onMarkMissing: () -> Unit,
    onComparePrice: () -> Unit = {},
    onViewHistory: () -> Unit = {},
    onDelete: () -> Unit
) {
    var expandedTip by remember { mutableStateOf(false) }

    val daysLeft = remember(item.expirationDateMillis) {
        val now = System.currentTimeMillis()
        val diff = item.expirationDateMillis - now
        (diff / (1000 * 60 * 60 * 24)).toInt()
    }

    val isZeroStock = item.quantity <= 0.0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pantry_item_${item.id}"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isZeroStock) Color(0xFFFFF1F2) else Color.White.copy(alpha = 0.82f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isZeroStock) Color(0xFFFECDD3) else Color.White.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (item.imageUri.isNotBlank()) {
                        AsyncImage(
                            model = item.imageUri,
                            contentDescription = "Foto de ${item.name}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(if (item.locationCategory.uppercase() == "NEVERA") BlueFridge else MaterialTheme.colorScheme.primary)
                        )
                    }
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                ExpirationStatusTag(daysLeft = daysLeft)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cantidad: ${item.quantity} ${item.unit} | ${item.locationCategory} • ${item.foodCategory}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isZeroStock) {
                    Text(
                        text = "⚠️ SIN STOCK (Faltante)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFDC2626),
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                } else if (item.quantity <= item.minThreshold) {
                    Text(
                        text = "Stock Bajo",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Supermercado: ${item.supermarket} | Precio: ${if (item.price > 0) String.format(Locale.US, "%.2f€", item.price) else "N/D"} | Caduca: ${dateFormat.format(Date(item.expirationDateMillis))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row {
                    if (item.conservationTip.isNotBlank()) {
                        IconButton(onClick = { expandedTip = !expandedTip }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Info, contentDescription = "Tip conservación", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    IconButton(
                        onClick = onComparePrice,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("compare_pantry_item_${item.id}")
                    ) {
                        Icon(
                            Icons.Default.Compare,
                            contentDescription = "Comparar Precios y Ofertas",
                            modifier = Modifier.size(18.dp),
                            tint = com.example.ui.theme.IndigoPrimary
                        )
                    }
                    IconButton(
                        onClick = onViewHistory,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("history_pantry_item_${item.id}")
                    ) {
                        Icon(
                            Icons.Default.ShowChart,
                            contentDescription = "Ver Histórico de Precios",
                            modifier = Modifier.size(18.dp),
                            tint = com.example.ui.theme.IndigoPrimary
                        )
                    }
                    IconButton(
                        onClick = onMarkMissing,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("mark_missing_pantry_item_${item.id}")
                    ) {
                        Icon(
                            Icons.Default.AddShoppingCart,
                            contentDescription = "Marcar como Faltante en Lista de Compras",
                            modifier = Modifier.size(18.dp),
                            tint = com.example.ui.theme.IndigoPrimary
                        )
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            AnimatedVisibility(visible = expandedTip && item.conservationTip.isNotBlank()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                ) {
                    Text(
                        text = "💡 Conservación: ${item.conservationTip}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}
