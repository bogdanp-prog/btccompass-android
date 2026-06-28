package app.btccompass.android.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.unit.ColorProvider
import app.btccompass.android.data.api.dto.ScoreDto
import java.text.NumberFormat
import java.util.Locale

// Glance-state preference keys — identical strings reused by all widget sizes.
// Each GlanceAppWidget subclass has its own DataStore file, so there is no key collision.
internal val KEY_SCORE = floatPreferencesKey("widget_score")
internal val KEY_BAND_NAME = stringPreferencesKey("widget_band_name")
internal val KEY_BAND_COLOR = stringPreferencesKey("widget_band_color")
internal val KEY_PRICE_USD = floatPreferencesKey("widget_price_usd")
// Absolute instant when the on-chain data was computed; age is derived at render time.
internal val KEY_DATA_AS_OF_EPOCH_MS = longPreferencesKey("widget_data_as_of_epoch_ms")

internal const val STALE_THRESHOLD_MINUTES = 8 * 60L

/** Projects a [ScoreDto] into the per-widget Glance-state DataStore for [glanceId]. */
internal suspend fun persistScoreSnapshot(
    context: Context,
    glanceId: GlanceId,
    score: ScoreDto,
    fetchTimeMillis: Long,
) {
    val dataAsOf = fetchTimeMillis - (score.staleMinutes * 60_000L)
    updateAppWidgetState(context, glanceId) { prefs: MutablePreferences ->
        prefs[KEY_SCORE] = score.score.toFloat()
        prefs[KEY_BAND_NAME] = score.band.name
        prefs[KEY_BAND_COLOR] = score.band.color
        prefs[KEY_PRICE_USD] = score.price.usd.toFloat()
        prefs[KEY_DATA_AS_OF_EPOCH_MS] = dataAsOf
    }
}

// ── Render helpers ─────────────────────────────────────────────────────────────

internal fun ageMinutes(dataAsOfEpochMs: Long): Long =
    (System.currentTimeMillis() - dataAsOfEpochMs) / 60_000L

internal fun isStale(dataAsOfEpochMs: Long): Boolean =
    ageMinutes(dataAsOfEpochMs) > STALE_THRESHOLD_MINUTES

internal fun staleLabelText(dataAsOfEpochMs: Long): String {
    val age = ageMinutes(dataAsOfEpochMs)
    return when {
        age > STALE_THRESHOLD_MINUTES -> "⚠ Data stale"
        age < 60 -> "Updated ${age}m ago"
        else -> "Updated ${age / 60}h ago"
    }
}

internal fun priceText(priceUsd: Float?): String =
    priceUsd?.let { "$" + NumberFormat.getNumberInstance(Locale.US).format(it.toLong()) } ?: "—"

/** Band color as a fixed [ColorProvider], or null if the hex string is absent/invalid. */
internal fun bandColorProvider(hex: String?): ColorProvider? =
    hex?.let { runCatching { ColorProvider(Color(android.graphics.Color.parseColor(it))) }.getOrNull() }

/**
 * Score color for the 1x1 widget.
 * When stale, the band color is dimmed to 35 % opacity — a visual-only stale indicator that
 * requires no extra text and stays readable on both light and dark backgrounds.
 * Falls back to [fallback] when the band hex is absent or unparseable.
 */
internal fun scoreColorDimmedIfStale(
    bandColorHex: String?,
    stale: Boolean,
    fallback: ColorProvider,
): ColorProvider {
    val raw = bandColorHex?.let {
        runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull()
    } ?: return fallback
    return if (stale) ColorProvider(raw.copy(alpha = 0.35f)) else ColorProvider(raw)
}
