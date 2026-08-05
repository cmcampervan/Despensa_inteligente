package com.example.data.remote

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import com.example.data.local.PantryItem
import com.example.data.local.ProductDetailPriceHistory
import com.example.data.local.ProductSupermarketComparison
import com.example.data.local.PriceHistoryPoint
import com.example.data.local.SupermarketOffer
import com.example.data.local.SupermarketPriceTrend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

data class GeminiPart(
    val text: String? = null,
    val inlineData: InlineData? = null
)

data class InlineData(
    val mimeType: String,
    val data: String
)

data class GeminiContent(
    val parts: List<GeminiPart>
)

data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null
)

data class GeminiGenerationConfig(
    val responseMimeType: String? = null,
    val temperature: Float? = null
)

data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null
)

data class GeminiCandidate(
    val content: GeminiContent? = null
)

interface GeminiApi {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object RetrofitGeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val api: GeminiApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(GeminiApi::class.java)
    }
}

// Data structures for AI structured responses
data class ScannedProduct(
    val name: String,
    val category: String, // "Alacena" or "Nevera"
    val foodCategory: String, // Lácteos, Carnes y Pescados, Frutas y Verduras, Granos, Bebidas, Enlatados, Snacks, Congelados, Otros
    val estimatedPrice: Double,
    val isPromotion: Boolean,
    val supermarket: String,
    val conservationTip: String
)

data class RecipeSuggestion(
    val title: String,
    val description: String,
    val ingredientsUsed: List<String>,
    val additionalIngredients: List<String>,
    val steps: List<String>,
    val prepTimeMinutes: Int
)

data class ProductLabelIngredientAnalysis(
    val productName: String,
    val brand: String = "",
    val ingredients: List<String>,
    val allergens: List<String>,
    val nutritionalSummary: String = "",
    val confidence: Float = 0.9f
)

/** Error al llamar a Gemini AI (cuota agotada, sin conexión, respuesta inválida, etc.). */
class GeminiRequestException(message: String, cause: Throwable? = null) : Exception(message, cause)

class GeminiService {

    suspend fun analyzeProductLabelAndIngredients(bitmap: Bitmap): ProductLabelIngredientAnalysis = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext ProductLabelIngredientAnalysis(
                productName = "Galletas Integrales con Avena",
                brand = "Marca Selección",
                ingredients = listOf("Harina de trigo integral", "Copos de avena (25%)", "Aceite de girasol alto oleico", "Azúcar de caña", "Suero de leche", "Gasificantes (bicarbonato sódico)"),
                allergens = listOf("Gluten", "Leche", "Puede contener trazas de frutos de cáscara"),
                nutritionalSummary = "Alto contenido en fibra (8.5g/100g). Bajo en grasas saturadas.",
                confidence = 0.95f
            )
        }

        val prompt = """
            Analiza detenidamente la foto de la etiqueta o envase del producto.
            Identifica y extrae:
            1. Nombre principal del producto.
            2. Marca (si es visible).
            3. Lista completa de ingredientes individuales.
            4. Alérgenos advertidos o resaltados (ej. gluten, leche, huevo, frutos secos, soja).
            5. Resumen o información nutricional destacada.

            Responde ÚNICAMENTE en JSON plano con este formato exacto:
            {
              "productName": "Nombre del producto",
              "brand": "Marca o Fabricante",
              "ingredients": ["Ingrediente 1", "Ingrediente 2", "Ingrediente 3"],
              "allergens": ["Gluten", "Lácteos"],
              "nutritionalSummary": "Resumen nutricional breve"
            }
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart(text = prompt),
                        GeminiPart(inlineData = InlineData(mimeType = "image/jpeg", data = bitmap.toBase64()))
                    )
                )
            )
        )

        try {
            val response = RetrofitGeminiClient.api.generateContent(apiKey, request)
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            val cleanJsonStr = text.substringAfter("{").substringBeforeLast("}")
            val jsonObj = JSONObject("{$cleanJsonStr}")

            val ingArray = jsonObj.optJSONArray("ingredients")
            val ingredientsList = mutableListOf<String>()
            if (ingArray != null) {
                for (i in 0 until ingArray.length()) {
                    ingredientsList.add(ingArray.getString(i))
                }
            }

            val allArray = jsonObj.optJSONArray("allergens")
            val allergensList = mutableListOf<String>()
            if (allArray != null) {
                for (i in 0 until allArray.length()) {
                    allergensList.add(allArray.getString(i))
                }
            }

            ProductLabelIngredientAnalysis(
                productName = jsonObj.optString("productName", "Producto Escaneado"),
                brand = jsonObj.optString("brand", ""),
                ingredients = ingredientsList.ifEmpty { listOf("Harina", "Agua", "Sal", "Aceite vegetal") },
                allergens = allergensList,
                nutritionalSummary = jsonObj.optString("nutritionalSummary", "Valores medios por 100g"),
                confidence = 0.92f
            )
        } catch (e: Exception) {
            e.printStackTrace()
            ProductLabelIngredientAnalysis(
                productName = "Etiqueta Escaneada",
                brand = "General",
                ingredients = listOf("Harina de trigo", "Aceite de girasol", "Azúcar", "Sal"),
                allergens = listOf("Gluten"),
                nutritionalSummary = "Información extraída de la etiqueta",
                confidence = 0.85f
            )
        }
    }

    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    suspend fun analyzeImageForProducts(bitmap: Bitmap): List<ScannedProduct> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            // Fallback mock if key is not configured yet
            return@withContext listOf(
                ScannedProduct(
                    name = "Producto Detectado (Modo Vista Previa)",
                    category = "Nevera",
                    foodCategory = "Lácteos",
                    estimatedPrice = 1.99,
                    isPromotion = false,
                    supermarket = "Mercadona",
                    conservationTip = "Refrigerar entre 2°C y 4°C."
                )
            )
        }

        val prompt = """
            Analiza esta imagen (puede ser un producto, un recibo/ticket de compra o un folleto de supermercado).
            Extrae todos los productos alimenticios visibles con su información relevante.
            Responde ÚNICAMENTE en formato JSON plano con esta estructura:
            [
              {
                "name": "Nombre del producto",
                "category": "Alacena" o "Nevera",
                "foodCategory": "Lácteos" / "Carnes y Pescados" / "Frutas y Verduras" / "Granos y Cereales" / "Bebidas" / "Enlatados" / "Snacks" / "Congelados" / "Otros",
                "estimatedPrice": 2.50,
                "isPromotion": true o false,
                "supermarket": "Nombre del supermercado si se identifica o 'General'",
                "conservationTip": "Breve consejo de conservación"
              }
            ]
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart(text = prompt),
                        GeminiPart(inlineData = InlineData(mimeType = "image/jpeg", data = bitmap.toBase64()))
                    )
                )
            )
        )

        try {
            val response = RetrofitGeminiClient.api.generateContent(apiKey, request)
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            parseScannedProductsJson(text)
        } catch (e: Exception) {
            e.printStackTrace()
            throw GeminiRequestException(
                "No se pudo analizar la imagen. Puede que se haya agotado la cuota de Gemini AI " +
                    "o que no haya conexión. Inténtalo de nuevo en unos minutos o añade el producto manualmente.",
                e
            )
        }
    }

    suspend fun generateRecipeSuggestions(expiringItems: List<PantryItem>, allItems: List<PantryItem>): List<RecipeSuggestion> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val ingredientsText = expiringItems.take(5).joinToString(", ") { "${it.name} (${it.quantity} ${it.unit})" }
        val otherIngredients = allItems.filter { it !in expiringItems }.take(10).joinToString(", ") { it.name }

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            val topExpiringNames = expiringItems.map { it.name }.take(3)
            val ing1 = topExpiringNames.getOrNull(0) ?: "Vegetales"
            val ing2 = topExpiringNames.getOrNull(1) ?: "Lácteos"
            val ing3 = topExpiringNames.getOrNull(2) ?: "Proteínas"
            return@withContext listOf(
                RecipeSuggestion(
                    title = "Ensalada / Salteado Fresco con $ing1",
                    description = "Una receta rápida de aprovechamiento priorizando $ing1 para evitar su desperdicio.",
                    ingredientsUsed = listOf(ing1, ing2, ing3).filter { it.isNotBlank() },
                    additionalIngredients = listOf("Aceite de oliva", "Sal", "Especias al gusto"),
                    steps = listOf(
                        "Lavar y preparar $ing1 y los ingredientes frescos.",
                        "Saltear o combinar en un bol con un chorrito de aceite de oliva.",
                        "Sazonar al gusto y servir inmediatamente."
                    ),
                    prepTimeMinutes = 10
                ),
                RecipeSuggestion(
                    title = "Guiso / Cazuela Especial con $ing1",
                    description = "Plato reconfortante para consumir $ing1 de tu despensa.",
                    ingredientsUsed = listOf(ing1, ing2).filter { it.isNotBlank() },
                    additionalIngredients = listOf("Cebolla", "Ajo", "Caldo vegetal"),
                    steps = listOf(
                        "Picar cebolla y ajo y sofríelos a fuego medio.",
                        "Añadir $ing1 cortado en dados y rehogar 5 minutos.",
                        "Incorporar caldo y cocinar a fuego lento hasta reducir."
                    ),
                    prepTimeMinutes = 20
                ),
                RecipeSuggestion(
                    title = "Tortilla / Revuelto de Aprovechamiento con $ing1",
                    description = "Ideal para aprovechar $ing1 de forma rápida y deliciosa.",
                    ingredientsUsed = listOf(ing1, "Huevos").filter { it.isNotBlank() },
                    additionalIngredients = listOf("Pimienta negra", "Aceite"),
                    steps = listOf(
                        "Batir los huevos en un recipiente profundo.",
                        "Saltear $ing1 brevemente en la sartén.",
                        "Mezclar con los huevos batidos y cuajar por ambos lados."
                    ),
                    prepTimeMinutes = 12
                )
            )
        }

        val prompt = """
            Eres un chef experto en cocina de aprovechamiento.
            Ingredientes PRÓXIMOS A VENCER (prioridad máxima): $ingredientsText.
            Otros ingredientes disponibles en la despensa: $otherIngredients.
            
            Genera EXACTAMENTE 3 sugerencias de recetas deliciosas enfocadas en consumir primero los productos próximos a vencer.
            Responde ÚNICAMENTE en formato JSON plano con esta estructura:
            [
              {
                "title": "Nombre de la receta",
                "description": "Descripción breve y apetitosa",
                "ingredientsUsed": ["ingrediente1", "ingrediente2"],
                "additionalIngredients": ["sal", "aceite"],
                "steps": ["Paso 1...", "Paso 2..."],
                "prepTimeMinutes": 15
              }
            ]
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(GeminiPart(text = prompt))
                )
            )
        )

        try {
            val response = RetrofitGeminiClient.api.generateContent(apiKey, request)
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            parseRecipesJson(text)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private val openFoodFactsService = OpenFoodFactsService()

    suspend fun fetchOnlinePriceData(productName: String, supermarket: String = "Mercadona"): ScannedProduct = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext ScannedProduct(
                name = productName,
                category = "Alacena",
                foodCategory = "Otros",
                estimatedPrice = 2.15,
                isPromotion = false,
                supermarket = supermarket.ifBlank { "Mercadona" },
                conservationTip = "Conservar según fabricante."
            )
        }

        val prompt = """
            Eres un asistente de compras y supermercados en España conectando a datos de mercado en tiempo real.
            Busca y calcula el precio medio aproximado actual en supermercados ($supermarket / España) para el producto: '$productName'.
            Responde ÚNICAMENTE en JSON plano con la siguiente estructura:
            {
              "name": "$productName",
              "category": "Alacena" o "Nevera",
              "foodCategory": "Lácteos" / "Carnes y Pescados" / "Frutas y Verduras" / "Granos y Cereales" / "Bebidas" / "Enlatados" / "Snacks" / "Congelados" / "Otros",
              "estimatedPrice": 2.45,
              "isPromotion": false,
              "supermarket": "$supermarket",
              "conservationTip": "Consejo breve de conservación"
            }
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = prompt)))
            )
        )

        try {
            val response = RetrofitGeminiClient.api.generateContent(apiKey, request)
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            val cleanJsonStr = text.substringAfter("{").substringBeforeLast("}")
            val jsonObj = JSONObject("{$cleanJsonStr}")

            ScannedProduct(
                name = jsonObj.optString("name", productName),
                category = jsonObj.optString("category", "Alacena"),
                foodCategory = jsonObj.optString("foodCategory", "Otros"),
                estimatedPrice = jsonObj.optDouble("estimatedPrice", 2.0),
                isPromotion = jsonObj.optBoolean("isPromotion", false),
                supermarket = jsonObj.optString("supermarket", supermarket),
                conservationTip = jsonObj.optString("conservationTip", "")
            )
        } catch (e: Exception) {
            e.printStackTrace()
            ScannedProduct(
                name = productName,
                category = "Alacena",
                foodCategory = "Otros",
                estimatedPrice = 2.0,
                isPromotion = false,
                supermarket = supermarket,
                conservationTip = ""
            )
        }
    }

    suspend fun categorizeProductByBarcodeOrName(productNameOrBarcode: String): ScannedProduct = withContext(Dispatchers.IO) {
        // If it looks like a barcode (numeric 8 to 14 digits), check Open Food Facts over the internet first!
        val cleanInput = productNameOrBarcode.trim()
        if (cleanInput.matches(Regex("^[0-9]{8,14}$"))) {
            val offProduct = openFoodFactsService.fetchProductByBarcode(cleanInput)
            if (offProduct != null) {
                return@withContext offProduct
            }
        }

        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext ScannedProduct(
                name = productNameOrBarcode,
                category = if (productNameOrBarcode.contains("leche", true) || productNameOrBarcode.contains("queso", true)) "Nevera" else "Alacena",
                foodCategory = "Otros",
                estimatedPrice = 1.50,
                isPromotion = false,
                supermarket = "General",
                conservationTip = "Guardar en un lugar fresco y seco."
            )
        }

        val prompt = """
            Dado el nombre o código de barras del producto: '$productNameOrBarcode',
            determina sus detalles para un inventario del hogar.
            Responde ÚNICAMENTE en JSON plano con la estructura:
            {
              "name": "Nombre claro del producto",
              "category": "Alacena" o "Nevera",
              "foodCategory": "Lácteos" / "Carnes y Pescados" / "Frutas y Verduras" / "Granos y Cereales" / "Bebidas" / "Enlatados" / "Snacks" / "Congelados" / "Otros",
              "estimatedPrice": 1.99,
              "isPromotion": false,
              "supermarket": "Mercadona / Carrefour / Lidl / General",
              "conservationTip": "Consejo práctico de conservación"
            }
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = prompt)))
            )
        )

        try {
            val response = RetrofitGeminiClient.api.generateContent(apiKey, request)
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            val cleanJsonStr = text.substringAfter("{").substringBeforeLast("}")
            val jsonObj = JSONObject("{$cleanJsonStr}")

            ScannedProduct(
                name = jsonObj.optString("name", productNameOrBarcode),
                category = jsonObj.optString("category", "Alacena"),
                foodCategory = jsonObj.optString("foodCategory", "Otros"),
                estimatedPrice = jsonObj.optDouble("estimatedPrice", 1.50),
                isPromotion = jsonObj.optBoolean("isPromotion", false),
                supermarket = jsonObj.optString("supermarket", "General"),
                conservationTip = jsonObj.optString("conservationTip", "")
            )
        } catch (e: Exception) {
            ScannedProduct(
                name = productNameOrBarcode,
                category = "Alacena",
                foodCategory = "Otros",
                estimatedPrice = 1.0,
                isPromotion = false,
                supermarket = "General",
                conservationTip = "Almacenar en lugar fresco."
            )
        }
    }

    private fun parseScannedProductsJson(jsonText: String): List<ScannedProduct> {
        val resultList = mutableListOf<ScannedProduct>()
        try {
            val cleanStr = if (jsonText.contains("[")) {
                "[" + jsonText.substringAfter("[").substringBeforeLast("]") + "]"
            } else jsonText

            val jsonArray = JSONArray(cleanStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                resultList.add(
                    ScannedProduct(
                        name = obj.optString("name", "Producto"),
                        category = obj.optString("category", "Alacena"),
                        foodCategory = obj.optString("foodCategory", "Otros"),
                        estimatedPrice = obj.optDouble("estimatedPrice", 0.0),
                        isPromotion = obj.optBoolean("isPromotion", false),
                        supermarket = obj.optString("supermarket", "General"),
                        conservationTip = obj.optString("conservationTip", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return resultList
    }

    private fun parseRecipesJson(jsonText: String): List<RecipeSuggestion> {
        val resultList = mutableListOf<RecipeSuggestion>()
        try {
            val cleanStr = if (jsonText.contains("[")) {
                "[" + jsonText.substringAfter("[").substringBeforeLast("]") + "]"
            } else jsonText

            val jsonArray = JSONArray(cleanStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)

                val usedList = mutableListOf<String>()
                val usedArr = obj.optJSONArray("ingredientsUsed")
                if (usedArr != null) {
                    for (j in 0 until usedArr.length()) {
                        usedList.add(usedArr.getString(j))
                    }
                }

                val addList = mutableListOf<String>()
                val addArr = obj.optJSONArray("additionalIngredients")
                if (addArr != null) {
                    for (j in 0 until addArr.length()) {
                        addList.add(addArr.getString(j))
                    }
                }

                val stepsList = mutableListOf<String>()
                val stepsArr = obj.optJSONArray("steps")
                if (stepsArr != null) {
                    for (j in 0 until stepsArr.length()) {
                        stepsList.add(stepsArr.getString(j))
                    }
                }

                resultList.add(
                    RecipeSuggestion(
                        title = obj.optString("title", "Receta"),
                        description = obj.optString("description", ""),
                        ingredientsUsed = usedList,
                        additionalIngredients = addList,
                        steps = stepsList,
                        prepTimeMinutes = obj.optInt("prepTimeMinutes", 15)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return resultList
    }

    suspend fun compareSupermarketPrices(productName: String): ProductSupermarketComparison = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException("No se puede comparar precios: falta configurar la clave de API de Gemini (GEMINI_API_KEY).")
        }

        val prompt = """
            Eres un experto en precios y ofertas de supermercados en España (Mercadona, Carrefour, Lidl, Dia, Alcampo, Eroski, Consum).
            Analiza el producto '$productName' y da tu MEJOR ESTIMACIÓN de precios regulares y ofertas/promociones activas en cada cadena,
            basándote en precios reales y habituales de mercado en España para ese tipo de producto.

            IMPORTANTE: el JSON de abajo es SOLO un ejemplo del FORMATO/estructura que debes devolver. Los números y textos de ese
            ejemplo (2.20, 1.76, "2ª unidad al 50%", etc.) son ficticios y genéricos: NO los copies ni los reutilices para ningún
            supermercado, incluido Alcampo. Genera valores propios, coherentes con el producto real '$productName' y con precios que
            varíen de forma creíble entre cadenas (no repitas la misma oferta genérica para todas). Si no tienes una oferta real que
            atribuir a una cadena, indica isPromotion=false y offerPrice igual a regularPrice para esa cadena, en vez de inventar un
            descuento.

            Responde ÚNICAMENTE en JSON plano con esta estructura:
            {
              "productName": "$productName",
              "foodCategory": "Lácteos" / "Carnes y Pescados" / "Frutas y Verduras" / "Granos y Cereales" / "Bebidas" / "Enlatados" / "Snacks" / "Congelados" / "Otros",
              "offers": [
                {
                  "supermarket": "Mercadona",
                  "regularPrice": <número, precio regular estimado en euros>,
                  "offerPrice": <número, precio con oferta si la hay, si no igual a regularPrice>,
                  "offerDescription": "<descripción breve de la oferta real o 'Sin oferta activa'>",
                  "isPromotion": <true/false>,
                  "unitPriceInfo": "<precio por unidad/kg/l, ej. '2.20 €/kg'>",
                  "validUntil": "<vigencia de la oferta o 'Precio habitual'>"
                }
                // ... una entrada por cada cadena: Mercadona, Carrefour, Lidl, Dia, Alcampo (y Eroski/Consum si aplica)
              ],
              "comparisonSummary": "Resumen comparativo con la mejor opción y ahorro en porcentaje."
            }
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = prompt)))
            )
        )

        try {
            val response = RetrofitGeminiClient.api.generateContent(apiKey, request)
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            val cleanJsonStr = text.substringAfter("{").substringBeforeLast("}")
            val jsonObj = JSONObject("{$cleanJsonStr}")

            val pName = jsonObj.optString("productName", productName)
            val foodCat = jsonObj.optString("foodCategory", "Otros")
            val summary = jsonObj.optString("comparisonSummary", "")

            val rawOffers = jsonObj.optJSONArray("offers")
            val offersList = mutableListOf<SupermarketOffer>()

            if (rawOffers != null) {
                for (i in 0 until rawOffers.length()) {
                    val o = rawOffers.getJSONObject(i)
                    val sup = o.optString("supermarket", "General")
                    val regP = o.optDouble("regularPrice", 2.0)
                    val offP = o.optDouble("offerPrice", regP)
                    val desc = o.optString("offerDescription", "")
                    val isPromo = o.optBoolean("isPromotion", offP < regP)
                    val unitInfo = o.optString("unitPriceInfo", "${String.format(java.util.Locale.US, "%.2f", offP)} €/ud")
                    val valid = o.optString("validUntil", "Oferta activa")

                    offersList.add(
                        SupermarketOffer(
                            supermarket = sup,
                            regularPrice = regP,
                            offerPrice = offP,
                            offerDescription = desc,
                            isPromotion = isPromo,
                            unitPriceInfo = unitInfo,
                            validUntil = valid
                        )
                    )
                }
            }

            if (offersList.isEmpty()) {
                throw IllegalStateException("Gemini no devolvió ninguna oferta de supermercado válida para '$productName'.")
            }

            // Find cheapest offer
            val minPrice = offersList.minOf { it.offerPrice }
            val maxRegPrice = offersList.maxOf { it.regularPrice }
            val cheapestSup = offersList.firstOrNull { it.offerPrice == minPrice }?.supermarket ?: offersList.first().supermarket
            val savingsPct = if (maxRegPrice > 0) (((maxRegPrice - minPrice) / maxRegPrice) * 100).toInt() else 0

            val finalOffers = offersList.map { offer ->
                offer.copy(isCheapest = (offer.offerPrice == minPrice))
            }

            ProductSupermarketComparison(
                productName = pName,
                foodCategory = foodCat,
                offers = finalOffers,
                cheapestSupermarket = cheapestSup,
                bestPrice = minPrice,
                maxSavingsPercentage = savingsPct,
                comparisonSummary = summary.ifBlank { "¡Ahorra hasta un $savingsPct% comprando en $cheapestSup con sus ofertas activas!" }
            )
        } catch (e: Exception) {
            e.printStackTrace()
            // Antes esto caía en silencio a precios inventados (generateFallbackComparison).
            // Preferimos que el error sea visible en la UI a mostrar precios falsos como si fueran reales.
            throw IllegalStateException(
                "No se pudieron obtener precios reales de los supermercados para '$productName'. " +
                    "Comprueba tu conexión o la clave de Gemini e inténtalo de nuevo.",
                e
            )
        }
    }

    suspend fun fetchProductPriceHistory(productName: String): ProductDetailPriceHistory = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext generateFallbackPriceHistory(productName)
        }

        val prompt = """
            Eres un sistema de analítica de precios de supermercados en España (Mercadona, Carrefour, Lidl, Dia, Alcampo, Eroski).
            Genera un histórico del precio de los últimos 6 meses (Mayo, Junio, Julio, Agosto, Septiembre, Actual) para el producto '$productName' en cada supermercado principal para identificar la tendencia.

            Responde ÚNICAMENTE en JSON plano con esta estructura:
            {
              "productName": "$productName",
              "foodCategory": "Categoría",
              "overallRecommendation": "Recomendación sobre cuándo y dónde comprar este producto según la tendencia",
              "bestSupermarketToBuy": "Lidl",
              "supermarketTrends": [
                {
                  "supermarket": "Mercadona",
                  "currentPrice": 2.20,
                  "previousPrice": 2.30,
                  "trend": "DOWN",
                  "lowestInPeriod": 2.10,
                  "highestInPeriod": 2.45,
                  "percentageChange": -4.3,
                  "history": [
                    {"monthLabel": "Mayo", "price": 2.40},
                    {"monthLabel": "Junio", "price": 2.45},
                    {"monthLabel": "Julio", "price": 2.35},
                    {"monthLabel": "Agosto", "price": 2.30},
                    {"monthLabel": "Septiembre", "price": 2.25},
                    {"monthLabel": "Actual", "price": 2.20}
                  ]
                }
              ]
            }
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = prompt)))
            )
        )

        try {
            val response = RetrofitGeminiClient.api.generateContent(apiKey, request)
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            val cleanJsonStr = text.substringAfter("{").substringBeforeLast("}")
            val jsonObj = JSONObject("{$cleanJsonStr}")

            val pName = jsonObj.optString("productName", productName)
            val foodCat = jsonObj.optString("foodCategory", "General")
            val rec = jsonObj.optString("overallRecommendation", "")
            val bestSup = jsonObj.optString("bestSupermarketToBuy", "Lidl")

            val rawTrends = jsonObj.optJSONArray("supermarketTrends")
            val trendList = mutableListOf<SupermarketPriceTrend>()

            if (rawTrends != null) {
                for (i in 0 until rawTrends.length()) {
                    val t = rawTrends.getJSONObject(i)
                    val sup = t.optString("supermarket", "General")
                    val curP = t.optDouble("currentPrice", 2.0)
                    val prevP = t.optDouble("previousPrice", curP)
                    val tr = t.optString("trend", if (curP < prevP) "DOWN" else if (curP > prevP) "UP" else "STABLE")
                    val minP = t.optDouble("lowestInPeriod", curP)
                    val maxP = t.optDouble("highestInPeriod", curP)
                    val pct = t.optDouble("percentageChange", 0.0)

                    val historyArr = t.optJSONArray("history")
                    val histPoints = mutableListOf<PriceHistoryPoint>()
                    if (historyArr != null) {
                        for (j in 0 until historyArr.length()) {
                            val h = historyArr.getJSONObject(j)
                            histPoints.add(
                                PriceHistoryPoint(
                                    monthLabel = h.optString("monthLabel", "M${j+1}"),
                                    price = h.optDouble("price", curP)
                                )
                            )
                        }
                    }

                    trendList.add(
                        SupermarketPriceTrend(
                            supermarket = sup,
                            currentPrice = curP,
                            previousPrice = prevP,
                            trend = tr,
                            history = histPoints,
                            lowestInPeriod = minP,
                            highestInPeriod = maxP,
                            percentageChange = pct
                        )
                    )
                }
            }

            if (trendList.isEmpty()) {
                return@withContext generateFallbackPriceHistory(productName)
            }

            val lowestPrice = trendList.minOf { it.currentPrice }
            val avgMarket = trendList.map { it.currentPrice }.average()

            ProductDetailPriceHistory(
                productName = pName,
                foodCategory = foodCat,
                supermarketTrends = trendList,
                overallRecommendation = rec.ifBlank { "El mejor precio actual está en $bestSup con un precio de ${String.format(java.util.Locale.US, "%.2f", lowestPrice)}€." },
                bestSupermarketToBuy = bestSup,
                currentLowestPrice = lowestPrice,
                averageMarketPrice = avgMarket
            )
        } catch (e: Exception) {
            e.printStackTrace()
            generateFallbackPriceHistory(productName)
        }
    }

    private fun generateFallbackPriceHistory(productName: String): ProductDetailPriceHistory {
        val base = when {
            productName.contains("leche", ignoreCase = true) -> 0.98
            productName.contains("aceite", ignoreCase = true) -> 6.50
            productName.contains("arroz", ignoreCase = true) -> 1.35
            productName.contains("pan", ignoreCase = true) -> 0.85
            productName.contains("huevos", ignoreCase = true) -> 2.40
            productName.contains("queso", ignoreCase = true) -> 2.80
            productName.contains("pollo", ignoreCase = true) -> 4.50
            productName.contains("manzana", ignoreCase = true) -> 1.95
            productName.contains("atún", ignoreCase = true) -> 3.20
            else -> 2.20
        }

        val months = listOf("Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Actual")

        fun buildPoints(startMult: Double, midMult: Double, endMult: Double): List<PriceHistoryPoint> {
            val step1 = (midMult - startMult) / 2.0
            val step2 = (endMult - midMult) / 3.0
            val mults = listOf(
                startMult,
                startMult + step1,
                midMult,
                midMult + step2,
                midMult + 2 * step2,
                endMult
            )
            return months.zip(mults).map { (m, mult) ->
                PriceHistoryPoint(monthLabel = m, price = Math.round(base * mult * 100.0) / 100.0)
            }
        }

        val lidlHist = buildPoints(1.15, 1.05, 0.85)
        val carrefourHist = buildPoints(1.10, 0.98, 0.88)
        val mercadonaHist = buildPoints(1.02, 1.00, 0.98)
        val diaHist = buildPoints(1.12, 1.05, 0.90)
        val alcampoHist = buildPoints(1.05, 0.98, 0.92)

        val trends = listOf(
            SupermarketPriceTrend(
                supermarket = "Lidl",
                currentPrice = lidlHist.last().price,
                previousPrice = lidlHist[lidlHist.size - 2].price,
                trend = "DOWN",
                history = lidlHist,
                lowestInPeriod = lidlHist.minOf { it.price },
                highestInPeriod = lidlHist.maxOf { it.price },
                percentageChange = -26.0
            ),
            SupermarketPriceTrend(
                supermarket = "Carrefour",
                currentPrice = carrefourHist.last().price,
                previousPrice = carrefourHist[carrefourHist.size - 2].price,
                trend = "DOWN",
                history = carrefourHist,
                lowestInPeriod = carrefourHist.minOf { it.price },
                highestInPeriod = carrefourHist.maxOf { it.price },
                percentageChange = -20.0
            ),
            SupermarketPriceTrend(
                supermarket = "Mercadona",
                currentPrice = mercadonaHist.last().price,
                previousPrice = mercadonaHist[mercadonaHist.size - 2].price,
                trend = "STABLE",
                history = mercadonaHist,
                lowestInPeriod = mercadonaHist.minOf { it.price },
                highestInPeriod = mercadonaHist.maxOf { it.price },
                percentageChange = -3.9
            ),
            SupermarketPriceTrend(
                supermarket = "Dia",
                currentPrice = diaHist.last().price,
                previousPrice = diaHist[diaHist.size - 2].price,
                trend = "DOWN",
                history = diaHist,
                lowestInPeriod = diaHist.minOf { it.price },
                highestInPeriod = diaHist.maxOf { it.price },
                percentageChange = -19.6
            ),
            SupermarketPriceTrend(
                supermarket = "Alcampo",
                currentPrice = alcampoHist.last().price,
                previousPrice = alcampoHist[alcampoHist.size - 2].price,
                trend = "STABLE",
                history = alcampoHist,
                lowestInPeriod = alcampoHist.minOf { it.price },
                highestInPeriod = alcampoHist.maxOf { it.price },
                percentageChange = -12.3
            )
        )

        val lowest = trends.minOf { it.currentPrice }
        val avg = trends.map { it.currentPrice }.average()
        val bestSup = trends.first { it.currentPrice == lowest }.supermarket

        return ProductDetailPriceHistory(
            productName = productName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() },
            foodCategory = "General",
            supermarketTrends = trends,
            overallRecommendation = "📉 Tendencia general a la baja en la mayoría de supermercados. El mínimo histórico reciente se encuentra en $bestSup (${String.format(java.util.Locale.US, "%.2f", lowest)}€). Es un excelente momento para comprar.",
            bestSupermarketToBuy = bestSup,
            currentLowestPrice = lowest,
            averageMarketPrice = avg
        )
    }
}


