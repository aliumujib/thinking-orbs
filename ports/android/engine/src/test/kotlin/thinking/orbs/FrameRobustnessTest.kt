package thinking.orbs

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The render clock can momentarily produce a slightly negative or
 * exactly-boundary `t` at startup (the Choreographer frame time can predate
 * the captured origin). On web, `array[-1]` is a harmless no-op, but Kotlin's
 * primitive arrays throw. Every mode frame must be total over all real `t`.
 */
class FrameRobustnessTest {
    private val states = listOf(
        "working", "searching", "solving", "listening", "connecting",
        "weaving", "composing", "breathing", "shaping",
    )

    @Test
    fun everyModeSurvivesNegativeAndBoundaryTimes() {
        // rubik cycle boundary is 2*14*0.42 = 11.76; jitter around it and 0.
        val times = listOf(
            -1.0, -0.5, -0.001, -1e-9, 0.0,
            11.759999999, 11.76, 11.760000001, 12.96, -12.96,
        )
        for (state in states) {
            for (size in listOf(64, 20)) {
                for (t in times) {
                    // must not throw
                    val frame = frameFor(state, size, t)
                    assertTrue(
                        frame.dots.isNotEmpty(),
                        "$state@$size t=$t produced no dots",
                    )
                }
            }
        }
    }
}
