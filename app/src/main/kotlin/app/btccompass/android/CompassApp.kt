package app.btccompass.android

import android.app.Application
import app.btccompass.android.di.appModule
import app.btccompass.android.widget.ScoreRefreshWorker
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class CompassApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@CompassApp)
            modules(appModule)
        }
        // Ensure the periodic refresh job is always registered with current constraints,
        // even after a reinstall, app update, or pm clear that wiped WorkManager's DB.
        ScoreRefreshWorker.enqueuePeriodicRefresh(this)
    }
}
