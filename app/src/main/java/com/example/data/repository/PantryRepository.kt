package com.example.data.repository

import android.content.Context
import com.example.data.local.*
import com.example.data.remote.AlexaSyncManager
import com.example.data.remote.AlexaSyncResult
import com.example.data.remote.GeminiService
import com.example.data.remote.ProductLabelIngredientAnalysis
import com.example.data.remote.RecipeSuggestion
import com.example.data.remote.ScannedProduct
import com.example.util.ConservationTips
import com.example.util.GeminiResponseCache
import com.example.util.GoogleDriveBackupManager
import com.example.util.GoogleDriveBackupResult
import com.example.util.NotificationHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.Calendar

data class AutoGenerateShoppingListResult(
    val addedCount: Int,
    val updatedCount: Int,
    val lowStockCount: Int,
    val expiredCount: Int,
    val processedProducts: List<String>,
    val message: String
)

class PantryRepository(
    private val pantryDao: PantryDao,
    private val shoppingListDao: ShoppingListDao,
    private val purchaseHistoryDao: PurchaseHistoryDao,
    private val appSettingsDao: AppSettingsDao,
    geminiCacheDao: GeminiCacheDao? = null,
    private val geminiService: GeminiService = GeminiService(),
    private val openFoodFactsService: com.example.data.remote.OpenFoodFactsService = com.example.data.remote.OpenFoodFactsService(),
    private val alexaSyncManager: AlexaSyncManager = AlexaSyncManager(shoppingListDao)
) {
    // Si no se pasa geminiCacheDao (código antiguo que aún no lo conoce), la caché queda
    // deshabilitada y el repositorio funciona exactamente como antes, sin romper nada.
    private val geminiCache: GeminiResponseCache? = geminiCacheDao?.let { GeminiResponseCache(it) }

    val allPantryItems: Flow<List<PantryItem>> = pantryDao.getAllPantryItems()
    val activeShoppingList: Flow<List<ShoppingListItem>> = shoppingListDao.getActiveShoppingList()
    val purchaseHistory: Flow<List<PurchaseHistoryItem>> = purchaseHistoryDao.getAllPurchaseHistory()
    val settingsFlow: Flow<AppSettings?> = appSettingsDao.getSettingsFlow()

    fun getPantryItemsByLocation(location: String): Flow<List<PantryItem>> =
        pantryDao.getPantryItemsByLocation(location)

    fun getExpiringItems(warningDays: Int): Flow<List<PantryItem>> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, warningDays)
        return pantryDao.getExpiringItems(calendar.timeInMillis)
    }

    suspend fun getAppSettings(): AppSettings {
        return appSettingsDao.getSettingsOnce() ?: AppSettings().also {
            appSettingsDao.saveSettings(it)
        }
    }

    suspend fun saveAppSettings(settings: AppSettings) {
        appSettingsDao.saveSettings(settings)
    }

    suspend fun addOrUpdatePantryItem(context: Context, item: PantryItem) {
        val tip = if (item.conservationTip.isBlank()) {
            ConservationTips.getTipForProduct(item.name, item.foodCategory, item.locationCategory)
        } else item.conservationTip

        val itemWithTip = item.copy(conservationTip = tip)
        pantryDao.insertPantryItem(itemWithTip)

        // Check if quantity is below threshold -> add to shopping list automatically
        val settings = getAppSettings()
        if (settings.autoAddToShoppingList && itemWithTip.quantity <= itemWithTip.minThreshold) {
            checkAndAddToShoppingListAuto(context, itemWithTip)
        }
    }

    suspend fun deletePantryItem(item: PantryItem) {
        pantryDao.deletePantryItem(item)
    }

    suspend fun deletePantryItemById(id: Int) {
        pantryDao.deletePantryItemById(id)
    }

    suspend fun syncZeroStockAndMissingItemsToShoppingList() {
        val allItems = pantryDao.getAllPantryItemsList()
        val zeroStockItems = allItems.filter { it.quantity <= 0.0 }
        
        for (item in zeroStockItems) {
            val duplicate = shoppingListDao.findDuplicateInShoppingList(item.name)
            if (duplicate == null) {
                shoppingListDao.insertShoppingListItem(
                    ShoppingListItem(
                        pantryItemId = item.id,
                        name = item.name,
                        quantityToBuy = if (item.minThreshold > 0) item.minThreshold else 1.0,
                        unit = item.unit,
                        locationCategory = item.locationCategory,
                        foodCategory = item.foodCategory,
                        supermarket = item.supermarket,
                        estimatedPrice = item.price,
                        isPromotion = item.isPromotion,
                        isMissing = true,
                        missingReason = "Stock 0 en Alacena"
                    )
                )
            } else if (!duplicate.isMissing) {
                shoppingListDao.updateShoppingListItem(
                    duplicate.copy(
                        isMissing = true,
                        missingReason = "Stock 0 en Alacena"
                    )
                )
            }
        }
    }

    suspend fun markPantryItemAsMissing(pantryItem: PantryItem) {
        val duplicate = shoppingListDao.findDuplicateInShoppingList(pantryItem.name)
        if (duplicate == null) {
            shoppingListDao.insertShoppingListItem(
                ShoppingListItem(
                    pantryItemId = pantryItem.id,
                    name = pantryItem.name,
                    quantityToBuy = if (pantryItem.minThreshold > 0) pantryItem.minThreshold else 1.0,
                    unit = pantryItem.unit,
                    locationCategory = pantryItem.locationCategory,
                    foodCategory = pantryItem.foodCategory,
                    supermarket = pantryItem.supermarket,
                    estimatedPrice = pantryItem.price,
                    isPromotion = pantryItem.isPromotion,
                    isMissing = true,
                    missingReason = "Marcado Manual"
                )
            )
        } else {
            shoppingListDao.updateShoppingListItem(
                duplicate.copy(
                    isMissing = true,
                    missingReason = "Marcado Manual"
                )
            )
        }
    }

    suspend fun toggleShoppingItemMissing(itemId: Int) {
        val activeList = shoppingListDao.getAllActiveShoppingListOnce()
        val item = activeList.find { it.id == itemId }
        if (item != null) {
            val newMissing = !item.isMissing
            val reason = if (newMissing) "Marcado Manual" else ""
            shoppingListDao.updateShoppingListItem(item.copy(isMissing = newMissing, missingReason = reason))
        }
    }

    suspend fun checkAndAddToShoppingListAuto(context: Context, item: PantryItem) {
        val duplicate = shoppingListDao.findDuplicateInShoppingList(item.name)
        val isZeroStock = item.quantity <= 0.0
        if (duplicate == null) {
            shoppingListDao.insertShoppingListItem(
                ShoppingListItem(
                    pantryItemId = item.id,
                    name = item.name,
                    quantityToBuy = maxOf(1.0, (item.minThreshold * 2) - item.quantity),
                    unit = item.unit,
                    locationCategory = item.locationCategory,
                    foodCategory = item.foodCategory,
                    supermarket = item.supermarket,
                    estimatedPrice = item.price,
                    isPromotion = item.isPromotion,
                    isMissing = isZeroStock,
                    missingReason = if (isZeroStock) "Stock 0 en Alacena" else ""
                )
            )
            NotificationHelper.showLowStockNotification(context, item.name)
        } else if (isZeroStock && !duplicate.isMissing) {
            shoppingListDao.updateShoppingListItem(
                duplicate.copy(
                    isMissing = true,
                    missingReason = "Stock 0 en Alacena"
                )
            )
        }
    }

    suspend fun generateShoppingListFromLowStockAndExpired(context: Context): AutoGenerateShoppingListResult {
        val lowStockItems = pantryDao.getLowStockItems()
        val now = System.currentTimeMillis()
        val expiredItems = pantryDao.getExpiringItemsList(now)

        val combinedItems = (lowStockItems + expiredItems).distinctBy { it.id }

        if (combinedItems.isEmpty()) {
            return AutoGenerateShoppingListResult(
                addedCount = 0,
                updatedCount = 0,
                lowStockCount = 0,
                expiredCount = 0,
                processedProducts = emptyList(),
                message = "No se encontraron productos con stock bajo o caducados en la despensa."
            )
        }

        var addedCount = 0
        var updatedCount = 0
        val processedNames = mutableListOf<String>()

        val lowStockCount = combinedItems.count { it.quantity <= it.minThreshold }
        val expiredCount = combinedItems.count { it.expirationDateMillis <= now }

        for (item in combinedItems) {
            val qtyToBuy = if (item.quantity <= item.minThreshold) {
                maxOf(1.0, (item.minThreshold * 2) - item.quantity)
            } else {
                1.0
            }

            val duplicate = shoppingListDao.findDuplicateInShoppingList(item.name)
            if (duplicate != null) {
                val updated = duplicate.copy(
                    quantityToBuy = maxOf(duplicate.quantityToBuy, qtyToBuy),
                    estimatedPrice = if (item.price > 0) item.price else duplicate.estimatedPrice,
                    supermarket = if (item.supermarket.isNotBlank()) item.supermarket else duplicate.supermarket
                )
                shoppingListDao.updateShoppingListItem(updated)
                updatedCount++
            } else {
                shoppingListDao.insertShoppingListItem(
                    ShoppingListItem(
                        pantryItemId = item.id,
                        name = item.name,
                        quantityToBuy = qtyToBuy,
                        unit = item.unit,
                        locationCategory = item.locationCategory,
                        foodCategory = item.foodCategory,
                        supermarket = item.supermarket,
                        estimatedPrice = item.price,
                        isPromotion = item.isPromotion
                    )
                )
                addedCount++
            }
            processedNames.add(item.name)
        }

        val message = "Generación completada: $addedCount añadido(s) y $updatedCount actualizado(s) ($lowStockCount stock bajo, $expiredCount caducado(s))."

        if (addedCount > 0) {
            NotificationHelper.showLowStockNotification(context, "$addedCount productos añadidos a la lista de compras")
        }

        return AutoGenerateShoppingListResult(
            addedCount = addedCount,
            updatedCount = updatedCount,
            lowStockCount = lowStockCount,
            expiredCount = expiredCount,
            processedProducts = processedNames,
            message = message
        )
    }

    suspend fun addShoppingListItem(name: String, quantity: Double, unit: String, location: String, foodCategory: String, supermarket: String, price: Double, isPromo: Boolean): Pair<Boolean, String> {
        val duplicate = shoppingListDao.findDuplicateInShoppingList(name)
        if (duplicate != null) {
            // Update quantity of existing item
            val updated = duplicate.copy(
                quantityToBuy = duplicate.quantityToBuy + quantity,
                estimatedPrice = if (price > 0) price else duplicate.estimatedPrice,
                isPromotion = isPromo || duplicate.isPromotion
            )
            shoppingListDao.updateShoppingListItem(updated)
            return Pair(true, "Producto ya existía en la lista. Se incrementó la cantidad a ${updated.quantityToBuy} $unit.")
        } else {
            shoppingListDao.insertShoppingListItem(
                ShoppingListItem(
                    name = name,
                    quantityToBuy = quantity,
                    unit = unit,
                    locationCategory = location,
                    foodCategory = foodCategory,
                    supermarket = supermarket,
                    estimatedPrice = price,
                    isPromotion = isPromo
                )
            )
            return Pair(false, "Producto añadido a la lista de compras.")
        }
    }

    suspend fun markShoppingListItemBought(item: ShoppingListItem, addToPantry: Boolean) {
        shoppingListDao.markAsBought(item.id)

        // Record in Purchase History
        purchaseHistoryDao.insertPurchaseHistory(
            PurchaseHistoryItem(
                name = item.name,
                quantity = item.quantityToBuy,
                unit = item.unit,
                price = item.estimatedPrice,
                supermarket = item.supermarket,
                locationCategory = item.locationCategory,
                foodCategory = item.foodCategory,
                wasPromotion = item.isPromotion
            )
        )

        // If requested, restock into Pantry
        if (addToPantry) {
            val existing = pantryDao.getPantryItemByName(item.name)
            if (existing != null) {
                pantryDao.updatePantryItem(
                    existing.copy(
                        quantity = existing.quantity + item.quantityToBuy,
                        price = if (item.estimatedPrice > 0) item.estimatedPrice else existing.price,
                        isPromotion = item.isPromotion,
                        supermarket = if (item.supermarket.isNotBlank()) item.supermarket else existing.supermarket
                    )
                )
            } else {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, 14) // default 2 weeks expiry
                val tip = ConservationTips.getTipForProduct(item.name, item.foodCategory, item.locationCategory)

                pantryDao.insertPantryItem(
                    PantryItem(
                        name = item.name,
                        quantity = item.quantityToBuy,
                        unit = item.unit,
                        locationCategory = item.locationCategory,
                        foodCategory = item.foodCategory,
                        expirationDateMillis = cal.timeInMillis,
                        supermarket = item.supermarket,
                        price = item.estimatedPrice,
                        isPromotion = item.isPromotion,
                        conservationTip = tip
                    )
                )
            }
        }
    }

    suspend fun deleteShoppingListItem(id: Int) {
        shoppingListDao.deleteShoppingListItemById(id)
    }

    suspend fun addPurchaseHistoryItem(item: PurchaseHistoryItem) {
        purchaseHistoryDao.insertPurchaseHistory(item)
    }

    suspend fun deletePurchaseHistoryItem(id: Int) {
        purchaseHistoryDao.deleteHistoryItemById(id)
    }

    suspend fun clearPurchaseHistory() {
        purchaseHistoryDao.clearHistory()
    }

    suspend fun analyzeImageWithGemini(bitmap: android.graphics.Bitmap): List<ScannedProduct> {
        return geminiService.analyzeImageForProducts(bitmap)
    }

    suspend fun analyzeProductLabelAndIngredients(bitmap: android.graphics.Bitmap): ProductLabelIngredientAnalysis {
        return geminiService.analyzeProductLabelAndIngredients(bitmap)
    }

    suspend fun addRecipeIngredientsToShoppingList(ingredients: List<String>): Int {
        var added = 0
        for (ing in ingredients) {
            val cleanName = ing.trim()
            if (cleanName.isNotBlank()) {
                val duplicate = shoppingListDao.findDuplicateInShoppingList(cleanName)
                if (duplicate == null) {
                    shoppingListDao.insertShoppingListItem(
                        ShoppingListItem(
                            name = cleanName,
                            quantityToBuy = 1.0,
                            unit = "ud",
                            locationCategory = "Alacena",
                            foodCategory = "Otros",
                            estimatedPrice = 1.5,
                            isMissing = true,
                            missingReason = "Receta Gemini"
                        )
                    )
                    added++
                }
            }
        }
        return added
    }

    suspend fun generateRecipesWithGemini(warningDays: Int): List<RecipeSuggestion> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, warningDays)
        val expiring = pantryDao.getExpiringItemsList(calendar.timeInMillis)
        val all = pantryDao.getAllPantryItems().firstOrNull() ?: emptyList()
        val sortedAll = all.sortedBy { it.expirationDateMillis }
        val effectiveExpiring = if (expiring.isNotEmpty()) expiring else sortedAll.take(5)

        return geminiService.generateRecipeSuggestions(effectiveExpiring, sortedAll)
    }

    suspend fun categorizeProductWithGemini(input: String): ScannedProduct {
        return geminiService.categorizeProductByBarcodeOrName(input)
    }

    suspend fun fetchProductByBarcode(barcode: String): ScannedProduct {
        val cleanBarcode = barcode.trim()
        val offResult = openFoodFactsService.fetchProductByBarcode(cleanBarcode)
        if (offResult != null) {
            return offResult
        }
        return geminiService.categorizeProductByBarcodeOrName(cleanBarcode)
    }

    suspend fun fetchOnlinePrice(productName: String, supermarket: String = "Mercadona"): ScannedProduct {
        return geminiService.fetchOnlinePriceData(productName, supermarket)
    }

    suspend fun compareSupermarketPrices(productName: String): com.example.data.local.ProductSupermarketComparison {
        val cache = geminiCache
        if (cache == null) return geminiService.compareSupermarketPrices(productName)
        val key = "compareSupermarketPrices:${productName.trim().lowercase()}"
        return cache.getOrFetch(key) { geminiService.compareSupermarketPrices(productName) }
    }

    suspend fun fetchProductPriceHistory(productName: String): com.example.data.local.ProductDetailPriceHistory {
        val cache = geminiCache
        if (cache == null) return geminiService.fetchProductPriceHistory(productName)
        val key = "fetchProductPriceHistory:${productName.trim().lowercase()}"
        return cache.getOrFetch(key) { geminiService.fetchProductPriceHistory(productName) }
    }

    suspend fun syncWithAlexa(settings: AppSettings? = null): AlexaSyncResult {
        val currentSettings = settings ?: getAppSettings()
        val effectiveSettings = if (!currentSettings.isAlexaSyncEnabled) {
            val enabled = currentSettings.copy(isAlexaSyncEnabled = true)
            saveAppSettings(enabled)
            enabled
        } else {
            currentSettings
        }
        return alexaSyncManager.performBidirectionalSync(effectiveSettings)
    }

    suspend fun performGoogleDriveBackup(context: Context, token: String): GoogleDriveBackupResult {
        val googleDriveManager = GoogleDriveBackupManager(context)
        val pantryList = pantryDao.getAllPantryItemsList()
        val shoppingList = shoppingListDao.getAllShoppingItemsList()
        val historyList = purchaseHistoryDao.getAllHistoryItemsList()
        val currentSettings = getAppSettings()

        val jsonStr = googleDriveManager.createBackupJson(pantryList, shoppingList, historyList, currentSettings)
        val result = googleDriveManager.uploadToGoogleDrive(token, jsonStr)

        if (result.success) {
            val updatedSettings = currentSettings.copy(
                lastDriveBackupTimestamp = result.backupTimestamp
            )
            saveAppSettings(updatedSettings)
        }

        return result
    }

    suspend fun restoreDataFromGoogleDrive(context: Context, token: String): Pair<Boolean, String> {
        val googleDriveManager = GoogleDriveBackupManager(context)
        val (success, jsonContent) = googleDriveManager.restoreFromGoogleDrive(token)

        if (!success || jsonContent.isNullOrBlank()) {
            return Pair(false, "No se encontró ningún archivo de respaldo 'despensa_virtual_backup.json' en Google Drive.")
        }

        try {
            val payload = googleDriveManager.parseBackupJson(jsonContent)

            // Clear existing local database
            pantryDao.deleteAllPantryItems()
            shoppingListDao.deleteAllShoppingItems()
            purchaseHistoryDao.clearHistory()

            // Restore Pantry items
            payload.pantryItems.forEach { item ->
                pantryDao.insertPantryItem(item)
            }

            // Restore Shopping List items
            payload.shoppingListItems.forEach { item ->
                shoppingListDao.insertShoppingListItem(item)
            }

            // Restore Purchase History
            payload.purchaseHistoryItems.forEach { item ->
                purchaseHistoryDao.insertPurchaseHistory(item)
            }

            // Restore Settings
            if (payload.settings != null) {
                saveAppSettings(payload.settings.copy(lastDriveBackupTimestamp = System.currentTimeMillis()))
            }

            val summary = "Restauración completada con éxito: ${payload.pantryItems.size} productos en inventario, ${payload.shoppingListItems.size} en lista de compras y ${payload.purchaseHistoryItems.size} en historial."
            return Pair(true, summary)
        } catch (e: Exception) {
            e.printStackTrace()
            return Pair(false, "Error al procesar el archivo de respaldo: ${e.localizedMessage}")
        }
    }
}
