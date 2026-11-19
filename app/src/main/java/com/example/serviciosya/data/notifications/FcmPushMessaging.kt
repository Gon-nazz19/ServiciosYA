package com.example.serviciosya.data.notifications

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.example.serviciosya.domain.notifications.PushMessaging
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

class FcmPushMessaging(
    private val context: Context,
    private val messaging: FirebaseMessaging,
) : PushMessaging {
    override fun areNotificationsEnabled(): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    override suspend fun subscribe(topic: String): Boolean = call(topic) { messaging.subscribeToTopic(topic).await() }

    override suspend fun unsubscribe(topic: String): Boolean = call(topic) { messaging.unsubscribeFromTopic(topic).await() }

    /** A failed subscription (no network) is retried the next time the screen records the activity. */
    private suspend fun call(topic: String, block: suspend () -> Unit): Boolean = try {
        block()
        true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "Topic operation failed: $topic", e)
        false
    }

    private companion object {
        const val TAG = "ServiciosYaPush"
    }
}

/** Used when Firebase is not configured: the app works the same, only without push campaigns. */
class NoOpPushMessaging : PushMessaging {
    override fun areNotificationsEnabled(): Boolean = false

    override suspend fun subscribe(topic: String): Boolean = false

    override suspend fun unsubscribe(topic: String): Boolean = false
}
