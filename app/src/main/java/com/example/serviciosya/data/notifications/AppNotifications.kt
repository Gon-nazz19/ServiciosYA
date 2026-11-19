package com.example.serviciosya.data.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.serviciosya.MainActivity
import com.example.serviciosya.R
import com.example.serviciosya.domain.notifications.NotificationOpen

/** Notification channels and the shared way of showing a notification that opens a route. */
object AppNotifications {
    const val CHANNEL_NEWS = "novedades"
    const val CHANNEL_REMINDERS = "recordatorios"

    /** Idempotent: safe to call on every app start and before every notification. */
    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(CHANNEL_NEWS, "Novedades", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Prestadores nuevos y novedades de las categorías que te interesan"
                },
                NotificationChannel(CHANNEL_REMINDERS, "Recordatorios", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Seguimiento de los prestadores que contactaste"
                },
            ),
        )
    }

    /** Intent extras that MainActivity reads to open [route] and track the notification. */
    fun openIntent(context: Context, route: String?, type: String, campaignId: String): Intent =
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(NotificationOpen.EXTRA_ROUTE, route)
            putExtra(NotificationOpen.EXTRA_TYPE, type)
            putExtra(NotificationOpen.EXTRA_CAMPAIGN_ID, campaignId)
        }

    /** Returns false when the user did not allow notifications. */
    fun show(
        context: Context,
        notificationId: Int,
        channelId: String,
        title: String,
        body: String,
        open: Intent,
    ): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        ensureChannels(context)
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            open,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        return try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            true
        } catch (_: SecurityException) {
            false // Permission revoked between the check and the call.
        }
    }
}
