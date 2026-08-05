package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.IndigoPrimary
import com.example.ui.viewmodel.PantryViewModel
import com.example.util.DriveTokenResult
import com.example.util.GoogleDriveAuthManager
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.launch

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class DrivePendingAction { BACKUP, RESTORE }

/**
 * Pide un token OAuth fresco con permiso de Drive para [account] y ejecuta la acción
 * pendiente. Si el permiso aún no está concedido, invoca [onNeedsConsent] con el intent
 * de consentimiento en vez de fallar; si la obtención del token falla por completo, recurre
 * al respaldo/restauración local (mismo comportamiento que sin cuenta configurada).
 */
private suspend fun performDriveActionWithAuth(
    action: DrivePendingAction,
    account: GoogleSignInAccount,
    authManager: GoogleDriveAuthManager,
    viewModel: PantryViewModel,
    onNeedsConsent: (Intent) -> Unit
) {
    when (val tokenResult = authManager.fetchAccessToken(account)) {
        is DriveTokenResult.Success -> when (action) {
            DrivePendingAction.BACKUP -> viewModel.performGoogleDriveBackup(tokenResult.accessToken)
            DrivePendingAction.RESTORE -> viewModel.restoreFromGoogleDriveBackup(tokenResult.accessToken)
        }
        is DriveTokenResult.RecoverableError -> onNeedsConsent(tokenResult.recoveryIntent)
        is DriveTokenResult.Failure -> when (action) {
            DrivePendingAction.BACKUP -> viewModel.performGoogleDriveBackup("")
            DrivePendingAction.RESTORE -> viewModel.restoreFromGoogleDriveBackup("")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: PantryViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.appSettings.collectAsState()

    var warningDays by remember(settings) { mutableFloatStateOf(settings.expirationWarningDays.toFloat()) }
    var language by remember(settings) { mutableStateOf(settings.language) }
    var isDarkMode by remember(settings) { mutableStateOf(settings.isDarkMode) }
    var alexaApiKey by remember(settings) { mutableStateOf(settings.alexaApiKey) }
    var alexaUserId by remember(settings) { mutableStateOf(settings.alexaUserId) }
    var isAlexaSyncEnabled by remember(settings) { mutableStateOf(settings.isAlexaSyncEnabled) }
    var autoAddShopping by remember(settings) { mutableStateOf(settings.autoAddToShoppingList) }
    var isDriveAutoBackup by remember(settings) { mutableStateOf(settings.isGoogleDriveAutoBackupEnabled) }
    var monthlyBudgetInput by remember(settings) {
        mutableStateOf(if (settings.monthlyBudget > 0) String.format(Locale.US, "%.2f", settings.monthlyBudget) else "")
    }

    val isDriveBackingUp by viewModel.isDriveBackingUp.collectAsState()
    val driveBackupStatusMessage by viewModel.driveBackupStatusMessage.collectAsState()
    val isRefreshingMercadonaCatalog by viewModel.isRefreshingMercadonaCatalog.collectAsState()
    val mercadonaCatalogStatus by viewModel.mercadonaCatalogStatus.collectAsState()

    // Inicio de sesión con Google para el respaldo en Drive
    val context = LocalContext.current
    val authManager = remember { GoogleDriveAuthManager(context) }
    val coroutineScope = rememberCoroutineScope()

    var googleAccount by remember { mutableStateOf(authManager.getLastSignedInAccount()) }
    var pendingDriveAction by remember { mutableStateOf<DrivePendingAction?>(null) }

    val consentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val action = pendingDriveAction
        val account = googleAccount
        pendingDriveAction = null
        if (result.resultCode == Activity.RESULT_OK && action != null && account != null) {
            coroutineScope.launch {
                performDriveActionWithAuth(action, account, authManager, viewModel) { /* ya se acaba de conceder */ }
            }
        }
    }

    val signInLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val account = authManager.extractAccountFromResult(result.data)
        if (account != null) {
            googleAccount = account
            viewModel.updateSettings(settings.copy(driveAccountEmail = account.email ?: ""))
            val action = pendingDriveAction
            pendingDriveAction = null
            if (action != null) {
                coroutineScope.launch {
                    performDriveActionWithAuth(action, account, authManager, viewModel) { intent ->
                        pendingDriveAction = action
                        consentLauncher.launch(intent)
                    }
                }
            }
        }
    }

    fun startDriveAction(action: DrivePendingAction) {
        val account = googleAccount
        if (account == null) {
            pendingDriveAction = action
            signInLauncher.launch(authManager.getSignInIntent())
        } else {
            coroutineScope.launch {
                performDriveActionWithAuth(action, account, authManager, viewModel) { intent ->
                    pendingDriveAction = action
                    consentLauncher.launch(intent)
                }
            }
        }
    }

    val glassCardColors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.82f))
    val glassBorder = BorderStroke(1.dp, Color.White.copy(alpha = 0.9f))
    val glassShape = RoundedCornerShape(22.dp)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Configuración de la Aplicación", style = MaterialTheme.typography.headlineSmall, color = IndigoPrimary)

        // Tema Claro / Oscuro
        Card(
            shape = glassShape,
            colors = glassCardColors,
            border = glassBorder,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode, contentDescription = null, tint = IndigoPrimary)
                        Text("Modo Oscuro / Tema Nocturno", style = MaterialTheme.typography.titleMedium)
                    }
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = {
                            isDarkMode = it
                            viewModel.updateSettings(settings.copy(isDarkMode = it))
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = IndigoPrimary),
                        modifier = Modifier.testTag("dark_mode_switch")
                    )
                }
            }
        }

        // Idioma
        Card(
            shape = glassShape,
            colors = glassCardColors,
            border = glassBorder,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Language, contentDescription = null, tint = IndigoPrimary)
                    Text("Idioma / Language", style = MaterialTheme.typography.titleMedium)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = language == "es",
                        onClick = {
                            language = "es"
                            viewModel.updateSettings(settings.copy(language = "es"))
                        },
                        label = { Text("Español") }
                    )
                    FilterChip(
                        selected = language == "en",
                        onClick = {
                            language = "en"
                            viewModel.updateSettings(settings.copy(language = "en"))
                        },
                        label = { Text("English") }
                    )
                }
            }
        }

        // Alerta de Caducidad (Días)
        Card(
            shape = glassShape,
            colors = glassCardColors,
            border = glassBorder,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = IndigoPrimary)
                    Text("Tiempo de Aviso de Caducidad", style = MaterialTheme.typography.titleMedium)
                }
                Text("Notificar cuando falten ${warningDays.toInt()} días para vencer:", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = warningDays,
                    onValueChange = { warningDays = it },
                    onValueChangeFinished = {
                        viewModel.updateSettings(settings.copy(expirationWarningDays = warningDays.toInt()))
                    },
                    colors = SliderDefaults.colors(thumbColor = IndigoPrimary, activeTrackColor = IndigoPrimary),
                    valueRange = 1f..14f,
                    steps = 12,
                    modifier = Modifier.testTag("expiration_days_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.triggerWorkManagerExpirationCheck() },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("test_workmanager_check_button")
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Verificar WorkManager")
                    }

                    OutlinedButton(
                        onClick = { viewModel.triggerProactiveIngredientNotification() },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("test_proactive_notification_button")
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Alerta Proactiva")
                    }
                }

            }
        }

        // Presupuesto Mensual de Gastos
        val history by viewModel.purchaseHistory.collectAsState()
        val currentMonthSpent = remember(history) {
            val cal = java.util.Calendar.getInstance()
            val currentMonth = cal.get(java.util.Calendar.MONTH)
            val currentYear = cal.get(java.util.Calendar.YEAR)
            history.filter {
                val itemCal = java.util.Calendar.getInstance().apply { timeInMillis = it.purchaseDateMillis }
                itemCal.get(java.util.Calendar.MONTH) == currentMonth && itemCal.get(java.util.Calendar.YEAR) == currentYear
            }.sumOf { it.price * it.quantity }
        }

        val budgetVal = settings.monthlyBudget
        val budgetPercent = if (budgetVal > 0) (currentMonthSpent / budgetVal).toFloat() else 0f

        Card(
            shape = glassShape,
            colors = glassCardColors,
            border = glassBorder,
            modifier = Modifier.fillMaxWidth().testTag("monthly_budget_settings_card")
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = IndigoPrimary)
                    Text("Presupuesto Mensual de Gastos", style = MaterialTheme.typography.titleMedium)
                }
                Text("Define un límite mensual de compras. Recibirás una notificación cuando tus gastos acumulados alcancen el 80% y 100% de dicho límite.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                OutlinedTextField(
                    value = monthlyBudgetInput,
                    onValueChange = { input ->
                        monthlyBudgetInput = input
                        val newBudget = input.toDoubleOrNull() ?: 0.0
                        viewModel.updateSettings(settings.copy(monthlyBudget = newBudget))
                    },
                    label = { Text("Límite Presupuestario Mensual (€)") },
                    placeholder = { Text("Ej. 250.00") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("monthly_budget_input")
                )

                // Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(100.0, 200.0, 300.0, 500.0).forEach { preset ->
                        FilterChip(
                            selected = settings.monthlyBudget == preset,
                            onClick = {
                                monthlyBudgetInput = String.format(Locale.US, "%.2f", preset)
                                viewModel.updateSettings(settings.copy(monthlyBudget = preset))
                            },
                            label = { Text("${preset.toInt()} €", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                if (budgetVal > 0) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Gasto este mes: ${String.format(Locale.US, "%.2f", currentMonthSpent)} € de ${String.format(Locale.US, "%.2f", budgetVal)} €",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                            )
                            Text(
                                text = "${(budgetPercent * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                color = when {
                                    budgetPercent >= 1.0f -> MaterialTheme.colorScheme.error
                                    budgetPercent >= 0.8f -> Color(0xFFF59E0B)
                                    else -> IndigoPrimary
                                }
                            )
                        }

                        LinearProgressIndicator(
                            progress = { budgetPercent.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = when {
                                budgetPercent >= 1.0f -> MaterialTheme.colorScheme.error
                                budgetPercent >= 0.8f -> Color(0xFFF59E0B)
                                else -> IndigoPrimary
                            },
                            trackColor = IndigoPrimary.copy(alpha = 0.12f),
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    }
                }

                OutlinedButton(
                    onClick = { viewModel.checkAndNotifyMonthlyBudget() },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("test_budget_notification_button")
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Probar Alerta de Presupuesto")
                }
            }
        }

        // Auto añadir a lista de compras
        Card(
            shape = glassShape,
            colors = glassCardColors,
            border = glassBorder,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-añadir a Lista de Compras", style = MaterialTheme.typography.titleMedium)
                        Text("Añadir producto a la lista cuando baje del límite configurado", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = autoAddShopping,
                        onCheckedChange = {
                            autoAddShopping = it
                            viewModel.updateSettings(settings.copy(autoAddToShoppingList = it))
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = IndigoPrimary)
                    )
                }
            }
        }

        // Integración con Alexa
        Card(
            shape = glassShape,
            colors = glassCardColors,
            border = glassBorder,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.GraphicEq, contentDescription = null, tint = IndigoPrimary)
                    Text("Sincronización con Alexa (Lista de Compras)", style = MaterialTheme.typography.titleMedium)
                }
                Text("Configura tus credenciales para sincronizar la lista de compras con Amazon Alexa:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                OutlinedTextField(
                    value = alexaApiKey,
                    onValueChange = { alexaApiKey = it },
                    label = { Text("Alexa API Key / Token") },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color.White.copy(alpha = 0.7f),
                        focusedContainerColor = Color.White.copy(alpha = 0.9f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = alexaUserId,
                    onValueChange = { alexaUserId = it },
                    label = { Text("Alexa User ID / Skill ID") },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color.White.copy(alpha = 0.7f),
                        focusedContainerColor = Color.White.copy(alpha = 0.9f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Habilitar Sincronización Automática", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = isAlexaSyncEnabled,
                        onCheckedChange = { isAlexaSyncEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = IndigoPrimary)
                    )
                }

                val isAlexaSyncing by viewModel.isAlexaSyncing.collectAsState()
                val alexaSyncResult by viewModel.alexaSyncResult.collectAsState()

                var alexaSaveConfirmation by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    OutlinedButton(
                        onClick = {
                            isAlexaSyncEnabled = true
                            val updated = settings.copy(
                                alexaApiKey = alexaApiKey,
                                alexaUserId = alexaUserId,
                                isAlexaSyncEnabled = true
                            )
                            viewModel.updateSettings(updated)
                            viewModel.syncWithAlexa(updated)
                        },
                        enabled = !isAlexaSyncing,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.testTag("alexa_sync_now_button")
                    ) {
                        if (isAlexaSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text("Sincronizar Ahora")
                    }

                    Button(
                        onClick = {
                            val updated = settings.copy(
                                alexaApiKey = alexaApiKey,
                                alexaUserId = alexaUserId,
                                isAlexaSyncEnabled = isAlexaSyncEnabled
                            )
                            viewModel.updateSettings(updated)
                            alexaSaveConfirmation = true
                        },
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        modifier = Modifier.testTag("save_alexa_credentials_button")
                    ) {
                        Text("Guardar Credenciales")
                    }
                }

                if (alexaSaveConfirmation) {
                    Text(
                        text = "✓ Credenciales de Alexa guardadas correctamente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = IndigoPrimary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                alexaSyncResult?.let { result ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = IndigoPrimary.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = result.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = IndigoPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.clearAlexaSyncResult() }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = IndigoPrimary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // Exportación CSV
        Card(
            shape = glassShape,
            colors = glassCardColors,
            border = glassBorder,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, tint = IndigoPrimary)
                    Text("Exportar Datos (Formato CSV)", style = MaterialTheme.typography.titleMedium)
                }
                Text("Descarga tu inventario y tu historial de consumo para gestionarlo externamente en Excel:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.exportInventoryCsv() },
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        modifier = Modifier.weight(1f).testTag("export_inventory_csv_button")
                    ) {
                        Text("Exportar Inventario")
                    }

                    OutlinedButton(
                        onClick = { viewModel.exportHistoryCsv() },
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.weight(1f).testTag("export_history_csv_button")
                    ) {
                        Text("Exportar Historial")
                    }
                }
            }
        }

        // Respaldo Automático en Google Drive
        Card(
            shape = glassShape,
            colors = glassCardColors,
            border = glassBorder,
            modifier = Modifier.fillMaxWidth().testTag("google_drive_backup_card")
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = IndigoPrimary)
                    Text("Respaldo en Google Drive", style = MaterialTheme.typography.titleMedium)
                }

                Text(
                    "Guarda una copia de seguridad de tu inventario, lista de compras e historial en tu cuenta de Google Drive para restaurarla en cualquier dispositivo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.AccountCircle, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(18.dp))
                        Text(
                            text = googleAccount?.email ?: "Sin cuenta de Google conectada",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (googleAccount != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(
                        onClick = { signInLauncher.launch(authManager.getSignInIntent()) },
                        modifier = Modifier.testTag("google_account_sign_in_button")
                    ) {
                        Text(if (googleAccount != null) "Cambiar cuenta" else "Iniciar sesión")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Respaldo Automático de Datos", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = isDriveAutoBackup,
                        onCheckedChange = {
                            isDriveAutoBackup = it
                            viewModel.updateSettings(settings.copy(isGoogleDriveAutoBackupEnabled = it))
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = IndigoPrimary)
                    )
                }

                if (settings.lastDriveBackupTimestamp > 0) {
                    val formattedDate = remember(settings.lastDriveBackupTimestamp) {
                        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(settings.lastDriveBackupTimestamp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(16.dp))
                        Text(
                            "Último respaldo: $formattedDate",
                            style = MaterialTheme.typography.labelMedium,
                            color = IndigoPrimary
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { startDriveAction(DrivePendingAction.BACKUP) },
                        enabled = !isDriveBackingUp,
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        modifier = Modifier.weight(1f).testTag("create_google_drive_backup_button")
                    ) {
                        if (isDriveBackingUp) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                        } else {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text("Crear Copia")
                    }

                    OutlinedButton(
                        onClick = { startDriveAction(DrivePendingAction.RESTORE) },
                        enabled = !isDriveBackingUp,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.weight(1f).testTag("restore_google_drive_backup_button")
                    ) {
                        if (isDriveBackingUp) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                        } else {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text("Restaurar")
                    }
                }

                driveBackupStatusMessage?.let { msg ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = IndigoPrimary.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = IndigoPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.clearDriveBackupStatusMessage() }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = IndigoPrimary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // Precios Reales de Mercadona
        Card(
            shape = glassShape,
            colors = glassCardColors,
            border = glassBorder,
            modifier = Modifier.fillMaxWidth().testTag("mercadona_prices_card")
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.LocalOffer, contentDescription = null, tint = IndigoPrimary)
                    Text("Precios Reales de Mercadona", style = MaterialTheme.typography.titleMedium)
                }

                Text(
                    "Actualiza el precio guardado de cada producto de tu inventario con el precio real de Mercadona, en lugar de la estimación de la IA.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = { viewModel.refreshMercadonaCatalog() },
                    enabled = !isRefreshingMercadonaCatalog,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                    modifier = Modifier.fillMaxWidth().testTag("refresh_mercadona_catalog_button")
                ) {
                    if (isRefreshingMercadonaCatalog) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Actualizando precios...")
                    } else {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Actualizar Precios de Mercadona")
                    }
                }

                mercadonaCatalogStatus?.let { msg ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = IndigoPrimary.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = IndigoPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.clearMercadonaCatalogStatus() }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = IndigoPrimary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
