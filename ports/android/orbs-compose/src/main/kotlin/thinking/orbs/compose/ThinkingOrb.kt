package thinking.orbs.compose

import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import thinking.orbs.LABELS
import thinking.orbs.OrbFrame
import thinking.orbs.REDUCED_MOTION_T
import thinking.orbs.frameFor
import thinking.orbs.inkGrey
import thinking.orbs.resolvePreset

enum class OrbTheme { Auto, Dark, Light }

/** Snap an arbitrary logical size to the nearest shipped preset (64 or 20). */
private fun drawSizePreset(sizeDp: Int): Int = if (sizeDp <= 42) 20 else 64

/**
 * Jetpack Compose ThinkingOrb.
 *
 * Geometry is the Kotlin transcription of `spec/orbs-spec.json` (same
 * contract as the planned Swift port): filled circles for dots, strokes
 * for the `connecting` web. Golden-vector tests live in the JVM engine
 * module and do not need an emulator.
 *
 * `sizeDp` is the logical preset — 64 (chat-avatar) or 20 (inline). The two
 * presets are separate tunings, not a scale factor; any other value snaps to
 * the nearest one. The finished draw list is then scaled to the Canvas's real
 * pixel size so the orb fills its box at any display density.
 */
@Composable
fun ThinkingOrb(
    state: String = "working",
    sizeDp: Int = 64,
    theme: OrbTheme = OrbTheme.Auto,
    speed: Float = 1f,
    paused: Boolean = false,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val dark = when (theme) {
        OrbTheme.Auto -> systemDark
        OrbTheme.Dark -> true
        OrbTheme.Light -> false
    }
    val reduced = remember(context) {
        val scale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )
        scale == 0f
    }
    val preset = drawSizePreset(sizeDp)
    val resolved = remember(state, preset) { resolvePreset(state, preset) }
    val effSpeed = resolved.speed * speed
    var t by remember { mutableStateOf(if (reduced) REDUCED_MOTION_T else 0.0) }

    LaunchedEffect(paused, reduced, effSpeed, state, preset) {
        if (reduced) {
            t = REDUCED_MOTION_T
            return@LaunchedEffect
        }
        // Seed the origin from the first frame's own timestamp, not
        // System.nanoTime(): withFrameNanos' `now` (Choreographer frame time)
        // can predate a separately-sampled nanoTime, yielding a negative
        // elapsed on the first frame. Guarding at >= 0 keeps the shared engine
        // clock non-negative (the web's performance.now() is always positive).
        var origin = -1L
        while (true) {
            withFrameNanos { now ->
                if (origin < 0L) origin = now
                if (!paused) {
                    val elapsed = ((now - origin).coerceAtLeast(0L)) / 1_000_000_000.0
                    t = elapsed * effSpeed
                }
            }
        }
    }

    val label = contentDescription ?: LABELS[state] ?: "Thinking…"

    Canvas(
        modifier
            .size(sizeDp.dp)
            .semantics { this.contentDescription = label },
    ) {
        // Geometry is tuned in the logical preset unit (64/20). Draw at the
        // canvas's real pixel size so the orb fills the box at any density,
        // capping the effective scale at 2x to match the web DPR cap.
        val px = size.minDimension
        val scale = (px / sizeDp.toFloat()).coerceAtMost(2f)
        val drawSize = (sizeDp * scale)
        val frame: OrbFrame = frameFor(state, preset, t)
        val k = drawSize / preset.toFloat() // px per logical (preset) unit
        val pad = (px - drawSize) / 2f

        for (l in frame.lines) {
            val g = inkGrey(l.white, dark) / 255f
            drawLine(
                color = Color(g, g, g, l.a.toFloat()),
                start = Offset(pad + l.x1.toFloat() * k, pad + l.y1.toFloat() * k),
                end = Offset(pad + l.x2.toFloat() * k, pad + l.y2.toFloat() * k),
                strokeWidth = (l.w.toFloat() * k).coerceAtLeast(1f),
                cap = StrokeCap.Butt,
            )
        }
        for (d in frame.dots) {
            val g = inkGrey(d.white, dark) / 255f
            drawCircle(
                color = Color(g, g, g, d.a.toFloat()),
                radius = d.r.toFloat() * k,
                center = Offset(pad + d.x.toFloat() * k, pad + d.y.toFloat() * k),
            )
        }
    }
}
