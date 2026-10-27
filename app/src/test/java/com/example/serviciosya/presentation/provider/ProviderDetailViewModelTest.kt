package com.example.serviciosya.presentation.provider

import com.example.serviciosya.domain.analytics.AnalyticsEvents
import com.example.serviciosya.domain.analytics.AnalyticsParams
import com.example.serviciosya.domain.analytics.ContactChannel
import com.example.serviciosya.domain.usecase.CreateServiceRequestUseCase
import com.example.serviciosya.domain.usecase.GetProviderUseCase
import com.example.serviciosya.testutil.FakeAnalyticsTracker
import com.example.serviciosya.domain.model.GeoPoint
import com.example.serviciosya.testutil.FakeContactLauncher
import com.example.serviciosya.testutil.FakeLocationProvider
import com.example.serviciosya.testutil.FakeProviderRepository
import com.example.serviciosya.testutil.FakeServiceRequestRepository
import com.example.serviciosya.testutil.MainDispatcherRule
import com.example.serviciosya.testutil.testProvider
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProviderDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeProviderRepository(
        providerResult = Result.success(testProvider()),
    )
    private val requestRepository = FakeServiceRequestRepository()
    private val analytics = FakeAnalyticsTracker()
    private val contactLauncher = FakeContactLauncher()
    private val locationProvider = FakeLocationProvider()

    private fun createViewModel(providerId: String = "provider-1") = ProviderDetailViewModel(
        providerId = providerId,
        getProvider = GetProviderUseCase(repository),
        createServiceRequest = CreateServiceRequestUseCase(requestRepository),
        contactLauncher = contactLauncher,
        locationProvider = locationProvider,
        analytics = analytics,
    )

    @Test
    fun `loads the provider by id`() = runTest {
        val provider = testProvider()
        repository.providerResult = Result.success(provider)

        val viewModel = createViewModel()
        assertTrue(viewModel.uiState.value.isLoading)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(provider, state.provider)
    }

    @Test
    fun `missing provider shows not found`() = runTest {
        repository.providerResult = Result.success(null)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.provider)
        assertEquals(ProviderDetailViewModel.NOT_FOUND_MESSAGE, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `failure shows error and retry recovers`() = runTest {
        repository.providerResult = Result.failure(RuntimeException("offline"))
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals(ProviderDetailViewModel.LOAD_ERROR_MESSAGE, viewModel.uiState.value.errorMessage)

        repository.providerResult = Result.success(testProvider())
        viewModel.loadProvider()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals("provider-1", viewModel.uiState.value.provider?.id)
    }

    @Test
    fun `request contact creates a pending request with provider data`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onMessageChange("  Necesito cambiar un enchufe  ")
        viewModel.requestContact(categoryName = "Electricista")
        assertTrue(viewModel.uiState.value.isSubmitting)
        advanceUntilIdle()

        val request = requestRepository.createdRequests.single()
        assertEquals("provider-1", request.providerId)
        assertEquals("electricistas", request.categoryId)
        assertEquals("Carlos Electricidad", request.providerName)
        assertEquals("Electricista", request.categoryName)
        assertEquals("Necesito cambiar un enchufe", request.message)

        val state = viewModel.uiState.value
        assertFalse(state.isSubmitting)
        assertTrue(state.requestSent)
        assertEquals("", state.message)
        assertEquals(ProviderDetailViewModel.REQUEST_SENT_MESSAGE, state.userMessage)
    }

    @Test
    fun `double tap creates a single request`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.requestContact(categoryName = "Electricista")
        viewModel.requestContact(categoryName = "Electricista")
        advanceUntilIdle()
        viewModel.requestContact(categoryName = "Electricista")
        advanceUntilIdle()

        assertEquals(1, requestRepository.createdRequests.size)
    }

    @Test
    fun `failed request shows error and allows retrying`() = runTest {
        requestRepository.createResult = Result.failure(RuntimeException("PERMISSION_DENIED"))
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onMessageChange("Hola")
        viewModel.requestContact(categoryName = "Electricista")
        advanceUntilIdle()

        var state = viewModel.uiState.value
        assertFalse(state.isSubmitting)
        assertFalse(state.requestSent)
        assertEquals("Hola", state.message)
        assertEquals(ProviderDetailViewModel.REQUEST_ERROR_MESSAGE, state.userMessage)

        viewModel.onUserMessageShown()
        assertNull(viewModel.uiState.value.userMessage)

        requestRepository.createResult = Result.success(Unit)
        viewModel.requestContact(categoryName = "Electricista")
        advanceUntilIdle()

        state = viewModel.uiState.value
        assertTrue(state.requestSent)
        assertEquals(2, requestRepository.createdRequests.size)
    }

    @Test
    fun `request is ignored while the provider is not loaded`() = runTest {
        val viewModel = createViewModel()

        viewModel.requestContact(categoryName = "Electricista")
        advanceUntilIdle()

        assertTrue(requestRepository.createdRequests.isEmpty())
    }

    @Test
    fun `message longer than the limit is not accepted`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onMessageChange("a".repeat(CreateServiceRequestUseCase.MAX_MESSAGE_LENGTH + 1))

        assertEquals("", viewModel.uiState.value.message)
    }

    @Test
    fun `provider view is tracked once after loading`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.loadProvider()
        advanceUntilIdle()

        val event = analytics.eventsNamed(AnalyticsEvents.PROVIDER_VIEW).single()
        assertEquals("provider-1", event.params[AnalyticsParams.PROVIDER_ID])
        assertEquals("electricistas", event.params[AnalyticsParams.CATEGORY_ID])
        assertEquals("unknown", event.params[AnalyticsParams.DISTANCE_BUCKET])
    }

    @Test
    fun `with location the distance is shown and only its bucket is tracked`() = runTest {
        repository.providerResult = Result.success(testProvider(latitude = -31.4280, longitude = -62.0830))
        locationProvider.location = GeoPoint(-31.4460, -62.0830)
        locationProvider.permissionGranted = true

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(2.0, viewModel.uiState.value.distanceKm ?: 0.0, 0.05)
        val event = analytics.eventsNamed(AnalyticsEvents.PROVIDER_VIEW).single()
        assertEquals("1-3km", event.params[AnalyticsParams.DISTANCE_BUCKET])
        assertTrue(event.params.values.none { it.contains("31.4") || it.contains("62.08") })
    }

    @Test
    fun `directions open the maps app on the provider location`() = runTest {
        repository.providerResult = Result.success(testProvider(latitude = -31.4268, longitude = -62.0849))
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.openDirections()

        assertEquals(
            "geo:-31.4268,-62.0849?q=-31.4268,-62.0849(Carlos%20Electricidad)",
            contactLauncher.mapUris.single(),
        )
    }

    @Test
    fun `directions do nothing when the provider has no location`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.openDirections()

        assertTrue(contactLauncher.mapUris.isEmpty())
    }

    @Test
    fun `provider view is not tracked when the provider does not exist`() = runTest {
        repository.providerResult = Result.success(null)
        createViewModel()
        advanceUntilIdle()

        assertTrue(analytics.events.isEmpty())
    }

    @Test
    fun `contact request is tracked only when the request succeeds`() = runTest {
        requestRepository.createResult = Result.failure(RuntimeException("offline"))
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.requestContact(categoryName = "Electricista")
        advanceUntilIdle()
        assertTrue(analytics.eventsNamed(AnalyticsEvents.CONTACT_REQUEST).isEmpty())

        requestRepository.createResult = Result.success(Unit)
        viewModel.requestContact(categoryName = "Electricista")
        advanceUntilIdle()

        val event = analytics.eventsNamed(AnalyticsEvents.CONTACT_REQUEST).single()
        assertEquals("provider-1", event.params[AnalyticsParams.PROVIDER_ID])
        assertEquals("electricistas", event.params[AnalyticsParams.CATEGORY_ID])
        assertEquals("false", event.params[AnalyticsParams.FROM_ASSISTANT])
    }

    @Test
    fun `suggested message prefills an empty field and marks the request`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.applySuggestedMessage("Hola, se me cortó la luz en la cocina.")
        assertEquals("Hola, se me cortó la luz en la cocina.", viewModel.uiState.value.message)
        viewModel.requestContact(categoryName = "Electricista")
        advanceUntilIdle()

        assertEquals("Hola, se me cortó la luz en la cocina.", requestRepository.createdRequests.single().message)
        val event = analytics.eventsNamed(AnalyticsEvents.CONTACT_REQUEST).single()
        assertEquals("true", event.params[AnalyticsParams.FROM_ASSISTANT])
    }

    @Test
    fun `suggested message never replaces what the user typed`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onMessageChange("Mi propio mensaje")

        viewModel.applySuggestedMessage("Mensaje de la IA")

        assertEquals("Mi propio mensaje", viewModel.uiState.value.message)
    }

    @Test
    fun `suggested message is applied only once`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.applySuggestedMessage("Mensaje de la IA")
        viewModel.onMessageChange("")

        viewModel.applySuggestedMessage("Mensaje de la IA")
        viewModel.requestContact(categoryName = "Electricista")
        advanceUntilIdle()

        assertEquals("", viewModel.uiState.value.message)
        val event = analytics.eventsNamed(AnalyticsEvents.CONTACT_REQUEST).single()
        assertEquals("false", event.params[AnalyticsParams.FROM_ASSISTANT])
    }

    @Test
    fun `whatsapp opens wa me with the typed message and tracks the channel`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onMessageChange("Necesito un enchufe")

        viewModel.contactByWhatsApp()

        assertEquals(
            "https://wa.me/5493564000000?text=Hola%20Carlos%20Electricidad%2C%20te%20encontr%C3%A9%20en%20" +
                "ServiciosYA.%20Necesito%20un%20enchufe",
            contactLauncher.whatsAppUrls.single(),
        )
        val event = analytics.eventsNamed(AnalyticsEvents.CONTACT_CHANNEL).single()
        assertEquals(ContactChannel.WHATSAPP.value, event.params[AnalyticsParams.CHANNEL])
        assertEquals("provider-1", event.params[AnalyticsParams.PROVIDER_ID])
        assertEquals("electricistas", event.params[AnalyticsParams.CATEGORY_ID])
    }

    @Test
    fun `whatsapp with an invalid phone shows a message and opens nothing`() = runTest {
        repository.providerResult = Result.success(testProvider().copy(phone = "123"))
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.contactByWhatsApp()

        assertTrue(contactLauncher.whatsAppUrls.isEmpty())
        assertEquals(ProviderDetailViewModel.INVALID_PHONE_MESSAGE, viewModel.uiState.value.userMessage)
        assertTrue(analytics.eventsNamed(AnalyticsEvents.CONTACT_CHANNEL).isEmpty())
    }

    @Test
    fun `call opens the dialer with the provider phone`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.call()

        assertEquals("3564-000000", contactLauncher.dialedPhones.single())
        val event = analytics.eventsNamed(AnalyticsEvents.CONTACT_CHANNEL).single()
        assertEquals(ContactChannel.CALL.value, event.params[AnalyticsParams.CHANNEL])
    }

    @Test
    fun `share sends the provider card text`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.share(categoryName = "Electricista")

        assertTrue(contactLauncher.sharedTexts.single().startsWith("Carlos Electricidad - Electricista"))
        val event = analytics.eventsNamed(AnalyticsEvents.CONTACT_CHANNEL).single()
        assertEquals(ContactChannel.SHARE.value, event.params[AnalyticsParams.CHANNEL])
    }

    @Test
    fun `failed launch shows an error and is not tracked`() = runTest {
        contactLauncher.result = false
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.contactByWhatsApp()

        assertEquals(ProviderDetailViewModel.WHATSAPP_ERROR_MESSAGE, viewModel.uiState.value.userMessage)
        assertTrue(analytics.eventsNamed(AnalyticsEvents.CONTACT_CHANNEL).isEmpty())
    }

    @Test
    fun `contact buttons do nothing while the provider is not loaded`() = runTest {
        val viewModel = createViewModel()

        viewModel.contactByWhatsApp()
        viewModel.call()
        viewModel.share(categoryName = "Electricista")

        assertTrue(contactLauncher.whatsAppUrls.isEmpty())
        assertTrue(contactLauncher.dialedPhones.isEmpty())
        assertTrue(contactLauncher.sharedTexts.isEmpty())
    }

    @Test
    fun `analytics events never include personal data`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onMessageChange("Mi teléfono es 3564-111111")
        viewModel.requestContact(categoryName = "Electricista")
        advanceUntilIdle()
        viewModel.contactByWhatsApp()
        viewModel.call()

        val allValues = analytics.events.flatMap { it.params.values }
        assertTrue(allValues.none { it.contains("3564") || it.contains("Carlos") || it.contains("user-") })
    }
}
