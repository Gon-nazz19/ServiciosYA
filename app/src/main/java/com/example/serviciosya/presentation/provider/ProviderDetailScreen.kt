package com.example.serviciosya.presentation.provider

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.serviciosya.domain.model.Provider
import com.example.serviciosya.domain.usecase.CreateServiceRequestUseCase
import com.example.serviciosya.presentation.components.InitialsAvatar
import com.example.serviciosya.presentation.components.formatRating

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderDetailScreen(
    uiState: ProviderDetailUiState,
    categoryName: String,
    onRetry: () -> Unit,
    onMessageChange: (String) -> Unit,
    onRequestContact: () -> Unit,
    onUserMessageShown: () -> Unit,
    onBack: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            onUserMessageShown()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Prestador") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            val provider = uiState.provider
            when {
                uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                provider == null -> DetailError(
                    message = uiState.errorMessage ?: ProviderDetailViewModel.NOT_FOUND_MESSAGE,
                    onRetry = onRetry,
                )
                else -> ProviderDetailContent(
                    provider = provider,
                    categoryName = categoryName,
                    uiState = uiState,
                    onMessageChange = onMessageChange,
                    onRequestContact = onRequestContact,
                )
            }
        }
    }
}

@Composable
private fun ProviderDetailContent(
    provider: Provider,
    categoryName: String,
    uiState: ProviderDetailUiState,
    onMessageChange: (String) -> Unit,
    onRequestContact: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            InitialsAvatar(
                name = provider.name,
                size = 96.dp,
                textStyle = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = provider.name,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            formatRating(provider.rating)?.let {
                Text(text = it, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            Text(
                text = listOf(categoryName, provider.city).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HorizontalDivider()
        Text(text = "Sobre mí", style = MaterialTheme.typography.titleMedium)
        Text(
            text = provider.description.ifBlank { "Este prestador todavía no agregó una descripción." },
            style = MaterialTheme.typography.bodyLarge,
        )
        if (!uiState.requestSent) {
            OutlinedTextField(
                value = uiState.message,
                onValueChange = onMessageChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSubmitting,
                label = { Text("Mensaje (opcional)") },
                placeholder = { Text("Contale brevemente qué necesitás") },
                supportingText = {
                    Text("${uiState.message.length}/${CreateServiceRequestUseCase.MAX_MESSAGE_LENGTH}")
                },
                minLines = 3,
            )
        }
        Button(
            onClick = onRequestContact,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isSubmitting && !uiState.requestSent,
        ) {
            when {
                uiState.isSubmitting -> CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
                uiState.requestSent -> Text("Solicitud enviada")
                else -> Text("Solicitar contacto")
            }
        }
    }
}

@Composable
private fun DetailError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onRetry) { Text("Reintentar") }
    }
}
