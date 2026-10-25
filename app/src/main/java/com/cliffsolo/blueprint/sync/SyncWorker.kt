package com.cliffsolo.blueprint.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.cliffsolo.blueprint.BlueprintApp
import java.util.concurrent.TimeUnit

/** Checks GitHub for new or updated courses in the background, a few times a day, only on a network. */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as BlueprintApp
        val settings = app.settings.current()
        if (!settings.autoSync) return Result.success()
        return runCatching { app.sync.sync(settings.githubOwner) }
            .fold({ Result.success() }, { if (runAttemptCount < 3) Result.retry() else Result.failure() })
    }

    companion object {
        private const val NAME = "course-sync"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(12, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun cancel(context: Context) = WorkManager.getInstance(context).cancelUniqueWork(NAME)
    }
}
