package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.remote.RecipeSuggestion
import com.example.ui.theme.IndigoPrimary
import com.example.ui.viewmodel.PantryViewModel

@Composable
fun RecipeSuggestionsScreen(
    viewModel: PantryViewModel,
    modifier: Modifier = Modifier
) {
    val recipes by viewModel.recipeSuggestions.collectAsState()
    val isGenerating by viewModel.isGeneratingRecipes.collectAsState()
    val expiringItems by viewModel.expiringItems.collectAsState()
    val pantryItems by viewModel.pantryItems.collectAsState()
    val recipeActionMessage by viewModel.recipeActionMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(recipeActionMessage) {
        recipeActionMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearRecipeActionMessage()
        }
    }

    LaunchedEffect(Unit) {
        if (recipes.isEmpty()) {
            viewModel.generateRecipes()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.85f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.9f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Recetas con Gemini AI",
                            style = MaterialTheme.typography.titleMedium,
                            color = IndigoPrimary
                        )
                        Text(
                            text = "Basado en ${pantryItems.size} producto(s) en tu alacena Room (${expiringItems.size} próximos a vencer).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (expiringItems.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.triggerProactiveIngredientNotification() },
                                modifier = Modifier.testTag("notify_ingredients_fab")
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = "Notificar Ingredientes", tint = IndigoPrimary)
                            }
                        }

                        Button(
                            onClick = { viewModel.generateRecipes() },
                            enabled = !isGenerating,
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                            modifier = Modifier.testTag("generate_recipes_button")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isGenerating) "Generando..." else "Sugerir Recetas")
                        }
                    }
                }
            }

            if (isGenerating) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(color = IndigoPrimary)
                        Text("Gemini AI analizando productos de la base de datos Room...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else if (recipes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Presiona 'Sugerir Recetas' para generar ideas con los productos de tu alacena.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(recipes) { recipe ->
                        RecipeCard(
                            recipe = recipe,
                            onAddMissingIngredients = {
                                viewModel.addRecipeIngredientsToShoppingList(recipe.additionalIngredients)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecipeCard(
    recipe: RecipeSuggestion,
    onAddMissingIngredients: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.82f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.9f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = recipe.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = IndigoPrimary,
                    modifier = Modifier.weight(1f)
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp), tint = IndigoPrimary)
                    Text("${recipe.prepTimeMinutes} min", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(recipe.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (recipe.ingredientsUsed.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text("Ingredientes de tu alacena (Room DB):", style = MaterialTheme.typography.labelSmall)
                Text(recipe.ingredientsUsed.joinToString(", "), style = MaterialTheme.typography.bodySmall, color = IndigoPrimary)
            }

            if (recipe.additionalIngredients.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text("Ingredientes adicionales / faltantes:", style = MaterialTheme.typography.labelSmall)
                Text(recipe.additionalIngredients.joinToString(", "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(
                    onClick = onAddMissingIngredients,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = IndigoPrimary),
                    modifier = Modifier.testTag("add_missing_recipe_ingredients_button")
                ) {
                    Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Añadir faltantes a Lista de Compras", style = MaterialTheme.typography.labelMedium)
                }
            }

            if (recipe.steps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Pasos de Preparación:", style = MaterialTheme.typography.labelSmall)
                recipe.steps.forEachIndexed { idx, step ->
                    Text("${idx + 1}. $step", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
