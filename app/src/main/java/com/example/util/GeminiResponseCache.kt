package com.example.util

import com.example.data.local.GeminiCacheDao
import com.example.data.local.GeminiCacheEntry
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.concurrent.TimeUnit

/**
 * Envuelve cualquier llamada suspend cara (normalmente a Gemini AI) con una caché persistida
 * en Room. Si hay un resultado reciente para la misma clave, lo devuelve sin llamar a la red,
 * ahorrando cuota de la API. Si no hay resultado o ha caducado, ejecuta [fetch], guarda el
 * resultado y lo devuelve.
 */
class GeminiResponseCache(
    @PublishedApi internal val dao: GeminiCacheDao,
    @PublishedApi internal val defaultTtlMillis: Long = TimeUnit.HOURS.toMillis(12),
) {
    @PublishedApi internal val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    suspend inline fun <reified T> getOrFetch(
        key: String,
        ttlMillis: Long = defaultTtlMillis,
        crossinline fetch: suspend () -> T,
    ): T {
        val adapter = moshi.adapter(T::class.java)
        val cached = dao.get(key)
        val now = System.currentTimeMillis()

        if (cached != null && (now - cached.cachedAtMillis) < ttlMillis) {
            try {
                val parsed = adapter.fromJson(cached.jsonPayload)
                if (parsed != null) return parsed
            } catch (e: Exception) {
                // Si el esquema cambió y el JSON cacheado no se puede parsear, se ignora
                // la caché y se vuelve a pedir el dato fresco.
                e.printStackTrace()
            }
        }

        val fresh = fetch()
        try {
            dao.put(GeminiCacheEntry(cacheKey = key, jsonPayload = adapter.toJson(fresh)))
        } catch (e: Exception) {
            e.printStackTrace() // Si falla el guardado en caché, seguimos igualmente con el dato fresco.
        }
        return fresh
    }

    /** Limpia entradas de caché con más de [olderThanMillis] de antigüedad (llamar periódicamente). */
    suspend fun clearExpired(olderThanMillis: Long = TimeUnit.DAYS.toMillis(7)) {
        dao.clearExpired(System.currentTimeMillis() - olderThanMillis)
    }
}
