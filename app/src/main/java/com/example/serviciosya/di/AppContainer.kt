package com.example.serviciosya.di

import android.content.Context
import com.example.serviciosya.BuildConfig
import com.example.serviciosya.data.ads.AdMobAdsProvider
import com.example.serviciosya.data.ads.NoOpAdsProvider
import com.example.serviciosya.data.analytics.FirebaseAnalyticsTracker
import com.example.serviciosya.data.billing.FirestoreProviderAccountRepository
import com.example.serviciosya.data.billing.FirestoreSubscriptionStore
import com.example.serviciosya.data.billing.SimulatedBillingRepository
import com.example.serviciosya.data.billing.UnavailableBillingRepository
import com.example.serviciosya.data.billing.UnavailableProviderAccountRepository
import com.example.serviciosya.data.analytics.NoOpAnalyticsTracker
import com.example.serviciosya.data.appcheck.appCheckProviderFactory
import com.example.serviciosya.data.assistant.GeminiServiceAssistant
import com.example.serviciosya.data.assistant.UnavailableServiceAssistant
import com.example.serviciosya.data.contact.AndroidContactLauncher
import com.example.serviciosya.data.location.FusedLocationProvider
import com.example.serviciosya.data.location.NoOpLocationProvider
import com.example.serviciosya.data.repository.FirebaseAuthRepository
import com.example.serviciosya.data.repository.FirestoreCategoryRepository
import com.example.serviciosya.data.repository.FirestoreProviderRepository
import com.example.serviciosya.data.repository.FirestoreServiceRequestRepository
import com.example.serviciosya.data.repository.UnavailableAuthRepository
import com.example.serviciosya.data.repository.UnavailableCategoryRepository
import com.example.serviciosya.data.repository.UnavailableProviderRepository
import com.example.serviciosya.data.repository.UnavailableServiceRequestRepository
import com.example.serviciosya.domain.ads.AdsProvider
import com.example.serviciosya.domain.analytics.AnalyticsTracker
import com.example.serviciosya.domain.billing.BillingRepository
import com.example.serviciosya.domain.billing.SimulatedCheckout
import com.example.serviciosya.domain.assistant.ServiceAssistant
import com.example.serviciosya.domain.contact.ContactLauncher
import com.example.serviciosya.domain.location.LocationProvider
import com.example.serviciosya.domain.repository.AuthRepository
import com.example.serviciosya.domain.repository.CategoryRepository
import com.example.serviciosya.domain.repository.ProviderAccountRepository
import com.example.serviciosya.domain.repository.ProviderRepository
import com.example.serviciosya.domain.repository.ServiceRequestRepository
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.LocationServices
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions

class AppContainer(context: Context) {
    private val isFirebaseConfigured = FirebaseApp.getApps(context).isNotEmpty()

    val authRepository: AuthRepository = if (isFirebaseConfigured) {
        FirebaseAuthRepository(
            auth = FirebaseAuth.getInstance(),
            firestore = FirebaseFirestore.getInstance(),
        )
    } else {
        UnavailableAuthRepository()
    }

    val categoryRepository: CategoryRepository = if (isFirebaseConfigured) {
        FirestoreCategoryRepository(FirebaseFirestore.getInstance())
    } else {
        UnavailableCategoryRepository()
    }

    val providerRepository: ProviderRepository = if (isFirebaseConfigured) {
        FirestoreProviderRepository(FirebaseFirestore.getInstance())
    } else {
        UnavailableProviderRepository()
    }

    val serviceRequestRepository: ServiceRequestRepository = if (isFirebaseConfigured) {
        FirestoreServiceRequestRepository(
            context = context,
            firestore = FirebaseFirestore.getInstance(),
            auth = FirebaseAuth.getInstance(),
            functions = if (BuildConfig.CREATE_REQUESTS_WITH_FUNCTION) {
                FirebaseFunctions.getInstance("southamerica-east1")
            } else {
                null
            },
        )
    } else {
        UnavailableServiceRequestRepository()
    }

    val providerAccountRepository: ProviderAccountRepository = if (isFirebaseConfigured) {
        FirestoreProviderAccountRepository(FirebaseFirestore.getInstance())
    } else {
        UnavailableProviderAccountRepository()
    }

    // Simulated payment gateway: it can be replaced by Google Play Billing without touching the screens.
    val billingRepository: BillingRepository = if (isFirebaseConfigured) {
        SimulatedBillingRepository(
            auth = authRepository,
            store = FirestoreSubscriptionStore(FirebaseFirestore.getInstance()),
            providerAccounts = providerAccountRepository,
        )
    } else {
        UnavailableBillingRepository()
    }

    /** The simulated payment screen needs this; a Play Billing implementation would leave it null. */
    val simulatedCheckout: SimulatedCheckout? = billingRepository as? SimulatedCheckout

    val analyticsTracker: AnalyticsTracker = if (isFirebaseConfigured) {
        FirebaseAnalyticsTracker(FirebaseAnalytics.getInstance(context))
    } else {
        NoOpAnalyticsTracker()
    }

    val adsProvider: AdsProvider = if (BuildConfig.ADS_ENABLED) {
        AdMobAdsProvider(
            context = context,
            appId = BuildConfig.ADMOB_APP_ID,
            nativeUnitId = BuildConfig.ADMOB_NATIVE_UNIT_ID,
            analytics = analyticsTracker,
        )
    } else {
        NoOpAdsProvider()
    }

    val serviceAssistant: ServiceAssistant = if (isFirebaseConfigured) {
        // Firebase AI Logic rejects requests without an App Check token.
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(appCheckProviderFactory(FirebaseApp.getInstance()))
        GeminiServiceAssistant.create()
    } else {
        UnavailableServiceAssistant()
    }

    val contactLauncher: ContactLauncher = AndroidContactLauncher(context)

    val locationProvider: LocationProvider =
        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS) {
            FusedLocationProvider(context, LocationServices.getFusedLocationProviderClient(context))
        } else {
            NoOpLocationProvider()
        }
}
