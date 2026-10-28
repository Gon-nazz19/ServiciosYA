package com.example.serviciosya.data.billing

import com.example.serviciosya.domain.billing.BillingRepository
import com.example.serviciosya.domain.billing.Entitlements
import com.example.serviciosya.domain.billing.PlanId
import com.example.serviciosya.domain.billing.PurchaseResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Used when Firebase is not configured: nobody has a plan and nothing can be bought. */
class UnavailableBillingRepository : BillingRepository {
    override fun observeSubscription(): Flow<Entitlements> = flowOf(Entitlements.NONE)

    override suspend fun purchase(plan: PlanId): PurchaseResult = PurchaseResult.Error(REASON_UNAVAILABLE)

    override suspend fun restore(): Result<Entitlements> = Result.failure(
        IllegalStateException(
            "Firebase no está configurado. Agregá app/google-services.json para usar los planes.",
        ),
    )

    private companion object {
        const val REASON_UNAVAILABLE = "unavailable"
    }
}
