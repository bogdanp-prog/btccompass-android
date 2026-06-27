package app.btccompass.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.Modifier
import app.btccompass.android.ui.home.HomeScreen
import app.btccompass.android.ui.theme.CompassTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CompassTheme {
                HomeScreen(modifier = Modifier.fillMaxSize().safeDrawingPadding())
            }
        }
    }
}
