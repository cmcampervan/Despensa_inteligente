package com.example.data.repository

import com.example.data.local.MercadonaCacheDao
import com.example.data.local.PantryDao
import com.example.data.remote.MercadonaApiClient
import com.example.data.remote.MercadonaProductPrice
import com.example.util.MercadonaPriceCache

data class MercadonaCatalogRefreshResult(
    val totalChecked: Int,
    val updatedCount: Int,
    val notFoundCount: Int,
    val message: String
)

/**
 * Encapsula la consulta de precios reales de Mercadona (por nombre o código de barras),
 * con caché propia para no golpear el endpoint en cada recomposición de la UI, y la
 * operación de "refrescar catálogo" que recorre el inventario actual y actualiza los
 * precios guardados en cada [com.example.data.local.PantryItem].
 */
class MercadonaCatalogRepository(
    private val pantryDao: PantryDao,
    mercadonaCacheDao: MercadonaCacheDao? = null,
    private val apiClient: MercadonaApiClient = MercadonaApiClient()
) {
    private val cache: MercadonaPriceCache? = mercadonaCacheDao?.let { MercadonaPriceCache(it) }

    suspend fun getPrice(productName: String, barcode: String? = null, forceRefresh: Boolean = false): MercadonaProductPrice? {
        val key = if (!barcode.isNullOrBlank()) {
            "mercadona:barcode:${barcode.trim()}"
        } else {
            "mercadona:name:${productName.trim().lowercase()}"
        }

        suspend fun fetch(): MercadonaProductPrice? {
            return if (!barcode.isNullOrBlank()) {
                apiClient.fetchPriceByBarcode(barcode) ?: apiClient.searchPriceByName(productName)
            } else {
                apiClient.searchPriceByName(productName)
            }
        }

        val activeCache = cache ?: return fetch()
        return activeCache.getOrFetch(key, forceRefresh = forceRefresh) { fetch() }
    }

    /**
     * Recorre todo el inventario actual y actualiza el precio de cada producto con el
     * precio real de Mercadona más reciente (saltándose la caché para forzar datos frescos).
     * Los productos sin coincidencia se dejan como están, con su precio previo.
     */
    suspend fun refreshFullCatalog(): MercadonaCatalogRefreshResult {
        val items = pantryDao.getAllPantryItemsList()
        var updated = 0
        var notFound = 0

        for (item in items) {
            val price = getPrice(item.name, item.barcode.ifBlank { null }, forceRefresh = true)
            val realPrice = price?.unitPrice ?: price?.referencePrice
            if (realPrice != null && realPrice > 0.0) {
                pantryDao.updatePantryItem(item.copy(price = realPrice, updatedAt = System.currentTimeMillis()))
                updated++
            } else {
                notFound++
            }
        }

        val message = if (items.isEmpty()) {
            "No hay productos en el inventario para actualizar."
        } else {
            "Precios actualizados: $updated de ${items.size} productos. " +
                if (notFound > 0) "$notFound sin coincidencia en Mercadona." else "Todos encontrados."
        }

        return MercadonaCatalogRefreshResult(
            totalChecked = items.size,
            updatedCount = updated,
            notFoundCount = notFound,
            message = message
        )
    }
}
