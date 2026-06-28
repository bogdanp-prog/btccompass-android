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
            val score = repository.getScore()
            val manager = GlanceAppWidgetManager(applicationContext)
            val glanceIds = manager.getGlanceIds(CompassWidget::class.java)
            for (id in glanceIds) {
                persistScoreSnapshot(applicationContext, id, score)
                CompassWidget().update(applicationContext, id)
            }
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 2) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val WORK_PERIODIC = "compass_score_refresh_periodic"
        private const val WORK_ONE_TIME = "compass_score_refresh_once"

        private val networkConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        fun enqueuePeriodicRefresh(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<ScoreRefreshWorker>(4, TimeUnit.HOURS)
                    .setConstraints(networkConstraints)
                    .build(),
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
