package com.example.serviciosya

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.serviciosya.data.notifications.AppNotifications
import com.example.serviciosya.di.AppContainer
import com.example.serviciosya.domain.notifications.NotificationOpen
import com.example.serviciosya.domain.notifications.NotificationRoute
import com.example.serviciosya.ui.theme.ServiciosYATheme

class MainActivity : ComponentActivity() {
    private val container by lazy { AppContainer(applicationContext) }

    /** The notification that opened the app, until the navigation consumes it. */
    private var pendingNotification by mutableStateOf<NotificationOpen?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppNotifications.ensureChannels(this)
        // On a configuration change the intent is the same one already handled.
        if (savedInstanceState == null) pendingNotification = intent.notificationOpen()
        setContent {
            ServiciosYATheme {
                ServiciosYaApp(
                    container = container,
                    pendingNotification = pendingNotification,
                    onNotificationHandled = { pendingNotification = null },
                )
            }
        }
    }

    /** A notification tapped while the app is open (launchMode singleTop). */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.notificationOpen()?.let { pendingNotification = it }
    }
}

/**
 * Our own notifications put these extras in the intent; FCM campaigns that arrive in the background
 * put their data fields (route, campaign_id) there too.
 */
private fun Intent.notificationOpen(): NotificationOpen? {
    val route = getStringExtra(NotificationOpen.EXTRA_ROUTE)
    val campaignId = getStringExtra(NotificationOpen.EXTRA_CAMPAIGN_ID)
    if (route == null && campaignId == null) return null
    return NotificationOpen(
        route = NotificationRoute.parse(route),
        type = getStringExtra(NotificationOpen.EXTRA_TYPE) ?: NotificationOpen.TYPE_CAMPAIGN,
        campaignId = campaignId ?: "unknown",
    )
}
