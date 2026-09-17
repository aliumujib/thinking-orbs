package thinking.orbs

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

fun frameRibbon(size: Double, t: Double, o: ModeOpts): OrbFrame {
    val cx = size / 2
    val cy = size / 2
    val R = (size / 2) * 0.78
    val spin = o.get("spin", 1.0)
    val camTilt = 0.3
    val pt = makeProj(t * 0.1 * spin, camTilt, cx, cy, 1.0)
    val rs = radiusScale(size, o.get("rsPow", 0.6))
    val dots = ArrayList<Dot>()
    val ghostN = o.get("ghostN", 150.0).toInt()
    for (i in 0 until ghostN) {
        val d = fibDir(i.toDouble(), ghostN.toDouble())
        val (px, py, z) = pt(d.first * R, d.second * R, d.third * R)
        val depth = (z / R + 1) / 2
        dots.add(Dot(px, py, z, 0.8 * rs, 0.78, 0.1 + 0.22 * depth))
    }
    val ya = t * 0.24 * spin
    val faceOn = o.get("faceOn", 0.0) != 0.0
    val ta = if (faceOn) -camTilt else 0.55 + 0.3 * sin(t * 0.18) * spin
    val ux = cos(ya)
    val uy = 0.0
    val uz = sin(ya)
    val vx = -uz * sin(ta)
    val vy = cos(ta)
    val vz = ux * sin(ta)
    val nx = uy * vz - uz * vy
    val ny = uz * vx - ux * vz
    val nz = ux * vy - uy * vx
    val wobAmp = 0.23 * o.get("wobMul", 1.0)
    val baseR = if (faceOn) R / (1 + 0.85 * wobAmp) else R
    val baseLanes = o.get("lanes", 5.0)
    val segs = o.get("segs", 88.0).toInt()
    val lanes = max(1.0, jsRound(baseLanes * o.get("bandMul", 1.0))).toInt()
    for (w in 0 until lanes) {
        val laneOff = (w - (lanes - 1) / 2.0) * 0.075
        val edge = abs(w - (lanes - 1) / 2.0) / max(1.0, (lanes - 1) / 2.0)
        for (k in 0 until segs) {
            val a = (k.toDouble() / segs) * 2 * PI
            val wob =
                (0.16 * sin(a * 3 - t * 1.7 + w * 0.22) + 0.07 * sin(a * 5 + t * 1.1)) * o.get("wobMul", 1.0)
            val radial = if (faceOn) 1 + wob else 1.0
            val off = if (faceOn) laneOff else laneOff + wob
            val x = ux * cos(a) + vx * sin(a) + nx * off
            val y = uy * cos(a) + vy * sin(a) + ny * off
            val z = uz * cos(a) + vz * sin(a) + nz * off
            val l = sqrt(x * x + y * y + z * z)
            val rr = baseR * radial
            val (px, py, zr) = pt((x / l) * rr, (y / l) * rr, (z / l) * rr)
            val depth = (zr / R + 1) / 2
            dots.add(
                Dot(
                    px, py, zr,
                    (o.get("rBase", 1.1) + o.get("rDepth", 1.7) * depth) * (1 - 0.25 * edge) * rs,
                    0.52 - 0.44 * depth + 0.18 * edge,
                    0.4 + 0.6 * depth,
                ),
            )
        }
    }
    return finalizeFrame(dots, emptyList(), o.get("rMin", 0.3))
}
