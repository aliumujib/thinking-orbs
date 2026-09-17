package thinking.orbs

import kotlin.math.max
import kotlin.math.sqrt

class ModeOpts(private val m: MutableMap<String, Double> = linkedMapOf()) {
    constructor(vararg pairs: Pair<String, Double>) : this(linkedMapOf(*pairs))

    operator fun get(key: String): Double? = m[key]
    fun get(key: String, default: Double): Double = m[key] ?: default
    operator fun set(key: String, value: Double) { m[key] = value }
    fun copy(): ModeOpts = ModeOpts(m.toMutableMap())
    fun merge(other: ModeOpts): ModeOpts {
        val out = copy()
        for ((k, v) in other.m) out[k] = v
        return out
    }
    fun entries(): Set<Map.Entry<String, Double>> = m.entries
}

private val COUNT_PAIRS = listOf(
    "latRings" to "lonDensity",
    "rings" to "lonDensity",
    "lanes" to "segs",
)
private val COUNT_KEYS = listOf("orbitN", "ghostN", "nodeN", "strandN", "signals")
private val ICON_DENSITY_KEYS = listOf("iconD")
private val RADIUS_KEYS = listOf(
    "rBase", "rDepth", "rActive", "rDot", "ghostR", "partR", "partRDepth", "nodeR", "nodeRDepth",
)

fun scaleCounts(opts: ModeOpts, scale: Double): ModeOpts {
    val out = opts.copy()
    val done = HashSet<String>()
    val rt = sqrt(scale)
    for ((a, b) in COUNT_PAIRS) {
        val va = out[a]
        val vb = out[b]
        if (va != null && vb != null && a !in done && b !in done) {
            out[a] = max(2.0, jsRound(va * rt))
            out[b] = max(2.0, jsRound(vb * rt))
            done.add(a)
            done.add(b)
        }
    }
    for (k in COUNT_KEYS) {
        val v = out[k]
        if (v != null && v != 0.0 && k !in done) out[k] = max(1.0, jsRound(v * scale))
    }
    for (k in ICON_DENSITY_KEYS) {
        val v = out[k]
        if (v != null) out[k] = max(0.02, v * scale)
    }
    return out
}

fun scaleRadii(opts: ModeOpts, scale: Double): ModeOpts {
    val out = opts.copy()
    for (k in RADIUS_KEYS) {
        val v = out[k]
        if (v != null) out[k] = v * scale
    }
    out["rSizeMul"] = (out["rSizeMul"] ?: 1.0) * scale
    return out
}

val BASE_PROFILES: Map<String, ModeOpts> = mapOf(
        "globe" to ModeOpts(
            "latRings" to 17.0,
            "lonDensity" to 44.0,
            "rBase" to 0.6,
            "rDepth" to 1.7,
            "rBoost" to 1.0,
            "inkFar" to 0.62,
            "inkSpan" to 0.54,
            "rsPow" to 0.6,
            "rMin" to 0.3
        ),
        "orbits" to ModeOpts(
            "orbitN" to 12.0,
            "ghostN" to 40.0,
            "ghostR" to 0.9,
            "ghostA" to 0.5,
            "particles" to 3.0,
            "partR" to 1.2,
            "partRDepth" to 1.6,
            "rsPow" to 0.6,
            "rMin" to 0.3
        ),
        "rubik" to ModeOpts(
            "latRings" to 15.0,
            "lonDensity" to 40.0,
            "moveCount" to 14.0,
            "rBase" to 0.6,
            "rDepth" to 1.7,
            "rActive" to 0.3,
            "inkFar" to 0.62,
            "inkSpan" to 0.54,
            "rsPow" to 0.6,
            "rMin" to 0.3
        ),
        "wave" to ModeOpts(
            "rings" to 15.0,
            "lonDensity" to 40.0,
            "rBase" to 0.6,
            "rDepth" to 1.7,
            "rsPow" to 0.6,
            "rMin" to 0.3
        ),
        "web" to ModeOpts(
            "nodeN" to 30.0,
            "thr" to 0.72,
            "signals" to 5.0,
            "nodeR" to 1.4,
            "nodeRDepth" to 1.8,
            "lineW" to 0.8,
            "rsPow" to 0.6,
            "rMin" to 0.3
        ),
        "braid" to ModeOpts(
            "strandN" to 52.0,
            "turns" to 3.0,
            "ghostN" to 150.0,
            "rBase" to 1.2,
            "rDepth" to 1.8,
            "rsPow" to 0.6,
            "rMin" to 0.3
        ),
        "ribbon" to ModeOpts(
            "lanes" to 5.0,
            "segs" to 88.0,
            "ghostN" to 150.0,
            "rBase" to 1.1,
            "rDepth" to 1.7,
            "rsPow" to 0.6,
            "rMin" to 0.3
        ),
        "ring" to ModeOpts(
            "lanes" to 5.0,
            "segs" to 88.0,
            "ghostN" to 0.0,
            "faceOn" to 1.0,
            "rBase" to 1.1,
            "rDepth" to 1.7,
            "rsPow" to 0.6,
            "rMin" to 0.3
        ),
        "morph" to ModeOpts(
            "rDot" to 0.021,
            "iconD" to 1.0,
            "rMin" to 0.25
        )
)

data class Preset(val speed: Double, val count: Double, val size: Double, val extra: ModeOpts? = null)

val PRESETS: Map<String, Map<Int, Preset>> = mapOf(
        "orbits" to mapOf(
            20 to Preset(3.9, 0.238, 2.4, null),
            64 to Preset(1.885, 1.0, 1.0, null)
        ),
        "globe" to mapOf(
            20 to Preset(2.665, 0.105, 1.75, ModeOpts(
                    "scanMul" to 4.335,
                    "dimBase" to 0.45
                )),
            64 to Preset(2.015, 0.42, 1.15, ModeOpts(
                    "scanMul" to 4.08,
                    "dimBase" to 0.45
                ))
        ),
        "rubik" to mapOf(
            20 to Preset(1.95, 0.088, 1.9, null),
            64 to Preset(1.82, 0.35, 1.05, null)
        ),
        "wave" to mapOf(
            20 to Preset(3.998, 0.105, 1.6, null),
            64 to Preset(4.388, 0.341, 1.0, null)
        ),
        "web" to mapOf(
            20 to Preset(6.63, 0.25, 1.52, null),
            64 to Preset(3.315, 1.35, 0.95, null)
        ),
        "braid" to mapOf(
            20 to Preset(2.75, 0.1125, 1.36, null),
            64 to Preset(1.625, 0.5, 1.0, null)
        ),
        "ribbon" to mapOf(
            20 to Preset(3.12, 0.051, 1.073, ModeOpts(
                    "spin" to 0.0,
                    "bandMul" to 4.94,
                    "wobMul" to 1.0
                )),
            64 to Preset(2.34, 0.25, 0.85, ModeOpts(
                    "spin" to 0.0,
                    "bandMul" to 3.9,
                    "wobMul" to 1.0
                ))
        ),
        "ring" to mapOf(
            20 to Preset(3.78, 0.028, 1.622, ModeOpts(
                    "spin" to 0.0,
                    "bandMul" to 3.968,
                    "wobMul" to 0.565
                )),
            64 to Preset(3.24, 0.25, 0.956, ModeOpts(
                    "spin" to 0.0,
                    "bandMul" to 3.627,
                    "wobMul" to 0.368
                ))
        ),
        "morph" to mapOf(
            20 to Preset(2.08, 0.53, 1.011, ModeOpts(
                    "spread" to 1.45
                )),
            64 to Preset(2.405, 0.702, 0.395, ModeOpts(
                    "spread" to 1.45
                ))
        )
)

val STATE_TO_MODE: Map<String, String> = mapOf(
        "working" to "orbits",
        "searching" to "globe",
        "solving" to "rubik",
        "listening" to "wave",
        "connecting" to "web",
        "weaving" to "braid",
        "composing" to "ribbon",
        "breathing" to "ring",
        "shaping" to "morph"
)

val LABELS: Map<String, String> = mapOf(
        "working" to "Working…",
        "searching" to "Searching…",
        "solving" to "Solving…",
        "listening" to "Listening…",
        "connecting" to "Connecting…",
        "weaving" to "Weaving…",
        "composing" to "Composing…",
        "breathing" to "Thinking…",
        "shaping" to "Shaping…"
)

data class Resolved(val mode: String, val speed: Double, val opts: ModeOpts)

private val cache = HashMap<String, Resolved>()

fun resolvePreset(state: String, size: Int): Resolved {
    val key = "$state-$size"
    cache[key]?.let { return it }
    val mode = STATE_TO_MODE[state] ?: error("unknown state $state")
    val preset = PRESETS[mode]?.get(size) ?: error("no preset $mode $size")
    var opts = BASE_PROFILES[mode]!!.copy()
    if (preset.count != 1.0) opts = scaleCounts(opts, preset.count)
    if (preset.size != 1.0) opts = scaleRadii(opts, preset.size)
    if (preset.extra != null) opts = opts.merge(preset.extra)
    val resolved = Resolved(mode, preset.speed, opts)
    cache[key] = resolved
    return resolved
}
