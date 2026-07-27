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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.remote.ScannedProduct

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
            onPhotoCaptured = { bitmap, mlResult ->
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
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Escanear Foto con Gemini AI")
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
                    "Saca una foto o sube una imagen de un producto, recibo/ticket de compra o folleto en oferta:",
                    style = MaterialTheme.typography.bodyMedium
                )

                Button(
                    onClick = { showCameraXScanner = true },
                    colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.IndigoPrimary),
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
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Foto elegida",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    )
                }

                if (isAnalyzing) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Text("Analizando imagen con Gemini AI...", style = MaterialTheme.typography.bodySmall)
                    }
                }

                if (scannedResults.isNotEmpty()) {
                    Text(
                        "Productos Detectados (${scannedResults.size}):",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(scannedResults) { prod ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(prod.name, style = MaterialTheme.typography.bodyLarge)
                                    Text("Ubicación: ${prod.category} | Cat: ${prod.foodCategory}", style = MaterialTheme.typography.bodySmall)
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("Precio: ${prod.estimatedPrice}€", style = MaterialTheme.typography.bodySmall)
                                        if (prod.isPromotion) {
                                            Text("¡EN PROMOCIÓN!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                onAddProductsToInventory(scannedResults)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Text("A Inventario")
                        }

                        OutlinedButton(
                            onClick = {
                                onAddProductsToShoppingList(scannedResults)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("A Lista Compra")
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
