package app.btccompass.android

import android.app.Application
import app.btccompass.android.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class CompassApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@CompassApp)
            modules(appModule)
        }
    }
}
