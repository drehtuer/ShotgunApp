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

    /**
     * Drawn, and being revealed one finger at a time. The whole result exists
     * already; this is only how much of it has been shown.
     */
    REVEALING,

    /** Result shown. */
    REVEALED,
}

/** What a completed draw gave each finger. */
data class DrawOutcome(
    val mode: DrawMode,
    val teamCount: Int?,
    /** Finger id to rank (order, 1-based) or team index (teams, 0-based). */
    val assignment: Map<Long, Int>,
    /**
     * The draw order. Revealing along it gives ranks 1, 2, 3 in order, and in
     * teams mode steps between teams on every reveal, because assignments are
     * dealt round robin.
     */
    val order: List<Long>,
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
    private val countdownMillis: Int,
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

    /** How many fingers of [DrawOutcome.order] have been shown so far. */
    var revealedCount: Int = 0
        private set

    /** Whether this finger's assignment is on screen yet. */
    fun isRevealed(id: Long): Boolean {
        val position = outcome?.order?.indexOf(id) ?: return false
        return position in 0 until revealedCount
    }

    private var deadlineAt: Long = 0
    private var armedAt: Long = 0

    /** 0..1 through the countdown; 0 when nothing is armed. */
    fun progress(now: Long): Float {
        if (phase != DrawPhase.COUNTING) return if (phase == DrawPhase.IDLE) 0f else 1f
        val span = (deadlineAt - armedAt).coerceAtLeast(1)
        return ((now - armedAt).toFloat() / span).coerceIn(0f, 1f)
    }

    /**
     * A finger lands.
     *
     * Ignored while a result is being shown *under people's hands* - a
     * latecomer must not join a draw that has already been decided. But once
     * everyone has lifted, the round is over and the result is only being read,
     * so a new finger starts a fresh one.
     */
    fun onDown(id: Long, x: Float, y: Float, now: Long): DrawEffect? {
        if (phase == DrawPhase.REVEALING) return null
        if (phase == DrawPhase.REVEALED) {
            if (_fingers.isNotEmpty()) return null
            reset()
        }
        if (_fingers.containsKey(id)) return null
        _fingers[id] = Finger(id, x, y)
        if (_fingers.size >= MIN_PLAYERS) arm(now)
        return DrawEffect.FingerTick
    }

    /** Repositioning. Never counts as a new player, and never buzzes. */
    fun onMove(id: Long, x: Float, y: Float) {
        _fingers[id]?.let { _fingers[id] = it.copy(x = x, y = y) }
    }

    /** A finger lifts. */
    fun onUp(id: Long, now: Long) {
        _fingers.remove(id) ?: return
        when (phase) {
            // The result deliberately survives the last hand leaving: you have
            // to lift to see what is underneath your own fingers. It is cleared
            // by leaving the surface, or by starting the next draw.
            DrawPhase.REVEALING, DrawPhase.REVEALED -> Unit

            DrawPhase.COUNTING ->
                if (_fingers.size < MIN_PLAYERS) {
                    phase = DrawPhase.IDLE
                    deadlineAt = 0
                } else {
                    // Someone leaving is a change too, and the group deserves
                    // the same settling time after it.
                    arm(now)
                }

            DrawPhase.IDLE -> Unit
        }
    }

    /**
     * Starts, or restarts, the countdown.
     *
     * The countdown is not a fixed delay from the second finger: it is how long
     * the hands have to be *still in number* before the draw runs. Any change -
     * someone joining, someone leaving - puts the full time back on the clock,
     * so nobody is caught out by a draw firing as they reach in.
     */
    private fun arm(now: Long) {
        armedAt = now
        deadlineAt = now + countdownMillis
        phase = DrawPhase.COUNTING
    }

    /** Drives the countdown. Returns an effect on the tick the draw fires. */
    fun tick(now: Long): DrawEffect? {
        if (phase != DrawPhase.COUNTING || now < deadlineAt) return null
        return draw()
    }

    /** Discards a shown result, so the surface is bare again. */
    fun clear() = reset()

    /**
     * Shows one more finger. The UI calls this on a timer while [DrawPhase.REVEALING].
     */
    fun revealNext() {
        if (phase != DrawPhase.REVEALING) return
        val total = outcome?.order?.size ?: return
        revealedCount = (revealedCount + 1).coerceAtMost(total)
        if (revealedCount >= total) phase = DrawPhase.REVEALED
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
            order = shuffled,
            winnerId = shuffled.first(),
            fingers = present,
        )
        outcome = result

        // Starter has one answer, so there is nothing to stagger: showing a
        // single winner slowly is just a slower single winner.
        val staged = !instantReveal && mode != DrawMode.STARTER
        if (staged) {
            // The first finger appears at once - the wait was the countdown,
            // not this - and the rest follow one at a time.
            revealedCount = 1
            phase = if (shuffled.size <= 1) DrawPhase.REVEALED else DrawPhase.REVEALING
        } else {
            revealedCount = shuffled.size
            phase = DrawPhase.REVEALED
        }
        return DrawEffect.Drawn(result)
    }

    private fun reset() {
        phase = DrawPhase.IDLE
        outcome = null
        revealedCount = 0
        deadlineAt = 0
        armedAt = 0
    }

    companion object {
        /** A draw needs someone to lose. */
        const val MIN_PLAYERS = 2
    }
}
