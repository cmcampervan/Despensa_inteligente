package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.ui.components.FrostedGlassMeshBackground
import com.example.ui.screens.*
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.PantryViewModel
import com.example.worker.WorkScheduler

enum class Screen(val title: String, val icon: ImageVector) {
    INVENTORY("Inventario", Icons.Default.Kitchen),
    SCANNER("Escáner", Icons.Default.QrCodeScanner),
    SHOPPING("Compras", Icons.Default.ShoppingCart),
    RECIPES("Recetas AI", Icons.Default.AutoAwesome),
    HISTORY("Historial", Icons.Default.History),
    SETTINGS("Ajustes", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private val viewModel: PantryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }

        // Schedule WorkManager periodic checks for expiration background alerts
        WorkScheduler.scheduleExpirationCheckWork(applicationContext)

        setContent {
            val settings by viewModel.appSettings.collectAsState()
            var currentScreen by remember { mutableStateOf(Screen.INVENTORY) }

            MyApplicationTheme(darkTheme = settings.isDarkMode) {
                FrostedGlassMeshBackground {
                    Scaffold(
                        containerColor = Color.Transparent,
                        bottomBar = {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ) {
                                Screen.values().forEach { screen ->
                                    val isSelected = currentScreen == screen
                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = { currentScreen = screen },
                                        icon = {
                                            Icon(
                                                screen.icon,
                                                contentDescription = screen.title,
                                                tint = if (isSelected) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        label = {
                                            Text(
                                                screen.title,
                                                color = if (isSelected) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            indicatorColor = IndigoPrimary.copy(alpha = 0.12f)
                                        ),
                                        modifier = Modifier.testTag("nav_tab_${screen.name.lowercase()}")
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        when (currentScreen) {
                            Screen.INVENTORY -> InventoryScreen(
                                viewModel = viewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                            Screen.SCANNER -> BarcodeScannerScreen(
                                viewModel = viewModel,
                                modifier = Modifier.padding(innerPadding),
                                onNavigateToInventory = { currentScreen = Screen.INVENTORY }
                            )
                            Screen.SHOPPING -> ShoppingListScreen(
                                viewModel = viewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                            Screen.RECIPES -> RecipeSuggestionsScreen(
                                viewModel = viewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                            Screen.HISTORY -> PurchaseHistoryScreen(
                                viewModel = viewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                            Screen.SETTINGS -> SettingsScreen(
                                viewModel = viewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }
}
