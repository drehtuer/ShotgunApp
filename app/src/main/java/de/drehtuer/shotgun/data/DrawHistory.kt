package de.drehtuer.shotgun.data

import kotlinx.coroutines.flow.Flow

/**
 * The record of every draw made on this device, which is what makes the
 * fairness field evidence rather than decoration.
 */
interface DrawHistory {

    /** Appends a completed draw. */
    suspend fun record(draw: DrawRecord)

    /**
     * The most recent [limit] winning positions, newest first, for plotting the
     * density field. Capped so the field stays cheap to render as history grows.
     */
    fun winners(limit: Int = DEFAULT_FIELD_SAMPLES): Flow<List<DrawPoint>>

    /** The most recent draw, or null when nothing has been drawn yet. */
    fun latest(): Flow<DrawRecord?>

    /** Total draws recorded. */
    fun count(): Flow<Int>

    /** Drops everything. */
    suspend fun clear()

    companion object {
        /** Matches the design's `heatSamples` default. */
        const val DEFAULT_FIELD_SAMPLES = 320

        /**
         * Rows older than this are pruned on write. History exists to show the
         * field is flat, and a few thousand points establish that as well as a
         * million would while keeping reads cheap.
         */
        const val MAX_RETAINED_DRAWS = 2_000
    }
}
