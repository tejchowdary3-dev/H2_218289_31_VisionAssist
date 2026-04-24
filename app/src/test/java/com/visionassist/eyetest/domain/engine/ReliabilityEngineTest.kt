package com.visionassist.eyetest.domain.engine

import com.visionassist.eyetest.domain.model.EnvironmentFlags
import com.visionassist.eyetest.domain.model.ReliabilityLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class ReliabilityEngineTest {

    private val engine = ReliabilityEngine()

    @Test
    fun highWhenSignalsClean() {
        val (level, _) = engine.evaluate(
            avgResponseTimeMs = 5_000L,
            lineConsistency01 = 0.9f,
            totalRetries = 0,
            environment = EnvironmentFlags(lightingGood = true, distanceGood = true)
        )
        assertEquals(ReliabilityLevel.HIGH, level)
    }

    @Test
    fun lowWhenVeryFastAndPoorEnvironment() {
        val (level, _) = engine.evaluate(
            avgResponseTimeMs = 100L,
            lineConsistency01 = 0.2f,
            totalRetries = 5,
            environment = EnvironmentFlags(lightingGood = false, distanceGood = false)
        )
        assertEquals(ReliabilityLevel.LOW, level)
    }
}
