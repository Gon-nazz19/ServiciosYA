package com.example.serviciosya.data.contact

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.serviciosya.domain.contact.ContactLauncher

/**
 * Uses Android intents, so no extra permission is needed: ACTION_DIAL only opens the dialer and
 * a wa.me link opens WhatsApp, or the browser when WhatsApp is not installed.
 */
class AndroidContactLauncher(
    private val context: Context,
) : ContactLauncher {
    override fun openWhatsApp(url: String): Boolean =
        start(Intent(Intent.ACTION_VIEW, Uri.parse(url)))

    override fun dial(phone: String): Boolean =
        start(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phone, null)))

    override fun share(text: String): Boolean {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        return start(Intent.createChooser(send, "Compartir prestador"))
    }

    private fun start(intent: Intent): Boolean = try {
        // The application context has no task of its own.
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
