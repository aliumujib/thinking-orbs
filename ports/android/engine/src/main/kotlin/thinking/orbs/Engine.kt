package thinking.orbs

val MODE_FRAMES: Map<String, (Double, Double, ModeOpts) -> OrbFrame> = mapOf(
    "orbits" to ::frameOrbits,
    "globe" to ::frameGlobe,
    "rubik" to ::frameRubik,
    "wave" to ::frameWave,
    "web" to ::frameWeb,
    "braid" to ::frameBraid,
    "ribbon" to ::frameRibbon,
    "ring" to ::frameRibbon,
    "morph" to ::frameMorph,
)

fun frameFor(state: String, size: Int, t: Double): OrbFrame {
    val resolved = resolvePreset(state, size)
    val fn = MODE_FRAMES[resolved.mode] ?: error("no frame ${resolved.mode}")
    return fn(size.toDouble(), t, resolved.opts)
}
