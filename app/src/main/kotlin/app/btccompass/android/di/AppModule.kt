package app.btccompass.android.di

import app.btccompass.android.data.api.ScoreApi
import app.btccompass.android.data.cache.ScoreCache
import app.btccompass.android.domain.repository.ScoreRepository
import app.btccompass.android.domain.repository.ScoreRepositoryImpl
import app.btccompass.android.ui.home.HomeViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { ScoreApi() }
    single { ScoreCache(androidContext()) }
    single<ScoreRepository> { ScoreRepositoryImpl(get(), get()) }
    viewModel { HomeViewModel(get()) }
}
