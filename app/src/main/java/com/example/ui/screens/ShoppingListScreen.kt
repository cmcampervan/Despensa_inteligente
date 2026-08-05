package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.font.FontWeight
import com.example.data.local.ShoppingListItem
import com.example.ui.components.DuplicateWarningBanner
import com.example.ui.components.ImageScanDialog
import com.example.ui.components.ProductDetailHistoryDialog
import com.example.ui.components.ShareFamilyListDialog
import com.example.ui.components.SupermarketPriceComparisonDialog
import com.example.ui.components.VoiceCommandDialog
import com.example.ui.theme.IndigoPrimary
import com.example.ui.viewmodel.PantryViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ShoppingListScreen(
    viewModel: PantryViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val shoppingList by viewModel.shoppingList.collectAsState()
    val duplicateMessage by viewModel.duplicateMessage.collectAsState()
    val isAnalyzingImage by viewModel.isAnalyzingImage.collectAsState()
    val scanErrorMessage by viewModel.scanErrorMessage.collectAsState()
    val scannedProducts by viewModel.scannedProducts.collectAsState()
    val isAlexaSyncing by viewModel.isAlexaSyncing.collectAsState()
    val alexaSyncResult by viewModel.alexaSyncResult.collectAsState()
    val autoGenerateResult by viewModel.autoGenerateResult.collectAsState()
    val pantryItems by viewModel.pantryItems.collectAsState()
    val recipeActionMessage by viewModel.recipeActionMessage.collectAsState()
    val priceComparison by viewModel.priceComparison.collectAsState()
    val isComparingPrices by viewModel.isComparingPrices.collectAsState()
    val productPriceHistory by viewModel.productPriceHistory.collectAsState()
    val isLoadingPriceHistory by viewModel.isLoadingPriceHistory.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(recipeActionMessage) {
        recipeActionMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearRecipeActionMessage()
        }
    }

    val lowStockCount = remember(pantryItems) { pantryItems.count { it.quantity <= it.minThreshold } }
    val expiredCount = remember(pantryItems) { pantryItems.count { it.expirationDateMillis <= System.currentTimeMillis() } }
    val zeroStockCount = remember(pantryItems) { pantryItems.count { it.quantity <= 0.0 } }
    val lowStockOrExpiredTotal = remember(lowStockCount, expiredCount) { lowStockCount + expiredCount }

    val missingItemsCount = remember(shoppingList) { shoppingList.count { it.isMissing } }

    var activeFilter by remember { mutableStateOf("TODOS") } // TODOS, FALTANTES, OFERTAS
    var showShareMenu by remember { mutableStateOf(false) }
    var showShareFamilyDialog by remember { mutableStateOf(false) }

    var showAddItemDialog by remember { mutableStateOf(false) }
    var showScanDialog by remember { mutableStateOf(false) }
    var showVoiceDialog by remember { mutableStateOf(false) }
    var showPriceComparisonDialog by remember { mutableStateOf(false) }
    var comparisonInitialProduct by remember { mutableStateOf("") }
    var showPriceHistoryDialog by remember { mutableStateOf(false) }
    var historyInitialProduct by remember { mutableStateOf("") }

    var newName by remember { mutableStateOf("") }
    var newQty by remember { mutableStateOf("1.0") }
    var newUnit by remember { mutableStateOf("ud") }
    var newLocation by remember { mutableStateOf("ALACENA") }
    var newSupermarket by remember { mutableStateOf("Mercadona") }
    var newPrice by remember { mutableStateOf("0.0") }
    var newIsPromo by remember { mutableStateOf(false) }

    val filteredList = remember(shoppingList, activeFilter) {
        when (activeFilter) {
            "FALTANTES" -> shoppingList.filter { it.isMissing }
            "OFERTAS" -> shoppingList.filter { it.isPromotion }
            else -> shoppingList
        }
    }

    val totalPrice = remember(filteredList) {
        filteredList.sumOf { it.estimatedPrice * it.quantityToBuy }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    contentColor = IndigoPrimary,
                    modifier = Modifier.testTag("shopping_compare_fab")
                ) {
                    Icon(Icons.Default.Compare, contentDescription = "Comparar Precios y Ofertas")
                }

                SmallFloatingActionButton(
                    onClick = { showVoiceDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = Color.White.copy(alpha = 0.9f),
                    contentColor = IndigoPrimary,
                    modifier = Modifier.testTag("shopping_voice_fab")
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "Entrada de Voz")
                }

                SmallFloatingActionButton(
                    onClick = { showScanDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = Color.White.copy(alpha = 0.9f),
                    contentColor = IndigoPrimary,
                    modifier = Modifier.testTag("shopping_scan_fab")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "Escanear foto/folleto")
                }

                FloatingActionButton(
                    onClick = { showAddItemDialog = true },
                    shape = RoundedCornerShape(20.dp),
                    containerColor = IndigoPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("shopping_add_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Añadir a Lista")
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Top Header & Share Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Lista de Compras",
                        style = MaterialTheme.typography.headlineSmall,
                        color = IndigoPrimary
                    )
                    Text(
                        text = "Total estimado: ${String.format("%.2f", totalPrice)}€",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = { viewModel.syncWithAlexa() },
                        enabled = !isAlexaSyncing,
                        modifier = Modifier.testTag("alexa_sync_button")
                    ) {
                        if (isAlexaSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = IndigoPrimary
                            )
                        } else {
                            Icon(
                                Icons.Default.GraphicEq,
                                contentDescription = "Sincronizar con Alexa",
                                tint = IndigoPrimary
                            )
                        }
                    }

                    Box {
                        Button(
                            onClick = { showShareMenu = true },
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                            modifier = Modifier.testTag("share_shopping_list_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Exportar / Compartir")
                        }

                        DropdownMenu(
                            expanded = showShareMenu,
                            onDismissRequest = { showShareMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Invitación / Enlace Familiar", fontWeight = FontWeight.Bold) },
                                leadingIcon = { Icon(Icons.Default.Group, contentDescription = null, tint = IndigoPrimary) },
                                onClick = {
                                    showShareMenu = false
                                    showShareFamilyDialog = true
                                },
                                modifier = Modifier.testTag("share_family_invite_option")
                            )
                            Divider()
                            DropdownMenuItem(
                                text = { Text("Compartir como Texto") },
                                leadingIcon = { Icon(Icons.Default.Send, contentDescription = null, tint = IndigoPrimary) },
                                onClick = {
                                    showShareMenu = false
                                    shareAsText(context, shoppingList, totalPrice)
                                },
                                modifier = Modifier.testTag("share_as_text_option")
                            )
                            DropdownMenuItem(
                                text = { Text("Exportar a archivo CSV") },
                                leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null, tint = IndigoPrimary) },
                                onClick = {
                                    showShareMenu = false
                                    exportAsCsv(context, shoppingList)
                                },
                                modifier = Modifier.testTag("export_as_csv_option")
                            )
                        }
                    }
                }
            }

            // Alexa Sync Status Banner
            alexaSyncResult?.let { result ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                    border = BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = IndigoPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = result.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = { viewModel.clearAlexaSyncResult() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Cerrar",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Auto-Generate Shopping List Card Banner
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                border = BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(IndigoPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoMode, contentDescription = null, tint = Color.White)
                        }
                        Column {
                            Text(
                                text = "Auto-generar desde Inventario",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = IndigoPrimary
                            )
                            Text(
                                text = if (lowStockOrExpiredTotal > 0)
                                    "Detectados: $lowStockCount stock bajo, $expiredCount caducado(s)"
                                else
                                    "Basado en stock bajo y caducados del inventario",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = { viewModel.generateShoppingListFromInventory() },
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("auto_generate_shopping_list_button")
                    ) {
                        Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Generar", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            // Filter Chips Bar (Todos, Faltantes, Ofertas)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = activeFilter == "TODOS",
                    onClick = { activeFilter = "TODOS" },
                    label = { Text("Todos (${shoppingList.size})", style = MaterialTheme.typography.labelMedium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = IndigoPrimary,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("filter_all_button")
                )

                FilterChip(
                    selected = activeFilter == "FALTANTES",
                    onClick = { activeFilter = "FALTANTES" },
                    label = { Text("⚠️ Faltantes ($missingItemsCount)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFDC2626),
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFFFEE2E2),
                        labelColor = Color(0xFF991B1B)
                    ),
                    modifier = Modifier.testTag("filter_missing_button")
                )

                FilterChip(
                    selected = activeFilter == "OFERTAS",
                    onClick = { activeFilter = "OFERTAS" },
                    label = { Text("🏷️ Ofertas (${shoppingList.count { it.isPromotion }})", style = MaterialTheme.typography.labelMedium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = IndigoPrimary,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("filter_promos_button")
                )
            }

            // Duplicate Banner Notice
            duplicateMessage?.let { msg ->
                DuplicateWarningBanner(
                    message = msg,
                    onDismiss = { viewModel.clearDuplicateMessage() }
                )
            }

            // Shopping List Content
            if (filteredList.isEmpty()) {
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
                            Icons.Default.ShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Text(
                            text = if (activeFilter == "FALTANTES") "No hay productos marcados como faltantes."
                            else if (activeFilter == "OFERTAS") "No hay ofertas en la lista."
                            else "Tu lista de compras está vacía.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Los productos con stock 0 en Room se añaden automáticamente como faltantes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredList, key = { it.id }) { item ->
                        ShoppingListItemCard(
                            item = item,
                            onMarkBought = { restock ->
                                viewModel.markShoppingItemBought(item, restock)
                            },
                            onToggleMissing = {
                                viewModel.toggleShoppingItemMissing(item.id)
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
                            onQuantityChange = { newQuantity ->
                                viewModel.updateShoppingItemQuantity(item.id, newQuantity)
                            },
                            onDelete = { viewModel.deleteShoppingItem(item.id) }
                        )
                    }
                }
            }
        }
    }

    // Add Item Dialog
    if (showAddItemDialog) {
        AlertDialog(
            onDismissRequest = { showAddItemDialog = false },
            title = { Text("Añadir a Lista de Compras") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Nombre del Producto") },
                        modifier = Modifier.fillMaxWidth().testTag("shopping_name_input")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newQty,
                            onValueChange = { newQty = it },
                            label = { Text("Cantidad") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newPrice,
                            onValueChange = { newPrice = it },
                            label = { Text("Precio (€)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    var isFetchingOnlinePrice by remember { mutableStateOf(false) }
                    OutlinedButton(
                        onClick = {
                            if (newName.isNotBlank()) {
                                isFetchingOnlinePrice = true
                                coroutineScope.launch {
                                    try {
                                        val scanned = viewModel.fetchOnlinePrice(newName.trim(), newSupermarket)
                                        newPrice = String.format(Locale.US, "%.2f", scanned.estimatedPrice)
                                        if (scanned.supermarket.isNotBlank()) newSupermarket = scanned.supermarket
                                        newIsPromo = scanned.isPromotion
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    } finally {
                                        isFetchingOnlinePrice = false
                                    }
                                }
                            }
                        },
                        enabled = !isFetchingOnlinePrice && newName.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().testTag("shopping_fetch_price_button")
                    ) {
                        if (isFetchingOnlinePrice) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Consultando precio...")
                        } else {
                            Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Consultar Precio en Internet")
                        }
                    }

                    OutlinedTextField(
                        value = newSupermarket,
                        onValueChange = { newSupermarket = it },
                        label = { Text("Supermercado / Tienda") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = newIsPromo, onCheckedChange = { newIsPromo = it })
                        Text("Es una oferta / promoción", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            viewModel.addShoppingItem(
                                name = newName.trim(),
                                quantity = newQty.toDoubleOrNull() ?: 1.0,
                                unit = newUnit,
                                location = newLocation,
                                foodCategory = "Otros",
                                supermarket = newSupermarket,
                                price = newPrice.toDoubleOrNull() ?: 0.0,
                                isPromo = newIsPromo
                            )
                            newName = ""
                            showAddItemDialog = false
                        }
                    },
                    modifier = Modifier.testTag("save_shopping_item_button")
                ) {
                    Text("Agregar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddItemDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    autoGenerateResult?.let { result ->
        AlertDialog(
            onDismissRequest = { viewModel.clearAutoGenerateResult() },
            icon = {
                Icon(
                    if (result.addedCount > 0 || result.updatedCount > 0) Icons.Default.CheckCircle else Icons.Default.Info,
                    contentDescription = null,
                    tint = IndigoPrimary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Generación Automática",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(result.message, style = MaterialTheme.typography.bodyMedium)
                    if (result.processedProducts.isNotEmpty()) {
                        Text(
                            "Productos incluidos (${result.processedProducts.size}):",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = IndigoPrimary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            result.processedProducts.forEach { name ->
                                AssistChip(
                                    onClick = { },
                                    label = { Text(name, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.clearAutoGenerateResult() },
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                ) {
                    Text("Entendido")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showScanDialog) {
        ImageScanDialog(
            isAnalyzing = isAnalyzingImage,
            scannedResults = scannedProducts,
            errorMessage = scanErrorMessage,
            onDismiss = {
                viewModel.clearScannedProducts()
                showScanDialog = false
            },
            onImageSelected = { bmp -> viewModel.analyzeImage(bmp) },
            onAddProductsToInventory = { products ->
                products.forEach { p ->
                    viewModel.addOrUpdatePantryItem(
                        com.example.data.local.PantryItem(
                            name = p.name,
                            quantity = 1.0,
                            locationCategory = p.category,
                            foodCategory = p.foodCategory,
                            expirationDateMillis = System.currentTimeMillis() + 7 * 24 * 3600 * 1000L,
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

    if (showVoiceDialog) {
        VoiceCommandDialog(
            onDismiss = { showVoiceDialog = false },
            onCommandRecognized = { command ->
                viewModel.processVoiceCommand(command)
                showVoiceDialog = false
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

    if (showShareFamilyDialog) {
        ShareFamilyListDialog(
            shoppingList = shoppingList,
            totalPrice = totalPrice,
            onDismiss = { showShareFamilyDialog = false },
            onJoinFamilyList = { inviteCode ->
                viewModel.syncWithAlexa()
            }
        )
    }
}

private fun formatQuantity(quantity: Double): String {
    return if (quantity == quantity.toLong().toDouble()) {
        quantity.toLong().toString()
    } else {
        String.format(Locale.US, "%.1f", quantity)
    }
}

@Composable
fun ShoppingListItemCard(
    item: ShoppingListItem,
    onMarkBought: (restockToPantry: Boolean) -> Unit,
    onToggleMissing: () -> Unit,
    onComparePrice: () -> Unit = {},
    onViewHistory: () -> Unit = {},
    onQuantityChange: (Double) -> Unit = {},
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("shopping_item_${item.id}"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isMissing) Color(0xFFFFF1F2) else Color.White.copy(alpha = 0.82f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (item.isMissing) Color(0xFFFECDD3) else Color.White.copy(alpha = 0.9f)
        ),
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (item.isMissing) {
                        SuggestionChip(
                            onClick = onToggleMissing,
                            label = {
                                Text(
                                    text = if (item.missingReason.isNotBlank()) "⚠️ FALTA (${item.missingReason})" else "⚠️ FALTANTE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = Color(0xFFFEE2E2),
                                labelColor = Color(0xFF991B1B)
                            )
                        )
                    } else if (item.isPromotion) {
                        SuggestionChip(
                            onClick = onComparePrice,
                            label = { Text("¡OFERTA!", style = MaterialTheme.typography.labelSmall) },
                            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val step = if (item.unit.equals("ud", ignoreCase = true)) 1.0 else 0.5
                    IconButton(
                        onClick = { onQuantityChange(item.quantityToBuy - step) },
                        modifier = Modifier.size(28.dp).testTag("decrease_quantity_${item.id}")
                    ) {
                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Reducir cantidad", tint = IndigoPrimary)
                    }
                    Text(
                        text = "${formatQuantity(item.quantityToBuy)} ${item.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(
                        onClick = { onQuantityChange(item.quantityToBuy + step) },
                        modifier = Modifier.size(28.dp).testTag("increase_quantity_${item.id}")
                    ) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = "Aumentar cantidad", tint = IndigoPrimary)
                    }
                    Text(
                        text = "| Supermercado: ${item.supermarket}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (item.estimatedPrice > 0) {
                    Text(
                        text = "Precio aprox: ${String.format(Locale.US, "%.2f", item.estimatedPrice * item.quantityToBuy)}€",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Compare prices button
                IconButton(
                    onClick = onComparePrice,
                    modifier = Modifier.testTag("compare_item_price_button_${item.id}")
                ) {
                    Icon(
                        Icons.Default.Compare,
                        contentDescription = "Comparar Precios",
                        tint = IndigoPrimary
                    )
                }

                // Price history button
                IconButton(
                    onClick = onViewHistory,
                    modifier = Modifier.testTag("history_item_price_button_${item.id}")
                ) {
                    Icon(
                        Icons.Default.ShowChart,
                        contentDescription = "Histórico de Precios",
                        tint = IndigoPrimary
                    )
                }

                // Manual toggle missing status
                IconButton(
                    onClick = onToggleMissing,
                    modifier = Modifier.testTag("toggle_missing_button_${item.id}")
                ) {
                    Icon(
                        imageVector = if (item.isMissing) Icons.Default.Warning else Icons.Default.WarningAmber,
                        contentDescription = if (item.isMissing) "Quitar marca de faltante" else "Marcar como Faltante",
                        tint = if (item.isMissing) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                IconButton(
                    onClick = { onMarkBought(true) },
                    modifier = Modifier.testTag("mark_bought_restock_button")
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Marcar como comprado y reponer alacena",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar de lista",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

private fun shareAsText(context: Context, shoppingList: List<ShoppingListItem>, totalPrice: Double) {
    if (shoppingList.isEmpty()) {
        Toast.makeText(context, "La lista de compras está vacía.", Toast.LENGTH_SHORT).show()
        return
    }
    val listText = shoppingList.joinToString("\n") { item ->
        val status = if (item.isMissing) " [⚠️ FALTANTE]" else if (item.isPromotion) " [🏷️ OFERTA]" else ""
        "• ${item.name}: ${item.quantityToBuy} ${item.unit} (${item.supermarket}) - ${String.format(Locale.US, "%.2f", item.estimatedPrice * item.quantityToBuy)}€$status"
    }
    val shareText = "🛒 LISTA DE COMPRAS - ALACENA VIRTUAL:\n\n$listText\n\nTotal estimado: ${String.format(Locale.US, "%.2f", totalPrice)}€"
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Lista de Compras - Despensa Virtual")
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Compartir Lista de Compras"))
}

private fun exportAsCsv(context: Context, shoppingList: List<ShoppingListItem>) {
    if (shoppingList.isEmpty()) {
        Toast.makeText(context, "La lista de compras está vacía.", Toast.LENGTH_SHORT).show()
        return
    }
    val sb = StringBuilder()
    sb.append("ID,Nombre,Cantidad,Unidad,Ubicacion,Categoria,Supermercado,Precio_Estimado_EUR,Es_Oferta,Es_Faltante,Razon_Faltante\n")
    for (item in shoppingList) {
        val name = item.name.replace("\"", "\"\"")
        val loc = item.locationCategory.replace("\"", "\"\"")
        val cat = item.foodCategory.replace("\"", "\"\"")
        val superm = item.supermarket.replace("\"", "\"\"")
        val reason = item.missingReason.replace("\"", "\"\"")
        sb.append("${item.id},\"$name\",${item.quantityToBuy},\"${item.unit}\",\"$loc\",\"$cat\",\"$superm\",${item.estimatedPrice},${item.isPromotion},${item.isMissing},\"$reason\"\n")
    }

    try {
        val file = java.io.File(context.cacheDir, "lista_compras_alacena.csv")
        file.writeText(sb.toString(), Charsets.UTF_8)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "Lista de Compras CSV - Despensa Virtual")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Exportar Lista en CSV"))
    } catch (e: Exception) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Lista de Compras CSV")
            putExtra(Intent.EXTRA_TEXT, sb.toString())
        }
        context.startActivity(Intent.createChooser(intent, "Exportar CSV como Texto"))
    }
}
