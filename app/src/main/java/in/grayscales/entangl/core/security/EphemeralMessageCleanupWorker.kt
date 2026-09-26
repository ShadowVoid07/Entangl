package `in`.grayscales.entangl.core.security

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import `in`.grayscales.entangl.data.local.dao.MessageDao
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

/**
 * Background worker scheduled via WorkManager to purge expired self-destruct messages.
 * Runs periodically (every 15 minutes) to ensure that expired messages are eliminated from disk
 * even when the application is backgrounded or inactive.
 */
class EphemeralMessageCleanupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val messageDao: MessageDao by inject()

    override suspend fun doWork(): Result {
        return try {
            val now = System.currentTimeMillis()
            messageDao.deleteExpired(now)
            Log.d(TAG, "Ephemeral message cleanup cycle completed successfully at $now")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to purge expired ephemeral messages", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "EphemeralCleanup"
        private const val WORK_NAME = "entangl_ephemeral_cleanup_work"

        fun schedule(context: Context) {
            val cleanupRequest = PeriodicWorkRequestBuilder<EphemeralMessageCleanupWorker>(
                15, TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                cleanupRequest
            )
        }
    }
}
