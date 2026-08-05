package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.remote.ScannedProduct
import com.example.ui.theme.IndigoPrimary

data class EditableScannedProduct(
    var name: String,
    var priceText: String,
    var supermarket: String,
    var isPromotion: Boolean,
    var category: String,
    var foodCategory: String,
    var conservationTip: String
)

@Composable
fun ImageScanDialog(
    isAnalyzing: Boolean,
    scannedResults: List<ScannedProduct>,
    onDismiss: () -> Unit,
    onImageSelected: (Bitmap) -> Unit,
    onAddProductsToInventory: (List<ScannedProduct>) -> Unit,
    onAddProductsToShoppingList: (List<ScannedProduct>) -> Unit
) {
    val context = LocalContext.current
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Maintain an editable list for user validation before submitting to form/list
    val editableList = remember { mutableStateListOf<EditableScannedProduct>() }

    LaunchedEffect(scannedResults) {
        if (scannedResults.isNotEmpty()) {
            editableList.clear()
            scannedResults.forEach { item ->
                editableList.add(
                    EditableScannedProduct(
                        name = item.name,
                        priceText = if (item.estimatedPrice > 0) String.format(java.util.Locale.US, "%.2f", item.estimatedPrice) else "1.50",
                        supermarket = item.supermarket.ifBlank { "General" },
                        isPromotion = item.isPromotion,
                        category = item.category,
                        foodCategory = item.foodCategory,
                        conservationTip = item.conservationTip
                    )
                )
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, it))
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            }
            selectedBitmap = bitmap
            onImageSelected(bitmap)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let {
            selectedBitmap = it
            onImageSelected(it)
        }
    }

    var showCameraXScanner by remember { mutableStateOf(false) }

    if (showCameraXScanner) {
        CameraXProductScannerDialog(
            onDismiss = { showCameraXScanner = false },
            onPhotoCaptured = { bitmap, _ ->
                selectedBitmap = bitmap
                showCameraXScanner = false
                onImageSelected(bitmap)
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = IndigoPrimary)
                Text("Escanear Folleto / Foto con Gemini AI")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Fotografía o sube la imagen de un folleto de ofertas, recibo o producto. Gemini AI extraerá los pares Nombre-Precio para tu lista:",
                    style = MaterialTheme.typography.bodyMedium
                )

                Button(
                    onClick = { showCameraXScanner = true },
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                    modifier = Modifier.fillMaxWidth().testTag("open_camerax_scanner_button")
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Escanear con CameraX (Live ML)")
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { cameraLauncher.launch(null) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cámara Rápida")
                    }

                    OutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Galería")
                    }
                }

                selectedBitmap?.let { bmp ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Foto o folleto elegido",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                            )

                            Button(
                                onClick = { onImageSelected(bmp) },
                                enabled = !isAnalyzing,
                                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("send_image_gemini_button")
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isAnalyzing) "Analizando folleto..." else "✨ Extraer Pares Nombre-Precio")
                            }
                        }
                    }
                }

                if (isAnalyzing) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text("Procesando pares 'Nombre-Precio' con Gemini AI...", style = MaterialTheme.typography.bodySmall, color = IndigoPrimary)
                    }
                }

                if (editableList.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = IndigoPrimary)
                            Text(
                                "${editableList.size} par(es) extraído(s). Revisa y ajusta antes de agregar:",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = IndigoPrimary
                            )
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(editableList) { index, item ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            "Producto #${index + 1}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = IndigoPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        IconButton(
                                            onClick = { editableList.removeAt(index) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Eliminar",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    OutlinedTextField(
                                        value = item.name,
                                        onValueChange = { editableList[index] = item.copy(name = it) },
                                        label = { Text("Nombre del Producto") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        OutlinedTextField(
                                            value = item.priceText,
                                            onValueChange = { editableList[index] = item.copy(priceText = it) },
                                            label = { Text("Precio (€)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )

                                        OutlinedTextField(
                                            value = item.supermarket,
                                            onValueChange = { editableList[index] = item.copy(supermarket = it) },
                                            label = { Text("Supermercado") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(
                                                checked = item.isPromotion,
                                                onCheckedChange = { editableList[index] = item.copy(isPromotion = it) }
                                            )
                                            Text("🏷️ Oferta / Promoción", style = MaterialTheme.typography.bodySmall)
                                        }

                                        Text(
                                            "Cat: ${item.foodCategory}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    TextButton(
                        onClick = {
                            editableList.add(
                                EditableScannedProduct(
                                    name = "Nuevo Producto",
                                    priceText = "1.00",
                                    supermarket = "General",
                                    isPromotion = true,
                                    category = "Alacena",
                                    foodCategory = "Otros",
                                    conservationTip = ""
                                )
                            )
                        },
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Añadir Par Nombre-Precio Manual", style = MaterialTheme.typography.labelMedium)
                    }

                    val validatedProducts = editableList.map {
                        ScannedProduct(
                            name = it.name.trim().ifBlank { "Producto" },
                            category = it.category,
                            foodCategory = it.foodCategory,
                            estimatedPrice = it.priceText.replace(',', '.').toDoubleOrNull() ?: 1.0,
                            isPromotion = it.isPromotion,
                            supermarket = it.supermarket.trim().ifBlank { "General" },
                            conservationTip = it.conservationTip
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                onAddProductsToShoppingList(validatedProducts)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                            modifier = Modifier.weight(1f).testTag("confirm_add_shopping_list_button")
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("A Lista Compra")
                        }

                        OutlinedButton(
                            onClick = {
                                onAddProductsToInventory(validatedProducts)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("A Inventario")
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}
