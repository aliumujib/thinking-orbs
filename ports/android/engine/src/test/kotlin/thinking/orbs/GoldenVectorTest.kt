package thinking.orbs

import java.io.File
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class GoldenVectorTest {
    private val eps = 1e-4

    @Test
    fun allGoldenCasesMatchWithinTolerance() {
        val json = loadGolden()
        val cases = parseCases(json)
        assertEquals(72, cases.size, "expected 9 states × 2 sizes × 4 timestamps")
        val states = cases.map { it.state }.toSet()
        val sizes = cases.map { it.size }.toSet()
        assertEquals(
            setOf("working", "searching", "solving", "listening", "connecting", "weaving", "composing", "breathing", "shaping"),
            states,
        )
        assertEquals(setOf(20, 64), sizes)

        var checked = 0
        for (c in cases) {
            val frame = frameFor(c.state, c.size, c.t)
            if (frame.dots.size != c.dotCount) {
                fail("${c.key}: dotCount expected ${c.dotCount} got ${frame.dots.size}")
            }
            if (frame.lines.size != c.lineCount) {
                fail("${c.key}: lineCount expected ${c.lineCount} got ${frame.lines.size}")
            }
            var di = 0
            for (d in frame.dots) {
                near(c.key, "dot[$di].x", c.dots[di * 6], d.x)
                near(c.key, "dot[$di].y", c.dots[di * 6 + 1], d.y)
                near(c.key, "dot[$di].z", c.dots[di * 6 + 2], d.z)
                near(c.key, "dot[$di].r", c.dots[di * 6 + 3], d.r)
                near(c.key, "dot[$di].white", c.dots[di * 6 + 4], d.white)
                near(c.key, "dot[$di].a", c.dots[di * 6 + 5], d.a)
                di++
            }
            var li = 0
            for (l in frame.lines) {
                near(c.key, "line[$li].x1", c.lines[li * 7], l.x1)
                near(c.key, "line[$li].y1", c.lines[li * 7 + 1], l.y1)
                near(c.key, "line[$li].x2", c.lines[li * 7 + 2], l.x2)
                near(c.key, "line[$li].y2", c.lines[li * 7 + 3], l.y2)
                near(c.key, "line[$li].white", c.lines[li * 7 + 4], l.white)
                near(c.key, "line[$li].a", c.lines[li * 7 + 5], l.a)
                near(c.key, "line[$li].w", c.lines[li * 7 + 6], l.w)
                li++
            }
            checked++
        }
        assertEquals(72, checked)
    }

    @Test
    fun themeMirrorsInkForDarkAndLight() {
        assertEquals(51, inkGrey(0.2, dark = false))
        assertEquals(Math.round((1 - 0.2) * 255.0).toInt(), inkGrey(0.2, dark = true))
        assertEquals(204, inkGrey(0.8, dark = false))
        assertEquals(51, inkGrey(0.8, dark = true))
        val frame = frameFor("connecting", 64, 0.6)
        assertTrue(frame.lines.isNotEmpty(), "web/connecting must emit strokes")
        for (themeDark in listOf(true, false)) {
            for (d in frame.dots.take(8)) {
                val g = inkGrey(d.white, themeDark)
                assertTrue(g in 0..255)
            }
        }
    }

    private fun near(key: String, field: String, expected: Double, got: Double) {
        if (abs(expected - got) > eps) {
            fail("$key $field expected $expected got $got (ε=$eps)")
        }
    }

    private fun loadGolden(): String {
        val resource = javaClass.classLoader.getResource("orbs-golden.json")
        if (resource != null) return resource.readText()
        val candidates = listOf(
            File("src/test/resources/orbs-golden.json"),
            File("../spec/orbs-golden.json"),
            File("../../spec/orbs-golden.json"),
            File("../../../spec/orbs-golden.json"),
        )
        return candidates.firstOrNull { it.isFile }?.readText()
            ?: error("orbs-golden.json not found")
    }
}

private data class GoldenCase(
    val key: String,
    val state: String,
    val size: Int,
    val t: Double,
    val dotCount: Int,
    val lineCount: Int,
    val dots: DoubleArray,
    val lines: DoubleArray,
)

private fun parseCases(json: String): List<GoldenCase> {
    val cases = mutableListOf<GoldenCase>()
    val re = Regex("\\{\\s*\"key\":\\s*\"([^\"]+)\".*?\"lines\":\\s*(\\[[^\\]]*\\])\\s*\\}", RegexOption.DOT_MATCHES_ALL)
    // Fallback: walk with a tiny extractor because cases are large.
    var i = json.indexOf("\"cases\"")
    i = json.indexOf('[', i)
    val arr = json.substring(i)
    var pos = 1
    while (true) {
        val start = arr.indexOf("{\"key\"", pos)
        if (start < 0) break
        val end = findMatching(arr, start)
        val obj = arr.substring(start, end + 1)
        cases.add(parseCase(obj))
        pos = end + 1
    }
    return cases
}

private fun findMatching(s: String, start: Int): Int {
    var depth = 0
    var inStr = false
    var esc = false
    for (i in start until s.length) {
        val c = s[i]
        if (inStr) {
            if (esc) esc = false
            else if (c == '\\') esc = true
            else if (c == '"') inStr = false
            continue
        }
        when (c) {
            '"' -> inStr = true
            '{' -> depth++
            '}' -> {
                depth--
                if (depth == 0) return i
            }
        }
    }
    error("unbalanced json")
}

private fun parseCase(obj: String): GoldenCase {
    fun str(name: String): String {
        val m = Regex("\"$name\":\\s*\"([^\"]*)\"").find(obj) ?: error("missing $name")
        return m.groupValues[1]
    }
    fun num(name: String): Double {
        val m = Regex("\"$name\":\\s*(-?\\d+(?:\\.\\d+)?(?:[eE][-+]?\\d+)?)").find(obj) ?: error("missing $name")
        return m.groupValues[1].toDouble()
    }
    fun arr(name: String): DoubleArray {
        val key = "\"$name\":"
        val k = obj.indexOf(key)
        val lb = obj.indexOf('[', k)
        val rb = obj.indexOf(']', lb)
        val body = obj.substring(lb + 1, rb).trim()
        if (body.isEmpty()) return DoubleArray(0)
        return body.split(',').map { it.trim().toDouble() }.toDoubleArray()
    }
    return GoldenCase(
        key = str("key"),
        state = str("state"),
        size = num("size").toInt(),
        t = num("t"),
        dotCount = num("dotCount").toInt(),
        lineCount = num("lineCount").toInt(),
        dots = arr("dots"),
        lines = arr("lines"),
    )
}
