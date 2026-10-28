package com.example.serviciosya.presentation.billing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.serviciosya.domain.billing.Plan
import com.example.serviciosya.domain.billing.formatPesos

@Composable
fun PlanCard(
    plan: Plan,
    isCurrent: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = if (plan.isBestOffer) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = plan.title, style = MaterialTheme.typography.titleMedium)
                if (plan.isBestOffer) {
                    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primary) {
                        Text(
                            text = "Mejor oferta",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = formatPesos(plan.priceArs), style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = " ${planPeriodLabel(plan)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
            planHighlights(plan).forEach { line ->
                Text(
                    text = "• $line",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = onSelect,
                enabled = enabled && !isCurrent,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = if (isCurrent) "Plan activo" else planActionLabel(plan))
            }
        }
    }
}
