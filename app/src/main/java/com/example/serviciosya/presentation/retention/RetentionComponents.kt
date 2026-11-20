package com.example.serviciosya.presentation.retention

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.serviciosya.domain.model.Provider
import com.example.serviciosya.domain.retention.Achievement
import com.example.serviciosya.presentation.components.InitialsAvatar
import com.example.serviciosya.presentation.components.formatRating

/**
 * Asks for the notification permission on Android 13+; on older versions notifications are allowed
 * by default, so it answers right away with the system setting.
 */
@Composable
fun rememberNotificationPermissionRequest(onResult: (Boolean) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), onResult)
    return remember(launcher, onResult) {
        {
            val needsRuntimePermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            if (needsRuntimePermission) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                onResult(NotificationManagerCompat.from(context).areNotificationsEnabled())
            }
        }
    }
}

@Composable
fun NotificationPromptCard(onEnable: () -> Unit, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.NotificationsActive, contentDescription = null)
                Text(
                    text = "¿Te avisamos si el prestador no te responde y cuando haya novedades en tus categorías?",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Ahora no") }
                TextButton(onClick = onEnable) { Text("Activar") }
            }
        }
    }
}

/** "Para vos": recently viewed and favorite providers, one horizontal row each. */
@Composable
fun ForYouSection(
    recent: List<Provider>,
    favorites: List<Provider>,
    categoryNames: Map<String, String>,
    onProviderClick: (providerId: String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Para vos", style = MaterialTheme.typography.titleLarge)
        if (favorites.isNotEmpty()) {
            ProviderRow("Tus favoritos", favorites, categoryNames, onProviderClick)
        }
        if (recent.isNotEmpty()) {
            ProviderRow("Vistos recientemente", recent, categoryNames, onProviderClick)
        }
    }
}

@Composable
private fun ProviderRow(
    title: String,
    providers: List<Provider>,
    categoryNames: Map<String, String>,
    onProviderClick: (String) -> Unit,
) {
    Text(text = title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 2.dp)) {
        items(providers, key = { "$title-${it.id}" }) { provider ->
            Card(modifier = Modifier.width(150.dp).clickable { onProviderClick(provider.id) }) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    InitialsAvatar(name = provider.name, size = 36.dp)
                    Text(
                        text = provider.name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = listOfNotNull(categoryNames[provider.categoryId], formatRating(provider.rating))
                            .joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationsSwitchRow(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.NotificationsActive, contentDescription = null)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Notificaciones", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Recordatorios y novedades de tus categorías",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
fun AchievementsSection(achievements: List<Achievement>) {
    val unlocked = achievements.count { it.unlocked }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Logros", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "$unlocked de ${achievements.size} desbloqueados",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LinearProgressIndicator(
            progress = { if (achievements.isEmpty()) 0f else unlocked.toFloat() / achievements.size },
            modifier = Modifier.fillMaxWidth(),
        )
        achievements.forEach { achievement ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (achievement.unlocked) Icons.Outlined.EmojiEvents else Icons.Outlined.Lock,
                    contentDescription = if (achievement.unlocked) "Desbloqueado" else "Bloqueado",
                    tint = if (achievement.unlocked) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Column {
                    Text(text = achievement.title, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = achievement.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
