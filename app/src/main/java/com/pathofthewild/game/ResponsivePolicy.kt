package com.pathofthewild.game

internal enum class NavigationMode {
    BottomBar,
    Rail
}

/**
 * Shared adaptive-layout thresholds in density-independent pixels.
 * No rule here depends on a particular phone model, resolution, or orientation.
 */
internal object ResponsivePolicy {
    const val NAV_RAIL_MIN_WIDTH_DP = 600f
    const val TWO_COLUMN_MIN_WIDTH_DP = 520f
    const val COMPACT_ACTION_ROW_MIN_WIDTH_DP = 420f
    const val FIVE_BUTTON_ROW_MIN_WIDTH_DP = 720f

    fun navigationMode(widthDp: Float): NavigationMode =
        if (widthDp >= NAV_RAIL_MIN_WIDTH_DP) NavigationMode.Rail else NavigationMode.BottomBar

    fun useTwoColumns(widthDp: Float): Boolean = widthDp >= TWO_COLUMN_MIN_WIDTH_DP

    fun useCompactActionRow(widthDp: Float): Boolean = widthDp >= COMPACT_ACTION_ROW_MIN_WIDTH_DP

    fun useFiveButtonRow(widthDp: Float): Boolean = widthDp >= FIVE_BUTTON_ROW_MIN_WIDTH_DP
}
