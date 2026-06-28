package app.btccompass.android.widget

import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.color.DynamicThemeColorProviders
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import app.btccompass.android.MainActivity
import app.btccompass.android.data.api.dto.ScoreDto
import java.text.NumberFormat
import java.util.Locale

// Keys for the per-widget Glance DataStore snapshot (render projection)
internal val KEY_SCORE = floatPreferencesKey("widget_score")
internal val KEY_BAND_NAME = stringPreferencesKey("widget_band_name")
internal val KEY_BAND_COLOR = stringPreferencesKey("widget_band_color")
internal val KEY_PRICE_USD = floatPreferencesKey("widget_price_usd")
// Absolute instant when the underlying on-chain data was computed; age is derived at render time.
internal val KEY_DATA_AS_OF_EPOCH_MS = longPreferencesKey("widget_data_as_of_epoch_ms")

private const val STALE_THRESHOLD_MINUTES = 8 * 60L

/** Projects a [ScoreDto] snapshot into the Glance widget's per-instance preferences store. */
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

class CompassWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val themeColors = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                DynamicThemeColorProviders
            } else {
                GlanceTheme.colors
            }
            GlanceTheme(colors = themeColors) {
                WidgetRoot(prefs)
            }
        }
    }
}

@Composable
private fun WidgetRoot(prefs: Preferences) {
    val score = prefs[KEY_SCORE]
    val bandName = prefs[KEY_BAND_NAME]

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.background)
            .padding(12.dp)
            .clickable(actionStartActivity<MainActivity>()),
        contentAlignment = Alignment.Center,
    ) {
        if (score == null || bandName == null) {
            EmptyContent()
        } else {
            ScoreContent(prefs = prefs, score = score, bandName = bandName)
        }
    }
}

@Composable
private fun EmptyContent() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "—",
            style = TextStyle(
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = GlanceTheme.colors.onBackground,
            ),
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = "Updating…",
            style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onBackground),
        )
    }
}

@Composable
private fun ScoreContent(prefs: Preferences, score: Float, bandName: String) {
    val bandColorHex = prefs[KEY_BAND_COLOR]
    val priceUsd = prefs[KEY_PRICE_USD]
    val dataAsOfEpochMs = prefs[KEY_DATA_AS_OF_EPOCH_MS]

    val bandColor: ColorProvider = bandColorHex
        ?.let { hex -> runCatching { ColorProvider(Color(android.graphics.Color.parseColor(hex))) }.getOrNull() }
        ?: GlanceTheme.colors.primary

    val priceText = priceUsd?.let {
        "$" + NumberFormat.getNumberInstance(Locale.US).format(it.toLong())
    } ?: "—"

    // Age computed at render time so the label advances correctly between refreshes.
    val staleLabel = dataAsOfEpochMs?.let { epoch ->
        val ageMinutes = (System.currentTimeMillis() - epoch) / 60_000L
        when {
            ageMinutes > STALE_THRESHOLD_MINUTES -> "⚠ Data stale"
            ageMinutes < 60 -> "Updated ${ageMinutes}m ago"
            else -> "Updated ${ageMinutes / 60}h ago"
        }
    } ?: ""

    Column(
        modifier = GlanceModifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "%.2f".format(score),
            style = TextStyle(
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = GlanceTheme.colors.onBackground,
            ),
        )
        Spacer(modifier = GlanceModifier.height(2.dp))
        Text(
            text = bandName,
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = bandColor,
            ),
        )
        Spacer(modifier = GlanceModifier.height(6.dp))
        Text(
            text = priceText,
            style = TextStyle(fontSize = 14.sp, color = GlanceTheme.colors.onBackground),
        )
        if (staleLabel.isNotEmpty()) {
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = staleLabel,
                style = TextStyle(fontSize = 10.sp, color = GlanceTheme.colors.onBackground),
            )
        }
    }
}

class CompassWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CompassWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        ScoreRefreshWorker.enqueueOneTimeRefresh(context)
        ScoreRefreshWorker.enqueuePeriodicRefresh(context)
    }
}
