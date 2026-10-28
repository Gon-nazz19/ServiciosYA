package com.example.serviciosya.presentation.billing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.serviciosya.domain.billing.PlanId
import com.example.serviciosya.domain.billing.PricingCatalog

/** "Sin anuncios": the yearly and lifetime plans for clients. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdFreeScreen(
    uiState: AdFreeUiState,
    onSelectPlan: (PlanId) -> Unit,
    onRestore: () -> Unit,
    onMessageShown: () -> Unit,
    onBack: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            onMessageShown()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sin anuncios") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Ocultá los anuncios de los listados de prestadores mientras tu plan esté activo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            uiState.status?.let {
                Text(
                    text = statusText(it),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (uiState.hasLifetime) {
                Text(
                    text = "Ya tenés el plan de por vida: no necesitás el anual.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            PricingCatalog.clientPlans.forEach { plan ->
                PlanCard(
                    plan = plan,
                    isCurrent = uiState.status?.plan == plan.id,
                    // The yearly plan makes no sense once the lifetime one is owned.
                    enabled = !uiState.isBusy && !(uiState.hasLifetime && plan.id == PlanId.ADFREE_YEARLY),
                    onSelect = { onSelectPlan(plan.id) },
                )
            }
            TextButton(
                onClick = onRestore,
                enabled = !uiState.isBusy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Restaurar compras") }
            Text(
                text = "Pasarela de pago simulada: no se cobra nada.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
