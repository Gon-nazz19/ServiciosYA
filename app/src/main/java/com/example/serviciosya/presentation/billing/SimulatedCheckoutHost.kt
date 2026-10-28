package com.example.serviciosya.presentation.billing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.serviciosya.domain.billing.CheckoutOutcome
import com.example.serviciosya.domain.billing.SimulatedCheckout
import com.example.serviciosya.domain.billing.formatPesos

/**
 * The simulated payment screen. It only exists with the simulated gateway: with Google Play Billing
 * the container has no [SimulatedCheckout], so nothing is drawn. No real card data is asked for or sent.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulatedCheckoutHost(checkout: SimulatedCheckout?) {
    if (checkout == null) return
    val pending by checkout.pending.collectAsStateWithLifecycle()
    val plan = pending?.plan ?: return

    ModalBottomSheet(onDismissRequest = { checkout.resolve(CheckoutOutcome.CANCELLED) }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "Pasarela de pago simulada", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Es una simulación: no se cobra nada ni se guardan datos de tarjeta.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = plan.title, style = MaterialTheme.typography.titleMedium)
                    val total = if (plan.trialDays > 0) {
                        "Hoy: ${formatPesos(0)} · después ${formatPesos(plan.priceArs)} ${planPeriodLabel(plan)}"
                    } else {
                        "${formatPesos(plan.priceArs)} ${planPeriodLabel(plan)}"
                    }
                    Text(text = total, style = MaterialTheme.typography.bodyLarge)
                    if (plan.trialDays > 0) {
                        Text(
                            text = "${plan.trialDays} días de prueba gratis",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.CreditCard, contentDescription = null)
                Text(text = "Tarjeta de prueba •••• •••• •••• 4242", style = MaterialTheme.typography.bodyLarge)
            }
            Text(
                text = "¿Qué respuesta querés simular?",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 4.dp),
            )
            Button(
                onClick = { checkout.resolve(CheckoutOutcome.APPROVED) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Pago aprobado") }
            OutlinedButton(
                onClick = { checkout.resolve(CheckoutOutcome.DECLINED) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Pago rechazado") }
            OutlinedButton(
                onClick = { checkout.resolve(CheckoutOutcome.PENDING) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Pago pendiente") }
            TextButton(
                onClick = { checkout.resolve(CheckoutOutcome.CANCELLED) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Cancelar") }
        }
    }
}
