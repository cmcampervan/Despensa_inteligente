package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

data class AlexaList(
    @Json(name = "listId") val listId: String,
    @Json(name = "name") val name: String,
    @Json(name = "state") val state: String? = "active",
    @Json(name = "status") val status: String? = "active",
    @Json(name = "items") val items: List<AlexaListItem>? = emptyList()
)

data class AlexaListsResponse(
    @Json(name = "lists") val lists: List<AlexaList>? = emptyList()
)

data class AlexaListItem(
    @Json(name = "id") val id: String,
    @Json(name = "value") val value: String,
    @Json(name = "status") val status: String = "active", // "active" or "completed"
    @Json(name = "createdTime") val createdTime: String? = null,
    @Json(name = "updatedTime") val updatedTime: String? = null,
    @Json(name = "version") val version: Int = 1
)

data class AlexaCreateItemRequest(
    @Json(name = "value") val value: String,
    @Json(name = "status") val status: String = "active"
)

data class AlexaUpdateItemRequest(
    @Json(name = "value") val value: String,
    @Json(name = "status") val status: String,
    @Json(name = "version") val version: Int = 1
)

interface AlexaApi {
    @GET("v2/householdlists/")
    suspend fun getLists(
        @Header("Authorization") authHeader: String
    ): Response<AlexaListsResponse>

    @GET("v2/householdlists/{listId}/active")
    suspend fun getActiveListItems(
        @Header("Authorization") authHeader: String,
        @Path("listId") listId: String
    ): Response<AlexaList>

    @POST("v2/householdlists/{listId}/items")
    suspend fun createListItem(
        @Header("Authorization") authHeader: String,
        @Path("listId") listId: String,
        @Body request: AlexaCreateItemRequest
    ): Response<AlexaListItem>

    @PUT("v2/householdlists/{listId}/items/{itemId}")
    suspend fun updateListItem(
        @Header("Authorization") authHeader: String,
        @Path("listId") listId: String,
        @Path("itemId") itemId: String,
        @Body request: AlexaUpdateItemRequest
    ): Response<AlexaListItem>

    @DELETE("v2/householdlists/{listId}/items/{itemId}")
    suspend fun deleteListItem(
        @Header("Authorization") authHeader: String,
        @Path("listId") listId: String,
        @Path("itemId") itemId: String
    ): Response<Unit>
}

object AlexaApiClient {
    const val BASE_URL_US = "https://api.amazonalexa.com/"
    const val BASE_URL_EU = "https://api.eu.amazonalexa.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    val apiUs: AlexaApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL_US)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AlexaApi::class.java)
    }

    val apiEu: AlexaApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL_EU)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AlexaApi::class.java)
    }

    val api: AlexaApi get() = apiUs
}

