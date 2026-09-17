package thinking.orbs.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import thinking.orbs.compose.OrbTheme
import thinking.orbs.compose.ThinkingOrb

private val STATES = listOf(
    "working", "searching", "solving", "listening", "connecting",
    "weaving", "composing", "breathing", "shaping",
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DemoScreen() }
    }
}

@Composable
fun DemoScreen() {
    var dark by remember { mutableStateOf(true) }
    var speed by remember { mutableFloatStateOf(1f) }
    var paused by remember { mutableStateOf(false) }

    val bg = if (dark) Color(0xFF0B0B0C) else Color(0xFFF7F7F8)
    val fg = if (dark) Color(0xFFEDEDED) else Color(0xFF1A1A1A)
    val theme = if (dark) OrbTheme.Dark else OrbTheme.Light

    Column(
        Modifier
            .fillMaxSize()
            .background(bg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("Thinking Orbs", color = fg, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Dark", color = fg)
            Switch(checked = dark, onCheckedChange = { dark = it })
            Spacer(Modifier.width(16.dp))
            Text("Paused", color = fg)
            Switch(checked = paused, onCheckedChange = { paused = it })
        }
        Text("Speed ${"%.2f".format(speed)}x", color = fg)
        Slider(value = speed, onValueChange = { speed = it }, valueRange = 0.25f..3f)

        Spacer(Modifier.height(8.dp))

        for (state in STATES) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    state,
                    color = fg,
                    modifier = Modifier.width(110.dp),
                )
                ThinkingOrb(
                    state = state,
                    sizeDp = 64,
                    theme = theme,
                    speed = speed,
                    paused = paused,
                )
                Spacer(Modifier.width(24.dp))
                ThinkingOrb(
                    state = state,
                    sizeDp = 20,
                    theme = theme,
                    speed = speed,
                    paused = paused,
                )
            }
        }
    }
}
