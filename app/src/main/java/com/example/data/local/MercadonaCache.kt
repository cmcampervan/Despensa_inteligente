package com.example.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

/**
 * Caché de precios reales de Mercadona, en tabla propia separada de "gemini_cache".
 * Se mantiene aparte porque los precios de supermercado tienen una cadencia de cambio
 * distinta a las respuestas de Gemini (recetas, categorización, etc.) y así se pueden
 * limpiar o invalidar de forma independiente sin afectar al resto de cachés de la app.
 */
@Entity(tableName = "mercadona_price_cache")
data class MercadonaCacheEntry(
    @PrimaryKey val cacheKey: String,
    val jsonPayload: String,
    val cachedAtMillis: Long = System.currentTimeMillis()
)

@Dao
interface MercadonaCacheDao {
    @Query("SELECT * FROM mercadona_price_cache WHERE cacheKey = :key LIMIT 1")
    suspend fun get(key: String): MercadonaCacheEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entry: MercadonaCacheEntry)

    @Query("DELETE FROM mercadona_price_cache WHERE cachedAtMillis < :olderThanMillis")
    suspend fun clearExpired(olderThanMillis: Long)

    @Query("DELETE FROM mercadona_price_cache")
    suspend fun clearAll()
}
