package com.example.serviciosya.data.appcheck

import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

/** Release builds prove they are the real app with Play Integrity. */
@Suppress("UNUSED_PARAMETER")
fun appCheckProviderFactory(app: FirebaseApp): AppCheckProviderFactory = PlayIntegrityAppCheckProviderFactory.getInstance()
