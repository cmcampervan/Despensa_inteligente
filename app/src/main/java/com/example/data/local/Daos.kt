package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PantryDao {
    @Query("SELECT * FROM pantry_items ORDER BY expirationDateMillis ASC")
    fun getAllPantryItems(): Flow<List<PantryItem>>

    @Query("SELECT * FROM pantry_items WHERE locationCategory = :location ORDER BY expirationDateMillis ASC")
    fun getPantryItemsByLocation(location: String): Flow<List<PantryItem>>

    @Query("SELECT * FROM pantry_items WHERE id = :id")
    suspend fun getPantryItemById(id: Int): PantryItem?

    @Query("SELECT * FROM pantry_items WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getPantryItemByName(name: String): PantryItem?

    @Query("SELECT * FROM pantry_items WHERE quantity <= minThreshold")
    suspend fun getLowStockItems(): List<PantryItem>

    @Query("SELECT * FROM pantry_items WHERE expirationDateMillis <= :maxTimeMillis ORDER BY expirationDateMillis ASC")
    fun getExpiringItems(maxTimeMillis: Long): Flow<List<PantryItem>>

    @Query("SELECT * FROM pantry_items WHERE expirationDateMillis <= :maxTimeMillis ORDER BY expirationDateMillis ASC")
    suspend fun getExpiringItemsList(maxTimeMillis: Long): List<PantryItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPantryItem(item: PantryItem): Long

    @Update
    suspend fun updatePantryItem(item: PantryItem)

    @Delete
    suspend fun deletePantryItem(item: PantryItem)

    @Query("SELECT * FROM pantry_items")
    suspend fun getAllPantryItemsList(): List<PantryItem>

    @Query("DELETE FROM pantry_items")
    suspend fun deleteAllPantryItems()

    @Query("DELETE FROM pantry_items WHERE id = :id")
    suspend fun deletePantryItemById(id: Int)
}

@Dao
interface ShoppingListDao {
    @Query("SELECT * FROM shopping_list_items WHERE isBought = 0 ORDER BY addedDateMillis DESC")
    fun getActiveShoppingList(): Flow<List<ShoppingListItem>>

    @Query("SELECT * FROM shopping_list_items WHERE LOWER(name) = LOWER(:name) AND isBought = 0 LIMIT 1")
    suspend fun findDuplicateInShoppingList(name: String): ShoppingListItem?

    @Query("SELECT * FROM shopping_list_items")
    suspend fun getAllShoppingItemsList(): List<ShoppingListItem>

    @Query("SELECT * FROM shopping_list_items WHERE isBought = 0")
    suspend fun getAllActiveShoppingListOnce(): List<ShoppingListItem>

    @Query("DELETE FROM shopping_list_items")
    suspend fun deleteAllShoppingItems()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingListItem(item: ShoppingListItem): Long

    @Update
    suspend fun updateShoppingListItem(item: ShoppingListItem)

    @Query("UPDATE shopping_list_items SET isBought = 1 WHERE id = :id")
    suspend fun markAsBought(id: Int)

    @Delete
    suspend fun deleteShoppingListItem(item: ShoppingListItem)

    @Query("DELETE FROM shopping_list_items WHERE id = :id")
    suspend fun deleteShoppingListItemById(id: Int)

    @Query("SELECT * FROM shopping_list_items WHERE pantryItemId = :pantryItemId AND isBought = 0 LIMIT 1")
    suspend fun findByPantryItemId(pantryItemId: Int): ShoppingListItem?

    @Query("UPDATE shopping_list_items SET isMissing = :isMissing, missingReason = :reason WHERE id = :id")
    suspend fun updateMissingStatus(id: Int, isMissing: Boolean, reason: String)

    @Query("DELETE FROM shopping_list_items WHERE isBought = 1")
    suspend fun clearBoughtItems()
}

@Dao
interface PurchaseHistoryDao {
    @Query("SELECT * FROM purchase_history_items ORDER BY purchaseDateMillis DESC")
    fun getAllPurchaseHistory(): Flow<List<PurchaseHistoryItem>>

    @Query("SELECT * FROM purchase_history_items")
    suspend fun getAllHistoryItemsList(): List<PurchaseHistoryItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseHistory(item: PurchaseHistoryItem)

    @Query("DELETE FROM purchase_history_items WHERE id = :id")
    suspend fun deleteHistoryItemById(id: Int)

    @Query("DELETE FROM purchase_history_items")
    suspend fun clearHistory()
}

@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun getSettingsFlow(): Flow<AppSettings?>

    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun getSettingsOnce(): AppSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: AppSettings)
}

@Dao
interface ProductoDao {
    @Query("SELECT * FROM productos ORDER BY fechaCaducidad ASC")
    fun getAllProductos(): Flow<List<Producto>>

    @Query("SELECT * FROM productos WHERE id = :id")
    suspend fun getProductoById(id: Int): Producto?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducto(producto: Producto): Long

    @Update
    suspend fun updateProducto(producto: Producto)

    @Delete
    suspend fun deleteProducto(producto: Producto)

    @Query("DELETE FROM productos WHERE id = :id")
    suspend fun deleteProductoById(id: Int)
}

