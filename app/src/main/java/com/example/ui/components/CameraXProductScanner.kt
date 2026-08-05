package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.data.remote.ProductLabelIngredientAnalysis
import com.example.data.remote.ScannedProduct
import com.example.ui.theme.IndigoPrimary
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import kotlin.coroutines.resume

/**
 * Machine Learning (ML Kit Barcode Scanning) processor for CameraX frame analysis.
 */
data class MLProductDetectionResult(
    val detectedLabel: String? = null,
    val confidence: Float = 0.0f,
    val detectedCategory: String = "Alacena",
    val barcodeOrOcrText: String? = null,
    val timestampMillis: Long = System.currentTimeMillis()
)

class MLProductProcessor {
    private val barcodeScanner: BarcodeScanner by lazy {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_ALL_FORMATS,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_DATA_MATRIX
            )
            .build()
        BarcodeScanning.getClient(options)
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    fun analyzeFrame(imageProxy: ImageProxy, onResult: (MLProductDetectionResult) -> Unit) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)

        barcodeScanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                val firstBarcode = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }
                if (firstBarcode != null) {
                    val rawBarcode = firstBarcode.rawValue ?: ""
                    val formatName = when (firstBarcode.format) {
                        Barcode.FORMAT_EAN_13 -> "EAN-13"
                        Barcode.FORMAT_EAN_8 -> "EAN-8"
                        Barcode.FORMAT_UPC_A -> "UPC-A"
                        Barcode.FORMAT_UPC_E -> "UPC-E"
                        Barcode.FORMAT_CODE_128 -> "CODE-128"
                        Barcode.FORMAT_CODE_39 -> "CODE-39"
                        Barcode.FORMAT_CODE_93 -> "CODE-93"
                        Barcode.FORMAT_CODABAR -> "CODABAR"
                        Barcode.FORMAT_ITF -> "ITF"
                        Barcode.FORMAT_QR_CODE -> "QR"
                        Barcode.FORMAT_DATA_MATRIX -> "Data Matrix"
                        else -> "Código de Barras"
                    }
                    onResult(
                        MLProductDetectionResult(
                            detectedLabel = "$formatName: $rawBarcode",
                            confidence = 0.99f,
                            detectedCategory = "Alacena",
                            barcodeOrOcrText = rawBarcode
                        )
                    )
                } else {
                    onResult(
                        MLProductDetectionResult(
                            detectedLabel = null,
                            confidence = 0.0f,
                            detectedCategory = "Alacena",
                            barcodeOrOcrText = null
                        )
                    )
                }
            }
            .addOnFailureListener {
                onResult(
                    MLProductDetectionResult(
                        detectedLabel = null,
                        confidence = 0.0f,
                        detectedCategory = "Alacena",
                        barcodeOrOcrText = null
                    )
                )
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    suspend fun processCapturedPhoto(bitmap: Bitmap): MLProductDetectionResult = withContext(Dispatchers.Default) {
        suspendCancellableCoroutine { continuation ->
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            barcodeScanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    val first = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }
                    if (first != null) {
                        val raw = first.rawValue ?: ""
                        continuation.resume(
                            MLProductDetectionResult(
                                detectedLabel = "Código de Barras ML Kit: $raw",
                                confidence = 0.99f,
                                detectedCategory = "Alacena",
                                barcodeOrOcrText = raw
                            )
                        )
                    } else {
                        continuation.resume(
                            MLProductDetectionResult(
                                detectedLabel = "Sin código detectado por ML Kit",
                                confidence = 0.0f,
                                detectedCategory = "Alacena",
                                barcodeOrOcrText = null
                            )
                        )
                    }
                }
                .addOnFailureListener {
                    continuation.resume(
                        MLProductDetectionResult(
                            detectedLabel = "Error al escanear código con ML Kit",
                            confidence = 0.0f,
                            detectedCategory = "Alacena",
                            barcodeOrOcrText = null
                        )
                    )
                }
        }
    }
}

suspend fun Context.getCameraProvider(): ProcessCameraProvider = suspendCancellableCoroutine { continuation ->
    val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
    cameraProviderFuture.addListener({
        try {
            continuation.resume(cameraProviderFuture.get())
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }, ContextCompat.getMainExecutor(this))
}

fun createSampleProductBitmap(context: Context, title: String = "Leche Entera 1L"): Bitmap {
    val width = 600
    val height = 800
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    val paint = android.graphics.Paint()
    paint.color = android.graphics.Color.parseColor("#0F172A")
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

    paint.color = android.graphics.Color.parseColor("#1E293B")
    val rect = android.graphics.RectF(40f, 80f, 560f, 720f)
    canvas.drawRoundRect(rect, 32f, 32f, paint)

    paint.color = android.graphics.Color.parseColor("#6366F1")
    canvas.drawRoundRect(android.graphics.RectF(70f, 120f, 530f, 210f), 24f, 24f, paint)

    paint.color = android.graphics.Color.WHITE
    paint.textSize = 34f
    paint.isFakeBoldText = true
    paint.textAlign = android.graphics.Paint.Align.CENTER
    canvas.drawText(title, 300f, 178f, paint)

    paint.color = android.graphics.Color.LTGRAY
    paint.textSize = 24f
    paint.isFakeBoldText = false
    canvas.drawText("Caducidad: 12/08/2026", 300f, 280f, paint)
    canvas.drawText("Categoría: Lácteos", 300f, 330f, paint)
    canvas.drawText("Supermercado: Mercadona", 300f, 380f, paint)

    paint.color = android.graphics.Color.WHITE
    canvas.drawRect(100f, 450f, 500f, 610f, paint)
    paint.color = android.graphics.Color.BLACK
    var x = 120f
    val random = Random(12345)
    while (x < 480f) {
        val w = if (random.nextBoolean()) 4f else 9f
        canvas.drawRect(x, 460f, x + w, 590f, paint)
        x += w + if (random.nextBoolean()) 5f else 10f
    }
    paint.textSize = 22f
    canvas.drawText("8480000123456", 300f, 645f, paint)

    return bitmap
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraXProductScannerDialog(
    onDismiss: () -> Unit,
    onPhotoCaptured: (Bitmap, MLProductDetectionResult) -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            if (hasCameraPermission) {
                CameraXCaptureView(
                    onDismiss = onDismiss,
                    onPhotoCaptured = onPhotoCaptured
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = IndigoPrimary
                            )
                            Text(
                                "Permiso de Cámara Requerido",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Para usar el escáner de productos CameraX y detección ML, otorga acceso a la cámara.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                                modifier = Modifier.testTag("request_camera_permission_button")
                            ) {
                                Text("Conceder Permiso")
                            }
                            TextButton(onClick = onDismiss) {
                                Text("Cancelar")
                            }
                        }
                    }
                }
            }
        }
    }
}

enum class CameraOverlayMode(val label: String, val hint: String) {
    BARCODE("Código Barras", "Alinea las barras con la línea láser horizontal"),
    BROCHURE_TEXT("Folleto / Texto", "Centra el par nombre-precio en la cuadrícula 3x3"),
    FULL_PRODUCT("Producto Completo", "Encuadra el producto en el objetivo central")
}

@Composable
fun CameraXCaptureView(
    onDismiss: () -> Unit,
    onPhotoCaptured: (Bitmap, MLProductDetectionResult) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashMode by remember { mutableStateOf(ImageCapture.FLASH_MODE_OFF) }
    var selectedOverlayMode by remember { mutableStateOf(CameraOverlayMode.BARCODE) }

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var previewView: PreviewView? by remember { mutableStateOf(null) }

    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var liveMLResult by remember { mutableStateOf<MLProductDetectionResult?>(null) }
    var isProcessingML by remember { mutableStateOf(false) }

    val mlProcessor = remember { MLProductProcessor() }

    // Scanline animation for ML Overlay
    val infiniteTransition = rememberInfiniteTransition(label = "scanline")
    val scanlineOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanlineOffset"
    )

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, it))
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                }
                capturedBitmap = bitmap
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    var cameraError by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (capturedBitmap == null) {
            // Live Camera Preview
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        previewView = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Setup CameraX
            LaunchedEffect(lensFacing) {
                try {
                    val cameraProvider = context.getCameraProvider()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView?.surfaceProvider)
                    }

                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setFlashMode(flashMode)
                        .build()

                    imageCapture = capture

                    val analyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                        .build().also { imageAnalysis ->
                            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                mlProcessor.analyzeFrame(imageProxy) { result ->
                                    liveMLResult = result
                                }
                            }
                        }

                    var cameraSelector = CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build()

                    if (!cameraProvider.hasCamera(cameraSelector)) {
                        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    }

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        capture,
                        analyzer
                    )
                    cameraError = null
                } catch (e: Throwable) {
                    e.printStackTrace()
                    cameraError = "Transmisión de cámara en vivo no disponible. Elige una foto de galería o usa una imagen de muestra."
                }
            }

            val isBarcodeDetected = liveMLResult?.barcodeOrOcrText != null
            val reticleColor = if (isBarcodeDetected) Color(0xFF10B981) else IndigoPrimary

            // High-Tech Scanner Framing Overlay with Dynamic Alignment Guides
            Canvas(modifier = Modifier.fillMaxSize()) {
                val boxWidth = when (selectedOverlayMode) {
                    CameraOverlayMode.BARCODE -> size.width * 0.88f
                    CameraOverlayMode.BROCHURE_TEXT -> size.width * 0.85f
                    CameraOverlayMode.FULL_PRODUCT -> size.width * 0.78f
                }
                val boxHeight = when (selectedOverlayMode) {
                    CameraOverlayMode.BARCODE -> size.height * 0.22f
                    CameraOverlayMode.BROCHURE_TEXT -> size.height * 0.50f
                    CameraOverlayMode.FULL_PRODUCT -> size.height * 0.60f
                }
                val left = (size.width - boxWidth) / 2
                val top = (size.height - boxHeight) / 2.3f

                // Dark translucent mask outside reticle
                drawRect(Color.Black.copy(alpha = 0.55f))

                // Clear center box
                drawRect(
                    color = Color.Transparent,
                    topLeft = androidx.compose.ui.geometry.Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(boxWidth, boxHeight),
                    blendMode = androidx.compose.ui.graphics.BlendMode.Clear
                )

                // Corner Reticles
                val lineLength = 40.dp.toPx()
                val stroke = 4.dp.toPx()

                // Top Left
                drawLine(reticleColor, androidx.compose.ui.geometry.Offset(left, top), androidx.compose.ui.geometry.Offset(left + lineLength, top), strokeWidth = stroke)
                drawLine(reticleColor, androidx.compose.ui.geometry.Offset(left, top), androidx.compose.ui.geometry.Offset(left, top + lineLength), strokeWidth = stroke)

                // Top Right
                drawLine(reticleColor, androidx.compose.ui.geometry.Offset(left + boxWidth, top), androidx.compose.ui.geometry.Offset(left + boxWidth - lineLength, top), strokeWidth = stroke)
                drawLine(reticleColor, androidx.compose.ui.geometry.Offset(left + boxWidth, top), androidx.compose.ui.geometry.Offset(left + boxWidth, top + lineLength), strokeWidth = stroke)

                // Bottom Left
                drawLine(reticleColor, androidx.compose.ui.geometry.Offset(left, top + boxHeight), androidx.compose.ui.geometry.Offset(left + lineLength, top + boxHeight), strokeWidth = stroke)
                drawLine(reticleColor, androidx.compose.ui.geometry.Offset(left, top + boxHeight), androidx.compose.ui.geometry.Offset(left, top + boxHeight - lineLength), strokeWidth = stroke)

                // Bottom Right
                drawLine(reticleColor, androidx.compose.ui.geometry.Offset(left + boxWidth, top + boxHeight), androidx.compose.ui.geometry.Offset(left + boxWidth - lineLength, top + boxHeight), strokeWidth = stroke)
                drawLine(reticleColor, androidx.compose.ui.geometry.Offset(left + boxWidth, top + boxHeight), androidx.compose.ui.geometry.Offset(left + boxWidth, top + boxHeight - lineLength), strokeWidth = stroke)

                // Mode-Specific Alignment Guides
                when (selectedOverlayMode) {
                    CameraOverlayMode.BARCODE -> {
                        // Center Barcode Laser Line
                        val centerY = top + (boxHeight / 2f)
                        val laserColor = if (isBarcodeDetected) Color(0xFF10B981) else Color(0xFFEF4444)
                        drawLine(
                            color = laserColor,
                            start = androidx.compose.ui.geometry.Offset(left, centerY),
                            end = androidx.compose.ui.geometry.Offset(left + boxWidth, centerY),
                            strokeWidth = 3.dp.toPx()
                        )

                        // Barcode Teeth Alignment Ticks at Top and Bottom
                        val tickCount = 12
                        val stepX = boxWidth / (tickCount + 1)
                        for (i in 1..tickCount) {
                            val xPos = left + (stepX * i)
                            // Top tick
                            drawLine(
                                color = reticleColor.copy(alpha = 0.6f),
                                start = androidx.compose.ui.geometry.Offset(xPos, top),
                                end = androidx.compose.ui.geometry.Offset(xPos, top + 10.dp.toPx()),
                                strokeWidth = 2.dp.toPx()
                            )
                            // Bottom tick
                            drawLine(
                                color = reticleColor.copy(alpha = 0.6f),
                                start = androidx.compose.ui.geometry.Offset(xPos, top + boxHeight),
                                end = androidx.compose.ui.geometry.Offset(xPos, top + boxHeight - 10.dp.toPx()),
                                strokeWidth = 2.dp.toPx()
                            )
                        }
                    }

                    CameraOverlayMode.BROCHURE_TEXT -> {
                        // Rule-of-Thirds 3x3 Grid
                        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                        val stepX = boxWidth / 3f
                        val stepY = boxHeight / 3f

                        // Vertical Grid Lines
                        drawLine(
                            color = Color.White.copy(alpha = 0.4f),
                            start = androidx.compose.ui.geometry.Offset(left + stepX, top),
                            end = androidx.compose.ui.geometry.Offset(left + stepX, top + boxHeight),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = dashEffect
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.4f),
                            start = androidx.compose.ui.geometry.Offset(left + (stepX * 2), top),
                            end = androidx.compose.ui.geometry.Offset(left + (stepX * 2), top + boxHeight),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = dashEffect
                        )

                        // Horizontal Grid Lines
                        drawLine(
                            color = Color.White.copy(alpha = 0.4f),
                            start = androidx.compose.ui.geometry.Offset(left, top + stepY),
                            end = androidx.compose.ui.geometry.Offset(left + boxWidth, top + stepY),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = dashEffect
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.4f),
                            start = androidx.compose.ui.geometry.Offset(left, top + (stepY * 2)),
                            end = androidx.compose.ui.geometry.Offset(left + boxWidth, top + (stepY * 2)),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = dashEffect
                        )
                    }

                    CameraOverlayMode.FULL_PRODUCT -> {
                        // Center Crosshair (+)
                        val centerX = left + (boxWidth / 2f)
                        val centerY = top + (boxHeight / 2f)
                        val crosshairSize = 20.dp.toPx()

                        drawLine(
                            color = reticleColor.copy(alpha = 0.8f),
                            start = androidx.compose.ui.geometry.Offset(centerX - crosshairSize, centerY),
                            end = androidx.compose.ui.geometry.Offset(centerX + crosshairSize, centerY),
                            strokeWidth = 2.dp.toPx()
                        )
                        drawLine(
                            color = reticleColor.copy(alpha = 0.8f),
                            start = androidx.compose.ui.geometry.Offset(centerX, centerY - crosshairSize),
                            end = androidx.compose.ui.geometry.Offset(centerX, centerY + crosshairSize),
                            strokeWidth = 2.dp.toPx()
                        )

                        // Center Target Ring
                        drawCircle(
                            color = reticleColor.copy(alpha = 0.5f),
                            radius = 28.dp.toPx(),
                            center = androidx.compose.ui.geometry.Offset(centerX, centerY),
                            style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f))
                        )
                    }
                }

                // Animated Active Scanline
                val currentScanY = top + (boxHeight * scanlineOffset)
                drawLine(
                    color = reticleColor.copy(alpha = 0.85f),
                    start = androidx.compose.ui.geometry.Offset(left, currentScanY),
                    end = androidx.compose.ui.geometry.Offset(left + boxWidth, currentScanY),
                    strokeWidth = 2.dp.toPx()
                )
            }

            // Top Controls Bar (Flash, Camera Switch, Close)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        .testTag("camerax_close_button")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconButton(
                        onClick = {
                            flashMode = when (flashMode) {
                                ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                                ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                                else -> ImageCapture.FLASH_MODE_OFF
                            }
                            imageCapture?.flashMode = flashMode
                        },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            imageVector = when (flashMode) {
                                ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                                ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                                else -> Icons.Default.FlashOff
                            },
                            contentDescription = "Flash",
                            tint = if (flashMode != ImageCapture.FLASH_MODE_OFF) Color.Yellow else Color.White
                        )
                    }

                    IconButton(
                        onClick = {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                CameraSelector.LENS_FACING_FRONT
                            } else {
                                CameraSelector.LENS_FACING_BACK
                            }
                        },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(Icons.Default.Cameraswitch, contentDescription = "Cambiar Cámara", tint = Color.White)
                    }
                }
            }

            // Interactive Framing Mode Chips Bar (Barcode, Text/Brochure, Full Product)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 70.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.75f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        CameraOverlayMode.values().forEach { mode ->
                            val isSelected = selectedOverlayMode == mode
                            val chipIcon = when (mode) {
                                CameraOverlayMode.BARCODE -> Icons.Default.QrCodeScanner
                                CameraOverlayMode.BROCHURE_TEXT -> Icons.Default.ReceiptLong
                                CameraOverlayMode.FULL_PRODUCT -> Icons.Default.Inventory
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) IndigoPrimary else Color.Transparent)
                                    .clickable { selectedOverlayMode = mode }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(chipIcon, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Text(
                                        text = mode.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Live ML Status & Guidance Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isBarcodeDetected) Color(0xFF065F46).copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.8f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isBarcodeDetected) Color(0xFF10B981) else Color.White.copy(alpha = 0.2f)
                ),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 120.dp, start = 20.dp, end = 20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isBarcodeDetected) Icons.Default.CheckCircle else Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = if (isBarcodeDetected) Color(0xFF34D399) else IndigoPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (isBarcodeDetected) {
                            "✨ ¡Alineación Correcta! ${liveMLResult?.detectedLabel}"
                        } else {
                            selectedOverlayMode.hint
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (isBarcodeDetected) FontWeight.Bold else FontWeight.Normal,
                        color = Color.White
                    )
                }
            }

            // Camera Error Overlay Banner
            cameraError?.let { err ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.85f)),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(40.dp))
                        Text("Cámara en Modo Simulación", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(err, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = Color.LightGray)

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { galleryLauncher.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Galería")
                            }

                            OutlinedButton(
                                onClick = {
                                    capturedBitmap = createSampleProductBitmap(context)
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Foto Prueba")
                            }
                        }
                    }
                }
            }

            // Bottom Controls Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp, start = 24.dp, end = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery Launcher
                IconButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = "Galería")
                }

                // Shutter Button
                Button(
                    onClick = {
                        val capture = imageCapture
                        if (capture != null) {
                            val file = File(context.cacheDir, "camerax_product_${System.currentTimeMillis()}.jpg")
                            val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

                            capture.takePicture(
                                outputOptions,
                                cameraExecutor,
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                                        val rotatedBitmap = fixImageRotation(bitmap, file.absolutePath)
                                        capturedBitmap = rotatedBitmap
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        exception.printStackTrace()
                                        capturedBitmap = createSampleProductBitmap(context)
                                    }
                                }
                            )
                        } else {
                            capturedBitmap = createSampleProductBitmap(context)
                        }
                    },
                    modifier = Modifier
                        .size(76.dp)
                        .testTag("camerax_shutter_button"),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(IndigoPrimary)
                            .border(3.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Capturar", tint = Color.White)
                    }
                }

                // Sample Photo Button
                IconButton(
                    onClick = {
                        capturedBitmap = createSampleProductBitmap(context)
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "Foto de prueba")
                }
            }
        } else {
            // Photo Captured Preview & ML Analysis Confirmation View
            val bitmap = capturedBitmap!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Foto Capturada",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { capturedBitmap = null }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Repetir", tint = Color.White)
                    }
                }

                // Image Preview Card
                Card(
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 12.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Foto capturada",
                            modifier = Modifier.fillMaxSize()
                        )

                        // ML Badge Overlay on Image
                        Surface(
                            color = IndigoPrimary.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(bottomEnd = 16.dp),
                            modifier = Modifier.align(Alignment.TopStart)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Text("Listo para ML Kit / Gemini AI", style = MaterialTheme.typography.labelSmall, color = Color.White)
                            }
                        }
                    }
                }

                // Action Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            isProcessingML = true
                            coroutineScope.launch {
                                val mlResult = mlProcessor.processCapturedPhoto(bitmap)
                                isProcessingML = false
                                onPhotoCaptured(bitmap, mlResult)
                            }
                        },
                        enabled = !isProcessingML,
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("camerax_process_ml_button")
                    ) {
                        if (isProcessingML) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ejecutando Modelo ML...")
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Procesar Imagen con ML")
                        }
                    }

                    OutlinedButton(
                        onClick = { capturedBitmap = null },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Repetir Foto")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CameraXLabelIngredientScannerDialog(
    onDismiss: () -> Unit,
    onAnalyzeLabel: suspend (Bitmap) -> ProductLabelIngredientAnalysis,
    onSaveToPantry: (String, List<String>) -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            if (hasCameraPermission) {
                CameraXLabelIngredientCaptureView(
                    onDismiss = onDismiss,
                    onAnalyzeLabel = onAnalyzeLabel,
                    onSaveToPantry = onSaveToPantry
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = IndigoPrimary
                            )
                            Text(
                                "Permiso de Cámara Requerido",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Otorga permiso de cámara para escanear etiquetas de ingredientes de productos.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                                modifier = Modifier.testTag("request_label_camera_permission")
                            ) {
                                Text("Conceder Permiso")
                            }
                            TextButton(onClick = onDismiss) {
                                Text("Cancelar")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CameraXLabelIngredientCaptureView(
    onDismiss: () -> Unit,
    onAnalyzeLabel: suspend (Bitmap) -> ProductLabelIngredientAnalysis,
    onSaveToPantry: (String, List<String>) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashMode by remember { mutableStateOf(ImageCapture.FLASH_MODE_OFF) }

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var previewView: PreviewView? by remember { mutableStateOf(null) }

    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var labelAnalysis by remember { mutableStateOf<ProductLabelIngredientAnalysis?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "label_scan")
    val scanProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanProgress"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        if (capturedBitmap == null) {
            // CameraX Live Preview
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        previewView = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            LaunchedEffect(lensFacing) {
                try {
                    val cameraProvider = context.getCameraProvider()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView?.surfaceProvider)
                    }

                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setFlashMode(flashMode)
                        .build()

                    imageCapture = capture

                    var cameraSelector = CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build()

                    if (!cameraProvider.hasCamera(cameraSelector)) {
                        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    }

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        capture
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // HUD Overlay for Label Framing
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width * 0.85f
                val height = size.height * 0.55f
                val left = (size.width - width) / 2
                val top = (size.height - height) / 2.5f

                drawRect(Color.Black.copy(alpha = 0.5f))

                drawRect(
                    color = Color.Transparent,
                    topLeft = androidx.compose.ui.geometry.Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(width, height),
                    blendMode = androidx.compose.ui.graphics.BlendMode.Clear
                )

                // Reticle Outline
                val strokeWidth = 3.dp.toPx()
                drawRect(
                    color = IndigoPrimary,
                    topLeft = androidx.compose.ui.geometry.Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(width, height),
                    style = Stroke(
                        width = strokeWidth,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(30f, 15f), 0f)
                    )
                )

                // Scanline
                val scanY = top + (height * scanProgress)
                drawLine(
                    color = Color.Green,
                    start = androidx.compose.ui.geometry.Offset(left, scanY),
                    end = androidx.compose.ui.geometry.Offset(left + width, scanY),
                    strokeWidth = 3.dp.toPx()
                )
            }

            // Top Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = {
                            flashMode = if (flashMode == ImageCapture.FLASH_MODE_OFF) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
                            imageCapture?.flashMode = flashMode
                        },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            if (flashMode == ImageCapture.FLASH_MODE_ON) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Flash",
                            tint = if (flashMode == ImageCapture.FLASH_MODE_ON) Color.Yellow else Color.White
                        )
                    }

                    IconButton(
                        onClick = {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                        },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(Icons.Default.Cameraswitch, contentDescription = "Cambiar Cámara", tint = Color.White)
                    }
                }
            }

            // Instruction Banner
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 70.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.DocumentScanner, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(20.dp))
                    Text(
                        "Encuadra la lista de ingredientes del envase",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Shutter Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 36.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = {
                        val capture = imageCapture ?: return@Button
                        val file = File(context.cacheDir, "label_scan_${System.currentTimeMillis()}.jpg")
                        val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

                        capture.takePicture(
                            outputOptions,
                            cameraExecutor,
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                                    val rotatedBitmap = fixImageRotation(bitmap, file.absolutePath)
                                    capturedBitmap = rotatedBitmap

                                    // Automatically trigger label & ingredient analysis
                                    isAnalyzing = true
                                    coroutineScope.launch {
                                        val result = onAnalyzeLabel(rotatedBitmap)
                                        labelAnalysis = result
                                        isAnalyzing = false
                                    }
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    exception.printStackTrace()
                                }
                            }
                        )
                    },
                    modifier = Modifier
                        .size(76.dp)
                        .testTag("camerax_label_shutter_button"),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(IndigoPrimary)
                            .border(3.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.DocumentScanner, contentDescription = "Escanear Etiqueta", tint = Color.White)
                    }
                }
            }
        } else {
            // Analysis & Ingredients View
            val bitmap = capturedBitmap!!
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = IndigoPrimary)
                        Text(
                            "Análisis de Etiqueta e Ingredientes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = {
                        capturedBitmap = null
                        labelAnalysis = null
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                // Thumbnail & Processing State
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Foto de etiqueta",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )

                        if (isAnalyzing) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Text("Procesando OCR & Visión IA...", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                }
                                Text("Analizando texto de la etiqueta para extraer lista de ingredientes...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else if (labelAnalysis != null) {
                            val result = labelAnalysis!!
                            Column {
                                Text(
                                    text = result.productName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = IndigoPrimary
                                )
                                if (result.brand.isNotBlank()) {
                                    Text("Marca: ${result.brand}", style = MaterialTheme.typography.bodySmall)
                                }
                                Text(
                                    text = "Confianza ML/IA: ${(result.confidence * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (labelAnalysis != null) {
                    val result = labelAnalysis!!

                    // Allergens Card (if present)
                    if (result.allergens.isNotEmpty()) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                    Text("Alérgenos Identificados", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                }
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    result.allergens.forEach { allergen ->
                                        SuggestionChip(
                                            onClick = { },
                                            label = { Text(allergen, color = MaterialTheme.colorScheme.error) },
                                            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Ingredients List Card
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.FormatListBulleted, contentDescription = null, tint = IndigoPrimary)
                                Text("Ingredientes Extraídos (${result.ingredients.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                result.ingredients.forEach { ingredient ->
                                    AssistChip(
                                        onClick = { },
                                        label = { Text(ingredient) },
                                        leadingIcon = {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = IndigoPrimary)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Nutritional Summary
                    if (result.nutritionalSummary.isNotBlank()) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = IndigoPrimary.copy(alpha = 0.1f))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = IndigoPrimary)
                                Text(result.nutritionalSummary, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                capturedBitmap = null
                                labelAnalysis = null
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Escanear Otra")
                        }

                        Button(
                            onClick = {
                                onSaveToPantry(result.productName, result.ingredients)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("save_scanned_label_product_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Guardar en Despensa")
                        }
                    }
                }
            }
        }
    }
}

private fun fixImageRotation(bitmap: Bitmap, path: String): Bitmap {
    return try {
        val exif = android.media.ExifInterface(path)
        val orientation = exif.getAttributeInt(
            android.media.ExifInterface.TAG_ORIENTATION,
            android.media.ExifInterface.ORIENTATION_UNDEFINED
        )
        val matrix = Matrix()
        when (orientation) {
            android.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            android.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            android.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> return bitmap
        }
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    } catch (e: Exception) {
        bitmap
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraXBarcodeScannerDialog(
    onDismiss: () -> Unit,
    onBarcodeScanned: (ScannedProduct) -> Unit,
    onFetchProductByBarcode: suspend (String) -> ScannedProduct
) {
    var hasPermission by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(Unit) {
        hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            if (hasPermission) {
                CameraXBarcodeScanView(
                    onDismiss = onDismiss,
                    onBarcodeScanned = onBarcodeScanned,
                    onFetchProductByBarcode = onFetchProductByBarcode
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
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
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(48.dp), tint = IndigoPrimary)
                            Text("Escáner de Código de Barras ML Kit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Para escanear códigos de barras de productos rápidamente con ML Kit y CameraX, concede permiso a la cámara.", style = MaterialTheme.typography.bodyMedium)
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                                modifier = Modifier.testTag("request_camera_barcode_permission_button")
                            ) {
                                Text("Conceder Permiso")
                            }
                            TextButton(onClick = onDismiss) {
                                Text("Cancelar")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CameraXBarcodeScanView(
    onDismiss: () -> Unit,
    onBarcodeScanned: (ScannedProduct) -> Unit,
    onFetchProductByBarcode: suspend (String) -> ScannedProduct
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    var detectedBarcode by remember { mutableStateOf<String?>(null) }
    var scannedProduct by remember { mutableStateOf<ScannedProduct?>(null) }
    var isLoadingProduct by remember { mutableStateOf(false) }
    var previewView: PreviewView? by remember { mutableStateOf(null) }

    val mlProcessor = remember { MLProductProcessor() }

    val infiniteTransition = rememberInfiniteTransition(label = "barcodeScanline")
    val scanlineOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanlineOffset"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    previewView = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Setup CameraX with ML Kit
        LaunchedEffect(Unit) {
            try {
                val cameraProvider = context.getCameraProvider()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView?.surfaceProvider)
                }

                val analyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .build().also { imageAnalysis ->
                        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            if (detectedBarcode == null && !isLoadingProduct) {
                                mlProcessor.analyzeFrame(imageProxy) { result ->
                                    val code = result.barcodeOrOcrText
                                    if (!code.isNullOrBlank() && detectedBarcode == null) {
                                        detectedBarcode = code
                                        isLoadingProduct = true
                                        coroutineScope.launch {
                                            try {
                                                val product = onFetchProductByBarcode(code)
                                                scannedProduct = product
                                            } catch (e: Exception) {
                                                scannedProduct = ScannedProduct(
                                                    name = "Producto $code",
                                                    category = "Alacena",
                                                    foodCategory = "Otros",
                                                    estimatedPrice = 1.99,
                                                    isPromotion = false,
                                                    supermarket = "Mercadona",
                                                    conservationTip = "Conservar en lugar fresco y seco."
                                                )
                                            } finally {
                                                isLoadingProduct = false
                                            }
                                        }
                                    }
                                }
                            } else {
                                imageProxy.close()
                            }
                        }
                    }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    analyzer
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Overlay & Reticle
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val boxWidth = width * 0.8f
            val boxHeight = width * 0.5f
            val left = (width - boxWidth) / 2f
            val top = (height - boxHeight) / 2.5f

            // Semi-transparent dimming outside target
            drawRect(
                color = Color.Black.copy(alpha = 0.55f),
                size = size
            )

            // Clear center target area
            drawRect(
                color = Color.Transparent,
                topLeft = androidx.compose.ui.geometry.Offset(left, top),
                size = androidx.compose.ui.geometry.Size(boxWidth, boxHeight),
                blendMode = androidx.compose.ui.graphics.BlendMode.Clear
            )

            // Target Border
            drawRect(
                color = if (detectedBarcode != null) Color.Green else IndigoPrimary,
                topLeft = androidx.compose.ui.geometry.Offset(left, top),
                size = androidx.compose.ui.geometry.Size(boxWidth, boxHeight),
                style = Stroke(width = 4.dp.toPx())
            )

            // Animated Laser Line
            val lineY = top + (boxHeight * scanlineOffset)
            drawLine(
                color = if (detectedBarcode != null) Color.Green else Color.Red,
                start = androidx.compose.ui.geometry.Offset(left + 8.dp.toPx(), lineY),
                end = androidx.compose.ui.geometry.Offset(left + boxWidth - 8.dp.toPx(), lineY),
                strokeWidth = 3.dp.toPx()
            )
        }

        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onDismiss,
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.6f))
            ) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.7f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(18.dp))
                    Text(
                        text = if (detectedBarcode != null) "¡Código Detectado!" else "ML Kit Escáner Activo",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Bottom Banner / Card for Results
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isLoadingProduct) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = IndigoPrimary)
                        Text(
                            "Buscando producto en base de datos ($detectedBarcode)...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else if (scannedProduct != null) {
                val product = scannedProduct!!
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = product.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            SuggestionChip(
                                onClick = {},
                                label = { Text(product.category) },
                                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = IndigoPrimary.copy(alpha = 0.1f))
                            )
                        }

                        Text(
                            text = "Categoría: ${product.foodCategory} | Supermercado: ${product.supermarket}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Precio Est.: ${String.format(Locale.US, "%.2f", product.estimatedPrice)} €",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = IndigoPrimary
                            )
                            if (product.conservationTip.isNotBlank()) {
                                Text(
                                    text = "💡 ${product.conservationTip}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    detectedBarcode = null
                                    scannedProduct = null
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Escanear Otro")
                            }

                            Button(
                                onClick = {
                                    onBarcodeScanned(product)
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.weight(1.3f).testTag("add_mlkit_scanned_product_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Añadir a Despensa")
                            }
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Apunta la cámara al código de barras (EAN-13, EAN-8, UPC, QR) para escaneo automático.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        modifier = Modifier.padding(14.dp),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

