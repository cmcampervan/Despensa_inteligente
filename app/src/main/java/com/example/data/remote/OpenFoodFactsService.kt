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

data class OpenFoodFactsProductResponse(
    @Json(name = "status") val status: Int?,
    @Json(name = "product") val product: OpenFoodFactsProductDetails?
)

data class OpenFoodFactsProductDetails(
    @Json(name = "product_name") val productName: String?,
    @Json(name = "product_name_es") val productNameEs: String?,
    @Json(name = "brands") val brands: String?,
    @Json(name = "categories") val categories: String?,
    @Json(name = "stores") val stores: String?,
    @Json(name = "image_url") val imageUrl: String?,
    @Json(name = "ecoscore_grade") val ecoScore: String?,
    @Json(name = "nutriscore_grade") val nutriScore: String?
)

data class OpenFoodFactsSearchResponse(
    @Json(name = "count") val count: Int?,
    @Json(name = "products") val products: List<OpenFoodFactsProductDetails>?
)

interface OpenFoodFactsApi {
    @GET("api/v0/product/{barcode}.json")
    suspend fun getProductByBarcode(
        @Path("barcode") barcode: String
    ): Response<OpenFoodFactsProductResponse>

    @GET("cgi/search.pl?search_simple=1&action=process&json=1")
    suspend fun searchProducts(
        @Query("search_terms") terms: String
    ): Response<OpenFoodFactsSearchResponse>
}

object OpenFoodFactsClient {
    private const val BASE_URL = "https://es.openfoodfacts.org/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    val api: OpenFoodFactsApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenFoodFactsApi::class.java)
    }
}

class OpenFoodFactsService {
    suspend fun fetchProductByBarcode(barcode: String): ScannedProduct? = withContext(Dispatchers.IO) {
        try {
            val response = OpenFoodFactsClient.api.getProductByBarcode(barcode.trim())
            if (response.isSuccessful && response.body()?.status == 1) {
                val p = response.body()?.product
                if (p != null) {
                    val name = p.productNameEs ?: p.productName ?: "Producto $barcode"
                    val brand = p.brands?.split(",")?.firstOrNull()?.trim() ?: ""
                    val fullName = if (brand.isNotBlank() && !name.contains(brand, true)) "$name ($brand)" else name
                    val stores = p.stores?.split(",")?.firstOrNull()?.trim() ?: "Mercadona"

                    return@withContext ScannedProduct(
                        name = fullName,
                        category = if (p.categories?.contains("frigo", true) == true || p.categories?.contains("dairy", true) == true) "Nevera" else "Alacena",
                        foodCategory = determineCategoryFromOff(p.categories ?: ""),
                        estimatedPrice = 1.95, // Fallback default price if missing
                        isPromotion = false,
                        supermarket = if (stores.isBlank()) "Mercadona" else stores,
                        conservationTip = "Guardar según las instrucciones del fabricante."
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    private fun determineCategoryFromOff(categories: String): String {
        val lower = categories.lowercase()
        return when {
            lower.contains("milk") || lower.contains("leche") || lower.contains("cheese") || lower.contains("queso") || lower.contains("yogurt") -> "Lácteos"
            lower.contains("meat") || lower.contains("carne") || lower.contains("fish") || lower.contains("pescado") -> "Carnes y Pescados"
            lower.contains("fruit") || lower.contains("fruta") || lower.contains("vegetable") || lower.contains("verdura") -> "Frutas y Verduras"
            lower.contains("beverage") || lower.contains("bebida") || lower.contains("juice") || lower.contains("agua") -> "Bebidas"
            lower.contains("canned") || lower.contains("lata") || lower.contains("conserva") -> "Enlatados"
            lower.contains("snack") || lower.contains("galleta") || lower.contains("chocolate") -> "Snacks"
            lower.contains("frozen") || lower.contains("congelado") -> "Congelados"
            lower.contains("cereal") || lower.contains("pasta") || lower.contains("arroz") -> "Granos y Cereales"
            else -> "Otros"
        }
    }
}
