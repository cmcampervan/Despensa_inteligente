package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pantry_items")
data class PantryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val quantity: Double,
    val unit: String = "ud", // ud, kg, g, L, ml, pack
    val locationCategory: String, // ALACENA, NEVERA
    val foodCategory: String = "Otros", // Lácteos, Carnes y Pescados, Frutas y Verduras, Granos y Cereales, Bebidas, Enlatados, Snacks, Congelados, Otros
    val expirationDateMillis: Long,
    val supermarket: String = "General",
    val minThreshold: Double = 1.0,
    val conservationTip: String = "",
    val barcode: String = "",
    val price: Double = 0.0,
    val isPromotion: Boolean = false,
    val imageUri: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "shopping_list_items")
data class ShoppingListItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val pantryItemId: Int? = null,
    val name: String,
    val quantityToBuy: Double = 1.0,
    val unit: String = "ud",
    val locationCategory: String = "ALACENA",
    val foodCategory: String = "Otros",
    val estimatedPrice: Double = 0.0,
    val isPromotion: Boolean = false,
    val supermarket: String = "General",
    val isBought: Boolean = false,
    val addedDateMillis: Long = System.currentTimeMillis(),
    val isMissing: Boolean = false,
    val missingReason: String = ""
)

@Entity(tableName = "purchase_history_items")
data class PurchaseHistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val quantity: Double,
    val unit: String = "ud",
    val price: Double = 0.0,
    val supermarket: String = "General",
    val locationCategory: String = "ALACENA",
    val foodCategory: String = "Otros",
    val purchaseDateMillis: Long = System.currentTimeMillis(),
    val wasPromotion: Boolean = false
)

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    val expirationWarningDays: Int = 3,
    val language: String = "es", // es, en
    val isDarkMode: Boolean = false,
    val alexaApiKey: String = "",
    val alexaUserId: String = "",
    val isAlexaSyncEnabled: Boolean = false,
    val autoAddToShoppingList: Boolean = true,
    val isGoogleDriveAutoBackupEnabled: Boolean = false,
    val lastDriveBackupTimestamp: Long = 0L,
    val driveAccountEmail: String = "",
    val monthlyBudget: Double = 0.0
)
