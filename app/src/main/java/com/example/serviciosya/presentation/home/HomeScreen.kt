package com.example.serviciosya.presentation.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.ElectricalServices
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material.icons.outlined.HomeRepairService
import androidx.compose.material.icons.outlined.Person
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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
    onRetry: () -> Unit = {},
    onCategoryClick: (HomeCategoryItem) -> Unit,
    onProfileClick: () -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    val visibleCategories = remember(categories, searchQuery) {
        categories.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ServiciosYA", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = onProfileClick) {
                        Icon(Icons.Outlined.Person, contentDescription = "Abrir perfil")
                    }
                },
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
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Buscar servicio") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                )
                Text(text = "Categorías", style = MaterialTheme.typography.titleLarge)
            }

            when {
                isLoading -> HomeLoadingState()
                errorMessage != null -> HomeErrorState(errorMessage, onRetry)
                visibleCategories.isEmpty() -> HomeEmptyState(hasSearch = searchQuery.isNotBlank())
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 148.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(visibleCategories, key = { it.id }) { category ->
                        CategoryCard(category = category, onClick = { onCategoryClick(category) })
                    }
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
private fun HomeEmptyState(hasSearch: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (hasSearch) {
                "No encontramos categorías para esa búsqueda."
            } else {
                "Todavía no hay categorías disponibles."
            },
            style = MaterialTheme.typography.bodyLarge,
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
