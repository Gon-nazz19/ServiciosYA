package com.example.serviciosya.data.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.serviciosya.domain.notifications.NotificationOpen
import com.example.serviciosya.domain.notifications.NotificationRoute
import com.example.serviciosya.domain.notifications.ReminderScheduler
import java.util.concurrent.TimeUnit

/**
 * Local notification some time after a contact, without any server: WorkManager keeps it scheduled
 * even if the app is closed or the phone restarts. One reminder per provider; contacting the same
 * provider again moves it forward.
 */
class WorkManagerReminderScheduler(
    private val context: Context,
    private val delayMinutes: Long,
) : ReminderScheduler {
    override fun scheduleFollowUp(providerId: String, providerName: String) {
        val request = OneTimeWorkRequestBuilder<FollowUpReminderWorker>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .setInputData(workDataOf(KEY_PROVIDER_ID to providerId, KEY_PROVIDER_NAME to providerName))
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork("follow-up-$providerId", ExistingWorkPolicy.REPLACE, request)
    }

    companion object {
        const val KEY_PROVIDER_ID = "provider_id"
        const val KEY_PROVIDER_NAME = "provider_name"
    }
}

class FollowUpReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val providerId = inputData.getString(WorkManagerReminderScheduler.KEY_PROVIDER_ID) ?: return Result.failure()
        val providerName = inputData.getString(WorkManagerReminderScheduler.KEY_PROVIDER_NAME).orEmpty()
        AppNotifications.show(
            context = applicationContext,
            notificationId = providerId.hashCode(),
            channelId = AppNotifications.CHANNEL_REMINDERS,
            title = "¿Pudiste resolverlo?",
            body = "Contactaste a ${providerName.ifBlank { "un prestador" }}. Si todavía no te respondió, " +
                "escribile de nuevo o buscá otra opción cerca tuyo.",
            open = AppNotifications.openIntent(
                context = applicationContext,
                route = NotificationRoute.provider(providerId),
                type = NotificationOpen.TYPE_REMINDER,
                campaignId = CAMPAIGN_ID,
            ),
        )
        return Result.success()
    }

    companion object {
        const val CAMPAIGN_ID = "follow_up"
    }
}
