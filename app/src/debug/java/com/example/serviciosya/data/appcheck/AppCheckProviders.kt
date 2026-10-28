package com.example.serviciosya.data.appcheck

import android.content.Context
import com.example.serviciosya.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/**
 * Debug builds (emulator) use the App Check debug provider. Without configuration it generates a random
 * token per install that has to be registered in Firebase console > App Check. With
 * APP_CHECK_DEBUG_TOKEN in local.properties every machine of the team reuses one registered token.
 */
fun appCheckProviderFactory(app: FirebaseApp): AppCheckProviderFactory {
    val sharedToken = BuildConfig.APP_CHECK_DEBUG_TOKEN
    if (sharedToken.isNotBlank()) {
        // Same storage the debug provider reads its secret from. Written synchronously so it is
        // in place before the provider looks for it.
        app.applicationContext
            .getSharedPreferences("com.google.firebase.appcheck.debug.store.${app.persistenceKey}", Context.MODE_PRIVATE)
            .edit()
            .putString("com.google.firebase.appcheck.debug.DEBUG_SECRET", sharedToken)
            .commit()
    }
    return DebugAppCheckProviderFactory.getInstance()
}
