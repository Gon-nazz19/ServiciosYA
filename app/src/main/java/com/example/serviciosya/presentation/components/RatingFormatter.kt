package com.example.serviciosya.presentation.components

import java.util.Locale

fun formatRating(rating: Double?): String? =
    rating?.takeIf { it > 0.0 }?.let { "★ " + String.format(Locale.ROOT, "%.1f", it) }
