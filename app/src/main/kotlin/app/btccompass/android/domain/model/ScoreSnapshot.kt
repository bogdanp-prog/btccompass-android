package app.btccompass.android.domain.model

/**
 * Minimal projection of the latest successful score fetch, used as the shared cache model.
 * [dataAsOfEpochMillis] is the absolute instant when the underlying on-chain data was computed:
 *   fetchTimeMillis - (staleMinutes * 60_000)
 * Consumers derive a live "Updated Xm ago" label from this value at render time.
 */
data class ScoreSnapshot(
    val score: Double,
    val bandName: String,
    val bandColor: String,
    val priceUsd: Double,
    val dataAsOfEpochMillis: Long,
)
