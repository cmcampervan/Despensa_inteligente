package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {
    private const val CHANNEL_ID = "pantry_expiration_channel"
    private const val CHANNEL_NAME = "Alertas de Caducidad y Stock"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones automáticas de productos próximos a vencer o stock bajo"
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showExpirationNotification(context: Context, productName: String, daysLeft: Int) {
        createNotificationChannel(context)

        val title = if (daysLeft <= 0) "¡Producto Vencido!" else "¡Producto Próximo a Vencer!"
        val text = if (daysLeft <= 0) {
            "El producto '$productName' ha caducado. Revisa tu alacena/nevera."
        } else {
            "El producto '$productName' caduca en $daysLeft día(s)."
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(productName.hashCode(), builder.build())
    }

    fun showLowStockNotification(context: Context, productName: String) {
        createNotificationChannel(context)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_input_add)
            .setContentTitle("Stock Bajo - Añadido a Lista de Compras")
            .setContentText("'$productName' ha alcanzado su límite mínimo. Se añadió a tu lista de compras.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify((productName + "_stock").hashCode(), builder.build())
    }

    fun showProactiveIngredientNotification(context: Context, ingredientNames: List<String>, customSuggestion: String? = null) {
        if (ingredientNames.isEmpty()) return
        createNotificationChannel(context)

        val namesText = ingredientNames.take(3).joinToString(", ")
        val title = "🍳 ¡Sugerencia Proactiva de Cocina!"
        val text = customSuggestion ?: "Tienes $namesText próximos a vencer. ¿Por qué no los usas para cocinar hoy?"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify("proactive_ingredients_suggestion".hashCode(), builder.build())
    }

    fun showBudgetAlertNotification(context: Context, spent: Double, budget: Double, alertLevel: Int) {
        createNotificationChannel(context)

        val formattedSpent = String.format(java.util.Locale.US, "%.2f", spent)
        val formattedBudget = String.format(java.util.Locale.US, "%.2f", budget)

        val title = if (alertLevel >= 100) {
            "🚨 ¡Presupuesto Mensual Alcanzado! (100%)"
        } else {
            "⚠️ ¡Alerta: Presupuesto al $alertLevel%!"
        }

        val text = if (alertLevel >= 100) {
            "Has alcanzado o superado tu presupuesto mensual: $formattedSpent € de $formattedBudget €."
        } else {
            "Tus compras acumuladas han alcanzado el 80% de tu presupuesto mensual: $formattedSpent € de $formattedBudget €."
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(if (alertLevel >= 100) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify("monthly_budget_alert_$alertLevel".hashCode(), builder.build())
    }
}
