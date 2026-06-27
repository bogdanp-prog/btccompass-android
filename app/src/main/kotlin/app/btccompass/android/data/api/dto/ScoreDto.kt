package app.btccompass.android.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ScoreDto(
    val ts: String,
    val score: Double,
    val band: BandDto,
    @SerialName("trend_7d") val trend7d: Double? = null,
    val price: PriceDto,
    val components: ComponentsDto,
    @SerialName("sources_ok") val sourcesOk: SourcesOkDto,
    @SerialName("stale_minutes") val staleMinutes: Int,
    @SerialName("is_stale") val isStale: Boolean,
)

@Serializable
data class BandDto(
    val name: String,
    val color: String,
    val range: List<Int>,
)

@Serializable
data class PriceDto(
    val usd: Double,
    @SerialName("change_24h") val change24h: Double? = null,
    val source: String,
    val ts: String,
)

@Serializable
data class ComponentsDto(
    val cbbi: ComponentDto,
    val fg: ComponentDto,
    val funding: ComponentDto,
    val mayer: ComponentDto,
    val etf: ComponentDto,
)

@Serializable
data class ComponentDto(
    val raw: Double? = null,
    val norm: Double? = null,
    val weight: Double,
    val contribution: Double? = null,
)

@Serializable
data class SourcesOkDto(
    val cbbi: Boolean,
    val fg: Boolean,
    val funding: Boolean,
    val mayer: Boolean,
    val etf: Boolean,
)
