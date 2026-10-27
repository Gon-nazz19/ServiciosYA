package com.example.serviciosya.presentation.provider

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.serviciosya.BuildConfig
import com.example.serviciosya.domain.location.formatDistance
import com.example.serviciosya.domain.model.GeoPoint
import com.example.serviciosya.domain.model.Provider
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
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
    onWhatsApp: () -> Unit,
    onCall: () -> Unit,
    onShare: () -> Unit,
    onDirections: () -> Unit,
    onUserMessageShown: () -> Unit,
    showMap: Boolean = BuildConfig.HAS_MAPS_KEY,
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
                    onWhatsApp = onWhatsApp,
                    onCall = onCall,
                    onShare = onShare,
                    onDirections = onDirections,
                    showMap = showMap,
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
    onWhatsApp: () -> Unit,
    onCall: () -> Unit,
    onShare: () -> Unit,
    onDirections: () -> Unit,
    showMap: Boolean,
) {
    val focusManager = LocalFocusManager.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            // Keep the content above the keyboard so "Solicitar contacto" stays reachable while typing.
            .imePadding()
            // Tapping outside the message field closes the keyboard.
            .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
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
            uiState.distanceKm?.let {
                Text(
                    text = formatDistance(it),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        provider.location?.let { location ->
            if (showMap) ProviderMap(location = location, title = provider.name)
            OutlinedButton(onClick = onDirections, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Directions, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(text = "Cómo llegar", modifier = Modifier.padding(start = 8.dp))
            }
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ContactButton(text = "WhatsApp", icon = Icons.AutoMirrored.Outlined.Chat, onClick = onWhatsApp)
            ContactButton(text = "Llamar", icon = Icons.Outlined.Call, onClick = onCall)
            ContactButton(text = "Compartir", icon = Icons.Outlined.Share, onClick = onShare)
        }
    }
}

@Composable
private fun ProviderMap(location: GeoPoint, title: String) {
    val position = LatLng(location.latitude, location.longitude)
    val cameraPositionState = rememberCameraPositionState {
        this.position = CameraPosition.fromLatLngZoom(position, MAP_ZOOM)
    }
    GoogleMap(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(MaterialTheme.shapes.medium),
        cameraPositionState = cameraPositionState,
        // Without scroll gestures the map does not fight with the vertical scroll of the screen.
        uiSettings = MapUiSettings(
            scrollGesturesEnabled = false,
            zoomControlsEnabled = true,
            mapToolbarEnabled = false,
        ),
    ) {
        Marker(state = rememberMarkerState(position = position), title = title)
    }
}

private const val MAP_ZOOM = 15f

@Composable
private fun RowScope.ContactButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(text = text, style = MaterialTheme.typography.labelMedium, maxLines = 1)
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
