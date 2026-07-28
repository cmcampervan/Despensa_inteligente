package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.*
import com.example.data.remote.AlexaSyncResult
import com.example.data.remote.ProductLabelIngredientAnalysis
import com.example.data.remote.RecipeSuggestion
import com.example.data.remote.ScannedProduct
import com.example.data.repository.AutoGenerateShoppingListResult
import com.example.data.repository.PantryRepository
import com.example.util.CsvExportUtil
import com.example.util.NotificationHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class PantryViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = PantryRepository(
        pantryDao = db.pantryDao(),
        shoppingListDao = db.shoppingListDao(),
        purchaseHistoryDao = db.purchaseHistoryDao(),
        appSettingsDao = db.appSettingsDao()
    )

    // Alexa Sync State
    private val _isAlexaSyncing = MutableStateFlow(false)
    val isAlexaSyncing = _isAlexaSyncing.asStateFlow()

    private val _alexaSyncResult = MutableStateFlow<AlexaSyncResult?>(null)
    val alexaSyncResult = _alexaSyncResult.asStateFlow()

    // Price Comparison State
    private val _priceComparison = MutableStateFlow<ProductSupermarketComparison?>(null)
    val priceComparison = _priceComparison.asStateFlow()

    private val _isComparingPrices = MutableStateFlow(false)
    val isComparingPrices = _isComparingPrices.asStateFlow()

    // Product Price History State
    private val _productPriceHistory = MutableStateFlow<ProductDetailPriceHistory?>(null)
    val productPriceHistory = _productPriceHistory.asStateFlow()

    private val _isLoadingPriceHistory = MutableStateFlow(false)
    val isLoadingPriceHistory = _isLoadingPriceHistory.asStateFlow()

    // Settings
    val appSettings: StateFlow<AppSettings> = repository.settingsFlow
        .map { it ?: AppSettings() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    // Search and Filter State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedLocationFilter = MutableStateFlow("TODOS") // TODOS, ALACENA, NEVERA
    val selectedLocationFilter = _selectedLocationFilter.asStateFlow()

    private val _selectedFoodCategoryFilter = MutableStateFlow("TODAS") // TODAS, Lácteos, Carnes y Pescados, etc.
    val selectedFoodCategoryFilter = _selectedFoodCategoryFilter.asStateFlow()

    private val _selectedExpirationFilter = MutableStateFlow("TODOS") // TODOS, CADUCADOS, POR_CADUCAR, FRESCOS, SIN_STOCK
    val selectedExpirationFilter = _selectedExpirationFilter.asStateFlow()

    // Unfiltered Inventory for Stats & Freshness Summary
    val rawPantryItems: StateFlow<List<PantryItem>> = repository.allPantryItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All Inventory
    val pantryItems: StateFlow<List<PantryItem>> = combine(
        repository.allPantryItems,
        _searchQuery,
        _selectedLocationFilter,
        _selectedFoodCategoryFilter,
        _selectedExpirationFilter
    ) { items, query, location, category, expirationFilter ->
        val now = System.currentTimeMillis()
        val warningDays = appSettings.value.expirationWarningDays
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, warningDays)
        val limitMillis = cal.timeInMillis

        val day3Millis = now + 3L * 24 * 3600 * 1000
        val day7Millis = now + 7L * 24 * 3600 * 1000
        val day15Millis = now + 15L * 24 * 3600 * 1000

        items.filter { item ->
            val matchesQuery = query.isBlank() || item.name.contains(query, ignoreCase = true) ||
                    item.supermarket.contains(query, ignoreCase = true) ||
                    item.barcode.contains(query)
            val matchesLocation = location == "TODOS" || item.locationCategory.uppercase() == location.uppercase()
            val matchesCategory = category == "TODAS" || item.foodCategory.equals(category, ignoreCase = true)
            
            val matchesExpiration = when (expirationFilter) {
                "CADUCADOS" -> item.expirationDateMillis <= now
                "POR_CADUCAR" -> item.expirationDateMillis in (now + 1)..limitMillis
                "CADUCAN_3_DIAS" -> item.expirationDateMillis > now && item.expirationDateMillis <= day3Millis
                "CADUCAN_7_DIAS" -> item.expirationDateMillis > now && item.expirationDateMillis <= day7Millis
                "CADUCAN_15_DIAS" -> item.expirationDateMillis > now && item.expirationDateMillis <= day15Millis
                "FRESCOS" -> item.expirationDateMillis > limitMillis
                "SIN_STOCK" -> item.quantity <= 0.0
                else -> true // TODOS
            }

            matchesQuery && matchesLocation && matchesCategory && matchesExpiration
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Expiring Items
    val expiringItems: StateFlow<List<PantryItem>> = repository.allPantryItems.map { items ->
        val warningDays = appSettings.value.expirationWarningDays
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, warningDays)
        val limit = cal.timeInMillis
        items.filter { it.expirationDateMillis <= limit }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Shopping List
    val shoppingList: StateFlow<List<ShoppingListItem>> = repository.activeShoppingList
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Purchase History
    val purchaseHistory: StateFlow<List<PurchaseHistoryItem>> = repository.purchaseHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Recipe Suggestions AI State
    private val _recipeSuggestions = MutableStateFlow<List<RecipeSuggestion>>(emptyList())
    val recipeSuggestions = _recipeSuggestions.asStateFlow()

    private val _isGeneratingRecipes = MutableStateFlow(false)
    val isGeneratingRecipes = _isGeneratingRecipes.asStateFlow()

    private val _recipeActionMessage = MutableStateFlow<String?>(null)
    val recipeActionMessage = _recipeActionMessage.asStateFlow()

    fun clearRecipeActionMessage() {
        _recipeActionMessage.value = null
    }

    // Gemini Image Scanning State
    private val _scannedProducts = MutableStateFlow<List<ScannedProduct>>(emptyList())
    val scannedProducts = _scannedProducts.asStateFlow()

    private val _isAnalyzingImage = MutableStateFlow(false)
    val isAnalyzingImage = _isAnalyzingImage.asStateFlow()

    // Duplicate Banner Notification State
    private val _duplicateMessage = MutableStateFlow<String?>(null)
    val duplicateMessage = _duplicateMessage.asStateFlow()

    // Auto-Generate Shopping List Result State
    private val _autoGenerateResult = MutableStateFlow<AutoGenerateShoppingListResult?>(null)
    val autoGenerateResult = _autoGenerateResult.asStateFlow()

    // Google Drive Backup State
    private val _isDriveBackingUp = MutableStateFlow(false)
    val isDriveBackingUp = _isDriveBackingUp.asStateFlow()

    private val _driveBackupStatusMessage = MutableStateFlow<String?>(null)
    val driveBackupStatusMessage = _driveBackupStatusMessage.asStateFlow()

    init {
        // Trigger initial check for notification alerts & proactive suggestions & zero stock sync
        viewModelScope.launch {
            pantryItems.collect { items ->
                // Auto sync any items with stock == 0 to shopping list as missing
                repository.syncZeroStockAndMissingItemsToShoppingList()

                val warningDays = appSettings.value.expirationWarningDays
                val now = System.currentTimeMillis()
                val expiringNames = mutableListOf<String>()

                items.forEach { item ->
                    val diffMillis = item.expirationDateMillis - now
                    val daysLeft = (diffMillis / (1000 * 60 * 60 * 24)).toInt()
                    if (daysLeft <= warningDays) {
                        NotificationHelper.showExpirationNotification(getApplication(), item.name, daysLeft)
                        if (daysLeft >= 0) {
                            expiringNames.add(item.name)
                        }
                    }
                }

                if (expiringNames.isNotEmpty()) {
                    triggerProactiveIngredientNotification(expiringNames)
                }
            }
        }
    }

    fun triggerProactiveIngredientNotification(expiringNames: List<String> = emptyList()) {
        viewModelScope.launch {
            val names = if (expiringNames.isNotEmpty()) {
                expiringNames
            } else {
                expiringItems.value.map { it.name }
            }

            if (names.isNotEmpty()) {
                val namesText = names.take(3).joinToString(", ")
                val idea = "Tienes $namesText próximos a vencer. ¿Qué tal cocinarlos hoy con nuestras sugerencias de AI?"
                NotificationHelper.showProactiveIngredientNotification(
                    context = getApplication(),
                    ingredientNames = names,
                    customSuggestion = idea
                )
            }
        }
    }

    fun triggerWorkManagerExpirationCheck() {
        com.example.worker.WorkScheduler.triggerImmediateCheck(getApplication())
    }


    fun syncWithAlexa(overrideSettings: AppSettings? = null) {
        viewModelScope.launch {
            _isAlexaSyncing.value = true
            val result = repository.syncWithAlexa(overrideSettings)
            _alexaSyncResult.value = result
            _isAlexaSyncing.value = false
        }
    }

    fun clearAlexaSyncResult() {
        _alexaSyncResult.value = null
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setLocationFilter(location: String) {
        _selectedLocationFilter.value = location
    }

    fun setFoodCategoryFilter(category: String) {
        _selectedFoodCategoryFilter.value = category
    }

    fun setExpirationFilter(expirationFilter: String) {
        _selectedExpirationFilter.value = expirationFilter
    }

    fun clearAllFilters() {
        _searchQuery.value = ""
        _selectedLocationFilter.value = "TODOS"
        _selectedFoodCategoryFilter.value = "TODAS"
        _selectedExpirationFilter.value = "TODOS"
    }

    suspend fun fetchOnlinePrice(productName: String, supermarket: String = "Mercadona"): ScannedProduct {
        return repository.fetchOnlinePrice(productName, supermarket)
    }

    fun comparePricesForProduct(productName: String) {
        viewModelScope.launch {
            _isComparingPrices.value = true
            _priceComparison.value = repository.compareSupermarketPrices(productName)
            _isComparingPrices.value = false
        }
    }

    fun clearPriceComparison() {
        _priceComparison.value = null
    }

    fun fetchPriceHistoryForProduct(productName: String) {
        viewModelScope.launch {
            _isLoadingPriceHistory.value = true
            _productPriceHistory.value = repository.fetchProductPriceHistory(productName)
            _isLoadingPriceHistory.value = false
        }
    }

    fun clearProductPriceHistory() {
        _productPriceHistory.value = null
    }

    suspend fun fetchProductByBarcode(barcode: String): ScannedProduct {
        return repository.fetchProductByBarcode(barcode)
    }

    suspend fun analyzeProductLabelAndIngredients(bitmap: Bitmap): ProductLabelIngredientAnalysis {
        return repository.analyzeProductLabelAndIngredients(bitmap)
    }

    fun clearDuplicateMessage() {
        _duplicateMessage.value = null
    }

    fun generateShoppingListFromInventory() {
        viewModelScope.launch {
            val result = repository.generateShoppingListFromLowStockAndExpired(getApplication())
            _autoGenerateResult.value = result
        }
    }

    fun clearAutoGenerateResult() {
        _autoGenerateResult.value = null
    }

    fun addOrUpdatePantryItem(item: PantryItem) {
        viewModelScope.launch {
            repository.addOrUpdatePantryItem(getApplication(), item)
        }
    }

    fun addBatchPantryItems(items: List<PantryItem>) {
        viewModelScope.launch {
            items.forEach { item ->
                repository.addOrUpdatePantryItem(getApplication(), item)
            }
            _recipeActionMessage.value = "🎤 Se han guardado ${items.size} productos en la Alacena."
        }
    }

    fun deletePantryItem(item: PantryItem) {
        viewModelScope.launch {
            repository.deletePantryItem(item)
        }
    }

    fun markPantryItemAsMissing(item: PantryItem) {
        viewModelScope.launch {
            repository.markPantryItemAsMissing(item)
        }
    }

    fun toggleShoppingItemMissing(itemId: Int) {
        viewModelScope.launch {
            repository.toggleShoppingItemMissing(itemId)
        }
    }

    fun syncZeroStockItems() {
        viewModelScope.launch {
            repository.syncZeroStockAndMissingItemsToShoppingList()
        }
    }

    fun addShoppingItem(name: String, quantity: Double, unit: String, location: String, foodCategory: String, supermarket: String, price: Double, isPromo: Boolean) {
        viewModelScope.launch {
            val (isDuplicate, message) = repository.addShoppingListItem(
                name, quantity, unit, location, foodCategory, supermarket, price, isPromo
            )
            if (isDuplicate) {
                _duplicateMessage.value = message
            }
        }
    }

    fun markShoppingItemBought(item: ShoppingListItem, addToPantry: Boolean) {
        viewModelScope.launch {
            repository.markShoppingListItemBought(item, addToPantry)
            checkAndNotifyMonthlyBudget()
        }
    }

    fun deleteShoppingItem(id: Int) {
        viewModelScope.launch {
            repository.deleteShoppingListItem(id)
        }
    }

    fun addPurchaseHistoryItem(item: PurchaseHistoryItem) {
        viewModelScope.launch {
            repository.addPurchaseHistoryItem(item)
            checkAndNotifyMonthlyBudget()
        }
    }

    fun deletePurchaseHistoryItem(id: Int) {
        viewModelScope.launch {
            repository.deletePurchaseHistoryItem(id)
        }
    }

    fun clearPurchaseHistory() {
        viewModelScope.launch {
            repository.clearPurchaseHistory()
        }
    }

    fun updateSettings(settings: AppSettings) {
        viewModelScope.launch {
            repository.saveAppSettings(settings)
            checkAndNotifyMonthlyBudget()
        }
    }

    fun checkAndNotifyMonthlyBudget() {
        val budget = appSettings.value.monthlyBudget
        if (budget <= 0.0) return

        val history = purchaseHistory.value
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)
        val currentMonthSpent = history.filter {
            val itemCal = Calendar.getInstance().apply { timeInMillis = it.purchaseDateMillis }
            itemCal.get(Calendar.MONTH) == currentMonth && itemCal.get(Calendar.YEAR) == currentYear
        }.sumOf { it.price * it.quantity }

        val percentage = (currentMonthSpent / budget) * 100.0
        if (percentage >= 100.0) {
            NotificationHelper.showBudgetAlertNotification(
                context = getApplication(),
                spent = currentMonthSpent,
                budget = budget,
                alertLevel = 100
            )
        } else if (percentage >= 80.0) {
            NotificationHelper.showBudgetAlertNotification(
                context = getApplication(),
                spent = currentMonthSpent,
                budget = budget,
                alertLevel = 80
            )
        }
    }

    fun analyzeImage(bitmap: Bitmap) {
        viewModelScope.launch {
            _isAnalyzingImage.value = true
            val results = repository.analyzeImageWithGemini(bitmap)
            _scannedProducts.value = results
            _isAnalyzingImage.value = false
        }
    }

    fun clearScannedProducts() {
        _scannedProducts.value = emptyList()
    }

    fun generateRecipes() {
        viewModelScope.launch {
            _isGeneratingRecipes.value = true
            val warningDays = appSettings.value.expirationWarningDays
            val recipes = repository.generateRecipesWithGemini(warningDays)
            _recipeSuggestions.value = recipes
            _isGeneratingRecipes.value = false
        }
    }

    fun addRecipeIngredientsToShoppingList(ingredients: List<String>) {
        viewModelScope.launch {
            val addedCount = repository.addRecipeIngredientsToShoppingList(ingredients)
            if (addedCount > 0) {
                _recipeActionMessage.value = "Se han añadido $addedCount ingrediente(s) faltante(s) a la Lista de Compras."
            } else {
                _recipeActionMessage.value = "Los ingredientes ya estaban en la Lista de Compras."
            }
        }
    }

    fun processVoiceCommand(command: String) {
        val raw = command.trim()
        if (raw.isBlank()) return

        val lowerRaw = raw.lowercase(Locale.getDefault())

        // Detect Assistant Wake Word
        val assistantLabel = when {
            lowerRaw.contains("ok google") || lowerRaw.contains("hey google") || lowerRaw.contains("oye google") || lowerRaw.contains("google") -> "Ok Google"
            lowerRaw.contains("alexa") || lowerRaw.contains("oye alexa") -> "Alexa"
            lowerRaw.contains("gemini") -> "Gemini AI"
            else -> "Asistente"
        }

        // Clean command by stripping hotwords
        val cleanCommand = raw
            .replace(Regex("(?i)\\b(ok\\s+google|hey\\s+google|oye\\s+google|google|alexa|oye\\s+alexa|gemini|asistente)\\b"), "")
            .trim()
            .trim(',', '.', ':', ';', '!')

        val lower = cleanCommand.lowercase(Locale.getDefault())

        viewModelScope.launch {
            // Check for recipes query
            val isRecipe = lower.contains("receta") || lower.contains("sugerir") || lower.contains("cocinar") || lower.contains("qué preparo") || lower.contains("que preparo")
            if (isRecipe) {
                generateRecipes()
                _recipeActionMessage.value = "🎤 [$assistantLabel] Generando recetas con los ingredientes disponibles..."
                return@launch
            }

            // Check if removal / deletion action
            val isRemove = lower.contains("eliminar") || lower.contains("elimina") ||
                    lower.contains("borrar") || lower.contains("borra") ||
                    lower.contains("quitar") || lower.contains("quita") ||
                    lower.contains("sacar") || lower.contains("saca") ||
                    lower.contains("delete") || lower.contains("remove")

            val isShoppingListExplicit = lower.contains("lista") || lower.contains("comprar") || lower.contains("compras")
            val isPantryExplicit = lower.contains("alacena") || lower.contains("nevera") || lower.contains("despensa") || lower.contains("frigorifico") || lower.contains("frigorífico")

            if (isRemove) {
                val targetName = cleanCommand
                    .replace(Regex("(?i)(eliminar|elimina|borrar|borra|quitar|quita|sacar|saca|delete|remove)"), "")
                    .replace(Regex("(?i)(de la lista de compras|de la lista de la compra|de la lista|de la alacena|de la nevera|de la despensa|del frigorífico|del frigorifico|de la|del|de|el|la|los|las|un|una)"), "")
                    .trim()
                    .trim(',', '.', ':', ';')

                if (targetName.isBlank()) {
                    _recipeActionMessage.value = "🎤 [$assistantLabel] Por favor especifica el nombre del producto a eliminar."
                    return@launch
                }

                var deleted = false
                var foundLocationLabel = ""

                // 1. Try shopping list if explicitly mentioned or default
                if (isShoppingListExplicit || (!isPantryExplicit && !isShoppingListExplicit)) {
                    val itemInList = shoppingList.value.find { it.name.contains(targetName, ignoreCase = true) }
                    if (itemInList != null) {
                        deleteShoppingItem(itemInList.id)
                        deleted = true
                        foundLocationLabel = "Lista de Compras"
                    }
                }

                // 2. Try pantry if explicitly mentioned or not found in shopping list
                if (!deleted && (isPantryExplicit || !isShoppingListExplicit)) {
                    val itemInPantry = pantryItems.value.find { it.name.contains(targetName, ignoreCase = true) }
                    if (itemInPantry != null) {
                        deletePantryItem(itemInPantry)
                        deleted = true
                        foundLocationLabel = if (itemInPantry.locationCategory == "NEVERA") "Nevera" else "Alacena"
                    }
                }

                if (deleted) {
                    _recipeActionMessage.value = "🎤 [$assistantLabel] Eliminado '$targetName' de la $foundLocationLabel."
                } else {
                    _recipeActionMessage.value = "🎤 [$assistantLabel] No se encontró '$targetName' para eliminar."
                }
                return@launch
            }

            // ADD / PURCHASE Action
            var extractedQty = 1.0
            val qtyMatch = Regex("(\\d+(?:[.,]\\d+)?)").find(cleanCommand)
            if (qtyMatch != null) {
                extractedQty = qtyMatch.value.replace(',', '.').toDoubleOrNull() ?: 1.0
            }

            var extractedUnit = "ud"
            val unitMatch = Regex("(?i)(litros?|l|kilos?|kg|gramos?|g|paquetes?|paq|latas?|unidades?|ud|botes?|cajas?|cartones?|docenas?)").find(cleanCommand)
            if (unitMatch != null) {
                val u = unitMatch.value.lowercase(Locale.getDefault())
                extractedUnit = when {
                    u.startsWith("litr") || u == "l" -> "L"
                    u.startsWith("kilo") || u == "kg" -> "kg"
                    u.startsWith("gram") || u == "g" -> "g"
                    u.startsWith("paquet") || u == "paq" -> "paq"
                    u.startsWith("lata") -> "lata"
                    u.startsWith("bote") -> "bote"
                    u.startsWith("caja") -> "caja"
                    u.startsWith("carton") || u.startsWith("cartón") -> "cartón"
                    u.startsWith("docen") -> "docena"
                    else -> "ud"
                }
            }

            val rawName = cleanCommand
                .replace(Regex("(?i)(añadir|añade|agregar|agrega|comprar|compra|pon|pone|poner|meter|mete|guardar|guarda)"), "")
                .replace(Regex("(?i)(a la alacena|en la alacena|a la nevera|en la nevera|a la despensa|en la despensa|a la lista de compras|a la lista de la compra|a la lista|de compras|en la lista)"), "")
                .replace(Regex("(?i)(\\d+(?:[.,]\\d+)?|litros?|kilos?|kg|gramos?|g|paquetes?|paq|latas?|unidades?|ud|botes?|cajas?|cartones?|docenas?|l)"), "")
                .replace(Regex("(?i)\\b(un|una|unos|unas|el|la|los|las|de)\\b"), "")
                .trim()
                .trim(',', '.', ':', ';')

            val cleanName = if (rawName.length >= 2) rawName else cleanCommand.trim()
            val location = if (lower.contains("nevera") || lower.contains("frigorifico") || lower.contains("frigorífico")) "NEVERA" else "ALACENA"

            val scanned = repository.categorizeProductWithGemini(cleanName)
            val finalName = scanned.name.ifBlank { cleanName }

            val targetShopping = isShoppingListExplicit || (!isPantryExplicit && (lower.contains("comprar") || lower.contains("compra") || lower.contains("lista")))

            if (targetShopping) {
                val (isDup, msg) = repository.addShoppingListItem(
                    name = finalName,
                    quantity = extractedQty,
                    unit = extractedUnit,
                    location = location,
                    foodCategory = scanned.foodCategory,
                    supermarket = scanned.supermarket,
                    price = scanned.estimatedPrice,
                    isPromo = scanned.isPromotion
                )
                _recipeActionMessage.value = if (isDup) msg else "🎤 [$assistantLabel] Añadido '$finalName' ($extractedQty $extractedUnit) a la Lista de Compras."
            } else {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, 10)
                addOrUpdatePantryItem(
                    PantryItem(
                        name = finalName,
                        quantity = extractedQty,
                        unit = extractedUnit,
                        locationCategory = location,
                        foodCategory = scanned.foodCategory,
                        expirationDateMillis = cal.timeInMillis,
                        supermarket = scanned.supermarket,
                        price = scanned.estimatedPrice,
                        isPromotion = scanned.isPromotion,
                        conservationTip = scanned.conservationTip
                    )
                )
                _recipeActionMessage.value = "🎤 [$assistantLabel] Añadido '$finalName' ($extractedQty $extractedUnit) a la $location."
            }
        }
    }

    fun exportInventoryCsv() {
        CsvExportUtil.exportPantryToCsv(getApplication(), pantryItems.value)
    }

    fun exportHistoryCsv() {
        CsvExportUtil.exportHistoryToCsv(getApplication(), purchaseHistory.value)
    }

    fun performGoogleDriveBackup(token: String = "") {
        viewModelScope.launch {
            _isDriveBackingUp.value = true
            _driveBackupStatusMessage.value = null
            val result = repository.performGoogleDriveBackup(getApplication(), token)
            _isDriveBackingUp.value = false
            _driveBackupStatusMessage.value = result.message
        }
    }

    fun restoreFromGoogleDriveBackup(token: String = "") {
        viewModelScope.launch {
            _isDriveBackingUp.value = true
            _driveBackupStatusMessage.value = null
            val (success, message) = repository.restoreDataFromGoogleDrive(getApplication(), token)
            _isDriveBackingUp.value = false
            _driveBackupStatusMessage.value = message
        }
    }

    fun clearDriveBackupStatusMessage() {
        _driveBackupStatusMessage.value = null
    }
}
