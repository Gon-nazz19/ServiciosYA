package com.example.serviciosya.testutil

import com.example.serviciosya.domain.notifications.PushMessaging

class FakePushMessaging(
    var enabled: Boolean = true,
) : PushMessaging {
    val subscribedTopics = mutableSetOf<String>()
    val unsubscribedTopics = mutableListOf<String>()

    override fun areNotificationsEnabled(): Boolean = enabled

    override suspend fun subscribe(topic: String): Boolean {
        subscribedTopics += topic
        return true
    }

    override suspend fun unsubscribe(topic: String): Boolean {
        subscribedTopics -= topic
        unsubscribedTopics += topic
        return true
    }
}
