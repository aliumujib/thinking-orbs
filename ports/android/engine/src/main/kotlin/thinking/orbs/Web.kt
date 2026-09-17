package thinking.orbs

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

fun frameWeb(size: Double, t: Double, o: ModeOpts): OrbFrame {
    val cx = size / 2
    val cy = size / 2
    val R = (size / 2) * 0.8 * o.get("spread", 1.0)
    val pt = makeProj(t * 0.12, 0.32, cx, cy, R)
    val rs = radiusScale(size, o.get("rsPow", 0.6))
    val nodeN = o.get("nodeN", 30.0).toInt()
    val thr = o.get("thr", 0.72)
    val nodeR = o.get("nodeR", 1.4)
    val nodeRDepth = o.get("nodeRDepth", 1.8)
    val nodes = ArrayList<Triple<Double, Double, Double>>(nodeN)
    for (i in 0 until nodeN) {
        val d = fibDir(i.toDouble(), nodeN.toDouble())
        val x = d.first + 0.3 * (vnoise(i * 0.31 + 9, t * 0.24) - 0.5) * 2
        val y = d.second + 0.3 * (vnoise(i * 0.53 + 27, t * 0.21) - 0.5) * 2
        val z = d.third + 0.3 * (vnoise(i * 0.77 + 55, t * 0.27) - 0.5) * 2
        val l = sqrt(x * x + y * y + z * z)
        nodes.add(Triple(x / l, y / l, z / l))
    }
    val lines = ArrayList<Line>()
    val dots = ArrayList<Dot>()
    for (i in 0 until nodeN) {
        for (j in i + 1 until nodeN) {
            val dx = nodes[i].first - nodes[j].first
            val dy = nodes[i].second - nodes[j].second
            val dz = nodes[i].third - nodes[j].third
            val dist = sqrt(dx * dx + dy * dy + dz * dz)
            if (dist >= thr) continue
            val (x1, y1, z1) = pt(nodes[i].first, nodes[i].second, nodes[i].third)
            val (x2, y2, z2) = pt(nodes[j].first, nodes[j].second, nodes[j].third)
            val depth = ((z1 + z2) / 2 + 1) / 2
            lines.add(
                Line(
                    x1, y1, x2, y2,
                    0.42,
                    (1 - dist / thr) * (0.3 + 0.55 * depth),
                    max(0.6, o.get("lineW", 0.8) * rs),
                ),
            )
        }
    }
    for (i in 0 until nodeN) {
        val (px, py, z) = pt(nodes[i].first, nodes[i].second, nodes[i].third)
        val depth = (z + 1) / 2
        val pulse = 1 + 0.25 * sin(t * 1.4 + i * 2.7)
        dots.add(Dot(px, py, z, (nodeR + nodeRDepth * depth) * pulse * rs, 0.55 - 0.45 * depth))
    }
    val signals = o.get("signals", 5.0).toInt()
    for (s in 0 until signals) {
        val seg = floor(t * 0.55 + s * 7.31)
        val a = floor(hashD(seg, s * 3.1 + 1.7) * nodeN).toInt()
        val candidate = floor(hashD(seg, s * 5.7 + 4.2) * nodeN).toInt()
        val b = if (candidate == a) (candidate + 1) % nodeN else candidate
        val f = frac(t * 0.55 + s * 7.31)
        val x = lerp(nodes[a].first, nodes[b].first, f)
        val y = lerp(nodes[a].second, nodes[b].second, f)
        val z = lerp(nodes[a].third, nodes[b].third, f)
        val l = max(1e-6, sqrt(x * x + y * y + z * z))
        val (px, py, zr) = pt(x / l, y / l, z / l)
        val depth = (zr + 1) / 2
        dots.add(Dot(px, py, zr, (nodeR * 1.5 + nodeRDepth * depth) * rs, 0.05, 0.5 + 0.5 * depth))
    }
    return finalizeFrame(dots, lines, o.get("rMin", 0.3))
}
