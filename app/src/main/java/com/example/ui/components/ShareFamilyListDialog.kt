package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.ShoppingListItem
import com.example.ui.theme.IndigoPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareFamilyListDialog(
    shoppingList: List<ShoppingListItem>,
    totalPrice: Double,
    onDismiss: () -> Unit,
    onJoinFamilyList: (inviteCode: String) -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Generar Invitación, 1: Unirse con Código
    var inputInviteCode by remember { mutableStateOf("") }
    var familyCode by remember { mutableStateOf("FAM-${(1000..9999).random()}") }
    val inviteLink = "https://alacenainteligente.app/join?code=$familyCode"

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val totalItems = shoppingList.sumOf { it.quantityToBuy.toInt().coerceAtLeast(1) }

    val formattedShareText = remember(shoppingList, familyCode, totalPrice) {
        val itemsStr = shoppingList.take(8).joinToString("\n") {
            "  • ${it.name} (${if (it.quantityToBuy % 1.0 == 0.0) it.quantityToBuy.toInt() else it.quantityToBuy} ${it.unit}) - ${it.supermarket}"
        }
        val moreText = if (shoppingList.size > 8) "\n  ... y ${shoppingList.size - 8} productos más." else ""

        """
        🛒 *Lista de la Compra Familiar - Alacena Inteligente*
        
        Te han invitado a colaborar en nuestra lista de compras.
        
        📋 *Resumen de productos ($totalItems uds, ~${String.format("%.2f", totalPrice)}€):*
        $itemsStr$moreText
        
        🔗 *Únete en un clic desde la app o navegador:*
        $inviteLink
        
        🔑 *Código de invitación:* $familyCode
        """.trimIndent()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
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
                                .size(44.dp)
                                .background(IndigoPrimary.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Group,
                                contentDescription = null,
                                tint = IndigoPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Column {
                            Text(
                                "Lista de Compra Familiar",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Sincroniza y comparte con tu familia",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_family_share_dialog")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tab selector
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        indicator = {},
                        divider = {}
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Compartir Enlace", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                            modifier = Modifier.testTag("tab_share_link")
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Unirse con Código", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                            modifier = Modifier.testTag("tab_join_code")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTab == 0) {
                    // TAB 0: SHARE ENLACE & INVITATION CODE
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Card with Invite Link & Code
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "Enlace de Invitación Familiar",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                SelectionContainer {
                                    Text(
                                        inviteLink,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = IndigoPrimary,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, IndigoPrimary)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.VpnKey, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(16.dp))
                                        Text(
                                            "Código: $familyCode",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = IndigoPrimary
                                        )
                                    }
                                }
                            }
                        }

                        // Preview of items included
                        Text(
                            "Vista previa de la lista a compartir (${shoppingList.size} productos):",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 140.dp)
                        ) {
                            if (shoppingList.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "La lista de compras está vacía actualmente. Se compartirán los productos cuando los añadas.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(shoppingList) { item ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "• ${item.name}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                "${if (item.quantityToBuy % 1.0 == 0.0) item.quantityToBuy.toInt() else item.quantityToBuy} ${item.unit} | ${item.supermarket}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Action Buttons: Copy Link & Send Intent
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(formattedShareText))
                                    Toast.makeText(context, "Enlace e invitación copiados al portapapeles", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("copy_family_invite_button")
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copiar Enlace")
                            }

                            Button(
                                onClick = {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, formattedShareText)
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Compartir lista familiar")
                                    context.startActivity(shareIntent)
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("send_family_invite_button")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Enviar Invitación")
                            }
                        }
                    }
                } else {
                    // TAB 1: JOIN WITH INVITATION CODE / LINK
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            "Ingresa el código o enlace recibido para vincular tu app con la lista de la compra de tu familiar:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = inputInviteCode,
                            onValueChange = { inputInviteCode = it },
                            placeholder = { Text("Ej. FAM-7892 o enlace completo...") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null, tint = IndigoPrimary) },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_invite_code_field")
                        )

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = IndigoPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    "Al unirte, tu lista se sincronizará automáticamente con las altas y bajas que realicen los miembros de tu grupo familiar.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                val cleanCode = inputInviteCode.trim()
                                if (cleanCode.isNotBlank()) {
                                    onJoinFamilyList(cleanCode)
                                    Toast.makeText(context, "¡Conectado exitosamente a la lista familiar ($cleanCode)!", Toast.LENGTH_LONG).show()
                                    onDismiss()
                                } else {
                                    Toast.makeText(context, "Por favor introduce un código o enlace válido", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = inputInviteCode.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("submit_join_family_list_button")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Unirse a Lista Familiar")
                        }
                    }
                }
            }
        }
    }
}
