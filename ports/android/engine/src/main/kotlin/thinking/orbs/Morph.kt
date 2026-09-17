package thinking.orbs

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private typealias Path = (Double) -> Pair<Double, Double>

private fun smoothE(x: Double): Double = x * x * (3 - 2 * x)

private fun polyPath(verts: Array<Pair<Double, Double>>): Path {
    val V = verts.size
    val L = DoubleArray(V)
    var total = 0.0
    for (i in 0 until V) {
        val a = verts[i]
        val b = verts[(i + 1) % V]
        val l = hypot(b.first - a.first, b.second - a.second)
        L[i] = l
        total += l
    }
    return { f ->
        var target = f * total
        var i = 0
        while (target > L[i] && i < V - 1) {
            target -= L[i]
            i++
        }
        val a = verts[i]
        val b = verts[(i + 1) % V]
        val ff = if (L[i] != 0.0) min(1.0, target / L[i]) else 0.0
        Pair(a.first + (b.first - a.first) * ff, a.second + (b.second - a.second) * ff)
    }
}

private val CIRCLE: Path = { f ->
    val a = -PI / 2 + f * 2 * PI
    Pair(cos(a) * 0.24, sin(a) * 0.24)
}
private val TRIANGLE = polyPath(arrayOf(0.0 to -0.26, 0.24 to 0.16, -0.24 to 0.16))
private val SQUARE = polyPath(arrayOf(0.0 to -0.2, 0.2 to -0.2, 0.2 to 0.2, -0.2 to 0.2, -0.2 to -0.2))
private val CYCLE = arrayOf(CIRCLE, TRIANGLE, SQUARE)

private fun morphN(d: Double): Int = max(6.0, jsRound(34 * d)).toInt()

private const val HOLD = 1.4
private const val MORPH = 0.9
private const val SEG = HOLD + MORPH

fun frameMorph(size: Double, t: Double, o: ModeOpts): OrbFrame {
    val K = CYCLE.size
    val tc = t % (SEG * K)
    val k = floor(tc / SEG).toInt()
    val local = tc - k * SEG
    val m = if (local > HOLD) smoothE((local - HOLD) / MORPH) else 0.0
    val sprd = o.get("spread", 1.0)
    val pA = CYCLE[k]
    val pB = CYCLE[(k + 1) % K]
    val M = 160
    val pts = ArrayList<Pair<Double, Double>>(M)
    for (i in 0 until M) {
        val f = i.toDouble() / M
        val a = pA(f)
        val b = pB(f)
        pts.add(Pair((a.first + (b.first - a.first) * m) * sprd, (a.second + (b.second - a.second) * m) * sprd))
    }
    val L = DoubleArray(M)
    var total = 0.0
    for (i in 0 until M) {
        val a = pts[i]
        val b = pts[(i + 1) % M]
        val l = hypot(b.first - a.first, b.second - a.second)
        L[i] = l
        total += l
    }
    val n = morphN(o.get("iconD", 1.0))
    val re = o.get("rDot", 0.021) * 1.35 * sprd
    val pulse = 1 + 0.02 * sin(local * 3.1)
    val dots = ArrayList<Dot>()
    val c2 = size / 2
    var seg = 0
    var acc = 0.0
    for (k2 in 0 until n) {
        val target = (k2.toDouble() / n) * total
        while (acc + L[seg] < target && seg < M - 1) {
            acc += L[seg]
            seg++
        }
        val a = pts[seg]
        val b = pts[(seg + 1) % M]
        val f = if (L[seg] != 0.0) min(1.0, (target - acc) / L[seg]) else 0.0
        val x = (a.first + (b.first - a.first) * f) * pulse
        val y = (a.second + (b.second - a.second) * f) * pulse
        dots.add(Dot(c2 + x * size, c2 + y * size, 0.0, max(0.35, re * size), 0.1))
    }
    return finalizeFrame(dots, emptyList(), o.get("rMin", 0.3))
}
