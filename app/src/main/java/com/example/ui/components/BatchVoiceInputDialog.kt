package com.example.ui.components

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.PantryItem
import com.example.ui.theme.BlueFridge
import com.example.ui.theme.IndigoPrimary
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchVoiceInputDialog(
    onDismiss: () -> Unit,
    onSaveBatch: (List<PantryItem>) -> Unit
) {
    val context = LocalContext.current
    val pendingItems = remember { mutableStateListOf<PantryItem>() }
    var manualInputText by remember { mutableStateOf("") }
    var lastRecognizedText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }

    fun parseSpeechToItems(rawSpeech: String): List<PantryItem> {
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 10) }
        val defaultExp = cal.timeInMillis

        // Split by separators like commas, " y ", " e ", newlines, or semicolons
        val chunks = rawSpeech
            .split(Regex("(?i)(,|\\n|;|\\s+y\\s+|\\s+e\\s+|\\s+luego\\s+|\\s+más\\s+)"))
            .map { it.trim() }
            .filter { it.isNotBlank() && it.length > 1 }

        val newItems = mutableListOf<PantryItem>()

        for (chunk in chunks) {
            val lowerChunk = chunk.lowercase(Locale.getDefault())

            // Quantity extraction
            var qty = 1.0
            val qtyMatch = Regex("(\\d+(?:[.,]\\d+)?)").find(chunk)
            if (qtyMatch != null) {
                qty = qtyMatch.value.replace(',', '.').toDoubleOrNull() ?: 1.0
            }

            // Unit extraction
            var unit = "ud"
            val unitMatch = Regex("(?i)(litros?|l|kilos?|kg|gramos?|g|paquetes?|paq|latas?|unidades?|ud|botes?|cajas?|cartones?|docenas?)").find(chunk)
            if (unitMatch != null) {
                val u = unitMatch.value.lowercase(Locale.getDefault())
                unit = when {
                    u.startsWith("litr") || u == "l" -> "L"
                    u.startsWith("kilo") || u == "kg" -> "kg"
                    u.startsWith("gram") || u == "g" -> "g"
                    u.startsWith("paquet") || u == "paq" -> "paq"
                    u.startsWith("lata") -> "lata"
                    u.startsWith("bote") -> "bote"
                    u.startsWith("caja") -> "caja"
                    u.startsWith("carton") || u.startsWith("cartón") -> "cartón"
                    u.startsWith("docen") -> "docena"
                    else -> "ud"
                }
            }

            // Location extraction
            val location = if (lowerChunk.contains("nevera") || lowerChunk.contains("frigorifico") || lowerChunk.contains("frigorífico") || lowerChunk.contains("congelador")) {
                "NEVERA"
            } else {
                "ALACENA"
            }

            // Clean name
            val cleanName = chunk.replace(Regex("(?i)(añadir|agregar|comprar|pon|pone|meter|meter a|a la alacena|en la alacena|a la nevera|en la nevera|en el congelador|a la despensa|en la despensa|de compras|\\d+(?:[.,]\\d+)?|litros?|kilos?|kg|gramos?|g|paquetes?|latas?|unidades?|ud|botes?|cajas?|cartones?|docenas?|l)"), "")
                .trim()
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

            if (cleanName.length >= 2) {
                // Determine broad food category
                val foodCategory = when {
                    lowerChunk.contains("leche") || lowerChunk.contains("queso") || lowerChunk.contains("yogur") || lowerChunk.contains("mantequilla") -> "Lácteos"
                    lowerChunk.contains("carne") || lowerChunk.contains("pollo") || lowerChunk.contains("pescado") || lowerChunk.contains("atún") || lowerChunk.contains("jamón") -> "Carnes y Pescados"
                    lowerChunk.contains("manzana") || lowerChunk.contains("plátano") || lowerChunk.contains("tomate") || lowerChunk.contains("lechuga") || lowerChunk.contains("fruta") || lowerChunk.contains("verdura") -> "Frutas y Verduras"
                    lowerChunk.contains("arroz") || lowerChunk.contains("pasta") || lowerChunk.contains("pan") || lowerChunk.contains("cereal") || lowerChunk.contains("harina") -> "Granos y Cereales"
                    lowerChunk.contains("agua") || lowerChunk.contains("zumo") || lowerChunk.contains("cerveza") || lowerChunk.contains("refresco") -> "Bebidas"
                    else -> "Otros"
                }

                newItems.add(
                    PantryItem(
                        name = cleanName,
                        quantity = qty,
                        unit = unit,
                        locationCategory = location,
                        foodCategory = foodCategory,
                        expirationDateMillis = defaultExp
                    )
                )
            }
        }
        return newItems
    }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenMatches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = spokenMatches?.firstOrNull() ?: ""
            if (spokenText.isNotBlank()) {
                lastRecognizedText = spokenText
                val parsed = parseSpeechToItems(spokenText)
                pendingItems.addAll(parsed)
            }
        }
    }

    fun startListening() {
        isListening = true
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Dicta tus productos (ej. 2 litros de leche, 1 kilo de plátanos en la nevera)...")
        }
        try {
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            isListening = false
            e.printStackTrace()
        }
    }

    LaunchedEffect(Unit) {
        startListening()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(IndigoPrimary.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.RecordVoiceOver,
                                    contentDescription = null,
                                    tint = IndigoPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    "Carga Rápida por Voz",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Dicta múltiples productos en sesión",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (pendingItems.isNotEmpty()) {
                            Badge(
                                containerColor = IndigoPrimary,
                                contentColor = Color.White
                            ) {
                                Text(
                                    "${pendingItems.size} en lote",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Instruction Banner / Mic button
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Ejemplo de dictado:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "\"2 litros de leche, 1 kilo de plátanos en la nevera, 6 huevos y 3 latas de atún\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { startListening() },
                                modifier = Modifier
                                    .background(IndigoPrimary, CircleShape)
                                    .testTag("batch_voice_mic_button")
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = "Dictar más productos", tint = Color.White)
                            }
                        }
                    }

                    if (lastRecognizedText.isNotBlank()) {
                        Text(
                            text = "Último dictado: \"$lastRecognizedText\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Product list
                    if (pendingItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.MicNone,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Pulsa el micrófono para dictar tus productos",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(pendingItems) { index, item ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = item.name,
                                                onValueChange = { newName ->
                                                    pendingItems[index] = item.copy(name = newName)
                                                },
                                                modifier = Modifier.weight(1f),
                                                label = { Text("Producto") },
                                                singleLine = true
                                            )

                                            IconButton(
                                                onClick = { pendingItems.removeAt(index) },
                                                modifier = Modifier.testTag("remove_batch_item_$index")
                                            ) {
                                                Icon(
                                                    Icons.Default.DeleteOutline,
                                                    contentDescription = "Eliminar del lote",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Quantity controls
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                FilledTonalIconButton(
                                                    onClick = {
                                                        if (item.quantity > 0.5) {
                                                            pendingItems[index] = item.copy(quantity = item.quantity - 0.5)
                                                        }
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.Remove, contentDescription = "Restar", modifier = Modifier.size(16.dp))
                                                }
                                                Text(
                                                    "${item.quantity} ${item.unit}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp)
                                                )
                                                FilledTonalIconButton(
                                                    onClick = {
                                                        pendingItems[index] = item.copy(quantity = item.quantity + 0.5)
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.Add, contentDescription = "Sumar", modifier = Modifier.size(16.dp))
                                                }
                                            }

                                            // Location Chip Toggle
                                            FilterChip(
                                                selected = item.locationCategory == "NEVERA",
                                                onClick = {
                                                    val nextLoc = if (item.locationCategory == "NEVERA") "ALACENA" else "NEVERA"
                                                    pendingItems[index] = item.copy(locationCategory = nextLoc)
                                                },
                                                label = {
                                                    Text(if (item.locationCategory == "NEVERA") "❄️ Nevera" else "📦 Alacena")
                                                },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = BlueFridge,
                                                    selectedLabelColor = Color.White
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Manual Addition row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = manualInputText,
                            onValueChange = { manualInputText = it },
                            placeholder = { Text("Añadir otro producto a mano...") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("batch_manual_input")
                        )
                        IconButton(
                            onClick = {
                                if (manualInputText.isNotBlank()) {
                                    val parsed = parseSpeechToItems(manualInputText)
                                    if (parsed.isNotEmpty()) {
                                        pendingItems.addAll(parsed)
                                    } else {
                                        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 10) }
                                        pendingItems.add(
                                            PantryItem(
                                                name = manualInputText.trim(),
                                                quantity = 1.0,
                                                unit = "ud",
                                                locationCategory = "ALACENA",
                                                foodCategory = "Otros",
                                                expirationDateMillis = cal.timeInMillis
                                            )
                                        )
                                    }
                                    manualInputText = ""
                                }
                            },
                            modifier = Modifier
                                .background(IndigoPrimary, RoundedCornerShape(12.dp))
                                .testTag("batch_add_manual_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Agregar producto", tint = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = {
                            if (pendingItems.isNotEmpty()) {
                                onSaveBatch(pendingItems.toList())
                                onDismiss()
                            }
                        },
                        enabled = pendingItems.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.testTag("save_batch_voice_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Guardar ${pendingItems.size} en Alacena")
                    }
                }
            }
        }
    }
}
