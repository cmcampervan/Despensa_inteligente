package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.ui.components.getCameraProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.local.PantryItem
import com.example.data.remote.ScannedProduct
import com.example.ui.components.MLProductProcessor
import com.example.ui.theme.IndigoPrimary
import com.example.ui.viewmodel.PantryViewModel
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScannerScreen(
    viewModel: PantryViewModel,
    modifier: Modifier = Modifier,
    onNavigateToInventory: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var isTorchOn by remember { mutableStateOf(false) }
    var autoAddEnabled by remember { mutableStateOf(true) }
    var detectedCode by remember { mutableStateOf<String?>(null) }
    var scannedProduct by remember { mutableStateOf<ScannedProduct?>(null) }
    var isLoadingProduct by remember { mutableStateOf(false) }
    var cameraControl: CameraControl? by remember { mutableStateOf(null) }
    var previewView: PreviewView? by remember { mutableStateOf(null) }

    val sessionAddedProducts = remember { mutableStateListOf<PantryItem>() }
    var showManualDialog by remember { mutableStateOf(false) }
    var showSessionList by remember { mutableStateOf(false) }
    var manualBarcodeInput by remember { mutableStateOf("") }
    var statusNotificationMessage by remember { mutableStateOf<String?>(null) }

    val mlProcessor = remember { MLProductProcessor() }

    val infiniteTransition = rememberInfiniteTransition(label = "scannerLaser")
    val laserOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserOffset"
    )

    fun triggerVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(100)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun processBarcodeResult(code: String) {
        if (isLoadingProduct) return
        detectedCode = code
        isLoadingProduct = true
        triggerVibration()

        coroutineScope.launch {
            try {
                val product = viewModel.fetchProductByBarcode(code)
                scannedProduct = product

                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, 10)
                val newItem = PantryItem(
                    name = product.name,
                    quantity = 1.0,
                    unit = "ud",
                    locationCategory = product.category.ifBlank { "Alacena" },
                    foodCategory = product.foodCategory.ifBlank { "Otros" },
                    expirationDateMillis = cal.timeInMillis,
                    price = product.estimatedPrice,
                    supermarket = product.supermarket.ifBlank { "Mercadona" },
                    isPromotion = product.isPromotion,
                    conservationTip = product.conservationTip
                )

                if (autoAddEnabled) {
                    viewModel.addOrUpdatePantryItem(newItem)
                    sessionAddedProducts.add(0, newItem)
                    statusNotificationMessage = "✓ '${product.name}' añadido a la alacena"
                }
            } catch (e: Exception) {
                val fallbackProduct = ScannedProduct(
                    name = "Producto $code",
                    category = "Alacena",
                    foodCategory = "Otros",
                    estimatedPrice = 1.99,
                    isPromotion = false,
                    supermarket = "Mercadona",
                    conservationTip = "Conservar en lugar fresco."
                )
                scannedProduct = fallbackProduct

                if (autoAddEnabled) {
                    val cal = Calendar.getInstance()
                    cal.add(Calendar.DAY_OF_YEAR, 10)
                    val newItem = PantryItem(
                        name = fallbackProduct.name,
                        quantity = 1.0,
                        unit = "ud",
                        locationCategory = "Alacena",
                        foodCategory = "Otros",
                        expirationDateMillis = cal.timeInMillis,
                        price = 1.99,
                        supermarket = "Mercadona",
                        conservationTip = "Añadido por código $code"
                    )
                    viewModel.addOrUpdatePantryItem(newItem)
                    sessionAddedProducts.add(0, newItem)
                    statusNotificationMessage = "✓ '${newItem.name}' añadido automáticamente"
                }
            } finally {
                isLoadingProduct = false
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("barcode_scanner_screen")
    ) {
        if (!hasPermission) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.padding(24.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = IndigoPrimary
                        )
                        Text(
                            "Escáner de Código de Barras",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Para escanear códigos de barras de productos con CameraX y ML Kit, activa el permiso de cámara.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                            modifier = Modifier.testTag("grant_camera_permission_button")
                        ) {
                            Text("Activar Permiso de Cámara")
                        }
                    }
                }
            }
        } else {
            // Camera Live View
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        previewView = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Setup CameraX Lifecycle & Analysis
            LaunchedEffect(hasPermission) {
                if (!hasPermission) return@LaunchedEffect
                try {
                    val cameraProvider = context.getCameraProvider()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView?.surfaceProvider)
                    }

                    val analyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build().also { imageAnalysis ->
                            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                if (detectedCode == null && !isLoadingProduct) {
                                    mlProcessor.analyzeFrame(imageProxy) { result ->
                                        val code = result.barcodeOrOcrText
                                        if (!code.isNullOrBlank() && detectedCode == null) {
                                            processBarcodeResult(code)
                                        }
                                    }
                                } else {
                                    imageProxy.close()
                                }
                            }
                        }

                    var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    if (!cameraProvider.hasCamera(cameraSelector)) {
                        cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                    }

                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        analyzer
                    )
                    cameraControl = camera.cameraControl
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }

            // Dark Dimming Overlay & Target Box
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val boxWidth = width * 0.82f
                val boxHeight = width * 0.52f
                val left = (width - boxWidth) / 2f
                val top = (height - boxHeight) / 2.6f

                // Outer dim background
                drawRect(
                    color = Color.Black.copy(alpha = 0.55f),
                    size = size
                )

                // Cutout
                drawRect(
                    color = Color.Transparent,
                    topLeft = androidx.compose.ui.geometry.Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(boxWidth, boxHeight),
                    blendMode = androidx.compose.ui.graphics.BlendMode.Clear
                )

                // Target Corner Border
                val strokePx = 4.dp.toPx()
                val activeColor = if (detectedCode != null) Color(0xFF10B981) else IndigoPrimary
                drawRect(
                    color = activeColor,
                    topLeft = androidx.compose.ui.geometry.Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(boxWidth, boxHeight),
                    style = Stroke(width = strokePx)
                )

                // Animated Scanning Laser Line
                val laserY = top + (boxHeight * laserOffset)
                drawLine(
                    color = if (detectedCode != null) Color(0xFF10B981) else Color.Red,
                    start = androidx.compose.ui.geometry.Offset(left + 12.dp.toPx(), laserY),
                    end = androidx.compose.ui.geometry.Offset(left + boxWidth - 12.dp.toPx(), laserY),
                    strokeWidth = 3.5f.dp.toPx()
                )
            }

            // Top Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.75f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (detectedCode != null) Color(0xFF10B981) else Color.Red)
                        )
                        Text(
                            text = if (isLoadingProduct) "Procesando..." else if (detectedCode != null) "¡Detectado!" else "CameraX ML Kit Activo",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Flash / Torch Toggle
                    IconButton(
                        onClick = {
                            isTorchOn = !isTorchOn
                            cameraControl?.enableTorch(isTorchOn)
                        },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.75f)),
                        modifier = Modifier.testTag("torch_toggle_button")
                    ) {
                        Icon(
                            if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Linterna",
                            tint = if (isTorchOn) Color.Yellow else Color.White
                        )
                    }

                    // Manual Barcode Button
                    IconButton(
                        onClick = { showManualDialog = true },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.75f)),
                        modifier = Modifier.testTag("manual_barcode_button")
                    ) {
                        Icon(Icons.Default.Keyboard, contentDescription = "Escribir código", tint = Color.White)
                    }
                }
            }

            // Status Banner Popup
            statusNotificationMessage?.let { msg ->
                LaunchedEffect(msg) {
                    kotlinx.coroutines.delay(3000)
                    statusNotificationMessage = null
                }
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 70.dp, start = 16.dp, end = 16.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF10B981),
                    shadowElevation = 6.dp
                ) {
                    Text(
                        text = msg,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }

            // Bottom Control Controls & Result Panel
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Auto-Add Mode Switcher
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.Black.copy(alpha = 0.8f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(20.dp))
                            Column {
                                Text("Añadir Automático", style = MaterialTheme.typography.bodySmall, color = Color.White, fontWeight = FontWeight.Bold)
                                Text("Guarda en alacena al detectar", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                            }
                        }

                        Switch(
                            checked = autoAddEnabled,
                            onCheckedChange = { autoAddEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = IndigoPrimary),
                            modifier = Modifier.testTag("auto_add_toggle_switch")
                        )
                    }
                }

                // Currently Scanned Card or Continuous Scanner Prompt
                if (isLoadingProduct) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(18.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = IndigoPrimary)
                            Text(
                                "Buscando código $detectedCode...",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else if (scannedProduct != null) {
                    val product = scannedProduct!!
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = product.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Cat: ${product.foodCategory} • ${product.supermarket}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Text(
                                    text = "${String.format(Locale.US, "%.2f", product.estimatedPrice)} €",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = IndigoPrimary
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (!autoAddEnabled) {
                                    Button(
                                        onClick = {
                                            val cal = Calendar.getInstance()
                                            cal.add(Calendar.DAY_OF_YEAR, 10)
                                            val newItem = PantryItem(
                                                name = product.name,
                                                quantity = 1.0,
                                                unit = "ud",
                                                locationCategory = product.category.ifBlank { "Alacena" },
                                                foodCategory = product.foodCategory.ifBlank { "Otros" },
                                                expirationDateMillis = cal.timeInMillis,
                                                price = product.estimatedPrice,
                                                supermarket = product.supermarket.ifBlank { "Mercadona" },
                                                conservationTip = product.conservationTip
                                            )
                                            viewModel.addOrUpdatePantryItem(newItem)
                                            sessionAddedProducts.add(0, newItem)
                                            statusNotificationMessage = "✓ '${product.name}' guardado"
                                            detectedCode = null
                                            scannedProduct = null
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("manual_add_to_pantry_button")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Añadir a Alacena")
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        detectedCode = null
                                        scannedProduct = null
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("scan_next_barcode_button")
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Escanear Otro")
                                }
                            }
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.Black.copy(alpha = 0.85f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Apunta al código (EAN-13/QR) o simula escaneo:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                    modifier = Modifier.weight(1f)
                                )

                                if (sessionAddedProducts.isNotEmpty()) {
                                    TextButton(
                                        onClick = { showSessionList = !showSessionList },
                                        modifier = Modifier.testTag("view_session_items_button")
                                    ) {
                                        Text("Sesión (${sessionAddedProducts.size})", color = IndigoPrimary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf(
                                    "8480000123456" to "🥛 Leche",
                                    "8410000001111" to "🫒 Aceite",
                                    "8410000002222" to "🌾 Arroz"
                                ).forEach { (code, label) ->
                                    FilterChip(
                                        selected = false,
                                        onClick = { processBarcodeResult(code) },
                                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = IndigoPrimary.copy(alpha = 0.8f),
                                            labelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Action to go to inventory
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onNavigateToInventory,
                        modifier = Modifier.testTag("go_to_inventory_from_scanner_button")
                    ) {
                        Icon(Icons.Default.Kitchen, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ver Alacena (${sessionAddedProducts.size} añadidos hoy)", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Manual Barcode Input Dialog
    if (showManualDialog) {
        AlertDialog(
            onDismissRequest = { showManualDialog = false },
            icon = { Icon(Icons.Default.Keyboard, contentDescription = null, tint = IndigoPrimary) },
            title = { Text("Ingresar Código Manualmente") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Escribe el número de código de barras (EAN-13, EAN-8 o UPC):", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = manualBarcodeInput,
                        onValueChange = { manualBarcodeInput = it },
                        placeholder = { Text("Ej: 8410000001234") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_barcode_text_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (manualBarcodeInput.isNotBlank()) {
                            val code = manualBarcodeInput.trim()
                            showManualDialog = false
                            manualBarcodeInput = ""
                            processBarcodeResult(code)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                    modifier = Modifier.testTag("confirm_manual_barcode_button")
                ) {
                    Text("Buscar y Añadir")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Session Items Bottom Sheet / Dialog
    if (showSessionList) {
        AlertDialog(
            onDismissRequest = { showSessionList = false },
            title = {
                Text("Productos Añadidos en esta Sesión (${sessionAddedProducts.size})", style = MaterialTheme.typography.titleMedium)
            },
            text = {
                if (sessionAddedProducts.isEmpty()) {
                    Text("Aún no has añadido productos en esta sesión.", style = MaterialTheme.typography.bodySmall)
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                    ) {
                        items(sessionAddedProducts) { p ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(p.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        Text("${p.foodCategory} • ${p.supermarket}", style = MaterialTheme.typography.labelSmall)
                                    }
                                    Text("${String.format(Locale.US, "%.2f", p.price)} €", fontWeight = FontWeight.Bold, color = IndigoPrimary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSessionList = false
                        onNavigateToInventory()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                ) {
                    Text("Ir a Alacena Completa")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSessionList = false }) {
                    Text("Seguir Escaneando")
                }
            }
        )
    }
}
