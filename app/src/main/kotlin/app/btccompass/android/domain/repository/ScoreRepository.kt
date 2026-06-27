package app.btccompass.android.domain.repository

import app.btccompass.android.data.api.ScoreApi
import app.btccompass.android.data.api.dto.ScoreDto

interface ScoreRepository {
    suspend fun getScore(): ScoreDto
}

class ScoreRepositoryImpl(private val api: ScoreApi) : ScoreRepository {
    override suspend fun getScore(): ScoreDto = api.getScore()
}
