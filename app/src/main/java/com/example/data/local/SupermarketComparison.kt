package com.example.data.local

data class SupermarketOffer(
    val supermarket: String,
    val regularPrice: Double,
    val offerPrice: Double,
    val offerDescription: String,
    val isPromotion: Boolean = false,
    val isCheapest: Boolean = false,
    val unitPriceInfo: String = "",
    val validUntil: String = "Oferta activa"
)

data class ProductSupermarketComparison(
    val productName: String,
    val foodCategory: String = "Otros",
    val offers: List<SupermarketOffer>,
    val cheapestSupermarket: String,
    val bestPrice: Double,
    val maxSavingsPercentage: Int,
    val comparisonSummary: String = ""
)

data class PriceHistoryPoint(
    val monthLabel: String,
    val price: Double
)

data class SupermarketPriceTrend(
    val supermarket: String,
    val currentPrice: Double,
    val previousPrice: Double,
    val trend: String, // "DOWN", "UP", "STABLE"
    val history: List<PriceHistoryPoint>,
    val lowestInPeriod: Double,
    val highestInPeriod: Double,
    val percentageChange: Double
)

data class ProductDetailPriceHistory(
    val productName: String,
    val foodCategory: String = "General",
    val supermarketTrends: List<SupermarketPriceTrend>,
    val overallRecommendation: String,
    val bestSupermarketToBuy: String,
    val currentLowestPrice: Double,
    val averageMarketPrice: Double
)

