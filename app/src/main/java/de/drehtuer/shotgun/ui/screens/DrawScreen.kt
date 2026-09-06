package de.drehtuer.shotgun.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.drehtuer.shotgun.data.settings.RevealTiming
import de.drehtuer.shotgun.data.settings.Settings
import de.drehtuer.shotgun.draw.DrawEffect
import de.drehtuer.shotgun.draw.DrawEngine
import de.drehtuer.shotgun.draw.DrawOutcome
import de.drehtuer.shotgun.draw.DrawPhase
import de.drehtuer.shotgun.draw.Finger
import de.drehtuer.shotgun.ui.components.Rule
import de.drehtuer.shotgun.ui.navigation.DrawMode
import de.drehtuer.shotgun.ui.theme.PPTheme
import de.drehtuer.shotgun.ui.util.Haptics
import de.drehtuer.shotgun.ui.util.KeepScreenOn
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** How long the result is held back before it is shown, in suspense mode. */
private const val SUSPENSE_MILLIS = 600L

/** How long a refusal stays on screen. */
private const val REFUSAL_MILLIS = 2_400L

private const val LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"

/** Test tags. Multi-touch cannot be exercised any other way without hardware. */
const val TAG_FINGER_RING = "finger-ring"
const val TAG_REVEAL_BAR = "reveal-bar"
const val TAG_REFUSAL = "refusal"

/**
 * The draw surface. Everyone puts a finger down, the edge glow counts, and the
 * result lands on the fingers themselves.
 *
 * The rules live in [DrawEngine], deliberately free of Android, so they can be
 * tested. This composable is the part that cannot be: pointer tracking,
 * drawing, and the two haptic strengths.
 */
@Composable
fun DrawScreen(
    mode: DrawMode,
    teamCount: Int,
    settings: Settings,
    onBack: () -> Unit,
    onOpenResult: () -> Unit,
    onDrawComplete: (DrawOutcome, Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Players hold still through the countdown, which produces no touch events
    // for Android's idle timer to see.
    KeepScreenOn()

    val context = LocalContext.current
    val engine = remember(mode, teamCount, settings.countdownSeconds, settings.revealTiming) {
        DrawEngine(
            mode = mode,
            teamCount = teamCount,
            countdownSeconds = settings.countdownSeconds,
            instantReveal = settings.revealTiming == RevealTiming.INSTANT,
        )
    }

    // The engine is a plain object, so a counter is what tells Compose that its
    // state moved. Cheaper than mirroring every field into snapshot state.
    var revision by remember { mutableIntStateOf(0) }
    var progress by remember { mutableStateOf(0f) }
    var refusal by remember { mutableStateOf<String?>(null) }
    var surface by remember { mutableStateOf(0f to 0f) }

    fun handle(effect: DrawEffect?) {
        when (effect) {
            is DrawEffect.FingerTick -> Haptics.tick(context, settings.haptics)
            is DrawEffect.Refused -> refusal = effect.message
            is DrawEffect.Drawn -> {
                Haptics.pattern(
                    context,
                    settings.haptics,
                    if (effect.outcome.mode == DrawMode.STARTER) Haptics.WINNER else Haptics.RESULT,
                )
                onDrawComplete(effect.outcome, surface.first, surface.second)
            }
            null -> Unit
        }
        revision++
    }

    // Drives the countdown and the glow. Runs only while something is armed.
    LaunchedEffect(revision) {
        if (engine.phase != DrawPhase.COUNTING) return@LaunchedEffect
        while (engine.phase == DrawPhase.COUNTING) {
            withFrameMillis { frame ->
                progress = engine.progress(System.currentTimeMillis())
                engine.tick(System.currentTimeMillis())?.let { handle(it) }
            }
        }
    }

    // Suspense is a pause, not a state the engine can time itself out of.
    LaunchedEffect(revision) {
        if (engine.phase == DrawPhase.SUSPENSE) {
            delay(SUSPENSE_MILLIS)
            engine.reveal()
            revision++
        }
    }

    LaunchedEffect(refusal) {
        if (refusal != null) {
            delay(REFUSAL_MILLIS)
            refusal = null
        }
    }

    @Suppress("UNUSED_EXPRESSION") revision // read so recomposition tracks it

    val phase = engine.phase
    val fingers = engine.fingers
    val outcome = engine.outcome

    val glow = when (phase) {
        DrawPhase.COUNTING -> 0.1f + progress * 0.4f
        DrawPhase.SUSPENSE, DrawPhase.REVEALED -> 0.08f
        DrawPhase.IDLE -> 0f
    }
    val frame = if (phase == DrawPhase.COUNTING) 0.2f + progress * 0.8f else 0f
    val chrome by animateFloatAsState(if (fingers.isEmpty()) 1f else 0f, label = "chrome")

    Box(
        modifier
            .fillMaxSize()
            .background(PPTheme.colors.bg)
            .pointerInput(engine) {
                surface = size.width.toFloat() to size.height.toFloat()
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val now = System.currentTimeMillis()
                        when (event.type) {
                            PointerEventType.Press ->
                                event.changes.filter { it.pressed }.forEach { change ->
                                    handle(
                                        engine.onDown(
                                            change.id.value,
                                            change.position.x,
                                            change.position.y,
                                            now,
                                        )
                                    )
                                }

                            PointerEventType.Move ->
                                event.changes.forEach { change ->
                                    engine.onMove(change.id.value, change.position.x, change.position.y)
                                    revision++
                                }

                            PointerEventType.Release ->
                                event.changes.filter { !it.pressed }.forEach { change ->
                                    engine.onUp(change.id.value)
                                    revision++
                                }
                        }
                    }
                }
            },
    ) {
        EdgeGlow(glow)

        if (frame > 0f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .border(PPTheme.dimens.rule, PPTheme.colors.accent.copy(alpha = frame)),
            )
        }

        fingers.forEach { finger ->
            FingerRing(finger, outcome, phase)
        }

        // Chrome: fades out the moment the first finger lands.
        Column(Modifier.alpha(chrome)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "← MODES",
                    style = PPTheme.typography.micro,
                    color = PPTheme.colors.dim,
                    modifier = Modifier
                        .clickable(enabled = fingers.isEmpty(), onClick = onBack)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                )
                Text(
                    text = mode.label(teamCount),
                    style = PPTheme.typography.micro,
                    color = PPTheme.colors.accent,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                )
            }
            Rule()
        }

        if (fingers.size < DrawEngine.MIN_PLAYERS && phase == DrawPhase.IDLE) {
            Hint(fingers.size, Modifier.align(Alignment.CenterStart))
        }

        refusal?.let { message ->
            Refusal(message, Modifier.align(Alignment.BottomCenter).testTag(TAG_REFUSAL))
        }

        if (phase == DrawPhase.REVEALED) {
            RevealBar(onOpenResult, Modifier.align(Alignment.BottomCenter).testTag(TAG_REVEAL_BAR))
        }
    }
}

/** The accent glow that grows out of every edge as the countdown runs. */
@Composable
private fun EdgeGlow(alpha: Float) {
    if (alpha <= 0f) return
    val accent = PPTheme.colors.accent
    Canvas(Modifier.fillMaxSize()) {
        val depth = 56.dp.toPx()
        val tint = accent.copy(alpha = alpha)
        listOf(
            Brush.verticalGradient(listOf(tint, Color.Transparent), 0f, depth),
            Brush.verticalGradient(listOf(Color.Transparent, tint), size.height - depth, size.height),
            Brush.horizontalGradient(listOf(tint, Color.Transparent), 0f, depth),
            Brush.horizontalGradient(listOf(Color.Transparent, tint), size.width - depth, size.width),
        ).forEach { drawRect(it) }
    }
}

/**
 * One finger. Idle it is a bare ring; after the draw it carries whatever the
 * mode gives it - a bloom for the winner, a rank, or a team letter.
 */
@Composable
private fun FingerRing(finger: Finger, outcome: DrawOutcome?, phase: DrawPhase) {
    val colors = PPTheme.colors
    val diameter = PPTheme.dimens.ringDiameter
    val assignment = outcome?.assignment?.get(finger.id)

    var ring = colors.ink
    var fill = Color.Transparent
    var alpha = 1f
    var scale = 1f
    var label: String? = null
    var sub: String? = null
    var labelSize = 40.sp
    var labelColor = colors.ink

    when {
        phase == DrawPhase.SUSPENSE -> {
            ring = colors.accent
            alpha = 0.85f
        }

        assignment != null && outcome != null -> when (outcome.mode) {
            DrawMode.STARTER -> {
                val won = finger.id == outcome.winnerId
                ring = if (won) colors.accent else colors.line
                alpha = if (won) 1f else 0.2f
                scale = if (won) 1.14f else 0.88f
                if (won) fill = colors.accentSoft
            }

            DrawMode.ORDER -> {
                val total = outcome.fingers.size
                val t = if (total > 1) (assignment - 1f) / (total - 1f) else 0f
                ring = colors.accent
                alpha = 1f - t * 0.7f
                scale = if (assignment == 1) 1.12f else 1f
                label = assignment.toString()
                labelSize = if (assignment == 1) 46.sp else 32.sp
                labelColor = colors.accent
                if (assignment == 1) fill = colors.accentSoft
            }

            DrawMode.TEAMS -> {
                val team = colors.teamFills[assignment % colors.teamFills.size]
                ring = team.bg
                fill = team.bg
                label = LETTERS.getOrNull(assignment % 26)?.toString() ?: "${assignment + 1}"
                labelColor = team.ink
                sub = "TEAM"
            }
        }
    }

    val density = LocalDensity.current
    val radiusPx = with(density) { diameter.toPx() } / 2f
    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (finger.x - radiusPx * scale).roundToInt(),
                    (finger.y - radiusPx * scale).roundToInt(),
                )
            }
            .size(diameter * scale)
            .testTag(TAG_FINGER_RING)
            .alpha(alpha)
            .background(fill, CircleShape)
            .border(4.dp, ring, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            label?.let {
                Text(it, color = labelColor, fontSize = labelSize, fontWeight = FontWeight.ExtraBold)
            }
            sub?.let {
                Text(
                    text = it,
                    color = labelColor,
                    style = PPTheme.typography.microWide,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun Hint(fingerCount: Int, modifier: Modifier = Modifier) {
    val (title, subtitle) = if (fingerCount == 0) {
        "EVERYONE, ONE FINGER DOWN." to
            "Hold still — the edge glow is the countdown. Drag to reposition; a moving finger is not a new player."
    } else {
        "ONE MORE FINGER." to "At least two players are needed to draw."
    }
    Column(modifier.padding(horizontal = 24.dp)) {
        Text(title, style = PPTheme.typography.hintTitle, color = PPTheme.colors.ink)
        Text(
            text = subtitle,
            style = PPTheme.typography.body,
            color = PPTheme.colors.dim,
            modifier = Modifier
                .padding(top = 10.dp)
                .widthIn(max = 300.dp),
        )
    }
}

/** The accent bar that says why a draw could not run. */
@Composable
private fun Refusal(message: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .background(PPTheme.colors.accent)
            .padding(16.dp),
    ) {
        Text(
            text = "CAN'T DRAW",
            style = PPTheme.typography.microWide,
            color = PPTheme.colors.accentInk.copy(alpha = 0.75f),
        )
        Text(
            text = message,
            style = PPTheme.typography.sectionTitle,
            color = PPTheme.colors.accentInk,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun RevealBar(onOpenResult: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Rule()
        Row(
            Modifier
                .fillMaxWidth()
                .background(PPTheme.colors.bg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "LIFT ALL FINGERS TO CLEAR",
                style = PPTheme.typography.micro,
                color = PPTheme.colors.dim,
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
            )
            Text(
                text = "DETAILS",
                style = PPTheme.typography.body.copy(fontWeight = FontWeight.ExtraBold),
                color = PPTheme.colors.accentInk,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .background(PPTheme.colors.accent)
                    .clickable(onClick = onOpenResult)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            )
        }
    }
}

/** The label shown top-right while the surface is still bare. */
private fun DrawMode.label(teamCount: Int): String = when (this) {
    DrawMode.STARTER -> "STARTING PLAYER"
    DrawMode.ORDER -> "PLAYER ORDER"
    DrawMode.TEAMS -> "$teamCount TEAMS"
}
