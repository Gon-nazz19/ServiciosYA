package com.example.serviciosya.presentation.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.ElectricalServices
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material.icons.outlined.HomeRepairService
import androidx.compose.material.icons.outlined.Plumbing
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.serviciosya.presentation.components.InitialsAvatar
import com.example.serviciosya.presentation.components.formatRating

data class HomeCategoryItem(
    val id: String,
    val name: String,
    val icon: CategoryIcon,
)

enum class CategoryIcon {
    ELECTRICITY,
    PLUMBING,
    AIR_CONDITIONING,
    COMPUTER,
    GARDEN,
    OTHER;

    companion object {
        fun fromFirestore(value: String): CategoryIcon = when (value.lowercase()) {
            "electrical_services", "electricity" -> ELECTRICITY
            "plumbing" -> PLUMBING
            "air", "air_conditioning", "ac_unit" -> AIR_CONDITIONING
            "computer", "desktop_windows" -> COMPUTER
            "grass", "garden", "yard" -> GARDEN
            else -> OTHER
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userName: String,
    categories: List<HomeCategoryItem>,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    search: HomeSearchState = HomeSearchState(),
    onRetry: () -> Unit = {},
    onSearchQueryChange: (String) -> Unit = {},
    onCategoryClick: (HomeCategoryItem) -> Unit,
    onProviderClick: (ProviderSearchItem) -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ServiciosYA", fontWeight = FontWeight.SemiBold) },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Hola, ${userName.ifBlank { "bienvenido" }}",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = "¿Qué servicio necesitás?",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedTextField(
                    value = search.query,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Buscar servicio o prestador") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        if (search.query.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Limpiar búsqueda")
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                )
                if (!search.isActive) {
                    Text(text = "Categorías", style = MaterialTheme.typography.titleLarge)
                }
            }

            when {
                isLoading -> HomeLoadingState()
                errorMessage != null -> HomeErrorState(errorMessage, onRetry)
                search.isActive -> SearchResults(
                    search = search,
                    onCategoryClick = onCategoryClick,
                    onProviderClick = onProviderClick,
                )
                categories.isEmpty() -> HomeEmptyState("Todavía no hay categorías disponibles.")
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 148.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(categories, key = { it.id }) { category ->
                        CategoryCard(category = category, onClick = { onCategoryClick(category) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResults(
    search: HomeSearchState,
    onCategoryClick: (HomeCategoryItem) -> Unit,
    onProviderClick: (ProviderSearchItem) -> Unit,
) {
    if (search.hasNoResults) {
        HomeEmptyState(
            if (search.providersUnavailable) {
                "No encontramos categorías para esa búsqueda y no pudimos buscar prestadores."
            } else {
                "No encontramos resultados para esa búsqueda."
            },
        )
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (search.categories.isNotEmpty()) {
            item(key = "categories-header") { SearchSectionTitle("Categorías") }
            items(search.categories, key = { "category-${it.id}" }) { category ->
                SearchResultRow(
                    title = category.name,
                    subtitle = null,
                    leading = {
                        Icon(
                            imageVector = category.icon.imageVector(),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    onClick = { onCategoryClick(category) },
                )
            }
        }
        if (search.providers.isNotEmpty()) {
            item(key = "providers-header") { SearchSectionTitle("Prestadores") }
            items(search.providers, key = { "provider-${it.id}" }) { provider ->
                SearchResultRow(
                    title = provider.name,
                    subtitle = listOfNotNull(
                        provider.categoryName.ifBlank { null },
                        provider.city.ifBlank { null },
                        formatRating(provider.rating),
                    ).joinToString(" · "),
                    leading = { InitialsAvatar(name = provider.name, size = 40.dp) },
                    onClick = { onProviderClick(provider) },
                )
            }
        }
        if (search.isLoadingProviders) {
            item(key = "providers-loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            }
        } else if (search.providersUnavailable) {
            item(key = "providers-error") {
                Text(
                    text = "No pudimos buscar prestadores en este momento.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun SearchSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

@Composable
private fun SearchResultRow(
    title: String,
    subtitle: String?,
    leading: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading()
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(category: HomeCategoryItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 132.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = category.icon.imageVector(),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = category.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun HomeLoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun HomeErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = message, color = MaterialTheme.colorScheme.error)
        Button(onClick = onRetry) { Text("Reintentar") }
    }
}

@Composable
private fun HomeEmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun CategoryIcon.imageVector(): ImageVector = when (this) {
    CategoryIcon.ELECTRICITY -> Icons.Outlined.ElectricalServices
    CategoryIcon.PLUMBING -> Icons.Outlined.Plumbing
    CategoryIcon.AIR_CONDITIONING -> Icons.Outlined.Air
    CategoryIcon.COMPUTER -> Icons.Outlined.Computer
    CategoryIcon.GARDEN -> Icons.Outlined.Grass
    CategoryIcon.OTHER -> Icons.Outlined.HomeRepairService
}
