package com.example.serviciosya.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.serviciosya.data.repository.FirebaseAuthRepository
import com.example.serviciosya.data.repository.FirestoreCategoryRepository
import com.example.serviciosya.data.repository.FirestoreProviderRepository
import com.example.serviciosya.data.repository.FirestoreServiceRequestRepository
import com.example.serviciosya.domain.model.AuthErrorReason
import com.example.serviciosya.domain.model.AuthException
import com.example.serviciosya.domain.model.NewServiceRequest
import com.example.serviciosya.domain.model.RequestStatus
import com.example.serviciosya.domain.model.User
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * US-151: the real Firebase repositories against the Firestore/Auth emulators,
 * with the project's firestore.rules loaded by the emulator.
 *
 * Run with tools/firestore-rules `npm run test:android` (see its README).
 */
@RunWith(AndroidJUnit4::class)
class FirebaseRepositoriesTest {
    private val auth = FirebaseEmulator.auth
    private val firestore = FirebaseEmulator.firestore

    private val authRepository = FirebaseAuthRepository(auth, firestore)
    private val categoryRepository = FirestoreCategoryRepository(firestore)
    private val providerRepository = FirestoreProviderRepository(firestore)
    private val requestRepository = FirestoreServiceRequestRepository(firestore, auth)

    @Before
    fun setUp() {
        FirebaseEmulator.reset()
        FirebaseEmulator.seed("categories/plomeros", mapOf("name" to "Plomero", "icon" to "plumbing", "active" to true))
        FirebaseEmulator.seed(
            "categories/electricistas",
            mapOf("name" to "Electricista", "icon" to "electrical_services", "active" to true),
        )
        FirebaseEmulator.seed("categories/cerrajeros", mapOf("name" to "Cerrajero", "icon" to "key", "active" to false))
        seedProvider("carlos", "Carlos Electricidad", "electricistas", rating = 4.7)
        seedProvider("electrofix", "ElectroFix", "electricistas", rating = 4.9)
        seedProvider("inactivo", "Electricista Inactivo", "electricistas", active = false)
        seedProvider("garcia", "Plomería García", "plomeros", rating = null)
    }

    private fun seedProvider(
        id: String,
        name: String,
        categoryId: String,
        rating: Double? = 4.0,
        active: Boolean = true,
    ) = FirebaseEmulator.seed(
        "providers/$id",
        mapOf(
            "userId" to "",
            "name" to name,
            "description" to "Descripción de $name",
            "categoryId" to categoryId,
            "city" to "San Francisco",
            "profileImageUrl" to "",
            "phone" to "3564-000000",
            "rating" to rating,
            "reviewCount" to 3L,
            "verified" to false,
            "active" to active,
        ),
    )

    private suspend fun registerAndSignIn(name: String, email: String) {
        authRepository.register(name, email, "secret123").getOrThrow()
    }

    // --- Auth ---

    @Test
    fun registerCreatesSessionAndClientProfile() = runBlocking {
        registerAndSignIn("Ana", "ana@example.com")

        val user: User = withTimeout(5_000) { authRepository.observeCurrentUser().filterNotNull().first() }
        assertEquals("Ana", user.name)
        assertEquals("ana@example.com", user.email)

        val profile = firestore.collection("users").document(user.id).get().await()
        assertEquals("CLIENT", profile.getString("role"))
        assertEquals("Ana", profile.getString("name"))
        assertNotNull(profile.getTimestamp("createdAt"))
    }

    @Test
    fun registeringAnExistingEmailFailsWithEmailInUse() = runBlocking {
        registerAndSignIn("Ana", "ana@example.com")
        authRepository.signOut().getOrThrow()

        val error = authRepository.register("Otra", "ana@example.com", "secret123").exceptionOrNull()

        assertEquals(AuthErrorReason.EMAIL_ALREADY_IN_USE, (error as AuthException).reason)
    }

    @Test
    fun signInWithWrongPasswordFailsWithInvalidCredentials() = runBlocking {
        registerAndSignIn("Ana", "ana@example.com")
        authRepository.signOut().getOrThrow()

        val error = authRepository.signIn("ana@example.com", "wrong-password").exceptionOrNull()

        assertEquals(AuthErrorReason.INVALID_CREDENTIALS, (error as AuthException).reason)
    }

    @Test
    fun signInAndSignOutUpdateTheSession() = runBlocking {
        registerAndSignIn("Ana", "ana@example.com")
        authRepository.signOut().getOrThrow()
        assertNull(auth.currentUser)

        authRepository.signIn("ana@example.com", "secret123").getOrThrow()

        assertEquals("ana@example.com", auth.currentUser?.email)
    }

    // --- Catalog ---

    @Test
    fun categoriesReturnOnlyActiveOnesSortedByName() = runBlocking {
        registerAndSignIn("Ana", "ana@example.com")

        val categories = categoryRepository.getActiveCategories().getOrThrow()

        assertEquals(listOf("Electricista", "Plomero"), categories.map { it.name })
    }

    @Test
    fun catalogIsNotReadableWithoutSession() = runBlocking {
        assertTrue(categoryRepository.getActiveCategories().isFailure)
        assertTrue(providerRepository.getActiveProvidersByCategory("electricistas").isFailure)
    }

    @Test
    fun providersByCategoryReturnOnlyActiveOnesSortedByRating() = runBlocking {
        registerAndSignIn("Ana", "ana@example.com")

        val providers = providerRepository.getActiveProvidersByCategory("electricistas").getOrThrow()

        assertEquals(listOf("electrofix", "carlos"), providers.map { it.id })
        assertEquals(4.9, providers.first().rating!!, 0.0)
        assertEquals(3, providers.first().reviewCount)
    }

    @Test
    fun providerByIdAndMissingProvider() = runBlocking {
        registerAndSignIn("Ana", "ana@example.com")

        val provider = providerRepository.getProvider("garcia").getOrThrow()
        assertEquals("Plomería García", provider?.name)
        assertNull(provider?.rating)
        assertNull(providerRepository.getProvider("no-existe").getOrThrow())
    }

    @Test
    fun activeProvidersForSearchExcludeInactiveOnes() = runBlocking {
        registerAndSignIn("Ana", "ana@example.com")

        val providers = providerRepository.getActiveProviders().getOrThrow()

        assertEquals(listOf("Carlos Electricidad", "ElectroFix", "Plomería García"), providers.map { it.name })
    }

    // --- Service requests ---

    private fun newRequest(providerId: String = "carlos") = NewServiceRequest(
        providerId = providerId,
        categoryId = "electricistas",
        providerName = "Carlos Electricidad",
        categoryName = "Electricista",
        message = "Necesito cambiar un enchufe",
    )

    @Test
    fun createdRequestIsPendingAndListedForItsOwner() = runBlocking {
        registerAndSignIn("Ana", "ana@example.com")

        requestRepository.createRequest(newRequest()).getOrThrow()
        val requests = requestRepository.getMyRequests().getOrThrow()

        val request = requests.single()
        assertEquals(auth.currentUser!!.uid, request.clientId)
        assertEquals("carlos", request.providerId)
        assertEquals("Carlos Electricidad", request.providerName)
        assertEquals("Electricista", request.categoryName)
        assertEquals(RequestStatus.PENDING, request.status)
        assertNotNull(request.createdAtMillis)
    }

    @Test
    fun usersOnlySeeTheirOwnRequests() = runBlocking {
        registerAndSignIn("Ana", "ana@example.com")
        requestRepository.createRequest(newRequest()).getOrThrow()
        authRepository.signOut().getOrThrow()

        registerAndSignIn("Beto", "beto@example.com")
        requestRepository.createRequest(newRequest()).getOrThrow()
        requestRepository.createRequest(newRequest()).getOrThrow()

        val betoRequests = requestRepository.getMyRequests().getOrThrow()
        assertEquals(2, betoRequests.size)
        assertTrue(betoRequests.all { it.clientId == auth.currentUser!!.uid })
    }

    @Test
    fun requestForUnknownProviderIsRejectedByRules() = runBlocking {
        registerAndSignIn("Ana", "ana@example.com")

        assertTrue(requestRepository.createRequest(newRequest(providerId = "no-existe")).isFailure)
        assertTrue(requestRepository.getMyRequests().getOrThrow().isEmpty())
    }

    @Test
    fun requestsNeedASession() = runBlocking {
        assertTrue(requestRepository.createRequest(newRequest()).isFailure)
        assertTrue(requestRepository.getMyRequests().isFailure)
    }
}
