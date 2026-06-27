package app.btccompass.android.di

import app.btccompass.android.data.api.ScoreApi
import app.btccompass.android.domain.repository.ScoreRepository
import app.btccompass.android.domain.repository.ScoreRepositoryImpl
import app.btccompass.android.ui.home.HomeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { ScoreApi() }
    single<ScoreRepository> { ScoreRepositoryImpl(get()) }
    viewModel { HomeViewModel(get()) }
}
