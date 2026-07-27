package com.example.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.PantryItem
import com.example.data.remote.ScannedProduct
import com.example.util.ConservationTips
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.filled.Close
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductDialog(
    initialItem: PantryItem? = null,
    onDismiss: () -> Unit,
    onSave: (PantryItem) -> Unit,
    onFetchOnlinePrice: (suspend (String, String) -> ScannedProduct)? = null,
    onFetchProductByBarcode: (suspend (String) -> ScannedProduct)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var name by remember { mutableStateOf(initialItem?.name ?: "") }
    var quantityText by remember { mutableStateOf(initialItem?.quantity?.toString() ?: "1.0") }
    var unit by remember { mutableStateOf(initialItem?.unit ?: "ud") }
    var locationCategory by remember { mutableStateOf(initialItem?.locationCategory ?: "ALACENA") }
    var foodCategory by remember { mutableStateOf(initialItem?.foodCategory ?: "Otros") }
    var supermarket by remember { mutableStateOf(initialItem?.supermarket ?: "Mercadona") }
    var minThresholdText by remember { mutableStateOf(initialItem?.minThreshold?.toString() ?: "1.0") }
    var priceText by remember { mutableStateOf(initialItem?.price?.toString() ?: "0.0") }
    var isPromotion by remember { mutableStateOf(initialItem?.isPromotion ?: false) }
    var barcode by remember { mutableStateOf(initialItem?.barcode ?: "") }
    var imageUri by remember { mutableStateOf(initialItem?.imageUri ?: "") }
    var isFetchingPrice by remember { mutableStateOf(false) }

    val calendar = remember {
        Calendar.getInstance().apply {
            if (initialItem != null) {
                timeInMillis = initialItem.expirationDateMillis
            } else {
                add(Calendar.DAY_OF_YEAR, 7) // default 1 week
            }
        }
    }
    var expirationMillis by remember { mutableLongStateOf(calendar.timeInMillis) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                expirationMillis = calendar.timeInMillis
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    val foodCategories = listOf("Lácteos", "Carnes y Pescados", "Frutas y Verduras", "Granos y Cereales", "Bebidas", "Enlatados", "Snacks", "Congelados", "Otros")
    val units = listOf("ud", "kg", "g", "L", "ml", "pack")
    val supermarkets = listOf("Mercadona", "Carrefour", "Lidl", "Alcampo", "Día", "Consum", "Eroski", "Otro")

    val tip = remember(name, foodCategory, locationCategory) {
        if (name.isNotBlank()) ConservationTips.getTipForProduct(name, foodCategory, locationCategory) else ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialItem == null) "Agregar Producto" else "Editar Producto") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del Producto *") },
                    modifier = Modifier.fillMaxWidth().testTag("product_name_input"),
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text("Cantidad") },
                        modifier = Modifier.weight(1f).testTag("product_quantity_input"),
                        singleLine = true
                    )

                    // Unit Selector
                    var unitExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = unitExpanded,
                        onExpandedChange = { unitExpanded = !unitExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = unit,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Unidad") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = unitExpanded,
                            onDismissRequest = { unitExpanded = false }
                        ) {
                            units.forEach { u ->
                                DropdownMenuItem(
                                    text = { Text(u) },
                                    onClick = {
                                        unit = u
                                        unitExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Ubicación (Alacena vs Nevera)
                Text("Ubicación Principal:", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = locationCategory == "ALACENA",
                        onClick = { locationCategory = "ALACENA" },
                        label = { Text("Alacena / Despensa") },
                        modifier = Modifier.testTag("location_alacena_chip")
                    )
                    FilterChip(
                        selected = locationCategory == "NEVERA",
                        onClick = { locationCategory = "NEVERA" },
                        label = { Text("Nevera / Frigo") },
                        modifier = Modifier.testTag("location_nevera_chip")
                    )
                }

                // Categoría de alimento
                var categoryExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = foodCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoría de Alimento") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        foodCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    foodCategory = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                // Fecha de Caducidad
                OutlinedTextField(
                    value = dateFormat.format(Date(expirationMillis)),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Fecha de Caducidad") },
                    trailingIcon = {
                        IconButton(onClick = { datePickerDialog.show() }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Elegir Fecha")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Supermercado
                var superExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = superExpanded,
                    onExpandedChange = { superExpanded = !superExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = supermarket,
                        onValueChange = { supermarket = it },
                        label = { Text("Supermercado / Lugar de Compra") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = superExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = superExpanded,
                        onDismissRequest = { superExpanded = false }
                    ) {
                        supermarkets.forEach { sm ->
                            DropdownMenuItem(
                                text = { Text(sm) },
                                onClick = {
                                    supermarket = sm
                                    superExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = minThresholdText,
                        onValueChange = { minThresholdText = it },
                        label = { Text("Límite Mínimo") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Precio (€)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                var showCameraXDialog by remember { mutableStateOf(false) }
                var showBarcodeScannerDialog by remember { mutableStateOf(false) }

                if (showBarcodeScannerDialog) {
                    CameraXBarcodeScannerDialog(
                        onDismiss = { showBarcodeScannerDialog = false },
                        onBarcodeScanned = { scanned ->
                            name = scanned.name
                            if (scanned.foodCategory.isNotBlank() && scanned.foodCategory != "Otros") {
                                foodCategory = scanned.foodCategory
                            }
                            if (scanned.category == "NEVERA" || scanned.category == "ALACENA") {
                                locationCategory = scanned.category
                            }
                            if (scanned.estimatedPrice > 0) {
                                priceText = String.format(Locale.US, "%.2f", scanned.estimatedPrice)
                            }
                            if (scanned.supermarket.isNotBlank()) {
                                supermarket = scanned.supermarket
                            }
                            showBarcodeScannerDialog = false
                        },
                        onFetchProductByBarcode = { code ->
                            onFetchProductByBarcode?.invoke(code) ?: ScannedProduct(
                                name = "Producto $code",
                                category = "Alacena",
                                foodCategory = "Otros",
                                estimatedPrice = 1.99,
                                isPromotion = false,
                                supermarket = "Mercadona",
                                conservationTip = "Guardar en lugar seco."
                            )
                        }
                    )
                }

                if (showCameraXDialog) {
                    CameraXProductScannerDialog(
                        onDismiss = { showCameraXDialog = false },
                        onPhotoCaptured = { bitmap, mlResult ->
                            showCameraXDialog = false
                            try {
                                val file = java.io.File(context.filesDir, "product_${System.currentTimeMillis()}.jpg")
                                java.io.FileOutputStream(file).use { out ->
                                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                                }
                                imageUri = file.absolutePath
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            if (!mlResult.detectedLabel.isNullOrBlank()) {
                                name = mlResult.detectedLabel.substringBefore(" (")
                            }
                            if (mlResult.detectedCategory.isNotBlank()) {
                                locationCategory = mlResult.detectedCategory
                            }
                        }
                    )
                }

                if (imageUri.isNotBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AsyncImage(
                                    model = imageUri,
                                    contentDescription = "Foto capturada del producto",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Column {
                                    Text("Foto Personalizada", style = MaterialTheme.typography.bodyMedium)
                                    Text("Guardada para este producto", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            IconButton(
                                onClick = { imageUri = "" },
                                modifier = Modifier.testTag("remove_photo_button")
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Eliminar foto")
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showBarcodeScannerDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("scan_barcode_button")
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Escanear Código")
                    }

                    OutlinedButton(
                        onClick = { showCameraXDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("camerax_photo_button")
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tomar Foto")
                    }
                }

                if (onFetchOnlinePrice != null) {
                    OutlinedButton(
                        onClick = {
                            if (name.isNotBlank()) {
                                isFetchingPrice = true
                                coroutineScope.launch {
                                    try {
                                        val scanned = onFetchOnlinePrice(name.trim(), supermarket)
                                        priceText = String.format(Locale.US, "%.2f", scanned.estimatedPrice)
                                        if (scanned.foodCategory.isNotBlank() && scanned.foodCategory != "Otros") {
                                            foodCategory = scanned.foodCategory
                                        }
                                        if (scanned.category == "NEVERA" || scanned.category == "ALACENA") {
                                            locationCategory = scanned.category
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    } finally {
                                        isFetchingPrice = false
                                    }
                                }
                            }
                        },
                        enabled = !isFetchingPrice && name.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("fetch_online_price_button")
                    ) {
                        if (isFetchingPrice) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Buscando en Internet...")
                        } else {
                            Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Consultar Precio en Internet")
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isPromotion,
                        onCheckedChange = { isPromotion = it }
                    )
                    Text("Marcar si es Promoción / Oferta", style = MaterialTheme.typography.bodyMedium)
                }

                // Conservation Tip Preview
                if (tip.isNotBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = "Tip", tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "Consejo de conservación: $tip",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val item = (initialItem ?: PantryItem(name = "", quantity = 1.0, locationCategory = "ALACENA", expirationDateMillis = 0)).copy(
                            name = name.trim(),
                            quantity = quantityText.toDoubleOrNull() ?: 1.0,
                            unit = unit,
                            locationCategory = locationCategory,
                            foodCategory = foodCategory,
                            expirationDateMillis = expirationMillis,
                            supermarket = supermarket,
                            minThreshold = minThresholdText.toDoubleOrNull() ?: 1.0,
                            price = priceText.toDoubleOrNull() ?: 0.0,
                            isPromotion = isPromotion,
                            barcode = barcode,
                            imageUri = imageUri,
                            conservationTip = tip
                        )
                        onSave(item)
                    }
                },
                modifier = Modifier.testTag("save_product_button")
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
