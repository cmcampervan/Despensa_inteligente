package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * Mercadona no ofrece una API pública oficial, así que este cliente está pensado para
 * apuntar a un backend/proxy propio (o a una API de terceros que replique la misma forma
 * de respuesta) que tú controles. Por eso [MercadonaApiConfig.baseUrl] es variable y no una
 * constante: cuando tengas el endpoint real, solo hay que cambiar este valor (por ejemplo,
 * leyéndolo desde BuildConfig o desde los Ajustes de la app) sin tocar el resto del código.
 *
 * Mientras no haya un endpoint configurado, todas las llamadas fallan de forma controlada
 * (devuelven null) en vez de lanzar una excepción, para que el resto de la app pueda seguir
 * funcionando con los precios estimados de Gemini como respaldo.
 */
object MercadonaApiConfig {
    // TODO: sustituir por el host real cuando esté disponible, p. ej. "https://tu-proxy-mercadona.com/"
    var baseUrl: String = "https://mercadona-api.example.invalid/"
}

data class MercadonaProductPrice(
    @Json(name = "ean") val barcode: String? = null,
    @Json(name = "display_name") val displayName: String? = null,
    @Json(name = "unit_price") val unitPrice: Double? = null,
    @Json(name = "reference_price") val referencePrice: Double? = null,
    @Json(name = "reference_format") val referenceFormat: String? = null,
    @Json(name = "is_pack") val isPack: Boolean? = null,
    @Json(name = "thumbnail") val thumbnailUrl: String? = null
)

data class MercadonaSearchResponse(
    @Json(name = "results") val results: List<MercadonaProductPrice>? = null
)

interface MercadonaApi {
    @GET("api/v1/products/{barcode}")
    suspend fun getProductByBarcode(
        @Path("barcode") barcode: String
    ): Response<MercadonaProductPrice>

    @GET("api/v1/products/search")
    suspend fun searchProducts(
        @Query("query") query: String
    ): Response<MercadonaSearchResponse>
}

private object MercadonaHttp {
    val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    // Se reconstruye si cambia MercadonaApiConfig.baseUrl (p. ej. tras configurarlo en Ajustes),
    // en vez de cachear un Retrofit apuntando a una URL ya obsoleta.
    private var cachedBaseUrl: String? = null
    private var cachedApi: MercadonaApi? = null

    val api: MercadonaApi
        get() {
            val currentBaseUrl = MercadonaApiConfig.baseUrl
            val existing = cachedApi
            if (existing != null && cachedBaseUrl == currentBaseUrl) return existing

            val fresh = Retrofit.Builder()
                .baseUrl(currentBaseUrl)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(MercadonaApi::class.java)

            cachedBaseUrl = currentBaseUrl
            cachedApi = fresh
            return fresh
        }
}

/**
 * Envoltorio defensivo sobre [MercadonaApi]: nunca propaga excepciones de red, siempre
 * devuelve null si el producto no se encuentra o si el endpoint no está configurado/disponible.
 */
class MercadonaApiClient {
    suspend fun fetchPriceByBarcode(barcode: String): MercadonaProductPrice? = withContext(Dispatchers.IO) {
        val cleanBarcode = barcode.trim()
        if (cleanBarcode.isEmpty()) return@withContext null
        try {
            val response = MercadonaHttp.api.getProductByBarcode(cleanBarcode)
            if (response.isSuccessful) response.body() else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun searchPriceByName(productName: String): MercadonaProductPrice? = withContext(Dispatchers.IO) {
        val cleanName = productName.trim()
        if (cleanName.isEmpty()) return@withContext null
        try {
            val response = MercadonaHttp.api.searchProducts(cleanName)
            if (response.isSuccessful) {
                response.body()?.results?.firstOrNull()
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
