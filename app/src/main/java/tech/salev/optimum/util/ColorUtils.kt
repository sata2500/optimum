package tech.salev.optimum.util

import androidx.compose.ui.graphics.Color

/**
 * Application-wide color parsing utilities.
 *
 * Color parsing from hex strings appears in multiple composables.
 * Centralising it here eliminates the try/catch boilerplate at every call site
 * and ensures a consistent fallback colour across the whole app.
 */
object ColorUtils {

    /** Slate-500 — neutral fallback when a hex string cannot be parsed. */
    val Fallback: Color = Color(0xFF64748B)

    /**
     * Parses a CSS-style hex color string (e.g. `"#FFD700"`) into a Compose [Color].
     *
     * Returns [Fallback] for null, blank, or malformed input so callers never
     * need a try/catch block.
     */
    fun parse(hex: String?): Color {
        if (hex.isNullOrBlank()) return Fallback
        return runCatching {
            Color(android.graphics.Color.parseColor(hex))
        }.getOrDefault(Fallback)
    }
}
