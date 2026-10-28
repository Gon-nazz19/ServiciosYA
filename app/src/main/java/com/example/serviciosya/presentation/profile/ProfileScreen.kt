package com.example.serviciosya.presentation.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.serviciosya.domain.model.User
import com.example.serviciosya.presentation.billing.ProfilePlansUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    user: User,
    isLoading: Boolean,
    errorMessage: String?,
    onSignOut: () -> Unit,
    plans: ProfilePlansUiState = ProfilePlansUiState(),
    onAdFree: () -> Unit = {},
    onMyPlan: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perfil") },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = user.name, style = MaterialTheme.typography.headlineSmall)
            Text(
                text = user.email,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            errorMessage?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }
            Text(text = "Planes", style = MaterialTheme.typography.titleMedium)
            PlanEntry(
                icon = Icons.Outlined.VisibilityOff,
                title = "Sin anuncios",
                subtitle = if (plans.adFreeActive) "Plan activo" else "Ocultá los anuncios de los listados",
                onClick = onAdFree,
            )
            // Only an account linked to a provider can buy the "Destacado" plans.
            if (plans.isProviderAccount) {
                PlanEntry(
                    icon = Icons.Outlined.Star,
                    title = "Mi plan",
                    subtitle = if (plans.featuredActive) "Destacado activo" else "Destacate en el listado",
                    onClick = onMyPlan,
                )
            }
            Button(
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
                    Text(text = "Cerrar sesión", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun PlanEntry(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null)
        }
    }
}
