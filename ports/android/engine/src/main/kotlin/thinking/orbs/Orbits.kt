package thinking.orbs

import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

fun frameOrbits(size: Double, t: Double, o: ModeOpts): OrbFrame {
    val cx = size / 2
    val cy = size / 2
    val R = (size / 2) * 0.82
    val pt = makeProj(t * 0.12, 0.3, cx, cy, 1.0)
    val rs = radiusScale(size, o.get("rsPow", 0.6))
    val dots = ArrayList<Dot>()
    val orbitN = o.get("orbitN", 12.0).toInt()
    val ghostN = o.get("ghostN", 40.0).toInt()
    val particles = o.get("particles", 3.0).toInt()
    for (orb in 0 until orbitN) {
        val h1 = hashD(orb.toDouble(), 1.7)
        val h2 = hashD(orb.toDouble(), 5.2)
        val h3 = hashD(orb.toDouble(), 8.9)
        val ro = R * (0.45 + 0.52 * h1)
        val th = h1 * 2 * PI
        val phi = acos(2 * h2 - 1)
        val nx = sin(phi) * cos(th)
        val ny = cos(phi)
        val nz = sin(phi) * sin(th)
        var ux = -ny
        var uy = nx
        val uz = 0.0
        val ul = max(1e-6, sqrt(ux * ux + uy * uy))
        ux /= ul
        uy /= ul
        val vx = ny * uz - nz * uy
        val vy = nz * ux - nx * uz
        val vz = nx * uy - ny * ux
        val speed = (0.25 + 0.55 * h3) * (if (h3 > 0.5) 1.0 else -1.0)
        for (k in 0 until ghostN) {
            val a = (k.toDouble() / ghostN) * 2 * PI
            val (px, py, z) = pt(
                (ux * cos(a) + vx * sin(a)) * ro,
                (uy * cos(a) + vy * sin(a)) * ro,
                (uz * cos(a) + vz * sin(a)) * ro,
            )
            val depth = (z / ro + 1) / 2
            dots.add(Dot(px, py, z, o.get("ghostR", 0.9) * rs, 0.72, o.get("ghostA", 0.5) * (0.4 + 0.6 * depth)))
        }
        for (m in 0 until particles) {
            val a = t * speed + (m.toDouble() / particles) * 2 * PI + h2 * 6
            val (px, py, z) = pt(
                (ux * cos(a) + vx * sin(a)) * ro,
                (uy * cos(a) + vy * sin(a)) * ro,
                (uz * cos(a) + vz * sin(a)) * ro,
            )
            val depth = (z / ro + 1) / 2
            dots.add(
                Dot(
                    px, py, z,
                    (o.get("partR", 1.2) + o.get("partRDepth", 1.6) * depth) * rs,
                    0.3 - 0.22 * depth,
                ),
            )
        }
    }
    return finalizeFrame(dots, emptyList(), o.get("rMin", 0.3))
}
