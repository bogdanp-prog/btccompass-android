package app.btccompass.android.domain.repository

import app.btccompass.android.data.api.ScoreApi
import app.btccompass.android.data.api.dto.ScoreDto
import app.btccompass.android.data.cache.ScoreCache
import app.btccompass.android.domain.model.ScoreSnapshot
import kotlinx.coroutines.flow.Flow

interface ScoreRepository {
    suspend fun getScore(): ScoreDto
    fun observeCachedScore(): Flow<ScoreSnapshot?>
}

class ScoreRepositoryImpl(
    private val api: ScoreApi,
    private val cache: ScoreCache,
) : ScoreRepository {

    override suspend fun getScore(): ScoreDto {
        val score = api.getScore()
        cache.write(score, System.currentTimeMillis())
        return score
    }

    override fun observeCachedScore(): Flow<ScoreSnapshot?> = cache.observe()
}
