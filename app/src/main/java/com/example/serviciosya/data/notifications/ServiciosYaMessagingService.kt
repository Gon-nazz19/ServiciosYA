package com.example.serviciosya.data.notifications

import android.util.Log
import com.example.serviciosya.BuildConfig
import com.example.serviciosya.domain.notifications.NotificationOpen
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Receives FCM messages. In the background Android shows campaign notifications by itself and passes
 * the data fields (route, campaign_id) to MainActivity; this service handles the foreground case so
 * the notification looks and behaves the same.
 */
class ServiciosYaMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: return
        val body = message.notification?.body ?: message.data["body"].orEmpty()
        val campaignId = message.data[NotificationOpen.EXTRA_CAMPAIGN_ID] ?: "unknown"
        AppNotifications.show(
            context = this,
            notificationId = campaignId.hashCode(),
            channelId = message.notification?.channelId ?: AppNotifications.CHANNEL_NEWS,
            title = title,
            body = body,
            open = AppNotifications.openIntent(
                context = this,
                route = message.data[NotificationOpen.EXTRA_ROUTE],
                type = NotificationOpen.TYPE_CAMPAIGN,
                campaignId = campaignId,
            ),
        )
    }

    /**
     * Campaigns go to topics, so the token is not stored anywhere. It is logged in debug builds to use
     * "Send test message" in the Firebase console.
     */
    override fun onNewToken(token: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, "FCM registration token: $token")
    }

    private companion object {
        const val TAG = "ServiciosYaPush"
    }
}
