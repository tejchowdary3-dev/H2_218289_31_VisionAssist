package com.visionassist.eyetest.data.local

import com.visionassist.eyetest.domain.model.AcuityResult
import com.visionassist.eyetest.domain.model.ReliabilityLevel
import com.visionassist.eyetest.domain.model.TestedEye
import com.visionassist.eyetest.domain.model.VisionSessionResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class VisionSessionResultJsonMapperTest {

    @Test
    fun roundTrip_preservesFields() {
        val original = VisionSessionResult(
            acuityRight = AcuityResult(
                eye = TestedEye.RIGHT,
                snellenApproxLabel = "20/40",
                levelIndexAchieved = 4,
                retriesUsed = 1
            ),
            acuityLeft = AcuityResult(
                eye = TestedEye.LEFT,
                snellenApproxLabel = "20/30",
                levelIndexAchieved = 5,
                retriesUsed = 0
            ),
            nearVisionScore01 = 0.62f,
            astigmatismFlag = true,
            contrastSensitivity01 = 0.66f,
            reliability = ReliabilityLevel.MEDIUM,
            reliabilityNotes = "Test note",
            responseTimeAvgMs = 1200L,
            lineConsistencyScore01 = 0.8f,
            blurIllustrationTierIndex = 3
        )
        val json = VisionSessionResultJsonMapper.toJson(original)
        val parsed = VisionSessionResultJsonMapper.fromJson(json)
        assertEquals(original.acuityRight, parsed.acuityRight)
        assertEquals(original.acuityLeft, parsed.acuityLeft)
        assertEquals(original.nearVisionScore01, parsed.nearVisionScore01, 0.0001f)
        assertEquals(original.astigmatismFlag, parsed.astigmatismFlag)
        assertEquals(original.contrastSensitivity01, parsed.contrastSensitivity01, 0.0001f)
        assertEquals(original.reliability, parsed.reliability)
        assertEquals(original.reliabilityNotes, parsed.reliabilityNotes)
        assertEquals(original.responseTimeAvgMs, parsed.responseTimeAvgMs)
        assertEquals(original.lineConsistencyScore01, parsed.lineConsistencyScore01, 0.0001f)
        assertEquals(original.blurIllustrationTierIndex, parsed.blurIllustrationTierIndex)
    }

    @Test
    fun missingAcuities_roundTrip() {
        val original = VisionSessionResult(
            acuityRight = null,
            acuityLeft = null,
            nearVisionScore01 = 0.5f,
            astigmatismFlag = false,
            contrastSensitivity01 = 0.5f,
            reliability = ReliabilityLevel.HIGH,
            reliabilityNotes = "OK",
            responseTimeAvgMs = 3000L,
            lineConsistencyScore01 = 0.75f
        )
        val parsed = VisionSessionResultJsonMapper.fromJson(VisionSessionResultJsonMapper.toJson(original))
        assertEquals(null, parsed.acuityRight)
        assertEquals(null, parsed.acuityLeft)
        assertFalse(parsed.astigmatismFlag)
    }
}
