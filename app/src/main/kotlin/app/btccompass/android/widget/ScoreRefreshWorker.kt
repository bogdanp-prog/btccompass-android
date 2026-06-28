package app.btccompass.android.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.btccompass.android.domain.repository.ScoreRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

class ScoreRefreshWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val repository: ScoreRepository by inject()

    override suspend fun doWork(): Result {
        return try {
            val fetchTime = System.currentTimeMillis()
            // getScore() also write-through caches into the app-level ScoreCache DataStore.
            val score = repository.getScore()
            // Fan-out: persist snapshot + re-render every placed instance of every widget size.
            // Single fetch, single schedule, all sizes updated in one worker run.
            persistAndRenderAll(applicationContext, score, fetchTime)
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 2) return Result.retry()
            // Fetch failed permanently. Re-render with the unchanged snapshot so the age label
            // can advance and cross the stale threshold — do NOT overwrite the stored instant.
            reRenderAll(applicationContext)
            Result.failure()
        }
    }

    companion object {
        private const val WORK_PERIODIC = "compass_score_refresh_periodic"
        private const val WORK_ONE_TIME = "compass_score_refresh_once"

        private val networkConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        fun enqueuePeriodicRefresh(context: Context) {
            // No network constraint: the worker runs every 4h regardless of connectivity.
            // On success it updates the snapshot; on failure it re-renders with the stored
            // snapshot so the age label can advance and escalate to "⚠ Data stale" offline.
            // UPDATE policy so constraints stay current across reinstalls and app updates.
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_PERIODIC,
                ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequestBuilder<ScoreRefreshWorker>(4, TimeUnit.HOURS).build(),
            )
        }

        fun enqueueOneTimeRefresh(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_ONE_TIME,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<ScoreRefreshWorker>()
                    .setConstraints(networkConstraints)
                    .build(),
            )
        }
    }
}

private suspend fun persistAndRenderAll(
    context: Context,
    score: app.btccompass.android.data.api.dto.ScoreDto,
    fetchTimeMillis: Long,
) {
    val manager = GlanceAppWidgetManager(context)
    for (id in manager.getGlanceIds(CompassWidget::class.java)) {
        persistScoreSnapshot(context, id, score, fetchTimeMillis)
        CompassWidget().update(context, id)
    }
    for (id in manager.getGlanceIds(CompassWidget1x1::class.java)) {
        persistScoreSnapshot(context, id, score, fetchTimeMillis)
        CompassWidget1x1().update(context, id)
    }
    for (id in manager.getGlanceIds(CompassWidget4x1::class.java)) {
        persistScoreSnapshot(context, id, score, fetchTimeMillis)
        CompassWidget4x1().update(context, id)
    }
}

private suspend fun reRenderAll(context: Context) {
    val manager = GlanceAppWidgetManager(context)
    for (id in manager.getGlanceIds(CompassWidget::class.java)) CompassWidget().update(context, id)
    for (id in manager.getGlanceIds(CompassWidget1x1::class.java)) CompassWidget1x1().update(context, id)
    for (id in manager.getGlanceIds(CompassWidget4x1::class.java)) CompassWidget4x1().update(context, id)
}
