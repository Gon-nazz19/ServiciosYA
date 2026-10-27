package com.example.serviciosya.presentation.assistant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.serviciosya.domain.usecase.AnalyzeProblemUseCase

@Composable
fun AssistantCard(
    state: AssistantUiState,
    onProblemChange: (String) -> Unit,
    onAnalyze: () -> Unit,
    onSeeProviders: (categoryId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null)
                Text(text = "¿Qué necesitás resolver?", style = MaterialTheme.typography.titleMedium)
            }
            OutlinedTextField(
                value = state.problem,
                onValueChange = onProblemChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading,
                placeholder = { Text("Ej.: se me cortó la luz en la cocina") },
                supportingText = {
                    Text("${state.problem.length}/${AnalyzeProblemUseCase.MAX_PROBLEM_LENGTH}")
                },
                maxLines = 3,
            )
            Button(
                onClick = {
                    focusManager.clearFocus()
                    onAnalyze()
                },
                enabled = !state.isLoading && state.problem.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Ayudame")
                }
            }
            state.errorMessage?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            when (val result = state.result) {
                is AssistantResult.Matched -> MatchedResult(result, onSeeProviders)
                is AssistantResult.Unmatched -> UnmatchedResult(result)
                null -> Unit
            }
            Text(
                text = "Las sugerencias las genera una IA y pueden equivocarse.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun MatchedResult(result: AssistantResult.Matched, onSeeProviders: (String) -> Unit) {
    Text(
        text = buildAnnotatedString {
            append("Te sugerimos: ")
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(result.categoryName) }
        },
        style = MaterialTheme.typography.bodyLarge,
    )
    if (result.suggestedMessage.isNotBlank()) {
        Text(
            text = "Mensaje para el prestador: \"${result.suggestedMessage}\"",
            style = MaterialTheme.typography.bodyMedium,
            fontStyle = FontStyle.Italic,
        )
    }
    Button(onClick = { onSeeProviders(result.categoryId) }, modifier = Modifier.fillMaxWidth()) {
        Text("Ver prestadores")
    }
}

@Composable
private fun UnmatchedResult(result: AssistantResult.Unmatched) {
    Text(
        text = if (result.service != null) {
            buildAnnotatedString {
                append("Todavía no tenemos ")
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(result.service) }
                append(" en ServiciosYA. Ya registramos tu búsqueda.")
            }
        } else {
            buildAnnotatedString {
                append("No encontramos un servicio para eso. Elegí la categoría manualmente.")
            }
        },
        style = MaterialTheme.typography.bodyLarge,
    )
}
