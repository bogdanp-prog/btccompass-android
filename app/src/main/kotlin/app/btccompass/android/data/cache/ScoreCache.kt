package app.btccompass.android.data.cache

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.btccompass.android.data.api.dto.ScoreDto
import app.btccompass.android.domain.model.ScoreSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.scoreDataStore: DataStore<Preferences> by preferencesDataStore(name = "score_cache")

private val KEY_SCORE = floatPreferencesKey("score")
private val KEY_BAND_NAME = stringPreferencesKey("band_name")
private val KEY_BAND_COLOR = stringPreferencesKey("band_color")
private val KEY_PRICE_USD = floatPreferencesKey("price_usd")
private val KEY_DATA_AS_OF_EPOCH_MS = longPreferencesKey("data_as_of_epoch_ms")

class ScoreCache(context: Context) {
    private val store = context.scoreDataStore

    suspend fun write(score: ScoreDto, fetchTimeMillis: Long) {
        val dataAsOf = fetchTimeMillis - (score.staleMinutes * 60_000L)
        store.edit { prefs ->
            prefs[KEY_SCORE] = score.score.toFloat()
            prefs[KEY_BAND_NAME] = score.band.name
            prefs[KEY_BAND_COLOR] = score.band.color
            prefs[KEY_PRICE_USD] = score.price.usd.toFloat()
            prefs[KEY_DATA_AS_OF_EPOCH_MS] = dataAsOf
        }
    }

    fun observe(): Flow<ScoreSnapshot?> = store.data.map { prefs ->
        ScoreSnapshot(
            score = (prefs[KEY_SCORE] ?: return@map null).toDouble(),
            bandName = prefs[KEY_BAND_NAME] ?: return@map null,
            bandColor = prefs[KEY_BAND_COLOR] ?: return@map null,
            priceUsd = (prefs[KEY_PRICE_USD] ?: return@map null).toDouble(),
            dataAsOfEpochMillis = prefs[KEY_DATA_AS_OF_EPOCH_MS] ?: return@map null,
        )
    }
}
