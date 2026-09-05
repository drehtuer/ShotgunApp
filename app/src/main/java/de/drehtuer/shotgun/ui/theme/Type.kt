package de.drehtuer.shotgun.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import de.drehtuer.shotgun.R

/** Archivo, bundled as static weights. Modernist sets everything in Archivo. */
val Archivo = FontFamily(
    Font(R.font.archivo_regular, FontWeight.Normal),
    Font(R.font.archivo_medium, FontWeight.Medium),
    Font(R.font.archivo_semibold, FontWeight.SemiBold),
    Font(R.font.archivo_bold, FontWeight.Bold),
    Font(R.font.archivo_extrabold, FontWeight.ExtraBold),
)

/**
 * Named type roles taken from the design export. These are deliberately not
 * Material's slots - the design has its own vocabulary (a wordmark, a card
 * title, a micro label) that does not map onto `titleLarge` and friends.
 */
@Immutable
data class PPTypography(
    /** The "SHOTGUN!" wordmark beside the mark on Home. */
    val wordmark: TextStyle,
    /** The home headline: "PICK A MODE. HANDS ON GLASS." */
    val display: TextStyle,
    /** Screen titles: RESULT, SETTINGS. */
    val screenTitle: TextStyle,
    /** Mode card titles: STARTING PLAYER, TEAMS. */
    val cardTitle: TextStyle,
    /** The breathing hint on an empty draw surface. */
    val hintTitle: TextStyle,
    /** Section headings inside a screen: FAIRNESS. */
    val sectionTitle: TextStyle,
    /** A settings row's title. */
    val settingTitle: TextStyle,
    /** Supporting copy under a title. */
    val body: TextStyle,
    /** Supporting copy in denser rows. */
    val bodySmall: TextStyle,
    /** Wide-tracked uppercase label: "← MODES", "CLOSE". */
    val micro: TextStyle,
    /** The widest-tracked, smallest label: "TEAMS", "SECONDS". */
    val microWide: TextStyle,
    /** Tabular value in a stepper. */
    val stepperValue: TextStyle,
)

private val base = TextStyle(fontFamily = Archivo)

fun ppTypography(): PPTypography = PPTypography(
    wordmark = base.copy(
        fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.02).em,
    ),
    display = base.copy(
        fontSize = 30.sp, fontWeight = FontWeight.ExtraBold,
        lineHeight = 31.5.sp, letterSpacing = (-0.01).em,
    ),
    screenTitle = base.copy(
        fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.01).em,
    ),
    cardTitle = base.copy(
        fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.01).em,
    ),
    hintTitle = base.copy(
        fontSize = 29.sp, fontWeight = FontWeight.ExtraBold,
        lineHeight = 31.9.sp, letterSpacing = (-0.01).em,
    ),
    sectionTitle = base.copy(fontSize = 19.sp, fontWeight = FontWeight.ExtraBold),
    settingTitle = base.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold),
    body = base.copy(fontSize = 15.sp, fontWeight = FontWeight.Normal),
    bodySmall = base.copy(fontSize = 14.sp, fontWeight = FontWeight.Normal),
    micro = base.copy(
        fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.14.em,
    ),
    microWide = base.copy(
        fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.18.em,
    ),
    stepperValue = base.copy(fontSize = 30.sp, fontWeight = FontWeight.ExtraBold),
)
