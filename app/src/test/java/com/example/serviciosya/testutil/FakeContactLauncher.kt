package com.example.serviciosya.testutil

import com.example.serviciosya.domain.contact.ContactLauncher

class FakeContactLauncher(
    var result: Boolean = true,
) : ContactLauncher {
    val whatsAppUrls = mutableListOf<String>()
    val dialedPhones = mutableListOf<String>()
    val sharedTexts = mutableListOf<String>()
    val mapUris = mutableListOf<String>()

    override fun openWhatsApp(url: String): Boolean {
        whatsAppUrls += url
        return result
    }

    override fun dial(phone: String): Boolean {
        dialedPhones += phone
        return result
    }

    override fun share(text: String): Boolean {
        sharedTexts += text
        return result
    }

    override fun openMap(geoUri: String): Boolean {
        mapUris += geoUri
        return result
    }
}
