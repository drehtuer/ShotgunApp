package de.drehtuer.shotgun.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import de.drehtuer.shotgun.data.DrawPoint
import de.drehtuer.shotgun.data.DrawRecord
import de.drehtuer.shotgun.result.HeatField
import de.drehtuer.shotgun.result.HeatStop
import de.drehtuer.shotgun.ui.components.Rule
import de.drehtuer.shotgun.ui.components.ScreenHeader
import de.drehtuer.shotgun.ui.navigation.DrawMode
import de.drehtuer.shotgun.ui.theme.PPColors
import de.drehtuer.shotgun.ui.theme.PPTheme
import kotlin.math.roundToInt

private const val LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"

/** The field is computed at this width and scaled up; it is a blur either way. */
private const val FIELD_WIDTH = 160

const val TAG_FAIRNESS_FIELD = "fairness-field"

/**
 * Result. The point of this screen is trust: it shows that where you put your
 * finger changes nothing, by plotting every winner ever recorded on this device
 * and letting you see the field is flat.
 */
@Composable
fun ResultScreen(
    winners: List<DrawPoint>,
    latest: DrawRecord?,
    totalDraws: Int,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(PPTheme.colors.bg),
    ) {
        ScreenHeader(title = "RESULT", actionLabel = "CLOSE", onAction = onClose)

        latest?.let { record ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = record.modeLabel(),
                    style = PPTheme.typography.microWide,
                    color = PPTheme.colors.accent,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                )
                Text(
                    text = record.summary(),
                    style = PPTheme.typography.micro,
                    color = PPTheme.colors.dim,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                )
            }
            Rule()
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "FAIRNESS",
                style = PPTheme.typography.sectionTitle,
                color = PPTheme.colors.ink,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
            )
            Text(
                text = "${winners.size} WINNERS PLOTTED",
                style = PPTheme.typography.micro,
                color = PPTheme.colors.dim,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
            )
        }
        Rule()

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(PPTheme.colors.surface)
                .clipToBounds()
                .testTag(TAG_FAIRNESS_FIELD),
        ) {
            FairnessField(winners, PPTheme.colors)

            latest?.let { record -> LastDrawDots(record, PPTheme.colors) }

            if (winners.isEmpty() && latest == null) {
                NoDrawsYet(Modifier.align(Alignment.TopStart))
            }

            Legend(Modifier.align(Alignment.BottomCenter))
        }
        Rule()

        Text(
            text = caption(latest != null, winners.size),
            style = PPTheme.typography.micro.copy(fontWeight = FontWeight.Normal),
            color = PPTheme.colors.dim,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
        )
    }
}

/** The density field itself, rasterised once per history change. */
@Composable
private fun FairnessField(winners: List<DrawPoint>, colors: PPColors) {
    var size by remember { mutableStateOf(IntSize.Zero) }

    val bitmap: ImageBitmap? = remember(winners, colors.isDark, size) {
        if (size.width == 0 || size.height == 0) return@remember null
        val height = (FIELD_WIDTH * size.height / size.width).coerceAtLeast(1)
        val stops = colors.heatRamp.map { HeatStop(it.position, it.color.toArgb()) }
        val density = HeatField.density(winners.map { it.x to it.y }, FIELD_WIDTH, height)
        val pixels = HeatField.colorise(density, stops)
        android.graphics.Bitmap.createBitmap(
            pixels, FIELD_WIDTH, height, android.graphics.Bitmap.Config.ARGB_8888,
        ).asImageBitmap()
    }

    Canvas(
        Modifier
            .fillMaxSize()
            .onSizeChanged { size = it },
    ) {
        bitmap?.let { drawScaled(it) }
    }
}

/** Stretches the low-resolution field over the whole panel. */
private fun DrawScope.drawScaled(bitmap: ImageBitmap) {
    drawImage(
        image = bitmap,
        srcSize = IntSize(bitmap.width, bitmap.height),
        dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
    )
}

/** The last draw, plotted on top of the accumulated field. */
@Composable
private fun LastDrawDots(record: DrawRecord, colors: PPColors) {
    // The dots are placed by fraction of the panel, so the same stored record
    // lands in the same relative spot whatever the screen size.
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val panelWidth = maxWidth
        val panelHeight = maxHeight
        record.points.forEach { point ->
            val teams = record.mode == DrawMode.TEAMS
            val team = colors.teamFills[(point.assignment ?: 0) % colors.teamFills.size]
            val diameter = when {
                teams -> 32.dp
                point.won -> 36.dp
                else -> 28.dp
            }
            val fill = when {
                teams -> team.bg
                point.won -> colors.ink
                else -> colors.bg
            }
            val ink = when {
                teams -> team.ink
                point.won -> colors.bg
                else -> colors.ink
            }
            val label = when {
                teams -> LETTERS.getOrNull((point.assignment ?: 0) % 26)?.toString() ?: ""
                record.mode == DrawMode.ORDER -> point.assignment?.toString() ?: ""
                point.won -> "1"
                else -> ""
            }

            Box(
                modifier = Modifier
                    .offset(
                        x = panelWidth * point.x - diameter / 2,
                        y = panelHeight * point.y - diameter / 2,
                    )
                    .size(diameter)
                    .background(fill, CircleShape)
                    .border(3.dp, colors.ink, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (label.isNotEmpty()) {
                    Text(label, color = ink, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun NoDrawsYet(modifier: Modifier = Modifier) {
    Column(modifier.padding(16.dp)) {
        Text(
            text = "NO DRAWS YET",
            style = PPTheme.typography.screenTitle.copy(fontSize = 21.sp),
            color = PPTheme.colors.ink,
            modifier = Modifier
                .background(PPTheme.colors.bg)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
        Text(
            text = "Run a draw — it lands on top of this field.",
            style = PPTheme.typography.body,
            color = PPTheme.colors.ink,
            modifier = Modifier
                .padding(top = 6.dp)
                .background(PPTheme.colors.bg)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun Legend(modifier: Modifier = Modifier) {
    val colors = PPTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        LegendLabel("COLD")
        Box(
            Modifier
                .weight(1f)
                .height(6.dp)
                .background(Brush.horizontalGradient(colors.heatRamp.map { it.color })),
        )
        LegendLabel("HOT")
    }
}

@Composable
private fun LegendLabel(text: String) {
    Text(
        text = text,
        style = PPTheme.typography.microWide.copy(fontSize = 10.sp),
        color = PPTheme.colors.ink,
        modifier = Modifier
            .background(PPTheme.colors.bg)
            .padding(horizontal = 5.dp, vertical = 2.dp),
    )
}

private fun DrawRecord.modeLabel(): String = when (mode) {
    DrawMode.STARTER -> "Starting player"
    DrawMode.ORDER -> "Player order"
    DrawMode.TEAMS -> "${teamCount ?: 0} teams"
}

private fun DrawRecord.summary(): String = when (mode) {
    DrawMode.STARTER -> "${points.size} PLAYERS"
    DrawMode.ORDER -> "${points.size} IN ORDER"
    DrawMode.TEAMS -> "${teamCount ?: 0} TEAMS · ${points.size} PLAYERS"
}

private fun caption(hasResult: Boolean, plotted: Int): String = if (hasResult) {
    "Your last draw sits on top of $plotted logged winners. The field stays flat " +
        "edge to edge — where you put your finger changes nothing."
} else {
    "$plotted logged winners, flat edge to edge — no corner of the screen wins more often."
}
