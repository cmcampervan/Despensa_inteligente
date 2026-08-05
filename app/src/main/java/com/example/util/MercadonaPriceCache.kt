package com.example.util

import com.example.data.local.MercadonaCacheDao
import com.example.data.local.MercadonaCacheEntry
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.concurrent.TimeUnit

/**
 * Misma idea que [GeminiResponseCache] pero sobre la tabla propia de precios de Mercadona.
 * TTL por defecto más corto (2 horas) porque los precios reales de supermercado interesa
 * refrescarlos con más frecuencia que, por ejemplo, una recomendación de receta de Gemini.
 */
class MercadonaPriceCache(
    private val dao: MercadonaCacheDao,
    private val defaultTtlMillis: Long = TimeUnit.HOURS.toMillis(2),
) {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    suspend inline fun <reified T> getOrFetch(
        key: String,
        ttlMillis: Long = defaultTtlMillis,
        forceRefresh: Boolean = false,
        crossinline fetch: suspend () -> T,
    ): T {
        val adapter = moshi.adapter(T::class.java)
        val now = System.currentTimeMillis()

        if (!forceRefresh) {
            val cached = dao.get(key)
            if (cached != null && (now - cached.cachedAtMillis) < ttlMillis) {
                try {
                    val parsed = adapter.fromJson(cached.jsonPayload)
                    if (parsed != null) return parsed
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        val fresh = fetch()
        try {
            dao.put(MercadonaCacheEntry(cacheKey = key, jsonPayload = adapter.toJson(fresh)))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return fresh
    }

    suspend fun clearExpired(olderThanMillis: Long = TimeUnit.DAYS.toMillis(3)) {
        dao.clearExpired(System.currentTimeMillis() - olderThanMillis)
    }

    suspend fun clearAll() {
        dao.clearAll()
    }
}
