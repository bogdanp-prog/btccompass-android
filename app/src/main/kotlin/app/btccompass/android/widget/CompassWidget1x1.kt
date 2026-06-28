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
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import app.btccompass.android.MainActivity

class CompassWidget1x1 : GlanceAppWidget() {
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
                WidgetRoot1x1(prefs)
            }
        }
    }
}

@Composable
private fun WidgetRoot1x1(prefs: Preferences) {
    val score = prefs[KEY_SCORE]

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.background)
            .padding(8.dp)
            .clickable(actionStartActivity<MainActivity>()),
        contentAlignment = Alignment.Center,
    ) {
        if (score == null) {
            // Empty cache — show dash placeholder
            Text(
                text = "—",
                style = TextStyle(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.onBackground,
                ),
            )
        } else {
            val dataAsOfEpochMs = prefs[KEY_DATA_AS_OF_EPOCH_MS]
            val stale = dataAsOfEpochMs != null && isStale(dataAsOfEpochMs)
            // Stale indicator: dim the score to 35 % opacity.
            // No room for text; dimming is legible on both light and dark backgrounds.
            val scoreColor = scoreColorDimmedIfStale(
                bandColorHex = prefs[KEY_BAND_COLOR],
                stale = stale,
                fallback = GlanceTheme.colors.onBackground,
            )
            Text(
                text = "%.2f".format(score),
                style = TextStyle(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = scoreColor,
                ),
            )
        }
    }
}

class CompassWidget1x1Receiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CompassWidget1x1()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        ScoreRefreshWorker.enqueueOneTimeRefresh(context)
        ScoreRefreshWorker.enqueuePeriodicRefresh(context)
    }
}
