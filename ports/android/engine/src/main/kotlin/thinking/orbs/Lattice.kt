package thinking.orbs

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private data class Move(val axis: Int, val lo: Double, val hi: Double, val ang: Double)

private data class SolveCycle(val amount: DoubleArray, val active: Int)

private fun solveCycle(time: Double, count: Int, slotDur: Double, rest: Double): SolveCycle {
    val cyc = 2 * count * slotDur + rest
    val tc = time % cyc
    val amount = DoubleArray(count)
    var active = -1
    if (tc < 2 * count * slotDur) {
        val slot = floor(tc / slotDur).toInt()
        val p = (tc - slot * slotDur) / slotDur
        val cl = min(1.0, p / 0.7)
        val ep = 1 - Math.pow(1 - cl, 3.0)
        if (slot < count) {
            for (i in 0 until slot) amount[i] = 1.0
            amount[slot] = ep
            active = slot
        } else {
            val u = 2 * count - 1 - slot
            for (i in 0 until u) amount[i] = 1.0
            amount[u] = 1 - ep
            active = u
        }
    }
    return SolveCycle(amount, active)
}

private fun applyMoves(
    pt3: Triple<Double, Double, Double>,
    moves: List<Move>,
    sc: SolveCycle,
): Pair<Triple<Double, Double, Double>, Boolean> {
    var x = pt3.first
    var y = pt3.second
    var z = pt3.third
    var inActive = false
    for (i in moves.indices) {
        if (sc.amount[i] <= 0) continue
        val mv = moves[i]
        val coord = if (mv.axis == 0) x else if (mv.axis == 1) y else z
        if (coord < mv.lo || coord >= mv.hi) continue
        if (i == sc.active) inActive = true
        val a = mv.ang * sc.amount[i]
        val ca = cos(a)
        val sa = sin(a)
        if (mv.axis == 0) {
            val y2 = y * ca - z * sa
            z = y * sa + z * ca
            y = y2
        } else if (mv.axis == 1) {
            val x2 = x * ca + z * sa
            z = -x * sa + z * ca
            x = x2
        } else {
            val x2 = x * ca - y * sa
            y = x * sa + y * ca
            x = x2
        }
    }
    return Triple(x, y, z) to inActive
}

private fun makeMoves(count: Int): List<Move> {
    val moves = ArrayList<Move>(count)
    for (i in 0 until count) {
        val axis = min(2.0, floor(hashD(i.toDouble(), 2.3) * 3)).toInt()
        val lo = -1.0 + 0.5 * min(3.0, floor(hashD(i.toDouble(), 5.9) * 4))
        val dir = if (hashD(i.toDouble(), 7.7) < 0.5) 1.0 else -1.0
        moves.add(Move(axis, lo, lo + 0.5, (dir * PI) / 2))
    }
    return moves
}

fun frameGlobe(size: Double, t: Double, o: ModeOpts): OrbFrame {
    val spin = 0.5
    val cx = size / 2
    val cy = size / 2
    val radius = (size / 2) * 0.82
    val tilt = 0.4 + 0.06 * sin(t * 0.35)
    val pt = makeProj(t * spin, tilt, cx, cy, radius)
    val scan = t * (spin + (1.7 - spin) * o.get("scanMul", 1.0))
    val rs = radiusScale(size, o.get("rsPow", 0.6))
    val dimBase = o.get("dimBase", 1.0)
    val dots = ArrayList<Dot>()
    val latRings = o.get("latRings", 17.0)
    val lonDensity = o.get("lonDensity", 44.0)
    var li = 0.0
    while (li <= latRings) {
        val lat = -PI / 2 + (li / latRings) * PI
        val cosLat = cos(lat)
        val sinLat = sin(lat)
        val lonCount = max(1.0, jsRound(abs(cosLat) * lonDensity))
        var lj = 0.0
        while (lj < lonCount) {
            val lon = (lj / lonCount) * 2 * PI
            val (px, py, z) = pt(cosLat * cos(lon), sinLat, cosLat * sin(lon))
            val depth = (z + 1) / 2
            val d = angleDelta(lon + t * spin, scan)
            val boost = exp(-(d * d) / 0.18) * max(0.0, z)
            dots.add(
                Dot(
                    px, py, z,
                    (o.get("rBase", 0.6) + o.get("rDepth", 1.7) * depth + o.get("rBoost", 1.0) * boost) * rs,
                    o.get("inkFar", 0.62) - o.get("inkSpan", 0.54) * depth,
                    dimBase + (1 - dimBase) * min(1.0, boost),
                ),
            )
            lj += 1
        }
        li += 1
    }
    return finalizeFrame(dots, emptyList(), o.get("rMin", 0.3))
}

fun frameRubik(size: Double, t: Double, o: ModeOpts): OrbFrame {
    val cx = size / 2
    val cy = size / 2
    val R = (size / 2) * 0.82
    val pt = makeProj(t * 0.55, 0.35 + 0.1 * sin(t * 0.9), cx, cy, R)
    val rs = radiusScale(size, o.get("rsPow", 0.6))
    val moveCount = o.get("moveCount", 14.0).toInt()
    val moves = makeMoves(moveCount)
    val sc = solveCycle(t, moveCount, 0.42, 1.2)
    val dots = ArrayList<Dot>()
    val latRings = o.get("latRings", 15.0)
    val lonDensity = o.get("lonDensity", 40.0)
    var li = 0.0
    while (li <= latRings) {
        val lat = -PI / 2 + (li / latRings) * PI
        val cosLat = cos(lat)
        val sinLat = sin(lat)
        val lonCount = max(1.0, jsRound(abs(cosLat) * lonDensity))
        var lj = 0.0
        while (lj < lonCount) {
            val lon = (lj / lonCount) * 2 * PI
            val (xyz, inActive) = applyMoves(
                Triple(cosLat * cos(lon), sinLat, cosLat * sin(lon)),
                moves,
                sc,
            )
            val (px, py, zr) = pt(xyz.first, xyz.second, xyz.third)
            val depth = (zr + 1) / 2
            dots.add(
                Dot(
                    px, py, zr,
                    (o.get("rBase", 0.6) + o.get("rDepth", 1.7) * depth + (if (inActive) o.get("rActive", 0.3) else 0.0)) * rs,
                    o.get("inkFar", 0.62) - o.get("inkSpan", 0.54) * depth - (if (inActive) 0.14 else 0.0),
                ),
            )
            lj += 1
        }
        li += 1
    }
    return finalizeFrame(dots, emptyList(), o.get("rMin", 0.3))
}

fun frameWave(size: Double, t: Double, o: ModeOpts): OrbFrame {
    val cx = size / 2
    val cy = size / 2
    val R = (size / 2) * 0.874
    val pt = makeProj(t * 0.18, 0.38, cx, cy, 1.0)
    val rs = radiusScale(size, o.get("rsPow", 0.6))
    val dots = ArrayList<Dot>()
    val rings = o.get("rings", 15.0)
    val lonDensity = o.get("lonDensity", 40.0)
    var ri = 0.0
    while (ri <= rings) {
        val lat = -PI / 2 + (ri / rings) * PI
        val cosLat = cos(lat)
        val sinLat = sin(lat)
        val w = 0.62 * sin(t * 2.1 - ri * 0.52) + 0.38 * sin(t * 1.27 + ri * 0.83)
        val rr = R * (0.88 + 0.105 * w)
        val lonCount = max(1.0, jsRound(abs(cosLat) * lonDensity))
        var lj = 0.0
        while (lj < lonCount) {
            val lon = (lj / lonCount) * 2 * PI
            val (px, py, z) = pt(cosLat * cos(lon) * rr, sinLat * rr, cosLat * sin(lon) * rr)
            val depth = (z / R + 1) / 2
            val crest = max(0.0, w)
            dots.add(
                Dot(
                    px, py, z,
                    (o.get("rBase", 0.6) + o.get("rDepth", 1.7) * depth) * (1 + 0.4 * crest) * rs,
                    0.66 - 0.56 * depth - 0.1 * crest,
                ),
            )
            lj += 1
        }
        ri += 1
    }
    return finalizeFrame(dots, emptyList(), o.get("rMin", 0.3))
}
