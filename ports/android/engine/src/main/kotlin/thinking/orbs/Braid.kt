package thinking.orbs

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

fun frameBraid(size: Double, t: Double, o: ModeOpts): OrbFrame {
    val cx = size / 2
    val cy = size / 2
    val R = (size / 2) * 0.76
    val pt = makeProj(t * 0.4, 0.3, cx, cy, 1.0)
    val rs = radiusScale(size, o.get("rsPow", 0.6))
    val dots = ArrayList<Dot>()
    val ghostN = o.get("ghostN", 150.0).toInt()
    for (i in 0 until ghostN) {
        val d = fibDir(i.toDouble(), ghostN.toDouble())
        val (px, py, z) = pt(d.first * R, d.second * R, d.third * R)
        val depth = (z / R + 1) / 2
        dots.add(Dot(px, py, z, 0.8 * rs, 0.78, 0.1 + 0.22 * depth))
    }
    val strandN = o.get("strandN", 52.0).toInt()
    val turns = o.get("turns", 3.0)
    for (s in 0 until 3) {
        val phase = (s / 3.0) * 2 * PI
        for (i in 0 until strandN) {
            val u = (frac(i.toDouble() / strandN + t * 0.045) * 2 - 1) * 0.96
            val surf = sqrt(maxOf(0.0, 1 - u * u))
            val endFade = min(1.0, (1 - abs(u)) / 0.1)
            val a = u * PI * turns + phase
            val weave = 1 + 0.075 * sin(u * PI * turns * 2 + phase * 2 + t * 0.8)
            val rr = surf * R * weave
            val (px, py, zr) = pt(cos(a) * rr, u * R * weave, sin(a) * rr)
            val depth = (zr / R + 1) / 2
            dots.add(
                Dot(
                    px, py, zr,
                    (o.get("rBase", 1.2) + o.get("rDepth", 1.8) * depth) * rs,
                    0.55 - 0.45 * depth,
                    endFade * (0.45 + 0.55 * depth),
                ),
            )
        }
    }
    return finalizeFrame(dots, emptyList(), o.get("rMin", 0.3))
}
