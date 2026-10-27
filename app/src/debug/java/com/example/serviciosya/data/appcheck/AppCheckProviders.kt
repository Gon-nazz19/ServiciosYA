package com.example.serviciosya.data.appcheck

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/**
 * Debug builds (emulator) use the App Check debug provider. On first launch it prints a token to
 * Logcat ("DebugAppCheckProvider") that has to be registered in Firebase console > App Check.
 */
fun appCheckProviderFactory(): AppCheckProviderFactory = DebugAppCheckProviderFactory.getInstance()
