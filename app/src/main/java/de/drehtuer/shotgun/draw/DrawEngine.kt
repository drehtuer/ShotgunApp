package de.drehtuer.shotgun.draw

import de.drehtuer.shotgun.ui.navigation.DrawMode
import kotlin.random.Random

/** A finger on the glass. [id] is the pointer id, stable while it stays down. */
data class Finger(val id: Long, val x: Float, val y: Float)

enum class DrawPhase {
    /** Fewer than two fingers, nothing armed. */
    IDLE,

    /** Two or more fingers down; the countdown is running. */
    COUNTING,

    /** Drawn, but holding the result back for a beat. */
    SUSPENSE,

    /** Result shown. */
    REVEALED,
}

/** What a completed draw gave each finger. */
data class DrawOutcome(
    val mode: DrawMode,
    val teamCount: Int?,
    /** Finger id to rank (order, 1-based) or team index (teams, 0-based). */
    val assignment: Map<Long, Int>,
    val winnerId: Long,
    val fingers: List<Finger>,
)

/** Something the UI must do that the engine cannot: buzz, or store a result. */
sealed interface DrawEffect {
    /** A finger landed: the keyboard-style tick. */
    data object FingerTick : DrawEffect

    /** The draw fired. Carries the outcome to show, buzz for and record. */
    data class Drawn(val outcome: DrawOutcome) : DrawEffect

    /** The draw could not run; show this for a moment. */
    data class Refused(val message: String) : DrawEffect
}

/**
 * The rules of a draw, with no Android in them.
 *
 * Time is passed in rather than read, and randomness is injected, so every rule
 * here - the countdown extending per finger, the teams guard, who wins - is
 * unit-testable. That matters because the one thing that cannot be tested
 * without hardware is the multi-touch itself.
 */
class DrawEngine(
    val mode: DrawMode,
    val teamCount: Int,
    private val countdownSeconds: Int,
    private val instantReveal: Boolean,
    private val random: Random = Random.Default,
) {
    private val _fingers = LinkedHashMap<Long, Finger>()

    /** Fingers in the order they landed. */
    val fingers: List<Finger> get() = _fingers.values.toList()

    var phase: DrawPhase = DrawPhase.IDLE
        private set

    var outcome: DrawOutcome? = null
        private set

    private var deadlineAt: Long = 0
    private var armedAt: Long = 0

    /** 0..1 through the countdown; 0 when nothing is armed. */
    fun progress(now: Long): Float {
        if (phase != DrawPhase.COUNTING) return if (phase == DrawPhase.IDLE) 0f else 1f
        val span = (deadlineAt - armedAt).coerceAtLeast(1)
        return ((now - armedAt).toFloat() / span).coerceIn(0f, 1f)
    }

    /**
     * A finger lands. Ignored once the draw has fired - a latecomer must not
     * join a result that is already being shown.
     */
    fun onDown(id: Long, x: Float, y: Float, now: Long): DrawEffect? {
        if (phase == DrawPhase.SUSPENSE || phase == DrawPhase.REVEALED) return null
        if (_fingers.containsKey(id)) return null
        _fingers[id] = Finger(id, x, y)

        when {
            _fingers.size < MIN_PLAYERS -> Unit
            phase != DrawPhase.COUNTING -> {
                // The second finger arms the countdown.
                armedAt = now
                deadlineAt = now + countdownSeconds * 1000L
                phase = DrawPhase.COUNTING
            }
            // Every finger after that buys everyone another second, so a late
            // joiner never costs the group their draw.
            else -> deadlineAt += EXTENSION_MILLIS
        }
        return DrawEffect.FingerTick
    }

    /** Repositioning. Never counts as a new player, and never buzzes. */
    fun onMove(id: Long, x: Float, y: Float) {
        _fingers[id]?.let { _fingers[id] = it.copy(x = x, y = y) }
    }

    /** A finger lifts. */
    fun onUp(id: Long) {
        _fingers.remove(id) ?: return
        when (phase) {
            DrawPhase.SUSPENSE, DrawPhase.REVEALED ->
                // The result stays until the last hand leaves the glass.
                if (_fingers.isEmpty()) reset()

            DrawPhase.COUNTING ->
                if (_fingers.size < MIN_PLAYERS) {
                    phase = DrawPhase.IDLE
                    deadlineAt = 0
                }

            DrawPhase.IDLE -> Unit
        }
    }

    /** Drives the countdown. Returns an effect on the tick the draw fires. */
    fun tick(now: Long): DrawEffect? {
        if (phase != DrawPhase.COUNTING || now < deadlineAt) return null
        return draw()
    }

    /** Ends suspense. The UI calls this once the pause has elapsed. */
    fun reveal() {
        if (phase == DrawPhase.SUSPENSE) phase = DrawPhase.REVEALED
    }

    private fun draw(): DrawEffect {
        val present = fingers
        if (mode == DrawMode.TEAMS && present.size < teamCount) {
            phase = DrawPhase.IDLE
            deadlineAt = 0
            return DrawEffect.Refused(
                "${present.size} fingers can't fill $teamCount teams"
            )
        }

        val shuffled = present.map { it.id }.toMutableList().apply { shuffle(random) }
        val assignment = shuffled.withIndex().associate { (index, id) ->
            id to if (mode == DrawMode.TEAMS) index % teamCount else index + 1
        }
        val result = DrawOutcome(
            mode = mode,
            teamCount = teamCount.takeIf { mode == DrawMode.TEAMS },
            assignment = assignment,
            winnerId = shuffled.first(),
            fingers = present,
        )
        outcome = result
        // Starter has nothing to stagger, so it always reveals at once.
        phase = if (instantReveal || mode == DrawMode.STARTER) {
            DrawPhase.REVEALED
        } else {
            DrawPhase.SUSPENSE
        }
        return DrawEffect.Drawn(result)
    }

    private fun reset() {
        phase = DrawPhase.IDLE
        outcome = null
        deadlineAt = 0
        armedAt = 0
    }

    companion object {
        /** A draw needs someone to lose. */
        const val MIN_PLAYERS = 2

        /** Each finger past the second adds this much. */
        const val EXTENSION_MILLIS = 1_000L
    }
}
