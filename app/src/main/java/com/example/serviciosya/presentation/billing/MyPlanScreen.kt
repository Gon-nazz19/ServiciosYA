package com.example.serviciosya.presentation.billing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.serviciosya.domain.billing.PlanId
import com.example.serviciosya.domain.billing.PricingCatalog

/** "Mi plan": the "Destacado" plans, only for an account linked to a provider. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPlanScreen(
    uiState: MyPlanUiState,
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
                title = { Text("Mi plan") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            uiState.isLoading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            !uiState.isProviderAccount -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Tu cuenta no está asociada a un prestador, así que no hay planes de Destacado para vos.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = "Los planes sin anuncios están en Perfil > Sin anuncios.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                uiState.providerName?.let { Text(text = it, style = MaterialTheme.typography.headlineSmall) }
                Text(
                    text = "Destacate: tu ficha muestra la insignia \"Destacado\" y aparece en un lugar prioritario " +
                        "del listado. Con un plan activo tampoco ves anuncios.",
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
                PricingCatalog.providerPlans.forEach { plan ->
                    PlanCard(
                        plan = plan,
                        isCurrent = uiState.status?.plan == plan.id,
                        enabled = !uiState.isBusy,
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
}
