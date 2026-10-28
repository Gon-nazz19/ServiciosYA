package com.example.serviciosya.data.billing

import com.example.serviciosya.domain.billing.CheckoutOutcome
import com.example.serviciosya.domain.billing.Entitlements
import com.example.serviciosya.domain.billing.PlanId
import com.example.serviciosya.domain.billing.PricingCatalog
import com.example.serviciosya.domain.billing.PurchaseResult
import com.example.serviciosya.domain.billing.Subscription
import com.example.serviciosya.domain.billing.Subscription.Companion.DAY_MILLIS
import com.example.serviciosya.domain.model.User
import com.example.serviciosya.testutil.FakeAuthRepository
import com.example.serviciosya.testutil.FakeProviderAccountRepository
import com.example.serviciosya.testutil.InMemorySubscriptionStore
import com.example.serviciosya.testutil.testProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SimulatedBillingRepositoryTest {
    private val now = 1_000_000_000_000L
    private val provider = testProvider(id = "p1")
    private val user = User(id = provider.userId, name = "Ana", email = "ana@example.com")

    private val auth = FakeAuthRepository(initialUser = user)
    private val store = InMemorySubscriptionStore()
    private val accounts = FakeProviderAccountRepository(provider)
    private val repository = SimulatedBillingRepository(auth, store, accounts) { now }

    /** Starts a purchase, lets it reach the payment screen and picks [outcome] there. */
    private suspend fun kotlinx.coroutines.test.TestScope.purchase(plan: PlanId, outcome: CheckoutOutcome): PurchaseResult {
        val result = async { repository.purchase(plan) }
        runCurrent()
        repository.resolve(outcome)
        return result.await()
    }

    @Test
    fun `an approved payment grants the client plan and saves it`() = runTest {
        val result = purchase(PlanId.ADFREE_YEARLY, CheckoutOutcome.APPROVED)

        assertEquals(PurchaseResult.Success, result)
        val saved = store.current(user.id).adFree
        assertEquals(PlanId.ADFREE_YEARLY, saved?.plan)
        assertEquals(now + 365 * DAY_MILLIS, saved?.expiresAtMillis)
        assertTrue(repository.observeSubscription().first().isAdFree(now))
    }

    @Test
    fun `the payment screen shows the plan while waiting and is cleared afterwards`() = runTest {
        val result = async { repository.purchase(PlanId.ADFREE_LIFETIME) }
        runCurrent()

        assertEquals(PricingCatalog.adFreeLifetime, repository.pending.value?.plan)

        repository.resolve(CheckoutOutcome.CANCELLED)
        result.await()
        assertNull(repository.pending.value)
    }

    @Test
    fun `a declined payment is an error and grants nothing`() = runTest {
        val result = purchase(PlanId.ADFREE_YEARLY, CheckoutOutcome.DECLINED)

        assertEquals(PurchaseResult.Error("declined"), result)
        assertEquals(Entitlements.NONE, store.current(user.id))
        assertEquals(0, store.saveCalls)
    }

    @Test
    fun `a pending payment grants nothing`() = runTest {
        val result = purchase(PlanId.ADFREE_LIFETIME, CheckoutOutcome.PENDING)

        assertEquals(PurchaseResult.Pending, result)
        assertEquals(Entitlements.NONE, store.current(user.id))
        assertFalse(repository.observeSubscription().first().isAdFree(now))
        assertEquals(0, store.saveCalls)
    }

    @Test
    fun `a cancelled payment grants nothing`() = runTest {
        val result = purchase(PlanId.FEATURED_WEEKLY, CheckoutOutcome.CANCELLED)

        assertEquals(PurchaseResult.Cancelled, result)
        assertEquals(Entitlements.NONE, store.current(user.id))
        assertTrue(accounts.featuredUntilCalls.isEmpty())
    }

    @Test
    fun `a provider plan features the provider until the plan ends`() = runTest {
        val result = purchase(PlanId.FEATURED_MONTHLY, CheckoutOutcome.APPROVED)

        assertEquals(PurchaseResult.Success, result)
        val saved = store.current(user.id).featured
        assertEquals(now + 7 * DAY_MILLIS, saved?.trialEndsAtMillis)
        assertEquals(listOf(provider.id to now + 37 * DAY_MILLIS), accounts.featuredUntilCalls)
    }

    @Test
    fun `a provider plan without a provider profile fails before asking for any payment`() = runTest {
        accounts.provider = null

        val result = repository.purchase(PlanId.FEATURED_WEEKLY)

        assertEquals(PurchaseResult.Error("not_a_provider"), result)
        assertNull(repository.pending.value)
        assertEquals(0, store.saveCalls)
    }

    @Test
    fun `a provider plan fails when the provider profile cannot be looked up`() = runTest {
        accounts.findFails = true

        assertEquals(PurchaseResult.Error("network"), repository.purchase(PlanId.FEATURED_WEEKLY))
        assertNull(repository.pending.value)
    }

    @Test
    fun `when the featured date cannot be written the plan stays saved and restoring retries`() = runTest {
        accounts.setFeaturedUntilFails = true

        val result = purchase(PlanId.FEATURED_YEARLY, CheckoutOutcome.APPROVED)

        assertEquals(PurchaseResult.Error("sync"), result)
        assertEquals(PlanId.FEATURED_YEARLY, store.current(user.id).featured?.plan)
        assertTrue(accounts.featuredUntilCalls.isEmpty())

        accounts.setFeaturedUntilFails = false
        assertTrue(repository.restore().isSuccess)
        assertEquals(listOf(provider.id to now + 365 * DAY_MILLIS), accounts.featuredUntilCalls)
    }

    @Test
    fun `the lifetime plan has no expiry date`() = runTest {
        purchase(PlanId.ADFREE_LIFETIME, CheckoutOutcome.APPROVED)

        val saved = store.current(user.id).adFree
        assertEquals(PlanId.ADFREE_LIFETIME, saved?.plan)
        assertNull(saved?.expiresAtMillis)
        assertTrue(repository.observeSubscription().first().isAdFree(now + 100 * 365 * DAY_MILLIS))
    }

    @Test
    fun `buying the yearly plan never downgrades the lifetime one`() = runTest {
        store.put(user.id, Entitlements(adFree = Subscription.start(PricingCatalog.adFreeLifetime, now)))

        val result = purchase(PlanId.ADFREE_YEARLY, CheckoutOutcome.APPROVED)

        assertEquals(PurchaseResult.Success, result)
        assertEquals(PlanId.ADFREE_LIFETIME, store.current(user.id).adFree?.plan)
    }

    @Test
    fun `buying the lifetime plan while on the yearly one replaces it`() = runTest {
        store.put(user.id, Entitlements(adFree = Subscription.start(PricingCatalog.adFreeYearly, now)))

        purchase(PlanId.ADFREE_LIFETIME, CheckoutOutcome.APPROVED)

        assertEquals(PlanId.ADFREE_LIFETIME, store.current(user.id).adFree?.plan)
    }

    @Test
    fun `restoring returns what the account bought, including the lifetime plan`() = runTest {
        store.put(user.id, Entitlements(adFree = Subscription.start(PricingCatalog.adFreeLifetime, now)))

        val restored = repository.restore().getOrThrow()

        assertTrue(restored.hasLifetimeAdFree)
        assertTrue(restored.isAdFree(now + 50 * 365 * DAY_MILLIS))
    }

    @Test
    fun `restoring an account without purchases returns no plans`() = runTest {
        assertEquals(Entitlements.NONE, repository.restore().getOrThrow())
    }

    @Test
    fun `restoring fails when the purchases cannot be loaded`() = runTest {
        store.loadFails = true

        assertTrue(repository.restore().isFailure)
    }

    @Test
    fun `a payment cannot be started twice at the same time`() = runTest {
        val first = async { repository.purchase(PlanId.ADFREE_YEARLY) }
        runCurrent()

        assertEquals(PurchaseResult.Error("busy"), repository.purchase(PlanId.ADFREE_LIFETIME))

        repository.resolve(CheckoutOutcome.CANCELLED)
        assertEquals(PurchaseResult.Cancelled, first.await())
    }

    @Test
    fun `leaving the screen while the payment waits closes the payment screen`() = runTest {
        val job = launch { repository.purchase(PlanId.ADFREE_YEARLY) }
        runCurrent()
        assertTrue(repository.pending.value != null)

        job.cancel()
        runCurrent()

        assertNull(repository.pending.value)
    }

    @Test
    fun `an approved payment is an error when the plan cannot be saved`() = runTest {
        store.saveFails = true

        val result = purchase(PlanId.ADFREE_YEARLY, CheckoutOutcome.APPROVED)

        assertEquals(PurchaseResult.Error("store"), result)
        assertEquals(Entitlements.NONE, store.current(user.id))
    }

    @Test
    fun `without a session nothing can be bought or restored and there are no plans`() = runTest {
        auth.currentUser.value = null

        assertEquals(PurchaseResult.Error("not_signed_in"), repository.purchase(PlanId.ADFREE_YEARLY))
        assertTrue(repository.restore().isFailure)
        assertEquals(Entitlements.NONE, repository.observeSubscription().first())
    }

    @Test
    fun `plans follow the signed in account`() = runTest {
        store.put(user.id, Entitlements(adFree = Subscription.start(PricingCatalog.adFreeLifetime, now)))
        assertTrue(repository.observeSubscription().first().hasLifetimeAdFree)

        auth.currentUser.value = User(id = "someone-else", name = "Beto", email = "beto@example.com")

        assertEquals(Entitlements.NONE, repository.observeSubscription().first())
    }
}
