package de.drehtuer.playerpicker.ui.navigation

/**
 * The four screens in the design. `Draw` carries the mode chosen on Home, which
 * is why mode lives on the home screen at all - it keeps the draw surface bare.
 */
sealed interface Destination {
    val route: String

    data object Home : Destination {
        override val route = "home"
    }

    data object Draw : Destination {
        override val route = "draw/{mode}"
        const val ARG_MODE = "mode"
        fun routeFor(mode: DrawMode) = "draw/${mode.name}"
    }

    data object Result : Destination {
        override val route = "result"
    }

    data object Settings : Destination {
        override val route = "settings"
    }
}

/** The three draw modes offered on the home screen. */
enum class DrawMode {
    /** One finger wins the draw. */
    STARTER,

    /** Every finger gets a number. */
    ORDER,

    /** Uneven sizes allowed. */
    TEAMS,
}
