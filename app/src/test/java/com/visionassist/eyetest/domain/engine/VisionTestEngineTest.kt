package com.visionassist.eyetest.domain.engine

import com.visionassist.eyetest.domain.model.VisionTestConfigDefaults
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VisionTestEngineTest {

    private val config = VisionTestConfigDefaults.create()
    private val engine = VisionTestEngine(config, Random(42L))

    @Test
    fun randomLine_respectsLengthAndAlphabet() {
        val line = engine.randomLine(5)
        assertEquals(5, line.length)
        line.forEach { ch ->
            assertTrue(config.letters.contains(ch))
        }
    }

    @Test
    fun linePassed_usesConfigThreshold() {
        assertTrue(engine.linePassed(correctCount = 4, total = 5))
        assertTrue(!engine.linePassed(correctCount = 3, total = 5))
    }

    @Test
    fun fontSizeSpForLevel_clampsIndex() {
        val last = engine.snellenLevels.lastIndex
        assertEquals(engine.fontSizeSpForLevel(last), engine.fontSizeSpForLevel(999))
        assertEquals(engine.fontSizeSpForLevel(0), engine.fontSizeSpForLevel(-5))
    }
}
