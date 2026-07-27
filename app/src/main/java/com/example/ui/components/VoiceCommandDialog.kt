package com.example.ui.components

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun VoiceCommandDialog(
    onDismiss: () -> Unit,
    onCommandRecognized: (String) -> Unit
) {
    val context = LocalContext.current
    var textInput by remember { mutableStateOf("") }
    var recognizedText by remember { mutableStateOf("") }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull() ?: ""
            if (spokenText.isNotBlank()) {
                recognizedText = spokenText
                onCommandRecognized(spokenText)
            }
        }
    }

    fun startSpeechRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Di tu comando para la despensa o lista de compras...")
        }
        try {
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    LaunchedEffect(Unit) {
        startSpeechRecognition()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Comandos de Voz Multi-Asistente")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Assistant Badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AssistChip(
                        onClick = { textInput = "Ok Google, añade 2 L de leche a la lista" },
                        label = { Text("Ok Google", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = { Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                    AssistChip(
                        onClick = { textInput = "Gemini, elimina pan de la alacena" },
                        label = { Text("Gemini", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = { Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                    AssistChip(
                        onClick = { textInput = "Alexa, pon 1 kg de arroz en la lista" },
                        label = { Text("Alexa", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = { Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                }

                Text(
                    "Habla usando tu asistente preferido (Ok Google, Gemini o Alexa) para añadir o eliminar productos:",
                    style = MaterialTheme.typography.bodySmall
                )
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("➕ AÑADIR/COMPRAR:", style = MaterialTheme.typography.labelSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("• 'Ok Google, añade 2 litros de leche a la lista de compras'", style = MaterialTheme.typography.bodySmall)
                        Text("• 'Alexa, pon 1 paquete de arroz en la alacena'", style = MaterialTheme.typography.bodySmall)
                        Text("• 'Gemini, agregar 3 kilos de manzanas a la nevera'", style = MaterialTheme.typography.bodySmall)

                        Spacer(modifier = Modifier.height(4.dp))

                        Text("➖ ELIMINAR/BORRAR:", style = MaterialTheme.typography.labelSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFFC62828))
                        Text("• 'Ok Google, borra la leche de la lista de compras'", style = MaterialTheme.typography.bodySmall)
                        Text("• 'Gemini, elimina pan de la alacena'", style = MaterialTheme.typography.bodySmall)
                        Text("• 'Alexa, quita huevos de la lista'", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Button(
                    onClick = { startSpeechRecognition() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("voice_dialog_mic_button")
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Presionar para Hablar")
                }

                if (recognizedText.isNotBlank()) {
                    Text(
                        "Reconocido: '$recognizedText'",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Divider()

                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    label = { Text("O escribe tu comando aquí...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("voice_dialog_text_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (textInput.isNotBlank()) {
                        onCommandRecognized(textInput)
                        onDismiss()
                    } else if (recognizedText.isNotBlank()) {
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("voice_dialog_confirm_button")
            ) {
                Text("Ejecutar")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("voice_dialog_cancel_button")
            ) {
                Text("Cerrar")
            }
        }
    )
}

