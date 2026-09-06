package de.drehtuer.shotgun.result

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** One stop of the heat ramp: [position] in 0..1 against a packed ARGB colour. */
data class HeatStop(val position: Float, val color: Int)

/**
 * The fairness field: a density map of where winning fingers have landed.
 *
 * Deliberately free of Android types so the maths can be unit-tested. The
 * screen's whole claim is that the field is flat - that no corner of the glass
 * wins more often - so the computation has to be right, and "looks about right"
 * is not a check.
 */
object HeatField {

    /** Kernel radius as a fraction of the shorter side. */
    private const val RADIUS_FRACTION = 0.13f

    /** Floor, so a tiny field still has a visible spread. */
    private const val MIN_RADIUS = 18f

    /** Pulls mid-range values up so sparse history still reads as a field. */
    private const val GAMMA = 0.9f

    /**
     * Density at every cell of a [width] x [height] grid, normalised so the
     * peak is 1. Points are normalised coordinates in 0..1.
     *
     * Points near an edge are **mirrored outward** and counted twice. Without
     * that, a win in a corner spreads into a quarter of the kernel's area
     * instead of all of it, and the corners read as permanently cold - which
     * would libel the draw as unfair when it is not.
     */
    fun density(points: List<Pair<Float, Float>>, width: Int, height: Int): FloatArray {
        val cells = FloatArray(width * height)
        if (points.isEmpty() || width <= 0 || height <= 0) return cells

        val radius = max(MIN_RADIUS, min(width, height) * RADIUS_FRACTION)
        val radiusSquared = radius * radius

        for ((nx, ny) in points) {
            val px = nx * width
            val py = ny * height
            // The point itself, plus its reflection in any edge it sits near.
            val xs = buildList {
                add(px)
                if (px < radius) add(-px)
                if (width - px < radius) add(2 * width - px)
            }
            val ys = buildList {
                add(py)
                if (py < radius) add(-py)
                if (height - py < radius) add(2 * height - py)
            }
            for (cx in xs) for (cy in ys) splat(cells, width, height, cx, cy, radius, radiusSquared)
        }

        val peak = cells.max()
        if (peak <= 0f) return cells
        for (i in cells.indices) {
            cells[i] = (cells[i] / peak).pow(GAMMA)
        }
        return cells
    }

    /** Quartic falloff, so a contribution fades to nothing at the radius. */
    private fun splat(
        cells: FloatArray,
        width: Int,
        height: Int,
        cx: Float,
        cy: Float,
        radius: Float,
        radiusSquared: Float,
    ) {
        val x0 = max(0, floor(cx - radius).toInt())
        val x1 = min(width - 1, ceil(cx + radius).toInt())
        val y0 = max(0, floor(cy - radius).toInt())
        val y1 = min(height - 1, ceil(cy + radius).toInt())
        for (y in y0..y1) {
            val dy = y - cy
            val row = y * width
            for (x in x0..x1) {
                val dx = x - cx
                val q = (dx * dx + dy * dy) / radiusSquared
                if (q < 1f) {
                    val k = 1f - q
                    cells[row + x] += k * k
                }
            }
        }
    }

    /** Maps a density grid through [stops] into packed ARGB pixels. */
    fun colorise(density: FloatArray, stops: List<HeatStop>): IntArray {
        require(stops.isNotEmpty()) { "a ramp needs at least one stop" }
        return IntArray(density.size) { sample(stops, density[it]) }
    }

    /** The colour at [value] on the ramp, interpolated in RGB. */
    fun sample(stops: List<HeatStop>, value: Float): Int {
        val v = value.coerceIn(0f, 1f)
        if (stops.size == 1) return stops.first().color
        var lower = stops.first()
        var upper = stops.last()
        for (i in 0 until stops.size - 1) {
            if (v >= stops[i].position && v <= stops[i + 1].position) {
                lower = stops[i]
                upper = stops[i + 1]
                break
            }
        }
        val span = upper.position - lower.position
        val t = if (span <= 0f) 0f else (v - lower.position) / span
        return lerpArgb(lower.color, upper.color, t)
    }

    private fun lerpArgb(from: Int, to: Int, t: Float): Int {
        fun channel(shift: Int): Int {
            val a = (from shr shift) and 0xFF
            val b = (to shr shift) and 0xFF
            return (a + (b - a) * t).toInt().coerceIn(0, 255)
        }
        return (0xFF shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }
}
