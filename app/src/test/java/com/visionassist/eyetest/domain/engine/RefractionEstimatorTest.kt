package com.visionassist.eyetest.domain.engine

import com.visionassist.eyetest.domain.model.AcuityResult
import com.visionassist.eyetest.domain.model.AppRuntimeConfigDefaults
import com.visionassist.eyetest.domain.model.BlurLadderDefaults
import com.visionassist.eyetest.domain.model.FocalDifficultyPattern
import com.visionassist.eyetest.domain.model.QuestionnaireAnswers
import com.visionassist.eyetest.domain.model.RefractionSeverity
import com.visionassist.eyetest.domain.model.ReliabilityLevel
import com.visionassist.eyetest.domain.model.SymptomLevel
import com.visionassist.eyetest.domain.model.TestedEye
import com.visionassist.eyetest.domain.model.VisionSessionResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RefractionEstimatorTest {

    private val ladder = BlurLadderDefaults.create()
    private val cfg = AppRuntimeConfigDefaults.create().refractionEstimator

    @Test
    fun `no blur tier perfect acuity and no symptoms yields indeterminate`() {
        val est = RefractionEstimator(cfg, ladder).estimate(
            session = baseSession().copy(
                acuityRight = AcuityResult(TestedEye.RIGHT, "20/20", 7, 0),
                acuityLeft = AcuityResult(TestedEye.LEFT, "20/20", 7, 0),
                nearVisionScore01 = 0.9f
            ),
            questionnaire = QuestionnaireAnswers(
                focalPattern = FocalDifficultyPattern.NEITHER,
                distanceBlur = SymptomLevel.NONE,
                nearDifficulty = SymptomLevel.NONE,
                eyeStrain = SymptomLevel.NONE,
                nightVision = null
            ),
            blurTierIndex = null
        )
        assertEquals(RefractionSeverity.INDETERMINATE, est.severity)
    }

    @Test
    fun `high blur tier increases severity`() {
        val est = RefractionEstimator(cfg, ladder).estimate(
            session = baseSession(),
            questionnaire = null,
            blurTierIndex = 5
        )
        assertTrue(est.severity == RefractionSeverity.MODERATE || est.severity == RefractionSeverity.STRONG)
        assertNotNull(est.diopterFeelLow)
        assertNotNull(est.diopterFeelHigh)
    }

    private fun baseSession() = VisionSessionResult(
        acuityRight = AcuityResult(TestedEye.RIGHT, "20/40", 4, 0),
        acuityLeft = AcuityResult(TestedEye.LEFT, "20/40", 4, 0),
        nearVisionScore01 = 0.5f,
        astigmatismFlag = false,
        contrastSensitivity01 = 0.5f,
        reliability = ReliabilityLevel.HIGH,
        reliabilityNotes = "",
        responseTimeAvgMs = 100L,
        lineConsistencyScore01 = 0.9f,
        blurIllustrationTierIndex = null
    )
}
