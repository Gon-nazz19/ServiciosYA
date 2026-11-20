package com.example.serviciosya.testutil

import com.example.serviciosya.domain.notifications.ReminderScheduler

class FakeReminderScheduler : ReminderScheduler {
    val scheduled = mutableListOf<Pair<String, String>>()

    override fun scheduleFollowUp(providerId: String, providerName: String) {
        scheduled += providerId to providerName
    }
}
