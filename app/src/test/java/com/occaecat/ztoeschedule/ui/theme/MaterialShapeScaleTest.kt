package com.occaecat.ztoeschedule.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class MaterialShapeScaleTest {
    @Test
    fun `24dp base radius keeps Material hierarchy`() {
        val scale = materialShapeScaleForCornerRadius(24)

        assertEquals(8, scale.extraSmall)
        assertEquals(12, scale.small)
        assertEquals(16, scale.medium)
        assertEquals(24, scale.large)
        assertEquals(32, scale.extraLarge)
    }

    @Test
    fun `small base radius is clamped to safe Material minimums`() {
        val scale = materialShapeScaleForCornerRadius(6)

        assertEquals(4, scale.extraSmall)
        assertEquals(8, scale.small)
        assertEquals(12, scale.medium)
        assertEquals(16, scale.large)
        assertEquals(24, scale.extraLarge)
    }
}
