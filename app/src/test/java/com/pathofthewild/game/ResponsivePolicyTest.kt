package com.pathofthewild.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResponsivePolicyTest {
    @Test
    fun navigationSwitchesAtSixHundredDp() {
        assertEquals(NavigationMode.BottomBar, ResponsivePolicy.navigationMode(320f))
        assertEquals(NavigationMode.BottomBar, ResponsivePolicy.navigationMode(599.9f))
        assertEquals(NavigationMode.Rail, ResponsivePolicy.navigationMode(600f))
        assertEquals(NavigationMode.Rail, ResponsivePolicy.navigationMode(1280f))
    }

    @Test
    fun twoColumnContentHasStableBoundary() {
        assertFalse(ResponsivePolicy.useTwoColumns(360f))
        assertFalse(ResponsivePolicy.useTwoColumns(519.9f))
        assertTrue(ResponsivePolicy.useTwoColumns(520f))
    }

    @Test
    fun compactActionRowsDoNotAssumeTallOrWidePhone() {
        assertFalse(ResponsivePolicy.useCompactActionRow(319f))
        assertFalse(ResponsivePolicy.useCompactActionRow(419.9f))
        assertTrue(ResponsivePolicy.useCompactActionRow(420f))
    }

    @Test
    fun fiveButtonRowsRequireWideContentArea() {
        assertFalse(ResponsivePolicy.useFiveButtonRow(600f))
        assertFalse(ResponsivePolicy.useFiveButtonRow(719.9f))
        assertTrue(ResponsivePolicy.useFiveButtonRow(720f))
    }
}
