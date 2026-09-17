package thinking.orbs

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

data class Dot(
    var x: Double,
    var y: Double,
    var z: Double,
    var r: Double,
    var white: Double,
    var a: Double = 1.0,
)

data class Line(
    var x1: Double,
    var y1: Double,
    var x2: Double,
    var y2: Double,
    var white: Double,
    var a: Double = 1.0,
    var w: Double,
)

data class OrbFrame(
    val dots: List<Dot>,
    val lines: List<Line>,
)

typealias Projector = (Double, Double, Double) -> Triple<Double, Double, Double>

fun lerp(a: Double, b: Double, f: Double): Double = a + (b - a) * f

fun frac(x: Double): Double = x - floor(x)

fun hashD(a: Double, b: Double): Double {
    val h = sin(a * 12.9898 + b * 78.233) * 43758.5453
    return h - floor(h)
}

fun vnoise(x: Double, y: Double): Double {
    val xi = floor(x)
    val yi = floor(y)
    var fx = x - xi
    var fy = y - yi
    fx = fx * fx * (3 - 2 * fx)
    fy = fy * fy * (3 - 2 * fy)
    val aa = hashD(xi, yi)
    val bb = hashD(xi + 1, yi)
    val c = hashD(xi, yi + 1)
    val d = hashD(xi + 1, yi + 1)
    return aa + (bb - aa) * fx + (c - aa) * fy + (aa - bb - c + d) * fx * fy
}

fun fibDir(i: Double, n: Double): Triple<Double, Double, Double> {
    val golden = PI * (3 - sqrt(5.0))
    val y = 1 - (2 * (i + 0.5)) / n
    val rad = sqrt(1 - y * y)
    val a = i * golden
    return Triple(rad * cos(a), y, rad * sin(a))
}

fun angleDelta(a: Double, b: Double): Double = atan2(sin(a - b), cos(a - b))

fun makeProj(yaw: Double, tilt: Double, cx: Double, cy: Double, scale: Double): Projector {
    val st = sin(tilt)
    val ct = cos(tilt)
    val sy = sin(yaw)
    val cyw = cos(yaw)
    return { x, y, z ->
        val x1 = x * cyw + z * sy
        val z1 = -x * sy + z * cyw
        val y1 = y * ct - z1 * st
        val z2 = y * st + z1 * ct
        Triple(cx + x1 * scale, cy - y1 * scale, z2)
    }
}

fun radiusScale(size: Double, pow: Double): Double = Math.pow(size / 300.0, pow)

/** ECMAScript Math.round for non-negative counts: floor(n + 0.5). */
fun jsRound(n: Double): Double = kotlin.math.floor(n + 0.5)

fun finalizeFrame(dots: MutableList<Dot>, lines: List<Line>, rMin: Double = 0.3): OrbFrame {
    val visible = ArrayList<Dot>(dots.size)
    for (d in dots) {
        val alpha = d.a
        if (alpha < 0.02) continue
        d.r = max(rMin, d.r)
        visible.add(d)
    }
    visible.sortBy { it.z }
    return OrbFrame(visible, lines.filter { it.a >= 0.02 })
}

fun inkGrey(white: Double, dark: Boolean): Int {
    val w = min(1.0, max(0.0, white))
    return Math.round((if (dark) 1.0 - w else w) * 255.0).toInt()
}

const val REDUCED_MOTION_T = 0.6
