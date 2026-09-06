package de.drehtuer.shotgun.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Binds the palette to the design export.
 *
 * [PPColors] says it is "transcribed verbatim from the `--pp-*` custom
 * properties in the design export", and until this test nothing checked that.
 * The export is regenerated from Claude Design and has twice come back with
 * decisions reverted, so the risk is not a typo - it is a re-export quietly
 * disagreeing with the app, which no other test here can see: the rest of
 * `PPColorsTest` asserts invariants (ink differs from ground, the ramp runs
 * cold to hot) that hold just as well for the wrong colours.
 *
 * Reading the export directly is the point. A copy of the values would drift
 * in exactly the situation this exists to catch.
 */
class DesignTokenTest {

    /**
     * The nine `--pp-*` tokens, against the [PPColors] field each becomes.
     *
     * Unmapped tokens are a failure rather than something to skip - a new token
     * in a re-export is a design decision that has not reached the app yet, and
     * silence is how it would stay that way.
     */
    private val mapping: Map<String, (PPColors) -> Color> = mapOf(
        "--pp-bg" to { it.bg },
        "--pp-surface" to { it.surface },
        "--pp-surface2" to { it.surface2 },
        "--pp-ink" to { it.ink },
        "--pp-dim" to { it.dim },
        "--pp-line" to { it.line },
        "--pp-accent" to { it.accent },
        "--pp-accent-ink" to { it.accentInk },
        "--pp-accent-soft" to { it.accentSoft },
    )

    // ---- the export ---------------------------------------------------------

    /**
     * Unit tests run with the module directory as the working directory, but
     * that is a Gradle detail rather than a promise, so the repository root is
     * found by walking up to it.
     */
    private val export: String by lazy {
        var dir: File? = File(System.getProperty("user.dir") ?: ".").absoluteFile
        while (dir != null) {
            val candidate = File(dir, "design/Shotgun.dc.html")
            if (candidate.isFile) return@lazy candidate.readText()
            dir = dir.parentFile
        }
        throw AssertionError(
            "design/Shotgun.dc.html not found above ${System.getProperty("user.dir")}. " +
                "The export is the source of these tokens; it cannot be skipped.",
        )
    }

    /** The declarations inside the first `{ ... }` following [selector]. */
    private fun block(selector: String): Map<String, Color> {
        val start = export.indexOf(selector)
        assertTrue("The export has no `$selector` block", start >= 0)
        val open = export.indexOf('{', start + selector.length)
        val close = export.indexOf('}', open)
        assertTrue("The `$selector` block is not closed", open in 0 until close)

        val declarations = Regex("(--pp-[a-z0-9-]+)\\s*:\\s*([^;}]+)")
            .findAll(export.substring(open, close))
            .associate { it.groupValues[1] to parse(it.groupValues[2].trim()) }

        assertTrue("The `$selector` block declares no --pp-* tokens", declarations.isNotEmpty())
        return declarations
    }

    /** `#rrggbb` or `rgba(r,g,b,a)`, the only two forms the export uses. */
    private fun parse(value: String): Color {
        Regex("^#([0-9a-fA-F]{6})\$").find(value)?.let { match ->
            return Color(match.groupValues[1].toLong(16) or 0xFF000000L)
        }
        Regex("^rgba\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*([0-9.]+)\\s*\\)\$")
            .find(value)?.let { match ->
                val (r, g, b, a) = match.destructured
                return Color(r.toInt() / 255f, g.toInt() / 255f, b.toInt() / 255f, a.toFloat())
            }
        throw AssertionError("Unrecognised colour in the export: `$value`")
    }

    /**
     * Compared as packed ARGB rather than as floats. The app builds its
     * translucent accent with `copy(alpha = …)` while the export writes
     * `rgba(…)`, and those agree to the byte but not always to the last bit of
     * a float.
     */
    private fun assertSame(token: String, palette: String, expected: Color, actual: Color) {
        assertEquals(
            "$token disagrees between the export and the $palette palette",
            String.format("#%08X", expected.toArgb()),
            String.format("#%08X", actual.toArgb()),
        )
    }

    /** A block as `token -> #AARRGGBB`, so a failure reads as colours. */
    private fun hexes(selector: String): Map<String, String> =
        block(selector).mapValues { String.format("#%08X", it.value.toArgb()) }

    private fun assertMatches(selector: String, palette: String, colors: PPColors) {
        val tokens = block(selector)
        assertEquals(
            "The $palette block's tokens are not the ones the app maps. " +
                "A token added or removed by a re-export is a decision to make, not to ignore.",
            mapping.keys.sorted(),
            tokens.keys.sorted(),
        )
        tokens.forEach { (token, expected) ->
            assertSame(token, palette, expected, mapping.getValue(token)(colors))
        }
    }

    // ---- the app matches the design ----------------------------------------

    @Test
    fun `the dark palette matches the export`() {
        assertMatches(":root", "dark", ppDarkColors())
    }

    @Test
    fun `the light palette matches the export`() {
        assertMatches("@media (prefers-color-scheme: light)", "light", ppLightColors())
    }

    // ---- the export agrees with itself -------------------------------------

    // The export carries each palette twice: once for the system preference and
    // once for an explicit `data-theme`. They are written out separately, so
    // they can disagree - and then the app would match one and not the other.

    @Test
    fun `the two dark blocks in the export agree`() {
        assertEquals(
            "`:root` and `[data-theme=\"dark\"]` declare different colours",
            hexes(":root"),
            hexes("[data-theme=\"dark\"]"),
        )
    }

    @Test
    fun `the two light blocks in the export agree`() {
        assertEquals(
            "the light media query and `[data-theme=\"light\"]` declare different colours",
            hexes("@media (prefers-color-scheme: light)"),
            hexes("[data-theme=\"light\"]"),
        )
    }
}
