package com.example.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.data.local.AppSettings
import com.example.util.NotificationHelper
import java.util.Calendar

class PantryExpirationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getDatabase(applicationContext)
            val settings = db.appSettingsDao().getSettingsOnce() ?: AppSettings()

            val warningDays = settings.expirationWarningDays
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, warningDays)
            val targetTimeMillis = calendar.timeInMillis

            val expiringItems = db.pantryDao().getExpiringItemsList(targetTimeMillis)
            val nowMillis = System.currentTimeMillis()
            val expiringNames = mutableListOf<String>()

            for (item in expiringItems) {
                val diffMillis = item.expirationDateMillis - nowMillis
                val daysLeft = Math.ceil(diffMillis.toDouble() / (1000 * 60 * 60 * 24)).toInt()

                if (daysLeft in 0..warningDays) {
                    expiringNames.add(item.name)
                }

                NotificationHelper.showExpirationNotification(
                    context = applicationContext,
                    productName = item.name,
                    daysLeft = daysLeft
                )
            }

            if (expiringNames.isNotEmpty()) {
                val topNamesText = expiringNames.take(3).joinToString(", ")
                val customIdea = "Tienes $topNamesText próximos a vencer. ¿Qué tal usarlos hoy para cocinar y evitar el desperdicio?"

                NotificationHelper.showProactiveIngredientNotification(
                    context = applicationContext,
                    ingredientNames = expiringNames,
                    customSuggestion = customIdea
                )
            }

            if (settings.monthlyBudget > 0.0) {
                try {
                    val history = db.purchaseHistoryDao().getAllHistoryItemsList()
                    val cal = Calendar.getInstance()
                    val currentMonth = cal.get(Calendar.MONTH)
                    val currentYear = cal.get(Calendar.YEAR)
                    val currentMonthSpent = history.filter {
                        val itemCal = Calendar.getInstance().apply { timeInMillis = it.purchaseDateMillis }
                        itemCal.get(Calendar.MONTH) == currentMonth && itemCal.get(Calendar.YEAR) == currentYear
                    }.sumOf { it.price * it.quantity }

                    val percentage = (currentMonthSpent / settings.monthlyBudget) * 100.0
                    if (percentage >= 100.0) {
                        NotificationHelper.showBudgetAlertNotification(
                            context = applicationContext,
                            spent = currentMonthSpent,
                            budget = settings.monthlyBudget,
                            alertLevel = 100
                        )
                    } else if (percentage >= 80.0) {
                        NotificationHelper.showBudgetAlertNotification(
                            context = applicationContext,
                            spent = currentMonthSpent,
                            budget = settings.monthlyBudget,
                            alertLevel = 80
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (settings.isAlexaSyncEnabled) {
                try {
                    val alexaSyncManager = com.example.data.remote.AlexaSyncManager(db.shoppingListDao())
                    alexaSyncManager.performBidirectionalSync(settings)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
