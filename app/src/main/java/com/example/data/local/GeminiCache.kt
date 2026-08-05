package com.example.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

/**
 * Caché genérica de respuestas de Gemini AI serializadas en JSON, indexadas por una clave
 * (normalmente "nombreDeLaFuncion:productoNormalizado"). Evita repetir llamadas idénticas
 * a la API en un periodo corto de tiempo, lo cual era la causa de agotar la cuota diaria
 * al reabrir varias veces la comparativa de precios o el historial de un mismo producto.
 */
@Entity(tableName = "gemini_cache")
data class GeminiCacheEntry(
    @PrimaryKey val cacheKey: String,
    val jsonPayload: String,
    val cachedAtMillis: Long = System.currentTimeMillis()
)

@Dao
interface GeminiCacheDao {
    @Query("SELECT * FROM gemini_cache WHERE cacheKey = :key LIMIT 1")
    suspend fun get(key: String): GeminiCacheEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entry: GeminiCacheEntry)

    @Query("DELETE FROM gemini_cache WHERE cachedAtMillis < :olderThanMillis")
    suspend fun clearExpired(olderThanMillis: Long)

    @Query("DELETE FROM gemini_cache")
    suspend fun clearAll()
}
