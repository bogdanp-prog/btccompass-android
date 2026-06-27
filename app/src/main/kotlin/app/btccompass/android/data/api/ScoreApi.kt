package app.btccompass.android.data.api

import app.btccompass.android.BuildConfig
import app.btccompass.android.data.api.dto.ScoreDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private const val BASE_URL = "https://api.btccompass.app"

class ScoreApi {
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
        install(Logging) {
            level = LogLevel.HEADERS
        }
        defaultRequest {
            header(HttpHeaders.UserAgent, "Compass-Android/${BuildConfig.VERSION_NAME}")
        }
    }

    suspend fun getScore(): ScoreDto =
        client.get("$BASE_URL/api/score").body()
}
