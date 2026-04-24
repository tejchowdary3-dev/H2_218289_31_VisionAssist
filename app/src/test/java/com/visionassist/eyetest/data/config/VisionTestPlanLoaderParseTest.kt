package com.visionassist.eyetest.data.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VisionTestPlanLoaderParseTest {

    @Test
    fun parse_minimalJson_mergesDefaults() {
        val json = """
            {
              "snellen_levels": [
                { "label": "20/60", "font_sp": 24.0 }
              ],
              "line_length": 5,
              "correct_threshold": 4
            }
        """.trimIndent()
        val cfg = VisionTestPlanLoader.parseVisionTestConfig(json)
        assertEquals(5, cfg.lineLength)
        assertEquals(4, cfg.correctThreshold)
        assertEquals(1, cfg.snellenLevels.size)
        assertEquals("20/60", cfg.snellenLevels[0].snellenLabel)
        assertTrue(cfg.nearOptionScores.isNotEmpty())
    }
}
