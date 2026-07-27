package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.local.PantryItem
import com.example.data.local.PurchaseHistoryItem
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExportUtil {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun exportPantryToCsv(context: Context, items: List<PantryItem>) {
        val fileName = "inventario_despensa_${System.currentTimeMillis()}.csv"
        val file = File(context.cacheDir, fileName)

        val csvHeader = "ID,Nombre,Cantidad,Unidad,Ubicacion,Categoria,Supermercado,Fecha_Caducidad,Precio,Consejo_Conservacion\n"
        val csvBody = items.joinToString("\n") { item ->
            val expStr = dateFormat.format(Date(item.expirationDateMillis))
            val cleanName = item.name.replace(",", " ")
            val cleanTip = item.conservationTip.replace(",", " ")
            "${item.id},$cleanName,${item.quantity},${item.unit},${item.locationCategory},${item.foodCategory},${item.supermarket},$expStr,${item.price},$cleanTip"
        }

        file.writeText(csvHeader + csvBody)
        shareCsvFile(context, file, "Exportar Inventario CSV")
    }

    fun exportHistoryToCsv(context: Context, items: List<PurchaseHistoryItem>) {
        val fileName = "historial_compras_${System.currentTimeMillis()}.csv"
        val file = File(context.cacheDir, fileName)

        val csvHeader = "ID,Nombre,Cantidad,Unidad,Precio,Supermercado,Ubicacion,Categoria,Fecha_Compra,Promocion\n"
        val csvBody = items.joinToString("\n") { item ->
            val dateStr = dateFormat.format(Date(item.purchaseDateMillis))
            val cleanName = item.name.replace(",", " ")
            "${item.id},$cleanName,${item.quantity},${item.unit},${item.price},${item.supermarket},${item.locationCategory},${item.foodCategory},$dateStr,${item.wasPromotion}"
        }

        file.writeText(csvHeader + csvBody)
        shareCsvFile(context, file, "Exportar Historial de Compras CSV")
    }

    private fun shareCsvFile(context: Context, file: File, title: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, title)
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
