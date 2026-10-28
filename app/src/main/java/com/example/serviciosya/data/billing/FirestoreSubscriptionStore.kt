package com.example.serviciosya.data.billing

import com.example.serviciosya.domain.billing.Audience
import com.example.serviciosya.domain.billing.Entitlements
import com.example.serviciosya.domain.billing.PlanId
import com.example.serviciosya.domain.billing.Subscription
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * Plans of each user in `subscriptions/{uid}`, with one slot per kind: `adFree` and `featured`.
 * Demo only: the app writes its own plan (see firestore.rules).
 */
class FirestoreSubscriptionStore(
    private val firestore: FirebaseFirestore,
) : SubscriptionStore {
    override fun observe(userId: String): Flow<Entitlements> = callbackFlow {
        val registration = document(userId).addSnapshotListener { snapshot, error ->
            // Without a readable plan the user simply has none, so ads keep showing.
            trySend(if (error != null || snapshot == null) Entitlements.NONE else snapshot.toEntitlements())
        }
        awaitClose { registration.remove() }
    }

    override suspend fun load(userId: String): Result<Entitlements> = runCatching {
        document(userId).get().await().toEntitlements()
    }

    override suspend fun save(userId: String, entitlements: Entitlements): Result<Unit> = runCatching {
        val data = buildMap<String, Any> {
            entitlements.adFree?.let { put(AD_FREE, it.toMap()) }
            entitlements.featured?.let { put(FEATURED, it.toMap()) }
            put(UPDATED_AT, FieldValue.serverTimestamp())
        }
        document(userId).set(data).await()
        Unit
    }

    private fun document(userId: String): DocumentReference =
        firestore.collection(COLLECTION).document(userId)

    private fun Subscription.toMap(): Map<String, Any?> = buildMap {
        put(PLAN, plan.value)
        put(EXPIRES_AT, expiresAtMillis?.toTimestamp())
        trialEndsAtMillis?.let { put(TRIAL_ENDS_AT, it.toTimestamp()) }
    }

    private fun Long.toTimestamp() = Timestamp(Date(this))

    private fun DocumentSnapshot.toEntitlements(): Entitlements =
        if (!exists()) {
            Entitlements.NONE
        } else {
            Entitlements(
                adFree = subscriptionIn(AD_FREE, Audience.CLIENT),
                featured = subscriptionIn(FEATURED, Audience.PROVIDER),
            )
        }

    private fun DocumentSnapshot.subscriptionIn(slot: String, audience: Audience): Subscription? {
        val fields = get(slot) as? Map<*, *> ?: return null
        val plan = PlanId.fromValue(fields[PLAN] as? String)?.takeIf { it.audience == audience } ?: return null
        val expiresAt = (fields[EXPIRES_AT] as? Timestamp)?.toDate()?.time
        // Only the lifetime plan has no expiry: any other plan without one is a malformed document.
        if (expiresAt == null && plan != PlanId.ADFREE_LIFETIME) return null
        return Subscription(
            plan = plan,
            trialEndsAtMillis = (fields[TRIAL_ENDS_AT] as? Timestamp)?.toDate()?.time,
            expiresAtMillis = expiresAt,
        )
    }

    private companion object {
        const val COLLECTION = "subscriptions"
        const val AD_FREE = "adFree"
        const val FEATURED = "featured"
        const val UPDATED_AT = "updatedAt"
        const val PLAN = "plan"
        const val EXPIRES_AT = "expiresAt"
        const val TRIAL_ENDS_AT = "trialEndsAt"
    }
}
