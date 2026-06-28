package app.btccompass.android.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.btccompass.android.domain.model.ScoreSnapshot
import org.koin.androidx.compose.koinViewModel
import java.text.NumberFormat
import java.util.Locale

private const val STALE_THRESHOLD_MINUTES = 8 * 60L

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        when (val state = uiState) {
            HomeUiState.Loading -> CircularProgressIndicator()
            is HomeUiState.Error -> ErrorContent(message = state.message, onRetry = viewModel::retry)
            is HomeUiState.Success -> ScoreContent(state.snapshot)
        }
    }
}

@Composable
private fun ScoreContent(snapshot: ScoreSnapshot) {
    val bandColor = runCatching { Color(android.graphics.Color.parseColor(snapshot.bandColor)) }
        .getOrDefault(MaterialTheme.colorScheme.primary)
    val priceFormatted = NumberFormat.getNumberInstance(Locale.US).format(snapshot.priceUsd.toLong())

    val ageMinutes = (System.currentTimeMillis() - snapshot.dataAsOfEpochMillis) / 60_000L
    val staleLabel = when {
        ageMinutes > STALE_THRESHOLD_MINUTES -> "⚠ Data stale"
        ageMinutes < 60 -> "Updated ${ageMinutes}m ago"
        else -> "Updated ${ageMinutes / 60}h ago"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(24.dp),
    ) {
        Text(
            text = "%.2f".format(snapshot.score),
            fontSize = 72.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = snapshot.bandName,
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
            color = bandColor,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "$$priceFormatted",
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = staleLabel,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(24.dp),
    ) {
        Text(
            text = "Failed to load score",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}
