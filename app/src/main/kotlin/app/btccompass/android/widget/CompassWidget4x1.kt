package app.btccompass.android.widget

import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.DynamicThemeColorProviders
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.wrapContentWidth
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import app.btccompass.android.MainActivity

class CompassWidget4x1 : GlanceAppWidget() {
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
                WidgetRoot4x1(prefs)
            }
        }
    }
}

@Composable
private fun WidgetRoot4x1(prefs: Preferences) {
    val score = prefs[KEY_SCORE]
    val bandName = prefs[KEY_BAND_NAME]

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.background)
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .clickable(actionStartActivity<MainActivity>()),
        contentAlignment = Alignment.Center,
    ) {
        if (score == null || bandName == null) {
            EmptyContent4x1()
        } else {
            ScoreContent4x1(prefs, score, bandName)
        }
    }
}

@Composable
private fun EmptyContent4x1() {
    Row(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "—  Compass",
            style = TextStyle(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = GlanceTheme.colors.onBackground,
            ),
        )
        Spacer(modifier = GlanceModifier.defaultWeight())
        Text(
            text = "Updating…",
            style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onBackground),
        )
    }
}

@Composable
private fun ScoreContent4x1(prefs: Preferences, score: Float, bandName: String) {
    val bandColor = bandColorProvider(prefs[KEY_BAND_COLOR]) ?: GlanceTheme.colors.primary
    val dataAsOfEpochMs = prefs[KEY_DATA_AS_OF_EPOCH_MS]
    val staleLabel = dataAsOfEpochMs?.let { staleLabelText(it) } ?: ""

    Row(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left: score + band name
        Column(
            modifier = GlanceModifier.wrapContentWidth(),
            horizontalAlignment = Alignment.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "%.2f".format(score),
                style = TextStyle(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.onBackground,
                ),
            )
            Text(
                text = bandName,
                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = bandColor),
            )
        }

        // Push right column to the trailing edge
        Spacer(modifier = GlanceModifier.defaultWeight())

        // Right: price + staleness
        Column(
            modifier = GlanceModifier.wrapContentWidth(),
            horizontalAlignment = Alignment.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = priceText(prefs[KEY_PRICE_USD]),
                style = TextStyle(fontSize = 16.sp, color = GlanceTheme.colors.onBackground),
            )
            if (staleLabel.isNotEmpty()) {
                Text(
                    text = staleLabel,
                    style = TextStyle(fontSize = 11.sp, color = GlanceTheme.colors.onBackground),
                )
            }
        }
    }
}

class CompassWidget4x1Receiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CompassWidget4x1()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        ScoreRefreshWorker.enqueueOneTimeRefresh(context)
        ScoreRefreshWorker.enqueuePeriodicRefresh(context)
    }
}
